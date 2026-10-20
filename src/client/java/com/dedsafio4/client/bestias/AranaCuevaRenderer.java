package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.MutacionArana;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.CaveSpider;

/**
 * La araña de cueva de siempre, pero con /mutacion se dibuja como la Araña Mutante (AranaMutanteModelo) con una de
 * sus 3 texturas (siempre la misma para cada araña). Los ojos que brillan solo van en la araña normal.
 */
public class AranaCuevaRenderer extends CaveSpiderRenderer {
	private static final ResourceLocation[] TEXTURAS = {textura(1), textura(2), textura(3)};

	private final SpiderModel<CaveSpider> normal;
	private final AranaMutanteModelo<CaveSpider> mutante;

	private static ResourceLocation textura(int n) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/arana_mutante_" + n + ".png");
	}

	public AranaCuevaRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.normal = this.model;
		this.mutante = new AranaMutanteModelo<>(contexto.bakeLayer(ModelLayers.CAVE_SPIDER));
		this.layers.removeIf(capa -> capa instanceof SpiderEyesLayer);
		SpiderEyesLayer<CaveSpider, SpiderModel<CaveSpider>> ojos = new SpiderEyesLayer<>(this);
		this.addLayer(new RenderLayer<>(this) {
			@Override
			public void render(PoseStack pose, MultiBufferSource buffers, int luz, CaveSpider arana, float paso, float velocidad,
							   float parcial, float edad, float cabezaY, float cabezaX) {
				if (!MutacionArana.ACTIVA_CLIENTE) ojos.render(pose, buffers, luz, arana, paso, velocidad, parcial, edad, cabezaY, cabezaX);
			}
		});
	}

	@Override
	public void render(CaveSpider arana, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		this.model = MutacionArana.ACTIVA_CLIENTE ? mutante : normal;
		super.render(arana, giro, parcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(CaveSpider arana) {
		if (!MutacionArana.ACTIVA_CLIENTE) return super.getTextureLocation(arana);
		return TEXTURAS[Math.floorMod(arana.getUUID().hashCode(), TEXTURAS.length)];
	}
}
