package com.dedsafio4.demonios;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El puzzle de la cámara (lo da el Farol al pegar): al jugador se le abre una pantalla "Desbloquea la cámara" y tiene
 * que arrastrar la palanca alrededor del círculo hasta dar GIROS vueltas. Mientras lo está haciendo, otro golpe no
 * se lo reinicia; cuando lo termina, el próximo golpe se lo vuelve a abrir.
 */
public final class PuzzleCamara {
	private PuzzleCamara() {}

	public static final int GIROS = 4;
	/** Si en este tiempo no avisó que terminó (se murió, se desconectó...), se le puede volver a abrir. */
	private static final long OLVIDAR = 60 * 20;

	/** Servidor → jugador: abre el puzzle (si ya lo tiene abierto, no pasa nada). */
	public record AbrirPayload(int giros) implements CustomPacketPayload {
		public static final Type<AbrirPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "puzzle_camara"));
		public static final StreamCodec<ByteBuf, AbrirPayload> CODEC = ByteBufCodecs.VAR_INT.map(AbrirPayload::new, AbrirPayload::giros);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Jugador → servidor: terminó el puzzle. */
	public record ListoPayload() implements CustomPacketPayload {
		public static final Type<ListoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "puzzle_camara_listo"));
		public static final StreamCodec<ByteBuf, ListoPayload> CODEC = StreamCodec.unit(new ListoPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Quiénes están haciendo el puzzle (y desde qué tick del mundo). */
	private static final Map<UUID, Long> HACIENDO = new HashMap<>();

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(AbrirPayload.TYPE, AbrirPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ListoPayload.TYPE, ListoPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ListoPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> HACIENDO.remove(contexto.player().getUUID())));
	}

	/** Le abre el puzzle al jugador, salvo que ya lo esté haciendo. */
	public static void abrir(ServerPlayer jugador) {
		if (jugador.isCreative() || jugador.isSpectator()) return;
		long ahora = jugador.serverLevel().getGameTime();
		Long desde = HACIENDO.get(jugador.getUUID());
		if (desde != null && ahora - desde < OLVIDAR && ahora >= desde) return;
		HACIENDO.put(jugador.getUUID(), ahora);
		ServerPlayNetworking.send(jugador, new AbrirPayload(GIROS));
	}
}
