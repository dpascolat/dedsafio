package com.dedsafio4.client.mixin;

import com.dedsafio4.client.disfraz.DisfracesCliente;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * /cambiarmob: en lugar del jugador se dibuja el mob en que está transformado (con el nombre, la vida y la
 * Hermandad arriba, como siempre).
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMobMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private PlayerRendererMobMixin(EntityRendererProvider.Context contexto, PlayerModel<AbstractClientPlayer> modelo, float sombra) {
		super(contexto, modelo, sombra);
	}

	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$dibujarComoMob(AbstractClientPlayer jugador, float giro, float parcial, PoseStack pose,
										  MultiBufferSource buffers, int luz, CallbackInfo ci) {
		Entity falso = DisfracesCliente.falso(jugador);
		if (falso == null) return;
		@SuppressWarnings("unchecked")
		EntityRenderer<Entity> dibujante = (EntityRenderer<Entity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(falso);
		try {
			dibujante.render(falso, giro, parcial, pose, buffers, luz);
		} catch (Exception e) {
			DisfracesCliente.fallo(falso.getType(), e);
			return;
		}
		if (this.shouldShowName(jugador)) this.renderNameTag(jugador, jugador.getDisplayName(), pose, buffers, luz, parcial);
		ci.cancel();
	}
}
