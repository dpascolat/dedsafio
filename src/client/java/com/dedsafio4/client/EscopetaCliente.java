package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.EscopetaPhoraItem;
import com.dedsafio4.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * La Escopeta Phora del lado del cliente:
 * - Al lado de la barra de ítems: "balas/8", el Proyectil Phora y entre paréntesis cuántos tienes en total.
 * - En primera persona: la animación de agarrarla (sube desde abajo a la derecha, con un rebote, y bombea) y, al
 *   disparar, el retroceso y el bombeo (como en el diseño de Patricio, "Escopeta Primera Persona.html").
 * - Los agujeritos negros que dejan los perdigones en los bloques, durante 30 segundos.
 */
public final class EscopetaCliente {
	private EscopetaCliente() {}

	private static final ResourceLocation AGUJERO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/misc/agujero_bala.png");
	private static final int DURA_AGUJERO = 30 * 20, MAX_AGUJEROS = 400;
	private static final float MEDIO_AGUJERO = 0.08f;

	private record Agujero(Vec3 lugar, Direction cara, long hasta) {}

	private static final List<Agujero> AGUJEROS = new ArrayList<>();
	/** Cuándo la agarró (para la animación) y qué tenía antes en la mano. */
	private static long agarro = -1000;
	private static boolean teniaEscopeta;
	private static int lugarAntes = -1;

	/** Cuántos agujeritos hay ahora (para las pruebas). */
	public static int cantidadAgujeros() {
		return AGUJEROS.size();
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(EscopetaPhoraItem.AgujerosPayload.TYPE, (payload, contexto) -> contexto.client().execute(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) return;
			long hasta = mc.level.getGameTime() + DURA_AGUJERO;
			for (int i = 0; i < payload.lugares().size() && i < payload.caras().size(); i++) {
				Vector3f l = payload.lugares().get(i);
				AGUJEROS.add(new Agujero(new Vec3(l.x(), l.y(), l.z()), Direction.from3DDataValue(payload.caras().get(i)), hasta));
			}
			while (AGUJEROS.size() > MAX_AGUJEROS) AGUJEROS.remove(0);
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AGUJEROS.clear());
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null || mc.level == null) return;
			boolean tiene = mc.player.getMainHandItem().is(ModItems.ESCOPETA_PHORA);
			int lugar = mc.player.getInventory().selected;
			if (tiene && (!teniaEscopeta || lugar != lugarAntes)) agarro = mc.level.getGameTime();
			teniaEscopeta = tiene;
			lugarAntes = lugar;
			long ahora = mc.level.getGameTime();
			AGUJEROS.removeIf(a -> a.hasta() < ahora);
		});
		HudRenderCallback.EVENT.register((g, contador) -> hud(g));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(EscopetaCliente::dibujarAgujeros);
	}

	/** La R: si tiene la Escopeta Phora en la mano, le pide al servidor que la recargue. */
	public static void teclaRecargar() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getMainHandItem().is(ModItems.ESCOPETA_PHORA)) {
			ClientPlayNetworking.send(new EscopetaPhoraItem.RecargarPayload());
		}
	}

	// ---------------------------------------------------------------- La mira

	/**
	 * La mira de la escopeta (en lugar de la cruz de Minecraft): un puntito en el centro y 4 rayitas separadas,
	 * blancas con borde oscuro. Al disparar se abren y vuelven de a poco. Devuelve false si no toca (y queda la normal).
	 */
	public static boolean mira(GuiGraphics g, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || !mc.player.getMainHandItem().is(ModItems.ESCOPETA_PHORA)) return false;
		if (!mc.options.getCameraType().isFirstPerson() || mc.player.isSpectator()) return false;
		if (mc.getDebugOverlay().showDebugScreen()) return false;
		int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
		float d = (mc.level.getGameTime() + parcial - EscopetaPhoraItem.disparoCliente) / 20f;
		int abre = d >= 0 && d < 0.4f ? Math.round(4 * (1 - d / 0.4f)) : 0;
		int hueco = 4 + abre, largo = 5;
		int borde = 0xA0000000, blanco = 0xFFFFFFFF;
		// Primero el borde (un píxel más grande), después lo blanco.
		rectangulo(g, cx - 1, cy - 1, 3, 3, borde);
		rectangulo(g, cx - 1, cy - hueco - largo - 1, 3, largo + 2, borde);
		rectangulo(g, cx - 1, cy + hueco, 3, largo + 2, borde);
		rectangulo(g, cx - hueco - largo - 1, cy - 1, largo + 2, 3, borde);
		rectangulo(g, cx + hueco, cy - 1, largo + 2, 3, borde);
		rectangulo(g, cx, cy, 1, 1, blanco);
		rectangulo(g, cx, cy - hueco - largo, 1, largo, blanco);
		rectangulo(g, cx, cy + hueco + 1, 1, largo, blanco);
		rectangulo(g, cx - hueco - largo, cy, largo, 1, blanco);
		rectangulo(g, cx + hueco + 1, cy, largo, 1, blanco);
		return true;
	}

	private static void rectangulo(GuiGraphics g, int x, int y, int ancho, int alto, int color) {
		g.fill(x, y, x + ancho, y + alto, color);
	}

	// ---------------------------------------------------------------- Al lado de la barra de ítems

	private static void hud(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		if (p == null || mc.options.hideGui || p.isSpectator()) return;
		ItemStack escopeta = p.getMainHandItem().is(ModItems.ESCOPETA_PHORA) ? p.getMainHandItem()
				: p.getOffhandItem().is(ModItems.ESCOPETA_PHORA) ? p.getOffhandItem() : ItemStack.EMPTY;
		if (escopeta.isEmpty()) return;
		int x = g.guiWidth() / 2 + 91 + 8;
		// Si la mano secundaria tiene algo y se dibuja a la derecha, se corre para no taparla.
		if (!p.getOffhandItem().isEmpty() && p.getMainArm() == HumanoidArm.LEFT) x += 29;
		int y = g.guiHeight() - 19;
		String balas = EscopetaPhoraItem.balas(escopeta) + "/" + EscopetaPhoraItem.CARGADOR;
		int color = EscopetaPhoraItem.recargando(escopeta) ? 0xAAAAAA : 0xFFFFFF;
		g.drawString(mc.font, balas, x, y + 5, color, true);
		int xi = x + mc.font.width(balas) + 2;
		g.renderItem(new ItemStack(ModItems.PROYECTIL_PHORA), xi, y);
		String total = "(" + EscopetaPhoraItem.municionTotal(p) + ")";
		g.drawString(mc.font, total, xi + 17, y + 5, 0xFFFFFF, true);
	}

	// ---------------------------------------------------------------- Animación en primera persona

	private static float easeOutBack(float x) {
		float c1 = 1.6f, c3 = c1 + 1;
		return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
	}

	private static float easeInOutQuad(float x) {
		return x < 0.5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
	}

	/** El bombeo: sube de 0 a 1 entre "desde" y "desde + 0,2 s" y vuelve a 0 en los 0,2 s siguientes. */
	private static float bombeo(float t, float desde) {
		float a = t - desde;
		if (a <= 0 || a >= 0.4f) return 0;
		return a < 0.2f ? easeInOutQuad(a / 0.2f) : easeInOutQuad(1 - (a - 0.2f) / 0.2f);
	}

	/**
	 * Mueve la escopeta en primera persona antes de que Minecraft la dibuje (lo llama ItemInHandEscopetaMixin).
	 * Todo gira alrededor de donde está la escopeta en la mano, no de la cámara.
	 */
	public static void animar(PoseStack pose, AbstractClientPlayer jugador, InteractionHand mano, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		float ahora = mc.level.getGameTime() + parcial;
		boolean derecha = (mano == InteractionHand.MAIN_HAND) == (jugador.getMainArm() == HumanoidArm.RIGHT);
		float s = derecha ? 1 : -1;
		float px = 0, py = 0, pz = 0, rx = 0, ry = 0, rz = 0;

		// Agarrarla: de abajo a la derecha, apuntando al piso, hasta la mano (0,55 s) y después bombea (0,4 s).
		float t = mano == InteractionHand.MAIN_HAND ? (ahora - agarro) / 20f : 10f;
		if (t < 0.55f) {
			float k = 1 - easeOutBack(Mth.clamp(t / 0.55f, 0, 1));
			px += s * 0.16f * k;
			py += -0.74f * k;
			pz += 0.22f * k;
			rx += -1.10f * k;
			ry += s * 0.44f * k;
			rz += s * 0.60f * k;
		}
		float b = bombeo(t, 0.55f);

		// Disparo: retroceso (se levanta y viene para atrás) y después bombea.
		float d = mano == InteractionHand.MAIN_HAND ? (ahora - EscopetaPhoraItem.disparoCliente) / 20f : 10f;
		if (d >= 0 && d < 0.12f) {
			float r = 1 - d / 0.12f;
			rx += 0.30f * r;
			pz += 0.12f * r;
			py += 0.02f * r;
		}
		b = Math.max(b, bombeo(d, 0.15f) * 1.4f);

		// Recarga (2,5 s): la gira para mostrar el tambor, va metiendo los proyectiles (un golpecito cada uno),
		// la vuelve a poner derecha y bombea.
		ItemStack pila = mano == InteractionHand.MAIN_HAND ? jugador.getMainHandItem() : jugador.getOffhandItem();
		long fin = EscopetaPhoraItem.finRecargaDe(pila);
		if (fin > 0) {
			float p = Mth.clamp(1f - (fin - ahora) / EscopetaPhoraItem.RECARGA, 0f, 1f);
			float giro = p < 0.18f ? easeInOutQuad(p / 0.18f) : p > 0.78f ? easeInOutQuad(Mth.clamp((0.92f - p) / 0.14f, 0, 1)) : 1f;
			rz += s * 0.38f * giro;   // de costado: el tambor mira a la cámara
			rx += -0.12f * giro;      // la punta un poco para abajo
			px += -s * 0.04f * giro;
			py += -0.05f * giro;
			if (p > 0.22f && p < 0.74f) {
				float golpe = ((p - 0.22f) / 0.52f * 6f) % 1f;   // 6 golpecitos
				py += golpe < 0.25f ? 0.025f * (1 - golpe / 0.25f) : 0f;
			}
			b = Math.max(b, bombeo(p * 2.5f, 2.15f) * 1.4f);
		}
		rx += 0.08f * b;
		py -= 0.015f * b;
		pz += 0.05f * b;

		if (px == 0 && py == 0 && pz == 0 && rx == 0 && ry == 0 && rz == 0) return;
		float cx = s * 0.56f, cy = -0.52f, cz = -0.72f;   // donde Minecraft pone el ítem en la mano
		pose.translate(px, py, pz);
		pose.translate(cx, cy, cz);
		pose.mulPose(Axis.ZP.rotation(rz));
		pose.mulPose(Axis.YP.rotation(ry));
		pose.mulPose(Axis.XP.rotation(rx));
		pose.translate(-cx, -cy, -cz);
	}

	/** Dónde van los dos brazos (corridos desde donde Minecraft pone el brazo vacío): {hacia el centro, arriba, adelante(-)/atrás(+), giro}. */
	public static float[] BRAZO_DERECHO = {-0.02f, 0.02f, 0.02f, 0f}, BRAZO_IZQUIERDO = {0.72f, 0.28f, -0.2f, 0f};

	/** Las dos manos agarrando la escopeta: la derecha en la empuñadura y la izquierda debajo del guardamanos. */
	public static void brazos(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador) {
		if (jugador.isInvisible()) return;
		if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(jugador) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer r)) return;
		boolean derechaPrincipal = jugador.getMainArm() == HumanoidArm.RIGHT;
		brazo(pose, buffers, luz, jugador, r, derechaPrincipal, derechaPrincipal ? BRAZO_DERECHO : BRAZO_IZQUIERDO);
		brazo(pose, buffers, luz, jugador, r, !derechaPrincipal, derechaPrincipal ? BRAZO_IZQUIERDO : BRAZO_DERECHO);
	}

	private static void brazo(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
							  net.minecraft.client.renderer.entity.player.PlayerRenderer r, boolean derecha, float[] d) {
		float f = derecha ? 1 : -1;
		float dx = -f * d[0];   // d[0] > 0: hacia el centro de la pantalla
		pose.pushPose();
		// Lo mismo que hace Minecraft para dibujar el brazo con la mano vacía, corrido a donde agarra la escopeta.
		pose.translate(dx, d[1], d[2]);
		pose.translate(f * 0.64f, -0.6f, -0.72f);
		pose.mulPose(Axis.YP.rotationDegrees(f * 45f + f * d[3]));
		pose.translate(f * -1f, 3.6f, 3.5f);
		pose.mulPose(Axis.ZP.rotationDegrees(f * 120f));
		pose.mulPose(Axis.XP.rotationDegrees(200f));
		pose.mulPose(Axis.YP.rotationDegrees(f * -135f));
		pose.translate(f * 5.6f, 0f, 0f);
		if (derecha) r.renderRightHand(pose, buffers, luz, jugador);
		else r.renderLeftHand(pose, buffers, luz, jugador);
		pose.popPose();
	}

	// ---------------------------------------------------------------- Agujeritos de bala

	private static void dibujarAgujeros(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || AGUJEROS.isEmpty()) return;
		Vec3 camara = contexto.camera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		RenderType tipo = RenderType.entityCutoutNoCull(AGUJERO);
		VertexConsumer vc = buffers.getBuffer(tipo);
		PoseStack pose = new PoseStack();
		Matrix4f m = pose.last().pose();
		for (Agujero a : AGUJEROS) {
			if (a.lugar().distanceToSqr(camara) > 64 * 64) continue;
			Direction cara = a.cara();
			Vec3 n = new Vec3(cara.getStepX(), cara.getStepY(), cara.getStepZ());
			Vec3 c = a.lugar().add(n.scale(0.003)).subtract(camara);
			Vec3 u, v;
			if (cara.getAxis() == Direction.Axis.Y) { u = new Vec3(1, 0, 0); v = new Vec3(0, 0, 1); }
			else if (cara.getAxis() == Direction.Axis.X) { u = new Vec3(0, 0, 1); v = new Vec3(0, 1, 0); }
			else { u = new Vec3(1, 0, 0); v = new Vec3(0, 1, 0); }
			u = u.scale(MEDIO_AGUJERO);
			v = v.scale(MEDIO_AGUJERO);
			int luz = LevelRenderer.getLightColor(mc.level, BlockPos.containing(a.lugar().add(n.scale(0.2))));
			vertice(vc, pose, m, c.subtract(u).subtract(v), 0, 0, luz, n);
			vertice(vc, pose, m, c.add(u).subtract(v), 1, 0, luz, n);
			vertice(vc, pose, m, c.add(u).add(v), 1, 1, luz, n);
			vertice(vc, pose, m, c.subtract(u).add(v), 0, 1, luz, n);
		}
		buffers.endBatch(tipo);
	}

	private static void vertice(VertexConsumer vc, PoseStack pose, Matrix4f m, Vec3 p, float uu, float vv, int luz, Vec3 n) {
		vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(-1).setUv(uu, vv).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(luz).setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
	}
}
