package com.dedsafio4.client.mixin;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En la nave, en primera persona, la cámara va 6 píxeles hacia atrás (hacia donde queda la cabeza del jugador
 * acostado, ver PlayerRendererNaveMixin).
 */
@Mixin(Camera.class)
public abstract class CameraNaveMixin {
	private static final double ATRAS = 6 / 16.0;

	@Shadow
	protected abstract void setPosition(Vec3 posicion);

	@Shadow
	public abstract Vec3 getPosition();

	@Inject(method = "setup", at = @At("TAIL"))
	private void dedsafio4$camaraEnLaNave(BlockGetter mundo, Entity entidad, boolean terceraPersona, boolean deFrente,
										  float partialTick, CallbackInfo ci) {
		if (terceraPersona || entidad == null || !(entidad.getVehicle() instanceof NaveViajeEntity nave)) return;
		float giro = nave.getYRot() * Mth.DEG_TO_RAD;
		setPosition(getPosition().add(Mth.sin(giro) * ATRAS, 0, -Mth.cos(giro) * ATRAS));
	}
}
