package com.dedsafio4.client.robots;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.robots.AldeanoRobotEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al Aldeano Phora con el modelo y la textura de Patricio (pa_aldeano_robot_onixestudio). */
public class AldeanoRobotRenderer extends MobRenderer<AldeanoRobotEntity, AldeanoPhoraModelo> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/aldeano_phora.png");

	public AldeanoRobotRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new AldeanoPhoraModelo(), 0.4f);
	}

	@Override
	public ResourceLocation getTextureLocation(AldeanoRobotEntity robot) {
		return TEXTURA;
	}
}
