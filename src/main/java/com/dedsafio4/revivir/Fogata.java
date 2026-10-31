package com.dedsafio4.revivir;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.baneos.Baneos;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * La Fogata de revivir (FogataEntity): el objeto que la pone, el menú con los baneados y qué pasa cuando alguien
 * revive. Se paga igual que en el Bloque de Baneados (cuchara o tenedor en la mano; la Cuchara gasta el Alma). El
 * desbaneado aparece junto a la fogata la próxima vez que entra.
 */
public final class Fogata {
	private Fogata() {}

	private static final double DISTANCIA = 10;
	/** A cuántos bloques de la fogata aparece el revivido. */
	private static final double LEJOS = 9;

	public static final EntityType<FogataEntity> TIPO = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("fogata"),
			EntityType.Builder.of(FogataEntity::new, MobCategory.MISC).sized(2.5f, 2.5f).eyeHeight(0.5f).fireImmune()
					.clientTrackingRange(8).updateInterval(Integer.MAX_VALUE).build("fogata"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("fogata"), new Item(new Item.Properties()) {
		@Override
		public InteractionResult useOn(UseOnContext contexto) {
			if (!(contexto.getLevel() instanceof ServerLevel mundo)) return InteractionResult.SUCCESS;
			FogataEntity fogata = TIPO.create(mundo);
			if (fogata == null) return InteractionResult.FAIL;
			BlockPos pos = new BlockPlaceContext(contexto).getClickedPos();
			double y = contexto.getClickedFace() == Direction.UP ? contexto.getClickLocation().y : pos.getY();
			fogata.moveTo(pos.getX() + 0.5, y, pos.getZ() + 0.5, contexto.getRotation() + 180, 0);
			mundo.addFreshEntity(fogata);
			mundo.playSound(null, fogata.getX(), fogata.getY(), fogata.getZ(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
			mundo.gameEvent(contexto.getPlayer(), GameEvent.ENTITY_PLACE, fogata.position());
			contexto.getItemInHand().consume(1, contexto.getPlayer());
			return InteractionResult.CONSUME;
		}
	});

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Servidor → cliente: abrir el menú de esa fogata con los baneados. */
	public record ListaPayload(int fogata, List<String> ids, List<String> nombres) implements CustomPacketPayload {
		public static final Type<ListaPayload> TYPE = new Type<>(Fogata.id("fogata_lista"));
		public static final StreamCodec<ByteBuf, ListaPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, ListaPayload::fogata,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ListaPayload::ids,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ListaPayload::nombres,
				ListaPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor → cliente: empezó (o terminó, con desde = -1) un ritual: el cielo oscuro y los sonidos. */
	public record CieloPayload(String id, double x, double y, double z, long desde) implements CustomPacketPayload {
		public static final Type<CieloPayload> TYPE = new Type<>(Fogata.id("fogata_cielo"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CieloPayload> CODEC = StreamCodec.of((buf, p) -> {
			buf.writeUtf(p.id());
			buf.writeDouble(p.x());
			buf.writeDouble(p.y());
			buf.writeDouble(p.z());
			buf.writeLong(p.desde());
		}, buf -> new CieloPayload(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readLong()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: revivir a ese jugador en esa fogata. */
	public record RevivirPayload(int fogata, String jugador) implements CustomPacketPayload {
		public static final Type<RevivirPayload> TYPE = new Type<>(Fogata.id("fogata_revivir"));
		public static final StreamCodec<ByteBuf, RevivirPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, RevivirPayload::fogata, ByteBufCodecs.STRING_UTF8, RevivirPayload::jugador, RevivirPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Los que revivieron y todavía no entraron: dónde tienen que aparecer. */
	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Map<UUID, CompoundTag> pendientes = new HashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (String k : tag.getAllKeys()) d.pendientes.put(UUID.fromString(k), tag.getCompound(k));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			pendientes.forEach((id, lugar) -> tag.put(id.toString(), lugar));
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_fogata");
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(ListaPayload.TYPE, ListaPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CieloPayload.TYPE, CieloPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RevivirPayload.TYPE, RevivirPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RevivirPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> elegir(contexto.player(), payload.fogata(), payload.jugador())));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			FogataEntity.alEntrar(handler.player);
			aparecer(handler.player);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((jugador, desde, hasta) -> FogataEntity.alEntrar(jugador));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FogataEntity.limpiar());
	}

	static void abrir(ServerPlayer jugador, FogataEntity fogata) {
		if (fogata.ocupada()) {
			jugador.displayClientMessage(Component.literal("✖ La fogata está en uso: espera a que termine.").withStyle(ChatFormatting.RED), false);
			return;
		}
		List<String> ids = new ArrayList<>(), nombres = new ArrayList<>();
		for (GameProfile perfil : Baneos.baneados(jugador)) {
			ids.add(perfil.getId().toString());
			nombres.add(perfil.getName() == null ? perfil.getId().toString().substring(0, 8) : perfil.getName());
		}
		ServerPlayNetworking.send(jugador, new ListaPayload(fogata.getId(), ids, nombres));
	}

	private static void elegir(ServerPlayer jugador, int idFogata, String id) {
		if (!(jugador.serverLevel().getEntity(idFogata) instanceof FogataEntity fogata) || jugador.distanceToSqr(fogata) > DISTANCIA * DISTANCIA) return;
		if (fogata.ocupada()) {
			jugador.displayClientMessage(Component.literal("✖ La fogata está en uso: espera a que termine.").withStyle(ChatFormatting.RED), false);
			return;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			return;
		}
		GameProfile perfil = Baneos.baneados(jugador).stream().filter(p -> p.getId().equals(uuid)).findFirst().orElse(null);
		if (perfil == null || !Baneos.pagar(jugador)) return;
		fogata.reservar();
		String nombre = perfil.getName() != null ? perfil.getName() : id.substring(0, 8);
		Vec3 lugar = lugar(jugador, fogata);
		float yaw = (float) (Mth.atan2(fogata.getZ() - lugar.z, fogata.getX() - lugar.x) * Mth.RAD_TO_DEG) - 90;
		String quien = jugador.getGameProfile().getName();
		MinecraftServer server = jugador.server;
		// La skin se pide a Mojang: lleva un rato, así que va aparte y el ritual empieza cuando vuelve.
		CompletableFuture.supplyAsync(() -> {
			try {
				var resultado = server.getSessionService().fetchProfile(uuid, true);
				if (resultado == null) return null;
				for (Property p : resultado.profile().getProperties().get("textures")) return p;
			} catch (RuntimeException e) {
				Dedsafio4.LOGGER.warn("No se pudo pedir la skin de {} para la fogata", nombre, e);
			}
			return null;
		}).thenAcceptAsync(textura -> {
			if (fogata.isRemoved()) return;
			fogata.empezar(uuid, nombre, textura == null ? "" : textura.value(), textura == null || textura.signature() == null ? "" : textura.signature(),
					lugar.x, lugar.y, lugar.z, yaw, quien);
		}, server);
		jugador.displayClientMessage(Component.literal("Reviviste a " + nombre + ".").withStyle(ChatFormatting.GREEN), false);
	}

	/** A 9 bloques de la fogata, del lado del que revive (o a un costado, si ahí hay algo). */
	private static Vec3 lugar(ServerPlayer jugador, FogataEntity fogata) {
		Vec3 centro = fogata.position();
		Vec3 hacia = new Vec3(jugador.getX() - centro.x, 0, jugador.getZ() - centro.z);
		hacia = hacia.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : hacia.normalize();
		for (double angulo : new double[]{Math.PI / 2, -Math.PI / 2, Math.PI, 0}) {
			double cos = Math.cos(angulo), sen = Math.sin(angulo);
			Vec3 lugar = new Vec3(centro.x + (hacia.x * cos - hacia.z * sen) * LEJOS, jugador.getY(), centro.z + (hacia.x * sen + hacia.z * cos) * LEJOS);
			if (jugador.serverLevel().noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(lugar))) return lugar;
		}
		return jugador.position();
	}

	static void mandarCielo(ServerPlayer jugador, String id, double x, double y, double z, long desde) {
		ServerPlayNetworking.send(jugador, new CieloPayload(id, x, y, z, desde));
	}

	/** Termina el ritual: se le saca el ban y queda anotado dónde aparece cuando entre. */
	static void revivir(ServerLevel mundo, UUID id, String nombre, double x, double y, double z, float yaw) {
		MinecraftServer server = mundo.getServer();
		server.getPlayerList().getBans().remove(new GameProfile(id, nombre));
		CompoundTag lugar = new CompoundTag();
		lugar.putString("dimension", mundo.dimension().location().toString());
		lugar.putDouble("x", x);
		lugar.putDouble("y", y);
		lugar.putDouble("z", z);
		lugar.putFloat("yaw", yaw);
		Datos d = datos(server);
		d.pendientes.put(id, lugar);
		d.setDirty();
		ServerPlayer conectado = server.getPlayerList().getPlayer(id);
		if (conectado != null) aparecer(conectado);
	}

	static void anunciar(MinecraftServer server, String quien, String nombre, String id) {
		Baneos.anunciarRevivido(server, quien, nombre, UUID.fromString(id));
	}

	private static void aparecer(ServerPlayer jugador) {
		Datos d = datos(jugador.server);
		CompoundTag lugar = d.pendientes.remove(jugador.getUUID());
		if (lugar == null) return;
		d.setDirty();
		ServerLevel mundo = jugador.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(lugar.getString("dimension"))));
		if (mundo != null) jugador.teleportTo(mundo, lugar.getDouble("x"), lugar.getDouble("y"), lugar.getDouble("z"), lugar.getFloat("yaw"), 0);
		jugador.fallDistance = 0;
	}
}
