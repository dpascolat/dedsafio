package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.CreeperAmarilloEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * La pantalla de "te echaron del servidor" de mentira que deja el Creeper Amarillo al explotar: igual a la de
 * Minecraft (el fondo del menú, "Conexión perdida", "Has sido expulsado por un operador" y el botón "Volver a la lista
 * de servidores"), pero el botón solo la cierra y sigues jugando. Con Esc no se cierra, como la de verdad.
 */
public class FalsoKickScreen extends Screen {
	private final LinearLayout layout = LinearLayout.vertical();

	public FalsoKickScreen() {
		super(Component.translatable("disconnect.lost"));
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(CreeperAmarilloEntity.FalsoKickPayload.TYPE, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new FalsoKickScreen())));
	}

	@Override
	protected void init() {
		layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
		layout.addChild(new StringWidget(title, font));
		layout.addChild(new MultiLineTextWidget(Component.translatable("multiplayer.disconnect.kicked"), font)
				.setMaxWidth(width - 50).setCentered(true));
		layout.defaultCellSetting().padding(2);
		layout.addChild(Button.builder(Component.translatable("gui.toMenu"), b -> onClose()).width(200).build());
		layout.arrangeElements();
		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	@Override
	protected void repositionElements() {
		FrameLayout.centerInRectangle(layout, getRectangle());
	}

	/** El fondo del menú (el panorama), como cuando de verdad te echan, aunque estés en el mundo. */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		renderPanorama(g, parcial);
		renderBlurredBackground(parcial);
		renderMenuBackground(g);
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
