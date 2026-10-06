package com.dedsafio4.client.mixin;

import com.dedsafio4.items.ModItems;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	/** La animación del tótem muestra el Totem Frerico o el de Concha si fue él el que te salvó (no hay Tótem de la Inmortalidad en la mano). */
	@Inject(method = "findTotem", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$totemFrerico(Player jugador, CallbackInfoReturnable<ItemStack> cir) {
		for (InteractionHand mano : InteractionHand.values()) {
			if (jugador.getItemInHand(mano).is(Items.TOTEM_OF_UNDYING)) return;
		}
		for (InteractionHand mano : InteractionHand.values()) {
			ItemStack pila = jugador.getItemInHand(mano);
			if (pila.is(ModItems.TOTEM_FRERICO) || pila.is(ModItems.TOTEM_GLEBANOIDE)) {
				cir.setReturnValue(pila.copy());
				return;
			}
		}
		// El Tótem del Ídolo salta desde cualquier lugar del inventario.
		var inventario = jugador.getInventory();
		for (int i = 0; i < inventario.getContainerSize(); i++) {
			if (inventario.getItem(i).is(ModItems.TOTEM_IDOLO)) {
				cir.setReturnValue(inventario.getItem(i).copy());
				return;
			}
		}
	}
}
