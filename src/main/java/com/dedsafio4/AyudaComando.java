package com.dedsafio4;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * /help <mensaje> → el mensaje les llega a los admins conectados como un susurro, y al que lo mandó le aparece
 * "Help request sent.". Reemplaza al /help de Minecraft (la lista de comandos).
 */
public final class AyudaComando {
	private AyudaComando() {}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		sacarComando(dispatcher.getRoot(), "help");
		dispatcher.register(Commands.literal("help")
				.executes(c -> {
					c.getSource().sendFailure(Component.literal("Escribí tu mensaje: /help <mensaje>"));
					return 0;
				})
				.then(Commands.argument("mensaje", StringArgumentType.greedyString())
						.executes(c -> pedirAyuda(c.getSource(), StringArgumentType.getString(c, "mensaje")))));
	}

	private static int pedirAyuda(CommandSourceStack fuente, String mensaje) {
		mensaje = mensaje.strip();
		if (mensaje.length() >= 2 && mensaje.startsWith("\"") && mensaje.endsWith("\"")) {
			mensaje = mensaje.substring(1, mensaje.length() - 1).strip();
		}
		ServerPlayer quien = fuente.getPlayer();
		Component texto = Component.literal(mensaje);
		// Como un /msg de Minecraft: "Fulano te susurra: ..." (gris e itálica).
		Component susurro = Component.translatable("commands.message.display.incoming", fuente.getDisplayName(), texto)
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
		for (ServerPlayer admin : fuente.getServer().getPlayerList().getPlayers()) {
			if (admin != quien && admin.hasPermissions(2)) admin.sendSystemMessage(susurro);
		}
		fuente.getServer().sendSystemMessage(Component.literal("[Ayuda] ").append(fuente.getDisplayName()).append(": " + mensaje));
		fuente.sendSystemMessage(Component.literal("Help request sent.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	/** Saca un comando que ya estaba registrado (acá: el /help de Minecraft). */
	@SuppressWarnings("unchecked")
	private static void sacarComando(CommandNode<CommandSourceStack> raiz, String nombre) {
		try {
			for (String campo : new String[]{"children", "literals"}) {
				Field f = CommandNode.class.getDeclaredField(campo);
				f.setAccessible(true);
				((Map<String, ?>) f.get(raiz)).remove(nombre);
			}
		} catch (ReflectiveOperationException e) {
			Dedsafio4.LOGGER.warn("No se pudo sacar el /{} de Minecraft", nombre, e);
		}
	}
}
