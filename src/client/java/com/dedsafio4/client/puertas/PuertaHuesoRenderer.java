package com.dedsafio4.client.puertas;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.puertas.PuertaHuesoAnimacion;
import com.dedsafio4.puertas.PuertaHuesoEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.Reader;

/**
 * Dibuja la Puerta de Hueso: las dos hojas del resource pack "Puerta de Hueso y Chatarra" (convertidas a
 * puerta_hueso/modelo.json: por cara, 4 × (x, y, z, u, v) y la normal, en píxeles), estiradas al grosor y al alto de la
 * puerta, cada una girando sobre su bisagra (ver PuertaHuesoAnimacion).
 */
public class PuertaHuesoRenderer extends EntityRenderer<PuertaHuesoEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/puerta_hueso.png");
	private static final ResourceLocation MODELO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "puerta_hueso/modelo.json");
	/** El grosor (en bloques) que tiene el modelo tal cual. */
	private static final float GROSOR_MODELO = PuertaHuesoEntity.GRUESO_NORMAL;

	private static float[] hojaA, hojaB;

	public PuertaHuesoRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
	}

	private static void cargar() {
		if (hojaA != null) return;
		hojaA = hojaB = new float[0];
		try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(MODELO)) {
			JsonObject m = JsonParser.parseReader(lector).getAsJsonObject();
			hojaA = numeros(m.getAsJsonArray("a"));
			hojaB = numeros(m.getAsJsonArray("b"));
		} catch (Exception ex) {
			Dedsafio4.LOGGER.error("No se pudo cargar el modelo de la Puerta de Hueso", ex);
		}
	}

	private static float[] numeros(JsonArray a) {
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	@Override
	public void render(PuertaHuesoEntity puerta, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		cargar();
		PuertaHuesoAnimacion anim = puerta.animacion;
		float sy = puerta.alto() / 2f, sz = puerta.grueso() / GROSOR_MODELO;
		int lado = puerta.lado();
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));

		pose.pushPose();
		float temblor = anim.temblor(parcial);
		if (temblor > 0) {
			float t = puerta.tickCount + parcial;
			pose.translate(Mth.sin(t * 2.3f) * temblor, 0, Mth.cos(t * 1.9f) * temblor * 0.5f);
		}
		pose.mulPose(Axis.YP.rotationDegrees(-puerta.getYRot()));
		pose.scale(1 / 16f, 1 / 16f, 1 / 16f);

		float abierta = anim.apertura(parcial), golpe = anim.abombado(parcial);
		float angA, angB;
		if (anim.reventada()) {
			angA = anim.hojaA(parcial) * lado;
			angB = anim.hojaB(parcial) * lado;
		} else {
			// Se abren hacia atrás; el golpe las abomba hacia donde empujó.
			angA = -abierta + golpe * lado;
			angB = -abierta + golpe * 0.85f * lado;
		}

		// Hoja a: bisagra en el borde izquierdo (x = -16). Si se arrancó, además cae acostada hacia donde empujó.
		pose.pushPose();
		float caida = anim.reventada() ? anim.caidaA(parcial) : 0;
		if (caida > 0) {
			float borde = lado * puerta.grueso() * 8;
			pose.translate(0, 0, borde);
			pose.mulPose(Axis.XP.rotation(caida * lado));
			pose.translate(0, 0, -borde);
		}
		hoja(pose, vc, luz, hojaA, -16, -angA, sy, sz);
		pose.popPose();

		// Hoja b: bisagra en el borde derecho (x = 16).
		pose.pushPose();
		hoja(pose, vc, luz, hojaB, 16, angB, sy, sz);
		pose.popPose();

		pose.popPose();
		super.render(puerta, yaw, parcial, pose, buffers, luz);
	}

	/** Una hoja: gira sobre su bisagra (en x) y se estira al alto y al grosor de la puerta. */
	private static void hoja(PoseStack pose, VertexConsumer vc, int luz, float[] q, float bisagra, float angulo, float sy, float sz) {
		pose.translate(bisagra, 0, 0);
		pose.mulPose(Axis.YP.rotation(angulo));
		pose.scale(1, sy, sz);
		pose.translate(-bisagra, 0, 0);
		PoseStack.Pose ultimo = pose.last();
		for (int k = 0; k + 23 <= q.length; k += 23) {
			for (int v = 0; v < 4; v++) {
				int o = k + v * 5;
				vc.addVertex(ultimo, q[o], q[o + 1], q[o + 2]).setColor(-1).setUv(q[o + 3], q[o + 4])
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(luz).setNormal(ultimo, q[k + 20], q[k + 21], q[k + 22]);
			}
		}
	}

	@Override
	public ResourceLocation getTextureLocation(PuertaHuesoEntity puerta) {
		return TEXTURA;
	}
}
