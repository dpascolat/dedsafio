package com.dedsafio4.robots;

import com.dedsafio4.bloques.ModBloques;
import com.dedsafio4.items.ModItems;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;

/**
 * Los comercios del Aldeano Phora. Cada aldeano tiene una de estas listas (al azar, para siempre). Se paga casi
 * todo con Dilitio. Tiene 7 niveles: los comercios de un nivel más alto se ven en la lista pero están bloqueados
 * (piden una Barrera "Nivel N", que nadie tiene) hasta que el aldeano sube a ese nivel comerciando.
 */
public final class OfertasPhora {
	private OfertasPhora() {}

	public static final int NIVEL_MAXIMO = 7;
	/** Cuánta experiencia hace falta para llegar a cada nivel (1 a 7). Los primeros 5 son los de Minecraft. */
	public static final int[] XP_NIVEL = {0, 0, 10, 70, 150, 250, 400, 600};
	/** Cuántas veces se puede hacer cada comercio antes de que el aldeano tenga que reponer. */
	private static final int USOS = 12;

	/** Un comercio: nivel, lo que pide (ítem y cantidad) y lo que da. */
	public record Comercio(int nivel, Item pide, int cuanto, Item da, int cuantoDa) {
		MerchantOffer oferta() {
			return new MerchantOffer(new ItemCost(pide, cuanto), new ItemStack(da, cuantoDa), USOS, xp(nivel), 0.05f);
		}

		/** Bloqueado: pide además una Barrera llamada "Nivel N" (con N de cantidad) y está agotado. */
		MerchantOffer bloqueada() {
			ItemCost candado = new ItemCost(Items.BARRIER.builtInRegistryHolder(), nivel,
					DataComponentPredicate.builder().expect(DataComponents.CUSTOM_NAME, Component.literal("Nivel " + nivel)).build());
			MerchantOffer o = new MerchantOffer(new ItemCost(pide, cuanto), java.util.Optional.of(candado),
					new ItemStack(da, cuantoDa), 0, 0, 0, 0f);
			o.setToOutOfStock();
			return o;
		}
	}

	/** Experiencia que da cada comercio según su nivel. */
	private static int xp(int nivel) {
		return switch (nivel) {
			case 1 -> 2;
			case 2 -> 5;
			case 3 -> 10;
			case 4 -> 15;
			case 5 -> 20;
			default -> 30;
		};
	}

	private static Comercio c(int nivel, Item pide, int cuanto, Item da, int cuantoDa) {
		return new Comercio(nivel, pide, cuanto, da, cuantoDa);
	}

	private static Comercio dilitio(int nivel, Item pide, int cuanto, int cuantos) {
		return c(nivel, pide, cuanto, ModItems.DILITIO, cuantos);
	}

	/** Las listas posibles (las de las capturas del Dedsafío). */
	public static final List<List<Comercio>> JUEGOS = List.of(
			List.of(
					dilitio(1, Items.STICK, 62, 1),
					dilitio(2, Items.COAL, 6, 1),
					dilitio(2, Items.CHARCOAL, 14, 1),
					dilitio(3, Items.LEATHER, 9, 1),
					dilitio(3, ModItems.CRISTAL_VERDE, 3, 1),
					dilitio(4, Items.OBSIDIAN, 4, 1),
					dilitio(4, ModItems.SANGRE_REPTISAURIO, 6, 1),
					dilitio(5, Items.DIAMOND, 4, 2),
					dilitio(5, ModItems.SIGNO_INTERROGACION, 1, 8),
					c(6, ModItems.DEDITA, 25, Items.DIAMOND_SWORD, 1),
					c(7, ModItems.DEDITA, 1, Items.GOLDEN_CARROT, 3)),
			List.of(
					dilitio(1, Items.STICK, 64, 1),
					dilitio(2, Items.SWEET_BERRIES, 8, 1),
					dilitio(2, Items.MELON_SLICE, 16, 1),
					dilitio(3, ModItems.FIBRA_GLEBANOIDE, 19, 1),
					dilitio(3, Items.PUMPKIN, 4, 1),
					dilitio(4, ModItems.ESCUPITAJO_DACTYLO, 8, 1),
					dilitio(4, ModBloques.BLOQUE_DIENTES_ITEM, 8, 1),
					dilitio(5, ModItems.PIMPOLLO_QUMARA, 1, 4),
					dilitio(5, ModBloques.ROBLE_CLARO_LOG_ITEM, 4, 1),
					c(6, ModItems.DEDITA, 17, Items.DIAMOND_LEGGINGS, 1),
					c(7, ModItems.DEDITA, 17, ModItems.SIGNO_INTERROGACION, 3)),
			List.of(
					dilitio(1, Items.STICK, 48, 1),
					dilitio(2, Items.COAL, 5, 1),
					dilitio(2, Items.GLOW_BERRIES, 60, 1),
					dilitio(3, ModItems.HUEVO_EBURIA, 3, 1),
					dilitio(3, ModItems.PELO_EBURIA, 5, 1),
					dilitio(4, ModBloques.BLOQUE_DIENTES_ITEM, 8, 1),
					dilitio(4, Items.AMETHYST_SHARD, 29, 1),
					dilitio(5, ModBloques.ROBLE_CLARO_LOG_ITEM, 5, 1),
					dilitio(5, ModItems.EXCREMENTO, 3, 3),
					c(6, ModItems.DEDITA, 26, Items.DIAMOND_SWORD, 1),
					c(7, ModItems.DEDITA, 1, Items.GOLDEN_CARROT, 3)),
			List.of(
					dilitio(1, Items.STICK, 64, 1),
					dilitio(1, Items.KELP, 16, 1),
					dilitio(2, Items.COAL, 9, 1),
					dilitio(2, ModItems.CRISTAL_VERDE, 2, 1),
					dilitio(3, Items.LEATHER, 8, 1),
					dilitio(3, ModItems.JERINGA, 2, 1),
					dilitio(4, ModItems.ESCUPITAJO_DACTYLO, 9, 1),
					c(4, ModBloques.ROBLE_CLARO_LOG_ITEM, 26, ModItems.CRISTAL_VERDE, 1),
					dilitio(5, ModItems.AMBAR, 3, 1),
					dilitio(5, ModItems.SANGRE_REPTISAURIO, 4, 1),
					dilitio(5, ModItems.EXCREMENTO, 3, 3),
					dilitio(5, Items.OBSIDIAN, 1, 3),
					c(6, ModItems.DEDITA, 20, Items.DIAMOND_AXE, 1),
					c(7, ModItems.DEDITA, 2, Items.ENDER_PEARL, 1)));
}
