package com.dedsafio4.marcas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Marcas que ponen los admins y que ven todos los jugadores en el mundo (con nombre y distancia).
 * Reemplazan a los waypoints de Xaero's Minimap, que para los jugadores normales está bloqueado.
 *
 * /marca crear <nombre> [color]   en la posición del admin (o la de /execute positioned ...)
 * /marca borrar <nombre>
 * /marca lista
 */
public final class Marcas {
	private Marcas() {}

	public static final Map<String, Integer> COLORES = new LinkedHashMap<>();
	static {
		COLORES.put("naranja", 0xFFAA00);
		COLORES.put("rojo", 0xFF5555);
		COLORES.put("amarillo", 0xFFFF55);
		COLORES.put("verde", 0x55FF55);
		COLORES.put("celeste", 0x55FFFF);
		COLORES.put("azul", 0x5555FF);
		COLORES.put("violeta", 0xFF55FF);
		COLORES.put("blanco", 0xFFFFFF);
	}

	private static MarcasData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(MarcasData.FACTORY, "dedsafio4_marcas");
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("marca").requires(s -> s.hasPermission(2))
				.then(Commands.literal("crear")
						.then(Commands.argument("nombre", StringArgumentType.string())
								.executes(ctx -> crear(ctx, "naranja"))
								.then(Commands.argument("color", StringArgumentType.word())
										.suggests((ctx, b) -> SharedSuggestionProvider.suggest(COLORES.keySet(), b))
										.executes(ctx -> crear(ctx, StringArgumentType.getString(ctx, "color"))))))
				.then(Commands.literal("borrar")
						.then(Commands.argument("nombre", StringArgumentType.string())
								.suggests((ctx, b) -> SharedSuggestionProvider.suggest(
										data(ctx.getSource().getServer()).todas().stream().map(m -> "\"" + m.nombre() + "\""), b))
								.executes(ctx -> {
									String nombre = StringArgumentType.getString(ctx, "nombre");
									MinecraftServer server = ctx.getSource().getServer();
									if (!data(server).borrar(nombre)) {
										ctx.getSource().sendFailure(Component.literal("No hay ninguna marca llamada " + nombre + "."));
										return 0;
									}
									enviarATodos(server);
									ctx.getSource().sendSuccess(() -> Component.literal("Marca " + nombre + " borrada."), true);
									return 1;
								})))
				.then(Commands.literal("lista").executes(ctx -> {
					var marcas = data(ctx.getSource().getServer()).todas();
					String texto = marcas.isEmpty() ? "No hay marcas." : marcas.stream()
							.map(m -> m.nombre() + " (" + m.x() + ", " + m.y() + ", " + m.z() + ")")
							.collect(Collectors.joining("\n", "Marcas:\n", ""));
					ctx.getSource().sendSuccess(() -> Component.literal(texto), false);
					return marcas.size();
				})));
	}

	private static int crear(CommandContext<CommandSourceStack> ctx, String nombreColor) {
		CommandSourceStack fuente = ctx.getSource();
		Integer color = COLORES.get(nombreColor.toLowerCase());
		if (color == null) {
			fuente.sendFailure(Component.literal("Colores: " + String.join(", ", COLORES.keySet())));
			return 0;
		}
		// Posición del comando: la del admin, o la que se indique con /execute positioned ...
		BlockPos pos = BlockPos.containing(fuente.getPosition());
		String nombre = StringArgumentType.getString(ctx, "nombre");
		MarcasPayload.Marca marca = new MarcasPayload.Marca(nombre, fuente.getLevel().dimension().location().toString(),
				pos.getX(), pos.getY(), pos.getZ(), color);
		data(fuente.getServer()).poner(marca);
		enviarATodos(fuente.getServer());
		ctx.getSource().sendSuccess(() -> Component.literal("Marca " + nombre + " creada en "
				+ marca.x() + ", " + marca.y() + ", " + marca.z() + "."), true);
		return 1;
	}

	private static void enviarATodos(MinecraftServer server) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) sincronizar(jugador);
	}

	public static void sincronizar(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new MarcasPayload(data(jugador.server).todas()));
	}
}
