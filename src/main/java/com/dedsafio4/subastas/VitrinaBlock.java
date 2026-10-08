package com.dedsafio4.subastas;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vitrina del /ah: un bloque invisible donde flota, girando, una de las cosas que están a la venta (con su precio
 * arriba). Cada vitrina muestra una distinta y cada tanto pasan a la siguiente. Con clic derecho se abre el /ah.
 * No se rompe en supervivencia (como la barrera): se pone y se saca en creativo.
 */
public class VitrinaBlock extends Block implements EntityBlock {
	private static final VoxelShape FORMA = Block.box(3, 3, 3, 13, 13, 13);

	public VitrinaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new VitrinaBlockEntity(pos, estado);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
		if (level.isClientSide || tipo != ModVitrina.ENTIDAD) return null;
		return (BlockEntityTicker<T>) (BlockEntityTicker<VitrinaBlockEntity>) VitrinaBlockEntity::tick;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer servidor) Subastas.abrir(servidor, 0);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
