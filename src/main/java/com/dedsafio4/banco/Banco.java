package com.dedsafio4.banco;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Operaciones sobre las deditas. Cualquier cambio de saldo se envía al cliente. */
public final class Banco {
	private Banco() {}

	private static BancoData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(BancoData.FACTORY, "dedsafio4_banco");
	}

	public static long saldo(ServerPlayer jugador) {
		return data(jugador.server).saldo(jugador.getUUID());
	}

	public static void setSaldo(ServerPlayer jugador, long cantidad) {
		data(jugador.server).setSaldo(jugador.getUUID(), cantidad);
		sincronizar(jugador);
	}

	public static void sumar(ServerPlayer jugador, long cantidad) {
		setSaldo(jugador, saldo(jugador) + cantidad);
	}

	/** Suma deditas a cualquier jugador, aunque no esté conectado (si está, se le actualiza el saldo en pantalla). */
	public static void sumar(MinecraftServer server, java.util.UUID jugador, long cantidad) {
		BancoData d = data(server);
		d.setSaldo(jugador, d.saldo(jugador) + cantidad);
		ServerPlayer conectado = server.getPlayerList().getPlayer(jugador);
		if (conectado != null) sincronizar(conectado);
	}

	/** Descuenta si alcanza el saldo. Devuelve false si no tiene suficientes deditas. */
	public static boolean cobrar(ServerPlayer jugador, long cantidad) {
		long actual = saldo(jugador);
		if (actual < cantidad) return false;
		setSaldo(jugador, actual - cantidad);
		return true;
	}

	public static void sincronizar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new DeditasPayload(saldo(jugador)));
	}
}
