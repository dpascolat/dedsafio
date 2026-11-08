package com.dedsafio4.recoger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /item <x y z> <tamaño> <cantidad> <veces por jugador> <segundos>: en ese lugar flota el ítem que tienes en la mano.
 * Con clic derecho un jugador recibe esa cantidad y el ítem desaparece; vuelve a aparecer a los segundos que se
 * pusieron. Cada jugador lo puede recoger como mucho "veces por jugador" veces.
 * /item quitar: saca el más cercano (a 5 bloques). /item lista: los que hay.
 * El ítem que se ve es una "item_display" y el clic lo recibe una "interaction" (entidades de Minecraft, con la marca
 * dedsafio4_recoger_<número>). Todo queda guardado en el mundo.
 */
public final class ItemsRecoger {
	private ItemsRecoger() {}

	private static final String MARCA = "dedsafio4_recoger";

	static final class Punto {
		int id;
		ResourceKey<Level> dimension;
		BlockPos pos;
		ItemStack item = ItemStack.EMPTY;
		float tamanio;
		int cantidad, veces, segundos;
		/** Cuándo vuelve a aparecer (tiempo del juego); 0 = está a la vista. */
		long reaparece;
		final Map<UUID, Integer> recogidas = new HashMap<>();

		String marca() {
			return MARCA + "_" + id;
		}
	}

	static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final List<Punto> puntos = new ArrayList<>();
		int siguiente = 1;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.siguiente = Math.max(1, tag.getInt("siguiente"));
			for (Tag t : tag.getList("puntos", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				Punto p = new Punto();
				p.id = c.getInt("id");
				p.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dimension")));
				p.pos = BlockPos.of(c.getLong("pos"));
				p.item = ItemStack.parseOptional(registros, c.getCompound("item"));
				p.tamanio = c.getFloat("tamanio");
				p.cantidad = c.getInt("cantidad");
				p.veces = c.getInt("veces");
				p.segundos = c.getInt("segundos");
				p.reaparece = c.getLong("reaparece");
				CompoundTag r = c.getCompound("recogidas");
				for (String k : r.getAllKeys()) p.recogidas.put(UUID.fromString(k), r.getInt(k));
				d.puntos.add(p);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("siguiente", siguiente);
			ListTag lista = new ListTag();
			for (Punto p : puntos) {
				CompoundTag c = new CompoundTag();
				c.putInt("id", p.id);
				c.putString("dimension", p.dimension.location().toString());
				c.putLong("pos", p.pos.asLong());
				c.put("item", p.item.saveOptional(registros));
				c.putFloat("tamanio", p.tamanio);
				c.putInt("cantidad", p.cantidad);
				c.putInt("veces", p.veces);
				c.putInt("segundos", p.segundos);
				c.putLong("reaparece", p.reaparece);
				CompoundTag r = new CompoundTag();
				p.recogidas.forEach((u, n) -> r.putInt(u.toString(), n));
				c.put("recogidas", r);
				lista.add(c);
			}
			tag.put("puntos", lista);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_items_recoger");
	}

	public static void registrar() {
		// Cada segundo: los que ya pueden volver, vuelven a aparecer.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			Datos d = datos(server);
			for (Punto p : d.puntos) {
				if (p.reaparece == 0) continue;
				ServerLevel mundo = server.getLevel(p.dimension);
				if (mundo == null || mundo.getGameTime() < p.reaparece || !mundo.isLoaded(p.pos)) continue;
				p.reaparece = 0;
				d.setDirty();
				mostrar(mundo, p);
			}
		});
		// Clic derecho en el ítem: lo recoge.
		UseEntityCallback.EVENT.register((jugador, level, mano, entidad, golpe) -> {
			if (!(entidad instanceof Interaction) || !(jugador instanceof ServerPlayer servidor)) return InteractionResult.PASS;
			if (!entidad.getTags().contains(MARCA)) return InteractionResult.PASS;
			Datos d = datos(servidor.server);
			for (Punto p : d.puntos) {
				if (entidad.getTags().contains(p.marca())) {
					recoger(servidor, d, p);
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});
	}

	private static void recoger(ServerPlayer jugador, Datos d, Punto p) {
		if (p.reaparece != 0) return;   // todavía no volvió a aparecer
		int ya = p.recogidas.getOrDefault(jugador.getUUID(), 0);
		if (ya >= p.veces) {
			jugador.displayClientMessage(Component.literal(p.veces == 1 ? "Ya recogiste este ítem."
					: "Ya recogiste este ítem " + p.veces + " veces.").withStyle(ChatFormatting.RED), true);
			return;
		}
		// Le da la cantidad (en pilas, si es más de lo que entra en una); lo que no entra cae al piso.
		int falta = p.cantidad;
		while (falta > 0) {
			ItemStack pila = p.item.copyWithCount(Math.min(falta, p.item.getMaxStackSize()));
			falta -= pila.getCount();
			if (!jugador.getInventory().add(pila)) jugador.drop(pila, false);
		}
		jugador.inventoryMenu.broadcastChanges();
		p.recogidas.put(jugador.getUUID(), ya + 1);
		ServerLevel mundo = jugador.serverLevel();
		mundo.playSound(null, p.pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 1f);
		// Desaparece y vuelve a aparecer a los segundos que se pusieron.
		p.reaparece = mundo.getGameTime() + Math.max(1, p.segundos * 20L);
		d.setDirty();
		quitarEntidades(mundo, p, true);
	}

	/** Pone el ítem flotando (y el lugar donde se hace clic). */
	private static void mostrar(ServerLevel mundo, Punto p) {
		quitarEntidades(mundo, p, false);
		double x = p.pos.getX() + 0.5, y = p.pos.getY() + 0.5, z = p.pos.getZ() + 0.5;
		String item = p.item.copyWithCount(1).save(mundo.registryAccess()).toString();
		float s = p.tamanio;
		comando(mundo, String.format(Locale.ROOT,
				"summon minecraft:item_display %.3f %.3f %.3f {Tags:[\"%s\",\"%s\"],billboard:\"vertical\",item:%s,"
						+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[%sf,%sf,%sf]}}",
				x, y, z, MARCA, p.marca(), item, s, s, s));
		float lado = Math.max(0.5f, s * 0.6f);
		comando(mundo, String.format(Locale.ROOT,
				"summon minecraft:interaction %.3f %.3f %.3f {Tags:[\"%s\",\"%s\"],width:%sf,height:%sf,response:1b}",
				x, y - lado / 2, z, MARCA, p.marca(), lado, lado));
	}

	/** Saca el ítem que se ve (y, si soloElItem es falso, también el lugar del clic). */
	private static void quitarEntidades(ServerLevel mundo, Punto p, boolean soloElItem) {
		List<Entity> sacar = new ArrayList<>();
		for (Entity e : mundo.getAllEntities()) {
			if (!e.getTags().contains(p.marca())) continue;
			if (soloElItem && e instanceof Interaction) continue;
			sacar.add(e);
		}
		sacar.forEach(Entity::discard);
	}

	private static void comando(ServerLevel mundo, String comando) {
		CommandSourceStack fuente = mundo.getServer().createCommandSourceStack().withLevel(mundo).withPermission(4).withSuppressedOutput();
		mundo.getServer().getCommands().performPrefixedCommand(fuente, comando);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("item").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar").executes(ItemsRecoger::quitar))
				.then(Commands.literal("lista").executes(ItemsRecoger::lista))
				.then(Commands.argument("lugar", BlockPosArgument.blockPos())
						.then(Commands.argument("tamaño", FloatArgumentType.floatArg(0.1f, 20f))
								.then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 6400))
										.then(Commands.argument("veces_por_jugador", IntegerArgumentType.integer(1))
												.then(Commands.argument("segundos", IntegerArgumentType.integer(0))
														.executes(ItemsRecoger::poner)))))));
	}

	private static int poner(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer jugador = c.getSource().getPlayerOrException();
		ItemStack mano = jugador.getMainHandItem();
		if (mano.isEmpty()) {
			c.getSource().sendFailure(Component.literal("Ponte en la mano el ítem que quieres que aparezca."));
			return 0;
		}
		Datos d = datos(c.getSource().getServer());
		Punto p = new Punto();
		p.id = d.siguiente++;
		p.dimension = jugador.level().dimension();
		p.pos = BlockPosArgument.getBlockPos(c, "lugar");
		p.item = mano.copyWithCount(1);
		p.tamanio = FloatArgumentType.getFloat(c, "tamaño");
		p.cantidad = IntegerArgumentType.getInteger(c, "cantidad");
		p.veces = IntegerArgumentType.getInteger(c, "veces_por_jugador");
		p.segundos = IntegerArgumentType.getInteger(c, "segundos");
		d.puntos.add(p);
		d.setDirty();
		mostrar(jugador.serverLevel(), p);
		c.getSource().sendSuccess(() -> Component.literal("Listo: en " + p.pos.toShortString() + " hay " + p.cantidad + " de ")
				.append(p.item.getHoverName())
				.append(" para recoger (" + p.veces + (p.veces == 1 ? " vez" : " veces") + " por jugador; vuelve a los "
						+ p.segundos + " segundos).").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int quitar(CommandContext<CommandSourceStack> c) {
		Datos d = datos(c.getSource().getServer());
		ServerLevel mundo = c.getSource().getLevel();
		Punto cerca = null;
		double mejor = 5 * 5;
		for (Punto p : d.puntos) {
			if (p.dimension != mundo.dimension()) continue;
			double dist = p.pos.getCenter().distanceToSqr(c.getSource().getPosition());
			if (dist <= mejor) {
				mejor = dist;
				cerca = p;
			}
		}
		if (cerca == null) {
			c.getSource().sendFailure(Component.literal("No hay ningún ítem para recoger a menos de 5 bloques."));
			return 0;
		}
		quitarEntidades(mundo, cerca, false);
		d.puntos.remove(cerca);
		d.setDirty();
		Punto quitado = cerca;
		c.getSource().sendSuccess(() -> Component.literal("Listo: se quitó el ítem de " + quitado.pos.toShortString() + ".")
				.withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int lista(CommandContext<CommandSourceStack> c) {
		Datos d = datos(c.getSource().getServer());
		if (d.puntos.isEmpty()) {
			c.getSource().sendSuccess(() -> Component.literal("No hay ítems para recoger.").withStyle(ChatFormatting.GOLD), false);
			return 0;
		}
		for (Punto p : d.puntos) {
			c.getSource().sendSuccess(() -> Component.literal("• " + p.pos.toShortString() + " (" + p.dimension.location().getPath() + "): "
					+ p.cantidad + " de ").append(p.item.getHoverName())
					.append(", " + p.veces + " por jugador, vuelve a los " + p.segundos + " s").withStyle(ChatFormatting.GOLD), false);
		}
		return d.puntos.size();
	}
}
