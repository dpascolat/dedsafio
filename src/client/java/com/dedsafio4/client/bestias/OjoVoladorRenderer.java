package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.OjoVoladorEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** El Ojo Volador con el modelo de Patricio ("OjoVolador"): cuando lo alumbran con la Linterna se va borrando. */
public class OjoVoladorRenderer extends RendererBB<OjoVoladorEntity> {
	public OjoVoladorRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, "ojo_volador", 1f, 180f, 0.8f);
	}

	@Override
	protected int color(OjoVoladorEntity ojo, float parcial) {
		// Alumbrado: se va haciendo transparente (a punto de desaparecer queda casi invisible).
		int alfa = Math.round(255 * (1 - 0.8f * ojo.luz()));
		return (alfa << 24) | 0xFFFFFF;
	}

	@Override
	protected RenderType tipo(ResourceLocation textura) {
		return RenderType.entityTranslucent(textura);
	}
}
