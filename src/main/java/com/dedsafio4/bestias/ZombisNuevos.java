package com.dedsafio4.bestias;

import com.dedsafio4.nave.ModEntidades;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Todos los zombis de Minecraft se convierten en uno de los zombis nuevos (ZombieDedsafioEntity): al aparecer, salir de
 * un spawner o cargarse con el mundo, el zombi se cambia por uno común, del desierto o de nieve (este último, más raro),
 * con la misma armadura, lo mismo en la mano y el mismo nombre. /zombies_nuevos desactivar lo deja de hacer.
 */
public final class ZombisNuevos {
	private ZombisNuevos() {}

	/** Chances: común 50, desierto 50, nieve 15 (de 115). */
	private static final int COMUN = 50, DESIERTO = 50, NIEVE = 15;

	/** Los que hay que cambiar en el próximo tick (no se puede agregar una entidad mientras se está cargando otra). */
	private static final List<Zombie> PENDIENTES = new ArrayList<>();

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean desactivado;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.desactivado = tag.getBoolean("desactivado");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("desactivado", desactivado);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_zombis_nuevos");
	}

	public static void registrar() {
		ServerEntityEvents.ENTITY_LOAD.register((entidad, mundo) -> {
			if (entidad.getType() == EntityType.ZOMBIE && entidad instanceof Zombie z && !datos(mundo.getServer()).desactivado) PENDIENTES.add(z);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDIENTES.isEmpty()) return;
			List<Zombie> ahora = new ArrayList<>(PENDIENTES);
			PENDIENTES.clear();
			for (Zombie z : ahora) cambiar(z);
		});
	}

	private static void cambiar(Zombie viejo) {
		if (viejo.isRemoved() || !viejo.isAlive() || !(viejo.level() instanceof ServerLevel mundo)) return;
		int r = mundo.random.nextInt(COMUN + DESIERTO + NIEVE);
		EntityType<ZombieDedsafioEntity> tipo = r < COMUN ? ModEntidades.ZOMBIE_COMUN
				: r < COMUN + DESIERTO ? ModEntidades.ZOMBIE_DESIERTO : ModEntidades.ZOMBIE_NIEVE;
		ZombieDedsafioEntity nuevo = tipo.create(mundo);
		if (nuevo == null) return;
		nuevo.moveTo(viejo.getX(), viejo.getY(), viejo.getZ(), viejo.getYRot(), viejo.getXRot());
		nuevo.setYHeadRot(viejo.getYHeadRot());
		for (EquipmentSlot lugar : EquipmentSlot.values()) {
			if (lugar.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
			nuevo.setItemSlot(lugar, viejo.getItemBySlot(lugar).copy());
		}
		if (viejo.hasCustomName()) {
			nuevo.setCustomName(viejo.getCustomName());
			nuevo.setCustomNameVisible(viejo.isCustomNameVisible());
		}
		if (viejo.isPersistenceRequired()) nuevo.setPersistenceRequired();
		nuevo.setNoAi(viejo.isNoAi());
		nuevo.setCanPickUpLoot(viejo.canPickUpLoot());
		viejo.discard();
		mundo.addFreshEntity(nuevo);
	}

	/** /zombies_nuevos activar|desactivar (admins). */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("zombies_nuevos").requires(s -> s.hasPermission(2))
				.executes(c -> {
					boolean a = !datos(c.getSource().getServer()).desactivado;
					c.getSource().sendSuccess(() -> Component.literal(a ? "Los zombis de Minecraft se convierten en los zombis nuevos."
							: "Los zombis de Minecraft quedan como siempre.").withStyle(ChatFormatting.GOLD), false);
					return 1;
				})
				.then(Commands.literal("activar").executes(c -> cambiarModo(c.getSource(), true)))
				.then(Commands.literal("desactivar").executes(c -> cambiarModo(c.getSource(), false))));
	}

	private static int cambiarModo(CommandSourceStack fuente, boolean activo) {
		Datos d = datos(fuente.getServer());
		d.desactivado = !activo;
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal(activo ? "Listo: los zombis de Minecraft se convierten en los zombis nuevos."
				: "Listo: los zombis de Minecraft quedan como siempre.").withStyle(ChatFormatting.GOLD), true);
		return 1;
	}
}
