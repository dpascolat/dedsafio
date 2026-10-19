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

/** El Bloque de Puja (ver Puja). */
public final class ModPuja {
	private ModPuja() {}

	public static final Block BLOQUE = Registry.register(BuiltInRegistries.BLOCK, id("bloque_puja"),
			new PujaBlock(BlockBehaviour.Properties.of().strength(-1f, 3_600_000f).noLootTable().noOcclusion()
					.noCollission().sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("bloque_puja"),
			new BlockItem(BLOQUE, new Item.Properties()));
	public static final BlockEntityType<PujaBlockEntity> ENTIDAD = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("bloque_puja"), FabricBlockEntityTypeBuilder.create(PujaBlockEntity::new, BLOQUE).build());

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Sólo carga la clase (registra lo de arriba). */
	public static void registrar() {}
}
