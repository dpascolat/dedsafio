package com.dedsafio4.mixin;

import com.dedsafio4.baneos.ItemsBaneados;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Los ítems baneados con /banear item no se pueden usar (ni en el aire ni sobre un bloque). */
@Mixin(ItemStack.class)
public abstract class ItemBaneadoMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$baneadoUsar(Level mundo, Player jugador, InteractionHand mano,
									  CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		ItemStack pila = (ItemStack) (Object) this;
		if (!ItemsBaneados.baneado(pila.getItem(), mundo)) return;
		if (!mundo.isClientSide) jugador.displayClientMessage(Component.literal("Este ítem está baneado.").withColor(0xFF5555), true);
		cir.setReturnValue(InteractionResultHolder.fail(pila));
	}

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$baneadoUsarEn(UseOnContext contexto, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack pila = (ItemStack) (Object) this;
		if (!ItemsBaneados.baneado(pila.getItem(), contexto.getLevel())) return;
		if (!contexto.getLevel().isClientSide && contexto.getPlayer() != null) {
			contexto.getPlayer().displayClientMessage(Component.literal("Este ítem está baneado.").withColor(0xFF5555), true);
		}
		cir.setReturnValue(InteractionResult.FAIL);
	}
}
