package com.dedsafio4.client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * El menú principal del Dedsafío (reemplaza al de Minecraft): el arte de SAO estudios con el personaje que respira y
 * polvo que flota (un loop de 2 s), y tres botones: JUGAR (entra directo al servidor), OPCIONES y SALIR.
 * Apretando M+Q aparece el menú normal de Minecraft (y M+Q ahí vuelve a este).
 */
public class MenuPrincipalScreen extends Screen {
	/**
	 * El servidor al que entra JUGAR. Se lee del dedsafio.json de GitHub (campo "servidor"), así se puede cambiar
	 * sin sacar otra versión del mod; si no hay internet o no está, se usa IP_POR_DEFECTO.
	 */
	public static final String IP_POR_DEFECTO = "";
	public static final String NOMBRE_SERVIDOR = "Dedsafío 4";
	private static final String MANIFIESTO = "https://raw.githubusercontent.com/dpascolat/dedsafio/main/dedsafio.json";
	private static volatile String ipServidor = IP_POR_DEFECTO;

	/** Al abrir el juego se fija (sin trabar nada) qué servidor dice el dedsafio.json. */
	private static void buscarServidor() {
		java.net.http.HttpClient.newHttpClient().sendAsync(
				java.net.http.HttpRequest.newBuilder(java.net.URI.create(MANIFIESTO))
						.timeout(java.time.Duration.ofSeconds(10)).header("User-Agent", "Dedsafio4").build(),
				java.net.http.HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
			try {
				var json = com.google.gson.JsonParser.parseString(r.body()).getAsJsonObject();
				if (json.has("servidor") && !json.get("servidor").getAsString().isBlank()) {
					ipServidor = json.get("servidor").getAsString().strip();
				}
			} catch (RuntimeException ignorado) {
				// Sin internet o sin el campo: queda la IP por defecto.
			}
		});
	}

	/** Si está en true (después de M+Q), se muestra el menú normal de Minecraft hasta cerrar el juego. */
	public static boolean modoNormal = false;
	private static boolean yaAparecio = false;

	private static final ResourceLocation FONDO = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/fondo.png");
	private static final ResourceLocation PERSONAJE = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/personaje.png");
	/** JUGAR / OPCIONES / SALIR recortadas del arte y agrandadas (una debajo de la otra); en el fondo ya no están. */
	private static final ResourceLocation PALABRAS = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/palabras.png");
	private static final int PAL_W = 225, PAL_H = 123;
	private static final ResourceLocation POLVO = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/menu/polvo.png");
	private static final int W = 1673, H = 940;
	/** Dónde está el personaje recortado dentro del arte, y el punto de sus pies (no se mueven). */
	private static final int PJ_X = 788, PJ_Y = 430, PJ_W = 400, PJ_H = 430;
	private static final float PIES_X = 985, PIES_Y = 815;
	private static final float TAU = (float) (Math.PI * 2);

	/** Cada botón: dónde está en la tira de palabras (v, ancho, alto) y su centro en el arte (x, y). */
	private static final float[][] BOTONES = {{0, 152, 39, 540.5f, 488.5f}, {41, 225, 39, 541.5f, 566.5f}, {82, 133, 39, 539f, 643.5f}};
	private static final int[] COLORES = {0xFF3048, 0xFFE040, 0x50FF70};
	private static final int MARGEN = 10;

	private static final Particula[] POLVOS = crearPolvo();

	/** Para las pruebas automáticas: "1" deja OPCIONES iluminado, "opciones" / "video" abren esas pantallas. */
	private static final String MODO_PRUEBA = System.getenv("DEDSAFIO4_MENU_PRUEBA");
	private static final boolean PRUEBA = "1".equals(MODO_PRUEBA);
	private static boolean pruebaHecha = false;
	private final float[] brillo = new float[BOTONES.length];
	private int elegido = -1;          // el botón elegido con el teclado
	private long aparecio;
	private float escala, ox, oy;

	/** Fuera de un mundo, los menús (opciones y demás) van con fondo negro, salvo en el modo normal (M+Q). */
	public static boolean fondoNegro() {
		return !modoNormal && net.minecraft.client.Minecraft.getInstance().level == null;
	}

	/** En el menú normal de Minecraft, M+Q vuelve al del Dedsafío. */
	public static void registrar() {
		buscarServidor();
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((mc, pantalla, ancho, alto) -> {
			if (pantalla.getClass() != TitleScreen.class) return;
			net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(pantalla).register((p, tecla, sc, mods) -> {
				if (esMasQ(tecla)) {
					modoNormal = false;
					mc.setScreen(new MenuPrincipalScreen());
				}
			});
		});
	}

	public MenuPrincipalScreen() {
		super(Component.translatable("narrator.screen.title"));
	}

	@Override
	protected void init() {
		escala = Math.max(width / (float) W, height / (float) H);
		ox = (width - W * escala) / 2;
		oy = (height - H * escala) / 2;
		if (aparecio == 0) aparecio = Util.getMillis();
		if (!pruebaHecha && MODO_PRUEBA != null && !PRUEBA) {
			pruebaHecha = true;
			yaAparecio = true;
			OptionsScreen opciones = new OptionsScreen(this, minecraft.options);
			minecraft.execute(() -> minecraft.setScreen("video".equals(MODO_PRUEBA)
					? new net.minecraft.client.gui.screens.options.VideoSettingsScreen(opciones, minecraft, minecraft.options) : opciones));
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		// Todo se dibuja en render().
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		float t = (Util.getMillis() % 2000L) / 1000f;
		int sobre = botonEn(mx, my);
		if (PRUEBA) sobre = 1;
		for (int i = 0; i < brillo.length; i++) {
			boolean activo = i == sobre || (sobre < 0 && i == elegido);
			brillo[i] += ((activo ? 1 : 0) - brillo[i]) * Math.min(1, parcial * 0.35f + 0.12f);
		}

		g.fill(0, 0, width, height, 0xFF000000);
		g.pose().pushPose();
		g.pose().translate(ox, oy, 0);
		g.pose().scale(escala, escala, 1);
		RenderSystem.enableBlend();
		g.blit(FONDO, 0, 0, 0, 0, W, H, W, H);
		dibujarPersonaje(g, t);
		dibujarBotones(g);
		dibujarPolvo(g, t);
		RenderSystem.defaultBlendFunc();
		g.setColor(1, 1, 1, 1);
		g.pose().popPose();

		// La primera vez que aparece (al abrir el juego), entra desde negro.
		float entrada = yaAparecio ? 1 : Math.min(1, (Util.getMillis() - aparecio) / 1200f);
		if (entrada < 1) {
			g.fill(0, 0, width, height, ((int) ((1 - entrada) * 255) << 24));
		} else {
			yaAparecio = true;
		}
	}

	/** El personaje "respira" (se estira un poquito) y se balancea muy poco, con los pies fijos. */
	private void dibujarPersonaje(GuiGraphics g, float t) {
		float b = (float) Math.sin(TAU * t / 2);
		float balanceo = (float) Math.sin(TAU * t / 2 + Math.PI / 2);
		float sy = 1 + 0.006f * (b * 0.5f + 0.5f);
		float sx = 1 + 0.002f * (b * 0.5f + 0.5f);
		g.pose().pushPose();
		g.pose().translate(PIES_X, PIES_Y, 0);
		g.pose().mulPose(Axis.ZP.rotationDegrees(0.25f * balanceo));
		g.pose().scale(sx, sy, 1);
		g.pose().translate(-PIES_X, -PIES_Y, 0);
		g.blit(PERSONAJE, PJ_X, PJ_Y, 0, 0, PJ_W, PJ_H, PJ_W, PJ_H);
		g.pose().popPose();
	}

	/** Las palabras de los botones; al pasar el mouse crecen un poquito y se iluminan con su color. */
	private void dibujarBotones(GuiGraphics g) {
		for (int i = 0; i < BOTONES.length; i++) {
			float[] b = BOTONES[i];
			float a = brillo[i];
			int w = (int) b[1], h = (int) b[2];
			g.pose().pushPose();
			g.pose().translate(b[3], b[4], 0);
			float s = 1 + 0.06f * a;
			g.pose().scale(s, s, 1);
			if (a > 0.01f) {
				RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
						com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
				float r = ((COLORES[i] >> 16) & 255) / 255f, v = ((COLORES[i] >> 8) & 255) / 255f, az = (COLORES[i] & 255) / 255f;
				g.setColor(r, v, az, 0.35f * a);
				g.blit(POLVO, -w, -h * 2, w * 2, h * 4, 0, 0, 64, 64, 64, 64);
				RenderSystem.defaultBlendFunc();
			}
			g.setColor(1, 1, 1, 1);
			g.pose().translate(-w / 2f, -h / 2f, 0);
			g.blit(PALABRAS, 0, 0, w, h, 0, b[0], w, h, PAL_W, PAL_H);
			if (a > 0.01f) {
				// Las letras otra vez encima, sumadas: quedan más brillantes.
				RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
						com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
				g.setColor(1, 1, 1, 0.7f * a);
				g.blit(PALABRAS, 0, 0, w, h, 0, b[0], w, h, PAL_W, PAL_H);
				RenderSystem.defaultBlendFunc();
				g.setColor(1, 1, 1, 1);
			}
			g.pose().popPose();
		}
	}

	private void dibujarPolvo(GuiGraphics g, float t) {
		for (Particula p : POLVOS) {
			float periodo = 2f / p.ciclos;
			float x = p.x + p.ax * (float) Math.sin(TAU * t / periodo + p.fase);
			float y = p.y + p.ay * (float) Math.sin(TAU * t / 2 + p.fase2);
			float o = p.base * (0.55f + 0.45f * (0.5f + 0.5f * (float) Math.sin(TAU * t / periodo + p.fase2)));
			float s = p.tam * 3;
			if (p.calida) g.setColor(1f, 190 / 255f, 120 / 255f, o);
			else g.setColor(1f, 240 / 255f, 225 / 255f, o);
			g.pose().pushPose();
			g.pose().translate(x - s / 2, y - s / 2, 0);
			g.pose().scale(s / 64f, s / 64f, 1);
			g.blit(POLVO, 0, 0, 0, 0, 64, 64, 64, 64);
			g.pose().popPose();
		}
		g.setColor(1, 1, 1, 1);
	}

	/** Qué botón hay bajo el mouse (o -1). */
	private int botonEn(double mx, double my) {
		double ix = (mx - ox) / escala, iy = (my - oy) / escala;
		for (int i = 0; i < BOTONES.length; i++) {
			float[] b = BOTONES[i];
			if (Math.abs(ix - b[3]) <= b[1] / 2 + MARGEN && Math.abs(iy - b[4]) <= b[2] / 2 + MARGEN) return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		int i = botonEn(mx, my);
		if (boton == 0 && i >= 0) {
			apretar(i);
			return true;
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public boolean keyPressed(int tecla, int scancode, int mods) {
		if (esMasQ(tecla)) {
			modoNormal = true;
			minecraft.setScreen(new TitleScreen());
			return true;
		}
		switch (tecla) {
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> {
				elegido = (elegido + 1) % BOTONES.length;
				return true;
			}
			case GLFW.GLFW_KEY_UP -> {
				elegido = (elegido + BOTONES.length - 1) % BOTONES.length;
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
				if (elegido >= 0) apretar(elegido);
				return true;
			}
			default -> {
				return super.keyPressed(tecla, scancode, mods);
			}
		}
	}

	/** ¿Se acaba de completar M+Q (una de las dos apretada y la otra recién)? */
	public static boolean esMasQ(int tecla) {
		long ventana = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
		return (tecla == GLFW.GLFW_KEY_Q && InputConstants.isKeyDown(ventana, GLFW.GLFW_KEY_M))
				|| (tecla == GLFW.GLFW_KEY_M && InputConstants.isKeyDown(ventana, GLFW.GLFW_KEY_Q));
	}

	private void apretar(int i) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		switch (i) {
			case 0 -> jugar();
			case 1 -> minecraft.setScreen(new OptionsScreen(this, minecraft.options));
			default -> minecraft.stop();
		}
	}

	/** Entra directo al servidor del Dedsafío (nunca a la lista de Multijugador). */
	private void jugar() {
		String ip = ipServidor;
		if (ip.isEmpty()) {
			minecraft.getToasts().addToast(net.minecraft.client.gui.components.toasts.SystemToast.multiline(minecraft,
					net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
					Component.literal("Servidor cerrado"), Component.literal("Todavía no se puede entrar al servidor del Dedsafío.")));
			return;
		}
		ServerData datos = new ServerData(NOMBRE_SERVIDOR, ip, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(ip), datos, false, null);
	}

	// --- El polvo: 90 partículas con la misma semilla que el diseño (mulberry32(7)) ---

	private record Particula(float x, float y, float tam, float ax, float ay, float fase, float fase2, boolean calida,
							 float base, int ciclos) {}

	private static Particula[] crearPolvo() {
		int[] semilla = {7};
		java.util.function.DoubleSupplier r = () -> {
			semilla[0] += 0x6D2B79F5;
			int s = semilla[0];
			int t = (s ^ (s >>> 15)) * (1 | s);
			t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
			return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
		};
		Particula[] out = new Particula[90];
		for (int i = 0; i < out.length; i++) {
			boolean grande = i < 12;
			float x = (float) (r.getAsDouble() * W), y = (float) (60 + r.getAsDouble() * (H - 160));
			float tam = (float) (grande ? 5 + r.getAsDouble() * 6 : 1.5 + r.getAsDouble() * 2.5);
			float ax = (float) (4 + r.getAsDouble() * 10), ay = (float) (3 + r.getAsDouble() * 8);
			float fase = (float) (r.getAsDouble() * TAU), fase2 = (float) (r.getAsDouble() * TAU);
			boolean calida = r.getAsDouble() < 0.75;
			float base = (float) (grande ? 0.25 : 0.4 + r.getAsDouble() * 0.4);
			int ciclos = 1 + (int) Math.floor(r.getAsDouble() * 2);
			out[i] = new Particula(x, y, tam, ax, ay, fase, fase2, calida, base, ciclos);
		}
		return out;
	}
}
