package com.dedsafio4.client.mixin;

import com.dedsafio4.client.momentito.MomentitoCamara;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Durante /momentito 1, la cámara la maneja la escena (MomentitoCamara). */
@Mixin(Camera.class)
public abstract class MomentitoCamaraMixin {
	@Shadow
	protected abstract void setPosition(Vec3 posicion);

	@Shadow
	protected abstract void setRotation(float giro, float inclinacion);

	@Shadow
	private boolean detached;

	@Inject(method = "setup", at = @At("TAIL"))
	private void dedsafio4$camaraDeCine(BlockGetter mundo, Entity entidad, boolean terceraPersona, boolean deFrente,
										float partialTick, CallbackInfo ci) {
		double[] c = MomentitoCamara.camara(partialTick);
		if (c == null) return;
		detached = true;   // así se ve el propio jugador en las tomas
		setRotation((float) c[3], (float) c[4]);
		setPosition(new Vec3(c[0], c[1], c[2]));
	}
}
