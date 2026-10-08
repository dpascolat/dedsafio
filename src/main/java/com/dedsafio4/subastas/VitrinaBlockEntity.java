package com.dedsafio4.subastas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Lo que muestra una Vitrina: qué ítem, a cuánto y quién lo vende (se manda a los jugadores para dibujarlo). */
public class VitrinaBlockEntity extends BlockEntity {
	/** Cada cuánto se fija qué mostrar, y cada cuánto pasan las vitrinas a la venta siguiente (en ticks). */
	private static final int REVISAR = 40, CAMBIAR = 600;

	/** El número de esta vitrina (cada una tiene el suyo, así no muestran todas lo mismo). */
	private int numero = -1;
	private ItemStack item = ItemStack.EMPTY;
	private long precio;
	private String vendedor = "";

	public VitrinaBlockEntity(BlockPos pos, BlockState estado) {
		super(ModVitrina.ENTIDAD, pos, estado);
	}

	public ItemStack item() {
		return item;
	}

	public long precio() {
		return precio;
	}

	public String vendedor() {
		return vendedor;
	}

	static void tick(Level level, BlockPos pos, BlockState estado, VitrinaBlockEntity vitrina) {
		if (!(level instanceof ServerLevel mundo) || mundo.getGameTime() % REVISAR != 0) return;
		SubastasData data = Subastas.data(mundo.getServer());
		if (vitrina.numero < 0) {
			vitrina.numero = data.nuevoNumeroDeVitrina();
			vitrina.setChanged();
		}
		List<SubastasData.Venta> ventas = data.ventas();
		ItemStack nuevo = ItemStack.EMPTY;
		long nuevoPrecio = 0;
		String nuevoVendedor = "";
		if (!ventas.isEmpty()) {
			long turno = mundo.getGameTime() / CAMBIAR;
			SubastasData.Venta venta = ventas.get((int) ((vitrina.numero + turno) % ventas.size()));
			nuevo = venta.item();
			nuevoPrecio = venta.precio();
			nuevoVendedor = venta.nombreVendedor();
		}
		if (!ItemStack.matches(nuevo, vitrina.item) || nuevoPrecio != vitrina.precio || !nuevoVendedor.equals(vitrina.vendedor)) {
			vitrina.item = nuevo.copy();
			vitrina.precio = nuevoPrecio;
			vitrina.vendedor = nuevoVendedor;
			vitrina.setChanged();
			mundo.sendBlockUpdated(pos, estado, estado, 3);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.saveAdditional(tag, registros);
		tag.putInt("numero", numero);
		if (!item.isEmpty()) tag.put("item", item.save(registros));
		tag.putLong("precio", precio);
		tag.putString("vendedor", vendedor);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.loadAdditional(tag, registros);
		numero = tag.contains("numero") ? tag.getInt("numero") : -1;
		item = tag.contains("item") ? ItemStack.parse(registros, tag.getCompound("item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		precio = tag.getLong("precio");
		vendedor = tag.getString("vendedor");
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
