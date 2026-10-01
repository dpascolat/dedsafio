package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.hermandad.ManuscritoAccionPayload;
import com.dedsafio4.hermandad.ManuscritoDatos;
import com.dedsafio4.items.ManuscritoHermandadItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Pergamino del Manuscrito de Hermandad: arriba el nombre (editable), abajo los inscritos
 * (solo cambian con el botón). "Crear Hermandad" se habilita con 3 inscritos y sella el
 * Manuscrito: desde ahí solo se puede leer.
 */
public class ManuscritoScreen extends Screen {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/manuscrito.png");
	private static final int ANCHO = 148, ALTO = 182;
	private static final int ANCHO_BOTON = 88, ALTO_BOTON = 20, SEPARACION = 6;
	private static final int NEGRO = 0x000000;

	private final InteractionHand mano;
	private String nombre;
	private int ticks;
	private Button inscribirse, crear;

	public ManuscritoScreen(InteractionHand mano, ItemStack manuscrito) {
		super(Component.literal("Manuscrito de Hermandad"));
		this.mano = mano;
		this.nombre = ManuscritoDatos.nombre(manuscrito);
	}

	private ItemStack manuscrito() {
		return minecraft.player.getItemInHand(mano);
	}

	private int x() { return (width - ANCHO) / 2; }
	private int y() { return (height - ALTO - SEPARACION - ALTO_BOTON) / 2; }

	@Override
	protected void init() {
		int yBotones = y() + ALTO + SEPARACION;
		int xBotones = (width - ANCHO_BOTON * 2 - SEPARACION) / 2;
		inscribirse = addRenderableWidget(Button.builder(Component.literal("Inscribirse"),
				b -> enviar(ManuscritoAccionPayload.INSCRIBIRSE))
				.bounds(xBotones, yBotones, ANCHO_BOTON, ALTO_BOTON).build());
		crear = addRenderableWidget(Button.builder(Component.literal("Crear Hermandad"),
				b -> enviar(ManuscritoAccionPayload.CREAR))
				.bounds(xBotones + ANCHO_BOTON + SEPARACION, yBotones, ANCHO_BOTON, ALTO_BOTON).build());
		actualizarBotones();
	}

	private void enviar(int accion) {
		setFocused(null); // que la barra espaciadora escriba en el nombre y no vuelva a apretar el botón
		ClientPlayNetworking.send(new ManuscritoAccionPayload(accion, nombre, mano == InteractionHand.MAIN_HAND));
	}

	private boolean sellado() {
		return ManuscritoDatos.sellado(manuscrito());
	}

	private void actualizarBotones() {
		ItemStack stack = manuscrito();
		inscribirse.visible = crear.visible = !ManuscritoDatos.sellado(stack);
		List<ManuscritoDatos.Inscrito> inscritos = ManuscritoDatos.inscritos(stack);
		boolean yoInscrito = ManuscritoDatos.estaInscrito(stack, minecraft.player.getUUID());
		inscribirse.active = !yoInscrito && inscritos.size() < ManuscritoDatos.MAXIMO_INSCRITOS;
		crear.active = inscritos.size() >= ManuscritoHermandadItem.minimoInscritos(stack) && yoInscrito
				&& !ManuscritoDatos.limpiarNombre(nombre).isEmpty();
	}

	@Override
	public void tick() {
		ticks++;
		// Si el manuscrito ya no está en la mano (se creó la Hermandad o se soltó), se cierra.
		if (!ManuscritoHermandadItem.es(manuscrito())) {
			minecraft.setScreen(null);
			return;
		}
		actualizarBotones();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int x = x(), y = y();
		graphics.blit(TEXTURA, x, y, 0, 0, ANCHO, ALTO, 256, 256);

		int xTexto = x + 20;
		String cursor = !sellado() && (ticks / 6) % 2 == 0 ? "_" : "";
		graphics.drawString(font, nombre + cursor, xTexto, y + 22, NEGRO, false);
		graphics.drawString(font, "Inscritos:", xTexto, y + 40, NEGRO, false);

		int yInscrito = y + 54;
		for (ManuscritoDatos.Inscrito inscrito : ManuscritoDatos.inscritos(manuscrito())) {
			graphics.drawString(font, inscrito.nombre(), xTexto, yInscrito, NEGRO, false);
			yInscrito += 11;
		}
	}

	@Override
	public boolean charTyped(char caracter, int modificadores) {
		if (!sellado() && StringUtil.isAllowedChatCharacter(caracter) && nombre.length() < ManuscritoDatos.LARGO_MAXIMO_NOMBRE) {
			nombre += caracter;
			return true;
		}
		return super.charTyped(caracter, modificadores);
	}

	@Override
	public boolean keyPressed(int tecla, int scanCode, int modificadores) {
		if (tecla == GLFW.GLFW_KEY_BACKSPACE && !sellado() && !nombre.isEmpty()) {
			nombre = nombre.substring(0, nombre.length() - 1);
			return true;
		}
		return super.keyPressed(tecla, scanCode, modificadores);
	}

	@Override
	public void removed() {
		// Al cerrar, se guarda el nombre que quedó escrito.
		if (ManuscritoHermandadItem.es(manuscrito()) && !sellado()) enviar(ManuscritoAccionPayload.GUARDAR_NOMBRE);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
