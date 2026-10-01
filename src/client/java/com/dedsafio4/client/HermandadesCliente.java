package com.dedsafio4.client;

import com.dedsafio4.hermandad.HermandadChatPayload;
import com.dedsafio4.hermandad.HermandadesJugadoresPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lo que sabe el cliente de a qué Hermandad pertenece cada jugador (lo manda el servidor). */
public final class HermandadesCliente {
	private HermandadesCliente() {}

	private static final Map<UUID, HermandadesJugadoresPayload.Entrada> POR_JUGADOR = new HashMap<>();

	/** El estandarte activo de cada Hermandad (nombre → estandarte). */
	private static final Map<String, net.minecraft.world.item.ItemStack> ACTIVOS = new HashMap<>();

	public static void actualizar(HermandadesJugadoresPayload payload) {
		POR_JUGADOR.clear();
		for (HermandadesJugadoresPayload.Entrada e : payload.entradas()) POR_JUGADOR.put(e.jugador(), e);
		ACTIVOS.clear();
		for (HermandadesJugadoresPayload.Activo a : payload.activos()) ACTIVOS.put(a.hermandad(), a.estandarte());
	}

	/** El estandarte que lleva de capa un jugador (vacío si su Hermandad no eligió ninguno). */
	public static net.minecraft.world.item.ItemStack estandarteDe(UUID jugador) {
		HermandadesJugadoresPayload.Entrada e = POR_JUGADOR.get(jugador);
		return e == null ? net.minecraft.world.item.ItemStack.EMPTY : estandarteDeHermandad(e.hermandad());
	}

	public static net.minecraft.world.item.ItemStack estandarteDeHermandad(String hermandad) {
		return ACTIVOS.getOrDefault(hermandad, net.minecraft.world.item.ItemStack.EMPTY);
	}

	/** Los anuncios del Tablón de mi Hermandad (el más nuevo primero). */
	private static List<com.dedsafio4.hermandad.HermandadTablonPayload.Anuncio> anuncios = List.of();

	public static void actualizarTablon(com.dedsafio4.hermandad.HermandadTablonPayload payload) {
		anuncios = List.copyOf(payload.anuncios());
	}

	public static List<com.dedsafio4.hermandad.HermandadTablonPayload.Anuncio> anuncios() {
		return anuncios;
	}

	/** La Colección de Estandartes de mi Hermandad (pestaña Estandartes) y cuál está activo. */
	private static List<net.minecraft.world.item.ItemStack> coleccion = List.of();
	private static int activo = -1;

	public static void actualizarEstandartes(com.dedsafio4.hermandad.HermandadEstandartesPayload payload) {
		coleccion = List.copyOf(payload.coleccion());
		activo = payload.activo();
	}

	public static List<net.minecraft.world.item.ItemStack> coleccion() {
		return coleccion;
	}

	public static int activo() {
		return activo;
	}

	/** Todas las Hermandades que existen (nombre → color), en orden alfabético. */
	public static Map<String, Integer> todas() {
		Map<String, Integer> lista = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (HermandadesJugadoresPayload.Entrada e : POR_JUGADOR.values()) lista.put(e.hermandad(), e.color());
		return lista;
	}

	/** El chat de mi Hermandad (lo manda el servidor solo a los miembros). */
	private static List<HermandadChatPayload.Linea> chat = List.of();

	public static void actualizarChat(HermandadChatPayload payload) {
		chat = List.copyOf(payload.lineas());
	}

	public static List<HermandadChatPayload.Linea> chat() {
		return chat;
	}

	public static void limpiar() {
		POR_JUGADOR.clear();
		ACTIVOS.clear();
		chat = List.of();
		coleccion = List.of();
		activo = -1;
		anuncios = List.of();
	}

	/** null si el jugador no está en ninguna Hermandad. */
	public static HermandadesJugadoresPayload.Entrada de(UUID jugador) {
		return POR_JUGADOR.get(jugador);
	}
}
