package com.dedsafio4.cambios;

import com.dedsafio4.aviso.Aviso;
import com.dedsafio4.ruleta.Ruleta;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /dificultadcambio <número>: todo junto. A todos les sale la ruleta de ese color y, cuando termina, se activa el
 * Cambio de Dificultad (como /cambio), con el anuncio en el chat y en el aviso de arriba a la izquierda.
 *   1.1  ruleta naranja → árboles 1 (las hojas no sueltan brotes)
 */
public final class DificultadCambio {
	private DificultadCambio() {}

	/** Qué hace cada número: el color de la ruleta, el cambio, su nivel y cuántos ticks dura la ruleta. */
	private record Paso(String ruleta, String cambio, int nivel, int ticksRuleta) {}

	private static final Map<String, Paso> PASOS = new LinkedHashMap<>();
	static {
		// La ruleta naranja dura 12,4 s (248 ticks) y el cliente tarda un poquito en cargarla.
		PASOS.put("1.1", new Paso("naranja", Cambios.ARBOLES, 1, 262));
	}

	private static Paso pendiente;
	private static long cuando = -1;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (pendiente == null || server.getTickCount() < cuando) return;
			Paso p = pendiente;
			pendiente = null;
			terminar(server, p);
		});
	}

	private static void terminar(MinecraftServer server, Paso p) {
		Cambios.Cambio cambio = Cambios.TODOS.stream().filter(c -> c.id().equals(p.cambio())).findFirst().orElse(null);
		if (cambio == null) return;
		String anuncio = CambiosComandos.activar(server, cambio, p.nivel());
		Aviso.mostrar(server, CambiosComandos.COLOR_TITULO, CambiosComandos.COLOR_TEXTO, "Cambio de Dificultad:", anuncio, "cambio");
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dificultadcambio").requires(s -> s.hasPermission(2))
				.then(Commands.argument("numero", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(PASOS.keySet(), b))
						.executes(c -> {
							String numero = StringArgumentType.getString(c, "numero");
							Paso p = PASOS.get(numero);
							if (p == null) {
								c.getSource().sendFailure(Component.literal("Todavía no existe el cambio de dificultad " + numero + "."));
								return 0;
							}
							MinecraftServer server = c.getSource().getServer();
							Ruleta.mostrarATodos(server, "ruleta_" + p.ruleta());
							pendiente = p;
							cuando = server.getTickCount() + p.ticksRuleta();
							c.getSource().sendSuccess(() -> Component.literal("Cambio de dificultad " + numero + ": ruleta " + p.ruleta() + ".")
									.withStyle(ChatFormatting.GOLD), true);
							return 1;
						})));
	}
}
