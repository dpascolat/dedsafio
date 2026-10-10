package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.FantasmaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Los fantasmas del Limbo con los modelos y animaciones del Dedsafío 3 (GeckoLib): geo/entity/&lt;nombre&gt;.geo.json,
 * animations/entity/&lt;nombre&gt;.animation.json y textures/entity/&lt;nombre&gt;.png.
 */
public class FantasmaRenderer extends GeoEntityRenderer<FantasmaEntity> {
	private FantasmaRenderer(EntityRendererProvider.Context contexto, String nombre) {
		super(contexto, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre)));
		this.shadowRadius = 0.35f;
	}

	public static FantasmaRenderer de(EntityRendererProvider.Context contexto, String nombre) {
		return new FantasmaRenderer(contexto, nombre);
	}
}
