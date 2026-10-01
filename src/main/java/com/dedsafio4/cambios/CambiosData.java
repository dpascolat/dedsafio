package com.dedsafio4.cambios;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.TreeMap;

/** Nivel activo de cada cambio (por ejemplo "arboles" → 1), guardado en el mundo. */
public class CambiosData extends SavedData {
	public static final SavedData.Factory<CambiosData> FACTORY =
			new SavedData.Factory<>(CambiosData::new, CambiosData::cargar, null);

	private final Map<String, Integer> niveles = new TreeMap<>();

	public int nivel(String cambio) {
		return niveles.getOrDefault(cambio, 0);
	}

	public void setNivel(String cambio, int nivel) {
		if (nivel <= 0) niveles.remove(cambio);
		else niveles.put(cambio, nivel);
		setDirty();
	}

	public Map<String, Integer> activos() {
		return niveles;
	}

	private static CambiosData cargar(CompoundTag tag, HolderLookup.Provider registries) {
		CambiosData data = new CambiosData();
		CompoundTag nivelesTag = tag.getCompound("niveles");
		for (String clave : nivelesTag.getAllKeys()) {
			data.niveles.put(clave, nivelesTag.getInt(clave));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag nivelesTag = new CompoundTag();
		niveles.forEach(nivelesTag::putInt);
		tag.put("niveles", nivelesTag);
		return tag;
	}
}
