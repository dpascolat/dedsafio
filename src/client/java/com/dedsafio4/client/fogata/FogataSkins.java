package com.dedsafio4.client.fogata;

import com.dedsafio4.revivir.Fogata;
import com.dedsafio4.revivir.FogataEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Las skins de los baneados: la lista del menú de la fogata y la del que está reviviendo. */
public final class FogataSkins {
	private FogataSkins() {}

	/** Un baneado del menú. */
	public static final class Muerto {
		public final UUID id;
		public final String nombre;
		public final Component texto;
		private CompletableFuture<PlayerSkin> skin;

		Muerto(UUID id, String nombre) {
			this.id = id;
			this.nombre = nombre;
			texto = Component.literal(nombre);
		}

		public PlayerSkin skin() {
			Minecraft mc = Minecraft.getInstance();
			if (skin == null) {
				skin = CompletableFuture.supplyAsync(() -> {
					var resultado = mc.getMinecraftSessionService().fetchProfile(id, false);
					return resultado == null ? new GameProfile(id, nombre) : resultado.profile();
				}, Util.backgroundExecutor()).thenCompose(perfil -> mc.getSkinManager().getOrLoad(perfil));
			}
			return skin.getNow(DefaultPlayerSkin.get(id));
		}
	}

	public static final List<Muerto> LISTA = new ArrayList<>();

	private static final Map<String, CompletableFuture<PlayerSkin>> RITUALES = new HashMap<>();

	public static void cargar(Fogata.ListaPayload lista) {
		LISTA.clear();
		for (int i = 0; i < lista.ids().size(); i++) LISTA.add(new Muerto(UUID.fromString(lista.ids().get(i)), lista.nombres().get(i)));
	}

	/** La skin del que revive esa fogata. */
	static PlayerSkin skin(FogataEntity fogata) {
		UUID id = fogata.muerto().isEmpty() ? fogata.getUUID() : UUID.fromString(fogata.muerto());
		String textura = fogata.textura();
		PlayerSkin comun = DefaultPlayerSkin.get(id);
		if (textura.isEmpty()) return comun;
		return RITUALES.computeIfAbsent(textura, t -> {
			GameProfile perfil = new GameProfile(id, fogata.nombre());
			perfil.getProperties().put("textures", new Property("textures", t, fogata.firma().isEmpty() ? null : fogata.firma()));
			return Minecraft.getInstance().getSkinManager().getOrLoad(perfil);
		}).getNow(comun);
	}

	public static void limpiar() {
		LISTA.clear();
		RITUALES.clear();
	}
}
