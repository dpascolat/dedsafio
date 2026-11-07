package com.dedsafio4.client.bossplanta;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bossplanta.PlantaBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Dibuja la Planta Mordedora: la malla del modelo (modelos/planta_boss.bin, hecha del boss.bbmodel: el cuerpo y la
 * mandíbula, que se abre girando en su pivote), más:
 * <ul>
 * <li>Quieta se mece despacio y abre un poco la boca, como respirando.</li>
 * <li>Luz: baila (se balancea, gira y salta) y arriba aparece una luz blanca y amarilla con rayos.</li>
 * <li>Mordida: lo rojo sobre los bloques alrededor (titila más rápido justo antes de morder) y la mandíbula se abre y
 *     se cierra de golpe en cada mordida.</li>
 * </ul>
 */
public class PlantaBossRenderer extends EntityRenderer<PlantaBossEntity> {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/planta_boss.png");
	private static final ResourceLocation MALLA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "modelos/planta_boss.bin");

	/** Cada grupo: pivote y triángulos (x, y, z, u, v por punto; 15 números por triángulo). */
	private static float[][] grupos;
	private static float[][] pivotes;

	public PlantaBossRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 1.6f;
	}

	@Override
	public ResourceLocation getTextureLocation(PlantaBossEntity planta) {
		return TEXTURA;
	}

	private static void cargar() {
		if (grupos != null) return;
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MALLA).open();
			 DataInputStream datos = new DataInputStream(new java.io.BufferedInputStream(in))) {
			int n = datos.readInt();
			float[][] g = new float[n][];
			float[][] p = new float[n][];
			for (int i = 0; i < n; i++) {
				p[i] = new float[]{datos.readFloat(), datos.readFloat(), datos.readFloat()};
				int tris = datos.readInt();
				g[i] = new float[tris * 15];
				for (int k = 0; k < g[i].length; k++) g[i][k] = datos.readFloat();
			}
			pivotes = p;
			grupos = g;
		} catch (Exception e) {
			Dedsafio4.LOGGER.error("No se pudo leer la malla de la Planta Mordedora", e);
			grupos = new float[0][];
			pivotes = new float[0][];
		}
	}

	@Override
	public void render(PlantaBossEntity planta, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		cargar();
		float t = planta.tickCount + parcial;
		int ataque = planta.ataque();
		float ta = planta.tiempoAtaque(parcial);

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, planta.yBodyRotO, planta.yBodyRot)));
		// Muriendo: se inclina y se hunde.
		if (planta.deathTime > 0) {
			float m = Math.min(1f, (planta.deathTime + parcial) / 20f);
			pose.translate(0, -m * 1.5f, 0);
			pose.mulPose(Axis.XP.rotationDegrees(-m * 70f));
		}
		// Se mece despacio siempre; en la Luz, baila.
		float baile = ataque == PlantaBossEntity.LUZ ? Mth.clamp(Math.min(ta / 15f, (PlantaBossEntity.DURA_LUZ - ta) / 15f), 0, 1) : 0;
		pose.translate(0, Math.abs(Mth.sin(t * 0.45f)) * 0.35f * baile, 0);
		pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(t * 0.22f) * 30f * baile));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.06f) * 2f + Mth.sin(t * 0.45f) * 14f * baile));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.05f + 1f) * 1.5f + Mth.cos(t * 0.3f) * 5f * baile));
		pose.scale(PlantaBossEntity.ESCALA, PlantaBossEntity.ESCALA, PlantaBossEntity.ESCALA);

		int overlay = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(planta.hurtTime > 0 || planta.deathTime > 0));
		VertexConsumer v = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		if (grupos.length > 0) dibujar(grupos[0], pose, v, luz, overlay);
		if (grupos.length > 1) {
			float[] pv = pivotes[1];
			pose.pushPose();
			pose.translate(pv[0], pv[1], pv[2]);
			pose.mulPose(Axis.XP.rotationDegrees(mandibula(planta, ataque, ta, t, baile)));
			pose.translate(-pv[0], -pv[1], -pv[2]);
			dibujar(grupos[1], pose, v, luz, overlay);
			pose.popPose();
		}
		pose.popPose();

		if (ataque == PlantaBossEntity.LUZ) luz(planta, ta, t, pose, buffers);
		if (ataque == PlantaBossEntity.MORDIDA && ta < PlantaBossEntity.MARCA + 4) zonaRoja(planta, ta, parcial, pose, buffers);
		super.render(planta, giro, parcial, pose, buffers, luz);
	}

	/** Cuánto se abre la mandíbula (grados). */
	private static float mandibula(PlantaBossEntity planta, int ataque, float ta, float t, float baile) {
		float quieta = 3f + Mth.sin(t * 0.08f) * 3f;
		if (ataque == PlantaBossEntity.LUZ) return quieta + 22f * baile + Mth.sin(t * 0.9f) * 4f * baile;
		if (ataque != PlantaBossEntity.MORDIDA) return quieta;
		int marca = PlantaBossEntity.MARCA, entre = PlantaBossEntity.ENTRE_MORDIDAS;
		if (ta < marca) return quieta + 30f * Mth.clamp((ta - 10) / (marca - 15f), 0, 1);       // se va abriendo
		boolean agarro = planta.agarradoId() >= 0;
		if (!agarro) return ta < marca + 4 ? Mth.lerp((ta - marca) / 4f, 33f, 0f) : quieta;  // tarascón al aire
		float k = (ta - marca) % entre;
		if (ta - marca >= entre * PlantaBossEntity.MORDIDAS) return quieta;
		// Se abre despacio y cierra de golpe en cada mordida.
		return k < entre - 4 ? 38f * k / (entre - 4) : 38f * (1 - (k - (entre - 4)) / 4f);
	}

	private static void dibujar(float[] tris, PoseStack pose, VertexConsumer v, int luz, int overlay) {
		PoseStack.Pose p = pose.last();
		Matrix4f m = p.pose();
		Matrix3f nm = p.normal();
		Vector3f n = new Vector3f();
		for (int i = 0; i < tris.length; i += 15) {
			float ax = tris[i], ay = tris[i + 1], az = tris[i + 2];
			float bx = tris[i + 5], by = tris[i + 6], bz = tris[i + 7];
			float cx = tris[i + 10], cy = tris[i + 11], cz = tris[i + 12];
			n.set((by - ay) * (cz - az) - (bz - az) * (cy - ay), (bz - az) * (cx - ax) - (bx - ax) * (cz - az), (bx - ax) * (cy - ay) - (by - ay) * (cx - ax));
			if (n.lengthSquared() < 1e-12f) continue;
			n.normalize().mul(nm).normalize();
			for (int k = 0; k < 4; k++) {
				int j = i + Math.min(k, 2) * 5;
				Vector3f q = m.transformPosition(tris[j], tris[j + 1], tris[j + 2], new Vector3f());
				v.addVertex(q.x, q.y, q.z, 0xFFFFFFFF, tris[j + 3], tris[j + 4], overlay, luz, n.x, n.y, n.z);
			}
		}
	}

	/** La luz de arriba: varias capas que brillan (siempre mirando a la cámara) y rayos que giran. */
	private void luz(PlantaBossEntity planta, float ta, float t, PoseStack pose, MultiBufferSource buffers) {
		float prendida = Mth.clamp((ta - PlantaBossEntity.AVISO_LUZ + 10) / 15f, 0, 1)
				* Mth.clamp((PlantaBossEntity.FIN_LUZ - ta) / 8f, 0, 1);
		if (prendida <= 0) return;
		pose.pushPose();
		pose.translate(0, PlantaBossEntity.ALTO_LUZ, 0);
		pose.mulPose(entityRenderDispatcher.cameraOrientation());
		VertexConsumer v = buffers.getBuffer(RenderType.lightning());
		Matrix4f m = pose.last().pose();
		float pulso = 1f + Mth.sin(t * 0.7f) * 0.08f;
		// Rayos
		for (int i = 0; i < 10; i++) {
			float a = t * 0.03f + i * Mth.TWO_PI / 10f;
			float largo = (4.5f + Mth.sin(t * 0.4f + i * 1.7f) * 1.2f) * prendida;
			float ancho = 0.18f * prendida;
			float cx = Mth.cos(a), sy = Mth.sin(a), px = -sy * ancho, py = cx * ancho;
			int alfa = (int) (90 * prendida);
			v.addVertex(m, px, py, 0).setColor(255, 250, 200, alfa);
			v.addVertex(m, -px, -py, 0).setColor(255, 250, 200, alfa);
			v.addVertex(m, cx * largo, sy * largo, 0).setColor(255, 230, 120, 0);
			v.addVertex(m, cx * largo, sy * largo, 0).setColor(255, 230, 120, 0);
		}
		// Bolas de luz
		float[][] capas = {{3.2f, 255, 220, 120, 40}, {2.2f, 255, 240, 170, 70}, {1.3f, 255, 255, 220, 140}, {0.7f, 255, 255, 255, 230}};
		for (float[] c : capas) {
			float r = c[0] * prendida * pulso;
			int al = (int) (c[4] * prendida);
			int cr = (int) c[1], cg = (int) c[2], cb = (int) c[3];
			for (int i = 0; i < 12; i++) {   // un disco de 12 porciones (centro fuerte, borde transparente)
				float a0 = i * Mth.TWO_PI / 12f, a1 = (i + 1) * Mth.TWO_PI / 12f;
				v.addVertex(m, 0, 0, 0).setColor(cr, cg, cb, al);
				v.addVertex(m, 0, 0, 0).setColor(cr, cg, cb, al);
				v.addVertex(m, Mth.cos(a0) * r, Mth.sin(a0) * r, 0).setColor(cr, cg, cb, 0);
				v.addVertex(m, Mth.cos(a1) * r, Mth.sin(a1) * r, 0).setColor(cr, cg, cb, 0);
			}
		}
		pose.popPose();
	}

	/** Lo rojo sobre los bloques alrededor de la planta (encima de cada bloque, hasta ZONA bloques). */
	private static void zonaRoja(PlantaBossEntity planta, float ta, float parcial, PoseStack pose, MultiBufferSource buffers) {
		Level mundo = planta.level();
		double ex = Mth.lerp(parcial, planta.xo, planta.getX()), ey = Mth.lerp(parcial, planta.yo, planta.getY()), ez = Mth.lerp(parcial, planta.zo, planta.getZ());
		int zona = PlantaBossEntity.ZONA;
		BlockPos centro = BlockPos.containing(planta.getX(), planta.getY(), planta.getZ());
		// Aparece de a poco desde la planta hacia afuera y titila más rápido cuando está por morder.
		float crece = Mth.clamp(ta / 12f, 0, 1);
		float velocidad = ta < PlantaBossEntity.MARCA - 20 ? 0.25f : 0.8f;
		float alfa = (0.58f + 0.17f * Mth.sin(ta * velocidad)) * Mth.clamp((PlantaBossEntity.MARCA + 4 - ta) / 4f, 0, 1);
		VertexConsumer v = buffers.getBuffer(RenderType.debugQuads());
		Matrix4f m = pose.last().pose();
		BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
		float limite = (zona + 0.5f) * crece;
		for (int dx = -zona; dx <= zona; dx++) {
			for (int dz = -zona; dz <= zona; dz++) {
				if (dx * dx + dz * dz > limite * limite) continue;
				int x = centro.getX() + dx, z = centro.getZ() + dz;
				// El bloque de arriba de todo (cerca de la altura de la planta).
				int y = Integer.MIN_VALUE;
				for (int yy = centro.getY() + 2; yy >= centro.getY() - 4; yy--) {
					bp.set(x, yy, z);
					if (!mundo.getBlockState(bp).getCollisionShape(mundo, bp).isEmpty()) {
						y = yy;
						break;
					}
				}
				if (y == Integer.MIN_VALUE) continue;
				bp.set(x, y, z);
				double alto = mundo.getBlockState(bp).getCollisionShape(mundo, bp).max(net.minecraft.core.Direction.Axis.Y);
				float x0 = (float) (x - ex), z0 = (float) (z - ez), yy = (float) (y + alto + 0.02 - ey);
				int a = (int) (255 * alfa);
				v.addVertex(m, x0, yy, z0).setColor(255, 10, 20, a);
				v.addVertex(m, x0, yy, z0 + 1).setColor(255, 10, 20, a);
				v.addVertex(m, x0 + 1, yy, z0 + 1).setColor(255, 10, 20, a);
				v.addVertex(m, x0 + 1, yy, z0).setColor(255, 10, 20, a);
			}
		}
	}

	@Override
	public boolean shouldRender(PlantaBossEntity planta, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
		return true;
	}

	/** No muestra el nombre arriba (ya está la barra de jefe). */
	@Override
	protected boolean shouldShowName(PlantaBossEntity planta) {
		return false;
	}
}
