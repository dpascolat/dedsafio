package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.baneos.Baneos;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La pantalla del Bloque de Baneados: a la izquierda la lista de baneados (cara y nombre), a la derecha la cuchara o
 * el tenedor que tenés en la mano y el botón para desbanear al elegido.
 */
public class BaneadosScreen extends Screen {
	private static final ResourceLocation FONDO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/baneados.png");
	private static final int W = 250, H = 208;
	/** La lista: dónde está, alto de cada fila y cuántas entran. */
	private static final int LISTA_X = 15, LISTA_Y = 15, FILA = 17, ANCHO_FILA = 102, FILAS = 10;
	/** El casillero del cubierto y el botón (en el panel derecho). */
	private static final int CASILLERO_X = 175, CASILLERO_Y = 66, BOTON_X = 146, BOTON_Y = 108, BOTON_W = 80, BOTON_H = 20;

	/** Las skins que ya se buscaron (se comparten entre aperturas). */
	private static final Map<UUID, PlayerSkin> SKINS = new HashMap<>();

	private final BlockPos pos;
	private final List<UUID> ids = new ArrayList<>();
	private final List<String> nombres = new ArrayList<>();
	private int elegido = -1;
	private int desplazamiento = 0;
	private int x0, y0;

	public BaneadosScreen(BlockPos pos) {
		super(Component.literal("Baneados"));
		this.pos = pos;
	}

	public BlockPos pos() {
		return pos;
	}

	/** Llega (o se actualiza) la lista del servidor. */
	public void ponerLista(List<String> nuevosIds, List<String> nuevosNombres) {
		UUID antes = elegido >= 0 && elegido < ids.size() ? ids.get(elegido) : null;
		ids.clear();
		nombres.clear();
		for (int i = 0; i < nuevosIds.size(); i++) {
			UUID uuid = UUID.fromString(nuevosIds.get(i));
			ids.add(uuid);
			nombres.add(nuevosNombres.get(i));
			buscarSkin(uuid);
		}
		elegido = antes == null ? -1 : ids.indexOf(antes);
		desplazamiento = Math.max(0, Math.min(desplazamiento, ids.size() - FILAS));
	}

	private static void buscarSkin(UUID uuid) {
		if (SKINS.containsKey(uuid)) return;
		SKINS.put(uuid, DefaultPlayerSkin.get(uuid));
		SkullBlockEntity.fetchGameProfile(uuid).thenAccept(perfil -> perfil.ifPresent(p ->
				minecraft().getSkinManager().getOrLoad(p).thenAccept(skin -> minecraft().execute(() -> SKINS.put(uuid, skin)))));
	}

	private static net.minecraft.client.Minecraft minecraft() {
		return net.minecraft.client.Minecraft.getInstance();
	}

	@Override
	protected void init() {
		x0 = (width - W) / 2;
		y0 = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private ItemStack cubierto() {
		if (minecraft.player == null) return ItemStack.EMPTY;
		if (Baneos.esCubierto(minecraft.player.getMainHandItem())) return minecraft.player.getMainHandItem();
		if (Baneos.esCubierto(minecraft.player.getOffhandItem())) return minecraft.player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		super.render(g, mx, my, parcial);
		g.blit(FONDO, x0, y0, 0, 0, W, H, 256, 256);

		// La lista.
		if (ids.isEmpty()) {
			g.drawCenteredString(font, "No hay nadie", x0 + LISTA_X + ANCHO_FILA / 2, y0 + 90, 0xFFF2E3C8);
			g.drawCenteredString(font, "baneado", x0 + LISTA_X + ANCHO_FILA / 2, y0 + 101, 0xFFF2E3C8);
		}
		int sobre = filaEn(mx, my);
		for (int f = 0; f < FILAS && desplazamiento + f < ids.size(); f++) {
			int i = desplazamiento + f;
			int x = x0 + LISTA_X, y = y0 + LISTA_Y + f * FILA;
			int fondo = i == elegido ? 0xFF3F9C4B : i == sobre ? 0xFFC9AE84 : 0xFFB4966C;
			g.fill(x, y, x + ANCHO_FILA, y + FILA - 1, fondo);
			g.fill(x, y + FILA - 2, x + ANCHO_FILA, y + FILA - 1, i == elegido ? 0xFF2C6E35 : 0xFF8C7250);
			PlayerFaceRenderer.draw(g, SKINS.getOrDefault(ids.get(i), DefaultPlayerSkin.get(ids.get(i))), x + 2, y + 2, 12);
			g.drawString(font, nombres.get(i), x + 18, y + 4, 0xFFFFFFFF, true);
		}
		// La barrita para ver que hay más.
		if (ids.size() > FILAS) {
			int alto = FILAS * FILA - 1, x = x0 + LISTA_X + ANCHO_FILA + 2;
			g.fill(x, y0 + LISTA_Y, x + 5, y0 + LISTA_Y + alto, 0xFF4A3420);
			int barra = Math.max(10, alto * FILAS / ids.size());
			int arriba = (alto - barra) * desplazamiento / (ids.size() - FILAS);
			g.fill(x, y0 + LISTA_Y + arriba, x + 5, y0 + LISTA_Y + arriba + barra, 0xFFD8C3A0);
		}

		// El cubierto (el de la mano) en el casillero.
		ItemStack item = cubierto();
		if (!item.isEmpty()) g.renderItem(item, x0 + CASILLERO_X + 3, y0 + CASILLERO_Y + 3);
		if (mx >= x0 + CASILLERO_X && mx < x0 + CASILLERO_X + 22 && my >= y0 + CASILLERO_Y && my < y0 + CASILLERO_Y + 22) {
			if (!item.isEmpty()) g.renderTooltip(font, item, mx, my);
			else g.renderTooltip(font, Component.literal("Tené una cuchara o un tenedor en la mano"), mx, my);
		}

		// El botón.
		boolean puede = elegido >= 0 && !item.isEmpty();
		boolean encima = enBoton(mx, my);
		int bx = x0 + BOTON_X, by = y0 + BOTON_Y;
		g.fill(bx, by, bx + BOTON_W, by + BOTON_H, 0xFF3A2816);
		g.fill(bx + 1, by + 1, bx + BOTON_W - 1, by + BOTON_H - 1, puede ? (encima ? 0xFF4E9E55 : 0xFF3F7F45) : 0xFF6B5237);
		String texto = elegido < 0 ? "Elegí a alguien" : item.isEmpty() ? "Falta cubierto" : "Revivir";
		g.drawCenteredString(font, texto, bx + BOTON_W / 2, by + 6, puede ? 0xFFFFFFFF : 0xFFC9B79A);
		if (elegido >= 0) {
			g.drawCenteredString(font, nombres.get(elegido), bx + BOTON_W / 2, by + BOTON_H + 8, 0xFF4A3420);
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
		renderTransparentBackground(g);
	}

	private int filaEn(double mx, double my) {
		int x = x0 + LISTA_X, y = y0 + LISTA_Y;
		if (mx < x || mx >= x + ANCHO_FILA || my < y || my >= y + FILAS * FILA) return -1;
		int i = desplazamiento + (int) ((my - y) / FILA);
		return i < ids.size() ? i : -1;
	}

	private boolean enBoton(double mx, double my) {
		return mx >= x0 + BOTON_X && mx < x0 + BOTON_X + BOTON_W && my >= y0 + BOTON_Y && my < y0 + BOTON_Y + BOTON_H;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		int fila = filaEn(mx, my);
		if (fila >= 0) {
			elegido = fila;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}
		if (enBoton(mx, my) && elegido >= 0 && !cubierto().isEmpty()) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			ClientPlayNetworking.send(new Baneos.DesbanearPayload(pos, ids.get(elegido).toString()));
			return true;
		}
		return super.mouseClicked(mx, my, boton);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (ids.size() > FILAS) {
			desplazamiento = Math.max(0, Math.min(ids.size() - FILAS, desplazamiento - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(mx, my, dx, dy);
	}

	/** Se llama al arrancar el cliente: abre (o actualiza) la pantalla cuando llega la lista. */
	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Baneos.ListaPayload.TYPE, (payload, contexto) -> contexto.client().execute(() -> {
			var mc = contexto.client();
			if (mc.screen instanceof BaneadosScreen pantalla && pantalla.pos().equals(payload.pos())) {
				pantalla.ponerLista(payload.ids(), payload.nombres());
			} else {
				BaneadosScreen pantalla = new BaneadosScreen(payload.pos());
				mc.setScreen(pantalla);
				pantalla.ponerLista(payload.ids(), payload.nombres());
			}
		}));
	}
}
