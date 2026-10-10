package com.dedsafio4.dimension;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * La Dimensión del Limbo: un lugar oscuro (sin sol ni luz del cielo, siempre de noche, cielo negro y ceniza en el
 * aire), plano: una sola capa de piedra y nada más (sin construcciones; solo aparecen los fantasmas). Se entra con "/admin limbo" (y con el mismo comando se vuelve).
 * El agua del Limbo te pega (1 corazón por segundo, la armadura no sirve) y te saca un corazón PARA SIEMPRE cada segundo que estés adentro (con 1 solo corazón no te saca
 * nada, hasta que vuelvas a tener más).
 * Los corazones perdidos quedan guardados en el mundo (siguen igual al morir, salir o reiniciar el servidor).
 * "/limbo devolver [jugadores]" se los devuelve; "/limbo ver <jugador>" dice cuántos perdió.
 * "/spawn_limbo <x y z>" elige adónde llega el que es mandado al Limbo (por ejemplo, por el Wraith); "/spawn_limbo
 * quitar" lo saca (entonces llega a un lugar seguro cerca de donde estaba).
 * En el Limbo no se pueden poner bloques, romper bloques ni poner agua (ni ningún balde). En creativo, sí (para armarlo).
 * El tridente (el modelo nuevo) solo sirve en el Limbo: afuera no se puede tirar ni pegar con él.
 */
public final class Limbo {
	private Limbo() {}

	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "limbo"));
	public static final ResourceKey<net.minecraft.world.damagesource.DamageType> DANIO_AGUA = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "agua_limbo"));
	private static final ResourceLocation MODIFICADOR = ResourceLocation.fromNamespaceAndPath("dedsafio4", "limbo_corazones");

	/** Cuántos corazones perdió cada jugador en el agua del Limbo. */
	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		final Map<UUID, Integer> perdidos = new HashMap<>();
		/** Adónde llega el que es mandado al Limbo (null = un lugar seguro cerca de donde estaba). */
		BlockPos spawn;

		private static Datos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (String k : tag.getAllKeys()) {
				if (k.equals("spawn")) d.spawn = BlockPos.of(tag.getLong(k));
				else d.perdidos.put(UUID.fromString(k), tag.getInt(k));
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			perdidos.forEach((j, n) -> tag.putInt(j.toString(), n));
			if (spawn != null) tag.putLong("spawn", spawn.asLong());
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_limbo");
	}

	/** ¿Este jugador está en el Limbo y no puede tocar nada? (en creativo, sí puede). */
	private static boolean bloqueado(net.minecraft.world.entity.player.Player jugador, Level mundo) {
		return mundo.dimension().equals(DIMENSION) && !jugador.isCreative();
	}

	private static boolean esBloqueOBalde(net.minecraft.world.item.ItemStack pila) {
		return pila.getItem() instanceof net.minecraft.world.item.BlockItem || pila.getItem() instanceof net.minecraft.world.item.BucketItem
				|| pila.getItem() instanceof net.minecraft.world.item.SolidBucketItem;
	}

	public static void registrar() {
		// Romper bloques: no.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((mundo, jugador, pos, estado, entidad) -> {
			if (!bloqueado(jugador, mundo)) return true;
			jugador.displayClientMessage(Component.literal("En el Limbo no se pueden romper bloques.").withColor(0x8A8AA8), true);
			return false;
		});
		// Poner bloques o agua (o cualquier balde) sobre un bloque: no. Abrir puertas, cofres, etc.: sí.
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((jugador, mundo, mano, golpe) -> {
			if (!bloqueado(jugador, mundo) || !esBloqueOBalde(jugador.getItemInHand(mano))) return net.minecraft.world.InteractionResult.PASS;
			jugador.displayClientMessage(Component.literal("En el Limbo no se puede poner nada.").withColor(0x8A8AA8), true);
			return net.minecraft.world.InteractionResult.FAIL;
		});
		// El tridente solo sirve en el Limbo: afuera no se carga ni se tira (pegar con él lo frena TridenteLimboMixin).
		net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((jugador, mundo, mano) -> {
			net.minecraft.world.item.ItemStack pila = jugador.getItemInHand(mano);
			if (!pila.is(net.minecraft.world.item.Items.TRIDENT) || mundo.dimension().equals(DIMENSION)) {
				return net.minecraft.world.InteractionResultHolder.pass(pila);
			}
			if (!mundo.isClientSide) jugador.displayClientMessage(Component.literal("El tridente solo sirve en el Limbo.").withColor(0x8A8AA8), true);
			return net.minecraft.world.InteractionResultHolder.fail(pila);
		});
		// Usar un balde apuntando al aire o al agua: tampoco.
		net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((jugador, mundo, mano) -> {
			net.minecraft.world.item.ItemStack pila = jugador.getItemInHand(mano);
			if (!bloqueado(jugador, mundo) || !esBloqueOBalde(pila)) return net.minecraft.world.InteractionResultHolder.pass(pila);
			return net.minecraft.world.InteractionResultHolder.fail(pila);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			Datos d = datos(server);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				// En el agua del Limbo: te pega (1 corazón, la armadura no sirve) y un corazón menos, para siempre.
				if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.level().dimension().equals(DIMENSION) && p.isInWater()) {
					p.hurt(new net.minecraft.world.damagesource.DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
							.getHolderOrThrow(DANIO_AGUA)), 2f);
					quitarCorazon(p, "El agua del Limbo te quitó un corazón para siempre.");
				}
				// La vida máxima de cada uno (también después de morir o al entrar al mundo).
				aplicar(p, d.perdidos.getOrDefault(p.getUUID(), 0));
			}
		});
	}

	/**
	 * Le saca un corazón para siempre (el agua del Limbo, el Fantasma Negro...), con el aviso y el latido. Si tiene un
	 * solo corazón no le saca nada, hasta que vuelva a tener más de uno. Se recupera con /limbo devolver.
	 */
	public static void quitarCorazon(ServerPlayer p, String aviso) {
		if (p.getMaxHealth() <= 2) return;
		Datos d = datos(p.server);
		int antes = d.perdidos.getOrDefault(p.getUUID(), 0);
		d.perdidos.put(p.getUUID(), antes + 1);
		d.setDirty();
		aplicar(p, antes + 1);
		p.displayClientMessage(Component.literal(aviso).withColor(0x8A8AA8), true);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1f, 0.6f);
	}

	private static void aplicar(ServerPlayer p, int perdidos) {
		AttributeInstance vida = p.getAttribute(Attributes.MAX_HEALTH);
		if (vida == null) return;
		AttributeModifier actual = vida.getModifier(MODIFICADOR);
		double quiere = -2.0 * perdidos;
		if (actual != null && actual.amount() == quiere) return;
		vida.removeModifier(MODIFICADOR);
		if (perdidos > 0) vida.addTransientModifier(new AttributeModifier(MODIFICADOR, quiere, AttributeModifier.Operation.ADD_VALUE));
		if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
	}

	/** Lo manda al Limbo: a las coordenadas de /spawn_limbo o, si no hay, a un lugar seguro cerca de donde estaba. */
	public static void mandar(ServerPlayer jugador) {
		ServerLevel limbo = jugador.server.getLevel(DIMENSION);
		if (limbo == null) return;
		BlockPos spawn = datos(jugador.server).spawn;
		BlockPos llegada = spawn != null ? spawn : Portales.lugarSeguro(limbo, jugador.blockPosition());
		jugador.teleportTo(limbo, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
		limbo.playSound(null, llegada, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8f, 0.5f);
		jugador.sendSystemMessage(Component.literal("Fuiste arrastrado al Limbo.").withColor(0x8A8AA8));
	}

	public static int viajar(ServerPlayer jugador) {
		ServerLevel desde = jugador.serverLevel();
		boolean volver = desde.dimension().equals(DIMENSION);
		ServerLevel destino = volver ? desde.getServer().overworld() : desde.getServer().getLevel(DIMENSION);
		if (destino == null) return 0;
		BlockPos llegada = Portales.lugarSeguro(destino, jugador.blockPosition());
		jugador.teleportTo(destino, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
		destino.playSound(null, llegada, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6f, 0.5f);
		jugador.sendSystemMessage(Component.literal(volver
				? "Saliste del Limbo."
				: "Entraste al Limbo. Cuidado con el agua: cada segundo te quita un corazón para siempre (para volver: /admin limbo).")
				.withColor(0x8A8AA8));
		return 1;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawn_limbo").requires(s -> s.hasPermission(2))
				.executes(c -> {
					BlockPos spawn = datos(c.getSource().getServer()).spawn;
					c.getSource().sendSuccess(() -> Component.literal(spawn == null
							? "No hay spawn del Limbo: se llega a un lugar seguro cerca de donde estabas."
							: "Spawn del Limbo: " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ()).withStyle(ChatFormatting.GRAY), false);
					return 1;
				})
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.spawn = null;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Spawn del Limbo quitado.").withStyle(ChatFormatting.GRAY), true);
					return 1;
				}))
				.then(Commands.argument("coordenadas", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c -> {
					BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "coordenadas");
					Datos d = datos(c.getSource().getServer());
					d.spawn = pos.immutable();
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Spawn del Limbo: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
							+ ". Ahí llega el que es mandado al Limbo.").withStyle(ChatFormatting.GRAY), true);
					return 1;
				})));
		dispatcher.register(Commands.literal("limbo").requires(s -> s.hasPermission(2))
				.then(Commands.literal("devolver")
						.executes(c -> devolver(c.getSource(), java.util.List.of(c.getSource().getPlayerOrException())))
						.then(Commands.argument("jugadores", EntityArgument.players())
								.executes(c -> devolver(c.getSource(), EntityArgument.getPlayers(c, "jugadores")))))
				.then(Commands.literal("ver").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
					ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
					int n = datos(c.getSource().getServer()).perdidos.getOrDefault(p.getUUID(), 0);
					c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " perdió " + n
							+ (n == 1 ? " corazón" : " corazones") + " en el Limbo.").withStyle(ChatFormatting.GRAY), false);
					return n;
				}))));
	}

	/** Le devuelve todos los corazones que perdió para siempre (lo usa el Corazón del Limbo). */
	public static void devolverCorazones(ServerPlayer p) {
		Datos d = datos(p.server);
		if (d.perdidos.remove(p.getUUID()) != null) d.setDirty();
		aplicar(p, 0);
	}

	private static int devolver(CommandSourceStack fuente, Collection<ServerPlayer> jugadores) {
		Datos d = datos(fuente.getServer());
		for (ServerPlayer p : jugadores) {
			d.perdidos.remove(p.getUUID());
			aplicar(p, 0);
			p.displayClientMessage(Component.literal("Recuperaste tus corazones.").withColor(0xFF5555), true);
		}
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal("Corazones devueltos a " + jugadores.size() + " jugador(es).")
				.withStyle(ChatFormatting.GRAY), true);
		return jugadores.size();
	}
}
