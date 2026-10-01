package com.dedsafio4.cambios;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * /momento revil 1.1    los jugadores por encima de la capa 250 reciben 3 de daño por segundo
 * /momento revil 1.2    los jugadores por encima de la capa -1 reciben 3 de daño por segundo
 * /momento revil parar  lo desactiva
 * Solo en el Overworld. El daño es mágico (la armadura no lo reduce).
 */
public final class MomentoReviil {
	private MomentoReviil() {}

	private static final String CLAVE = "momento_reviil";
	private static final float DANIO = 3f;

	private record Fase(String nombre, int codigo, int capaMaxima, String anuncio) {}

	private static final Fase[] FASES = {
			new Fase("1.1", 11, 250, "Reviil reclama las alturas. Todo el que esté por encima de la capa 250 recibirá daño."),
			new Fase("1.2", 12, -1, "Reviil cubrió la superficie. Todo el que esté por encima de la capa -1 recibirá daño.")
	};

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO = String.valueOf((char) 0xE001);

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		var revil = Commands.literal("revil")
				.then(Commands.literal("parar").executes(ctx -> {
					Cambios.data(ctx.getSource().getServer()).setNivel(CLAVE, 0);
					ctx.getSource().sendSuccess(() -> Component.literal("Momento Reviil desactivado."), true);
					return 1;
				}));
		for (Fase fase : FASES) {
			revil.then(Commands.literal(fase.nombre()).executes(ctx -> {
				MinecraftServer server = ctx.getSource().getServer();
				Cambios.data(server).setNivel(CLAVE, fase.codigo());
				anunciar(server, fase);
				return 1;
			}));
		}
		dispatcher.register(Commands.literal("momento").requires(s -> s.hasPermission(2)).then(revil));
	}

	private static void anunciar(MinecraftServer server, Fase fase) {
		Component mensaje = Component.empty()
				.append(Component.literal(ICONO).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS)))
				.append(Component.literal(" Momento Reviil " + fase.nombre() + ":")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF3B3B)).withBold(true)))
				.append(Component.literal("\n" + fase.anuncio())
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF9A9A))));
		server.getPlayerList().broadcastSystemMessage(mensaje, false);
	}

	/** Una vez por segundo, daña a los jugadores del Overworld que estén por encima de la capa de la fase activa. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		int codigo = Cambios.nivel(server, CLAVE);
		if (codigo == 0) return;
		Integer capa = null;
		for (Fase fase : FASES) if (fase.codigo() == codigo) capa = fase.capaMaxima();
		if (capa == null) return;

		for (ServerPlayer jugador : server.overworld().players()) {
			if (jugador.getBlockY() > capa && jugador.isAlive()) {
				jugador.hurt(jugador.damageSources().magic(), DANIO);
			}
		}
	}
}
