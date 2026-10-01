package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.WalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja el Walker con GeckoLib. */
public class WalkerRenderer extends GeoEntityRenderer<WalkerEntity> {
	public WalkerRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new WalkerModelo());
		this.shadowRadius = 2.5f;
	}
}
