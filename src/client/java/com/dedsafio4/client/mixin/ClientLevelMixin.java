package com.dedsafio4.client.mixin;

import com.dedsafio4.client.CieloCliente;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	/** Con /cielo rojo, las nubes se tiñen de rojo oscuro en vez de blanco. */
	@Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$nubesRojas(float partialTick, CallbackInfoReturnable<Vec3> cir) {
		if (!CieloCliente.activoAqui()) return;
		Vec3 color = cir.getReturnValue();
		double p = CieloCliente.progresoCielo(partialTick);
		cir.setReturnValue(new Vec3(color.x * (1 - 0.65 * p), color.y * (1 - 0.94 * p), color.z * (1 - 0.94 * p)));
	}
}
