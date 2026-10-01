package com.dedsafio4.cofres;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * El cofre nuevo: ocupa dos bloques (como un cofre doble). La mitad principal guarda las cosas
 * (54 espacios) y dibuja el modelo entero; la otra mitad sólo ocupa el lugar y reenvía los clicks.
 */
public class CofreBlock extends BaseEntityBlock {
	public static final MapCodec<CofreBlock> CODEC = simpleCodec(CofreBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Parte> PARTE = EnumProperty.create("parte", Parte.class);

	public enum Parte implements StringRepresentable {
		PRINCIPAL("principal"), SECUNDARIA("secundaria");

		private final String nombre;

		Parte(String nombre) {
			this.nombre = nombre;
		}

		@Override
		public String getSerializedName() {
			return nombre;
		}
	}

	public CofreBlock(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH).setValue(PARTE, Parte.PRINCIPAL));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(FACING, PARTE);
	}

	/** Hacia dónde está la otra mitad (a la derecha, mirando el frente del cofre). */
	public static Direction haciaLaOtra(BlockState estado) {
		Direction derecha = estado.getValue(FACING).getClockWise();
		return estado.getValue(PARTE) == Parte.PRINCIPAL ? derecha : derecha.getOpposite();
	}

	public static BlockPos principal(BlockState estado, BlockPos pos) {
		return estado.getValue(PARTE) == Parte.PRINCIPAL ? pos : pos.relative(haciaLaOtra(estado));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		Direction frente = contexto.getHorizontalDirection().getOpposite();
		BlockPos otra = contexto.getClickedPos().relative(frente.getClockWise());
		if (!contexto.getLevel().getBlockState(otra).canBeReplaced(contexto)) return null;
		return this.defaultBlockState().setValue(FACING, frente).setValue(PARTE, Parte.PRINCIPAL);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, @Nullable LivingEntity quien, ItemStack pila) {
		super.setPlacedBy(level, pos, estado, quien, pila);
		if (level.isClientSide) return;
		BlockPos otra = pos.relative(haciaLaOtra(estado));
		level.setBlock(otra, estado.setValue(PARTE, Parte.SECUNDARIA), 3);
	}

	/** Si se rompe una mitad, se va la otra; las cosas se tiran desde la principal. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
		BlockPos otra = pos.relative(haciaLaOtra(estado));
		BlockState estadoOtra = level.getBlockState(otra);
		if (estadoOtra.is(this) && estadoOtra.getValue(PARTE) != estado.getValue(PARTE)) {
			// El cofre lo suelta sólo la mitad principal: si rompieron la otra, se tira a mano.
			if (!level.isClientSide && estado.getValue(PARTE) == Parte.SECUNDARIA
					&& !jugador.isCreative() && jugador.hasCorrectToolForDrops(estado)) {
				Block.popResource(level, pos, new ItemStack(this.asItem()));
			}
			level.setBlock(otra, Blocks.AIR.defaultBlockState(), 35);
			level.levelEvent(jugador, 2001, otra, Block.getId(estadoOtra));
		}
		return super.playerWillDestroy(level, pos, estado, jugador);
	}

	@Override
	protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
		if (!estado.is(nuevo.getBlock()) && level.getBlockEntity(pos) instanceof CofreBlockEntity cofre) {
			Containers.dropContents(level, pos, cofre);
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(estado, level, pos, nuevo, movido);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(principal(estado, pos)) instanceof CofreBlockEntity cofre) {
			jugador.openMenu(cofre);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return estado.getValue(PARTE) == Parte.PRINCIPAL ? new CofreBlockEntity(tipoEntidad(), pos, estado) : null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
		return level.isClientSide ? createTickerHelper(tipo, tipoEntidad(), CofreBlockEntity::tickCliente) : null;
	}

	/** El tipo de inventario de este cofre (el Cofre de Huesos usa otro, con su propio dibujo). */
	protected BlockEntityType<CofreBlockEntity> tipoEntidad() {
		return ModCofres.COFRE_ENTIDAD;
	}

	/** Los sonidos al abrir y cerrar. */
	public net.minecraft.sounds.SoundEvent sonidoAbrir() {
		return ModCofres.SONIDO_ABRIR;
	}

	public net.minecraft.sounds.SoundEvent sonidoCerrar() {
		return ModCofres.SONIDO_CERRAR;
	}

	@Override
	protected void tick(BlockState estado, ServerLevel level, BlockPos pos, RandomSource azar) {
		if (level.getBlockEntity(pos) instanceof CofreBlockEntity cofre) cofre.revisarQuienMira();
	}

	@Override
	protected boolean triggerEvent(BlockState estado, Level level, BlockPos pos, int id, int dato) {
		super.triggerEvent(estado, level, pos, id, dato);
		BlockEntity entidad = level.getBlockEntity(pos);
		return entidad != null && entidad.triggerEvent(id, dato);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState estado) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState estado, Level level, BlockPos pos) {
		return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(
				level.getBlockEntity(principal(estado, pos)));
	}

	/** Cuánto se ve abierto (0 cerrado, 1 abierto): lo usa el dibujo. */
	public static float apertura(BlockGetter level, BlockPos pos, float parcial) {
		return level.getBlockEntity(pos) instanceof CofreBlockEntity cofre ? cofre.getOpenNess(parcial) : 0f;
	}
}
