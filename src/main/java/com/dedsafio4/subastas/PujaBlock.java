package com.dedsafio4.subastas;

import com.dedsafio4.Dedsafio4;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloque de Puja: un bloque invisible donde flota, girando, lo que se está pujando, con la puja actual arriba y lo
 * menos que se puede pujar abajo. Con click derecho se puja lo justo (la puja actual más el mínimo). No se rompe en
 * supervivencia (como la barrera): se pone y se saca en creativo.
 */
public class PujaBlock extends Block implements EntityBlock {
	private static final VoxelShape FORMA = Block.box(3, 3, 3, 13, 13, 13);

	public PujaBlock(Properties propiedades) {
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
		return new PujaBlockEntity(pos, estado);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
		if (level.isClientSide || tipo != ModPuja.ENTIDAD) return null;
		return (BlockEntityTicker<T>) (BlockEntityTicker<PujaBlockEntity>) PujaBlockEntity::tick;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (jugador instanceof ServerPlayer servidor) Puja.pujar(servidor, Puja.datos(servidor.server).siguiente());
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
