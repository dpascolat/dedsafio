package com.dedsafio4.trex;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** El T-Rex gigante y sus dos botones (Gritar y Abrir puerta), que manda el cliente del jinete. */
public final class ModTRex {
	private ModTRex() {}

	/** La caja para chocar es la de las patas (el dibujo mide 40 de alto y 30 de ancho). */
	public static final EntityType<TRexEntity> TREX = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("trex"),
			EntityType.Builder.<TRexEntity>of(TRexEntity::new, MobCategory.MISC).sized(8f, 20f)
					.clientTrackingRange(16).updateInterval(1).build("trex"));

	/** Los botones: 1, 2 y 4 (R, G y V). En el T-Rex: 1 gritar, 2 abrir puerta; en Qumara: 1 nacer, 2 giro, 4 círculos, 5 atraer, 6 gas. */
	public static final int GRITAR = 1, ABRIR_PUERTA = 2, BOTON_4 = 4, BOTON_5 = 5, BOTON_6 = 6;

	/** Cliente → servidor: el jinete apretó un botón. */
	public record AccionPayload(int accion) implements CustomPacketPayload {
		public static final Type<AccionPayload> TYPE = new Type<>(id("trex_accion"));
		public static final StreamCodec<ByteBuf, AccionPayload> CODEC = ByteBufCodecs.VAR_INT.map(AccionPayload::new, AccionPayload::accion);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(TREX, TRexEntity.crearAtributos());
		PayloadTypeRegistry.playC2S().register(AccionPayload.TYPE, AccionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AccionPayload.TYPE, (payload, context) ->
				context.server().execute(() -> accion(context.player(), payload.accion())));
	}

	private static void accion(ServerPlayer jugador, int accion) {
		// El mismo paquete sirve para los botones de todos los jefes que se manejan.
		if (jugador.getVehicle() instanceof com.dedsafio4.qumara.QumaraEntity qumara && qumara.getControllingPassenger() == jugador) {
			qumara.accion(jugador, accion);
			return;
		}
		if (!(jugador.getVehicle() instanceof TRexEntity trex) || trex.getControllingPassenger() != jugador) return;
		if (accion == GRITAR) trex.rugir();
		else if (accion == ABRIR_PUERTA) {
			jugador.displayClientMessage(Component.literal("Abrir puerta: próximamente.").withColor(0xC6CFD6), true);
		}
	}
}
