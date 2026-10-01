package com.dedsafio4.client.marcos;

import com.dedsafio4.marcos.MarcoGrandeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.phys.Vec3;

/** El marco grande se dibuja como uno normal invisible (solo su ítem o mapa), pero 4 veces más grande. */
public class MarcoGrandeRenderer extends ItemFrameRenderer<MarcoGrandeEntity> {
	public MarcoGrandeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	@Override
	public void render(MarcoGrandeEntity marco, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		// Minecraft ya corrió el dibujo por getRenderOffset y el marco lo deshace: se agranda alrededor del centro
		// del marco sin que ese corrimiento se duplique.
		Vec3 corrimiento = getRenderOffset(marco, parcial);
		pose.pushPose();
		pose.translate(-corrimiento.x, -corrimiento.y, -corrimiento.z);
		float escala = MarcoGrandeEntity.ESCALA;
		pose.scale(escala, escala, escala);
		pose.translate(corrimiento.x, corrimiento.y, corrimiento.z);
		super.render(marco, yaw, parcial, pose, buffers, luz);
		pose.popPose();
	}
}
