package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * Cuando a alguien le salta un tótem del mod (todos menos el Nutritótem, el de Minecraft) sale en el chat de todos:
 * "(cara) Jugador ha consumido un Tótem de Eón". La cara la dibuja el cliente (ChatCaraMixin): el mensaje empieza
 * con {@link #MARCA} en la fuente "cara" (un hueco del ancho de la cara) y el nombre del jugador va guardado en ese
 * carácter.
 */
public final class AvisoTotem {
	private AvisoTotem() {}

	public static final ResourceLocation FUENTE_CARA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cara");
	public static final String MARCA = "";
	private static final int AMARILLO = 0xFFFF55;

	public static void consumido(ServerPlayer jugador, Item totem) {
		String nombre = jugador.getGameProfile().getName();
		Component mensaje = Component.empty()
				.append(Component.literal(MARCA).withStyle(Style.EMPTY.withFont(FUENTE_CARA).withInsertion(nombre)))
				.append(Component.literal(" " + nombre).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AMARILLO)).withBold(true)))
				.append(Component.literal(" ha consumido un ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AMARILLO))))
				.append(totem.getDescription().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AMARILLO))));
		jugador.server.getPlayerList().broadcastSystemMessage(mensaje, false);
	}
}
