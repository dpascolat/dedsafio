package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperPapelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja el Creeper de Papel: la solicitud de empleo caminando; se infla y parpadea en blanco antes de "explotar". */
public class CreeperPapelRenderer extends MobRenderer<CreeperPapelEntity, CreeperPapelModelo> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_papel.png");

	public CreeperPapelRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new CreeperPapelModelo(contexto.bakeLayer(ModelLayers.CREEPER)), 0.4f);
	}

	@Override
	protected void scale(CreeperPapelEntity creeper, PoseStack pose, float parcial) {
		float f = creeper.getSwelling(parcial);
		float g = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
		f = Mth.clamp(f, 0.0F, 1.0F);
		f *= f;
		f *= f;
		pose.scale((1.0F + f * 0.4F) * g, (1.0F + f * 0.1F) / g, (1.0F + f * 0.4F) * g);
	}

	@Override
	protected float getWhiteOverlayProgress(CreeperPapelEntity creeper, float parcial) {
		float f = creeper.getSwelling(parcial);
		return (int) (f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperPapelEntity creeper) {
		return TEXTURA;
	}
}
