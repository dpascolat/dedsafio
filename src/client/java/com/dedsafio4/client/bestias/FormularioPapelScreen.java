package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * La solicitud de empleo del Creeper de Papel: tapa la pantalla hasta que la firmas. Después de firmar hay que esperar
 * 2 segundos (se va contando) y recién ahí se cierra. Con Escape no se puede salir.
 */
public class FormularioPapelScreen extends Screen {
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/creeper_papel.png");
	/** La hoja dentro de la textura (468 x 483): de (54, 4) a (418, 480). */
	private static final int TEX_ANCHO = 468, TEX_ALTO = 483, HOJA_X = 54, HOJA_Y = 4, HOJA_ANCHO = 364, HOJA_ALTO = 476;
	private static final long ESPERA = 2000;

	private long firmadoEn = -1;
	private Button firmar;

	public FormularioPapelScreen() {
		super(Component.literal("Solicitud de empleo"));
	}

	@Override
	protected void init() {
		int alto = alto();
		int y = (this.height - alto) / 2 + alto - 4;
		firmar = Button.builder(Component.literal("✍ Firmar"), b -> {
			firmadoEn = Util.getMillis();
			b.active = false;
		}).bounds(this.width / 2 - 50, Math.min(this.height - 24, y), 100, 20).build();
		if (firmadoEn >= 0) firmar.active = false;
		addRenderableWidget(firmar);
	}

	/** La hoja ocupa casi todo el alto de la pantalla. */
	private int alto() {
		return (int) (this.height * 0.86f);
	}

	/** El fondo oscuro de siempre y encima la hoja (el botón se dibuja después, arriba de la hoja). */
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float parcial) {
		super.renderBackground(g, mouseX, mouseY, parcial);
		int alto = alto(), ancho = alto * HOJA_ANCHO / HOJA_ALTO;
		g.blit(TEXTURA, (this.width - ancho) / 2, (this.height - alto) / 2 - 10, ancho, alto, HOJA_X, HOJA_Y, HOJA_ANCHO, HOJA_ALTO,
				TEX_ANCHO, TEX_ALTO);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
		super.render(g, mouseX, mouseY, parcial);
		int alto = alto(), y = (this.height - alto) / 2 - 10;
		if (firmadoEn >= 0) {
			long pasaron = Util.getMillis() - firmadoEn;
			String nombre = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getGameProfile().getName() : "";
			g.drawCenteredString(this.font, Component.literal("Firmado: " + nombre), this.width / 2, y + alto - 30, 0x1A3BB0);
			int quedan = (int) Math.ceil((ESPERA - pasaron) / 1000.0);
			if (quedan > 0) {
				g.drawCenteredString(this.font, Component.literal("Procesando tu solicitud... " + quedan), this.width / 2, y + alto - 18, 0x555555);
			} else {
				onClose();
			}
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
}
