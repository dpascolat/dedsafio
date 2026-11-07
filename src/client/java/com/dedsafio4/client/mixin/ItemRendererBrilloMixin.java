package com.dedsafio4.client.mixin;

import com.dedsafio4.items.ModItems;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * La Escopeta Phora se dibuja en partes: el cuerpo, lo naranja del cuerpo otra vez a luz plena (así brilla en la
 * oscuridad), y el tambor con su naranja (va aparte para poder sacarlo y poner otro al recargar).
 */
@Mixin(ItemRenderer.class)
abstract class ItemRendererBrilloMixin {
	@Shadow
	public abstract BakedModel getModel(ItemStack pila, net.minecraft.world.level.Level mundo, net.minecraft.world.entity.LivingEntity entidad, int semilla);

	@WrapMethod(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V")
	private void dedsafio4$brillo(ItemStack pila, ItemDisplayContext contexto, boolean izquierda, PoseStack pose, MultiBufferSource buffers,
								  int luz, int overlay, BakedModel modelo, Operation<Void> original) {
		original.call(pila, contexto, izquierda, pose, buffers, luz, overlay, modelo);
		if (!pila.is(ModItems.ESCOPETA_PHORA)) return;
		ItemStack brillo = new ItemStack(ModItems.ESCOPETA_PHORA_BRILLO);
		original.call(brillo, contexto, izquierda, pose, buffers, LightTexture.FULL_BRIGHT, overlay, getModel(brillo, null, null, 0));
		// El tambor va aparte: en primera persona, al recargar, se saca para abajo y entra uno nuevo.
		boolean primera = contexto == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || contexto == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		float baja = primera ? com.dedsafio4.client.EscopetaCliente.tamborBaja(pila) : 0f;
		if (baja > 1.05f) return;   // sin tambor
		pose.pushPose();
		pose.translate(0, -baja, 0);
		ItemStack tambor = new ItemStack(ModItems.ESCOPETA_PHORA_TAMBOR);
		original.call(tambor, contexto, izquierda, pose, buffers, luz, overlay, getModel(tambor, null, null, 0));
		ItemStack tamborBrillo = new ItemStack(ModItems.ESCOPETA_PHORA_TAMBOR_BRILLO);
		original.call(tamborBrillo, contexto, izquierda, pose, buffers, LightTexture.FULL_BRIGHT, overlay, getModel(tamborBrillo, null, null, 0));
		pose.popPose();
	}
}
