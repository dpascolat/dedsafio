package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * Creeper Pastel: una torta con cara de creeper, de 1 bloque de alto. Se porta como un creeper, pero la
 * explosión es chica y no rompe bloques; a los jugadores que estén a menos de 5 bloques les mancha la
 * pantalla con pastel unos segundos (eso lo dibuja el cliente al recibir PastelPayload).
 */
public class CreeperPastelEntity extends Creeper {
	private static final float POTENCIA = 1.5f, ALCANCE_MANCHA = 5;

	public CreeperPastelEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		// La explosión del creeper queda en 0: la de verdad (chiquita y sin romper nada) la hace tick().
		CompoundTag datos = new CompoundTag();
		datos.putByte("ExplosionRadius", (byte) 0);
		super.readAdditionalSaveData(datos);
	}

	@Override
	public void tick() {
		super.tick();
		// El creeper, al explotar, queda muerto y descartado en el mismo tick (si muere peleando, lo quitan como KILLED).
		if (!level().isClientSide && dead && getRemovalReason() == RemovalReason.DISCARDED && level() instanceof ServerLevel mundo) {
			mundo.explode(this, getX(), getY(), getZ(), POTENCIA, Level.ExplosionInteraction.NONE);
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.distanceToSqr(this) <= ALCANCE_MANCHA * ALCANCE_MANCHA) {
					ServerPlayNetworking.send(jugador, new PastelPayload());
				}
			}
		}
	}

	/** Servidor → cliente: mancharle la pantalla con pastel. */
	public record PastelPayload() implements CustomPacketPayload {
		public static final Type<PastelPayload> TYPE =
				new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "pastel"));
		public static final StreamCodec<ByteBuf, PastelPayload> CODEC = StreamCodec.unit(new PastelPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
