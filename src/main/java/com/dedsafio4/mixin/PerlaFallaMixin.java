package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** perlas_fallan 1: cada Perla del End tiene 1% de fallar: al caer se rompe y no te lleva a ningún lado. */
@Mixin(ThrownEnderpearl.class)
public abstract class PerlaFallaMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$falla(HitResult golpe, CallbackInfo ci) {
		ThrownEnderpearl perla = (ThrownEnderpearl) (Object) this;
		if (!(perla.level() instanceof ServerLevel mundo)) return;
		if (Cambios.nivel(mundo.getServer(), Cambios.PERLAS_FALLAN) < 1 || mundo.getRandom().nextFloat() >= 0.01f) return;
		ci.cancel();
		mundo.sendParticles(ParticleTypes.SMOKE, perla.getX(), perla.getY(), perla.getZ(), 12, 0.2, 0.2, 0.2, 0.02);
		mundo.playSound(null, perla.getX(), perla.getY(), perla.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.4f);
		if (perla.getOwner() instanceof net.minecraft.server.level.ServerPlayer jugador) {
			jugador.displayClientMessage(net.minecraft.network.chat.Component.literal("¡La Perla del End falló!")
					.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
		}
		perla.discard();
	}
}
