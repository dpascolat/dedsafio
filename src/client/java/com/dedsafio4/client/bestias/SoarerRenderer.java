package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.SoarerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja el Soarer con GeckoLib. */
public class SoarerRenderer extends GeoEntityRenderer<SoarerEntity> {
	public SoarerRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new SoarerModelo());
		this.shadowRadius = 1.6f;
	}
}
