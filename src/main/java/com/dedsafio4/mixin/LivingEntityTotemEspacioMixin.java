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
 * (desde el inventario).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemEspacioMixin {
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$totemEspacio(DamageSource fuente, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity yo = (LivingEntity) (Object) this;
		if (!TotemEspacioItem.enEspacio(yo)) return;
		if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			cir.setReturnValue(false);
			return;
		}
		cir.setReturnValue(yo instanceof ServerPlayer jugador && TotemEspacioItem.salvar(jugador));
	}
}
