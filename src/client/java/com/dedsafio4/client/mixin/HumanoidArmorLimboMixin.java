package com.dedsafio4.client.mixin;

import com.dedsafio4.items.ArmaduraLimbo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** La Armadura del Limbo usa su propio modelo (ArmaduraLimboLayer): no se dibuja la normal, en nadie. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLimboMixin {
	@Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
			+ "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinArmaduraNormal(PoseStack pose, MultiBufferSource buffers, LivingEntity entidad, EquipmentSlot lugar,
											 int luz, HumanoidModel<?> modelo, CallbackInfo ci) {
		if (entidad.getItemBySlot(lugar).getItem() instanceof ArmaduraLimbo.Pieza) ci.cancel();
	}

	/** NeoForge dibuja la armadura con otra versión del método (con 6 números más): también se saltea ahí. */
	@Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
			+ "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void dedsafio4$sinArmaduraNormalNeo(PoseStack pose, MultiBufferSource buffers, LivingEntity entidad, EquipmentSlot lugar,
												int luz, HumanoidModel<?> modelo, float a, float b, float c, float d, float e, float f,
												CallbackInfo ci) {
		if (entidad.getItemBySlot(lugar).getItem() instanceof ArmaduraLimbo.Pieza) ci.cancel();
	}
}
