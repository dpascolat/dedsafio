package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.MutacionFantasma;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;

/**
 * El fantasma de siempre, pero con /mutacion pantome se dibuja como el Fantasma Mutante (FantasmaMutanteModelo) con
 * su textura y sus ojos que brillan.
 */
public class PhantomMutadoRenderer extends PhantomRenderer {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/fantasma_mutante.png");
	private static final ResourceLocation OJOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/fantasma_mutante_ojos.png");

	private final PhantomModel<Phantom> normal;
	private final FantasmaMutanteModelo<Phantom> mutante;

	public PhantomMutadoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.normal = this.model;
		this.mutante = new FantasmaMutanteModelo<>(contexto.bakeLayer(ModelLayers.PHANTOM));
		// Los ojos: los de siempre en el normal, los del mutante en el mutado.
		this.layers.removeIf(capa -> capa instanceof PhantomEyesLayer);
		PhantomEyesLayer<Phantom> ojos = new PhantomEyesLayer<>(this);
		this.addLayer(new RenderLayer<>(this) {
			@Override
			public void render(PoseStack pose, MultiBufferSource buffers, int luz, Phantom fantasma, float paso, float velocidad,
							   float parcial, float edad, float cabezaY, float cabezaX) {
				if (!MutacionFantasma.ACTIVA_CLIENTE) {
					ojos.render(pose, buffers, luz, fantasma, paso, velocidad, parcial, edad, cabezaY, cabezaX);
					return;
				}
				mutante.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(OJOS)), 0xF00000, OverlayTexture.NO_OVERLAY, -1);
			}
		});
	}

	@Override
	public void render(Phantom fantasma, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		this.model = MutacionFantasma.ACTIVA_CLIENTE ? mutante : normal;
		super.render(fantasma, giro, parcial, pose, buffers, luz);
	}

	@Override
	public ResourceLocation getTextureLocation(Phantom fantasma) {
		return MutacionFantasma.ACTIVA_CLIENTE ? TEXTURA : super.getTextureLocation(fantasma);
	}
}
