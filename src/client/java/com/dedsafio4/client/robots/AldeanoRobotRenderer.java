package com.dedsafio4.client.robots;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.robots.AldeanoRobotEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al Aldeano Robot (la textura es blanca: el color va en cada pieza del modelo). */
public class AldeanoRobotRenderer extends MobRenderer<AldeanoRobotEntity, AldeanoRobotModelo> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/blanco.png");

	public AldeanoRobotRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new AldeanoRobotModelo(), 0.4f);
	}

	@Override
	public ResourceLocation getTextureLocation(AldeanoRobotEntity robot) {
		return TEXTURA;
	}
}
