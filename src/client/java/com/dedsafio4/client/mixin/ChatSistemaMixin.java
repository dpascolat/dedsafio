package com.dedsafio4.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Algunos mods o plugins del servidor mandan el chat de los jugadores como mensaje del sistema, con el formato de
 * Minecraft ("<Nombre> mensaje"). Si es un jugador conectado, se vuelve a armar con el formato del Dedsafío.
 */
@Mixin(ChatListener.class)
public abstract class ChatSistemaMixin {
	private static final Pattern CON_FLECHAS = Pattern.compile("^<([A-Za-z0-9_]{1,16})> (.*)$", Pattern.DOTALL);

	@ModifyVariable(method = "handleSystemMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component dedsafio4$sinFlechas(Component mensaje) {
		Matcher m = CON_FLECHAS.matcher(mensaje.getString());
		if (!m.matches()) return mensaje;
		var conexion = Minecraft.getInstance().getConnection();
		if (conexion == null || conexion.getPlayerInfo(m.group(1)) == null) return mensaje;
		return Component.translatable("chat.type.dedsafio4",
				Component.literal(m.group(1)).withStyle(s -> s.withColor(com.dedsafio4.chat.ColoresChat.colorCliente(m.group(1)))), Component.literal(m.group(2)));
	}
}
