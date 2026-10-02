package com.dedsafio4.client.nave;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.NaveVelozEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja la Nave Veloz con GeckoLib (geo/entity/nave_veloz.geo.json, textures/entity/nave_veloz.png y sus animaciones). */
public class NaveVelozRenderer extends GeoEntityRenderer<NaveVelozEntity> {
	public NaveVelozRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "nave_veloz")));
		this.shadowRadius = 0.5f;
	}
}
