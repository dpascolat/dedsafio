package com.dedsafio4.misiones;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Misiones secundarias que completó cada jugador, guardadas en el mundo. */
public class MisionesData extends SavedData {
	public static final SavedData.Factory<MisionesData> FACTORY =
			new SavedData.Factory<>(MisionesData::new, MisionesData::cargar, null);

	private final Map<UUID, Set<String>> completadas = new HashMap<>();

	/** Marca la misión como completada. Devuelve false si el jugador ya la tenía. */
	public boolean completar(UUID jugador, String mision) {
		boolean nueva = completadas.computeIfAbsent(jugador, k -> new HashSet<>()).add(mision);
		if (nueva) setDirty();
		return nueva;
	}

	public boolean tiene(UUID jugador, String mision) {
		Set<String> s = completadas.get(jugador);
		return s != null && s.contains(mision);
	}

	private static MisionesData cargar(CompoundTag tag, HolderLookup.Provider registries) {
		MisionesData data = new MisionesData();
		CompoundTag jugadores = tag.getCompound("completadas");
		for (String clave : jugadores.getAllKeys()) {
			Set<String> misiones = new HashSet<>();
			for (Tag t : jugadores.getList(clave, Tag.TAG_STRING)) misiones.add(t.getAsString());
			data.completadas.put(UUID.fromString(clave), misiones);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag jugadores = new CompoundTag();
		completadas.forEach((uuid, misiones) -> {
			ListTag lista = new ListTag();
			misiones.forEach(m -> lista.add(StringTag.valueOf(m)));
			jugadores.put(uuid.toString(), lista);
		});
		tag.put("completadas", jugadores);
		return tag;
	}
}
