package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.Linternas;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * La luz de la Linterna, del lado del cliente: la luz sale de la linterna (no se prenden bloques).
 * - El haz: un cono de luz que sale de la lente, en la linterna de cualquier jugador.
 * - De noche o en cuevas: un brillo suave donde apuntás (no oscurece nada alrededor).
 * - En el Eclipse (donde nada más alumbra): lo que apuntás se ve iluminado dentro de un círculo suave y
 *   afuera del haz queda oscuro.
 */
public final class LinternaLuz {
	private LinternaLuz() {}

	private static final ResourceLocation HAZ_PANTALLA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/linterna_haz.png");
	/** Largo del haz que se ve, en bloques. */
	private static final float LARGO = 10f;
	private static final int LADOS = 20;

	/** ¿El jugador de esta pantalla tiene la Linterna prendida en alguna mano? */
	public static boolean prendidaLocal() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && !Linternas.linternaPrendida(mc.player).isEmpty();
	}

	/** Qué tan oscuro está donde está el jugador (0 = de día a pleno, 1 = negro o Eclipse). */
	public static float oscuridad() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return 0;
		if (EclipseCliente.activoAqui()) return 1;
		return oscuridadEn(mc, mc.player);
	}

	/**
	 * Con la Linterna prendida en la oscuridad (en primera persona), el mundo se dibuja iluminado y después
	 * se oscurece afuera del haz (también en tercera persona desde atrás; mirando de frente, solo el haz).
	 */
	public static boolean alumbrando() {
		return EclipseCliente.activoAqui() && prendidaLocal() && !Minecraft.getInstance().options.getCameraType().isMirrored();
	}

	/** Fuera del Eclipse: con la Linterna prendida en la oscuridad, un brillo suave donde apuntás (no oscurece nada). */
	private static boolean brillando() {
		return !EclipseCliente.activoAqui() && prendidaLocal() && oscuridad() > 0.02f
				&& !Minecraft.getInstance().options.getCameraType().isMirrored();
	}

	/** La tabla de luces: lo que está dentro del haz se ve iluminado (como con una linterna de verdad). */
	public static void iluminar(NativeImage luces) {
		int minimo = (int) (255 * 0.82f);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int abgr = luces.getPixelRGBA(x, y);
				int r = Math.max(abgr & 0xFF, minimo), g = Math.max((abgr >> 8) & 0xFF, minimo - 6), b = Math.max((abgr >> 16) & 0xFF, minimo - 20);
				luces.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
			}
		}
	}

	/** Afuera del haz, oscuro: un círculo suave en el medio de la pantalla (hacia donde apunta la linterna). */
	public static void dibujarPantalla(GuiGraphics g) {
		if (brillando()) {
			dibujarBrillo(g);
			return;
		}
		if (!alumbrando()) return;
		float alfa = oscuridad() * 0.97f;
		int ancho = g.guiWidth(), alto = g.guiHeight();
		int lado = (int) (alto * 1.5f), x = (ancho - lado) / 2, y = (alto - lado) / 2;
		int negro = ((int) (alfa * 255) << 24);
		RenderSystem.enableBlend();
		g.setColor(1, 1, 1, alfa);
		g.blit(HAZ_PANTALLA, x, y, lado, lado, 0, 0, 256, 256, 256, 256);
		g.setColor(1, 1, 1, 1);
		// Lo que queda afuera del cuadrado de la textura, también oscuro.
		if (x > 0) {
			g.fill(0, 0, x, alto, negro);
			g.fill(x + lado, 0, ancho, alto, negro);
		}
		if (y > 0) {
			g.fill(x, 0, x + lado, y, negro);
			g.fill(x, y + lado, x + lado, alto, negro);
		}
		RenderSystem.disableBlend();
	}

	private static final ResourceLocation BRILLO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/linterna_brillo.png");

	/** Un círculo de luz cálida que se suma en el medio de la pantalla (lo de afuera queda como está). */
	private static void dibujarBrillo(GuiGraphics g) {
		int alto = g.guiHeight(), lado = (int) (alto * 1.2f), x = (g.guiWidth() - lado) / 2, y = (alto - lado) / 2;
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
		g.setColor(1, 1, 1, 0.55f * oscuridad());
		g.blit(BRILLO, x, y, lado, lado, 0, 0, 256, 256, 256, 256);
		g.setColor(1, 1, 1, 1);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** Los haces de luz: un cono que sale de la linterna de cada jugador que la tiene prendida. */
	public static void dibujarHaces(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		float parcial = contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		Vec3 camara = contexto.camera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(Haz.TIPO);
		boolean dibujo = false;
		for (AbstractClientPlayer jugador : mc.level.players()) {
			if (Linternas.linternaPrendida(jugador).isEmpty() || jugador.isSpectator()) continue;
			if (jugador.distanceToSqr(camara) > 96 * 96) continue;
			Vec3 mira = jugador.getViewVector(parcial);
			Vec3 origen = origen(mc, jugador, parcial, mira);
			if (camaraAdentro(camara.subtract(origen), mira)) continue;   // si la cámara está dentro del haz, taparía todo
			PoseStack pose = new PoseStack();
			pose.translate(origen.x - camara.x, origen.y - camara.y, origen.z - camara.z);
			// De día el haz casi no se ve; en la oscuridad, sí.
			float fuerza = 0.25f + 0.75f * (EclipseCliente.activoAqui() ? 1f : oscuridadEn(mc, jugador));
			cono(vc, pose.last().pose(), mira, 0.05f, (float) Math.tan(Math.toRadians(Linternas.APERTURA)) * LARGO, 0.30f * fuerza);
			cono(vc, pose.last().pose(), mira, 0.03f, (float) Math.tan(Math.toRadians(Linternas.APERTURA * 0.5)) * LARGO, 0.24f * fuerza);
			dibujo = true;
		}
		if (dibujo) buffers.endBatch(Haz.TIPO);
	}

	/**
	 * Luz 11 o más (de día, o un cuarto con antorchas): nada; luz 4 o menos (de noche, cuevas): oscuro del
	 * todo. La luz del cielo se cuenta según la hora (de noche casi no alumbra).
	 */
	private static float oscuridadEn(Minecraft mc, net.minecraft.world.entity.player.Player jugador) {
		BlockPos pos = BlockPos.containing(jugador.getEyePosition());
		float dia = Mth.clamp((mc.level.getSkyDarken(1f) - 0.2f) / 0.8f, 0, 1);
		float cielo = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) * (0.25f + 0.75f * dia);
		float luz = Math.max(cielo, mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos));
		return Mth.clamp((11 - luz) / 7f, 0, 1);
	}

	/** ¿La cámara (relativa a la lente) está dentro del cono del haz? */
	private static boolean camaraAdentro(Vec3 relativa, Vec3 mira) {
		Vec3 eje = mira.normalize();
		double largo = relativa.dot(eje);
		if (largo <= 0 || largo >= LARGO) return false;
		double radio = Math.tan(Math.toRadians(Linternas.APERTURA)) * largo + 0.4;
		return relativa.subtract(eje.scale(largo)).length() < radio;
	}

	/** De dónde sale la luz: la mano con la linterna (en primera persona, abajo a la derecha de la vista). */
	private static Vec3 origen(Minecraft mc, AbstractClientPlayer jugador, float parcial, Vec3 mira) {
		boolean derecha = jugador.getMainHandItem().is(com.dedsafio4.items.ModItems.LINTERNA)
				? jugador.getMainArm() == HumanoidArm.RIGHT : jugador.getMainArm() != HumanoidArm.RIGHT;
		Vec3 costado = mira.cross(new Vec3(0, 1, 0));
		if (costado.lengthSqr() < 1e-4) costado = new Vec3(1, 0, 0);
		costado = costado.normalize().scale(derecha ? 1 : -1);
		Vec3 arriba = costado.cross(mira).normalize().scale(derecha ? 1 : -1);
		Vec3 ojos = jugador.getEyePosition(parcial);
		boolean primeraPersona = jugador == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON;
		if (primeraPersona) return ojos.add(costado.scale(0.32)).add(arriba.scale(-0.26)).add(mira.scale(0.55));
		return ojos.add(costado.scale(0.38)).add(arriba.scale(-0.45)).add(mira.scale(0.6));
	}

	/**
	 * Un cono abierto hacia "mira" (luz aditiva): arranca transparente en la lente, es más fuerte a un
	 * bloque y medio, y se apaga hacia la punta.
	 */
	private static void cono(VertexConsumer vc, Matrix4f m, Vec3 mira, float radioInicio, float radioFin, float alfa) {
		Vec3 eje = mira.normalize();
		Vec3 u = eje.cross(Math.abs(eje.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize();
		Vec3 v = eje.cross(u).normalize();
		float[] largos = {0, 1.5f, LARGO};
		int[] alfas = {0, (int) (alfa * 255), 0};
		for (int tramo = 0; tramo < 2; tramo++) {
			float l0 = largos[tramo], l1 = largos[tramo + 1];
			float r0 = Mth.lerp(l0 / LARGO, radioInicio, radioFin), r1 = Mth.lerp(l1 / LARGO, radioInicio, radioFin);
			for (int i = 0; i < LADOS; i++) {
				double a0 = Math.PI * 2 * i / LADOS, a1 = Math.PI * 2 * (i + 1) / LADOS;
				Vec3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0))), d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
				vertice(vc, m, eje.scale(l0).add(d0.scale(r0)), alfas[tramo]);
				vertice(vc, m, eje.scale(l0).add(d1.scale(r0)), alfas[tramo]);
				vertice(vc, m, eje.scale(l1).add(d1.scale(r1)), alfas[tramo + 1]);
				vertice(vc, m, eje.scale(l1).add(d0.scale(r1)), alfas[tramo + 1]);
			}
		}
	}

	private static void vertice(VertexConsumer vc, Matrix4f m, Vec3 p, int alfa) {
		vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(255, 244, 214, alfa);
	}

	/** El tipo de dibujo del haz: luz que se suma (no tapa lo de atrás), de los dos lados. */
	private static final class Haz extends RenderType {
		static final RenderType TIPO = create("dedsafio4_haz_linterna", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
				4096, false, true, CompositeState.builder()
						.setShaderState(RENDERTYPE_LIGHTNING_SHADER)
						.setTransparencyState(LIGHTNING_TRANSPARENCY)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		private Haz(String nombre, VertexFormat formato, VertexFormat.Mode modo, int tamanio, boolean afectaCrumbling,
					boolean ordenar, Runnable antes, Runnable despues) {
			super(nombre, formato, modo, tamanio, afectaCrumbling, ordenar, antes, despues);
		}
	}
}
