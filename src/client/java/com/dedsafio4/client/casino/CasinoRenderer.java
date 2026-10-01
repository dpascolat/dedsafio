package com.dedsafio4.client.casino;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.casino.CasinoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja el Casino con GeckoLib (geo/entity/casino.geo.json, textures/entity/casino.png, animations/entity/casino.animation.json). */
public class CasinoRenderer extends GeoEntityRenderer<CasinoEntity> {
	public CasinoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "casino")));
		this.shadowRadius = 0.8f;
	}
}
