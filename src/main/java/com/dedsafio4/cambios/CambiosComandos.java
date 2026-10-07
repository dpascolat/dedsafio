package com.dedsafio4.cambios;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * /cambio                        lista los cambios activos
 * /cambio <cambio...> <nivel>    activa un cambio y lo anuncia a todos (nivel 0 lo desactiva)
 */
public final class CambiosComandos {
	private CambiosComandos() {}

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO_CAMBIO = String.valueOf((char) 0xE001);
	public static final int COLOR_TITULO = 0xFFA526;
	public static final int COLOR_TEXTO = 0xFFD89A;

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("cambio").requires(s -> s.hasPermission(2))
				.executes(CambiosComandos::listar);

		for (Cambios.Cambio cambio : Cambios.TODOS) {
			raiz.then(rama(cambio, cambio.comando()));
		}
		dispatcher.register(raiz);
	}

	/** "respirar agua" -> literal("respirar").then(literal("agua").then(<nivel>)) */
	private static ArgumentBuilder<CommandSourceStack, ?> rama(Cambios.Cambio cambio, String comando) {
		String[] palabras = comando.split(" ");
		ArgumentBuilder<CommandSourceStack, ?> rama = Commands.argument("nivel", IntegerArgumentType.integer(0))
				.executes(ctx -> activar(ctx, cambio, IntegerArgumentType.getInteger(ctx, "nivel")));
		for (int i = palabras.length - 1; i >= 0; i--) {
			rama = Commands.literal(palabras[i]).then(rama);
		}
		return rama;
	}

	private static int listar(CommandContext<CommandSourceStack> ctx) {
		Map<String, Integer> activos = Cambios.data(ctx.getSource().getServer()).activos();
		String texto = activos.isEmpty() ? "No hay cambios activos"
				: "Cambios activos: " + Cambios.TODOS.stream()
						.filter(c -> activos.containsKey(c.id()))
						.map(c -> c.comando() + " " + activos.get(c.id()))
						.reduce((a, b) -> a + ", " + b).orElse("");
		ctx.getSource().sendSuccess(() -> Component.literal(texto), false);
		return activos.size();
	}

	private static int activar(CommandContext<CommandSourceStack> ctx, Cambios.Cambio cambio, int nivel) {
		if (nivel == 0) {
			activar(ctx.getSource().getServer(), cambio, 0);
			ctx.getSource().sendSuccess(() -> Component.literal("Cambio " + cambio.comando() + " desactivado"), true);
			return 1;
		}
		activar(ctx.getSource().getServer(), cambio, nivel);
		return 1;
	}

	/** Activa el cambio y lo anuncia en el chat. Devuelve el texto del anuncio. */
	public static String activar(net.minecraft.server.MinecraftServer server, Cambios.Cambio cambio, int nivel) {
		Cambios.data(server).setNivel(cambio.id(), nivel);
		CambiosEventos.alActivar(server, cambio.id(), nivel);
		if (nivel == 0) return "";
		String anuncio = cambio.anuncios().getOrDefault(nivel, "Se activó " + cambio.comando() + " " + nivel + ".");
		Component mensaje = Component.empty()
				.append(Component.literal(ICONO_CAMBIO).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS)))
				.append(Component.literal(" Cambio de Dificultad:")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_TITULO)).withBold(true)))
				.append(Component.literal("\n" + anuncio)
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_TEXTO))));
		server.getPlayerList().broadcastSystemMessage(mensaje, false);
		return anuncio;
	}
}
