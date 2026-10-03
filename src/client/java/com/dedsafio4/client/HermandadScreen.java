package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.hermandad.HermandadAccionPayload;
import com.dedsafio4.hermandad.HermandadChatPayload;
import com.dedsafio4.hermandad.HermandadInfoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

/**
 * Interfaz de la Hermandad (tecla H). Pestañas: Inicio (libro), ? (cartel), Ubicaciones (brújula),
 * Estandartes (estandarte) y Gestión (cabeza del jugador). El catalejo de la derecha muestra la
 * lista de todas las Hermandades del servidor.
 * Por ahora funcionan: la lista de miembros, invitar, expulsar, el chat de la Hermandad (pestaña
 * Inicio) y la Colección de Estandartes (el activo es la capa de todos los miembros).
 */
public class HermandadScreen extends Screen {
	private static final ResourceLocation ICONO_DEDITA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/dedita.png");
	private static final Component PROXIMAMENTE = Component.literal("Próximamente");

	private static final int ANCHO = 388, ALTO = 232;
	private static final int TAB = 22, FILA = 15;

	private static final int INICIO = 0, TABLON = 1, BRUJULA = 2, ESTANDARTES = 3, GESTION = 4, LISTA = 5;
	private static final String[] NOMBRES_TABS = {"Inicio", "Tablones", "Ubicaciones", "Estandartes", "Gestión"};
	private static final int MAXIMO_UBICACIONES = 16;

	// Colores
	private static final int METAL = 0xFF9A9AA6, METAL_CLARO = 0xFFC8C8D2, METAL_OSCURO = 0xFF5E5E68, BORDE = 0xFF141418;
	private static final int FONDO = 0xFF1E1E23, PANEL = 0xFF28282E, PANEL_BORDE = 0xFF4A4A54;
	private static final int TEXTO = 0xFFE6E6E6, TEXTO_GRIS = 0xFFA8A8B0, CELESTE = 0xFF7FB2FF;
	private static final int ORO = 0xFFFFAA00, AZUL = 0xFF55AAFF;

	private static int pestana = INICIO;

	private HermandadInfoPayload info;
	private ItemStack[] iconosTabs;
	private EditBox invitarNombre;
	private EditBox chatEntrada;
	/** Cuántas líneas se subió el chat con la ruedita (0 = lo último). */
	private int chatScroll;
	/** Cuántas filas se bajó la lista de Hermandades con la ruedita. */
	private int listaScroll;
	/** Desde qué anuncio se muestra el Tablón (la ruedita lo mueve). */
	private int tablonScroll;
	private EditBox anuncioEntrada;
	/** El estandarte elegido en la colección (-1 si ninguno). */
	private int estandarteElegido = -1;

	public HermandadScreen(HermandadInfoPayload info) {
		super(Component.literal("Hermandad"));
		this.info = info;
	}

	/** Llegaron datos nuevos del servidor (alguien se unió, fue expulsado, etc.). */
	public void actualizar(HermandadInfoPayload nueva) {
		String escrito = invitarNombre != null ? invitarNombre.getValue() : "";
		String chateando = chatEntrada != null ? chatEntrada.getValue() : "";
		boolean conFoco = chatEntrada != null && getFocused() == chatEntrada;
		this.info = nueva;
		rebuildWidgets();
		if (invitarNombre != null) invitarNombre.setValue(escrito);
		if (chatEntrada != null) {
			chatEntrada.setValue(chateando);
			if (conFoco) setFocused(chatEntrada);
		}
	}

	private int x() { return (width - ANCHO) / 2; }
	private int y() { return (height - ALTO) / 2; }

	private boolean soyMaestro() {
		return info.maestro().equals(minecraft.player.getUUID());
	}

	/** El Maestro y los Líderes publican en el Tablón. */
	private boolean puedoPublicar() {
		return soyMaestro() || info.miembros().stream()
				.anyMatch(m -> m.uuid().equals(minecraft.player.getUUID()) && m.lider());
	}

	@Override
	protected void init() {
		ItemStack cabeza = new ItemStack(Items.PLAYER_HEAD);
		cabeza.set(DataComponents.PROFILE, new ResolvableProfile(minecraft.player.getGameProfile()));
		iconosTabs = new ItemStack[]{new ItemStack(Items.BOOK), new ItemStack(Items.OAK_SIGN),
				new ItemStack(Items.COMPASS), new ItemStack(Items.WHITE_BANNER), cabeza};
		invitarNombre = null;
		chatEntrada = null;
		anuncioEntrada = null;

		int x = x(), y = y();
		switch (pestana) {
			case INICIO -> initInicio(x, y);
			case TABLON -> initTablon(x, y);
			case GESTION -> initGestion(x, y);
			case ESTANDARTES -> initEstandartes(x, y);
			case BRUJULA -> initUbicaciones(x, y);
			default -> {}
		}
	}

	/** El renglón para escribir en el chat de la Hermandad; Enter lo manda. */
	private void initInicio(int x, int y) {
		chatEntrada = addRenderableWidget(new EditBox(font, x + 18, y + 211, 216, 10, Component.literal("Chat de la Hermandad")) {
			@Override
			public boolean keyPressed(int tecla, int scanCode, int modificadores) {
				if (tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_KP_ENTER) {
					String mensaje = getValue().strip();
					if (!mensaje.isEmpty()) {
						ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.CHAT, mensaje));
						setValue("");
						chatScroll = 0;
					}
					return true;
				}
				return super.keyPressed(tecla, scanCode, modificadores);
			}
		});
		chatEntrada.setBordered(false);
		chatEntrada.setMaxLength(HermandadAccionPayload.LARGO_CHAT);
		chatEntrada.setTextColor(0xFFE6E6E6);
		chatEntrada.setHint(Component.literal("Escribe a tu Hermandad...").withColor(0xFF707078));
	}

	private void initUbicaciones(int x, int y) {
		int centro = x + ANCHO / 2;
		inactivo(Button.builder(Component.literal("⌖"), b -> {}).bounds(centro - 88, y + 202, 16, 16).build());
		EditBox nombre = addRenderableWidget(new EditBox(font, centro - 68, y + 202, 108, 16, Component.literal("Ubicación")));
		nombre.setHint(Component.literal("Nombre ubicación...").withColor(0xFF707078));
		nombre.setEditable(false);
		inactivo(Button.builder(Component.literal("Guardar"), b -> {}).bounds(centro + 44, y + 201, 70, 18).build());
	}

	private void initGestion(int x, int y) {
		invitarNombre = addRenderableWidget(new EditBox(font, x + 16, y + 111, 122, 16, Component.literal("Invitar jugador")));
		invitarNombre.setMaxLength(16);
		invitarNombre.setHint(Component.literal("Nombre del jugador...").withColor(0xFF707078));
		Button invitar = addRenderableWidget(Button.builder(Component.literal("Invitar"), b -> {
			String nombre = invitarNombre.getValue().strip();
			if (nombre.isEmpty()) return;
			ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.INVITAR, nombre));
			invitarNombre.setValue("");
		}).bounds(x + 142, y + 110, 60, 18).build());
		invitar.active = soyMaestro();
		invitarNombre.setEditable(soyMaestro());

		EditBox motd = addRenderableWidget(new EditBox(font, x + 16, y + 147, 92, 16, Component.literal("MOTD")));
		motd.setHint(Component.literal("Mensaje...").withColor(0xFF707078));
		motd.setEditable(false);
		inactivo(Button.builder(Component.literal("Fijar"), b -> {}).bounds(x + 112, y + 146, 44, 18).build());
		inactivo(Button.builder(Component.literal("Limpiar"), b -> {}).bounds(x + 158, y + 146, 44, 18).build());
		inactivo(Button.builder(Component.literal("Solicitudes de unión: Cerradas"), b -> {}).bounds(x + 20, y + 170, 180, 18).build());
		inactivo(Button.builder(Component.literal("Disolver Hermandad"), b -> {}).bounds(x + 44, y + 200, 132, 18).build());

		// Botones ^ y x de cada miembro (solo los ve el Maestro, no en su propia fila).
		if (!soyMaestro()) return;
		int derecha = x + ANCHO - 14;
		List<HermandadInfoPayload.Miembro> miembros = info.miembros();
		for (int i = 0; i < miembros.size(); i++) {
			HermandadInfoPayload.Miembro m = miembros.get(i);
			if (m.uuid().equals(info.maestro())) continue;
			int yFila = y + 48 + i * FILA + 2;
			Button rango = addRenderableWidget(Button.builder(Component.literal(m.lider() ? "v" : "^"), b ->
					ClientPlayNetworking.send(new HermandadAccionPayload(m.lider() ? HermandadAccionPayload.BAJAR_RANGO
							: HermandadAccionPayload.SUBIR_RANGO, m.uuid().toString())))
					.bounds(derecha - 30, yFila, 13, 11).build());
			rango.setTooltip(Tooltip.create(Component.literal(m.lider() ? "Bajar a Miembro a " + m.nombre()
					: "Subir de rango a " + m.nombre() + " (Líder)")));
			Button expulsar = addRenderableWidget(Button.builder(Component.literal("x"), b ->
					ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.EXPULSAR, m.uuid().toString())))
					.bounds(derecha - 15, yFila, 13, 11).build());
			expulsar.setTooltip(Tooltip.create(Component.literal("Expulsar a " + m.nombre())));
		}
	}

	private void initEstandartes(int x, int y) {
		int centro = x + ANCHO / 2;
		boolean maestro = soyMaestro();
		if (estandarteElegido >= HermandadesCliente.coleccion().size()) estandarteElegido = -1;
		boolean hayElegido = estandarteElegido >= 0;
		Tooltip soloMaestro = Tooltip.create(Component.literal("Solo el Maestro"));

		Button activar = addRenderableWidget(Button.builder(Component.literal("Activar"),
				b -> accionEstandarte(HermandadAccionPayload.ESTANDARTE_ACTIVAR)).bounds(centro - 95, y + 176, 58, 16).build());
		Button recuperar = addRenderableWidget(Button.builder(Component.literal("Recuperar"),
				b -> accionEstandarte(HermandadAccionPayload.ESTANDARTE_RECUPERAR)).bounds(centro - 29, y + 176, 58, 16).build());
		Button borrar = addRenderableWidget(Button.builder(Component.literal("Borrar"),
				b -> accionEstandarte(HermandadAccionPayload.ESTANDARTE_BORRAR)).bounds(centro + 37, y + 176, 58, 16).build());
		Button quitar = addRenderableWidget(Button.builder(Component.literal("Quitar Activo"),
				b -> ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.ESTANDARTE_QUITAR, "")))
				.bounds(x + 20, y + 202, 108, 18).build());
		Button guardar = addRenderableWidget(Button.builder(Component.literal("Guardar Mano"),
				b -> ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.ESTANDARTE_GUARDAR, "")))
				.bounds(x + ANCHO - 128, y + 202, 108, 18).build());

		activar.active = maestro && hayElegido;
		recuperar.active = maestro && hayElegido;
		borrar.active = maestro && hayElegido;
		quitar.active = maestro && HermandadesCliente.activo() >= 0;
		guardar.active = minecraft.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BannerItem;
		if (!maestro) {
			activar.setTooltip(soloMaestro);
			recuperar.setTooltip(soloMaestro);
			borrar.setTooltip(soloMaestro);
			quitar.setTooltip(soloMaestro);
		}
		if (!guardar.active) guardar.setTooltip(Tooltip.create(Component.literal("Ten un estandarte en la mano")));
	}

	private void accionEstandarte(int accion) {
		if (estandarteElegido < 0) return;
		ClientPlayNetworking.send(new HermandadAccionPayload(accion, Integer.toString(estandarteElegido)));
		if (accion != HermandadAccionPayload.ESTANDARTE_ACTIVAR) estandarteElegido = -1;
	}

	/** Llegó la colección nueva: se rearman los botones. */
	public void refrescarEstandartes() {
		if (pestana == ESTANDARTES) rebuildWidgets();
	}

	private static final int CELDA_ESTANDARTE = 24;

	private int x0Estandartes() { return x() + (ANCHO - CELDA_ESTANDARTE * 9) / 2; }
	private int y0Estandartes() { return y() + 68; }

	/** El lugar de la colección debajo del mouse, o -1. */
	private int lugarEstandarte(double mx, double my) {
		int x0 = x0Estandartes(), y0 = y0Estandartes();
		if (!dentro(mx, my, x0, y0, CELDA_ESTANDARTE * 9, CELDA_ESTANDARTE * 3)) return -1;
		return ((int) (my - y0) / CELDA_ESTANDARTE) * 9 + (int) (mx - x0) / CELDA_ESTANDARTE;
	}

	/** Botones de funciones que todavía no están definidas. */
	private void inactivo(Button boton) {
		boton.active = false;
		boton.setTooltip(Tooltip.create(PROXIMAMENTE));
		addRenderableWidget(boton);
	}

	// ---------------------------------------------------------------- Dibujo

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);   // fondo (abajo) + botones
		if (paletaAbierta) dibujarPaleta(g, mouseX, mouseY);
		else tooltipTabs(g, x(), y(), mouseX, mouseY);
		if (pestana == ESTANDARTES && !paletaAbierta) {
			int lugar = lugarEstandarte(mouseX, mouseY);
			if (lugar >= 0 && lugar < HermandadesCliente.coleccion().size()) {
				g.renderTooltip(font, HermandadesCliente.coleccion().get(lugar), mouseX, mouseY);
			}
		}
	}

	// ---------------------------------------------------------------- Color del nombre

	/** Los 16 colores de Minecraft. */
	private static final int[] PALETA = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
	private static final int CELDA_PALETA = 11;
	private boolean paletaAbierta;

	private int xCuadro() {
		return x() + 150 + font.width(info.nombre()) + 4;
	}

	private int xPaleta() { return xCuadro() + 4 - CELDA_PALETA * 4; }
	private int yPaleta() { return y() + 26; }

	private void dibujarPaleta(GuiGraphics g, int mouseX, int mouseY) {
		int px = xPaleta(), py = yPaleta();
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);   // por encima de los botones
		caja(g, px - 3, py - 3, px + CELDA_PALETA * 8 + 3, py + CELDA_PALETA * 2 + 3, 0xFF2A2A30, BORDE);
		for (int i = 0; i < PALETA.length; i++) {
			int cx = px + (i % 8) * CELDA_PALETA, cy = py + (i / 8) * CELDA_PALETA;
			boolean elegido = PALETA[i] == info.color();
			boolean encima = dentro(mouseX, mouseY, cx, cy, CELDA_PALETA, CELDA_PALETA);
			caja(g, cx, cy, cx + CELDA_PALETA - 1, cy + CELDA_PALETA - 1, 0xFF000000 | PALETA[i],
					elegido || encima ? 0xFFFFFFFF : BORDE);
		}
		g.pose().popPose();
	}

	/** Devuelve true si el clic lo manejó la paleta o el cuadradito de color. */
	private boolean clicColor(double mouseX, double mouseY) {
		if (paletaAbierta) {
			int px = xPaleta(), py = yPaleta();
			for (int i = 0; i < PALETA.length; i++) {
				if (dentro(mouseX, mouseY, px + (i % 8) * CELDA_PALETA, py + (i / 8) * CELDA_PALETA, CELDA_PALETA, CELDA_PALETA)) {
					ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.COLOR,
							Integer.toHexString(PALETA[i])));
				}
			}
			paletaAbierta = false;   // cualquier clic cierra la paleta
			return true;
		}
		if (soyMaestro() && dentro(mouseX, mouseY, xCuadro(), y() + 14, 8, 8)) {
			paletaAbierta = true;
			return true;
		}
		return false;
	}

	/** Marco, pestañas y contenido; Screen#render lo dibuja antes que los botones. */
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		int x = x(), y = y();
		dibujarMarco(g, x, y);
		dibujarTabs(g, x, y, mouseX, mouseY);

		switch (pestana) {
			case INICIO -> dibujarInicio(g, x, y);
			case GESTION -> dibujarGestion(g, x, y);
			case ESTANDARTES -> dibujarEstandartes(g, x, y);
			case BRUJULA -> dibujarUbicaciones(g, x, y);
			case LISTA -> dibujarLista(g, x, y);
			case TABLON -> dibujarTablon(g, x, y);
			default -> g.drawCenteredString(font, "Próximamente", x + ANCHO / 2, y + 128, TEXTO_GRIS);
		}
	}

	private void dibujarMarco(GuiGraphics g, int x, int y) {
		caja(g, x, y, x + ANCHO, y + ALTO, METAL, BORDE);
		g.fill(x + 1, y + 1, x + ANCHO - 1, y + 2, METAL_CLARO);
		g.fill(x + 1, y + 1, x + 2, y + ALTO - 1, METAL_CLARO);
		g.fill(x + 1, y + ALTO - 2, x + ANCHO - 1, y + ALTO - 1, METAL_OSCURO);
		caja(g, x + 8, y + 40, x + ANCHO - 8, y + ALTO - 6, FONDO, BORDE);
	}

	private void dibujarTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
		for (int i = 0; i < iconosTabs.length; i++) {
			int tx = x + 8 + i * (TAB + 4), ty = y + 8;
			boolean encima = dentro(mouseX, mouseY, tx, ty, TAB, TAB);
			caja(g, tx, ty, tx + TAB, ty + TAB, i == pestana ? 0xFFD6E2F2 : encima ? 0xFF7A7A86 : 0xFF55555E, BORDE);
			g.renderItem(iconosTabs[i], tx + 3, ty + 3);
		}
		// Botón de la derecha (catalejo): la lista de todas las Hermandades.
		int cx = x + ANCHO - 8 - TAB;
		boolean encimaCatalejo = dentro(mouseX, mouseY, cx, y + 8, TAB, TAB);
		caja(g, cx, y + 8, cx + TAB, y + 8 + TAB,
				pestana == LISTA ? 0xFFD6E2F2 : encimaCatalejo ? 0xFF7A7A86 : 0xFF55555E, BORDE);
		g.renderItem(new ItemStack(Items.SPYGLASS), cx + 3, y + 11);

		int xNombre = x + 150;
		caja(g, xNombre - 4, y + 12, xNombre + font.width(info.nombre()) + 16, y + 25, 0xFF2A2A30, BORDE);
		g.drawString(font, info.nombre(), xNombre, y + 15, 0xFF000000 | info.color(), false);
		int xCuadro = xCuadro();
		boolean encimaCuadro = soyMaestro() && dentro(mouseX, mouseY, xCuadro, y + 14, 8, 8);
		caja(g, xCuadro, y + 14, xCuadro + 8, y + 22, 0xFF000000 | info.color(), encimaCuadro ? 0xFFFFFFFF : BORDE);

		if (pestana == BRUJULA) {
			String contador = "0/" + MAXIMO_UBICACIONES;
			g.drawString(font, contador, x + ANCHO - 12 - font.width(contador), y + 31, TEXTO_GRIS, false);
		} else {
			long enLinea = info.miembros().stream().filter(m -> conectado(m) != null).count();
			g.drawString(font, "En línea: " + enLinea + "/" + info.miembros().size(), x + 246, y + 30, TEXTO_GRIS, false);
		}
	}

	private void dibujarUbicaciones(GuiGraphics g, int x, int y) {
		caja(g, x + 14, y + 46, x + ANCHO - 14, y + 196, PANEL, PANEL_BORDE);
		int centro = x + ANCHO / 2;
		caja(g, centro - 108, y + 202, centro - 92, y + 218, 0xFFFFFFFF, PANEL_BORDE);   // color de la ubicación
	}

	private void tooltipTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
		for (int i = 0; i < iconosTabs.length; i++) {
			if (dentro(mouseX, mouseY, x + 8 + i * (TAB + 4), y + 8, TAB, TAB)) {
				g.renderTooltip(font, NOMBRES_TABS[i] != null ? Component.literal(NOMBRES_TABS[i]) : PROXIMAMENTE, mouseX, mouseY);
			}
		}
		if (dentro(mouseX, mouseY, x + ANCHO - 8 - TAB, y + 8, TAB, TAB)) {
			g.renderTooltip(font, Component.literal("Hermandades"), mouseX, mouseY);
		}
	}

	private void dibujarInicio(GuiGraphics g, int x, int y) {
		caja(g, x + 14, y + 46, x + 238, y + 76, PANEL, PANEL_BORDE);
		String motd = info.motd().isEmpty() ? "Sin mensaje fijado." : info.motd();
		g.drawString(font, "MOTD: ", x + 20, y + 57, CELESTE, false);
		g.drawString(font, motd, x + 20 + font.width("MOTD: "), y + 57, TEXTO, false);

		caja(g, x + 14, y + 80, x + 238, y + 204, PANEL, PANEL_BORDE);
		dibujarChat(g, x + 18, y + 83, 212, y + 201);
		caja(g, x + 14, y + 207, x + 238, y + 222, 0xFF18181C, PANEL_BORDE);

		dibujarMiembros(g, x + 242, y + 46, x + ANCHO - 14, y + 222);
	}

	// ---------------------------------------------------------------- Chat

	private static final int ALTO_LINEA = 10;
	private static final int HORA = 0xFF7FB2FF, AUTOR = 0xFF55AAFF, AVISO = 0xFF7FB2FF, BARRA = 0xFF4F7FD0;

	/** Todas las líneas del chat ya cortadas al ancho del panel. */
	private List<FormattedCharSequence> renglonesChat(int ancho) {
		List<FormattedCharSequence> renglones = new ArrayList<>();
		for (HermandadChatPayload.Linea linea : HermandadesCliente.chat()) {
			MutableComponent texto = Component.literal("[" + linea.hora() + "] ").withColor(HORA);
			if (linea.autor().isEmpty()) {
				texto.append(Component.literal(linea.texto()).withColor(AVISO));
			} else {
				texto.append(Component.literal(linea.autor()).withColor(AUTOR))
						.append(Component.literal(": " + linea.texto()).withColor(TEXTO));
			}
			renglones.addAll(font.split(texto, ancho));
		}
		return renglones;
	}

	private int renglonesVisibles(int y1, int y2) {
		return (y2 - y1) / ALTO_LINEA;
	}

	private void dibujarChat(GuiGraphics g, int x1, int y1, int ancho, int y2) {
		List<FormattedCharSequence> renglones = renglonesChat(ancho - 6);
		int visibles = renglonesVisibles(y1, y2);
		int maximo = Math.max(0, renglones.size() - visibles);
		chatScroll = Math.max(0, Math.min(chatScroll, maximo));
		int desde = Math.max(0, renglones.size() - visibles - chatScroll);
		int hasta = Math.min(renglones.size(), desde + visibles);
		for (int i = desde; i < hasta; i++) {
			g.drawString(font, renglones.get(i), x1, y1 + (i - desde) * ALTO_LINEA, TEXTO, false);
		}
		if (renglones.isEmpty()) g.drawString(font, "Todavía no hay mensajes.", x1, y1, TEXTO_GRIS, false);
		// Barrita a la derecha cuando hay más de lo que entra.
		if (maximo > 0) {
			int alto = y2 - y1;
			int barra = Math.max(8, alto * visibles / renglones.size());
			int yBarra = y1 + (alto - barra) * (maximo - chatScroll) / maximo;
			g.fill(x1 + ancho - 2, yBarra, x1 + ancho, yBarra + barra, BARRA);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int x = x(), y = y();
		if (pestana == INICIO && dentro(mouseX, mouseY, x + 14, y + 80, 224, 124)) {
			chatScroll += scrollY > 0 ? 1 : -1;   // el límite se ajusta al dibujar
			return true;
		}
		if (pestana == LISTA) {
			listaScroll += scrollY > 0 ? -1 : 1;
			return true;
		}
		if (pestana == TABLON) {
			tablonScroll += scrollY > 0 ? -1 : 1;
			refrescarTablon();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	// ---------------------------------------------------------------- Lista de Hermandades

	private static final int FILA_LISTA = 20, ARRIBA_LISTA = 66;

	private int filasVisiblesLista() {
		return (ALTO - 10 - ARRIBA_LISTA) / FILA_LISTA;
	}

	private void dibujarLista(GuiGraphics g, int x, int y) {
		caja(g, x + 14, y + 46, x + ANCHO - 14, y + ALTO - 10, PANEL, PANEL_BORDE);
		g.drawCenteredString(font, "Hermandades", x + ANCHO / 2, y + 53, CELESTE);
		List<Map.Entry<String, Integer>> todas = new ArrayList<>(HermandadesCliente.todas().entrySet());
		int visibles = filasVisiblesLista();
		listaScroll = Math.max(0, Math.min(listaScroll, todas.size() - visibles));
		ItemStack blanco = new ItemStack(Items.WHITE_BANNER);
		for (int i = 0; i < visibles && listaScroll + i < todas.size(); i++) {
			var hermandad = todas.get(listaScroll + i);
			ItemStack estandarte = HermandadesCliente.estandarteDeHermandad(hermandad.getKey());
			if (estandarte.isEmpty()) estandarte = blanco;
			int fy = y + ARRIBA_LISTA + i * FILA_LISTA;
			g.fill(x + 15, fy, x + ANCHO - 15, fy + FILA_LISTA, i % 2 == 0 ? 0xFF000000 : 0xFF2B2B31);
			g.renderItem(estandarte, x + 20, fy + 2);
			g.drawString(font, hermandad.getKey(), x + 42, fy + 6, 0xFF000000 | hermandad.getValue(), false);
		}
		if (todas.isEmpty()) g.drawCenteredString(font, "Todavía no hay Hermandades.", x + ANCHO / 2, y + 80, TEXTO_GRIS);
		// Barrita a la derecha cuando hay más de las que entran.
		if (todas.size() > visibles) {
			int alto = visibles * FILA_LISTA;
			int barra = Math.max(8, alto * visibles / todas.size());
			int yBarra = y + ARRIBA_LISTA + (alto - barra) * listaScroll / (todas.size() - visibles);
			g.fill(x + ANCHO - 19, yBarra, x + ANCHO - 17, yBarra + barra, BARRA);
		}
	}

	private void dibujarGestion(GuiGraphics g, int x, int y) {
		caja(g, x + 14, y + 46, x + 204, y + 94, PANEL, PANEL_BORDE);
		g.drawString(font, "Maestro: " + nombreMaestro(), x + 20, y + 52, TEXTO, false);
		// Balance en monedas: 10.000 = 1 Roja, 100 = 1 Verde, el resto en Deditas.
		String balance = "Balance: ";
		g.drawString(font, balance, x + 20, y + 65, TEXTO, false);
		long total = Math.max(0, info.balance());
		long[] monedas = {total / 10_000, total / 100 % 100, total % 100};
		ResourceLocation[] iconos = {com.dedsafio4.client.cajero.EstiloCajero.ICONO_ROJA,
				com.dedsafio4.client.cajero.EstiloCajero.ICONO_VERDE, ICONO_DEDITA};
		int bx = x + 20 + font.width(balance);
		for (int i = 0; i < 3; i++) {
			String cantidad = Long.toString(monedas[i]);
			g.drawString(font, cantidad, bx, y + 65, TEXTO, false);
			bx += font.width(cantidad) + 1;
			g.blit(iconos[i], bx, y + 64, 9, 9, 0, 0, 16, 16, 16, 16);
			bx += 9 + 4;
		}
		g.drawString(font, "Miembros: " + info.miembros().size(), x + 20, y + 78, TEXTO, false);
		String banco = "Banco: Nvl " + info.nivelBanco();
		g.drawString(font, banco, x + 198 - font.width(banco), y + 78, TEXTO, false);

		g.drawString(font, "Invitar jugador:", x + 16, y + 100, TEXTO, false);
		g.drawString(font, "Establecer MOTD:", x + 16, y + 136, TEXTO, false);

		dibujarMiembros(g, x + 208, y + 46, x + ANCHO - 14, y + 222);
	}

	private void dibujarEstandartes(GuiGraphics g, int x, int y) {
		caja(g, x + 14, y + 46, x + ANCHO - 14, y + 198, PANEL, PANEL_BORDE);
		g.drawCenteredString(font, "Colección de Estandartes", x + ANCHO / 2, y + 54, TEXTO_GRIS);
		int celda = CELDA_ESTANDARTE, x0 = x0Estandartes(), y0 = y0Estandartes();
		List<ItemStack> coleccion = HermandadesCliente.coleccion();
		g.fill(x0 - 1, y0 - 1, x0 + celda * 9 + 1, y0 + celda * 3 + 1, 0xFF101013);
		for (int fila = 0; fila < 3; fila++) {
			for (int col = 0; col < 9; col++) {
				int i = fila * 9 + col;
				int cx = x0 + col * celda, cy = y0 + fila * celda;
				// El activo con borde dorado; el elegido con borde blanco.
				int borde = i == HermandadesCliente.activo() ? ORO : i == estandarteElegido ? 0xFFFFFFFF : 0;
				if (borde != 0) g.fill(cx, cy, cx + celda, cy + celda, borde);
				g.fill(cx + 1, cy + 1, cx + celda - 1, cy + celda - 1, 0xFF232328);
				if (i < coleccion.size()) g.renderItem(coleccion.get(i), cx + 4, cy + 4);
			}
		}
		if (HermandadesCliente.activo() >= 0) {
			g.drawCenteredString(font, "El estandarte con borde dorado es la capa de todos los miembros.",
					x + ANCHO / 2, y0 + celda * 3 + 6, TEXTO_GRIS);
		}
	}

	private void dibujarMiembros(GuiGraphics g, int x1, int y1, int x2, int y2) {
		caja(g, x1, y1, x2, y2, 0xFF18181C, PANEL_BORDE);
		List<HermandadInfoPayload.Miembro> miembros = info.miembros();
		for (int i = 0; i < miembros.size(); i++) {
			HermandadInfoPayload.Miembro m = miembros.get(i);
			int fy = y1 + 2 + i * FILA;
			if (fy + FILA > y2) break;
			boolean maestro = m.uuid().equals(info.maestro());
			int color = maestro ? ORO : AZUL;

			g.fill(x1 + 1, fy, x2 - 1, fy + FILA, i % 2 == 0 ? 0xFF000000 : 0xFF2B2B31);
			g.fill(x1 + 3, fy + 3, x1 + 4, fy + FILA - 3, color);
			PlayerInfo enLinea = conectado(m);
			// El cuadrado de la cara: verde si está conectado, rojo si no.
			caja(g, x1 + 6, fy + 1, x1 + 19, fy + 14, 0xFF000000, enLinea != null ? 0xFF3CB043 : 0xFFD83A3A);
			PlayerFaceRenderer.draw(g, enLinea != null ? enLinea.getSkin() : DefaultPlayerSkin.get(m.uuid()), x1 + 8, fy + 3, 9);
			pixelArt(g, maestro ? CORONA : m.lider() ? ESTRELLA : PERSONA, x1 + 22, fy + 4,
					maestro ? 0xFFFFC21F : m.lider() ? LIDER : 0xFF4DA6FF);
			g.drawString(font, m.nombre(), x1 + 31, fy + 4, color, false);
		}
	}

	private static final String[] CORONA = {"X..X..X", "XX.X.XX", "XXXXXXX", "XXXXXXX", ".XXXXX."};
	private static final String[] PERSONA = {".XXX.", ".XXX.", "..X..", "XXXXX", "XXXXX", "XXXXX"};
	private static final String[] ESTRELLA = {"..X..", "..X..", "XXXXX", ".XXX.", ".X.X.", "X...X"};
	private static final int LIDER = 0xFFC77DF0;

	// ---------------------------------------------------------------- Tablón

	private static final int ARRIBA_TABLON = 50, ABAJO_TABLON = 196;

	private record Renglones(int anuncio, int y, List<FormattedCharSequence> lineas) {}

	/** Dónde queda cada anuncio visible del Tablón (título + texto cortado al ancho). */
	private List<Renglones> armarTablon(int x, int y) {
		List<Renglones> visibles = new ArrayList<>();
		var anuncios = HermandadesCliente.anuncios();
		tablonScroll = Math.max(0, Math.min(tablonScroll, anuncios.size() - 1));
		int fy = y + ARRIBA_TABLON;
		for (int i = tablonScroll; i < anuncios.size(); i++) {
			List<FormattedCharSequence> lineas = font.split(Component.literal(anuncios.get(i).texto()), ANCHO - 48);
			int alto = 12 + lineas.size() * 10 + 6;
			if (fy + alto > y + ABAJO_TABLON && !visibles.isEmpty()) break;
			visibles.add(new Renglones(i, fy, lineas));
			fy += alto;
		}
		return visibles;
	}

	private void initTablon(int x, int y) {
		// Borrar (x) al lado de cada anuncio, para el Maestro y los Líderes.
		if (puedoPublicar()) {
			for (Renglones r : armarTablon(x, y)) {
				int lugar = r.anuncio();
				Button borrar = addRenderableWidget(Button.builder(Component.literal("x"), b ->
						ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.ANUNCIO_BORRAR, Integer.toString(lugar))))
						.bounds(x + ANCHO - 32, r.y() - 1, 13, 11).build());
				borrar.setTooltip(Tooltip.create(Component.literal("Borrar anuncio")));
			}
			anuncioEntrada = addRenderableWidget(new EditBox(font, x + 16, y + 203, ANCHO - 124, 16, Component.literal("Anuncio")));
			anuncioEntrada.setMaxLength(HermandadAccionPayload.LARGO_CHAT);
			anuncioEntrada.setHint(Component.literal("Escribe un anuncio...").withColor(0xFF707078));
			addRenderableWidget(Button.builder(Component.literal("Publicar"), b -> publicarAnuncio())
					.bounds(x + ANCHO - 104, y + 202, 88, 18).build());
		}
	}

	private void publicarAnuncio() {
		if (anuncioEntrada == null) return;
		String texto = anuncioEntrada.getValue().strip();
		if (texto.isEmpty()) return;
		ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.ANUNCIO_PUBLICAR, texto));
		anuncioEntrada.setValue("");
		tablonScroll = 0;
	}

	/** Llegaron anuncios nuevos: se rearman los botones de borrar. */
	public void refrescarTablon() {
		if (pestana != TABLON) return;
		String escrito = anuncioEntrada != null ? anuncioEntrada.getValue() : "";
		rebuildWidgets();
		if (anuncioEntrada != null) anuncioEntrada.setValue(escrito);
	}

	private void dibujarTablon(GuiGraphics g, int x, int y) {
		caja(g, x + 14, y + 46, x + ANCHO - 14, y + 198, PANEL, PANEL_BORDE);
		var anuncios = HermandadesCliente.anuncios();
		if (anuncios.isEmpty()) {
			g.drawCenteredString(font, "Todavía no hay anuncios en el Tablón.", x + ANCHO / 2, y + 116, TEXTO_GRIS);
		}
		for (Renglones r : armarTablon(x, y)) {
			var anuncio = anuncios.get(r.anuncio());
			g.drawString(font, anuncio.fecha(), x + 20, r.y(), 0xFF7FB2FF, false);
			g.drawString(font, anuncio.autor(), x + 24 + font.width(anuncio.fecha()), r.y(), AZUL, false);
			for (int i = 0; i < r.lineas().size(); i++) {
				g.drawString(font, r.lineas().get(i), x + 24, r.y() + 12 + i * 10, TEXTO, false);
			}
			int abajo = r.y() + 12 + r.lineas().size() * 10 + 2;
			g.fill(x + 18, abajo, x + ANCHO - 18, abajo + 1, PANEL_BORDE);
		}
		if (!puedoPublicar()) {
			g.drawCenteredString(font, "Solo el Maestro y los Líderes pueden publicar anuncios.", x + ANCHO / 2, y + 208, TEXTO_GRIS);
		}
	}

	private static void pixelArt(GuiGraphics g, String[] patron, int x, int y, int color) {
		for (int fila = 0; fila < patron.length; fila++) {
			for (int col = 0; col < patron[fila].length(); col++) {
				if (patron[fila].charAt(col) == 'X') g.fill(x + col, y + fila, x + col + 1, y + fila + 1, color);
			}
		}
	}

	private static void caja(GuiGraphics g, int x1, int y1, int x2, int y2, int fondo, int borde) {
		g.fill(x1, y1, x2, y2, borde);
		g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fondo);
	}

	private PlayerInfo conectado(HermandadInfoPayload.Miembro m) {
		return minecraft.getConnection() != null ? minecraft.getConnection().getPlayerInfo(m.uuid()) : null;
	}

	private String nombreMaestro() {
		return info.miembros().stream().filter(m -> m.uuid().equals(info.maestro()))
				.map(HermandadInfoPayload.Miembro::nombre).findFirst().orElse("?");
	}

	private static boolean dentro(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---------------------------------------------------------------- Entrada

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int boton) {
		if (clicColor(mouseX, mouseY)) return true;
		int x = x(), y = y();
		for (int i = 0; i < iconosTabs.length; i++) {
			if (dentro(mouseX, mouseY, x + 8 + i * (TAB + 4), y + 8, TAB, TAB) && i != pestana) {
				pestana = i;
				rebuildWidgets();
				return true;
			}
		}
		if (pestana == ESTANDARTES) {
			int lugar = lugarEstandarte(mouseX, mouseY);
			if (lugar >= 0) {
				estandarteElegido = lugar < HermandadesCliente.coleccion().size() && lugar != estandarteElegido ? lugar : -1;
				rebuildWidgets();
				return true;
			}
		}
		if (dentro(mouseX, mouseY, x + ANCHO - 8 - TAB, y + 8, TAB, TAB) && pestana != LISTA) {
			pestana = LISTA;
			listaScroll = 0;
			rebuildWidgets();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, boton);
	}

	@Override
	public boolean keyPressed(int tecla, int scanCode, int modificadores) {
		// La misma tecla H cierra la interfaz, salvo que se esté escribiendo en un campo.
		if (Dedsafio4Client.TECLA_HERMANDAD.matches(tecla, scanCode) && !(getFocused() instanceof EditBox)) {
			onClose();
			return true;
		}
		return super.keyPressed(tecla, scanCode, modificadores);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
