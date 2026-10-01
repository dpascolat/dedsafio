package com.dedsafio4.client.dactylos;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.dactylos.DactyloBebeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DactyloBebeRenderer extends MobRenderer<DactyloBebeEntity, DactyloBebeModelo> {
	static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/dactylo_bebe.png");

	public DactyloBebeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new DactyloBebeModelo(), 0.4f);
	}

	@Override
	public ResourceLocation getTextureLocation(DactyloBebeEntity dactylo) {
		return TEXTURA;
	}
}
