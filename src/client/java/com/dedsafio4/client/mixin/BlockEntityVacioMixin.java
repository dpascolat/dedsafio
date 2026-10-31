package com.dedsafio4.client.mixin;

import com.dedsafio4.client.ruleta.EntregaCliente;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En el Vacío Cósmico no se ven los cofres ni los carteles (se dibujan después del vacío). */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityVacioMixin {
	@Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinVacio(BlockEntity bloque, float parcial, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
		if (EntregaCliente.vacioCompleto()) ci.cancel();
	}
}
