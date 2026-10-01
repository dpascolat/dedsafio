package com.dedsafio4.robots;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import com.dedsafio4.nave.ModEntidades;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * El Aldeano Robot dormido: plegado en un bloque, con la pantalla apagada y la cabeza caída.
 * Con la llave se despierta y vuelve a ser un Aldeano Robot.
 */
public class RobotDormidoBlock extends BaseEntityBlock {
	public static final MapCodec<RobotDormidoBlock> CODEC = simpleCodec(RobotDormidoBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape FORMA = Block.box(1, 0, 1, 15, 16, 15);

	public RobotDormidoBlock(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		return this.defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
	}

	/** Saca el bloque y deja en su lugar al Aldeano Robot despierto, mirando para el mismo lado. */
	public static void despertar(Level level, BlockPos pos) {
		BlockState estado = level.getBlockState(pos);
		if (!(level instanceof ServerLevel mundo) || !(estado.getBlock() instanceof RobotDormidoBlock)) return;
		AldeanoRobotEntity robot = ModEntidades.ALDEANO_ROBOT.create(mundo);
		if (robot == null) return;
		mundo.removeBlock(pos, false);
		float giro = estado.getValue(FACING).toYRot();
		robot.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, giro, 0);
		robot.setYHeadRot(giro);
		robot.setYBodyRot(giro);
		mundo.addFreshEntity(robot);
		mundo.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.6f);
		mundo.playSound(null, pos, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 0.8f, 1.2f);
		mundo.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
				25, 0.4, 0.5, 0.4, 0.1);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new RobotDormidoBlockEntity(pos, estado);
	}
}
