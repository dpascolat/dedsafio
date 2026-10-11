package com.dedsafio4.client;

/** Lo que sabe el cliente del alma del jugador (lo manda el servidor con Almas.Payload). */
public final class AlmaCliente {
	private AlmaCliente() {}

	/** Todos empiezan con alma. */
	public static boolean tieneAlma = true;

	/** Los jugadores (conectados) que no tienen alma: para el ícono arriba de la cabeza. */
	public static final java.util.Set<java.util.UUID> SIN_ALMA = java.util.concurrent.ConcurrentHashMap.newKeySet();
}
