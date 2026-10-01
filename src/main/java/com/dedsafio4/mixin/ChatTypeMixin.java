package com.dedsafio4.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatType.class)
public abstract class ChatTypeMixin {
	/** Verde azulado del nombre de quien habla en el chat. */
	private static final TextColor COLOR_NOMBRE = TextColor.fromRgb(0x55FFA0);

	/**
	 * En el chat, el nombre del jugador que habla sale en verde azulado.
	 * (Las flechas < > se sacan en data/minecraft/chat_type/chat.json.)
	 */
	@Inject(method = "bind(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/ChatType$Bound;",
			at = @At("RETURN"), cancellable = true)
	private static void dedsafio4$nombreVerde(ResourceKey<ChatType> tipo, Entity entidad, CallbackInfoReturnable<ChatType.Bound> cir) {
		if (tipo != ChatType.CHAT || !(entidad instanceof Player)) return;
		ChatType.Bound bound = cir.getReturnValue();
		cir.setReturnValue(new ChatType.Bound(bound.chatType(),
				bound.name().copy().withStyle(s -> s.withColor(COLOR_NOMBRE)), bound.targetName()));
	}
}
