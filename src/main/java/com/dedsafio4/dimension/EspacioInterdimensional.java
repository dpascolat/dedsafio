package com.dedsafio4.dimension;

import com.dedsafio4.Dedsafio4;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

/**
 * El Espacio Interdimensional: una dimensión como el End (islas flotando en el vacío) pero sin estructuras.
 * El piso es de Roca Espacial (oscura) con manchas de Roca Espacial Gris; crecen Árboles Espaciales (como la planta
 * del End: el tubo morado oscuro y las flores de las puntas rosas o lilas; no sueltan nada) y Pasto Espacial marrón. El cielo (nebulosa y estrellas) lo dibuja EspacioInterdimensionalCielo.
 * Se entra con: /execute in dedsafio4:espacio_interdimensional run tp @s 0 80 0
 */
public final class EspacioInterdimensional {
	private EspacioInterdimensional() {}

	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, id("espacio_interdimensional"));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	private static Block bloque(String nombre, Block bloque) {
		Registry.register(BuiltInRegistries.BLOCK, id(nombre), bloque);
		Registry.register(BuiltInRegistries.ITEM, id(nombre), new BlockItem(bloque, new Item.Properties()));
		return bloque;
	}

	public static final Block ROCA = bloque("roca_espacial", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK).strength(3f, 9f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
	public static final Block ROCA_GRIS = bloque("roca_espacial_gris", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY).strength(3f, 9f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	/** El tubo del árbol (morado oscuro). */
	public static final Block ARBOL = bloque("arbol_espacial", new ArbolBlock(propiedadesArbol(MapColor.COLOR_PURPLE)));
	/** Las flores de las puntas, rosas o lilas. */
	public static final Block FLOR_ROSA = bloque("flor_espacial_rosa", new FlorBlock(propiedadesArbol(MapColor.COLOR_PINK)));
	public static final Block FLOR_LILA = bloque("flor_espacial_lila", new FlorBlock(propiedadesArbol(MapColor.COLOR_MAGENTA)));
	public static final Block PASTO = bloque("pasto_espacial", new PastoBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BROWN).replaceable().noCollission().instabreak().sound(SoundType.GRASS)
			.offsetType(BlockBehaviour.OffsetType.XYZ).pushReaction(PushReaction.DESTROY)));

	public static final Block[] FLORES = {FLOR_ROSA, FLOR_LILA};

	private static BlockBehaviour.Properties propiedadesArbol(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(0.4f).sound(SoundType.WOOD).noOcclusion()
				.pushReaction(PushReaction.DESTROY).noLootTable();
	}

	/** El piso de la dimensión (donde crecen los árboles y el pasto). */
	public static boolean esPiso(BlockState estado) {
		return estado.is(ROCA) || estado.is(ROCA_GRIS);
	}

	public static final Feature<NoneFeatureConfiguration> ARBOL_FEATURE = Registry.register(BuiltInRegistries.FEATURE,
			id("arbol_espacial"), new ArbolFeature());

	/** Fuerza la carga de la clase (registra los bloques y la generación). */
	public static void registrar() {}

	// --- El Árbol Espacial: como la planta del End (un tubo que se conecta con sus vecinos) ---

	public static class ArbolBlock extends PipeBlock {
		public static final MapCodec<ArbolBlock> CODEC = simpleCodec(ArbolBlock::new);

		public ArbolBlock(Properties propiedades) {
			super(0.3125f, propiedades);
			registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
					.setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
		}

		@Override
		protected MapCodec<? extends PipeBlock> codec() {
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
			constructor.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
		}

		private static boolean conecta(BlockState vecino, Direction hacia) {
			return vecino.getBlock() instanceof ArbolBlock || vecino.getBlock() instanceof FlorBlock || (hacia == Direction.DOWN && esPiso(vecino));
		}

		/** El estado con las conexiones según lo que tiene alrededor. */
		public BlockState conectado(BlockGetter mundo, BlockPos pos) {
			BlockState estado = defaultBlockState();
			for (Direction d : Direction.values()) {
				estado = estado.setValue(PROPERTY_BY_DIRECTION.get(d), conecta(mundo.getBlockState(pos.relative(d)), d));
			}
			return estado;
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext contexto) {
			return conectado(contexto.getLevel(), contexto.getClickedPos());
		}

		@Override
		protected BlockState updateShape(BlockState estado, Direction lado, BlockState vecino, LevelAccessor mundo, BlockPos pos, BlockPos posVecino) {
			if (!estado.canSurvive(mundo, pos)) {
				mundo.scheduleTick(pos, this, 1);
				return estado;
			}
			return estado.setValue(PROPERTY_BY_DIRECTION.get(lado), conecta(vecino, lado));
		}

		@Override
		protected void tick(BlockState estado, ServerLevel mundo, BlockPos pos, RandomSource azar) {
			if (!estado.canSurvive(mundo, pos)) mundo.destroyBlock(pos, true);
		}

		/** Como la planta del End: se sostiene en el piso o en otro tubo de abajo (o de costado, si ese está sostenido). */
		@Override
		protected boolean canSurvive(BlockState estado, LevelReader mundo, BlockPos pos) {
			BlockState abajo = mundo.getBlockState(pos.below());
			boolean libreArribaYAbajo = !mundo.getBlockState(pos.above()).isAir() && !abajo.isAir();
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos lado = pos.relative(d);
				if (mundo.getBlockState(lado).getBlock() instanceof ArbolBlock) {
					if (libreArribaYAbajo) return false;
					BlockState debajoDelLado = mundo.getBlockState(lado.below());
					if (debajoDelLado.getBlock() instanceof ArbolBlock || esPiso(debajoDelLado)) return true;
				}
			}
			return abajo.getBlock() instanceof ArbolBlock || esPiso(abajo);
		}

		@Override
		protected boolean isPathfindable(BlockState estado, net.minecraft.world.level.pathfinder.PathComputationType tipo) {
			return false;
		}
	}

	// --- La flor de las puntas: un cubo que se sostiene arriba (o al costado) de un tubo ---

	public static class FlorBlock extends Block {
		public FlorBlock(Properties propiedades) {
			super(propiedades);
		}

		@Override
		protected boolean canSurvive(BlockState estado, LevelReader mundo, BlockPos pos) {
			for (Direction d : Direction.values()) {
				if (d != Direction.UP && mundo.getBlockState(pos.relative(d)).getBlock() instanceof ArbolBlock) return true;
			}
			return false;
		}

		@Override
		protected BlockState updateShape(BlockState estado, Direction lado, BlockState vecino, LevelAccessor mundo, BlockPos pos, BlockPos posVecino) {
			if (!estado.canSurvive(mundo, pos)) mundo.scheduleTick(pos, this, 1);
			return estado;
		}

		@Override
		protected void tick(BlockState estado, ServerLevel mundo, BlockPos pos, RandomSource azar) {
			if (!estado.canSurvive(mundo, pos)) mundo.destroyBlock(pos, true);
		}
	}

	// --- El Pasto Espacial: una plantita marrón ---

	public static class PastoBlock extends BushBlock {
		public static final MapCodec<PastoBlock> CODEC = simpleCodec(PastoBlock::new);
		private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 12, 14);

		public PastoBlock(Properties propiedades) {
			super(propiedades);
		}

		@Override
		protected MapCodec<? extends BushBlock> codec() {
			return CODEC;
		}

		@Override
		protected boolean mayPlaceOn(BlockState estado, BlockGetter mundo, BlockPos pos) {
			return esPiso(estado) || super.mayPlaceOn(estado, mundo, pos);
		}

		@Override
		protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
			return FORMA.move(estado.getOffset(mundo, pos).x, estado.getOffset(mundo, pos).y, estado.getOffset(mundo, pos).z);
		}
	}

	// --- La generación del árbol: un tronco y ramas que suben y se abren, como la planta del End ---

	static class ArbolFeature extends Feature<NoneFeatureConfiguration> {
		ArbolFeature() {
			super(NoneFeatureConfiguration.CODEC);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
			WorldGenLevel mundo = contexto.level();
			BlockPos base = contexto.origin();
			RandomSource azar = contexto.random();
			if (!mundo.isEmptyBlock(base) || !esPiso(mundo.getBlockState(base.below()))) return false;
			ArbolBlock bloque = (ArbolBlock) ARBOL;
			Block flor = FLORES[azar.nextInt(FLORES.length)];
			Set<BlockPos> puestos = new HashSet<>();
			Set<BlockPos> puntas = new HashSet<>();
			crecer(mundo, base, base, azar, 0, puestos, puntas);
			for (BlockPos p : puestos) mundo.setBlock(p, bloque.defaultBlockState(), 2);
			// Una flor arriba de cada punta de rama.
			for (BlockPos p : puntas) if (mundo.isEmptyBlock(p.above())) mundo.setBlock(p.above(), flor.defaultBlockState(), 2);
			// Las conexiones, cuando ya están todos puestos.
			for (BlockPos p : puestos) mundo.setBlock(p, bloque.conectado(mundo, p), 2);
			return !puestos.isEmpty();
		}

		/** Sube un tramo y abre ramas hacia los costados, que vuelven a subir (como ChorusFlowerBlock.generatePlant). */
		private static void crecer(WorldGenLevel mundo, BlockPos desde, BlockPos raiz, RandomSource azar, int nivel, Set<BlockPos> puestos,
								   Set<BlockPos> puntas) {
			int alto = azar.nextInt(4) + 1;
			if (nivel == 0) alto++;
			BlockPos p = desde;
			for (int i = 0; i < alto; i++) {
				p = p.above();
				if (!libre(mundo, p, puestos) || p.getY() > raiz.getY() + 22) {
					if (i > 0) puntas.add(p.below());
					return;
				}
				puestos.add(p);
			}
			int ramas = nivel >= 4 ? 0 : azar.nextInt(4) + (nivel == 0 ? 1 : 0);
			int hechas = 0;
			for (int r = 0; r < ramas; r++) {
				Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(azar);
				BlockPos lado = p.relative(d);
				if (Math.abs(lado.getX() - raiz.getX()) >= 8 || Math.abs(lado.getZ() - raiz.getZ()) >= 8) continue;
				if (!libre(mundo, lado, puestos) || !libre(mundo, lado.below(), puestos)) continue;
				puestos.add(lado);
				hechas++;
				crecer(mundo, lado, raiz, azar, nivel + 1, puestos, puntas);
			}
			if (hechas == 0) puntas.add(p);   // sin ramas: termina en flor
		}

		private static boolean libre(WorldGenLevel mundo, BlockPos p, Set<BlockPos> puestos) {
			return !puestos.contains(p) && mundo.isEmptyBlock(p);
		}
	}
}
