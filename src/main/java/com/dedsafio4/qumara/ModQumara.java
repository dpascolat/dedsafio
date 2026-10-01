package com.dedsafio4.qumara;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Qumara, la Flor Mutante (jefe). */
public final class ModQumara {
	private ModQumara() {}

	/** Todo el cuerpo recibe golpes: la caja abarca raíces, tallo y cabeza (24 de ancho, 40 de alto). */
	public static final EntityType<QumaraEntity> QUMARA = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "qumara"),
			EntityType.Builder.<QumaraEntity>of(QumaraEntity::new, MobCategory.MISC).sized(24f, 40f)
					.clientTrackingRange(16).updateInterval(1).build("qumara"));

	/** Los bichos cubo del Enfriamiento: el no de la cabeza mide 7 de alto; el de la cabeza, 1 bloque. */
	public static final EntityType<BichoCuboEntity> BICHO_NO_CABEZA = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bicho_no_cabeza"),
			EntityType.Builder.<BichoCuboEntity>of((t, l) -> new BichoCuboEntity(t, l, false), MobCategory.MISC).sized(4f, 7f)
					.clientTrackingRange(10).build("bicho_no_cabeza"));
	public static final EntityType<BichoCuboEntity> BICHO_CABEZA = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bicho_cabeza"),
			EntityType.Builder.<BichoCuboEntity>of((t, l) -> new BichoCuboEntity(t, l, true), MobCategory.MISC).sized(0.6f, 1f)
					.clientTrackingRange(10).updateInterval(1).build("bicho_cabeza"));

	/** El Capullo, de donde salen los Bichos de la cabeza (14 de alto, 11 de ancho). */
	public static final EntityType<CapulloEntity> CAPULLO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "capullo"),
			EntityType.Builder.<CapulloEntity>of(CapulloEntity::new, MobCategory.MISC).sized(9f, 14f)
					.clientTrackingRange(10).build("capullo"));

	/** Círculo Explosivo (botón 4): 24 bloques de diámetro, en el piso. */
	public static final EntityType<CirculoEntity> CIRCULO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "circulo"),
			EntityType.Builder.<CirculoEntity>of(CirculoEntity::new, MobCategory.MISC).sized(1f, 0.1f)
					.clientTrackingRange(10).updateInterval(20).build("circulo"));
	/** El Gas Morado (lo tira Qumara por la boca; da el Veneno Primitivo). */
	public static final EntityType<GasMoradoEntity> GAS_MORADO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "gas_morado"),
			EntityType.Builder.<GasMoradoEntity>of(GasMoradoEntity::new, MobCategory.MISC).sized(2f, 1f)
					.clientTrackingRange(8).updateInterval(2).build("gas_morado"));
	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> DANIO_CIRCULO =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "circulo"));
	/** Estar pegado a Qumara. */
	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> DANIO_PLANTA =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "qumara"));

	/** Agarrado por Qumara y perdió el minijuego. */
	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> DANIO_AGARRE =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "agarre"));

	/** Cliente → servidor: el agarrado pulsó el espacio en el minijuego (y si acertó). */
	public record MinijuegoPayload(boolean acierto) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<MinijuegoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "minijuego_agarre"));
		public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, MinijuegoPayload> CODEC =
				net.minecraft.network.codec.ByteBufCodecs.BOOL.map(MinijuegoPayload::new, MinijuegoPayload::acierto);

		@Override
		public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: tirar el Bicho de la cabeza (click derecho con él encima). */
	public record LanzarPayload() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<LanzarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "lanzar_bicho"));
		public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, LanzarPayload> CODEC =
				net.minecraft.network.codec.StreamCodec.unit(new LanzarPayload());

		@Override
		public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(QUMARA, QumaraEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(BICHO_NO_CABEZA, BichoCuboEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(BICHO_CABEZA, BichoCuboEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(CAPULLO, CapulloEntity.crearAtributos());
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(LanzarPayload.TYPE, LanzarPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(MinijuegoPayload.TYPE, MinijuegoPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(MinijuegoPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					if (context.player().getVehicle() instanceof QumaraEntity q) q.pulso(context.player(), payload.acierto());
				}));
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(LanzarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					for (var pasajero : context.player().getPassengers()) {
						if (pasajero instanceof BichoCuboEntity bicho && bicho.deLaCabeza()) {
							bicho.lanzar(context.player());
							return;
						}
					}
				}));
	}
}
