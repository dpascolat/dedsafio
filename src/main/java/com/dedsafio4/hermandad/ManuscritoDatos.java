package com.dedsafio4.hermandad;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lo que está escrito en un Manuscrito de Hermandad. Se guarda en el propio ítem,
 * así el libro se puede pasar de mano en mano para que firmen los demás.
 */
public final class ManuscritoDatos {
	private ManuscritoDatos() {}

	public static final int MINIMO_INSCRITOS = 3;
	public static final int MAXIMO_INSCRITOS = 10;
	public static final int LARGO_MAXIMO_NOMBRE = 16;

	public record Inscrito(UUID uuid, String nombre) {}

	private static CompoundTag leer(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	private static void escribir(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static String nombre(ItemStack stack) {
		return leer(stack).getString("nombre");
	}

	public static List<Inscrito> inscritos(ItemStack stack) {
		List<Inscrito> lista = new ArrayList<>();
		for (Tag t : leer(stack).getList("inscritos", Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag) t;
			lista.add(new Inscrito(c.getUUID("uuid"), c.getString("nombre")));
		}
		return lista;
	}

	public static boolean estaInscrito(ItemStack stack, UUID jugador) {
		return inscritos(stack).stream().anyMatch(i -> i.uuid().equals(jugador));
	}

	/** Sellado = ya se apretó "Crear Hermandad"; no se puede editar y queda listo para /guild found. */
	public static boolean sellado(ItemStack stack) {
		return leer(stack).getBoolean("sellado");
	}

	public static void sellar(ItemStack stack) {
		CompoundTag tag = leer(stack);
		tag.putBoolean("sellado", true);
		escribir(stack, tag);
	}

	public static void setNombre(ItemStack stack, String nombre) {
		CompoundTag tag = leer(stack);
		tag.putString("nombre", nombre);
		escribir(stack, tag);
	}

	public static void inscribir(ItemStack stack, UUID uuid, String nombre) {
		CompoundTag tag = leer(stack);
		ListTag lista = tag.getList("inscritos", Tag.TAG_COMPOUND);
		CompoundTag c = new CompoundTag();
		c.putUUID("uuid", uuid);
		c.putString("nombre", nombre);
		lista.add(c);
		tag.put("inscritos", lista);
		escribir(stack, tag);
	}

	/** Recorta y limpia el nombre que escribió el jugador. */
	public static String limpiarNombre(String nombre) {
		String limpio = nombre.strip().replaceAll("\\s+", " ");
		return limpio.length() > LARGO_MAXIMO_NOMBRE ? limpio.substring(0, LARGO_MAXIMO_NOMBRE) : limpio;
	}
}
