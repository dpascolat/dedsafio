package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperPastoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja el Creeper de Pasto (chiquito: 0,64 bloques de alto): se infla y parpadea en blanco antes de explotar. */
public class CreeperPastoRenderer extends MobRenderer<CreeperPastoEntity, CreeperPastoModelo> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_pasto.png");

	public CreeperPastoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new CreeperPastoModelo(contexto.getResourceManager()), 0.22f);
	}

	/** El modelo mide 0,625 bloques; queda 1,5 veces 1/4 de un creeper normal (1,7 / 4 x 1,5 = 0,64 bloques). */
	private static final float ESCALA = 1.7f / 4f * 1.5f / 0.625f;

	@Override
	protected void scale(CreeperPastoEntity creeper, PoseStack pose, float parcial) {
		pose.scale(ESCALA, ESCALA, ESCALA);
		float f = creeper.getSwelling(parcial);
		float g = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
		f = Mth.clamp(f, 0.0F, 1.0F);
		f *= f;
		f *= f;
		float ancho = (1.0F + f * 0.4F) * g;
		float alto = (1.0F + f * 0.1F) / g;
		pose.scale(ancho, alto, ancho);
	}

	@Override
	protected float getWhiteOverlayProgress(CreeperPastoEntity creeper, float parcial) {
		float f = creeper.getSwelling(parcial);
		return (int) (f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(CreeperPastoEntity creeper) {
		return TEXTURA;
	}
}
