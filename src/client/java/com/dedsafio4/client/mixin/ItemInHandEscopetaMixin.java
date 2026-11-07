package com.dedsafio4.client.mixin;

import com.dedsafio4.client.EscopetaCliente;
import com.dedsafio4.items.EscopetaPhoraItem;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * La Escopeta Phora en primera persona: su propia animación de agarrarla y de disparar (EscopetaCliente.animar),
 * sin la de Minecraft (bajar y subir al cambiar de ítem, ni el golpe con la mano).
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandEscopetaMixin {
	@WrapMethod(method = "renderArmWithItem")
	private void dedsafio4$escopeta(AbstractClientPlayer jugador, float parcial, float inclinacion, InteractionHand mano, float golpe,
									ItemStack pila, float agarre, PoseStack pose, MultiBufferSource buffers, int luz, Operation<Void> original) {
		if (!(pila.getItem() instanceof EscopetaPhoraItem)) {
			original.call(jugador, parcial, inclinacion, mano, golpe, pila, agarre, pose, buffers, luz);
			return;
		}
		pose.pushPose();
		EscopetaCliente.animar(pose, jugador, mano, parcial);
		original.call(jugador, parcial, inclinacion, mano, 0f, pila, 0f, pose, buffers, luz);
		if (mano == InteractionHand.MAIN_HAND) EscopetaCliente.brazos(pose, buffers, luz, jugador);
		pose.popPose();
	}
}
