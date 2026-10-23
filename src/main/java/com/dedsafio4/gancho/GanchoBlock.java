package com.dedsafio4.gancho;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Gancho de Huesos: cuelga del techo (sólo se pone debajo de un bloque y se cae si le sacan el de arriba). No
 * choca: se pasa a través. Guarda su impulso (ver GanchoBlockEntity) y lo devuelve al romperlo.
 */
public class GanchoBlock extends Block implements EntityBlock {
	public static final DirectionProperty MIRA = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape FORMA = Block.box(4, 0, 4, 12, 16, 12);

	public GanchoBlock(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(MIRA, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(MIRA);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		BlockState estado = this.defaultBlockState().setValue(MIRA, contexto.getHorizontalDirection().getOpposite());
		return estado.canSurvive(contexto.getLevel(), contexto.getClickedPos()) ? estado : null;
	}

	@Override
	protected BlockState updateShape(BlockState estado, Direction lado, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos posVecino) {
		if (lado == Direction.UP && !estado.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(estado, lado, vecino, level, pos, posVecino);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new GanchoBlockEntity(pos, estado);
	}

	/** Al ponerlo, se queda con el impulso que tenía el ítem. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, @Nullable LivingEntity quien, ItemStack pila) {
		if (level.getBlockEntity(pos) instanceof GanchoBlockEntity gancho) gancho.setDistancia(GanchoItem.distancia(pila));
	}

	/** Al romperlo (en supervivencia) suelta el gancho con su mismo impulso. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
		if (!level.isClientSide && !jugador.isCreative() && level.getBlockEntity(pos) instanceof GanchoBlockEntity gancho) {
			Block.popResource(level, pos, GanchoItem.con(gancho.distancia()));
		}
		return super.playerWillDestroy(level, pos, estado, jugador);
	}
}
