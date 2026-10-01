package com.dedsafio4.candados;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Los candados puestos en el mundo, guardados con la partida. */
public class CandadosData extends SavedData {
	public static final SavedData.Factory<CandadosData> FACTORY =
			new SavedData.Factory<>(CandadosData::new, CandadosData::cargar, null);

	/**
	 * @param codigo      el código que pidió el dueño
	 * @param duenio      quien puso el candado (abre sin que le pregunten)
	 * @param autorizados los que ya escribieron el código bien
	 */
	public record Cerradura(String codigo, UUID duenio, Set<UUID> autorizados) {}

	private final Map<BlockPos, Cerradura> cerraduras = new HashMap<>();

	public Cerradura cerradura(BlockPos pos) {
		return cerraduras.get(pos);
	}

	public void poner(BlockPos pos, String codigo, UUID duenio) {
		Set<UUID> autorizados = new HashSet<>();
		autorizados.add(duenio);
		cerraduras.put(pos.immutable(), new Cerradura(codigo, duenio, autorizados));
		setDirty();
	}

	public void sacar(BlockPos pos) {
		if (cerraduras.remove(pos) != null) setDirty();
	}

	/** Pasa una cerradura a otra posición (cuando se rompe la mitad de un cofre doble que la tenía). */
	public void mover(BlockPos pos, Cerradura cerradura) {
		cerraduras.put(pos.immutable(), cerradura);
		setDirty();
	}

	public void autorizar(BlockPos pos, UUID jugador) {
		Cerradura cerradura = cerraduras.get(pos);
		if (cerradura == null || !cerradura.autorizados().add(jugador)) return;
		setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
		ListTag lista = new ListTag();
		cerraduras.forEach((pos, cerradura) -> {
			CompoundTag entrada = new CompoundTag();
			entrada.putLong("pos", pos.asLong());
			entrada.putString("codigo", cerradura.codigo());
			entrada.putUUID("duenio", cerradura.duenio());
			ListTag autorizados = new ListTag();
			for (UUID uuid : cerradura.autorizados()) {
				CompoundTag u = new CompoundTag();
				u.putUUID("uuid", uuid);
				autorizados.add(u);
			}
			entrada.put("autorizados", autorizados);
			lista.add(entrada);
		});
		tag.put("cerraduras", lista);
		return tag;
	}

	private static CandadosData cargar(CompoundTag tag, HolderLookup.Provider registros) {
		CandadosData data = new CandadosData();
		ListTag lista = tag.getList("cerraduras", Tag.TAG_COMPOUND);
		for (int i = 0; i < lista.size(); i++) {
			CompoundTag entrada = lista.getCompound(i);
			Set<UUID> autorizados = new HashSet<>();
			ListTag guardados = entrada.getList("autorizados", Tag.TAG_COMPOUND);
			for (int j = 0; j < guardados.size(); j++) autorizados.add(guardados.getCompound(j).getUUID("uuid"));
			data.cerraduras.put(BlockPos.of(entrada.getLong("pos")),
					new Cerradura(entrada.getString("codigo"), entrada.getUUID("duenio"), autorizados));
		}
		return data;
	}

	/** Las posiciones con candado, para poder recorrerlas. */
	public List<BlockPos> posiciones() {
		return new ArrayList<>(cerraduras.keySet());
	}
}
