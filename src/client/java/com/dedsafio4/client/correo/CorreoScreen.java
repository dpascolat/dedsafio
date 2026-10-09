package com.dedsafio4.client.correo;

import com.dedsafio4.client.cajero.BotonCajero;
import com.dedsafio4.client.cajero.EstiloCajero;
import com.dedsafio4.correo.Correo;
import com.dedsafio4.correo.CorreoMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Un mensaje del Buzón, como en las imágenes: "<< ATRÁS" arriba a la izquierda, el título "Mensaje", arriba el texto
 * (o los campos "Para:" y "Mensaje:" si se está escribiendo), la grilla de 6×3 con bordes de arcoíris para los objetos,
 * "ENVIAR" (o "BORRAR" si es uno recibido) y el inventario abajo.
 */
public class CorreoScreen extends AbstractContainerScreen<CorreoMenu> {
	private EditBox para;
	private MultiLineEditBox texto;
	private String error = "";

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
		addRenderableWidget(new BotonCajero(l + 8, t + 8, 46, 14, "<< ATRÁS", () -> {
			minecraft.player.closeContainer();
			minecraft.setScreen(new BuzonScreen());
		}));
		int yBoton = t + CorreoMenu.GRILLA_Y + CorreoMenu.FILAS * CorreoMenu.CELDA_H + 3;
		int xBoton = l + CorreoMenu.GRILLA_X + CorreoMenu.COLUMNAS * CorreoMenu.CELDA_W - 50;
		if (menu.leyendo()) {
			addRenderableWidget(new BotonCajero(xBoton, yBoton, 50, 13, "BORRAR", () -> {
				// Primero se cierra (así el servidor guarda lo que quedó en la grilla y lo devuelve al borrar).
				minecraft.player.closeContainer();
				ClientPlayNetworking.send(new Correo.AccionPayload("borrar", menu.carta()));
				minecraft.setScreen(new BuzonScreen());
			}));
		} else {
			para = new EditBox(font, l + 44, t + 27, 132, 12, Component.literal("Para"));
			para.setMaxLength(16);
			para.setHint(Component.literal("nombre del jugador").withColor(0xFF5A6070));
			addRenderableWidget(para);
			texto = new MultiLineEditBox(font, l + 14, t + 54, 162, 42, Component.literal("Escribe tu mensaje...").withColor(0xFF5A6070),
					Component.literal("Mensaje"));
			texto.setCharacterLimit(Correo.MAX_TEXTO);
			addRenderableWidget(texto);
			addRenderableWidget(new BotonCajero(xBoton, yBoton, 50, 13, "ENVIAR", () -> {
				error = "";
				ClientPlayNetworking.send(new Correo.EnviarPayload(para.getValue(), texto.getValue()));
			}));
			setInitialFocus(para);
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
		int l = leftPos, t = topPos;
		EstiloCajero.marco(g, font, l, t, imageWidth, imageHeight, null);
		// La grilla del mensaje: cada columna con su color del arcoíris.
		for (int fila = 0; fila < CorreoMenu.FILAS; fila++) {
			for (int col = 0; col < CorreoMenu.COLUMNAS; col++) {
				int x = l + CorreoMenu.GRILLA_X + col * CorreoMenu.CELDA_W, y = t + CorreoMenu.GRILLA_Y + fila * CorreoMenu.CELDA_H;
				int color = EstiloCajero.arcoiris(0.16f + col / (float) CorreoMenu.COLUMNAS * 0.85f);
				g.fill(x, y, x + CorreoMenu.CELDA_W + 1, y + CorreoMenu.CELDA_H + 1, color);
				g.fill(x + 1, y + 1, x + CorreoMenu.CELDA_W, y + CorreoMenu.CELDA_H, 0xFF0B0D12);
			}
		}
		// El inventario.
		for (int fila = 0; fila < 4; fila++) {
			for (int col = 0; col < 9; col++) {
				int x = l + CorreoMenu.INVENTARIO_X - 1 + col * 18;
				int y = t + CorreoMenu.INVENTARIO_Y - 1 + (fila < 3 ? fila * 18 : 58);
				g.fill(x, y, x + 18, y + 18, 0xFF2A2E38);
				g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF151821);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, (imageWidth - font.width(title)) / 2 + 12, 11, 0xFFFFFFFF, false);
		if (menu.leyendo()) {
			Correo.CartaVista c = CorreoCliente.carta(menu.carta());
			if (c == null) return;
			g.drawString(font, "De: " + c.autor(), 14, 28, 0xFFFFFFFF, false);
			int y = 40;
			if (!c.asunto().isEmpty()) {
				g.drawString(font, c.asunto(), 14, 38, 0xFF55FFFF, false);
				y = 50;
			}
			List<FormattedCharSequence> lineas = font.split(Component.literal(c.texto()), 162);
			for (FormattedCharSequence linea : lineas) {
				if (y > CorreoMenu.GRILLA_Y - 10) break;
				g.drawString(font, linea, 14, y, 0xFFD8DCE4, false);
				y += 9;
			}
		} else {
			g.drawString(font, "Para:", 14, 29, 0xFFC0C6D0, false);
			g.drawString(font, "Mensaje:", 14, 44, 0xFFC0C6D0, false);
			if (!error.isEmpty()) {
				int y = CorreoMenu.GRILLA_Y + CorreoMenu.FILAS * CorreoMenu.CELDA_H + 5;
				g.drawString(font, font.plainSubstrByWidth(error, 105), 14, y, 0xFFFF6060, false);
			}
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		renderTooltip(g, mouseX, mouseY);
	}

	/** Escribiendo, las teclas son para el texto (la E y los números no cierran ni mueven nada). */
	@Override
	public boolean keyPressed(int tecla, int scan, int mods) {
		if (tecla != GLFW.GLFW_KEY_ESCAPE) {
			if (para != null && para.isFocused()) return para.keyPressed(tecla, scan, mods) || para.canConsumeInput();
			if (texto != null && texto.isFocused()) return texto.keyPressed(tecla, scan, mods) || true;
		}
		return super.keyPressed(tecla, scan, mods);
	}
}
