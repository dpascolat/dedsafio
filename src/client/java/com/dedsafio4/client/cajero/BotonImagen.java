package com.dedsafio4.client.cajero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Un botón hecho con imágenes (textures/gui/<nombre>.png): una normal, otra con el mouse encima (que puede ser un poco
 * más grande) y, si está apagado, otra o la misma más oscura. El texto va encima de la imagen, si tiene.
 */
public class BotonImagen extends AbstractButton {
	/** Una imagen: su nombre, cuánto mide en pantalla y cuánto mide el archivo. */
	public record Imagen(String nombre, int ancho, int alto, int texAncho, int texAlto) {
		public static Imagen de(String nombre, int ancho, int alto) {
			return new Imagen(nombre, ancho, alto, ancho, alto);
		}
	}

	private final Imagen normal, encima, apagado;
	private final int encimaX, encimaY;
	private final float escalaTexto;
	private final Runnable accion;

	public BotonImagen(int x, int y, Imagen normal, Imagen encima, Imagen apagado, int encimaX, int encimaY, String texto, float escalaTexto, Runnable accion) {
		super(x, y, normal.ancho(), normal.alto(), Component.literal(texto));
		this.normal = normal;
		this.encima = encima;
		this.apagado = apagado;
		this.encimaX = encimaX;
		this.encimaY = encimaY;
		this.escalaTexto = escalaTexto;
		this.accion = accion;
	}

	public BotonImagen(int x, int y, Imagen normal, Imagen encima, Runnable accion) {
		this(x, y, normal, encima, null, 0, 0, "", 1, accion);
	}

	@Override
	public void onPress() {
		accion.run();
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		boolean arriba = active && isHovered() && encima != null;
		Imagen imagen = arriba ? encima : !active && apagado != null ? apagado : normal;
		if (!active && apagado == null) g.setColor(0.55f, 0.55f, 0.55f, 1);
		else if (isHovered() && active && encima == null) g.setColor(1.25f, 1.25f, 1.25f, 1);
		EstiloCajero.pieza(g, imagen, getX() + (arriba ? encimaX : 0), getY() + (arriba ? encimaY : 0));
		g.setColor(1, 1, 1, 1);
		if (getMessage().getString().isEmpty()) return;
		var font = Minecraft.getInstance().font;
		g.pose().pushPose();
		g.pose().translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0);
		g.pose().scale(escalaTexto, escalaTexto, 1);
		g.drawString(font, getMessage(), -font.width(getMessage()) / 2, -4, 0xFFFFFFFF, false);
		g.pose().popPose();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput salida) {
		defaultButtonNarrationText(salida);
	}
}
