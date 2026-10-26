package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Dibuja la nave parada (ver NaveViajeModelo). Al despegar la nave se queda en el soporte y lo que sube es este
 * dibujo, cuadro a cuadro (NaveViajeEntity.altura), así la subida es suave y no a los tirones.
 */
public class NaveViajeRenderer extends GeoEntityRenderer<NaveViajeEntity> {
	public NaveViajeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new NaveViajeModelo());
		this.shadowRadius = 1.0f;
	}

	/** Pintada: toda la nave teñida del color de su tinte. */
	@Override
	public software.bernie.geckolib.util.Color getRenderColor(NaveViajeEntity nave, float parcial, int luz) {
		if (nave.color() < 0) return super.getRenderColor(nave, parcial, luz);
		return software.bernie.geckolib.util.Color.ofOpaque(net.minecraft.world.item.DyeColor.byId(nave.color()).getTextureDiffuseColor());
	}

	@Override
	public void render(NaveViajeEntity nave, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		pose.translate(0, nave.altura(parcial), 0);
		// GeckoLib sólo gira a los mobs: la nave se gira acá para que mire hacia donde la pusieron.
		pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-nave.getYRot()));
		super.render(nave, giro, parcial, pose, buffers, luz);
		pose.popPose();
	}
}
