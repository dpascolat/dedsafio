package com.dedsafio4;

import com.dedsafio4.qumara.QumaraEntity;
import com.dedsafio4.trex.TRexEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * /admin no_boss → el admin que lo usa ya no se puede subir a los jefes (Qumara, T-Rex).
 * /admin boss    → se puede volver a subir.
 * /admin organos → ir a la Dimensión de los Órganos (o volver al Overworld si ya estás ahí).
 * Se guarda en el jugador (sigue igual después de reiniciar).
 */
public final class AdminComandos {
	private AdminComandos() {}

	private static final String SIN_JEFES = "dedsafio4_no_boss";

	/** ¿Se puede subir a los jefes? (en el cliente siempre dice que sí: lo decide el servidor) */
	public static boolean puedeManejarJefes(Player p) {
		return !p.getTags().contains(SIN_JEFES);
	}

	/** Para los jefes: si no se puede subir, le avisa (del lado del servidor). */
	public static boolean bloqueado(Player p) {
		if (puedeManejarJefes(p)) return false;
		if (!p.level().isClientSide) {
			p.displayClientMessage(Component.literal("Tienes /admin no_boss: no te puedes subir a los jefes (/admin boss para volver).").withColor(0xF0418F), true);
		}
		return true;
	}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String admin : new String[]{"admin", "Admin"}) {
			dispatcher.register(Commands.literal(admin).requires(s -> s.hasPermission(2))
					.then(Commands.literal("no_boss").executes(c -> cambiar(c.getSource().getPlayerOrException(), false)))
					.then(Commands.literal("boss").executes(c -> cambiar(c.getSource().getPlayerOrException(), true)))
					.then(Commands.literal("organos").executes(c ->
							com.dedsafio4.dimension.Organos.viajar(c.getSource().getPlayerOrException()))));
		}
	}

	private static int cambiar(ServerPlayer p, boolean puede) {
		if (puede) {
			p.removeTag(SIN_JEFES);
			p.sendSystemMessage(Component.literal("Ya te puedes subir a los jefes otra vez.").withColor(0xF0418F));
		} else {
			p.addTag(SIN_JEFES);
			if (p.getVehicle() instanceof QumaraEntity q && q.getControllingPassenger() == p || p.getVehicle() instanceof TRexEntity) p.stopRiding();
			p.sendSystemMessage(Component.literal("Listo: ya no te puedes subir a los jefes (para volver: /admin boss).").withColor(0xF0418F));
		}
		return 1;
	}
}
