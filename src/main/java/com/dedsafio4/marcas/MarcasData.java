package com.dedsafio4.marcas;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Marcas de los admins, guardadas en el mundo. */
public class MarcasData extends SavedData {
	public static final SavedData.Factory<MarcasData> FACTORY =
			new SavedData.Factory<>(MarcasData::new, MarcasData::cargar, null);

	private final List<MarcasPayload.Marca> marcas = new ArrayList<>();

	public List<MarcasPayload.Marca> todas() {
		return marcas;
	}

	/** Agrega o reemplaza la marca con ese nombre. */
	public void poner(MarcasPayload.Marca marca) {
		marcas.removeIf(m -> m.nombre().equalsIgnoreCase(marca.nombre()));
		marcas.add(marca);
		setDirty();
	}

	public boolean borrar(String nombre) {
		boolean borrada = marcas.removeIf(m -> m.nombre().equalsIgnoreCase(nombre));
		if (borrada) setDirty();
		return borrada;
	}

	private static MarcasData cargar(CompoundTag tag, HolderLookup.Provider registries) {
		MarcasData data = new MarcasData();
		for (Tag t : tag.getList("marcas", Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag) t;
			data.marcas.add(new MarcasPayload.Marca(c.getString("nombre"), c.getString("dimension"),
					c.getInt("x"), c.getInt("y"), c.getInt("z"), c.getInt("color")));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag lista = new ListTag();
		for (MarcasPayload.Marca m : marcas) {
			CompoundTag c = new CompoundTag();
			c.putString("nombre", m.nombre());
			c.putString("dimension", m.dimension());
			c.putInt("x", m.x());
			c.putInt("y", m.y());
			c.putInt("z", m.z());
			c.putInt("color", m.color());
			lista.add(c);
		}
		tag.put("marcas", lista);
		return tag;
	}
}
