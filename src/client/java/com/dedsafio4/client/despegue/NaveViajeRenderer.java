package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Dibuja la nave parada (ver NaveViajeModelo). Al despegar la nave se queda en la plataforma y lo que sube es este
 * dibujo, cuadro a cuadro (NaveViajeEntity.altura), así la subida es suave y no a los tirones.
 */
public class NaveViajeRenderer extends GeoEntityRenderer<NaveViajeEntity> {
	public NaveViajeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new NaveViajeModelo());
		this.shadowRadius = 1.0f;
	}

	@Override
	public void render(NaveViajeEntity nave, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		if (nave.estado() == NaveViajeEntity.ESPACIO) return;   // está "en el espacio": la cinemática la muestra
		pose.pushPose();
		pose.translate(0, nave.altura(parcial), 0);
		super.render(nave, giro, parcial, pose, buffers, luz);
		pose.popPose();
	}
}
