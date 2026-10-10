package com.dedsafio4.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Los ahogados ya no aparecen con tridente (el tridente ahora es del Limbo). */
@Mixin(Drowned.class)
public abstract class DrownedSinTridenteMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void dedsafio4$sinTridente(RandomSource azar, DifficultyInstance dificultad, CallbackInfo ci) {
		Drowned ahogado = (Drowned) (Object) this;
		if (ahogado.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.TRIDENT)) ahogado.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
	}
}
