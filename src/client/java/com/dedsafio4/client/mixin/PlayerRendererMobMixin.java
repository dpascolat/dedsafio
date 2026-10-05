package com.dedsafio4.client.mixin;

import com.dedsafio4.client.disfraz.DisfracesCliente;
import com.dedsafio4.client.disfraz.SkinsCliente;
import com.dedsafio4.disfraz.Disfraces;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * /cambiarmob: en lugar del jugador se dibuja el mob en que está transformado, o el modelo del Skin Pack Dedsafío
 * (con el nombre, la vida y la Hermandad arriba, como siempre).
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMobMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private PlayerRendererMobMixin(EntityRendererProvider.Context contexto, PlayerModel<AbstractClientPlayer> modelo, float sombra) {
		super(contexto, modelo, sombra);
	}

	@Shadow
	private void setModelProperties(AbstractClientPlayer jugador) {
	}

	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$dibujarComoMob(AbstractClientPlayer jugador, float giro, float parcial, PoseStack pose,
										  MultiBufferSource buffers, int luz, CallbackInfo ci) {
		String skin = Disfraces.skin(jugador);
		if (skin != null) {
			if (!dedsafio4$dibujarSkin(skin, jugador, parcial, pose, buffers, luz)) return;
		} else {
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
		}
		if (this.shouldShowName(jugador)) this.renderNameTag(jugador, jugador.getDisplayName(), pose, buffers, luz, parcial);
		ci.cancel();
	}

	/** Lo mismo que hace Minecraft para dibujar al jugador, pero con el modelo de la skin en lugar del cuerpo normal. */
	private boolean dedsafio4$dibujarSkin(String skin, AbstractClientPlayer jugador, float parcial, PoseStack pose,
										  MultiBufferSource buffers, int luz) {
		setModelProperties(jugador);
		PlayerModel<AbstractClientPlayer> modelo = this.getModel();
		pose.pushPose();
		modelo.attackTime = this.getAttackAnim(jugador, parcial);
		modelo.riding = jugador.isPassenger();
		modelo.young = jugador.isBaby();
		float cuerpo = Mth.rotLerp(parcial, jugador.yBodyRotO, jugador.yBodyRot);
		float cabeza = Mth.rotLerp(parcial, jugador.yHeadRotO, jugador.yHeadRot);
		float giroCabeza = cabeza - cuerpo;
		if (jugador.isPassenger() && jugador.getVehicle() instanceof LivingEntity montura) {
			cuerpo = Mth.rotLerp(parcial, montura.yBodyRotO, montura.yBodyRot);
			float d = Mth.clamp(Mth.wrapDegrees(cabeza - cuerpo), -85f, 85f);
			cuerpo = cabeza - d;
			if (d * d > 2500f) cuerpo += d * 0.2f;
			giroCabeza = cabeza - cuerpo;
		}
		float inclinacion = Mth.lerp(parcial, jugador.xRotO, jugador.getXRot());
		float escala = jugador.getScale();
		pose.scale(escala, escala, escala);
		if (jugador.hasPose(Pose.SLEEPING)) {
			Direction cama = jugador.getBedOrientation();
			if (cama != null) {
				float ojos = jugador.getEyeHeight(Pose.STANDING) - 0.1f;
				pose.translate(-cama.getStepX() * ojos, 0, -cama.getStepZ() * ojos);
			}
		}
		float bob = this.getBob(jugador, parcial);
		this.setupRotations(jugador, pose, bob, cuerpo, parcial, escala);
		pose.scale(-1f, -1f, 1f);
		this.scale(jugador, pose, parcial);
		pose.translate(0f, -1.501f, 0f);
		float velocidad = 0, posicion = 0;
		if (!jugador.isPassenger() && jugador.isAlive()) {
			velocidad = Math.min(1f, jugador.walkAnimation.speed(parcial));
			posicion = jugador.walkAnimation.position(parcial);
		}
		modelo.prepareMobModel(jugador, posicion, velocidad, parcial);
		modelo.setupAnim(jugador, posicion, velocidad, bob, giroCabeza, inclinacion);
		boolean listo = true;
		if (!jugador.isInvisible()) {
			int overlay = LivingEntityRenderer.getOverlayCoords(jugador, this.getWhiteOverlayProgress(jugador, parcial));
			listo = SkinsCliente.dibujar(skin, modelo, jugador, pose, buffers, luz, overlay);
		}
		pose.popPose();
		return listo;
	}
}
