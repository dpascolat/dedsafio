package com.dedsafio4.bestias;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que hace el Zombie de Nieve al pegarte: uno de los casilleros de la barra queda bloqueado un rato (2 minutos) con
 * una barrera que no se puede sacar. Lo que había en ese casillero pasa a otro lugar del inventario, y si no hay lugar
 * se cae al piso. /zombienieve espera|duracion <segundos> cambia cada cuánto puede hacerlo cada zombi y cuánto dura.
 */
public final class SlotsBloqueados {
	private SlotsBloqueados() {}

	private static final String MARCA = "dedsafio4_slot_bloqueado";

	private record Bloqueo(int casillero, long hasta) {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Map<UUID, List<Bloqueo>> bloqueos = new HashMap<>();
		int espera = 30, duracion = 120;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("espera")) d.espera = tag.getInt("espera");
			if (tag.contains("duracion")) d.duracion = tag.getInt("duracion");
			CompoundTag b = tag.getCompound("bloqueos");
			for (String k : b.getAllKeys()) {
				List<Bloqueo> l = new ArrayList<>();
				for (Tag t : b.getList(k, Tag.TAG_COMPOUND)) l.add(new Bloqueo(((CompoundTag) t).getInt("c"), ((CompoundTag) t).getLong("h")));
				d.bloqueos.put(UUID.fromString(k), l);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("espera", espera);
			tag.putInt("duracion", duracion);
			CompoundTag b = new CompoundTag();
			bloqueos.forEach((k, l) -> {
				ListTag lista = new ListTag();
				for (Bloqueo x : l) {
					CompoundTag t = new CompoundTag();
					t.putInt("c", x.casillero());
					t.putLong("h", x.hasta());
					lista.add(t);
				}
				b.put(k.toString(), lista);
			});
			tag.put("bloqueos", b);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_slots_bloqueados");
	}

	/** Cada cuánto (en ticks) puede bloquear cada Zombie de Nieve. */
	public static long espera(MinecraftServer server) {
		return datos(server).espera * 20L;
	}

	private static ItemStack barrera() {
		ItemStack b = new ItemStack(Items.BARRIER);
		CompoundTag t = new CompoundTag();
		t.putBoolean(MARCA, true);
		b.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
		b.set(DataComponents.CUSTOM_NAME, Component.literal("Casillero congelado").withStyle(ChatFormatting.AQUA));
		return b;
	}

	private static boolean esBarrera(ItemStack pila) {
		if (!pila.is(Items.BARRIER)) return false;
		CustomData d = pila.get(DataComponents.CUSTOM_DATA);
		return d != null && d.copyTag().getBoolean(MARCA);
	}

	/** Bloquea un casillero libre (que no esté ya bloqueado) de la barra del jugador. */
	public static void bloquear(ServerPlayer jugador) {
		if (jugador.isCreative() || jugador.isSpectator()) return;
		Datos d = datos(jugador.server);
		List<Bloqueo> l = d.bloqueos.computeIfAbsent(jugador.getUUID(), k -> new ArrayList<>());
		List<Integer> libres = new ArrayList<>();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			int c = i;
			if (l.stream().noneMatch(b -> b.casillero() == c)) libres.add(i);
		}
		if (libres.isEmpty()) return;
		int casillero = libres.get(jugador.getRandom().nextInt(libres.size()));
		l.add(new Bloqueo(casillero, jugador.server.overworld().getGameTime() + d.duracion * 20L));
		d.setDirty();
		ponerBarrera(jugador, casillero);
		jugador.displayClientMessage(Component.literal("❄ ¡Un casillero se congeló por " + d.duracion + " segundos!")
				.withStyle(ChatFormatting.AQUA), true);
	}

	/** Pone la barrera en el casillero; lo que había ahí pasa a otro lugar o se cae. */
	private static void ponerBarrera(ServerPlayer jugador, int casillero) {
		Inventory inv = jugador.getInventory();
		ItemStack habia = inv.getItem(casillero);
		inv.setItem(casillero, barrera());
		if (!habia.isEmpty() && !esBarrera(habia) && !inv.add(habia) && !habia.isEmpty()) jugador.drop(habia, false);
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Datos d = datos(server);
			if (d.bloqueos.isEmpty()) return;
			long ahora = server.overworld().getGameTime();
			for (var it = d.bloqueos.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				ServerPlayer jugador = server.getPlayerList().getPlayer(e.getKey());
				if (jugador == null) continue;   // desconectado: se arregla cuando vuelva
				Inventory inv = jugador.getInventory();
				for (var b = e.getValue().iterator(); b.hasNext(); ) {
					Bloqueo x = b.next();
					if (ahora >= x.hasta()) {
						if (esBarrera(inv.getItem(x.casillero()))) inv.setItem(x.casillero(), ItemStack.EMPTY);
						b.remove();
						d.setDirty();
					} else if (!esBarrera(inv.getItem(x.casillero()))) {
						ponerBarrera(jugador, x.casillero());   // la movió o puso algo encima: vuelve
					}
				}
				// Barreras fuera de su lugar (en otro casillero, en la mano del mouse o en un cofre abierto): se borran.
				List<Integer> mios = e.getValue().stream().map(Bloqueo::casillero).toList();
				for (int i = 0; i < inv.getContainerSize(); i++) {
					if (!mios.contains(i) && esBarrera(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
				}
				if (esBarrera(jugador.containerMenu.getCarried())) jugador.containerMenu.setCarried(ItemStack.EMPTY);
				for (Slot s : jugador.containerMenu.slots) {
					if (s.container != inv && esBarrera(s.getItem())) s.set(ItemStack.EMPTY);
				}
				if (e.getValue().isEmpty()) {
					it.remove();
					d.setDirty();
				}
			}
		});
		// Si una barrera se cae al piso (al tirarla o al morir), desaparece.
		ServerEntityEvents.ENTITY_LOAD.register((entidad, mundo) -> {
			if (entidad instanceof ItemEntity item && esBarrera(item.getItem())) item.discard();
		});
		// No se puede poner como bloque.
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((jugador, mundo, mano, golpe) ->
				esBarrera(jugador.getItemInHand(mano)) ? InteractionResult.FAIL : InteractionResult.PASS);
	}

	/** /zombienieve espera <segundos> | duracion <segundos> (admins). */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("zombienieve").requires(s -> s.hasPermission(2))
				.executes(c -> {
					Datos d = datos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal("Zombie de Nieve: bloquea un casillero por " + d.duracion
							+ " s y espera " + d.espera + " s para volver a hacerlo.").withStyle(ChatFormatting.AQUA), false);
					return 1;
				})
				.then(Commands.literal("espera").then(Commands.argument("segundos", IntegerArgumentType.integer(0, 3600))
						.executes(c -> {
							Datos d = datos(c.getSource().getServer());
							d.espera = IntegerArgumentType.getInteger(c, "segundos");
							d.setDirty();
							c.getSource().sendSuccess(() -> Component.literal("Listo: el Zombie de Nieve espera " + d.espera
									+ " s entre bloqueos.").withStyle(ChatFormatting.AQUA), true);
							return 1;
						})))
				.then(Commands.literal("duracion").then(Commands.argument("segundos", IntegerArgumentType.integer(1, 3600))
						.executes(c -> {
							Datos d = datos(c.getSource().getServer());
							d.duracion = IntegerArgumentType.getInteger(c, "segundos");
							d.setDirty();
							c.getSource().sendSuccess(() -> Component.literal("Listo: el casillero queda bloqueado " + d.duracion
									+ " s.").withStyle(ChatFormatting.AQUA), true);
							return 1;
						}))));
	}
}
