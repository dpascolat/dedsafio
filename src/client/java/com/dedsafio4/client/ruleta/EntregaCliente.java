package com.dedsafio4.client.ruleta;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.client.mixin.LightTextureAccessor;
import com.dedsafio4.ruleta.EntregaAnimacion;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La animación de entregar la Dedita de la Misión (animación de NavithLabs / AlieRequiem, usada con su permiso). En
 * segundos desde que se toca el bloque:
 *   0–4,6    crece el piso de 8 piezas de agua de colores (entrega_piso.fsh)
 *   2,9–7,9  quien entregó entra al Vacío Cósmico: el mundo se vuelve espacio con estrellas (entrega_espacio.fsh)
 *   3,3–6,4  de las piezas salen chispas que suben hasta la luz
 *   4,1–9,6  la luz sobre el bloque (entrega_luz.fsh), con destellos; a los 6,05 s se agranda de golpe
 *   4,6–6,4  las piezas se encogen hacia el aro chico, una detrás de otra
 *   7,95     el golpe: el aro grande sale del chico y salta una lluvia de chispas
 *   9        la luz se apaga con un estallido de chispas grandes
 */
public final class EntregaCliente {
	private EntregaCliente() {}

	private static final float SUBE_LUZ = 2.35f, LUZ_SALE = 4.1f, LUZ_CRECE = 6.05f, LUZ_FIN = 9f, LUZ_CHICA = 0.26f, LUZ_GRANDE = 0.66f;
	private static final float DESTELLOS_FIN = 7f, CHISPAS_SALEN = 3.3f, PIEZA_SALE = 4.6f, ESCALON = 0.15f, T_PIEZA = 0.8f;
	private static final float GOLPE = 7.95f, FIN = 10.6f, ALCANCE = 48f, PISO_RADIO = 9.6f, PISO_FIN = 8.9f;
	/** El Vacío Cósmico: cuándo entra quien entregó y cuánto dura (lo ve solo esa persona, si está cerca). */
	private static final float VACIO_DESDE = 2.9f, VACIO_DURA = 5f, ENTRA = 0.6f, SALE = 1.2f;
	private static final float[][] COLORES_PIEZA = {{190, 35, 55}, {215, 110, 30}, {205, 190, 45}, {55, 185, 60}, {35, 185, 185}, {40, 70, 205}, {115, 45, 195}, {200, 50, 175}};
	private static final float[][] COLORES_FINAL = {{1, 0.32f, 0.72f}, {1, 0.86f, 0.25f}, {0.35f, 1, 0.45f}, {0.3f, 0.85f, 1}, {0.7f, 0.4f, 1}, {1, 0.55f, 0.22f}};
	/** r, g, b y hasta qué número al azar sale ese color. */
	private static final float[][] COLORES_GOLPE = {{0.45f, 1, 0.72f, 0.45f}, {0.4f, 0.85f, 1, 0.65f}, {1, 0.45f, 0.85f, 0.8f}, {1, 0.62f, 0.3f, 0.9f}, {1, 0.92f, 0.4f, 1}};
	private static final float[][] COLORES_ESTRELLA = {{1, 0.38f, 0.78f}, {0.72f, 0.38f, 1}, {0.38f, 0.55f, 1}, {0.85f, 0.88f, 1}, {1, 0.45f, 0.95f}};

	private static final SoundEvent SONIDO = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "entrega_mision"));

	private static ShaderInstance shaderEspacio, shaderPiso, shaderLuz;
	private static TextureTarget profundidad;
	private static final List<Animacion> ANIMACIONES = new ArrayList<>();
	private static final List<Estrella> ESTRELLAS = new ArrayList<>();
	private static final RandomSource AZAR = RandomSource.create();
	/** Cuánto se ve el Vacío Cósmico y sus estrellas en este cuadro (0..1), y adónde miraba quien entró. */
	private static float vacio, estrellas, adelanteX, adelanteZ = 1;
	private static long ultimoCuadro = System.nanoTime();
	private static final int[] LUZ_GUARDADA = new int[256];
	private static boolean luzApagada;

	private static ResourceLocation id(String ruta) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, ruta);
	}

	public static void registrar() {
		CoreShaderRegistrationCallback.EVENT.register(ctx -> {
			ctx.register(id("entrega_espacio"), DefaultVertexFormat.POSITION, s -> shaderEspacio = s);
			ctx.register(id("entrega_piso"), DefaultVertexFormat.POSITION_TEX, s -> shaderPiso = s);
			ctx.register(id("entrega_luz"), DefaultVertexFormat.POSITION_TEX_COLOR, s -> shaderLuz = s);
		});
		ClientPlayNetworking.registerGlobalReceiver(EntregaAnimacion.Payload.TYPE, (payload, context) -> context.client().execute(() -> empezar(context.client(), payload)));
		ClientTickEvents.END_CLIENT_TICK.register(EntregaCliente::tick);
	}

	private static void empezar(Minecraft mc, EntregaAnimacion.Payload p) {
		if (mc.level == null || mc.player == null) return;
		ANIMACIONES.add(new Animacion(p, mc.level));
		if (mc.player.position().distanceTo(new Vec3(p.x(), p.y() + 1, p.z())) <= ALCANCE) {
			mc.getSoundManager().play(new SimpleSoundInstance(SONIDO, SoundSource.BLOCKS, 1f, 1f, SoundInstance.createUnseededRandom(), p.x(), p.y() + 1, p.z()));
		}
	}

	private static void tick(Minecraft mc) {
		ANIMACIONES.removeIf(a -> a.nivel != mc.level || segundos(a, 0) > FIN);
	}

	private static float segundos(Animacion a, float parcial) {
		return (a.nivel.getGameTime() - a.inicio + parcial) / 20f;
	}

	private static float suave(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}

	// --- El Vacío Cósmico ---

	/** Lo tapa todo: sin agua, lluvia, nubes ni cofres. */
	public static boolean vacioCompleto() {
		return vacio > 0.97f;
	}

	public static float vacio() {
		return vacio;
	}

	private static void calcularVacio(Minecraft mc, float parcial) {
		vacio = estrellas = 0;
		if (mc.player == null) return;
		UUID yo = mc.player.getUUID();
		for (Animacion a : ANIMACIONES) {
			if (!a.quien.equals(yo)) continue;
			float t = segundos(a, parcial) - VACIO_DESDE;
			if (t < 0) continue;
			if (!a.vacioMirado) {
				a.vacioMirado = true;
				a.conVacio = mc.player.position().distanceToSqr(a.x, a.y + 1, a.z) <= ALCANCE * ALCANCE;
				Vec3 vista = mc.player.getViewVector(1f);
				double largo = Math.hypot(vista.x, vista.z);
				if (a.conVacio && largo >= 0.001) {
					adelanteX = (float) (vista.x / largo);
					adelanteZ = (float) (vista.z / largo);
				}
			}
			float queda = VACIO_DURA - t;
			if (!a.conVacio || queda <= 0) continue;
			vacio = Math.min(suave(t / ENTRA), suave(queda / SALE));
			estrellas = Math.min(suave((t - ENTRA) / ENTRA), suave((queda - SALE) / ENTRA));
		}
	}

	/** La luz de los bloques se apaga junto con el vacío (se pone de vuelta en encenderTerreno). */
	public static void apagarTerreno() {
		if (vacio <= 0.001f) return;
		LightTextureAccessor luz = (LightTextureAccessor) Minecraft.getInstance().gameRenderer.lightTexture();
		NativeImage imagen = luz.dedsafio4$imagen();
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int color = imagen.getPixelRGBA(x, y), apagado = 0xFF000000;
				LUZ_GUARDADA[y * 16 + x] = color;
				for (int canal = 0; canal < 24; canal += 8) apagado |= Math.round((color >> canal & 255) * (1 - vacio)) << canal;
				imagen.setPixelRGBA(x, y, apagado);
			}
		}
		luz.dedsafio4$textura().upload();
		luzApagada = true;
	}

	public static void encenderTerreno() {
		if (!luzApagada) return;
		luzApagada = false;
		LightTextureAccessor luz = (LightTextureAccessor) Minecraft.getInstance().gameRenderer.lightTexture();
		NativeImage imagen = luz.dedsafio4$imagen();
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) imagen.setPixelRGBA(x, y, LUZ_GUARDADA[y * 16 + x]);
		}
		luz.dedsafio4$textura().upload();
	}

	/** La profundidad de lo que ya se dibujó, para el shader del vacío. */
	private static int copiarProfundidad() {
		RenderTarget principal = Minecraft.getInstance().getMainRenderTarget();
		if (profundidad == null) profundidad = new TextureTarget(principal.width, principal.height, true, Minecraft.ON_OSX);
		else if (profundidad.width != principal.width || profundidad.height != principal.height) profundidad.resize(principal.width, principal.height, Minecraft.ON_OSX);
		profundidad.copyDepthFrom(principal);
		principal.bindWrite(false);
		return profundidad.getDepthTextureId();
	}

	/** Antes de las criaturas: el terreno y el cielo se vuelven espacio. */
	public static void dibujarVacio(Matrix4f vista, Matrix4f proyeccion, float parcial) {
		Minecraft mc = Minecraft.getInstance();
		calcularVacio(mc, parcial);
		if (vacio <= 0.001f || shaderEspacio == null) return;
		int textura = copiarProfundidad();
		ShaderInstance shader = shaderEspacio;
		RenderSystem.setShader(() -> shader);
		uniforme(shader, "InvVistaProy", new Matrix4f(proyeccion).mul(vista).invert());
		uniforme(shader, "Forma", adelanteX, adelanteZ, 0, (System.nanoTime() / 1_000_000L % 600_000L) / 1000f);
		uniforme(shader, "Fundido", vacio, estrellas, 0, 0);
		// El bloque de la entrega es invisible: no hay caja que dejar sin tapar.
		uniforme(shader, "PilarMin", 1e6f, 1e6f, 1e6f, 0);
		uniforme(shader, "PilarMax", -1e6f, -1e6f, -1e6f, 0);
		int antes = RenderSystem.getShaderTexture(2);
		RenderSystem.setShaderTexture(2, textura);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		b.addVertex(-1, -1, 0);
		b.addVertex(1, -1, 0);
		b.addVertex(1, 1, 0);
		b.addVertex(-1, 1, 0);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.setShaderTexture(2, antes);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** Una estrellita del vacío que nace en cualquier lado alrededor. mezclada: al empezar, ya a medio camino. */
	private static void reiniciar(Estrella e, Vec3 ojo, boolean mezclada) {
		double distancia = 2.5 + 27.5 * Math.pow(AZAR.nextFloat(), 1.4), angulo = AZAR.nextFloat() * Math.PI * 2;
		double alto = (AZAR.nextFloat() * 2 - 0.6f) * 0.9, plano = Math.sqrt(Math.max(0, 1 - alto * alto));
		e.x = ojo.x + Math.cos(angulo) * plano * distancia;
		e.y = ojo.y + alto * distancia * 0.6 + 1;
		e.z = ojo.z + Math.sin(angulo) * plano * distancia;
		e.vx = (AZAR.nextFloat() - 0.5f) * 0.25f;
		e.vy = (AZAR.nextFloat() - 0.3f) * 0.18f;
		e.vz = (AZAR.nextFloat() - 0.5f) * 0.25f;
		e.dura = 2 + 2 * AZAR.nextFloat();
		e.vida = mezclada ? AZAR.nextFloat() * e.dura : 0;
		e.tamanio = (0.09f + 0.11f * AZAR.nextFloat()) * (float) (0.6 + distancia * 0.05);
		e.color = COLORES_ESTRELLA[AZAR.nextInt(COLORES_ESTRELLA.length)];
		e.fase = AZAR.nextFloat() * 6.28f;
	}

	private static void dibujarEstrellas(Vec3 ojo, Matrix4f vistaProy, Vector3f derecha, Vector3f arriba, float paso, float reloj) {
		if (estrellas <= 0.01f || shaderLuz == null) {
			ESTRELLAS.clear();
			return;
		}
		if (ESTRELLAS.isEmpty()) {
			for (int i = 0; i < 220; i++) {
				Estrella e = new Estrella();
				reiniciar(e, ojo, true);
				ESTRELLAS.add(e);
			}
		}
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (Estrella e : ESTRELLAS) {
			e.vida += paso;
			e.x += e.vx * paso;
			e.y += e.vy * paso;
			e.z += e.vz * paso;
			float avance = e.vida / e.dura;
			if (e.vida >= e.dura || ojo.distanceToSqr(e.x, e.y, e.z) > 1156) {
				reiniciar(e, ojo, false);
				avance = 0;
			}
			float brillo = (float) Math.sin(Math.PI * Math.min(1, avance)) * (0.75f + 0.25f * Mth.sin(reloj * 5 + e.fase)) * estrellas;
			if (brillo <= 0.01f) continue;
			cuadrado(b, (float) (e.x - ojo.x), (float) (e.y - ojo.y), (float) (e.z - ojo.z), derecha, arriba, e.tamanio * 2.2f,
					Math.round(255 * e.color[0] * brillo), Math.round(255 * e.color[1] * brillo), Math.round(255 * e.color[2] * brillo), 210);
		}
		ShaderInstance shader = shaderLuz;
		RenderSystem.setShader(() -> shader);
		uniforme(shader, "VistaProy", vistaProy);
		uniforme(shader, "Tiempo", 0, reloj, 0, 0);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.disableDepthTest();
		MeshData malla = b.build();
		if (malla != null) BufferUploader.drawWithShader(malla);
		RenderSystem.enableDepthTest();
	}

	// --- Las chispas ---

	/** Cuánto espera la pieza k para encogerse (verde primero, como las agujas del reloj). */
	private static float esperaPieza(int k) {
		return Math.floorMod(3 - k, 8) * ESCALON;
	}

	private static Vector3f direccionAlAzar(RandomSource azar, float alto, float abajo) {
		return new Vector3f(azar.nextFloat() * 2 - 1, azar.nextFloat() * alto - abajo, azar.nextFloat() * 2 - 1).normalize();
	}

	private static void actualizarChispas(Animacion a, float t, float paso) {
		RandomSource azar = a.azar;
		if (a.ultimoT < 0) a.ultimoT = t;
		float antes = a.ultimoT;
		a.ultimoT = t;
		// De cada pieza salen chispas que suben en curva hasta la luz.
		for (int k = 0; k < 8; k++) {
			float desde = Math.max(antes, CHISPAS_SALEN), hasta = Math.min(t, PIEZA_SALE + esperaPieza(k) + T_PIEZA);
			if (hasta <= desde) continue;
			a.pendientes[k] += (hasta - desde) * 18;
			while (a.pendientes[k] >= 1) {
				a.pendientes[k]--;
				float encoge = suave((t - PIEZA_SALE - esperaPieza(k)) / T_PIEZA);
				float adentro = 0.8f + 0.42f * encoge, afuera = 8 - 6.46f * encoge;
				float angulo = (float) (Math.PI * 2 * (k + 0.15f + 0.7f * azar.nextFloat()) / 8);
				float radio = adentro + (afuera - adentro) * (float) Math.sqrt(azar.nextFloat());
				Chispa c = new Chispa();
				c.x0 = a.x + Math.sin(angulo) * radio;
				c.z0 = a.z - Math.cos(angulo) * radio;
				c.y0 = a.y + 0.1;
				c.x = c.x0;
				c.y = c.y0;
				c.z = c.z0;
				c.curva = true;
				c.dura = 1 + 0.7f * azar.nextFloat();
				c.tamanio = 0.035f * (1 + azar.nextFloat());
				float[] color = COLORES_PIEZA[k];
				c.color(Math.min(1, color[0] / 255 * 0.65f + 0.35f), Math.min(1, color[1] / 255 * 0.65f + 0.35f), Math.min(1, color[2] / 255 * 0.65f + 0.35f));
				a.chispas.add(c);
			}
		}
		// Los destellos chicos alrededor de la luz.
		float desde = Math.max(antes, LUZ_SALE), hasta = Math.min(t, DESTELLOS_FIN);
		if (hasta > desde) {
			a.pendientes[8] += (hasta - desde) * 22;
			while (a.pendientes[8] >= 1) {
				a.pendientes[8]--;
				Chispa c = new Chispa();
				Vector3f d = direccionAlAzar(azar, 2, 1);
				float lejos = 0.35f + 0.8f * azar.nextFloat();
				c.x = a.x + d.x * lejos;
				c.y = a.luzY + d.y * lejos;
				c.z = a.z + d.z * lejos;
				c.vx = d.x * 0.25f;
				c.vy = d.y * 0.25f + 0.1f;
				c.vz = d.z * 0.25f;
				c.dura = 0.5f + 0.4f * azar.nextFloat();
				c.tamanio = 0.04f + 0.03f * azar.nextFloat();
				c.titila = true;
				float cual = azar.nextFloat();
				if (cual < 0.45f) c.color(1, 0.45f, 0.85f);
				else if (cual < 0.8f) c.color(0.75f, 0.45f, 1);
				else c.color(1, 1, 1);
				a.chispas.add(c);
			}
		}
		if (!a.golpeo && t >= GOLPE) {
			a.golpeo = true;
			for (int i = 0; i < 26; i++) {
				Chispa c = estallido(a, direccionAlAzar(azar, 2, 1), 16 + 14 * azar.nextFloat(), 4);
				c.dura = 0.55f + 0.45f * azar.nextFloat();
				c.tamanio = 0.05f + 0.04f * azar.nextFloat();
				c.titila = true;
				float cual = azar.nextFloat();
				for (float[] color : COLORES_GOLPE) {
					if (cual < color[3]) {
						c.color(color[0], color[1], color[2]);
						break;
					}
				}
				a.chispas.add(c);
			}
		}
		if (!a.termino && t >= LUZ_FIN) {
			a.termino = true;
			for (int i = 0; i < 16; i++) {
				Chispa c = estallido(a, direccionAlAzar(azar, 1.6f, 0.6f), 3 + 2.5f * azar.nextFloat(), 2.5f);
				c.dura = 0.6f + 0.35f * azar.nextFloat();
				c.tamanio = 0.13f + 0.08f * azar.nextFloat();
				float[] color = COLORES_FINAL[i % COLORES_FINAL.length];
				c.color(color[0], color[1], color[2]);
				a.chispas.add(c);
			}
		}
		for (var it = a.chispas.iterator(); it.hasNext(); ) {
			Chispa c = it.next();
			c.vida += paso;
			if (c.vida >= c.dura) {
				it.remove();
			} else if (c.curva) {
				float p = suave(c.vida / c.dura);
				double q = 1 - p, mx = c.x0 + (a.x - c.x0) * 0.25, my = c.y0 + 1.6, mz = c.z0 + (a.z - c.z0) * 0.25;
				c.x = q * q * c.x0 + 2 * q * p * mx + p * p * a.x;
				c.y = q * q * c.y0 + 2 * q * p * my + p * p * a.luzY;
				c.z = q * q * c.z0 + 2 * q * p * mz + p * p * a.z;
			} else {
				if (c.freno > 0) {
					float f = (float) Math.exp(-c.freno * paso);
					c.vx *= f;
					c.vy *= f;
					c.vz *= f;
				}
				c.x += c.vx * paso;
				c.y += c.vy * paso;
				c.z += c.vz * paso;
			}
		}
	}

	private static Chispa estallido(Animacion a, Vector3f d, float velocidad, float freno) {
		Chispa c = new Chispa();
		c.x = a.x;
		c.y = a.luzY;
		c.z = a.z;
		c.vx = d.x * velocidad;
		c.vy = d.y * velocidad;
		c.vz = d.z * velocidad;
		c.freno = freno;
		return c;
	}

	private static float tamanioLuz(float t) {
		if (t < LUZ_SALE) return 0;
		float r = LUZ_CHICA * suave((t - LUZ_SALE) / 0.3f);
		if (t >= LUZ_CRECE) {
			float p = Mth.clamp((t - LUZ_CRECE) / 0.25f, 0, 1);
			r = (LUZ_CHICA + (LUZ_GRANDE - LUZ_CHICA) * suave(p)) * (p < 1 ? (float) (1 + 0.18 * Math.sin(p * Math.PI)) : 1);
		}
		return t >= LUZ_FIN ? r * (1 - suave((t - LUZ_FIN) / 0.6f)) : r;
	}

	// --- El dibujo ---

	/** Después del agua: las estrellas, el piso, la luz y las chispas. */
	public static void dibujar(Camera camara, Matrix4f vista, Matrix4f proyeccion, float parcial) {
		long ahora = System.nanoTime();
		float paso = Math.min(0.1f, (ahora - ultimoCuadro) / 1e9f);
		ultimoCuadro = ahora;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || (ANIMACIONES.isEmpty() && estrellas <= 0.01f)) {
			ESTRELLAS.clear();
			return;
		}
		boolean conNubes = mc.levelRenderer.getCloudsTarget() != null;
		if (conNubes) mc.getMainRenderTarget().bindWrite(false);
		Vec3 ojo = camara.getPosition();
		Matrix4f vistaProy = new Matrix4f(proyeccion).mul(vista), inversa = new Matrix4f(vista).invert();
		Vector3f derecha = inversa.transformDirection(new Vector3f(1, 0, 0)), arriba = inversa.transformDirection(new Vector3f(0, 1, 0));
		float reloj = (ahora / 1_000_000L % 600_000L) / 1000f;
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		dibujarEstrellas(ojo, vistaProy, derecha, arriba, paso, reloj);
		for (Animacion a : ANIMACIONES) {
			float t = segundos(a, parcial);
			actualizarChispas(a, t, paso);
			float x = (float) (a.x - ojo.x), y = (float) (a.y - ojo.y), z = (float) (a.z - ojo.z);
			if (shaderPiso != null && t < PISO_FIN) {
				ShaderInstance shader = shaderPiso;
				RenderSystem.setShader(() -> shader);
				uniforme(shader, "VistaProy", vistaProy);
				uniforme(shader, "Tiempo", t, reloj, vacio, 0);
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
				float r = PISO_RADIO, py = y + 0.075f;
				BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
				b.addVertex(x - r, py, z - r).setUv(-r, -r);
				b.addVertex(x - r, py, z + r).setUv(-r, r);
				b.addVertex(x + r, py, z + r).setUv(r, r);
				b.addVertex(x + r, py, z - r).setUv(r, -r);
				BufferUploader.drawWithShader(b.buildOrThrow());
			}
			if (shaderLuz == null) continue;
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			boolean hay = false;
			float luz = tamanioLuz(t), luzY = (float) (a.luzY - ojo.y);
			if (luz > 0.002f) {
				cuadrado(b, x, luzY, z, derecha, arriba, luz * 3.2f, 255, 0, 0, 128);
				cuadrado(b, x, luzY, z, derecha, arriba, luz * 1.9f, 255, 0, 0, 0);
				hay = true;
			}
			for (Chispa c : a.chispas) {
				float avance = c.vida / c.dura;
				float brillo = Math.min(1, c.vida / 0.08f) * (1 - suave((avance - 0.6f) / 0.4f));
				if (c.titila) brillo *= 0.55f + 0.45f * Mth.sin(c.vida * 26 + c.tamanio * 400);
				if (c.curva) brillo *= 1 - suave((avance - 0.75f) / 0.25f);
				if (brillo <= 0.01f) continue;
				cuadrado(b, (float) (c.x - ojo.x), (float) (c.y - ojo.y), (float) (c.z - ojo.z), derecha, arriba, c.tamanio * 2.2f,
						Math.round(255 * c.r * brillo), Math.round(255 * c.g * brillo), Math.round(255 * c.b * brillo), 255);
				hay = true;
			}
			MeshData malla = b.build();
			if (malla == null) continue;
			if (hay) {
				ShaderInstance shader = shaderLuz;
				RenderSystem.setShader(() -> shader);
				uniforme(shader, "VistaProy", vistaProy);
				uniforme(shader, "Tiempo", t, reloj, 0, 0);
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
				BufferUploader.drawWithShader(malla);
			} else {
				malla.close();
			}
		}
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(conNubes);
		if (conNubes) {
			RenderTarget clima = mc.levelRenderer.getWeatherTarget();
			(clima != null ? clima : mc.getMainRenderTarget()).bindWrite(false);
		}
	}

	/** Un cuadrado de frente a la cámara. */
	private static void cuadrado(BufferBuilder b, float x, float y, float z, Vector3f derecha, Vector3f arriba, float t, int r, int g, int bl, int a) {
		float dx = derecha.x * t, dy = derecha.y * t, dz = derecha.z * t, ax = arriba.x * t, ay = arriba.y * t, az = arriba.z * t;
		b.addVertex(x - dx - ax, y - dy - ay, z - dz - az).setUv(-1, -1).setColor(r, g, bl, a);
		b.addVertex(x + dx - ax, y + dy - ay, z + dz - az).setUv(1, -1).setColor(r, g, bl, a);
		b.addVertex(x + dx + ax, y + dy + ay, z + dz + az).setUv(1, 1).setColor(r, g, bl, a);
		b.addVertex(x - dx + ax, y - dy + ay, z - dz + az).setUv(-1, 1).setColor(r, g, bl, a);
	}

	private static void uniforme(ShaderInstance shader, String nombre, Matrix4f valor) {
		var u = shader.getUniform(nombre);
		if (u != null) u.set(valor);
	}

	private static void uniforme(ShaderInstance shader, String nombre, float x, float y, float z, float w) {
		var u = shader.getUniform(nombre);
		if (u != null) u.set(x, y, z, w);
	}

	private static final class Animacion {
		final double x, y, z, luzY;
		final UUID quien;
		final ClientLevel nivel;
		final long inicio;
		final RandomSource azar;
		final List<Chispa> chispas = new ArrayList<>();
		final float[] pendientes = new float[9];
		float ultimoT = -1;
		boolean vacioMirado, conVacio, golpeo, termino;

		Animacion(EntregaAnimacion.Payload p, ClientLevel nivel) {
			x = p.x();
			y = p.y();
			z = p.z();
			luzY = p.luzY();
			quien = p.quien();
			this.nivel = nivel;
			inicio = nivel.getGameTime();
			azar = RandomSource.create(Double.doubleToLongBits(x * 31 + z) ^ inicio);
		}
	}

	private static final class Chispa {
		double x, y, z, vx, vy, vz, x0, y0, z0;
		float vida, dura, tamanio, freno, r, g, b;
		boolean curva, titila;

		void color(float r, float g, float b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}

	private static final class Estrella {
		double x, y, z;
		float vx, vy, vz, vida, dura, tamanio, fase;
		float[] color;
	}
}
