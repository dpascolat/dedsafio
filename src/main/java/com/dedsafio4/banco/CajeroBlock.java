package com.dedsafio4.banco;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

/** Cajero: con click derecho se abre el menú para ingresar (o sacar) deditas. */
public class CajeroBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<CajeroBlock> CODEC = simpleCodec(CajeroBlock::new);

	/** Abre la pantalla del Cajero (la pone el cliente al iniciar). */
	public static Consumer<BlockPos> abrirPantalla = pos -> {};

	public CajeroBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (level.isClientSide) abrirPantalla.accept(pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
