package com.dedsafio4.marcas;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Bloquea Xaero's Minimap. Por defecto a todos (también a los admins); con "/minimapa admins" los
 * admins lo pueden volver a usar, y con "/minimapa todos" se vuelve a sacar para todos.
 *
 * 1. Efectos de Xaero "no_minimap" y "no_waypoints", infinitos y ocultos. Se revisan cada
 *    cuarto de segundo, así no se pierden con leche ni al morir. Solo existen si Xaero está en el servidor
 *    (en un mundo de un jugador siempre).
 * 2. Por si no está: el código "nominimap" en un mensaje del sistema (con § antes de cada letra,
 *    así no se ve texto). Se manda un rato después de entrar, porque si llega apenas se conecta
 *    Xaero todavía no inició y lo ignora.
 * Además, del lado del cliente se bloquean las teclas y menús de Xaero (ver XaeroBloqueo).
 */
public final class BloqueoMinimapa {
	private BloqueoMinimapa() {}

	private static final List<ResourceLocation> EFECTOS = List.of(
			ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_minimap"),
			ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_waypoints"));

	private static String codigo(String palabra) {
		StringBuilder sb = new StringBuilder();
		for (char c : palabra.toCharArray()) sb.append('§').append(c);
		return sb.toString();
	}

	private static final String SIN_MINIMAPA = codigo("nominimap");
	private static final String RESTABLECER = codigo("resetxaero");
	/** Ticks después de entrar en los que se manda el código del chat (2 y 10 segundos). */
	private static final int PRIMER_ENVIO = 40, SEGUNDO_ENVIO = 200;

	/** Jugador → ticks desde que entró (solo hasta mandar el código del chat). */
	private static final Map<UUID, Integer> PENDIENTES = new HashMap<>();

	public static void alEntrar(ServerPlayer jugador) {
		PENDIENTES.put(jugador.getUUID(), 0);
		ServerPlayNetworking.send(jugador, new MinimapaPayload(data(jugador.server).adminsLoUsan()));
	}

	private static MinimapaData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(MinimapaData.FACTORY, "dedsafio4_minimapa");
	}

	/** Cambia si los admins lo pueden usar, y se lo avisa a todos al instante. */
	public static void setAdminsLoUsan(MinecraftServer server, boolean valor) {
		data(server).setAdminsLoUsan(valor);
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(jugador, new MinimapaPayload(valor));
			enviarCodigo(jugador);
			aplicarEfectos(jugador);
		}
	}

	/** /minimapa: ver cómo está; /minimapa todos: nadie lo usa; /minimapa admins: los admins sí. */
	public static void registrarComando(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("minimapa").requires(s -> s.hasPermission(2))
				.executes(ctx -> {
					boolean admins = data(ctx.getSource().getServer()).adminsLoUsan();
					ctx.getSource().sendSuccess(() -> Component.literal(admins
							? "El minimapa está sacado para los jugadores; los admins lo pueden usar."
							: "El minimapa está sacado para todos (también los admins)."), false);
					return 1;
				})
				.then(net.minecraft.commands.Commands.literal("todos").executes(ctx -> {
					setAdminsLoUsan(ctx.getSource().getServer(), false);
					ctx.getSource().sendSuccess(() -> Component.literal("Minimapa sacado para todos (también los admins)."), true);
					return 1;
				}))
				.then(net.minecraft.commands.Commands.literal("admins").executes(ctx -> {
					setAdminsLoUsan(ctx.getSource().getServer(), true);
					ctx.getSource().sendSuccess(() -> Component.literal("Ahora los admins pueden usar el minimapa (los demás no)."), true);
					return 1;
				})));
	}

	public static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Integer>> it = PENDIENTES.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> e = it.next();
			int ticks = e.getValue() + 1;
			e.setValue(ticks);
			ServerPlayer jugador = server.getPlayerList().getPlayer(e.getKey());
			if (jugador == null) {
				it.remove();
				continue;
			}
			if (ticks == PRIMER_ENVIO || ticks == SEGUNDO_ENVIO) enviarCodigo(jugador);
			if (ticks >= SEGUNDO_ENVIO) it.remove();
		}

		// Seguido, así no se ve el minimapa si alguien se saca los efectos (leche, /effect clear, un tótem).
		if (server.getTickCount() % 5 == 0) {
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) aplicarEfectos(jugador);
		}
	}

	/** Lo puede usar solo un admin, y solo si está permitido para los admins. */
	private static boolean esAdmin(ServerPlayer jugador) {
		return jugador.hasPermissions(2) && data(jugador.server).adminsLoUsan();
	}

	private static void enviarCodigo(ServerPlayer jugador) {
		jugador.sendSystemMessage(Component.literal(RESTABLECER + (esAdmin(jugador) ? "" : SIN_MINIMAPA)));
	}

	/** Pone (o saca) los efectos que bloquean el minimapa. */
	public static void aplicarEfectos(ServerPlayer jugador) {
		boolean admin = esAdmin(jugador);
		for (ResourceLocation id : EFECTOS) {
			Optional<Holder.Reference<MobEffect>> efecto = BuiltInRegistries.MOB_EFFECT.getHolder(id);
			if (efecto.isEmpty()) continue;   // Xaero no está en el servidor
			if (admin) {
				jugador.removeEffect(efecto.get());
			} else if (!jugador.hasEffect(efecto.get())) {
				jugador.addEffect(new MobEffectInstance(efecto.get(), MobEffectInstance.INFINITE_DURATION, 0,
						true, false, false));
			}
		}
	}
}
