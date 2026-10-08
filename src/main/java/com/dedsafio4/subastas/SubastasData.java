package com.dedsafio4.subastas;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Las ventas del /ah, guardadas en el mundo. */
public class SubastasData extends SavedData {
	public static final SavedData.Factory<SubastasData> FACTORY = new SavedData.Factory<>(SubastasData::new, SubastasData::cargar, null);

	/** Una venta: qué se vende, quién lo vende y a cuántas deditas. */
	public record Venta(UUID id, UUID vendedor, String nombreVendedor, ItemStack item, long precio, long cuando) {}

	/** De la más nueva a la más vieja. */
	private final List<Venta> ventas = new ArrayList<>();
	/** El número que le toca a la próxima Vitrina que se pone. */
	private int siguienteVitrina;

	public int nuevoNumeroDeVitrina() {
		setDirty();
		return siguienteVitrina++;
	}

	public List<Venta> ventas() {
		return ventas;
	}

	public void agregar(Venta venta) {
		ventas.add(0, venta);
		setDirty();
	}

	public Venta buscar(UUID id) {
		for (Venta v : ventas) if (v.id().equals(id)) return v;
		return null;
	}

	public boolean quitar(Venta venta) {
		boolean estaba = ventas.remove(venta);
		if (estaba) setDirty();
		return estaba;
	}

	public long cuantasDe(UUID vendedor) {
		return ventas.stream().filter(v -> v.vendedor().equals(vendedor)).count();
	}

	private static SubastasData cargar(CompoundTag tag, HolderLookup.Provider registros) {
		SubastasData data = new SubastasData();
		data.siguienteVitrina = tag.getInt("siguienteVitrina");
		for (Tag t : tag.getList("ventas", Tag.TAG_COMPOUND)) {
			CompoundTag v = (CompoundTag) t;
			ItemStack item = ItemStack.parse(registros, v.getCompound("item")).orElse(ItemStack.EMPTY);
			if (item.isEmpty()) continue;
			data.ventas.add(new Venta(v.getUUID("id"), v.getUUID("vendedor"), v.getString("nombre"), item, v.getLong("precio"), v.getLong("cuando")));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
		ListTag lista = new ListTag();
		for (Venta v : ventas) {
			CompoundTag t = new CompoundTag();
			t.putUUID("id", v.id());
			t.putUUID("vendedor", v.vendedor());
			t.putString("nombre", v.nombreVendedor());
			t.put("item", v.item().save(registros));
			t.putLong("precio", v.precio());
			t.putLong("cuando", v.cuando());
			lista.add(t);
		}
		tag.put("ventas", lista);
		tag.putInt("siguienteVitrina", siguienteVitrina);
		return tag;
	}
}
