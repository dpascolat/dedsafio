package com.dedsafio4.misiones;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.banco.Banco;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/** El cobro de las Misiones (las del Catálogo): cada jugador cobra la recompensa en deditas una sola vez por misión. */
public final class Misiones {
	private Misiones() {}

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO_MISION = String.valueOf((char) 0xE000);
	private static final String ICONO_DEDITA = String.valueOf((char) 0xE002);

	private static MisionesData data(ServerPlayer jugador) {
		return jugador.server.overworld().getDataStorage().computeIfAbsent(MisionesData.FACTORY, "dedsafio4_misiones");
	}

	public static boolean completada(ServerPlayer jugador, String id) {
		return data(jugador).tiene(jugador.getUUID(), id);
	}

	/** La marca como cumplida, le da las deditas y le avisa en el chat. Devuelve false si ya la había cumplido. */
	public static boolean completar(ServerPlayer jugador, String id, boolean principal, Item item, long recompensa) {
		if (!data(jugador).completar(jugador.getUUID(), id)) return false;
		if (recompensa > 0) Banco.sumar(jugador, recompensa);

		Style iconos = Style.EMPTY.withFont(FUENTE_ICONOS);
		Component mensaje = Component.empty()
				.append(Component.literal(ICONO_MISION).withStyle(iconos))
				.append(Component.literal(principal ? " Completaste la misión principal '" : " Completaste la misión guía '").withStyle(ChatFormatting.YELLOW))
				.append(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
				.append(Component.literal("'.").withStyle(ChatFormatting.YELLOW));
		if (recompensa > 0) {
			mensaje = Component.empty().append(mensaje)
					.append(Component.literal("\nHas ingresado " + recompensa + " ").withStyle(ChatFormatting.YELLOW))
					.append(Component.literal(ICONO_DEDITA).withStyle(iconos));
		}
		jugador.sendSystemMessage(mensaje);
		return true;
	}
}
