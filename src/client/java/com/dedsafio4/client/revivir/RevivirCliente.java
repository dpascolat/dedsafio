package com.dedsafio4.client.revivir;

import com.dedsafio4.revivir.Revivir;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Los efectos de /revivir cinematica, dibujados lisos (no con partículas):
 * - 2 círculos en el piso (t=0 y t=100): borde turquesa y relleno tipo aurora verde que pulsa; se agrandan de 3,6 a
 *   14,6 bloques en 8 s, aparecen rápido y se desvanecen.
 * - 4 aros parados que giran en el centro de la fogata (verde azulejo y amarillo, alternados).
 * - La onda roja cuando sale el jugador y el anillo de polvo turquesa donde cae.
 * - La copia del jugador, con su skin, que hace todo hasta que cae al piso (el jugador de verdad, mientras, está en
 *   espectador). Desde que cae, el jugador de verdad hace la animación; al propio jugador le pone la pose (acostado)
 *   para que también se vea así con F5.
 */
public final class RevivirCliente {
	private RevivirCliente() {}

	private static final int TURQUESA = 0x12A59C, AURORA = 0x3FF0A8, AURORA_2 = 0x1FD38C, AZULEJO = 0x1FC8B8,
			AMARILLO = 0xFFD23A, ROJO = 0xEC3013;

	private record Escena(UUID jugador, Vec3 centro, Vec3 adelante, double pisoCaida, long inicio) {}

	private static final List<Escena> ESCENAS = new ArrayList<>();

	/** En qué momento de la cinemática está ese jugador (en ticks, con decimales), o -1 si no está en una. */
	public static float tiempo(UUID jugador, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return -1;
		for (Escena e : ESCENAS) {
			if (!e.jugador().equals(jugador)) continue;
			float t = mc.level.getGameTime() - e.inicio() + parcial;
			return t >= 0 && t < Revivir.DURACION ? t : -1;
		}
		return -1;
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Revivir.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			Minecraft mc = context.client();
			if (mc.level == null) return;
			ESCENAS.removeIf(e -> e.jugador().equals(payload.jugador()));
			ESCENAS.add(new Escena(payload.jugador(), new Vec3(payload.x(), payload.y(), payload.z()), Revivir.adelante(payload.yaw()),
					payload.pisoCaida(), mc.level.getGameTime()));
		}));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.level == null || mc.player == null) {
				ESCENAS.clear();
				return;
			}
			for (var it = ESCENAS.iterator(); it.hasNext(); ) {
				Escena e = it.next();
				int t = (int) (mc.level.getGameTime() - e.inicio());
				boolean yo = e.jugador().equals(mc.player.getUUID());
				if (t >= Revivir.DURACION) {
					if (yo) mc.player.setForcedPose(null);
					it.remove();
				} else if (yo) {
					mc.player.setForcedPose(Revivir.pose(t));
				}
			}
		});
		WorldRenderEvents.AFTER_TRANSLUCENT.register(RevivirCliente::dibujar);
	}

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (ESCENAS.isEmpty() || mc.level == null) return;
		Vec3 camara = contexto.camera().getPosition();
		float parcial = contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
		for (Escena e : ESCENAS) {
			float t = mc.level.getGameTime() - e.inicio() + parcial;
			if (t < 0 || t >= Revivir.DURACION) continue;
			Matrix4f m = new Matrix4f().translate((float) (e.centro().x - camara.x), (float) (e.centro().y - camara.y),
					(float) (e.centro().z - camara.z));
			circulosDelPiso(vc, m, t);
			aros(vc, m, t);
			// La onda roja cuando sale (0,85 s): un anillo que se agranda de 1 a 10 a la altura de la fogata.
			if (t >= 100 && t < 117) {
				float w = (t - 100) / 17f, radio = 1 + 9 * w;
				anillo(vc, m, 0, 3.05f, 0, radio * 0.9f, radio, ROJO, (1 - w) * 0.8f, ROJO, (1 - w) * 0.8f, 64);
			}
			// El polvo turquesa donde cae (0,9 s).
			if (t >= 258 && t < 276) {
				float d = (t - 258) / 18f, radio = 0.6f + 3 * d;
				Vec3 caida = e.adelante().scale(9);
				float y = (float) (e.pisoCaida() - e.centro().y) + 0.06f;
				anillo(vc, m, (float) caida.x, y, (float) caida.z, radio * 0.75f, radio, TURQUESA, 0, TURQUESA, (1 - d) * 0.9f, 48);
			}
		}
		buffers.endBatch(RenderType.debugQuads());
		// La copia con la skin, hasta que cae.
		for (Escena e : ESCENAS) {
			float t = mc.level.getGameTime() - e.inicio() + parcial;
			if (t >= 0 && t < Revivir.CAIDA) copia(mc, buffers, contexto, e, t, camara);
		}
	}

	private static net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity> modeloAncho, modeloFino;

	/** La copia del jugador con su skin, en su lugar y pose del momento t (como dibuja Minecraft a un jugador). */
	private static void copia(Minecraft mc, MultiBufferSource.BufferSource buffers, WorldRenderContext contexto, Escena e, float t, Vec3 camara) {
		var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(e.jugador());
		net.minecraft.client.resources.PlayerSkin skin = info != null ? info.getSkin()
				: net.minecraft.client.resources.DefaultPlayerSkin.get(e.jugador());
		boolean fino = skin.model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
		if (modeloAncho == null) {
			modeloAncho = new net.minecraft.client.model.PlayerModel<>(mc.getEntityModels().bakeLayer(
					net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
			modeloFino = new net.minecraft.client.model.PlayerModel<>(mc.getEntityModels().bakeLayer(
					net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM), true);
		}
		var m = fino ? modeloFino : modeloAncho;
		// Todo derecho, y después la pose del momento.
		for (var parte : new net.minecraft.client.model.geom.ModelPart[]{m.head, m.hat, m.body, m.rightArm, m.leftArm, m.rightLeg,
				m.leftLeg, m.leftSleeve, m.rightSleeve, m.leftPants, m.rightPants, m.jacket}) {
			parte.resetPose();
			parte.visible = true;
		}
		m.young = false;
		boolean agachado = RevivirAnimacion.agachado(t);
		m.crouching = agachado;
		if (agachado) {
			// Lo mismo que hace Minecraft al agacharse.
			m.body.xRot = 0.5f;
			m.body.y = 3.2f;
			m.head.y = 4.2f;
			m.rightArm.y = m.leftArm.y = 5.2f;
			m.rightLeg.z = m.leftLeg.z = 4.0f;
			m.rightLeg.y = m.leftLeg.y = 12.2f;
		}
		m.hat.copyFrom(m.head);
		RevivirAnimacion.aplicar(m, t);
		if (agachado) {
			m.rightArm.xRot += 0.4f;
			m.leftArm.xRot += 0.4f;
			m.leftSleeve.copyFrom(m.leftArm);
			m.rightSleeve.copyFrom(m.rightArm);
		}

		Vec3 pies = Revivir.posicion(t, e.centro(), e.adelante(), e.pisoCaida());
		float yaw = (float) Math.toDegrees(Math.atan2(-e.adelante().x, e.adelante().z));
		com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
		pose.translate(pies.x - camara.x, pies.y - camara.y, pies.z - camara.z);
		pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - yaw));
		pose.scale(-0.9375f, -0.9375f, 0.9375f);
		pose.translate(0, -1.501f, 0);
		int luz = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, net.minecraft.core.BlockPos.containing(pies.add(0, 0.5, 0)));
		RenderType tipo = m.renderType(skin.texture());
		m.renderToBuffer(pose, buffers.getBuffer(tipo), luz, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
		buffers.endBatch(tipo);
	}

	/** Los 2 círculos del piso: el relleno aurora (más transparente hacia adentro) y el borde turquesa. */
	private static void circulosDelPiso(VertexConsumer vc, Matrix4f m, float t) {
		for (int inicio : new int[]{0, 100}) {
			if (t < inicio || t >= inicio + 160) continue;
			float p = (t - inicio) / 160f;
			float radio = 3.6f + 11 * p, alfa = Math.min(1, p * 6) * (1 - p);
			// La aurora pulsa (±10 %, 0,35 veces por segundo) y cambia entre verde y verde azulado.
			float pulso = 1 + 0.1f * Mth.sin((float) (2 * Math.PI * 0.35 * t / 20));
			int aurora = mezclar(AURORA, AURORA_2, 0.5f + 0.5f * Mth.sin(t * 0.08f));
			anillo(vc, m, 0, 0.03f + inicio * 0.0002f, 0, radio * 0.36f, radio * 0.94f, aurora, 0, aurora, 0.35f * alfa * pulso, 96);
			anillo(vc, m, 0, 0.035f + inicio * 0.0002f, 0, radio * 0.94f, radio, TURQUESA, alfa, TURQUESA, alfa, 96);
		}
	}

	/** Los 4 aros parados que giran sobre el eje vertical, en el centro de la fogata (1,8 de alto). */
	private static void aros(VertexConsumer vc, Matrix4f m, float t) {
		float[] radios = {1.8f, 2.1f, 2.4f, 2.7f}, velocidades = {0.8f, 1.15f, 1.5f, 1.85f};
		for (int i = 0; i < 4; i++) {
			double giro = i * Math.PI / 4 + (i % 2 == 0 ? 1 : -1) * velocidades[i] * t / 20.0;
			Vec3 d = new Vec3(Math.cos(giro), 0, Math.sin(giro)), n = new Vec3(-Math.sin(giro), 0, Math.cos(giro));
			aro(vc, m, new Vec3(0, 1.8, 0), d, n, radios[i], 0.06f, i % 2 == 0 ? AZULEJO : AMARILLO, 64);
		}
	}

	/**
	 * Un aro parado: dos cintas finas cruzadas (una en el plano del aro y otra de costado), así se ve como un tubo
	 * desde cualquier lado.
	 */
	private static void aro(VertexConsumer vc, Matrix4f m, Vec3 centro, Vec3 d, Vec3 n, float radio, float grosor, int color, int partes) {
		for (int j = 0; j < partes; j++) {
			double a0 = 2 * Math.PI * j / partes, a1 = 2 * Math.PI * (j + 1) / partes;
			Vec3 u0 = d.scale(Math.cos(a0)).add(0, Math.sin(a0), 0), u1 = d.scale(Math.cos(a1)).add(0, Math.sin(a1), 0);
			Vec3 p0 = centro.add(u0.scale(radio)), p1 = centro.add(u1.scale(radio));
			// En el plano del aro (hacia afuera y hacia adentro).
			cuad(vc, m, p0.add(u0.scale(-grosor)), p0.add(u0.scale(grosor)), p1.add(u1.scale(grosor)), p1.add(u1.scale(-grosor)), color, 1f);
			// De costado.
			cuad(vc, m, p0.add(n.scale(-grosor)), p0.add(n.scale(grosor)), p1.add(n.scale(grosor)), p1.add(n.scale(-grosor)), color, 1f);
		}
	}

	/** Un anillo acostado (de radio adentro a radio afuera), con color y transparencia que pueden cambiar de adentro a afuera. */
	private static void anillo(VertexConsumer vc, Matrix4f m, float cx, float y, float cz, float adentro, float afuera,
							   int colorAdentro, float alfaAdentro, int colorAfuera, float alfaAfuera, int partes) {
		for (int j = 0; j < partes; j++) {
			double a0 = 2 * Math.PI * j / partes, a1 = 2 * Math.PI * (j + 1) / partes;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			vertice(vc, m, cx + c0 * adentro, y, cz + s0 * adentro, colorAdentro, alfaAdentro);
			vertice(vc, m, cx + c0 * afuera, y, cz + s0 * afuera, colorAfuera, alfaAfuera);
			vertice(vc, m, cx + c1 * afuera, y, cz + s1 * afuera, colorAfuera, alfaAfuera);
			vertice(vc, m, cx + c1 * adentro, y, cz + s1 * adentro, colorAdentro, alfaAdentro);
			// Y al revés, para que se vea también desde abajo.
			vertice(vc, m, cx + c1 * adentro, y, cz + s1 * adentro, colorAdentro, alfaAdentro);
			vertice(vc, m, cx + c1 * afuera, y, cz + s1 * afuera, colorAfuera, alfaAfuera);
			vertice(vc, m, cx + c0 * afuera, y, cz + s0 * afuera, colorAfuera, alfaAfuera);
			vertice(vc, m, cx + c0 * adentro, y, cz + s0 * adentro, colorAdentro, alfaAdentro);
		}
	}

	/** Un cuadrilátero de las dos caras. */
	private static void cuad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alfa) {
		for (Vec3 v : new Vec3[]{a, b, c, d, d, c, b, a}) vertice(vc, m, (float) v.x, (float) v.y, (float) v.z, color, alfa);
	}

	private static void vertice(VertexConsumer vc, Matrix4f m, float x, float y, float z, int color, float alfa) {
		vc.addVertex(m, x, y, z).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, (int) (Mth.clamp(alfa, 0, 1) * 255));
	}

	private static int mezclar(int a, int b, float k) {
		int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255), g = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255),
				bl = (int) Mth.lerp(k, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}
}
