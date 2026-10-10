package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * Creeper Amarillo: igual que el creeper de siempre (mismo modelo y misma explosión), pero amarillo clarito. Cuando
 * explota, a los jugadores que estaban cerca les aparece en la pantalla la de "te echaron del servidor", pero es de
 * mentira: con el botón vuelven al juego (FalsoKickScreen, en el cliente).
 */
public class CreeperAmarilloEntity extends Creeper {
	/** A cuánto de la explosión les sale la pantalla. */
	private static final double CERCA = 8;

	public CreeperAmarilloEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
	}

	/** Servidor → jugador: mostrar la pantalla de "te echaron" de mentira. */
	public record FalsoKickPayload() implements CustomPacketPayload {
		public static final Type<FalsoKickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "falso_kick"));
		public static final StreamCodec<RegistryFriendlyByteBuf, FalsoKickPayload> CODEC = StreamCodec.unit(new FalsoKickPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(FalsoKickPayload.TYPE, FalsoKickPayload.CODEC);
	}

	@Override
	public void tick() {
		boolean vivo = !isRemoved();
		super.tick();
		// En su tick el creeper solo se borra cuando explota (queda "muerto" y lo descartan).
		if (vivo && isRemoved() && dead && !level().isClientSide) {
			for (ServerPlayer jugador : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(CERCA))) {
				if (jugador.isSpectator()) continue;
				ServerPlayNetworking.send(jugador, new FalsoKickPayload());
			}
		}
	}
}
