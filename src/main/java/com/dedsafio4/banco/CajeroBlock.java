package com.dedsafio4.banco;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

/**
 * Cajero: con click derecho se abre el menú para ingresar (o sacar) deditas. Es el modelo de Patricio (Cajero.bbmodel):
 * mide dos bloques de alto, así que son dos mitades como una puerta (la de abajo dibuja todo el cajero; la de arriba
 * es invisible y sólo ocupa el lugar). Romper cualquiera rompe las dos.
 */
public class CajeroBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<CajeroBlock> CODEC = simpleCodec(CajeroBlock::new);
	public static final EnumProperty<DoubleBlockHalf> MITAD = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/**
	 * El cajero ocupa de 2 a 14 de ancho y está corrido 3 píxeles hacia atrás (de 0 a 10,5 de fondo, con el frente
	 * mirando a FACING); arriba llega a 31. Una forma por dirección.
	 */
	private static final java.util.Map<Direction, VoxelShape> FORMA_ABAJO = formas(16), FORMA_ARRIBA = formas(15);

	private static java.util.Map<Direction, VoxelShape> formas(double alto) {
		return java.util.Map.of(
				Direction.SOUTH, Block.box(2, 0, 0, 14, alto, 10.5),
				Direction.NORTH, Block.box(2, 0, 5.5, 14, alto, 16),
				Direction.EAST, Block.box(0, 0, 2, 10.5, alto, 14),
				Direction.WEST, Block.box(5.5, 0, 2, 16, alto, 14));
	}

	/** Abre la pantalla del Cajero (la pone el cliente al iniciar). */
	public static Consumer<BlockPos> abrirPantalla = pos -> {};

	public CajeroBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MITAD, DoubleBlockHalf.LOWER));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MITAD);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		BlockPos pos = contexto.getClickedPos();
		Level level = contexto.getLevel();
		// Hace falta lugar arriba para la otra mitad.
		if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(contexto)) return null;
		return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite()).setValue(MITAD, DoubleBlockHalf.LOWER);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
		level.setBlock(pos.above(), estado.setValue(MITAD, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return (estado.getValue(MITAD) == DoubleBlockHalf.LOWER ? FORMA_ABAJO : FORMA_ARRIBA).get(estado.getValue(FACING));
	}

	/** Si falta la otra mitad, ésta también se va. */
	@Override
	protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos vecinoPos) {
		DoubleBlockHalf mitad = estado.getValue(MITAD);
		if (direccion.getAxis() == Direction.Axis.Y && (mitad == DoubleBlockHalf.LOWER) == (direccion == Direction.UP)) {
			return vecino.is(this) && vecino.getValue(MITAD) != mitad ? estado : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(estado, direccion, vecino, level, pos, vecinoPos);
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		if (estado.getValue(MITAD) == DoubleBlockHalf.LOWER) return true;
		BlockState abajo = level.getBlockState(pos.below());
		return abajo.is(this) && abajo.getValue(MITAD) == DoubleBlockHalf.LOWER;
	}

	/** Al romper la mitad de arriba (en creativo o sin la herramienta justa) la de abajo no suelta el cajero. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
		if (!level.isClientSide && estado.getValue(MITAD) == DoubleBlockHalf.UPPER
				&& (jugador.isCreative() || !jugador.hasCorrectToolForDrops(estado))) {
			BlockPos abajo = pos.below();
			BlockState estadoAbajo = level.getBlockState(abajo);
			if (estadoAbajo.is(this) && estadoAbajo.getValue(MITAD) == DoubleBlockHalf.LOWER) {
				level.setBlock(abajo, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(jugador, 2001, abajo, Block.getId(estadoAbajo));
			}
		}
		return super.playerWillDestroy(level, pos, estado, jugador);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (level.isClientSide) abrirPantalla.accept(pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
