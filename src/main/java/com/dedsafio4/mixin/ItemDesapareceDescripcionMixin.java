package com.dedsafio4.mixin;

import com.dedsafio4.cofres.ItemsQueDesaparecen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Los ítems de /item_desaparecer muestran en la descripción cuánto les falta para desaparecer. */
@Mixin(ItemStack.class)
public abstract class ItemDesapareceDescripcionMixin {
	@Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$desaparece(Item.TooltipContext contexto, Player jugador, TooltipFlag bandera, CallbackInfoReturnable<List<Component>> cir) {
		List<Component> lineas = cir.getReturnValue();
		if (lineas.isEmpty()) return;
		List<Component> nuevas = ItemsQueDesaparecen.conLinea(lineas, (ItemStack) (Object) this, jugador);
		if (nuevas != lineas) cir.setReturnValue(nuevas);
	}
}
