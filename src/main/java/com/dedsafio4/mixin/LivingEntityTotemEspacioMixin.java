package com.dedsafio4.mixin;

import com.dedsafio4.items.TotemEspacioItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * En el Espacio entre Dimensiones el tótem de Minecraft no sirve: sólo el Tótem del Espacio entre Dimensiones
 * (desde el inventario). Y cuando el tótem de Minecraft (el Nutritótem) salva a un jugador, sale en el chat de todos
 * como los demás tótems.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemEspacioMixin {
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$totemEspacio(DamageSource fuente, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity yo = (LivingEntity) (Object) this;
		if (!TotemEspacioItem.enEspacio(yo)) {
			if (yo instanceof ServerPlayer jugador && com.dedsafio4.items.TotemManos.saltearMinecraft(jugador)) cir.setReturnValue(false);
			return;
		}
		if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			cir.setReturnValue(false);
			return;
		}
		cir.setReturnValue(yo instanceof ServerPlayer jugador && TotemEspacioItem.salvar(jugador));
	}

	/** Acá sólo llega el tótem de Minecraft (los del mod avisan solos): "X ha consumido un Nutritótem". */
	@Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
	private void dedsafio4$avisoNutritotem(DamageSource fuente, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof ServerPlayer jugador) {
			com.dedsafio4.items.AvisoTotem.consumido(jugador, net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
		}
	}
}
