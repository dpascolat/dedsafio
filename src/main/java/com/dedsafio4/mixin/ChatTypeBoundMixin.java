package com.dedsafio4.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El formato del chat del Dedsafío ("Nombre: mensaje", con el nombre en verde azulado y sin las flechas < >) se arma
 * acá, en el juego de cada uno, y no solo en el servidor: así gana aunque el servidor tenga otro mod, plugin o
 * datapack que cambie el formato del chat. Vale para el chat normal y para los tipos de chat que agregan otros mods.
 */
@Mixin(ChatType.Bound.class)
public abstract class ChatTypeBoundMixin {
	@Shadow
	public abstract net.minecraft.core.Holder<ChatType> chatType();

	@Shadow
	public abstract Component name();

	@Inject(method = "decorate", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$formatoDedsafio(Component mensaje, CallbackInfoReturnable<Component> cir) {
		var tipo = chatType();
		boolean propio = tipo.is(ChatType.CHAT)
				|| tipo.unwrapKey().map(k -> !k.location().getNamespace().equals("minecraft")).orElse(false);
		if (!propio) return;
		Component nombre = name().copy().withStyle(s -> s.withColor(0x55FFA0));
		cir.setReturnValue(Component.translatable("chat.type.dedsafio4", nombre, mensaje));
	}
}
