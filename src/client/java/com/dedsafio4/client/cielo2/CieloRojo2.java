package com.dedsafio4.client.cielo2;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Cielo rojo 2 (/cielo rojo 2, del mod de los fans): un shader que tiñe el cielo de rojo desde arriba como un remolino,
 * después un rayo parte el cielo, se abre una grieta y por ella espía la cabeza 3D de Reviil (CabezaReviil).
 *
 * Con el comando (segundos desde que llega):
 *   0       la animación de Reviil en la pantalla (la manda el servidor), la pantalla se tiñe de rojo y tiembla
 *   11.1    el rojo empieza a cubrir el cielo (tarda 60 s en cubrirlo todo), con un retumbo y su música
 *   ~       cuando el rojo ya llegó lo bastante lejos: tres rayos y se abre la grieta con Reviil
 *   15      la música de Reviil (la del cielo se baja mientras suena)
 * Con /cielo normal: se cierra la grieta y después el rojo se va (15 s). /cielo cabezazo: Reviil golpea la grieta.
 * Al entrar al mundo con el cielo ya puesto, aparece todo de una (sin animación, temblor ni música de Reviil).
 */
public final class CieloRojo2 {
	private CieloRojo2() {}

	static ShaderInstance shader;

	private static ResourceLocation id(String ruta) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, ruta);
	}

	private static SoundEvent sonido(String nombre) {
		return SoundEvent.createVariableRangeEvent(id(nombre));
	}

	private static final ResourceLocation RUIDO = id("textures/cielo2/ruido.png");
	private static final ResourceLocation SONIDO_RETUMBO = id("cielo2.retumbo");
	private static final ResourceLocation SONIDO_DESPEJA = id("cielo2.despeja");
	private static final ResourceLocation SONIDO_MUSICA = id("cielo2.musica");
	private static final ResourceLocation SONIDO_REVIIL = id("cielo2.reviil_musica");
	private static final SoundEvent GRIETA_ABRE = sonido("cielo2.grieta_abre");
	private static final SoundEvent GRIETA_CIERRA = sonido("cielo2.grieta_cierra");
	private static final SoundEvent GRIETA_RAYO = sonido("cielo2.grieta_rayo");
	private static final SoundEvent CABEZAZO = sonido("cielo2.cabezazo");

	// Los tiempos (en segundos), como vienen en el mod de los fans.
	private static final float CIELO_ESPERA = 11.1f, CIELO_ABRIR = 60f, CIELO_CERRAR = 15f, ANIMACION = 3f;
	private static final float RAYO = 3.6f, ABRIR = 1.2f, CERRAR = 1.5f, LARGO = (float) Math.toRadians(100.0) / 2f;
	private static final float MUSICA_REVIIL = 15f;

	private static final float[] RETUMBO_SALE = {2.6f, 3.8f};
	private static final float[] RETUMBO_TONO = {0.85f, 2.2f};
	private static final float[] PULSO_VOLUMEN = {0.5f, 1f};
	private static final float[][] PULSOS = {{0f, 0.35f, 0.15f}, {0.44f, 1f, 0.07f}, {1f, 1f, 0.08f}};
	private static final float[] NIEBLA = {0.2f, 0.004f, 0.018f};
	private static final float AP_FINAL = 4.1f, AP_ARRANQUE = 0.083f;

	private static SonidoCielo2 sonidoRetumbo, sonidoDespeja, sonidoMusica, musicaReviil;
	private static float musicaLibre = 1f;
	private static boolean reviilSigue;
	private static boolean grietaSono;
	private static int pulsosSonados;

	private static long cieloDesde = -1L, cieloCierraDesde = -1L, efectoDesde;
	private static float cieloAlCerrar;
	private static long grietaDesde = -1L, grietaCierraDesde = -1L;
	private static float angulo, semilla;
	private static long cabezazoDesde = -1L;
	private static int cabezazos;
	private static float rayoAlCerrar, aberturaAlCerrar, ramasAlCerrar;
	private static boolean filtroPuesto;
	private static float cubre;
	private static long nubesVuelvenDesde = -1L;
	private static float formaEje, formaHuevo;
	private static int caraTextura = -1;

	/** Tinte rojo de la pantalla y temblor: desde que empieza hasta que el rojo cubre todo el cielo. */
	private static long efectosHasta = -1L;
	private static float tinte, temblorEntra, temblorGrados;
	private static long temblorUltimo;

	private record Programado(long en, Runnable accion) {}
	private static final List<Programado> PROGRAMADOS = new ArrayList<>();

	public static void registrar() {
		CoreShaderRegistrationCallback.EVENT.register(ctx -> {
			ctx.register(id("cielo_rojo2"), DefaultVertexFormat.POSITION, s -> shader = s);
			ctx.register(id("cabeza_reviil"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, s -> CabezaReviil.shader = s);
		});
		ClientTickEvents.END_CLIENT_TICK.register(cliente -> tick());
		HudRenderCallback.EVENT.register((graphics, tiempo) -> dibujarTinte(graphics));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, cliente) -> cliente.execute(CieloRojo2::limpiar));
	}

	// ---------------------------------------------------------------- lo que manda el servidor

	/** /cielo rojo 2. Con {@code animar} es la secuencia completa; sin animar (al entrar al mundo), todo aparece ya puesto. */
	public static void poner(boolean animar) {
		PROGRAMADOS.clear();
		long ahora = System.nanoTime();
		if (!animar) {
			if (activo() && cieloCierraDesde < 0L) return;
			limpiar();
			abrirCielo(ahora - segundos(CIELO_ABRIR));
			abrirGrieta(ahora - segundos(RAYO + ABRIR + 1f));
			grietaSono = true;
			pulsosSonados = PULSO_VOLUMEN.length;
			return;
		}
		efectosHasta = ahora + segundos(CIELO_ESPERA + CIELO_ABRIR);
		long tCielo = ahora + segundos(CIELO_ESPERA);
		long tGrieta = Math.max(tCielo, tCielo + segundos(cieloCubreGrieta() - RAYO));
		programar(tCielo, () -> abrirCielo(System.nanoTime()));
		programar(tGrieta, () -> abrirGrieta(System.nanoTime()));
		programar(ahora + segundos(MUSICA_REVIIL), CieloRojo2::musicaReviil);
	}

	/** /cielo normal (o /cielo rojo): se cierra la grieta y después se va el rojo. Sin animar, se va de una. */
	public static void quitar(boolean animar) {
		PROGRAMADOS.clear();
		efectosHasta = -1L;
		reviilSigue = false;   // la música de Reviil se apaga de a poco
		if (!animar) {
			limpiar();
			return;
		}
		boolean conGrieta = grietaDesde >= 0L && grietaCierraDesde < 0L;
		cerrarGrieta();
		if (conGrieta) programar(System.nanoTime() + segundos(2f * CERRAR), CieloRojo2::cerrarCielo);
		else cerrarCielo();
	}

	/** /cielo cabezazo. */
	public static void cabezazo() {
		if (grietaDesde < 0L) return;
		cabezazoDesde = System.nanoTime() + 349_999_994L;
		cabezazos++;
		tocar(CABEZAZO, 1f, 1f);
	}

	public static void limpiar() {
		PROGRAMADOS.clear();
		efectosHasta = -1L;
		cabezazoDesde = grietaCierraDesde = grietaDesde = cieloCierraDesde = cieloDesde = -1L;
		nubesVuelvenDesde = -1L;
		cubre = 0f;
		var sonidos = Minecraft.getInstance().getSoundManager();
		for (SonidoCielo2 s : new SonidoCielo2[]{sonidoRetumbo, sonidoDespeja, sonidoMusica, musicaReviil}) {
			if (s != null) sonidos.stop(s);
		}
		sonidoRetumbo = sonidoDespeja = sonidoMusica = musicaReviil = null;
		CabezaReviil.limpiar();
		caraTextura = -1;
	}

	public static boolean activo() {
		return cieloDesde >= 0L || grietaDesde >= 0L;
	}

	// ---------------------------------------------------------------- piezas

	private static long segundos(float s) {
		return (long) (s * 1.0E9);
	}

	private static void programar(long en, Runnable accion) {
		PROGRAMADOS.add(new Programado(en, accion));
	}

	private static void abrirCielo(long desde) {
		long ahora = System.nanoTime();
		float ya = cieloDesde >= 0L ? radioCielo(ahora) : 0f;
		if (cieloDesde < 0L) {
			formaEje = (float) (Math.random() * Math.PI);
			formaHuevo = (float) (Math.random() * 2.0 * Math.PI);
			efectoDesde = desde;
			cieloDesde = desde;
		} else {
			cieloDesde = ahora - (long) (inversaApertura(ya) * CIELO_ABRIR * 1.0E9);
		}
		cieloCierraDesde = -1L;
		sonidoRetumbo = sonar(sonidoRetumbo, SONIDO_RETUMBO, CieloRojo2::estadoRetumbo, true);
		sonidoMusica = sonar(sonidoMusica, SONIDO_MUSICA, CieloRojo2::estadoMusica, true);
	}

	private static void cerrarCielo() {
		if (cieloDesde < 0L || cieloCierraDesde >= 0L) return;
		long ahora = System.nanoTime();
		cieloAlCerrar = radioCielo(ahora);
		cieloCierraDesde = ahora;
		sonidoDespeja = sonar(sonidoDespeja, SONIDO_DESPEJA, CieloRojo2::estadoDespeja, true);
	}

	private static void abrirGrieta(long desde) {
		angulo = (float) (Math.random() * Math.PI);
		semilla = (float) (Math.random() * 100.0);
		grietaDesde = desde;
		grietaCierraDesde = -1L;
		grietaSono = false;
		pulsosSonados = 0;
	}

	private static void cerrarGrieta() {
		if (grietaDesde < 0L || grietaCierraDesde >= 0L) return;
		long ahora = System.nanoTime();
		float[] g = grieta(ahora);
		if (g == null) return;
		rayoAlCerrar = g[0];
		aberturaAlCerrar = g[1];
		ramasAlCerrar = g[2];
		grietaCierraDesde = ahora;
		if (grietaSono) tocar(GRIETA_CIERRA, 1f, 1f);
	}

	private static void musicaReviil() {
		reviilSigue = true;
		musicaReviil = new SonidoCielo2(SONIDO_REVIIL, () -> reviilSigue ? new float[]{1f, 1f} : null, false);
		Minecraft.getInstance().getSoundManager().play(musicaReviil);
	}

	private static boolean musicaReviilSonando() {
		return musicaReviil != null && Minecraft.getInstance().getSoundManager().isActive(musicaReviil);
	}

	private static SonidoCielo2 sonar(SonidoCielo2 ya, ResourceLocation evento, Supplier<float[]> estado, boolean repetir) {
		var sonidos = Minecraft.getInstance().getSoundManager();
		if (ya != null && !ya.isStopped() && sonidos.isActive(ya)) return ya;
		SonidoCielo2 s = new SonidoCielo2(evento, estado, repetir);
		sonidos.play(s);
		return s;
	}

	private static void tocar(SoundEvent evento, float tono, float volumen) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(evento, tono, volumen));
	}

	private static float[] grave(float r) {
		float sale = suave((r - RETUMBO_SALE[0]) / (RETUMBO_SALE[1] - RETUMBO_SALE[0]));
		return new float[]{(0.4f + 0.6f * suave(r / 0.8f)) * (1f - sale), RETUMBO_TONO[0] + (1f - RETUMBO_TONO[0]) * suave(r / RETUMBO_TONO[1])};
	}

	private static float[] estadoRetumbo() {
		return cieloDesde >= 0L && cieloCierraDesde < 0L ? grave(radioCielo(System.nanoTime())) : null;
	}

	private static float[] estadoDespeja() {
		return cieloDesde >= 0L && cieloCierraDesde >= 0L ? grave(radioCielo(System.nanoTime())) : null;
	}

	private static float[] estadoMusica() {
		float r = radioCielo(System.nanoTime());
		return cieloDesde < 0L ? null
				: new float[]{suave((r - RETUMBO_SALE[0]) / (RETUMBO_SALE[1] - RETUMBO_SALE[0])) * suave(musicaLibre), 1f};
	}

	// ---------------------------------------------------------------- cada tick

	public static void tick() {
		long ahora = System.nanoTime();
		if (!PROGRAMADOS.isEmpty()) {
			List<Programado> ya = new ArrayList<>();
			PROGRAMADOS.removeIf(p -> {
				if (p.en() > ahora) return false;
				ya.add(p);
				return true;
			});
			for (Programado p : ya) p.accion().run();
		}
		float paso = 1f / 60f;
		musicaLibre = musicaReviilSonando() ? Math.max(0f, musicaLibre - paso) : Math.min(1f, musicaLibre + paso);
		if (grietaDesde >= 0L && grietaCierraDesde < 0L && !grietaSono) {
			float t = (ahora - grietaDesde) / 1.0E9f;
			for (; pulsosSonados < PULSO_VOLUMEN.length && t >= PULSOS[pulsosSonados][0] * RAYO; pulsosSonados++) {
				if (t - PULSOS[pulsosSonados][0] * RAYO < 0.25f) tocar(GRIETA_RAYO, 1f, PULSO_VOLUMEN[pulsosSonados]);
			}
			if (t >= RAYO) {
				grietaSono = true;
				if (t - RAYO < 0.25f) tocar(GRIETA_ABRE, 1f, 1f);
			}
		}
		// El tinte entra y sale de a poco.
		boolean conEfectos = efectosHasta >= 0L && ahora < efectosHasta;
		tinte = conEfectos ? Math.min(1f, tinte + 0.05f / 0.4f * 1f) : Math.max(0f, tinte - 0.05f / 1.5f);
	}

	// ---------------------------------------------------------------- formas del cielo y la grieta

	private static double avance(double x) {
		return x - AP_ARRANQUE * (1.0 - Math.exp(-x / AP_ARRANQUE));
	}

	private static float apertura(float u) {
		u = Math.max(0f, Math.min(1f, u));
		return (float) (AP_FINAL * avance(u) / avance(1.0));
	}

	private static float inversaApertura(float r) {
		if (r <= 0f) return 0f;
		float lo = 0f, hi = 1f;
		for (int i = 0; i < 20; i++) {
			float m = (lo + hi) / 2f;
			if (apertura(m) < r) lo = m;
			else hi = m;
		}
		return lo;
	}

	/** Segundos desde que el rojo empieza hasta que llega lo bastante lejos para que quepa la grieta. */
	private static float cieloCubreGrieta() {
		double necesita = LARGO + 0.05 + 0.46 + 0.22;
		double lo = 0, hi = 1;
		for (int i = 0; i < 30; i++) {
			double u = (lo + hi) / 2;
			if (AP_FINAL * avance(u) / avance(1.0) < necesita) lo = u;
			else hi = u;
		}
		return (float) (hi * CIELO_ABRIR);
	}

	private static float suave(float x) {
		x = Math.max(0f, Math.min(1f, x));
		return x * x * (3f - 2f * x);
	}

	private static float sale(float x) {
		x = Math.max(0f, Math.min(1f, x));
		return 1f - (1f - x) * (1f - x);
	}

	private static float radioCielo(long ahora) {
		if (cieloDesde < 0L) return 0f;
		if (cieloCierraDesde >= 0L) {
			float v = (ahora - cieloCierraDesde) / 1.0E9f / CIELO_CERRAR;
			if (v >= 1f) {
				cieloCierraDesde = -1L;
				cieloDesde = -1L;
				nubesVuelvenDesde = ahora;
				return 0f;
			}
			return (float) (cieloAlCerrar * avance(1f - v) / avance(1.0));
		}
		return apertura((ahora - cieloDesde) / 1.0E9f / CIELO_ABRIR);
	}

	/** {largo del rayo, abertura, ramas, destello, radio del anillo, su opacidad, partículas, s desde el cabezazo, cuál}. */
	private static float[] grieta(long ahora) {
		if (grietaDesde < 0L) return null;
		float t = (ahora - grietaDesde) / 1.0E9f;
		float destello = 0f, radio = 0f, aro = 0f;
		float largo, abre, ramas, part;
		if (grietaCierraDesde >= 0L) {
			float c = (ahora - grietaCierraDesde) / 1.0E9f;
			abre = aberturaAlCerrar * (1f - suave(c / CERRAR));
			float c2 = Math.max(0f, c - (aberturaAlCerrar > 0f ? CERRAR : 0f));
			largo = rayoAlCerrar * (1f - suave(c2 / CERRAR));
			ramas = ramasAlCerrar * (1f - suave(c2 / (0.6f * CERRAR)));
			part = 1f - suave(c2 / CERRAR);
			if (largo <= 0.001f && abre <= 0.001f) {
				cabezazoDesde = grietaCierraDesde = grietaDesde = -1L;
				return null;
			}
		} else {
			float u = t / RAYO;
			largo = 0.35f * sale((u - 0.02f) / 0.17f) + 0.65f * sale((u - 0.44f) / 0.14f);
			ramas = suave((u - 0.15f) / 0.75f);
			for (float[] pu : PULSOS) {
				float dt = t - pu[0] * RAYO;
				if (dt < 0f) continue;
				destello = Math.max(destello, pu[1] * (dt < 0.02f ? dt / 0.02f : (float) Math.exp(-(dt - 0.02f) / pu[2])));
				float da = dt - 0.12f;
				if (da >= 0f && da < 0.9f) {
					float a = (float) Math.exp(-da / 0.2f) * Math.min(1f, da / 0.03f);
					if (a > aro) {
						aro = a;
						radio = 0.18f + 1.8f * da;
					}
				}
			}
			part = suave((u - 0.1f) / 0.3f);
			abre = suave((t - RAYO) / ABRIR);
		}
		float edad = -1f, cual = 0f;
		if (cabezazoDesde >= 0L) {
			float e = (ahora - cabezazoDesde) / 1.0E9f;
			if (e < 2.6f) {
				edad = e;
				cual = cabezazos;
				float g = e - 0.2f;
				if (g >= 0f) destello = Math.max(destello, 0.6f * (g < 0.02f ? g / 0.02f : (float) Math.exp(-(g - 0.02f) / 0.1f)));
			} else {
				cabezazoDesde = -1L;
			}
		}
		return new float[]{largo, abre, ramas, destello, radio, aro, part, edad, cual};
	}

	// ---------------------------------------------------------------- dibujo

	private static boolean enOverworld() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.dimension() == Level.OVERWORLD;
	}

	/** Antes de dibujar el mundo: la cabeza de Reviil en su imagen (sólo si la grieta está abierta). */
	public static void antesDelMundo() {
		caraTextura = -1;
		if (grietaDesde < 0L || !enOverworld()) return;
		long ahora = System.nanoTime();
		float[] g = grieta(ahora);
		if (g != null && g[1] > 0f) caraTextura = CabezaReviil.dibujar((ahora - grietaDesde) / 1.0E9f, g[7]);
	}

	/** Después del cielo de Minecraft: un cubo alrededor de la cámara donde el shader dibuja el rojo y la grieta. */
	public static void dibujar(Matrix4f vista) {
		if (cieloDesde < 0L && grietaDesde < 0L) {
			float vuelven = nubesVuelvenDesde >= 0L ? (System.nanoTime() - nubesVuelvenDesde) / 1.0E9f / 2f : 1f;
			cubre = 1f - suave(vuelven);
			if (vuelven >= 1f) nubesVuelvenDesde = -1L;
			return;
		}
		long ahora = System.nanoTime();
		float r = radioCielo(ahora);
		float[] g = grieta(ahora);
		float tapa;
		if (r > 0f) {
			tapa = cieloCierraDesde >= 0L ? 1f : suave(r / 0.3f);
			nubesVuelvenDesde = -1L;
		} else {
			tapa = nubesVuelvenDesde >= 0L ? 1f - suave((ahora - nubesVuelvenDesde) / 1.0E9f / 2f) : 0f;
		}
		cubre = Math.max(tapa, g != null ? Math.max(g[0], g[1]) : 0f);
		if (r <= 0f && g == null) return;
		ShaderInstance sh = shader;
		if (sh == null || !enOverworld()) return;

		Minecraft mc = Minecraft.getInstance();
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		RenderSystem.setShader(() -> sh);
		float t = (ahora / 1_000_000L % 3_600_000L) / 1000f;
		set(sh, "Camara", (float) cam.x, (float) cam.y, (float) cam.z);
		set(sh, "Polo", (float) cam.x, (float) cam.y + 1000f, (float) cam.z);
		float efecto = cieloDesde >= 0L ? (ahora - efectoDesde) / 1.0E9f : 0f;
		set(sh, "Cielo", r, t, Math.min(1f, r / 0.012f), efecto);
		set(sh, "Forma", formaEje, formaHuevo, ANIMACION, 0f);
		if (g != null) {
			set(sh, "Grieta", g[0], g[1], 1f, angulo);
			set(sh, "GrietaExtra", semilla, g[2], LARGO, (ahora - grietaDesde) / 1.0E9f);
			set(sh, "Pulso", g[3], g[4], g[5], g[6]);
			set(sh, "Latido", g[7], g[8], 0f, 0f);
		} else {
			set(sh, "Grieta", 0f, 0f, 0f, 0f);
			set(sh, "Pulso", 0f, 0f, 0f, 0f);
			set(sh, "Latido", -1f, 0f, 0f, 0f);
		}

		AbstractTexture ruido = mc.getTextureManager().getTexture(RUIDO);
		if (!filtroPuesto) {
			ruido.setFilter(true, false);
			GlStateManager._bindTexture(ruido.getId());
			GlStateManager._texParameter(3553, 10242, 10497);   // se repite a lo ancho
			GlStateManager._texParameter(3553, 10243, 10497);   // y a lo alto
			filtroPuesto = true;
		}
		RenderSystem.setShaderTexture(0, caraTextura >= 0 ? caraTextura : ruido.getId());
		RenderSystem.setShaderTexture(1, ruido.getId());

		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.mul(vista);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		float s = 50f;
		float[][] caras = {
				{-s, s, -s}, {s, s, -s}, {s, s, s}, {-s, s, s},
				{-s, -s, -s}, {s, -s, -s}, {s, s, -s}, {-s, s, -s},
				{s, -s, s}, {-s, -s, s}, {-s, s, s}, {s, s, s},
				{-s, -s, s}, {-s, -s, -s}, {-s, s, -s}, {-s, s, s},
				{s, -s, -s}, {s, -s, s}, {s, s, s}, {s, s, -s},
				{-s, -s, -s}, {-s, -s, s}, {s, -s, s}, {s, -s, -s}};
		for (float[] v : caras) b.addVertex(v[0], v[1], v[2]);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		mv.popMatrix();
		RenderSystem.applyModelViewMatrix();
	}

	private static void set(ShaderInstance sh, String nombre, float... v) {
		var u = sh.getUniform(nombre);
		if (u == null) return;
		switch (v.length) {
			case 3 -> u.set(v[0], v[1], v[2]);
			case 4 -> u.set(v[0], v[1], v[2], v[3]);
			default -> u.set(v[0]);
		}
	}

	/** El color de la niebla: cuando el rojo ya cubre casi todo, el horizonte se vuelve rojo oscuro. Null = sin cambio. */
	public static float[] niebla(float r, float g, float b) {
		if (cieloDesde < 0L || !enOverworld()) return null;
		float k = suave((radioCielo(System.nanoTime()) - 1.35f) / 0.9f);
		if (k <= 0f) return null;
		return new float[]{r + (NIEBLA[0] - r) * k, g + (NIEBLA[1] - g) * k, b + (NIEBLA[2] - b) * k};
	}

	/** Qué tanto se ven las nubes (1 = normal, 0 = nada). */
	public static float nubes() {
		return enOverworld() ? 1f - cubre : 1f;
	}

	// ---------------------------------------------------------------- tinte y temblor

	/** La pantalla teñida de rojo (más fuerte en los bordes). */
	public static void dibujarTinte(GuiGraphics graphics) {
		if (tinte <= 0.003f) return;
		float w = graphics.guiWidth(), h = graphics.guiHeight();
		PoseStack pose = graphics.pose();
		pose.pushPose();
		Matrix4f m = pose.last().pose();
		VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());
		int gx = 16, gy = 10;
		for (int j = 0; j < gy; j++) {
			for (int i = 0; i < gx; i++) {
				float x0 = w * i / gx, x1 = w * (i + 1) / gx, y0 = h * j / gy, y1 = h * (j + 1) / gy;
				vc.addVertex(m, x0, y0, 0f).setColor(colorTinte(i, j, gx, gy));
				vc.addVertex(m, x0, y1, 0f).setColor(colorTinte(i, j + 1, gx, gy));
				vc.addVertex(m, x1, y1, 0f).setColor(colorTinte(i + 1, j + 1, gx, gy));
				vc.addVertex(m, x1, y0, 0f).setColor(colorTinte(i + 1, j, gx, gy));
			}
		}
		graphics.flush();
		pose.popPose();
	}

	private static int colorTinte(int i, int j, int gx, int gy) {
		float nx = 2f * i / gx - 1f, ny = 2f * j / gy - 1f;
		float r = (float) Math.sqrt(nx * nx + ny * ny) / 1.25f;
		float a = Math.min(85f, 38f + 31.5f * (float) Math.pow(r, 1.89)) / 255f * suave(tinte);
		return Math.round(a * 255f) << 24 | 0xBE0C0C;
	}

	/** La cámara tiembla un poco (se llama antes del movimiento de cámara al recibir daño). */
	public static void temblor(PoseStack m, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			temblorEntra = 0f;
			return;
		}
		long ahora = System.nanoTime();
		float dt = (ahora - temblorUltimo) / 1.0E9f;
		if (temblorUltimo == 0L || dt > 0.5f) dt = 0f;
		temblorUltimo = ahora;
		boolean tiembla = efectosHasta >= 0L && ahora < efectosHasta;
		if (tiembla) {
			temblorGrados = 0.8f;
			temblorEntra = Math.min(1f, temblorEntra + dt / 0.4f);
		} else {
			temblorEntra = Math.max(0f, temblorEntra - dt / 0.6f);
		}
		float a = temblorGrados * suave(temblorEntra) * mc.options.screenEffectScale().get().floatValue();
		if (a <= 0.001f) return;
		double t = (mc.level.getGameTime() + partialTick) / 20.0;
		m.mulPose(Axis.XP.rotationDegrees(a * 0.6f * ruido(t, 0)));
		m.mulPose(Axis.YP.rotationDegrees(a * ruido(t, 1)));
		m.mulPose(Axis.ZP.rotationDegrees(a * ruido(t, 2)));
	}

	private static float ruido(double t, int c) {
		return 0.7f * capa(t * 9.0, c) + 0.3f * capa(t * 21.0, c + 7);
	}

	private static float capa(double x, int semilla) {
		long i = (long) Math.floor(x);
		float f = (float) (x - i);
		f = f * f * (3f - 2f * f);
		float a = azar(i, semilla), b = azar(i + 1L, semilla);
		return a + (b - a) * f;
	}

	private static float azar(long i, int semilla) {
		long h = i * -7046029254386353131L + semilla * -4417276706812531889L;
		h = (h ^ h >>> 31) * -4658895280553007687L;
		h ^= h >>> 29;
		return (h & 65535L) / 32767.5f - 1f;
	}
}
