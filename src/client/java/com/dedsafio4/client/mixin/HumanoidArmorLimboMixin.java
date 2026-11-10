package com.dedsafio4.client.mixin;

import com.dedsafio4.items.ArmaduraLimbo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** En los jugadores la Armadura del Limbo usa su propio modelo (ArmaduraLimboLayer): no se dibuja la normal. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLimboMixin {
	@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinArmaduraNormal(PoseStack pose, MultiBufferSource buffers, LivingEntity entidad, EquipmentSlot lugar,
											 int luz, HumanoidModel<?> modelo, CallbackInfo ci) {
		if (entidad instanceof Player && entidad.getItemBySlot(lugar).getItem() instanceof ArmaduraLimbo.Pieza) ci.cancel();
	}
}
