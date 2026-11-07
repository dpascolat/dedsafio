package com.dedsafio4.client.demonios;

import com.dedsafio4.demonios.PuzzleCamara;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * El puzzle de la cámara (lo abre el Farol al pegar): "Desbloquea la cámara — Arrastra la palanca para girar". Un
 * aro de colores con una palanca arriba: hay que agarrarla con el mouse y arrastrarla alrededor del aro hasta dar
 * las vueltas pedidas (para cualquier lado). Abajo, la barra y "X de 4 giros". No se puede cerrar con Escape;
 * mientras está abierto no se puede mover la cámara.
 */
public class PuzzleCamaraScreen extends Screen {
	/** Radio del aro de colores (en unidades de la interfaz) y su grosor. */
	private static final int RADIO = 38, GROSOR = 7;
	private static final float VUELTA = Mth.TWO_PI;

	private final int giros;
	/** Cuánto giró en total (en radianes; para un lado suma y para el otro resta). */
	private float total;
	private float ultimo;
	private boolean arrastrando;
	private int girosAntes;

	public PuzzleCamaraScreen(int giros) {
		super(Component.literal("Desbloquea la cámara"));
		this.giros = giros;
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(PuzzleCamara.AbrirPayload.TYPE, (payload, contexto) -> contexto.client().execute(() -> {
			Minecraft mc = contexto.client();
			if (mc.screen instanceof PuzzleCamaraScreen) return;   // ya lo está haciendo: no se reinicia
			mc.setScreen(new PuzzleCamaraScreen(payload.giros()));
		}));
	}

	private int cx() {
		return width / 2;
	}

	private int cy() {
		return height / 2 - 6;
	}

	/** El ángulo de la palanca (0 arriba, crece para la derecha). */
	private float angulo() {
		return total;
	}

	private int girosHechos() {
		return Math.min(giros, (int) (Math.abs(total) / VUELTA));
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
		g.fill(0, 0, width, height, 0x88000000);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		int cx = cx(), cy = cy();
		g.drawCenteredString(font, Component.literal("Desbloquea la cámara"), cx, cy - RADIO - 50, 0xFFFFFF);
		g.drawCenteredString(font, Component.literal("Arrastra la palanca para girar"), cx, cy - RADIO - 36, 0xD8D8D8);

		// El aro: borde negro, colores del arcoíris según el ángulo, y adentro oscuro.
		circulo(g, cx, cy, RADIO + 4, 0xFF0B0B0F);
		for (int dy = -RADIO; dy <= RADIO; dy++) {
			for (int dx = -RADIO; dx <= RADIO; dx++) {
				double r = Math.sqrt(dx * dx + dy * dy);
				if (r > RADIO || r < RADIO - GROSOR) continue;
				float a = (float) Math.atan2(dx, -dy);
				int color = Mth.hsvToRgb(((a / VUELTA) + 1f) % 1f, 0.85f, 1f);
				g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, 0xFF000000 | color);
			}
		}
		circulo(g, cx, cy, RADIO - GROSOR - 1, 0xFF0B0B0F);

		// Las dos flechas del medio (giran con la palanca).
		flechas(g, cx, cy, angulo());

		// La palanca: círculo gris sobre el aro, un palito y la manija roja hacia afuera.
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0);
		g.pose().mulPose(Axis.ZP.rotation(angulo()));
		int base = -(RADIO - GROSOR / 2);
		g.fill(-2, base - 18, 2, base, 0xFF6B6B74);                // palito
		g.fill(-5, base - 28, 5, base - 16, 0xFF7A1530);           // borde de la manija
		g.fill(-4, base - 27, 4, base - 17, arrastrando ? 0xFFE04466 : 0xFFC8324F);
		g.fill(-3, base - 26, 0, base - 23, 0xFFF08AA0);           // brillo
		circuloRel(g, 0, base, 7, 0xFF2A2A30);
		circuloRel(g, 0, base, 6, 0xFFB9B9C2);
		circuloRel(g, 0, base, 3, 0xFF6B6B74);
		g.pose().popPose();

		// La barra y cuántos giros lleva.
		int ancho = 170, x0 = cx - ancho / 2, y0 = cy + RADIO + 40;
		float avance = Math.min(1f, Math.abs(total) / (giros * VUELTA));
		g.fill(x0 - 1, y0 - 1, x0 + ancho + 1, y0 + 13, 0xFF000000);
		g.fill(x0, y0, x0 + ancho, y0 + 12, 0xFF141418);
		if (avance > 0) g.fill(x0, y0, x0 + (int) (ancho * avance), y0 + 12, 0xFF000000 | Mth.hsvToRgb(avance * 0.8f, 0.8f, 1f));
		g.drawCenteredString(font, Component.literal(girosHechos() + " de " + giros + " giros"), cx, y0 + 22, 0xFFFFFF);
	}

	private static void flechas(GuiGraphics g, int cx, int cy, float giro) {
		for (int f = 0; f < 2; f++) {
			float inicio = giro + f * Mth.PI + 0.35f, fin = inicio + 2.1f;
			for (float a = inicio; a <= fin; a += 0.06f) {
				int x = cx + Math.round(Mth.sin(a) * 15), y = cy - Math.round(Mth.cos(a) * 15);
				g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFFFFFFF);
			}
			// Punta de la flecha (un triángulo hacia adelante en el sentido del giro).
			float tx = Mth.cos(fin), ty = Mth.sin(fin);     // dirección de avance
			float nx = Mth.sin(fin), ny = -Mth.cos(fin);    // hacia afuera
			float px = cx + nx * 15, py = cy + ny * 15;
			for (int k = 0; k <= 6; k++) {
				float ancho = (6 - k) * 1.1f;
				for (float w = -ancho; w <= ancho; w += 1f) {
					int x = Math.round(px + tx * k + nx * w), y = Math.round(py + ty * k + ny * w);
					g.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
				}
			}
		}
	}

	private static void circulo(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int w = (int) Math.sqrt(r * r - dy * dy);
			g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
		}
	}

	private static void circuloRel(GuiGraphics g, int cx, int cy, int r, int color) {
		circulo(g, cx, cy, r, color);
	}

	private float anguloMouse(double mx, double my) {
		return (float) Math.atan2(mx - cx(), -(my - cy()));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (boton == 0) {
			// Se agarra la palanca (el círculo o la manija), con un poco de margen.
			float a = angulo();
			double r1 = RADIO - GROSOR / 2.0, r2 = r1 + 22;
			double px1 = cx() + Math.sin(a) * r1, py1 = cy() - Math.cos(a) * r1;
			double px2 = cx() + Math.sin(a) * r2, py2 = cy() - Math.cos(a) * r2;
			if (Math.hypot(mx - px1, my - py1) < 14 || Math.hypot(mx - px2, my - py2) < 14) {
				arrastrando = true;
				ultimo = anguloMouse(mx, my);
				return true;
			}
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int boton, double dx, double dy) {
		if (!arrastrando) return super.mouseDragged(mx, my, boton, dx, dy);
		if (Math.hypot(mx - cx(), my - cy()) < 6) return true;   // en el centro el ángulo salta: no cuenta
		float a = anguloMouse(mx, my);
		float d = Mth.wrapDegrees((a - ultimo) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
		ultimo = a;
		if (Math.abs(d) > 1.2f) return true;   // un salto grande (pasó por el medio): no cuenta
		total += d;
		int hechos = girosHechos();
		if (hechos != girosAntes) {
			girosAntes = hechos;
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 0.8f + hechos * 0.2f));
		}
		if (hechos >= giros) terminar();
		return true;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int boton) {
		arrastrando = false;
		return super.mouseReleased(mx, my, boton);
	}

	private void terminar() {
		ClientPlayNetworking.send(new PuzzleCamara.ListoPayload());
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.4f));
		Minecraft.getInstance().setScreen(null);
	}
}
