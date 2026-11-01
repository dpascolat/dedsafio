package com.dedsafio4.salidalimbo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.dimension.Portales;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Salida del Limbo: un bloque que mira hacia adelante en línea recta. Al jugador que ve, a cualquier distancia, se
 * lo lleva al lugar del Overworld elegido con /spawnspawn_limbo <x y z>: la pantalla se pone negra de a poco, viaja y
 * al llegar no recibe daño de caída. Una pared u otro bloque cortan la vista: con dos Salidas enfrentadas, sólo te
 * lleva el pasillo que queda entre las dos. No ve a los de creativo ni a los espectadores, y sólo mira chunks ya
 * cargados. /spawnspawn_limbo solo dice el lugar y /spawnspawn_limbo quitar lo saca (sin lugar, no lleva a nadie).
 */
public final class SalidaLimbo {
	private SalidaLimbo() {}

	/** Hasta cuántos bloques ve. */
	private static final int ALCANCE = 512;
	/** Cada cuántos ticks mira. */
	private static final int CADA = 5;
	/** Lo que tarda la pantalla en ponerse negra: 2 segundos. */
	private static final int TARDA = 40;
	/** Durante cuántos ticks después de llegar se ignora el "toca el piso" que traía del Limbo. */
	private static final int MARGEN = 20;

	public static final Block BLOQUE = Registry.register(BuiltInRegistries.BLOCK, id("salida_limbo"),
			new SalidaLimboBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
					.sound(SoundType.SCULK).pushReaction(PushReaction.BLOCK)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("salida_limbo"),
			new BlockItem(BLOQUE, new Item.Properties()));
	public static final BlockEntityType<SalidaLimboBlockEntity> ENTIDAD = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("salida_limbo"), FabricBlockEntityTypeBuilder.create(SalidaLimboBlockEntity::new, BLOQUE).build());

	/** Jugador → en qué tick viaja. */
	private static final Map<UUID, Integer> SALIENDO = new HashMap<>();
	/** Jugador → en qué tick llegó (no recibe daño de caída hasta tocar el piso). */
	private static final Map<UUID, Integer> SIN_CAIDA = new HashMap<>();

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	private static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		BlockPos lugar;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("lugar")) d.lugar = BlockPos.of(tag.getLong("lugar"));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			if (lugar != null) tag.putLong("lugar", lugar.asLong());
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_salida_limbo");
	}

	/** Carga la clase (registra lo de arriba) y el viaje de los que van saliendo. */
	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(SalidaLimbo::tick);
	}

	/** Lo que hace cada bloque: mira si tiene a alguien adelante. */
	static void mirar(ServerLevel mundo, BlockPos pos, Direction adelante) {
		if (mundo.getGameTime() % CADA != 0 || mundo.players().isEmpty() || datos(mundo.getServer()).lugar == null) return;
		AABB rayo = new AABB(pos.relative(adelante)).expandTowards(adelante.getStepX() * (double) ALCANCE, 0, adelante.getStepZ() * (double) ALCANCE);
		for (ServerPlayer jugador : mundo.players()) {
			if (!jugador.isAlive() || jugador.isCreative() || jugador.isSpectator() || SALIENDO.containsKey(jugador.getUUID())) continue;
			if (!jugador.getBoundingBox().intersects(rayo) || !loVe(mundo, pos, adelante, jugador)) continue;
			SALIENDO.put(jugador.getUUID(), mundo.getServer().getTickCount() + TARDA);
			ServerPlayNetworking.send(jugador, new Portales.FundidoPayload(TARDA));
			mundo.playSound(null, jugador.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5f, 0.6f);
		}
	}

	/** Recorre la línea hasta el jugador: una pared, otro bloque o un chunk sin cargar cortan la vista. */
	private static boolean loVe(ServerLevel mundo, BlockPos pos, Direction adelante, ServerPlayer jugador) {
		for (int i = 1; i <= ALCANCE; i++) {
			BlockPos lugar = pos.relative(adelante, i);
			if (!mundo.hasChunkAt(lugar)) return false;
			if (jugador.getBoundingBox().intersects(new AABB(lugar))) return true;
			if (mundo.getBlockState(lugar).blocksMotion()) return false;
		}
		return false;
	}

	private static void tick(MinecraftServer server) {
		int ahora = server.getTickCount();
		SALIENDO.entrySet().removeIf(e -> {
			ServerPlayer jugador = server.getPlayerList().getPlayer(e.getKey());
			if (jugador == null || !jugador.isAlive()) return true;
			if (ahora < e.getValue()) return false;
			viajar(jugador);
			return true;
		});
		SIN_CAIDA.entrySet().removeIf(e -> {
			ServerPlayer jugador = server.getPlayerList().getPlayer(e.getKey());
			if (jugador == null) return false;   // se desconectó en el aire: sigue protegido al volver
			if (!jugador.isAlive()) return true;
			jugador.fallDistance = 0;
			return ahora - e.getValue() > MARGEN && (jugador.onGround() || jugador.isInWater());
		});
	}

	private static void viajar(ServerPlayer jugador) {
		MinecraftServer server = jugador.getServer();
		BlockPos lugar = datos(server).lugar;
		if (lugar == null) {
			ServerPlayNetworking.send(jugador, new Portales.FundidoPayload(0));
			return;
		}
		ServerLevel destino = server.overworld();
		SIN_CAIDA.put(jugador.getUUID(), server.getTickCount());
		jugador.fallDistance = 0;
		jugador.teleportTo(destino, lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
		destino.playSound(null, lugar, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.5f);
	}

	/** ¿Viene de la Salida del Limbo y todavía no tocó el piso? */
	public static boolean sinCaida(ServerPlayer jugador) {
		return SIN_CAIDA.containsKey(jugador.getUUID());
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawnspawn_limbo").requires(s -> s.hasPermission(2))
				.executes(c -> {
					BlockPos lugar = datos(c.getSource().getServer()).lugar;
					c.getSource().sendSuccess(() -> Component.literal(lugar == null
							? "La Salida del Limbo no tiene lugar: no lleva a nadie."
							: "La Salida del Limbo lleva a " + lugar.getX() + " " + lugar.getY() + " " + lugar.getZ() + " del Overworld.")
							.withStyle(ChatFormatting.GRAY), false);
					return 1;
				})
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.lugar = null;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Lugar de la Salida del Limbo quitado.").withStyle(ChatFormatting.GRAY), true);
					return 1;
				}))
				.then(Commands.argument("coordenadas", BlockPosArgument.blockPos()).executes(c -> {
					BlockPos pos = BlockPosArgument.getBlockPos(c, "coordenadas");
					Datos d = datos(c.getSource().getServer());
					d.lugar = pos.immutable();
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("La Salida del Limbo ahora lleva a " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
							+ " del Overworld.").withStyle(ChatFormatting.GRAY), true);
					return 1;
				})));
	}
}
