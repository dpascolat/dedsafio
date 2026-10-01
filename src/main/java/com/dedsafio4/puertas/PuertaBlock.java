package com.dedsafio4.puertas;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Puerta de 3x3 bloques que se abre con una llave: al usar la llave sobre la puerta cerrada, la llave
 * se gasta y la puerta se abre (la mitad de arriba sube y la de abajo baja, dentro del marco) y queda
 * abierta. No se puede romper en supervivencia.
 *
 * Son 9 bloques; PARTE = fila * 3 + columna (fila 0 abajo, columna 0 a la izquierda mirando el frente).
 * El del medio de abajo (parte 1) es el principal: tiene la entidad que dibuja toda la puerta.
 */
public class PuertaBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 8);
	public static final BooleanProperty ABIERTA = BooleanProperty.create("abierta");
	public static final int PRINCIPAL = 1;

	private final Supplier<Item> llave;
	/** Nombre de la llave para el aviso ("Necesitás la ..."). */
	private final String nombreLlave;

	public PuertaBlock(Properties propiedades, Supplier<Item> llave, String nombreLlave) {
		super(propiedades);
		this.llave = llave;
		this.nombreLlave = nombreLlave;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PARTE, PRINCIPAL).setValue(ABIERTA, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(p -> new PuertaBlock(p, llave, nombreLlave));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PARTE, ABIERTA);
	}

	// --- Las 9 partes ---

	/** Hacia dónde está la "derecha" de la puerta (mirándola de frente es la izquierda de quien mira). */
	private static Direction derecha(Direction frente) {
		return frente.getCounterClockWise();
	}

	private static BlockPos posParte(BlockPos principal, Direction frente, int parte) {
		int col = parte % 3, fila = parte / 3;
		return principal.relative(derecha(frente), col - 1).above(fila);
	}

	public static BlockPos principal(BlockPos pos, BlockState estado) {
		int parte = estado.getValue(PARTE), col = parte % 3, fila = parte / 3;
		return pos.relative(derecha(estado.getValue(FACING)), 1 - col).below(fila);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		Direction frente = contexto.getHorizontalDirection().getOpposite();
		BlockPos pos = contexto.getClickedPos();
		Level level = contexto.getLevel();
		for (int parte = 0; parte < 9; parte++) {
			BlockPos p = posParte(pos, frente, parte);
			if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(contexto)) return null;
		}
		return defaultBlockState().setValue(FACING, frente).setValue(PARTE, PRINCIPAL);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
		for (int parte = 0; parte < 9; parte++) {
			if (parte == PRINCIPAL) continue;
			level.setBlock(posParte(pos, estado.getValue(FACING), parte), estado.setValue(PARTE, parte), 3);
		}
	}

	/** Si se saca una parte, se saca la puerta entera. */
	@Override
	protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
		if (!nuevo.is(this)) {
			BlockPos principal = principal(pos, estado);
			for (int parte = 0; parte < 9; parte++) {
				BlockPos p = posParte(principal, estado.getValue(FACING), parte);
				if (p.equals(pos)) continue;
				BlockState otro = level.getBlockState(p);
				if (otro.is(this) && otro.getValue(PARTE) == parte && otro.getValue(FACING) == estado.getValue(FACING)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		super.onRemove(estado, level, pos, nuevo, movido);
	}

	// --- Abrir con la llave ---

	@Override
	protected ItemInteractionResult useItemOn(ItemStack pila, BlockState estado, Level level, BlockPos pos, Player jugador,
											  InteractionHand mano, BlockHitResult golpe) {
		if (estado.getValue(ABIERTA) || !pila.is(llave.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide) {
			pila.consume(1, jugador);
			abrir(level, pos, estado);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (estado.getValue(ABIERTA)) return InteractionResult.PASS;
		if (!level.isClientSide) {
			jugador.displayClientMessage(Component.literal("Está cerrada. Necesitás la " + nombreLlave + ".").withColor(0xFF6B6B), true);
			level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 0.6f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private void abrir(Level level, BlockPos pos, BlockState estado) {
		BlockPos principal = principal(pos, estado);
		Direction frente = estado.getValue(FACING);
		for (int parte = 0; parte < 9; parte++) {
			BlockPos p = posParte(principal, frente, parte);
			BlockState otro = level.getBlockState(p);
			if (otro.is(this)) level.setBlock(p, otro.setValue(ABIERTA, true), 3);
		}
		BlockPos centro = principal.above();
		level.playSound(null, centro, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.6f);
		level.playSound(null, centro, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7f, 0.7f);
	}

	// --- Forma ---

	/** Una caja en coordenadas de la puerta (lx a lo ancho, ly alto, en píxeles), con el grosor del marco. */
	private static VoxelShape caja(Direction frente, double lx0, double ly0, double lx1, double ly1) {
		double z0 = 6.25, z1 = 9.75;
		Direction der = derecha(frente);
		double a = der.getAxisDirection() == Direction.AxisDirection.POSITIVE ? lx0 : 16 - lx1;
		double b = der.getAxisDirection() == Direction.AxisDirection.POSITIVE ? lx1 : 16 - lx0;
		return der.getAxis() == Direction.Axis.X ? Block.box(a, ly0, z0, b, ly1, z1) : Block.box(z0, ly0, a, z1, ly1, b);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
		Direction frente = estado.getValue(FACING);
		if (!estado.getValue(ABIERTA)) return caja(frente, 0, 0, 16, 16);
		// Abierta: solo queda el marco de los bordes.
		int parte = estado.getValue(PARTE), col = parte % 3, fila = parte / 3;
		VoxelShape forma = Shapes.empty();
		if (col == 0) forma = Shapes.or(forma, caja(frente, 0, 0, 2, 16));
		if (col == 2) forma = Shapes.or(forma, caja(frente, 14, 0, 16, 16));
		if (fila == 0) forma = Shapes.or(forma, caja(frente, 0, 0, 16, 2));
		if (fila == 2) forma = Shapes.or(forma, caja(frente, 0, 14, 16, 16));
		return forma;
	}

	// --- Dibujo: lo hace la entidad del bloque principal ---

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return estado.getValue(PARTE) == PRINCIPAL ? new PuertaBlockEntity(pos, estado) : null;
	}
}
