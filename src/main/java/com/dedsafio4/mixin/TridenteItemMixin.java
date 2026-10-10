package com.dedsafio4.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El Tridente del Limbo (el tridente de Minecraft, cambiado): el nombre en violeta y, apenas está en el inventario de
 * alguien, ya viene encantado con Propulsión acuática III.
 */
@Mixin(Item.class)
public abstract class TridenteItemMixin {
	@Inject(method = "getName", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$nombreVioleta(ItemStack pila, CallbackInfoReturnable<Component> cir) {
		if ((Object) this != Items.TRIDENT) return;
		cir.setReturnValue(cir.getReturnValue().copy().withColor(0xE07AE6));
	}

	@Inject(method = "inventoryTick", at = @At("HEAD"))
	private void dedsafio4$yaEncantado(ItemStack pila, Level mundo, Entity duenio, int casilla, boolean enMano, CallbackInfo ci) {
		if ((Object) this != Items.TRIDENT || mundo.isClientSide) return;
		var propulsion = mundo.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.RIPTIDE);
		if (pila.getEnchantments().getLevel(propulsion) < 3) pila.enchant(propulsion, 3);
	}
}
