package com.dedsafio4.misiones;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.banco.Banco;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Misiones secundarias: cada jugador cobra la recompensa en deditas una sola vez por misión. */
public final class Misiones {
	private Misiones() {}

	/** Misión que se completa al fabricar un ítem por primera vez. */
	public record MisionFabricar(String id, String nombre, Item item, long recompensa) {}

	public static final List<MisionFabricar> FABRICAR = List.of(
			new MisionFabricar("casco_diamante", "Crea un Casco de Diamante", Items.DIAMOND_HELMET, 2),
			new MisionFabricar("pechera_diamante", "Crea una Pechera de Diamante", Items.DIAMOND_CHESTPLATE, 3),
			new MisionFabricar("pantalones_diamante", "Crea unos Pantalones de Diamante", Items.DIAMOND_LEGGINGS, 3),
			new MisionFabricar("botas_diamante", "Crea unas Botas de Diamante", Items.DIAMOND_BOOTS, 2)
	);

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO_MISION = String.valueOf((char) 0xE000);
	private static final String ICONO_DEDITA = String.valueOf((char) 0xE002);

	private static MisionesData data(ServerPlayer jugador) {
		return jugador.server.overworld().getDataStorage().computeIfAbsent(MisionesData.FACTORY, "dedsafio4_misiones");
	}

	public static void alFabricar(ServerPlayer jugador, ItemStack fabricado) {
		for (MisionFabricar mision : FABRICAR) {
			if (fabricado.is(mision.item())) completar(jugador, mision.id(), mision.nombre(), mision.recompensa());
		}
	}

	private static void completar(ServerPlayer jugador, String id, String nombre, long recompensa) {
		if (!data(jugador).completar(jugador.getUUID(), id)) return;
		Banco.sumar(jugador, recompensa);

		Style iconos = Style.EMPTY.withFont(FUENTE_ICONOS);
		jugador.sendSystemMessage(Component.empty()
				.append(Component.literal(ICONO_MISION).withStyle(iconos))
				.append(Component.literal(" Completaste la misión secundaria '").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(nombre).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
				.append(Component.literal("'.").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal("\nHas ingresado " + recompensa + " ").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(ICONO_DEDITA).withStyle(iconos)));
	}
}
