package com.dedsafio4.client.bote;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bote.BoteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Dibuja el Bote (geo/entity/bote.geo.json, el bote largo de madera violeta con el farol en la punta). El farol
 * brilla en la oscuridad (textures/entity/bote_glowmask.png). Se mece un poco cuando lo golpean, como el de Minecraft.
 */
public class BoteRenderer extends GeoEntityRenderer<BoteEntity> {
	/**
	 * El modelo tiene el fondo del bote en y = 0: se sube un poco para que flote (el agua queda por debajo de los
	 * bancos). TAPA_AGUA es la altura (en píxeles del modelo) de la "tapa" invisible que no deja ver el agua adentro.
	 */
	public static final float SUBIR = 4 / 16f, TAPA_AGUA = 6f;
	/** El contorno de adentro del bote (en píxeles, visto de arriba): la parte recta del medio y las dos puntas. */
	private static final float ANCHO = 8.6f, RECTO = 48f, PUNTA = 84f;

	public BoteRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, new Modelo());
		this.shadowRadius = 1.2f;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public void render(BoteEntity bote, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		// GeckoLib sólo gira a los mobs: el bote se gira acá (la punta del modelo, con el farol, mira para adelante).
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, bote.yRotO, bote.getYRot())));
		float golpe = bote.getHurtTime() - parcial;
		float dano = Math.max(0, bote.getDamage() - parcial);
		if (golpe > 0) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(golpe) * golpe * dano / 40f * bote.getHurtDir()));
		pose.translate(0, SUBIR, 0);
		super.render(bote, giro, parcial, pose, buffers, luz);
		tapaAgua(pose, buffers);
		pose.popPose();
	}

	/** Como el bote de Minecraft: una tapa invisible que tapa el agua que, si no, se vería adentro del bote. */
	private static void tapaAgua(PoseStack pose, MultiBufferSource buffers) {
		var v = buffers.getBuffer(RenderType.waterMask());
		var p = pose.last();
		float y = TAPA_AGUA / 16f, a = ANCHO / 16f, r = RECTO / 16f, pu = PUNTA / 16f;
		float[][] cuadros = {
				{-a, -r, a, -r, a, r, -a, r},     // el medio
				{-a, r, a, r, 0, pu, 0, pu},      // la punta de adelante
				{a, -r, -a, -r, 0, -pu, 0, -pu}}; // la de atrás
		for (float[] c : cuadros) {
			for (int i = 0; i < 4; i++) v.addVertex(p, c[i * 2], y, c[i * 2 + 1]);
			for (int i = 3; i >= 0; i--) v.addVertex(p, c[i * 2], y, c[i * 2 + 1]);
		}
	}

	static class Modelo extends GeoModel<BoteEntity> {
		private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "geo/entity/bote.geo.json");
		private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/bote.png");
		private static final ResourceLocation ANIMACIONES = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "animations/entity/bote.animation.json");

		@Override
		public ResourceLocation getModelResource(BoteEntity bote) {
			return MODELO;
		}

		@Override
		public ResourceLocation getTextureResource(BoteEntity bote) {
			return TEXTURA;
		}

		@Override
		public ResourceLocation getAnimationResource(BoteEntity bote) {
			return ANIMACIONES;
		}
	}
}
