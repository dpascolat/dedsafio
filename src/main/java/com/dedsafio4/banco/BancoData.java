package com.dedsafio4.banco;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saldo de deditas de cada jugador, guardado en el mundo. */
public class BancoData extends SavedData {
	public static final SavedData.Factory<BancoData> FACTORY =
			new SavedData.Factory<>(BancoData::new, BancoData::cargar, null);

	private final Map<UUID, Long> saldos = new HashMap<>();

	public long saldo(UUID jugador) {
		return saldos.getOrDefault(jugador, 0L);
	}

	public void setSaldo(UUID jugador, long cantidad) {
		saldos.put(jugador, Math.max(0, cantidad));
		setDirty();
	}

	private static BancoData cargar(CompoundTag tag, HolderLookup.Provider registries) {
		BancoData data = new BancoData();
		CompoundTag saldosTag = tag.getCompound("saldos");
		for (String clave : saldosTag.getAllKeys()) {
			data.saldos.put(UUID.fromString(clave), saldosTag.getLong(clave));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag saldosTag = new CompoundTag();
		saldos.forEach((uuid, cantidad) -> saldosTag.putLong(uuid.toString(), cantidad));
		tag.put("saldos", saldosTag);
		return tag;
	}
}
