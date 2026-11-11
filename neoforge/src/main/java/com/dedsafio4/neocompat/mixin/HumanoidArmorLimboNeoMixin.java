package com.dedsafio4.neocompat.mixin;

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

/**
 * Como HumanoidArmorLimboMixin, pero para la versión del método que usa NeoForge (con 6 números más): la Armadura
 * del Limbo (y cualquier armadura con el Traje Espacial puesto) no se dibuja como la armadura normal.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLimboNeoMixin {
	@Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
			+ "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void dedsafio4$sinArmaduraNormalNeo(PoseStack pose, MultiBufferSource buffers, LivingEntity entidad, EquipmentSlot lugar,
												int luz, HumanoidModel<?> modelo, float a, float b, float c, float d, float e, float f,
												CallbackInfo ci) {
		boolean conTraje = entidad instanceof net.minecraft.world.entity.player.Player jugador
				&& com.dedsafio4.brazalete.TrajeEspacial.puesto(jugador);
		if (entidad.getItemBySlot(lugar).getItem() instanceof ArmaduraLimbo.Pieza || conTraje) ci.cancel();
	}
}
