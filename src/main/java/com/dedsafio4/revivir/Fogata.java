package com.dedsafio4.revivir;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.baneos.Baneos;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La Fogata de revivir (FogataEntity): el objeto que la pone, el menú con los baneados y qué pasa cuando alguien
 * revive. Se paga igual que en el Bloque de Baneados (cuchara o tenedor en la mano; la Cuchara gasta el Alma).
 * Al elegirlo se le saca el ban. Cuando entra, mira el ritual en espectador y, cuando termina, vuelve a ser jugador
 * junto a la fogata (si ya está conectado, el ritual empieza enseguida).
 */
public final class Fogata {
	private Fogata() {}

	private static final double DISTANCIA = 10;
	/** A cuántos bloques de la fogata aparece el revivido. */
	private static final double LEJOS = 9;
	/** Desde dónde mira el ritual: a 15 bloques de la fogata y 3 más arriba. */
	private static final double MIRA_LEJOS = 15, MIRA_ARRIBA = 3;
	/** Si la fogata ya no está, cuántos ticks se la espera antes de hacerlo aparecer igual. */
	private static final int PACIENCIA = 200;

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

	/**
	 * esperando: los desbaneados que todavía no empezaron su ritual (qué fogata, dónde aparecen y quién los revivió).
	 * pendientes: los que terminaron el ritual desconectados: dónde tienen que aparecer cuando entren.
	 * modos: el modo de juego al que vuelven los que están mirando en espectador.
	 */
	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Map<UUID, CompoundTag> esperando = new HashMap<>(), pendientes = new HashMap<>();
		final Map<UUID, String> modos = new HashMap<>();

		private static void leer(CompoundTag tag, Map<UUID, CompoundTag> mapa) {
			for (String k : tag.getAllKeys()) mapa.put(UUID.fromString(k), tag.getCompound(k));
		}

		private static CompoundTag guardar(Map<UUID, CompoundTag> mapa) {
			CompoundTag tag = new CompoundTag();
			mapa.forEach((id, lugar) -> tag.put(id.toString(), lugar));
			return tag;
		}

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			leer(tag.getCompound("esperando"), d.esperando);
			leer(tag.getCompound("pendientes"), d.pendientes);
			CompoundTag m = tag.getCompound("modos");
			for (String k : m.getAllKeys()) d.modos.put(UUID.fromString(k), m.getString(k));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.put("esperando", guardar(esperando));
			tag.put("pendientes", guardar(pendientes));
			CompoundTag m = new CompoundTag();
			modos.forEach((id, modo) -> m.putString(id.toString(), modo));
			tag.put("modos", m);
			return tag;
		}
	}

	/** Los que ya entraron y están mirando en espectador hasta que su fogata se libere: [ticks sin fogata, ya avisado]. */
	private static final Map<UUID, int[]> MIRANDO = new HashMap<>();

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
			entrar(handler.player);
		});
		ServerTickEvents.END_SERVER_TICK.register(Fogata::tick);
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((jugador, desde, hasta) -> FogataEntity.alEntrar(jugador));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			FogataEntity.limpiar();
			MIRANDO.clear();
		});
	}

	static void abrir(ServerPlayer jugador, FogataEntity fogata) {
		if (fogata.reviviendo()) {
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
		if (fogata.reviviendo()) {
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
		MinecraftServer server = jugador.server;
		server.getPlayerList().getBans().remove(perfil);
		String nombre = perfil.getName() != null ? perfil.getName() : id.substring(0, 8);
		Vec3 lugar = lugar(jugador, fogata);
		float yaw = (float) (Mth.atan2(fogata.getZ() - lugar.z, fogata.getX() - lugar.x) * Mth.RAD_TO_DEG) - 90;
		CompoundTag espera = lugarTag(jugador.serverLevel(), lugar.x, lugar.y, lugar.z, yaw);
		espera.putUUID("fogata", fogata.getUUID());
		espera.putDouble("fogata_x", fogata.getX());
		espera.putDouble("fogata_y", fogata.getY());
		espera.putDouble("fogata_z", fogata.getZ());
		espera.putString("quien", jugador.getGameProfile().getName());
		Datos d = datos(server);
		d.esperando.put(uuid, espera);
		d.setDirty();
		ServerPlayer conectado = server.getPlayerList().getPlayer(uuid);
		if (conectado != null) entrar(conectado);
		else jugador.displayClientMessage(Component.literal(nombre + " no está conectado: la fogata lo va a revivir cuando entre.").withStyle(ChatFormatting.GREEN), false);
	}

	private static CompoundTag lugarTag(ServerLevel mundo, double x, double y, double z, float yaw) {
		CompoundTag lugar = new CompoundTag();
		lugar.putString("dimension", mundo.dimension().location().toString());
		lugar.putDouble("x", x);
		lugar.putDouble("y", y);
		lugar.putDouble("z", z);
		lugar.putFloat("yaw", yaw);
		return lugar;
	}

	private static ServerLevel mundo(MinecraftServer server, CompoundTag lugar) {
		return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(lugar.getString("dimension"))));
	}

	/** Entró uno que tiene que revivir: queda en espectador mirando la fogata hasta que empiece su ritual. */
	private static void entrar(ServerPlayer jugador) {
		Datos d = datos(jugador.server);
		CompoundTag espera = d.esperando.get(jugador.getUUID());
		if (espera == null) return;
		d.modos.putIfAbsent(jugador.getUUID(), jugador.gameMode.getGameModeForPlayer().getName());
		d.setDirty();
		jugador.setGameMode(GameType.SPECTATOR);
		ServerLevel mundo = mundo(jugador.server, espera);
		if (mundo != null) {
			double fx = espera.getDouble("fogata_x"), fz = espera.getDouble("fogata_z");
			double x = espera.getDouble("x") - fx, z = espera.getDouble("z") - fz;
			double largo = Math.max(0.001, Math.sqrt(x * x + z * z));
			double ojoX = fx + x / largo * MIRA_LEJOS, ojoZ = fz + z / largo * MIRA_LEJOS;
			float yaw = (float) (Mth.atan2(fz - ojoZ, fx - ojoX) * Mth.RAD_TO_DEG) - 90;
			jugador.teleportTo(mundo, ojoX, espera.getDouble("fogata_y") + MIRA_ARRIBA, ojoZ, yaw, 10);
		}
		MIRANDO.put(jugador.getUUID(), new int[2]);
		jugador.displayClientMessage(Component.literal("Te revivieron en la fogata: mira.").withStyle(ChatFormatting.GREEN), true);
	}

	/** Cada tick: a los que están mirando, les empieza el ritual apenas su fogata esté libre. */
	private static void tick(MinecraftServer server) {
		if (MIRANDO.isEmpty()) return;
		Datos d = datos(server);
		for (Iterator<Map.Entry<UUID, int[]>> it = MIRANDO.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, int[]> entrada = it.next();
			ServerPlayer jugador = server.getPlayerList().getPlayer(entrada.getKey());
			CompoundTag espera = d.esperando.get(entrada.getKey());
			if (jugador == null || espera == null) {
				it.remove();
				continue;
			}
			ServerLevel mundo = mundo(server, espera);
			if (mundo != null && mundo.getEntity(espera.getUUID("fogata")) instanceof FogataEntity fogata && !fogata.isRemoved()) {
				if (fogata.reviviendo()) {
					if (entrada.getValue()[1]++ == 0) jugador.displayClientMessage(Component.literal("La fogata está reviviendo a otro: espera tu turno.").withStyle(ChatFormatting.GRAY), false);
					continue;
				}
				it.remove();
				d.esperando.remove(entrada.getKey());
				d.setDirty();
				Property textura = jugador.getGameProfile().getProperties().get("textures").stream().findFirst().orElse(null);
				fogata.empezar(jugador.getUUID(), jugador.getGameProfile().getName(), textura == null ? "" : textura.value(),
						textura == null || textura.signature() == null ? "" : textura.signature(),
						espera.getDouble("x"), espera.getDouble("y"), espera.getDouble("z"), espera.getFloat("yaw"), espera.getString("quien"));
			} else if (++entrada.getValue()[0] > PACIENCIA) {
				// La fogata ya no está: aparece igual, sin ritual.
				it.remove();
				d.esperando.remove(entrada.getKey());
				d.pendientes.put(entrada.getKey(), espera);
				d.setDirty();
				anunciar(server, espera.getString("quien"), jugador.getGameProfile().getName(), entrada.getKey().toString());
				aparecer(jugador);
			}
		}
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

	/** Termina el ritual: vuelve a ser jugador junto a la fogata (o cuando entre, si se desconectó). */
	static void revivir(ServerLevel mundo, UUID id, double x, double y, double z, float yaw) {
		MinecraftServer server = mundo.getServer();
		Datos d = datos(server);
		d.pendientes.put(id, lugarTag(mundo, x, y, z, yaw));
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
		String modo = d.modos.remove(jugador.getUUID());
		d.setDirty();
		GameType tipo = modo == null ? GameType.SURVIVAL : GameType.byName(modo, GameType.SURVIVAL);
		jugador.setGameMode(tipo == GameType.SPECTATOR ? GameType.SURVIVAL : tipo);
		ServerLevel mundo = mundo(jugador.server, lugar);
		if (mundo != null) jugador.teleportTo(mundo, lugar.getDouble("x"), lugar.getDouble("y"), lugar.getDouble("z"), lugar.getFloat("yaw"), 0);
		jugador.setHealth(jugador.getMaxHealth());
		jugador.getFoodData().setFoodLevel(20);
		jugador.getFoodData().setSaturation(5);
		jugador.clearFire();
		jugador.fallDistance = 0;
	}
}
