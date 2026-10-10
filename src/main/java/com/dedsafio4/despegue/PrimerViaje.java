package com.dedsafio4.despegue;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El primer viaje en nave de cada jugador: cuando despega una nave en la que alguno viaja por primera vez, sale en
 * el chat "X ha despegado en una nave junto a Y." (los jugadores que ya viajaron quedan guardados en el mundo).
 */
public final class PrimerViaje {
	private PrimerViaje() {}

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath("dedsafio4", "iconos");
	private static final String ICONO = String.valueOf((char) 0xE003);
	private static final int ROJO = 0xFF4040, BLANCO = 0xFFFFFF;

	private static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		final Set<String> viajaron = new HashSet<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("viajaron", Tag.TAG_STRING)) d.viajaron.add(t.getAsString());
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag lista = new ListTag();
			for (String s : viajaron) lista.add(StringTag.valueOf(s));
			tag.put("viajaron", lista);
			return tag;
		}
	}

	/** Al despegar: si alguno de los que van en la nave viaja por primera vez, el anuncio en el chat. */
	public static void alDespegar(MinecraftServer server, List<Player> viajeros) {
		if (viajeros.isEmpty()) return;
		Datos d = server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_primer_viaje");
		boolean alguno = false;
		for (Player p : viajeros) alguno |= d.viajaron.add(p.getUUID().toString());
		if (!alguno) return;
		d.setDirty();
		MutableComponent mensaje = Component.empty()
				.append(Component.literal(ICONO).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS)))
				.append(Component.literal(" ").withStyle(Style.EMPTY.withBold(true)))
				.append(nombre(viajeros.get(0)))
				.append(texto(" ha despegado en una nave"));
		if (viajeros.size() > 1) mensaje.append(texto(" junto a ")).append(nombre(viajeros.get(1)));
		mensaje.append(texto("."));
		server.getPlayerList().broadcastSystemMessage(mensaje, false);
	}

	private static Component nombre(Player p) {
		return Component.literal(p.getGameProfile().getName()).withStyle(Style.EMPTY.withColor(BLANCO).withBold(true));
	}

	private static Component texto(String s) {
		return Component.literal(s).withStyle(Style.EMPTY.withColor(ROJO).withBold(true));
	}
}
