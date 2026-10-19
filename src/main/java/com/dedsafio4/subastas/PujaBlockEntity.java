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

/** Lo que muestra el Bloque de Puja: qué se puja, cuánto va y lo menos que se puede pujar (se manda a los jugadores). */
public class PujaBlockEntity extends BlockEntity {
	private ItemStack item = ItemStack.EMPTY;
	private long actual, siguiente;

	public PujaBlockEntity(BlockPos pos, BlockState estado) {
		super(ModPuja.ENTIDAD, pos, estado);
	}

	public ItemStack item() {
		return item;
	}

	public long actual() {
		return actual;
	}

	public long siguiente() {
		return siguiente;
	}

	static void tick(Level level, BlockPos pos, BlockState estado, PujaBlockEntity bloque) {
		if (!(level instanceof ServerLevel mundo) || mundo.getGameTime() % 5 != 0) return;
		Puja.Datos d = Puja.datos(mundo.getServer());
		ItemStack nuevo = d.activa ? d.item : ItemStack.EMPTY;
		long nuevoActual = d.activa ? d.actual() : 0, nuevoSiguiente = d.activa ? d.siguiente() : 0;
		if (!ItemStack.matches(nuevo, bloque.item) || nuevoActual != bloque.actual || nuevoSiguiente != bloque.siguiente) {
			bloque.item = nuevo.copy();
			bloque.actual = nuevoActual;
			bloque.siguiente = nuevoSiguiente;
			bloque.setChanged();
			mundo.sendBlockUpdated(pos, estado, estado, 3);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.saveAdditional(tag, registros);
		if (!item.isEmpty()) tag.put("item", item.save(registros));
		tag.putLong("actual", actual);
		tag.putLong("siguiente", siguiente);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.loadAdditional(tag, registros);
		item = tag.contains("item") ? ItemStack.parse(registros, tag.getCompound("item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		actual = tag.getLong("actual");
		siguiente = tag.getLong("siguiente");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registros) {
		return saveWithoutMetadata(registros);
	}
}
