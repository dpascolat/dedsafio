package com.dedsafio4.cambios;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cambio "luna_roja 1": además de lo que hacen los zombis con pico (ZombiMinero), de noche la luna se ve roja y el
 * cielo se pone rojizo (lo dibuja LunaRojaCliente). Acá sólo se le avisa a cada jugador si la Luna Roja está activa.
 */
public final class LunaRoja {
	private LunaRoja() {}

	public record Payload(boolean activa) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "luna_roja"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Payload::activa, Payload::new);

		@Override
		public Type<Payload> type() {
			return TYPE;
		}
	}

	/** Lo último que se le mandó a cada jugador (para avisar sólo cuando cambia). */
	private static final Map<UUID, Boolean> AVISADO = new HashMap<>();

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			boolean activa = Cambios.nivel(server, Cambios.LUNA_ROJA) >= 1;
			AVISADO.keySet().removeIf(u -> server.getPlayerList().getPlayer(u) == null);
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				Boolean antes = AVISADO.put(jugador.getUUID(), activa);
				if (antes == null || antes != activa) ServerPlayNetworking.send(jugador, new Payload(activa));
			}
		});
	}
}
