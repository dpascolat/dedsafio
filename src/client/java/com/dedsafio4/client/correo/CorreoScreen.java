package com.dedsafio4.client.correo;

import com.dedsafio4.client.cajero.BotonImagen;
import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.correo.Correo;
import com.dedsafio4.correo.CorreoMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Un mensaje del Buzón: "<< ATRÁS" arriba a la izquierda, el título "Mensaje", arriba el texto (con la rueda del mouse
 * si es largo) o los campos "Para:" y "Mensaje:" si se está escribiendo, la grilla de 9×3 para los objetos, "ENVIAR" (si
 * se está escribiendo) y el inventario abajo. Un mensaje recibido desaparece solo cuando se cierra sin objetos adentro.
 */
public class CorreoScreen extends AbstractContainerScreen<CorreoMenu> {
	private static final float CHICO = 2f / 3f, MEDIO = 0.8f;
	/** Cuántos renglones del mensaje entran arriba de la grilla, y a qué distancia van. */
	private static final int RENGLONES = 3, ALTO_RENGLON = 7;
	private EditBox para, texto;
	private String error = "";
	private int bajado;

	public CorreoScreen(CorreoMenu menu, Inventory inventario, Component titulo) {
		super(menu, inventario, titulo);
		imageWidth = CorreoMenu.ANCHO;
		imageHeight = CorreoMenu.ALTO;
	}

	public void error(String error) {
		this.error = error;
	}

	@Override
	protected void init() {
		super.init();
		int l = leftPos, t = topPos;
		addRenderableWidget(new BotonImagen(l + (menu.leyendo() ? 8 : 4), t + 4, BotonImagen.Imagen.de("correo/boton_atras", 42, 16),
				BotonImagen.Imagen.de("correo/boton_atras_encima", 42, 16), () -> {
			minecraft.player.closeContainer();
			minecraft.setScreen(new BuzonScreen());
		}));
		if (!menu.leyendo()) {
			para = campo(l + 30, t + 31, 16, "nombre del jugador", "Para");
			texto = campo(l + 30, t + 53, Correo.MAX_TEXTO, "Escribe tu mensaje...", "Mensaje");
			addRenderableWidget(new BotonImagen(l + 126, t + 123, BotonImagen.Imagen.de("correo/boton_enviar", 37, 10), null, () -> {
				error = "";
				ClientPlayNetworking.send(new Correo.EnviarPayload(para.getValue(), texto.getValue()));
			}));
			setInitialFocus(para);
		}
	}

	private EditBox campo(int x, int y, int maximo, String ayuda, String nombre) {
		EditBox caja = new EditBox(font, x, y, 118, 10, Component.literal(nombre));
		caja.setBordered(false);
		caja.setMaxLength(maximo);
		caja.setHint(Component.literal(ayuda).withColor(0xFF5A6070));
		return addRenderableWidget(caja);
	}

	private void chico(GuiGraphics g, Component texto, float x, float y, float escala, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(escala, escala, 1);
		g.drawString(font, texto, 0, 0, color, false);
		g.pose().popPose();
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		int l = leftPos, t = topPos;
		EstiloCajero.pieza(g, menu.leyendo() ? "correo/marco_leer" : "correo/marco_escribir", l, t, imageWidth, imageHeight);
		if (!menu.leyendo()) {
			EstiloCajero.pieza(g, "correo/campo", l + 26, t + 28, 125, 13);
			EstiloCajero.pieza(g, "correo/campo", l + 26, t + 50, 125, 13);
		}
		// La grilla del mensaje: 9 columnas con los 7 colores del diseño, las casillas pegadas para que entren en el marco.
		for (int fila = 0; fila < CorreoMenu.FILAS; fila++) {
			for (int col = 0; col < CorreoMenu.COLUMNAS; col++) {
				EstiloCajero.pieza(g, new BotonImagen.Imagen("correo/casilla_cuadricula_" + col % 7, 18, 18, 19, 19),
						l + CorreoMenu.GRILLA_X + col * CorreoMenu.CELDA_W, t + CorreoMenu.GRILLA_Y + fila * CorreoMenu.CELDA_H);
			}
		}
		EstiloCajero.casillasInventario(g, l + CorreoMenu.INVENTARIO_X - 1, t + CorreoMenu.INVENTARIO_Y - 1, CorreoMenu.BARRA);
	}

	/** Los renglones del mensaje abierto, ya cortados al ancho de la pantalla. */
	private List<FormattedCharSequence> renglones(Correo.CartaVista c) {
		return font.split(Component.literal(c.texto()), (int) (130 / CHICO));
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		int medio = imageWidth / 2;
		g.drawString(font, title, medio - font.width(title) / 2, menu.leyendo() ? 9 : 15, 0xFFFFFFFF, false);
		if (menu.leyendo()) {
			Correo.CartaVista c = CorreoCliente.carta(menu.carta());
			if (c == null) return;
			Component de = Component.literal("De: " + c.autor());
			if (!c.asunto().isEmpty()) de = de.copy().append(Component.literal(" · " + c.asunto()).withColor(0xFF55FFFF));
			chico(g, de, 24.67f, 34.67f, CHICO, 0xFFFFFFFF);
			List<FormattedCharSequence> lineas = renglones(c);
			bajado = Math.max(0, Math.min(bajado, lineas.size() - RENGLONES));
			for (int i = 0; i < RENGLONES && bajado + i < lineas.size(); i++) {
				g.pose().pushPose();
				g.pose().translate(24.67f, 44.67f + i * ALTO_RENGLON, 0);
				g.pose().scale(CHICO, CHICO, 1);
				g.drawString(font, lineas.get(bajado + i), 0, 0, 0xFFD8DCE4, false);
				g.pose().popPose();
			}
			if (lineas.size() > RENGLONES) chico(g, Component.literal((bajado > 0 ? "▲" : " ") + (bajado + RENGLONES < lineas.size() ? "▼" : " ")),
					150, 44.67f, CHICO, 0xFFAAAAAA);
		} else {
			chico(g, Component.literal("Para:"), 24.67f, 20.67f, MEDIO, 0xFFFFFFFF);
			chico(g, Component.literal("Mensaje:"), 24.67f, 42.33f, MEDIO, 0xFFFFFFFF);
			if (!error.isEmpty()) chico(g, Component.literal(font.plainSubstrByWidth(error, 150)), 14, 124.67f, CHICO, 0xFFFF6060);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (menu.leyendo() && dy != 0) {
			bajado = Math.max(0, bajado - (int) Math.signum(dy));
			return true;
		}
		return super.mouseScrolled(mx, my, dx, dy);
	}

	/** Escribiendo, las teclas son para el texto (la E y los números no cierran ni mueven nada). */
	@Override
	public boolean keyPressed(int tecla, int scan, int mods) {
		if (tecla != GLFW.GLFW_KEY_ESCAPE) {
			if (para != null && para.isFocused()) return para.keyPressed(tecla, scan, mods) || para.canConsumeInput();
			if (texto != null && texto.isFocused()) return texto.keyPressed(tecla, scan, mods) || texto.canConsumeInput();
		}
		return super.keyPressed(tecla, scan, mods);
	}
}
