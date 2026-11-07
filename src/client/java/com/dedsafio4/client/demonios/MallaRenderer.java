package com.dedsafio4.client.demonios;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.demonios.MallaMobEntity;
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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja los mobs hechos con modelos 3D de malla (los .glb que pasó Patricio, achicados): la malla está en
 * modelos/NOMBRE.bin (en "píxeles", con los pies en y = 0 y 16 de alto) y se agranda a ALTO bloques. Los que flotan
 * se mecen arriba y abajo. Respeta el tamaño de /boss tamano.
 */
public class MallaRenderer extends EntityRenderer<MallaMobEntity> {
	private static final Map<ResourceLocation, float[]> MALLAS = new HashMap<>();

	private final ResourceLocation malla, textura;
	private final float alto;

	public MallaRenderer(EntityRendererProvider.Context contexto, String malla, String textura, float alto) {
		super(contexto);
		this.malla = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "modelos/" + malla + ".bin");
		this.textura = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/" + textura + ".png");
		this.alto = alto;
		this.shadowRadius = 0.8f;
	}

	@Override
	public ResourceLocation getTextureLocation(MallaMobEntity mob) {
		return textura;
	}

	/** Los triángulos de la malla (todos los grupos juntos): x, y, z, u, v por punto. */
	public static float[] cargar(ResourceLocation donde) {
		return MALLAS.computeIfAbsent(donde, d -> {
			try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(d).open();
				 DataInputStream datos = new DataInputStream(new java.io.BufferedInputStream(in))) {
				int n = datos.readInt();
				java.util.List<float[]> partes = new java.util.ArrayList<>();
				int total = 0;
				for (int i = 0; i < n; i++) {
					datos.readFloat();
					datos.readFloat();
					datos.readFloat();
					float[] g = new float[datos.readInt() * 15];
					for (int k = 0; k < g.length; k++) g[k] = datos.readFloat();
					partes.add(g);
					total += g.length;
				}
				float[] todo = new float[total];
				int o = 0;
				for (float[] g : partes) {
					System.arraycopy(g, 0, todo, o, g.length);
					o += g.length;
				}
				return todo;
			} catch (Exception e) {
				Dedsafio4.LOGGER.error("No se pudo leer la malla " + d, e);
				return new float[0];
			}
		});
	}

	@Override
	public void render(MallaMobEntity mob, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		float[] tris = cargar(malla);
		float t = mob.tickCount + parcial;
		pose.pushPose();
		if (mob.flota()) pose.translate(0, 0.25f + Mth.sin(t * 0.08f) * 0.15f, 0);
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, mob.yBodyRotO, mob.yBodyRot)));
		if (mob.deathTime > 0) {
			float m = Math.min(1f, (mob.deathTime + parcial) / 20f);
			pose.mulPose(Axis.ZP.rotationDegrees(m * 90f));
		}
		float e = alto / 16f * mob.getScale();
		pose.scale(e, e, e);
		int overlay = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(mob.hurtTime > 0 || mob.deathTime > 0));
		dibujar(tris, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(textura)), luz, overlay);
		pose.popPose();
		super.render(mob, giro, parcial, pose, buffers, luz);
	}

	/** Dibuja triángulos (x, y, z, u, v por punto) como cuadrados con el último punto repetido. */
	public static void dibujar(float[] tris, PoseStack pose, VertexConsumer v, int luz, int overlay) {
		PoseStack.Pose p = pose.last();
		Matrix4f m = p.pose();
		Matrix3f nm = p.normal();
		Vector3f n = new Vector3f();
		Vector3f q = new Vector3f();
		for (int i = 0; i + 14 < tris.length; i += 15) {
			float ax = tris[i], ay = tris[i + 1], az = tris[i + 2];
			float bx = tris[i + 5], by = tris[i + 6], bz = tris[i + 7];
			float cx = tris[i + 10], cy = tris[i + 11], cz = tris[i + 12];
			n.set((by - ay) * (cz - az) - (bz - az) * (cy - ay), (bz - az) * (cx - ax) - (bx - ax) * (cz - az), (bx - ax) * (cy - ay) - (by - ay) * (cx - ax));
			if (n.lengthSquared() < 1e-12f) continue;
			n.normalize().mul(nm).normalize();
			for (int k = 0; k < 4; k++) {
				int j = i + Math.min(k, 2) * 5;
				m.transformPosition(tris[j], tris[j + 1], tris[j + 2], q);
				v.addVertex(q.x, q.y, q.z, 0xFFFFFFFF, tris[j + 3], tris[j + 4], overlay, luz, n.x, n.y, n.z);
			}
		}
	}

	@Override
	protected boolean shouldShowName(MallaMobEntity mob) {
		return mob.hasCustomName() && super.shouldShowName(mob);
	}
}
