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
	/** Cuánto está apuntando (0 normal, 1 apuntando con el clic derecho), antes y ahora (se mezcla entre ticks). */
	private static float apuntaAntes, apunta;
	private static boolean teniaEscopeta;
	private static int lugarAntes = -1;

	/** Cuántos agujeritos hay ahora (para las pruebas). */
	public static int cantidadAgujeros() {
		return AGUJEROS.size();
	}

	/** Una bala que vuela: de dónde sale, dónde pega y cuándo salió (en ticks del mundo). */
	private record Bala(Vec3 desde, Vec3 hasta, long salio) {}

	private static final java.util.List<Bala> BALAS = new java.util.ArrayList<>();
	/** Lo que avanza la bala por tick (en bloques), y su tamaño: el rayo de la nave 4 veces más chico, en naranja. */
	private static final double VELOCIDAD_BALA = 8;
	private static final float MEDIO_LARGO_BALA = 0.5f / 4, MEDIO_LADO_BALA = 2.5f / 16f / 4;
	private static final float[] NARANJA_BALA = {1f, 0.45f, 0.08f, 0.9f}, NUCLEO_BALA = {1f, 0.85f, 0.55f, 1f};

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(EscopetaPhoraItem.BalasPayload.TYPE, (payload, contexto) -> contexto.client().execute(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) return;
			for (int i = 0; i < payload.desde().size() && i < payload.hasta().size(); i++) {
				Vector3f a = payload.desde().get(i), b = payload.hasta().get(i);
				BALAS.add(new Bala(new Vec3(a.x(), a.y(), a.z()), new Vec3(b.x(), b.y(), b.z()), mc.level.getGameTime()));
			}
			while (BALAS.size() > 200) BALAS.remove(0);
		}));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(EscopetaCliente::dibujarBalas);
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
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			AGUJEROS.clear();
			BALAS.clear();
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null || mc.level == null) return;
			boolean tiene = mc.player.getMainHandItem().getItem() instanceof EscopetaPhoraItem;
			int lugar = mc.player.getInventory().selected;
			if (tiene && (!teniaEscopeta || lugar != lugarAntes)) agarro = mc.level.getGameTime();
			apuntaAntes = apunta;
			boolean apuntando = tiene && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof EscopetaPhoraItem;
			apunta = Mth.approach(apunta, apuntando ? 1f : 0f, 0.25f);
			teniaEscopeta = tiene;
			lugarAntes = lugar;
			long ahora = mc.level.getGameTime();
			AGUJEROS.removeIf(a -> a.hasta() < ahora);
		});
		HudRenderCallback.EVENT.register((g, contador) -> hud(g));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(EscopetaCliente::dibujarAgujeros);
	}

	/** Cuánto está apuntando ahora (0 a 1). */
	public static float apuntando(float parcial) {
		return Mth.lerp(parcial, apuntaAntes, apunta);
	}

	/**
	 * Clic izquierdo con la escopeta en la mano (lo llama MinecraftDispararMixin): dispara en vez de pegar o romper.
	 * Devuelve true si la tenía (así no pega ni rompe bloques).
	 */
	public static boolean clicIzquierdo() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.player.isSpectator()) return false;
		ItemStack pila = mc.player.getMainHandItem();
		if (!(pila.getItem() instanceof EscopetaPhoraItem)) return false;
		if (!mc.player.getCooldowns().isOnCooldown(pila.getItem()) && !EscopetaPhoraItem.recargando(pila)) {
			if (EscopetaPhoraItem.balas(pila) > 0) EscopetaPhoraItem.disparoCliente = mc.level.getGameTime();
			ClientPlayNetworking.send(new EscopetaPhoraItem.DispararPayload());
		}
		return true;
	}

	/** La R: si tiene la Escopeta Phora en la mano, le pide al servidor que la recargue. */
	public static void teclaRecargar() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof EscopetaPhoraItem) {
			ClientPlayNetworking.send(new EscopetaPhoraItem.RecargarPayload());
		}
	}

	/** Las balas que van volando: un rayo naranja chiquito que va del cañón al impacto y desaparece al llegar. */
	private static void dibujarBalas(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (BALAS.isEmpty() || mc.level == null) return;
		float ahora = mc.level.getGameTime() + contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		Vec3 camara = contexto.camera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
		for (var it = BALAS.iterator(); it.hasNext(); ) {
			Bala b = it.next();
			Vec3 recorrido = b.hasta().subtract(b.desde());
			double largo = recorrido.length(), avanzo = (ahora - b.salio()) * VELOCIDAD_BALA;
			if (largo < 1.0E-3 || avanzo > largo + VELOCIDAD_BALA) {
				it.remove();
				continue;
			}
			Vec3 dir = recorrido.scale(1 / largo);
			Vec3 punta = b.desde().add(dir.scale(Math.min(avanzo, largo))).subtract(dir.scale(MEDIO_LARGO_BALA)).subtract(camara);
			PoseStack pose = new PoseStack();
			pose.translate(punta.x, punta.y, punta.z);
			pose.mulPose(com.mojang.math.Axis.YP.rotation((float) -Math.atan2(dir.z, dir.x)));
			pose.mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.asin(Mth.clamp(dir.y, -1, 1))));
			org.joml.Matrix4f m = pose.last().pose();
			com.dedsafio4.client.nave.RayoRenderer.caja(vc, m, MEDIO_LARGO_BALA, MEDIO_LADO_BALA, NARANJA_BALA);
			com.dedsafio4.client.nave.RayoRenderer.caja(vc, m, MEDIO_LARGO_BALA * 0.95f, MEDIO_LADO_BALA * 0.45f, NUCLEO_BALA);
		}
		buffers.endBatch(net.minecraft.client.renderer.RenderType.lightning());
	}

	// ---------------------------------------------------------------- La mira

	/**
	 * La mira de la escopeta (en lugar de la cruz de Minecraft): un puntito en el centro y 4 rayitas separadas,
	 * blancas con borde oscuro. Al disparar se abren y vuelven de a poco. Devuelve false si no toca (y queda la normal).
	 */
	public static boolean mira(GuiGraphics g, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || !(mc.player.getMainHandItem().getItem() instanceof EscopetaPhoraItem)) return false;
		if (!mc.options.getCameraType().isFirstPerson() || mc.player.isSpectator()) return false;
		if (mc.getDebugOverlay().showDebugScreen()) return false;
		if (miraTelescopica(parcial)) {
			telescopica(g);
			return true;
		}
		if (apuntando(parcial) > 0.5f) return true;   // apuntando: sin mira, se mira por arriba de la escopeta
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

	/** Si está mirando por la mira del Francotirador (apuntando con él, en primera persona): entonces no se ve el arma. */
	public static boolean miraTelescopica(float parcial) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.options.getCameraType().isFirstPerson() && apuntando(parcial) > 0.8f
				&& EscopetaPhoraItem.armaDe(mc.player.getMainHandItem()) == EscopetaPhoraItem.FRANCOTIRADOR;
	}

	/**
	 * La mira telescópica del Francotirador: todo negro menos un círculo en el medio, la cruz (líneas negras finas) y,
	 * abajo, la escalera de la caída de la bala (una línea ancha y después rayitas cada vez más cortas, unidas por
	 * escalones a los costados).
	 */
	private static void telescopica(GuiGraphics g) {
		int w = g.guiWidth(), h = g.guiHeight(), cx = w / 2, cy = h / 2;
		float r = h * 0.45f;
		int negro = 0xFF000000;
		// Afuera del círculo, negro (fila por fila: queda pixelado, como una mira de verdad en Minecraft).
		for (int y = 0; y < h; y++) {
			float dy = y + 0.5f - cy;
			if (Math.abs(dy) >= r) {
				g.fill(0, y, w, y + 1, negro);
				continue;
			}
			int dx = Math.round((float) Math.sqrt(r * r - dy * dy));
			g.fill(0, y, cx - dx, y + 1, negro);
			g.fill(cx + dx, y, w, y + 1, negro);
		}
		int ri = Math.round(r);
		// La cruz.
		g.fill(cx, cy - ri, cx + 1, cy + ri, negro);
		g.fill(cx - ri, cy, cx + ri, cy + 1, negro);
		// La escalera de la caída: la primera línea ancha y las demás cada vez más cortas, con escalones a los costados.
		int yAncha = cy + Math.round(r * 0.32f), mitadAncha = Math.round(r * 0.94f);
		g.fill(cx - mitadAncha, yAncha, cx + mitadAncha, yAncha + 1, negro);
		int yAntes = -1, mitadAntes = 0;
		for (int i = 0; i < 6; i++) {
			int y = cy + Math.round(r * (0.42f + 0.095f * i)), mitad = Math.round(r * (0.5f - 0.045f * i));
			g.fill(cx - mitad, y, cx + mitad, y + 1, negro);
			if (yAntes >= 0) {
				// El escalón: de la punta de la línea de arriba baja hasta esta y entra hasta su punta.
				g.fill(cx - mitadAntes, yAntes, cx - mitadAntes + 1, y + 1, negro);
				g.fill(cx + mitadAntes - 1, yAntes, cx + mitadAntes, y + 1, negro);
				g.fill(cx - mitadAntes, y, cx - mitad + 1, y + 1, negro);
				g.fill(cx + mitad - 1, y, cx + mitadAntes, y + 1, negro);
			}
			yAntes = y;
			mitadAntes = mitad;
		}
	}

	private static void rectangulo(GuiGraphics g, int x, int y, int ancho, int alto, int color) {
		g.fill(x, y, x + ancho, y + alto, color);
	}

	// ---------------------------------------------------------------- Al lado de la barra de ítems

	private static void hud(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		if (p == null || mc.options.hideGui || p.isSpectator()) return;
		ItemStack escopeta = p.getMainHandItem().getItem() instanceof EscopetaPhoraItem ? p.getMainHandItem()
				: p.getOffhandItem().getItem() instanceof EscopetaPhoraItem ? p.getOffhandItem() : ItemStack.EMPTY;
		if (escopeta.isEmpty()) return;
		int x = g.guiWidth() / 2 + 91 + 8;
		// Si la mano secundaria tiene algo y se dibuja a la derecha, se corre para no taparla.
		if (!p.getOffhandItem().isEmpty() && p.getMainArm() == HumanoidArm.LEFT) x += 29;
		int y = g.guiHeight() - 19;
		String balas = EscopetaPhoraItem.balas(escopeta) + "/" + EscopetaPhoraItem.armaDe(escopeta).cargador();
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
		// El fusil casi no salta y no bombea (dispara rápido).
		boolean fusil = EscopetaPhoraItem.armaDe(mano == InteractionHand.MAIN_HAND ? jugador.getMainHandItem() : jugador.getOffhandItem())
				== EscopetaPhoraItem.FUSIL;
		if (d >= 0 && d < 0.12f) {
			float r = (1 - d / 0.12f) * (fusil ? 0.25f : 1f);
			rx += 0.30f * r;
			pz += 0.12f * r;
			py += 0.02f * r;
		}
		if (!fusil) b = Math.max(b, bombeo(d, 0.15f) * 1.4f);

		// Recarga (2,5 s): la inclina un poco, se saca el tambor vacío (cae para abajo), entra uno nuevo desde abajo,
		// hace clic, la vuelve a poner derecha y bombea (el tambor lo mueve tamborBaja, en ItemRendererBrilloMixin).
		ItemStack pila = mano == InteractionHand.MAIN_HAND ? jugador.getMainHandItem() : jugador.getOffhandItem();
		float p = progresoRecarga(pila, parcial);
		if (p >= 0) {
			float giro = p < 0.12f ? easeInOutQuad(p / 0.12f) : p > 0.86f ? easeInOutQuad(Mth.clamp((0.97f - p) / 0.11f, 0, 1)) : 1f;
			rz += s * 0.22f * giro;
			rx += 0.10f * giro;
			py += 0.03f * giro;
			if (p > 0.80f && p < 0.86f) py += 0.02f * (1 - (p - 0.80f) / 0.06f);   // el clic del tambor nuevo
			b = Math.max(b, bombeo(p * 2.5f, 2.1f) * 1.4f);
		}

		// Apuntando (clic derecho): al medio, más cerca y derecha, para mirar por arriba del arma.
		float a = mano == InteractionHand.MAIN_HAND ? apuntando(parcial) : 0f;
		if (a > 0) {
			px += -s * APUNTAR[0] * a;
			py += APUNTAR[1] * a;
			pz += APUNTAR[2] * a;
			ry += s * APUNTAR[3] * a;
			rx += APUNTAR[4] * a;
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

	/** Cuánto se mueve al apuntar: {hacia el centro, arriba, atrás, giro en Y, giro en X}. */
	public static float[] APUNTAR = {0.66f, 0.205f, 0.10f, -0.15f, 0f};

	/** Qué parte de la recarga va (0 a 1), o -1 si no está recargando. */
	private static float progresoRecarga(ItemStack pila, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		long fin = EscopetaPhoraItem.finRecargaDe(pila);
		if (fin <= 0 || mc.level == null) return -1;
		return Mth.clamp(1f - (fin - (mc.level.getGameTime() + parcial)) / (float) EscopetaPhoraItem.armaDe(pila).recarga(), 0f, 1f);
	}

	/** Cuánto baja el tambor (en bloques) en la recarga: se cae el viejo, un rato sin tambor, y sube el nuevo. */
	public static float tamborBaja(ItemStack pila) {
		float p = progresoRecarga(pila, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
		if (p < 0.14f) return 0;
		if (p < 0.38f) return 1.1f * easeInOutQuad((p - 0.14f) / 0.24f);   // se saca el viejo
		if (p < 0.55f) return 1.1f;                                          // sin tambor
		if (p < 0.80f) return 1.1f * (1 - easeOutBack((p - 0.55f) / 0.25f) * 0.98f) * 1.0f;   // entra el nuevo
		return 0;
	}

	/** Dónde van los dos brazos (corridos desde donde Minecraft pone el brazo vacío): {hacia el centro, arriba, adelante(-)/atrás(+), giro}. */
	public static float[] BRAZO_DERECHO = {-0.02f, 0.02f, 0.02f, 0f}, BRAZO_IZQUIERDO = {1.3f, 0.08f, -0.1f, 0f};

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
