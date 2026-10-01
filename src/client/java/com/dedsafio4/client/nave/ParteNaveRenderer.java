package com.dedsafio4.client.nave;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.ParteNaveItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Dibuja una parte de la nave con su modelo (geo/item/<parte>.geo.json) y la textura de la nave (textures/item/nave.png). */
public class ParteNaveRenderer extends GeoItemRenderer<ParteNaveItem> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/item/nave.png");

	public ParteNaveRenderer(String modelo) {
		super(new GeoModel<>() {
			private final ResourceLocation geo = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/item/" + modelo + ".geo.json");
			private final ResourceLocation animaciones =
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/item/" + modelo + ".animation.json");

			@Override
			public ResourceLocation getModelResource(ParteNaveItem item) {
				return geo;
			}

			@Override
			public ResourceLocation getTextureResource(ParteNaveItem item) {
				return TEXTURA;
			}

			@Override
			public ResourceLocation getAnimationResource(ParteNaveItem item) {
				return animaciones;
			}
		});
	}

	/** Se llama al arrancar el cliente: cada parte arma su dibujo la primera vez que se muestra. */
	public static void registrar() {
		ParteNaveItem.dibujo = item -> new GeoRenderProvider() {
			private ParteNaveRenderer dibujo;

			@Override
			public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
				if (dibujo == null) dibujo = new ParteNaveRenderer(item.modelo);
				return dibujo;
			}
		};
	}
}
