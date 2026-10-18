package com.dedsafio4.bestias;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lo que suelta un mob al morir, agregado a mano (se guarda en el mundo):
 *   /dropsmob <mob> <objeto> <cantidad máxima> <porcentaje>   al morir, con esa probabilidad suelta de 1 a esa cantidad
 *   /dropsmob <mob> quitar <objeto>                             ese objeto no lo suelta más (ni el agregado ni el de siempre)
 *   /dropsmob <mob> normal <objeto>                             ese objeto vuelve a soltarse como siempre
 *   /dropsmob <mob>                                             muestra los drops que tiene
 * Al agregar un objeto que el mob ya soltaba por su cuenta, el de siempre se saca: queda sólo el de /dropsmob (con su
 * porcentaje). Si se vuelve a poner el mismo objeto, se cambia. Lo de siempre se frena en EntityDropsMixin.
 */
public final class DropsMob {
	private DropsMob() {}

	/** Un drop: qué objeto, de 1 a cuántos y con qué probabilidad (0 a 100). */
	private record Drop(String item, int maximo, double porcentaje) {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		/** id del mob → sus drops. */
		final Map<String, List<Drop>> drops = new HashMap<>();
		/** id del mob → los objetos de su drop de siempre que ya no suelta. */
		final Map<String, java.util.Set<String>> sacados = new HashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			CompoundTag sac = tag.getCompound("_sacados");
			for (String mob : sac.getAllKeys()) {
				java.util.Set<String> s = new java.util.HashSet<>();
				for (Tag t : sac.getList(mob, Tag.TAG_STRING)) s.add(t.getAsString());
				d.sacados.put(mob, s);
			}
			for (String mob : tag.getAllKeys()) {
				if (mob.equals("_sacados")) continue;
				List<Drop> l = new ArrayList<>();
				for (Tag t : tag.getList(mob, Tag.TAG_COMPOUND)) {
					CompoundTag c = (CompoundTag) t;
					l.add(new Drop(c.getString("item"), c.getInt("maximo"), c.getDouble("porcentaje")));
				}
				d.drops.put(mob, l);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			drops.forEach((mob, l) -> {
				ListTag lista = new ListTag();
				for (Drop dr : l) {
					CompoundTag c = new CompoundTag();
					c.putString("item", dr.item());
					c.putInt("maximo", dr.maximo());
					c.putDouble("porcentaje", dr.porcentaje());
					lista.add(c);
				}
				tag.put(mob, lista);
			});
			CompoundTag sac = new CompoundTag();
			sacados.forEach((mob, s) -> {
				ListTag lista = new ListTag();
				for (String item : s) lista.add(net.minecraft.nbt.StringTag.valueOf(item));
				sac.put(mob, lista);
			});
			tag.put("_sacados", sac);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_drops_mob");
	}

	/** Mientras se sueltan los drops de /dropsmob (para que no los frene EntityDropsMixin). */
	private static boolean soltandoLosNuestros;

	/**
	 * ¿Este mob (muriendo) no tiene que soltar este objeto de su drop de siempre? Lo llama EntityDropsMixin cada vez
	 * que algo va a soltar un objeto.
	 */
	public static boolean frenar(net.minecraft.world.entity.Entity entidad, ItemStack pila) {
		if (soltandoLosNuestros || pila.isEmpty() || !(entidad instanceof net.minecraft.world.entity.LivingEntity vivo) || vivo.isAlive()
				|| !(entidad.level() instanceof ServerLevel mundo)) return false;
		// menos_perlas 1: los Endermen sueltan Perlas del End un 20% menos.
		if (entidad instanceof net.minecraft.world.entity.monster.EnderMan && pila.is(net.minecraft.world.item.Items.ENDER_PEARL)
				&& com.dedsafio4.cambios.Cambios.nivel(mundo.getServer(), com.dedsafio4.cambios.Cambios.MENOS_PERLAS) >= 1
				&& mundo.getRandom().nextFloat() < 0.2f) return true;
		java.util.Set<String> s = datos(mundo.getServer()).sacados.get(EntityType.getKey(entidad.getType()).toString());
		return s != null && s.contains(BuiltInRegistries.ITEM.getKey(pila.getItem()).toString());
	}

	public static void registrar() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (!(entidad.level() instanceof ServerLevel mundo)) return;
			List<Drop> l = datos(mundo.getServer()).drops.get(EntityType.getKey(entidad.getType()).toString());
			if (l == null) return;
			var azar = entidad.getRandom();
			for (Drop dr : l) {
				if (azar.nextDouble() * 100 >= dr.porcentaje()) continue;
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(dr.item()));
				if (item == Items.AIR) continue;
				int cuantos = 1 + azar.nextInt(Math.max(1, dr.maximo()));
				while (cuantos > 0) {
					int esta = Math.min(cuantos, item.getDefaultMaxStackSize());
					soltandoLosNuestros = true;
					try {
						entidad.spawnAtLocation(new ItemStack(item, esta));
					} finally {
						soltandoLosNuestros = false;
					}
					cuantos -= esta;
				}
			}
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dropsmob").requires(s -> s.hasPermission(2))
				.then(Commands.argument("mob", ResourceLocationArgument.id())
						.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b))
						.executes(DropsMob::mostrar)
						.then(Commands.literal("quitar").then(Commands.argument("objeto", ResourceLocationArgument.id())
								.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), b))
								.executes(DropsMob::quitar)))
						.then(Commands.literal("normal").then(Commands.argument("objeto", ResourceLocationArgument.id())
								.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), b))
								.executes(DropsMob::normal)))
						.then(Commands.argument("objeto", ResourceLocationArgument.id())
								.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), b))
								.then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 6400))
										.then(Commands.argument("porcentaje", DoubleArgumentType.doubleArg(0, 100))
												.executes(DropsMob::agregar))))));
	}

	/** Busca el mob: "zombie" o "minecraft:zombie"; si no es de Minecraft, prueba con los del mod ("walker"). */
	private static EntityType<?> mob(ResourceLocation id) {
		var tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
		if (tipo.isEmpty() && id.getNamespace().equals("minecraft")) {
			tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("dedsafio4", id.getPath()));
		}
		return tipo.orElse(null);
	}

	/** Busca el objeto igual que el mob ("diamond" o "dedsafio4:gema_roja"). */
	private static Item objeto(ResourceLocation id) {
		var item = BuiltInRegistries.ITEM.getOptional(id);
		if (item.isEmpty() && id.getNamespace().equals("minecraft")) {
			item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("dedsafio4", id.getPath()));
		}
		return item.filter(i -> i != Items.AIR).orElse(null);
	}

	private static int agregar(CommandContext<CommandSourceStack> c) {
		EntityType<?> tipo = mob(ResourceLocationArgument.getId(c, "mob"));
		Item item = objeto(ResourceLocationArgument.getId(c, "objeto"));
		if (tipo == null || item == null) {
			c.getSource().sendFailure(Component.literal(tipo == null ? "No existe ese mob." : "No existe ese objeto."));
			return 0;
		}
		int maximo = IntegerArgumentType.getInteger(c, "cantidad");
		double porcentaje = DoubleArgumentType.getDouble(c, "porcentaje");
		String idItem = BuiltInRegistries.ITEM.getKey(item).toString();
		Datos d = datos(c.getSource().getServer());
		List<Drop> l = d.drops.computeIfAbsent(EntityType.getKey(tipo).toString(), k -> new ArrayList<>());
		l.removeIf(dr -> dr.item().equals(idItem));
		l.add(new Drop(idItem, maximo, porcentaje));
		// Si ya lo soltaba por su cuenta, ése se saca: queda sólo éste.
		d.sacados.computeIfAbsent(EntityType.getKey(tipo).toString(), k -> new java.util.HashSet<>()).add(idItem);
		d.setDirty();
		c.getSource().sendSuccess(() -> Component.literal(tipo.getDescription().getString() + " ahora suelta ")
				.append(new ItemStack(item).getHoverName())
				.append(Component.literal(" (de 1 a " + maximo + ", " + porcentaje + "% de las veces).")).withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	/** Ese objeto ya no lo suelta: ni el agregado con /dropsmob ni el de siempre. */
	private static int quitar(CommandContext<CommandSourceStack> c) {
		EntityType<?> tipo = mob(ResourceLocationArgument.getId(c, "mob"));
		Item item = objeto(ResourceLocationArgument.getId(c, "objeto"));
		if (tipo == null || item == null) {
			c.getSource().sendFailure(Component.literal(tipo == null ? "No existe ese mob." : "No existe ese objeto."));
			return 0;
		}
		Datos d = datos(c.getSource().getServer());
		String idMob = EntityType.getKey(tipo).toString(), idItem = BuiltInRegistries.ITEM.getKey(item).toString();
		List<Drop> l = d.drops.get(idMob);
		if (l != null) {
			l.removeIf(dr -> dr.item().equals(idItem));
			if (l.isEmpty()) d.drops.remove(idMob);
		}
		d.sacados.computeIfAbsent(idMob, k -> new java.util.HashSet<>()).add(idItem);
		d.setDirty();
		c.getSource().sendSuccess(() -> Component.literal(tipo.getDescription().getString() + " ya no suelta ")
				.append(new ItemStack(item).getHoverName()).append(Component.literal(".")).withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	/** Ese objeto vuelve a soltarse como siempre (y se saca el agregado con /dropsmob, si había). */
	private static int normal(CommandContext<CommandSourceStack> c) {
		EntityType<?> tipo = mob(ResourceLocationArgument.getId(c, "mob"));
		Item item = objeto(ResourceLocationArgument.getId(c, "objeto"));
		if (tipo == null || item == null) {
			c.getSource().sendFailure(Component.literal(tipo == null ? "No existe ese mob." : "No existe ese objeto."));
			return 0;
		}
		Datos d = datos(c.getSource().getServer());
		String idMob = EntityType.getKey(tipo).toString(), idItem = BuiltInRegistries.ITEM.getKey(item).toString();
		List<Drop> l = d.drops.get(idMob);
		if (l != null) {
			l.removeIf(dr -> dr.item().equals(idItem));
			if (l.isEmpty()) d.drops.remove(idMob);
		}
		java.util.Set<String> s = d.sacados.get(idMob);
		if (s != null) {
			s.remove(idItem);
			if (s.isEmpty()) d.sacados.remove(idMob);
		}
		d.setDirty();
		c.getSource().sendSuccess(() -> Component.literal(tipo.getDescription().getString() + " vuelve a soltar ")
				.append(new ItemStack(item).getHoverName()).append(Component.literal(" como siempre.")).withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	private static int mostrar(CommandContext<CommandSourceStack> c) {
		EntityType<?> tipo = mob(ResourceLocationArgument.getId(c, "mob"));
		if (tipo == null) {
			c.getSource().sendFailure(Component.literal("No existe ese mob."));
			return 0;
		}
		List<Drop> l = datos(c.getSource().getServer()).drops.getOrDefault(EntityType.getKey(tipo).toString(), List.of());
		java.util.Set<String> sacados = datos(c.getSource().getServer()).sacados.getOrDefault(EntityType.getKey(tipo).toString(), java.util.Set.of());
		for (String item : sacados) {
			if (l.stream().anyMatch(dr -> dr.item().equals(item))) continue;
			c.getSource().sendSuccess(() -> Component.literal("• " + item + ": no lo suelta").withStyle(ChatFormatting.GRAY), false);
		}
		if (l.isEmpty()) {
			if (!sacados.isEmpty()) return 0;
			c.getSource().sendSuccess(() -> Component.literal(tipo.getDescription().getString() + " no tiene drops agregados.").withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		for (Drop dr : l) {
			c.getSource().sendSuccess(() -> Component.literal("• " + dr.item() + ": de 1 a " + dr.maximo() + ", " + dr.porcentaje() + "%")
					.withStyle(ChatFormatting.AQUA), false);
		}
		return l.size();
	}
}
