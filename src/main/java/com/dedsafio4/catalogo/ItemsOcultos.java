package com.dedsafio4.catalogo;

import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Los ítems del mod que la G tiene ocultos no se pueden tener a la vista: si un jugador consigue uno (comprándolo,
 * agarrándolo, como sea), en su inventario se convierte en el signo de pregunta, que lo lleva guardado adentro.
 * Cuando ese ítem aparece en la G, el signo de pregunta vuelve a ser el ítem. Los de Minecraft no se tocan.
 */
public final class ItemsOcultos {
	private ItemsOcultos() {}

	/** Dónde va guardado el ítem de verdad, dentro del signo de pregunta. */
	public static final String CLAVE = "dedsafio4_oculto";

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) revisar(server, p);
		});
	}

	private static boolean oculto(MinecraftServer server, ItemStack pila) {
		if (pila.isEmpty() || pila.is(ModItems.SIGNO_INTERROGACION)) return false;
		var id = BuiltInRegistries.ITEM.getKey(pila.getItem());
		return id.getNamespace().equals("dedsafio4") && Catalogo.oculto(server, id.toString());
	}

	/** ¿Es un signo de pregunta con un ítem guardado adentro? */
	public static boolean tieneAdentro(ItemStack pila) {
		if (!pila.is(ModItems.SIGNO_INTERROGACION)) return false;
		CustomData datos = pila.get(DataComponents.CUSTOM_DATA);
		return datos != null && datos.contains(CLAVE);
	}

	private static ItemStack envolver(ItemStack real, HolderLookup.Provider registros) {
		ItemStack signo = new ItemStack(ModItems.SIGNO_INTERROGACION, real.getCount());
		CompoundTag t = new CompoundTag();
		t.put(CLAVE, real.copyWithCount(1).save(registros));
		signo.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
		return signo;
	}

	private static ItemStack desenvolver(ItemStack signo, HolderLookup.Provider registros) {
		Tag guardado = signo.get(DataComponents.CUSTOM_DATA).copyTag().get(CLAVE);
		if (guardado == null) return ItemStack.EMPTY;
		return ItemStack.parse(registros, guardado).map(r -> r.copyWithCount(signo.getCount())).orElse(ItemStack.EMPTY);
	}

	private static void revisar(MinecraftServer server, ServerPlayer p) {
		var registros = server.registryAccess();
		Inventory inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack pila = inv.getItem(i);
			if (oculto(server, pila)) {
				inv.setItem(i, envolver(pila, registros));
			} else if (tieneAdentro(pila)) {
				ItemStack real = desenvolver(pila, registros);
				if (!real.isEmpty() && !oculto(server, real)) inv.setItem(i, real);
			}
		}
	}
}
