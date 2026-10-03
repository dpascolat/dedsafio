package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.candados.CandadoCodigoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import org.lwjgl.glfw.GLFW;

/**
 * Teclado del candado: el código se escribe con los números, el botón rojo borra,
 * el verde confirma y el de arriba a la derecha cierra. También se puede tipear.
 */
public class CandadoScreen extends Screen {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/candado_teclado.png");

	/** La imagen mide 456x536; se dibuja a la mitad. */
	private static final int ANCHO = 228, ALTO = 268;
	public static final int LARGO_CODIGO = 8;

	// Medidas sacadas de la imagen, ya divididas a la mitad.
	private static final int TECLA = 30;
	private static final int[] COLUMNAS = {72, 115, 159};
	private static final int[] FILAS = {95, 138, 183, 226};
	private static final int CERRAR_X = 182, CERRAR_Y = 42, CERRAR_LADO = 19;

	private static final int NARANJA = 0xFFFFA23C, ROJO = 0xFFFF5555, BLANCO = 0xFFE8E8E8;

	private final BlockPos pos;
	private final boolean cerrar;
	private final String aviso;
	private final StringBuilder codigo = new StringBuilder();
	/** Tecla que se muestra apretada, y cuántos ticks le quedan. */
	private int teclaApretada = -1, brillo;
	private float escala = 1f;
	private int izquierda, arriba;

	public CandadoScreen(BlockPos pos, boolean cerrar, String aviso) {
		super(Component.literal(cerrar ? "Elige tu código" : "Introduce tu código"));
		this.pos = pos;
		this.cerrar = cerrar;
		this.aviso = aviso;
	}

	@Override
	protected void init() {
		// Si la ventana es baja, se achica para que entre entera.
		escala = Math.min(1f, (this.height - 8) / (float) ALTO);
		izquierda = Math.round((this.width - ANCHO * escala) / 2f);
		arriba = Math.round((this.height - ALTO * escala) / 2f);
	}

	/** Las 12 teclas: 0..8 son los números 1..9; 9 = borrar, 10 = el cero, 11 = confirmar. */
	private int teclaEn(double raton, double ratonY) {
		for (int i = 0; i < 12; i++) {
			int x = COLUMNAS[i % 3] - TECLA / 2, y = FILAS[i / 3] - TECLA / 2;
			if (dentro(raton, ratonY, x, y, TECLA, TECLA)) return i;
		}
		return -1;
	}

	private boolean dentro(double raton, double ratonY, int x, int y, int ancho, int alto) {
		double rx = (raton - izquierda) / escala, ry = (ratonY - arriba) / escala;
		return rx >= x && rx < x + ancho && ry >= y && ry < y + alto;
	}

	@Override
	public boolean mouseClicked(double raton, double ratonY, int boton) {
		if (boton == 0) {
			if (dentro(raton, ratonY, CERRAR_X, CERRAR_Y, CERRAR_LADO, CERRAR_LADO)) {
				sonido(0.8f);
				onClose();
				return true;
			}
			int tecla = teclaEn(raton, ratonY);
			if (tecla >= 0) {
				apretar(tecla);
				return true;
			}
		}
		return super.mouseClicked(raton, ratonY, boton);
	}

	private void apretar(int tecla) {
		teclaApretada = tecla;
		brillo = 3;
		if (tecla == 9) {
			borrar();
		} else if (tecla == 11) {
			enviar();
		} else {
			agregar(tecla == 10 ? '0' : (char) ('1' + tecla));
		}
	}

	private void agregar(char numero) {
		if (codigo.length() >= LARGO_CODIGO) return;
		codigo.append(numero);
		sonido(1.2f + codigo.length() * 0.05f);
	}

	private void borrar() {
		if (codigo.isEmpty()) return;
		codigo.setLength(codigo.length() - 1);
		sonido(0.7f);
	}

	private void enviar() {
		if (codigo.isEmpty()) {
			sonido(0.5f);
			return;
		}
		sonido(1.6f);
		ClientPlayNetworking.send(new CandadoCodigoPayload(pos, cerrar, codigo.toString()));
		onClose();
	}

	private void sonido(float tono) {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), tono));
	}

	@Override
	public boolean keyPressed(int tecla, int codigoEscaneo, int modificadores) {
		if (tecla >= GLFW.GLFW_KEY_0 && tecla <= GLFW.GLFW_KEY_9) {
			apretar(tecla == GLFW.GLFW_KEY_0 ? 10 : tecla - GLFW.GLFW_KEY_1);
			return true;
		}
		if (tecla >= GLFW.GLFW_KEY_KP_0 && tecla <= GLFW.GLFW_KEY_KP_9) {
			apretar(tecla == GLFW.GLFW_KEY_KP_0 ? 10 : tecla - GLFW.GLFW_KEY_KP_1);
			return true;
		}
		if (tecla == GLFW.GLFW_KEY_BACKSPACE) {
			apretar(9);
			return true;
		}
		if (tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_KP_ENTER) {
			apretar(11);
			return true;
		}
		return super.keyPressed(tecla, codigoEscaneo, modificadores);
	}

	@Override
	public void tick() {
		if (brillo > 0 && --brillo == 0) teclaApretada = -1;
	}

	@Override
	public void render(GuiGraphics graphics, int raton, int ratonY, float parcial) {
		super.render(graphics, raton, ratonY, parcial);
		var pose = graphics.pose();
		pose.pushPose();
		pose.translate(izquierda, arriba, 0);
		pose.scale(escala, escala, 1f);
		graphics.blit(TEXTURA, 0, 0, 0, 0, ANCHO, ALTO, ANCHO, ALTO);

		// Título arriba: en rojo cuando el código estuvo mal.
		Component titulo = aviso.isEmpty() ? this.title : Component.literal(aviso);
		graphics.drawCenteredString(this.font, titulo, ANCHO / 2, 14, aviso.isEmpty() ? BLANCO : ROJO);

		// El código escrito, en la pantallita del candado (tapa las rayitas naranjas de adorno).
		if (!codigo.isEmpty()) {
			int mitad = this.font.width(codigo.toString()) / 2 + 3;
			graphics.fill(ANCHO / 2 - mitad, 65, ANCHO / 2 + mitad, 77, 0xFF0B0D10);
			graphics.drawCenteredString(this.font, codigo.toString(), ANCHO / 2, 67, NARANJA);
		}

		// La tecla recién apretada se ilumina; la de abajo del ratón se marca apenas.
		if (teclaApretada >= 0) marcar(graphics, teclaApretada, 0x50FFFFFF);
		int encima = teclaEn(raton, ratonY);
		if (encima >= 0 && encima != teclaApretada) marcar(graphics, encima, 0x28FFFFFF);
		pose.popPose();
	}

	private void marcar(GuiGraphics graphics, int tecla, int color) {
		int x = COLUMNAS[tecla % 3] - TECLA / 2, y = FILAS[tecla / 3] - TECLA / 2;
		graphics.fill(x, y, x + TECLA, y + TECLA, color);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
