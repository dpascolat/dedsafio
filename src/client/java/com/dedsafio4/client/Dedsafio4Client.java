package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.banco.DeditasPayload;
import com.dedsafio4.cielo.CieloPayload;
import com.dedsafio4.hermandad.HermandadAccionPayload;
import com.dedsafio4.hermandad.HermandadInfoPayload;
import com.dedsafio4.hermandad.HermandadesJugadoresPayload;
import com.dedsafio4.hermandad.ManuscritoDatos;
import com.dedsafio4.items.ManuscritoHermandadItem;
import com.dedsafio4.items.ModItems;
import com.dedsafio4.candados.CandadoPantallaPayload;
import com.dedsafio4.candados.Candados;
import com.dedsafio4.candados.CandadosListaPayload;
import com.dedsafio4.bloques.ModBloques;
import com.dedsafio4.client.bestias.SoarerRenderer;
import com.dedsafio4.client.bestias.WalkerRenderer;
import com.dedsafio4.client.robots.AldeanoRobotRenderer;
import com.dedsafio4.client.lagartos.LagartoRenderer;
import com.dedsafio4.lagartos.LagartoEntity;
import com.dedsafio4.client.bombas.BombaWardenModelo;
import com.dedsafio4.client.bombas.BombaWardenRenderer;
import com.dedsafio4.client.nave.NaveRenderer;
import com.dedsafio4.client.nave.RayoRenderer;
import com.dedsafio4.marcas.MarcasPayload;
import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.dedsafio4.temporizador.TemporizadorPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class Dedsafio4Client implements ClientModInitializer {

	/** Saldo que mandó el servidor; -1 mientras no llegó ninguno. */
	private static long saldo = -1;

	public static final KeyMapping TECLA_HERMANDAD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.dedsafio4.hermandad", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.dedsafio4"));

	@Override
	public void onInitializeClient() {
		BaneadosScreen.registrar();
		BlockRenderLayerMap.INSTANCE.putBlock(com.dedsafio4.baneos.Baneos.BLOQUE, RenderType.cutout());
		com.dedsafio4.items.SinAlmaItem.abrirCatalogo = () -> {
			var mc = net.minecraft.client.Minecraft.getInstance();
			if (mc.screen == null && mc.player != null) mc.setScreen(new CatalogoScreen());
		};
		com.dedsafio4.client.nave.ParteNaveRenderer.registrar();
		com.dedsafio4.client.menu.MenuPrincipalScreen.registrar();
		// El montoncito de la CACA necesita transparencia.
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.CACA, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.PASTO_ROSA, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.ARANDANO_NOCTURNO, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.FRUTA_SOLARIA, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.BAYA_UVINA, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.HUEVO_EBURIA, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBloques.AGUA_PORTAL, RenderType.translucent());
		// El cofre nuevo: se dibuja con su malla (y el ítem también, cerrado).
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.dedsafio4.cofres.ModCofres.COFRE_ENTIDAD, com.dedsafio4.client.cofres.CofreRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
				com.dedsafio4.cofres.ModCofres.COFRE_ITEM, (pila, modo, pose, buffers, luz, overlay) -> {
					pose.pushPose();
					pose.translate(0.5, 0, 0.5);
					pose.scale(0.5f, 0.5f, 0.5f);   // mide dos bloques: en la mano se achica
					pose.translate(0, 0.5, 0);
					com.dedsafio4.client.cofres.CofreRenderer.dibujar(pose, buffers, luz, overlay,
							net.minecraft.core.Direction.SOUTH, 0f, false);
					pose.popPose();
				});
		// El Cofre de Huesos: igual, con su modelo de costillas.
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.dedsafio4.cofres.ModCofres.COFRE_HUESOS_ENTIDAD, com.dedsafio4.client.cofres.CofreHuesosRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
				com.dedsafio4.cofres.ModCofres.COFRE_HUESOS_ITEM, (pila, modo, pose, buffers, luz, overlay) -> {
					pose.pushPose();
					pose.translate(0.5, 0, 0.5);
					pose.scale(0.5f, 0.5f, 0.5f);
					pose.translate(0, 0.5, 0);
					com.dedsafio4.client.cofres.CofreHuesosRenderer.dibujar(pose, buffers, luz, overlay,
							net.minecraft.core.Direction.SOUTH, 0f, false);
					pose.popPose();
				});
		// Las puertas de 3x3 con llave: las dibuja su bloque principal.
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.dedsafio4.puertas.ModPuertas.PUERTA_ENTIDAD, com.dedsafio4.client.puertas.PuertaRenderer::new);
		// El Aldeano Robot dormido (bloque), con el mismo modelo del robot.
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.dedsafio4.robots.ModRobots.ROBOT_DORMIDO_ENTIDAD, com.dedsafio4.client.robots.RobotDormidoRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
				com.dedsafio4.robots.ModRobots.ROBOT_DORMIDO_ITEM, (pila, modo, pose, buffers, luz, overlay) ->
						com.dedsafio4.client.robots.RobotDormidoRenderer.dibujar(pose, buffers, luz, overlay,
								net.minecraft.core.Direction.SOUTH, 0f));
		// El Cajero: el menú se abre al tocar el bloque; la grilla de ingresar la abre el servidor.
		com.dedsafio4.banco.CajeroBlock.abrirPantalla = pos ->
				Minecraft.getInstance().setScreen(new com.dedsafio4.client.cajero.CajeroScreen(pos));
		net.minecraft.client.gui.screens.MenuScreens.register(com.dedsafio4.banco.ModCajero.MENU,
				com.dedsafio4.client.cajero.CajeroIngresarScreen::new);
		DebugCielo.registrar();
		ClientTickEvents.END_CLIENT_TICK.register(PocionesCliente::tick);

		// H → se piden los datos al servidor; la interfaz se abre cuando llegan.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (TECLA_HERMANDAD.consumeClick()) {
				if (client.screen == null) {
					ClientPlayNetworking.send(new HermandadAccionPayload(HermandadAccionPayload.PEDIR_INFO, ""));
				}
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(CandadosListaPayload.TYPE, (payload, context) ->
				Candados.cerradosCliente = java.util.Set.copyOf(payload.posiciones()));
		ClientPlayNetworking.registerGlobalReceiver(CandadoPantallaPayload.TYPE, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new CandadoScreen(payload.pos(), payload.cerrar(), payload.aviso()))));
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.hermandad.HermandadChatPayload.TYPE, (payload, context) ->
				HermandadesCliente.actualizarChat(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.hermandad.HermandadTablonPayload.TYPE, (payload, context) -> {
			HermandadesCliente.actualizarTablon(payload);
			if (context.client().screen instanceof HermandadScreen pantalla) pantalla.refrescarTablon();
		});
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.hermandad.HermandadEstandartesPayload.TYPE, (payload, context) -> {
			HermandadesCliente.actualizarEstandartes(payload);
			if (context.client().screen instanceof HermandadScreen pantalla) pantalla.refrescarEstandartes();
		});
		// La capa con el estandarte de la Hermandad.
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
				(tipo, dibujante, registro, contexto) -> {
					if (dibujante instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer jugador) {
						registro.register(new CapaEstandarteLayer(jugador, contexto.getModelSet()));
					}
				});
		ClientPlayNetworking.registerGlobalReceiver(HermandadInfoPayload.TYPE, (payload, context) -> {
			Minecraft mc = context.client();
			if (mc.screen instanceof HermandadScreen pantalla) pantalla.actualizar(payload);
			else if (mc.screen == null) mc.setScreen(new HermandadScreen(payload));
		});

		ClientPlayNetworking.registerGlobalReceiver(DeditasPayload.TYPE, (payload, context) -> saldo = payload.saldo());
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.eclipse.Eclipse.Payload.TYPE, (payload, context) ->
				EclipseCliente.setActivo(payload.activo(), payload.animar()));
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.marcas.MinimapaPayload.TYPE, (payload, context) ->
				XaeroBloqueo.adminsLoUsan = payload.adminsLoUsan());
		ClientPlayNetworking.registerGlobalReceiver(HermandadesJugadoresPayload.TYPE, (payload, context) ->
				HermandadesCliente.actualizar(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio4.almas.Almas.Payload.TYPE, (payload, context) ->
				AlmaCliente.tieneAlma = payload.tiene());
		ClientPlayNetworking.registerGlobalReceiver(CieloPayload.TYPE, (payload, context) ->
				CieloCliente.setActivo(payload.rojo(), payload.animar()));
		ClientPlayNetworking.registerGlobalReceiver(TemporizadorPayload.TYPE, (payload, context) ->
				TemporizadorCliente.actualizar(payload.segundos()));
		ClientPlayNetworking.registerGlobalReceiver(MarcasPayload.TYPE, (payload, context) ->
				MarcasCliente.actualizar(payload));
		WorldRenderEvents.LAST.register(MarcasCliente::dibujar);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(LinternaLuz::dibujarHaces);

		EntityRendererRegistry.register(ModEntidades.NAVE, NaveRenderer::new);
		EntityRendererRegistry.register(ModEntidades.BOMBA_WARDEN, BombaWardenRenderer::new);
		EntityRendererRegistry.register(ModEntidades.SOARER, SoarerRenderer::new);
		EntityRendererRegistry.register(ModEntidades.WALKER, WalkerRenderer::new);
		EntityRendererRegistry.register(ModEntidades.ALDEANO_ROBOT, AldeanoRobotRenderer::new);
		EntityRendererRegistry.register(ModEntidades.CREEPER_PASTO, com.dedsafio4.client.bestias.CreeperPastoRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.meteoritos.ModMeteoritos.METEORITO, com.dedsafio4.client.meteoritos.MeteoritoRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.marcos.ModMarcos.MARCO_GRANDE, com.dedsafio4.client.marcos.MarcoGrandeRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.casino.ModCasino.CASINO, com.dedsafio4.client.casino.CasinoRenderer::new);
		// En la Dimensión de los Órganos no hay nubes: se cambian por un dibujo vacío.
		net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry.registerCloudRenderer(
				com.dedsafio4.dimension.Organos.DIMENSION, contexto -> {});
		com.dedsafio4.client.despegue.NaveCamara.registrar();
		EntityRendererRegistry.register(com.dedsafio4.despegue.ModDespegue.NAVE_VIAJE,
				com.dedsafio4.client.despegue.NaveViajeRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.dactylos.ModDactylos.DACTYLO_BEBE, com.dedsafio4.client.dactylos.DactyloBebeRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.dactylos.ModDactylos.TINTA_ENTIDAD, com.dedsafio4.client.dactylos.TintaDactyloRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, com.dedsafio4.client.herobrine.HerobrineRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.trex.ModTRex.TREX, com.dedsafio4.client.trex.TRexRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.qumara.ModQumara.QUMARA, com.dedsafio4.client.qumara.QumaraRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.qumara.ModQumara.BICHO_NO_CABEZA, c -> new com.dedsafio4.client.qumara.BichoCuboRenderer(c, false));
		EntityRendererRegistry.register(com.dedsafio4.qumara.ModQumara.BICHO_CABEZA, c -> new com.dedsafio4.client.qumara.BichoCuboRenderer(c, true));
		com.dedsafio4.client.qumara.BichoCuboRenderer.registrarLanzar();
		EntityRendererRegistry.register(com.dedsafio4.qumara.ModQumara.CAPULLO, com.dedsafio4.client.qumara.CapulloRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.qumara.ModQumara.CIRCULO, com.dedsafio4.client.qumara.CirculoRenderer::new);
		EntityRendererRegistry.register(com.dedsafio4.qumara.ModQumara.GAS_MORADO, com.dedsafio4.client.qumara.GasMoradoRenderer::new);
		com.dedsafio4.client.trex.TRexControles.registrar();
		com.dedsafio4.client.qumara.AgarreMinijuego.registrar();
		com.dedsafio4.client.qumara.AtraerCliente.registrar();
		CatalogoCliente.registrar();
		for (int i = 0; i < ModEntidades.LAGARTOS.size(); i++) {
			LagartoEntity.Variante variante = LagartoEntity.Variante.values()[i];
			EntityRendererRegistry.register(ModEntidades.LAGARTOS.get(i),
					contexto -> new LagartoRenderer(contexto, variante));
		}
		EntityModelLayerRegistry.registerModelLayer(BombaWardenRenderer.CAPA, BombaWardenModelo::crearCapa);
		EntityRendererRegistry.register(ModEntidades.RAYO, RayoRenderer::new);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			saldo = -1;
			HermandadesCliente.limpiar();
			CieloCliente.setActivo(false, false);
			TemporizadorCliente.limpiar();
			MarcasCliente.limpiar();
		});

		for (Item manuscrito : ModItems.MANUSCRITOS) {
			ItemProperties.register(manuscrito, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "sellado"),
					(stack, level, entidad, semilla) -> ManuscritoDatos.sellado(stack) ? 1 : 0);
		}
		ManuscritoHermandadItem.abrirPantalla = mano -> {
			Minecraft mc = Minecraft.getInstance();
			mc.setScreen(new ManuscritoScreen(mano, mc.player.getItemInHand(mano)));
		};

		HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
			com.dedsafio4.client.dactylos.TintaPantalla.dibujar(graphics);
			com.dedsafio4.client.qumara.QumaraBarra.dibujar(graphics);
			com.dedsafio4.client.trex.TRexControles.dibujar(graphics);
			com.dedsafio4.client.qumara.AgarreMinijuego.dibujar(graphics);
			com.dedsafio4.client.qumara.AtraerCliente.dibujar(graphics);
			Destello.dibujar(graphics);
			Minecraft mc = Minecraft.getInstance();
			if (mc.options.hideGui) return;
			TemporizadorCliente.dibujar(graphics, mc);
			if (saldo >= 0) dibujarSaldo(graphics, mc);
		});
	}

	public static long saldo() {
		return saldo;
	}

	/** Tamaño del recuadro del saldo (1 = tamaño normal). */
	private static final float ESCALA_SALDO = 0.75f;
	/** Tamaño del ícono de cada moneda en el contador (antes de la escala). */
	private static final int ICONO_SALDO = 10;

	/**
	 * Recuadro arriba a la derecha con el saldo en monedas: 100 deditas = 1 Verde, 100 Verdes = 1 Roja.
	 * Ej.: 270 → [ 2 (verde)  70 (dedita) ]. Las monedas más grandes aparecen solo si alcanza.
	 */
	private static void dibujarSaldo(GuiGraphics graphics, Minecraft mc) {
		int margen = 3, relleno = 3;
		int ancho = relleno * 2 + com.dedsafio4.client.cajero.EstiloCajero.anchoMonedas(mc.font, saldo, ICONO_SALDO);
		int alto = relleno + Math.max(ICONO_SALDO, mc.font.lineHeight) + relleno;
		graphics.pose().pushPose();
		graphics.pose().translate(graphics.guiWidth() - margen, margen, 0);
		graphics.pose().scale(ESCALA_SALDO, ESCALA_SALDO, 1);
		int x = -ancho;
		graphics.fill(x, 0, 0, alto, 0xFF55555A);             // borde
		graphics.fill(x + 1, 1, -1, alto - 1, 0xFF1E1E22);    // fondo
		com.dedsafio4.client.cajero.EstiloCajero.dibujarMonedas(graphics, mc.font, x + relleno, relleno, saldo, ICONO_SALDO);
		graphics.pose().popPose();
	}
}
