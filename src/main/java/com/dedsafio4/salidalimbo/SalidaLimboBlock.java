package com.dedsafio4.salidalimbo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/** Salida del Limbo (ver SalidaLimbo): el ojo del bloque queda mirando al que lo puso, y ve en esa línea. */
public class SalidaLimboBlock extends Block implements EntityBlock {
	public static final DirectionProperty MIRA = HorizontalDirectionalBlock.FACING;

	public SalidaLimboBlock(Properties propiedades) {
		super(propiedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(MIRA, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(MIRA);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		return this.defaultBlockState().setValue(MIRA, contexto.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new SalidaLimboBlockEntity(pos, estado);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level mundo, BlockState estado, BlockEntityType<T> tipo) {
		if (mundo.isClientSide || tipo != SalidaLimbo.ENTIDAD) return null;
		return (nivel, pos, est, entidad) -> SalidaLimbo.mirar((ServerLevel) nivel, pos, est.getValue(MIRA));
	}
}
