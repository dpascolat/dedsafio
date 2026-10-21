package com.dedsafio4.mixin;

import com.dedsafio4.items.ModItems;
import com.dedsafio4.pociones.ModPociones;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El Antídoto Primitivo en el soporte para pociones: con botellas de agua (o pociones raras, con verruga)
 * sale directo la Poción arrojadiza de Antibiótico, sin tener que ponerle pólvora. Y la Poción de Respiración
 * acuática ya no se hace con pez globo sino con la Baya Uvina (en una poción rara, como siempre).
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {
	private static boolean dedsafio4$sirve(ItemStack botella) {
		if (!botella.is(Items.POTION)) return false;
		PotionContents c = botella.get(DataComponents.POTION_CONTENTS);
		return c != null && (c.is(Potions.WATER) || c.is(Potions.AWKWARD));
	}

	@Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$antidotoEsIngrediente(ItemStack pila, CallbackInfoReturnable<Boolean> cir) {
		if (pila.is(ModItems.ANTIDOTO_PRIMITIVO) || pila.is(ModItems.BAYA_UVINA)) cir.setReturnValue(true);
		if (pila.is(Items.PUFFERFISH)) cir.setReturnValue(false);
	}

	private static boolean dedsafio4$rara(ItemStack botella) {
		PotionContents c = botella.get(DataComponents.POTION_CONTENTS);
		return c != null && c.is(Potions.AWKWARD)
				&& (botella.is(Items.POTION) || botella.is(Items.SPLASH_POTION) || botella.is(Items.LINGERING_POTION));
	}

	@Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$antidotoMezcla(ItemStack botella, ItemStack ingrediente, CallbackInfoReturnable<Boolean> cir) {
		if (ingrediente.is(ModItems.ANTIDOTO_PRIMITIVO)) cir.setReturnValue(dedsafio4$sirve(botella));
		if (ingrediente.is(ModItems.BAYA_UVINA)) cir.setReturnValue(dedsafio4$rara(botella));
		if (ingrediente.is(Items.PUFFERFISH)) cir.setReturnValue(false);
	}

	@Inject(method = "mix", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$antidotoResultado(ItemStack ingrediente, ItemStack botella, CallbackInfoReturnable<ItemStack> cir) {
		if (ingrediente.is(ModItems.BAYA_UVINA)) {
			cir.setReturnValue(dedsafio4$rara(botella) ? PotionContents.createItemStack(botella.getItem(), Potions.WATER_BREATHING) : botella);
			return;
		}
		if (ingrediente.is(Items.PUFFERFISH)) {
			cir.setReturnValue(botella);
			return;
		}
		if (ingrediente.is(ModItems.ANTIDOTO_PRIMITIVO)) {
			cir.setReturnValue(dedsafio4$sirve(botella)
					? PotionContents.createItemStack(Items.SPLASH_POTION, ModPociones.ANTIBIOTICO) : botella);
		}
	}
}
