package com.dedsafio4.client.despegue;

import com.dedsafio4.despegue.NaveViajeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Dibuja la nave parada (ver NaveViajeModelo). */
public class NaveViajeRenderer extends GeoEntityRenderer<NaveViajeEntity> {
	public NaveViajeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new NaveViajeModelo());
		this.shadowRadius = 1.0f;
	}
}
