package com.dedsafio4.neocompat;

import com.dedsafio4.neocompat.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.dedsafio4.neocompat.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.dedsafio4.neocompat.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import com.dedsafio4.neocompat.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.dedsafio4.neocompat.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.HudRenderCallback;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.WorldRenderContext;
import com.dedsafio4.neocompat.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.dedsafio4.neocompat.fabric.api.client.screen.v1.ScreenEvents;
import com.dedsafio4.neocompat.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import com.dedsafio4.neocompat.fabric.api.networking.v1.PacketSender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** La parte de Puente que sólo existe en el cliente. */
public final class PuenteCliente {
	private PuenteCliente() {}

	static void iniciarCliente() {
		new com.dedsafio4.client.Dedsafio4Client().onInitializeClient();
	}

	@SuppressWarnings("unchecked")
	static void recibir(CustomPacketPayload payload, IPayloadContext ctx) {
		var receptor = (ClientPlayNetworking.PlayPayloadHandler<CustomPacketPayload>) ClientPlayNetworking.RECEPTORES.get(payload.type());
		if (receptor == null) return;
		Minecraft mc = Minecraft.getInstance();
		receptor.receive(payload, new ClientPlayNetworking.Context() {
			@Override
			public Minecraft client() {
				return mc;
			}

			@Override
			public LocalPlayer player() {
				return mc.player;
			}

			@Override
			public PacketSender responseSender() {
				return PacketDistributor::sendToServer;
			}
		});
	}

	static void iniciar(IEventBus mod) {
		mod.addListener(EventPriority.NORMAL, false, EntityRenderersEvent.RegisterRenderers.class, e -> {
			for (var r : EntityRendererRegistry.REGISTROS) renderer(e, r);
		});
		mod.addListener(EventPriority.NORMAL, false, EntityRenderersEvent.RegisterLayerDefinitions.class, e ->
				EntityModelLayerRegistry.CAPAS.forEach((capa, proveedor) -> e.registerLayerDefinition(capa, proveedor::createModelData)));
		mod.addListener(EventPriority.NORMAL, false, EntityRenderersEvent.AddLayers.class, PuenteCliente::capas);
		mod.addListener(EventPriority.NORMAL, false, RegisterKeyMappingsEvent.class, e -> KeyBindingHelper.TECLAS.forEach(e::register));
		mod.addListener(EventPriority.NORMAL, false, RegisterMenuScreensEvent.class, e -> NeoCompat.PANTALLAS.forEach(p -> p.accept(e)));
		mod.addListener(EventPriority.NORMAL, false, RegisterClientExtensionsEvent.class, e ->
				BuiltinItemRendererRegistry.INSTANCE.DIBUJANTES.forEach((item, dibujante) -> e.registerItem(new IClientItemExtensions() {
					private BlockEntityWithoutLevelRenderer dibujanteItem;

					@Override
					public BlockEntityWithoutLevelRenderer getCustomRenderer() {
						if (dibujanteItem == null) {
							Minecraft mc = Minecraft.getInstance();
							dibujanteItem = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
								@Override
								public void renderByItem(ItemStack pila, ItemDisplayContext modo, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
									dibujante.render(pila, modo, pose, buffers, luz, overlay);
								}
							};
						}
						return dibujanteItem;
					}
				}, item)));

		IEventBus juego = NeoForge.EVENT_BUS;
		juego.addListener(EventPriority.NORMAL, false, ClientTickEvent.Pre.class,
				e -> ClientTickEvents.START_CLIENT_TICK.oyentes().forEach(o -> o.onStartTick(Minecraft.getInstance())));
		juego.addListener(EventPriority.NORMAL, false, ClientTickEvent.Post.class,
				e -> ClientTickEvents.END_CLIENT_TICK.oyentes().forEach(o -> o.onEndTick(Minecraft.getInstance())));
		juego.addListener(EventPriority.NORMAL, false, ScreenEvent.Init.Pre.class, e -> {
			// Como en Fabric: al armarse de nuevo la pantalla, sus eventos propios empiezan vacíos.
			ScreenEvents.DESPUES_DE_DIBUJAR.remove(e.getScreen());
			ScreenKeyboardEvents.DESPUES_DE_TECLA.remove(e.getScreen());
			ScreenEvents.BEFORE_INIT.oyentes().forEach(o -> o.beforeInit(Minecraft.getInstance(), e.getScreen(), e.getScreen().width, e.getScreen().height));
		});
		juego.addListener(EventPriority.NORMAL, false, ScreenEvent.Init.Post.class, e ->
				ScreenEvents.AFTER_INIT.oyentes().forEach(o -> o.afterInit(Minecraft.getInstance(), e.getScreen(), e.getScreen().width, e.getScreen().height)));
		juego.addListener(EventPriority.NORMAL, false, ScreenEvent.Render.Post.class, e -> {
			var evento = ScreenEvents.DESPUES_DE_DIBUJAR.get(e.getScreen());
			if (evento != null) evento.oyentes().forEach(o -> o.afterRender(e.getScreen(), e.getGuiGraphics(), e.getMouseX(), e.getMouseY(), e.getPartialTick()));
		});
		juego.addListener(EventPriority.NORMAL, false, ScreenEvent.KeyPressed.Post.class, e -> {
			var evento = ScreenKeyboardEvents.DESPUES_DE_TECLA.get(e.getScreen());
			if (evento != null) evento.oyentes().forEach(o -> o.afterKeyPress(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers()));
		});
		juego.addListener(EventPriority.NORMAL, false, RenderGuiEvent.Post.class,
				e -> HudRenderCallback.EVENT.oyentes().forEach(o -> o.onHudRender(e.getGuiGraphics(), e.getPartialTick())));
		juego.addListener(EventPriority.NORMAL, false, RenderLevelStageEvent.class, e -> {
			var etapa = e.getStage();
			WorldRenderContext contexto = new WorldRenderContext(e.getCamera(), e.getModelViewMatrix(), e.getProjectionMatrix(), e.getPartialTick(), e.getPoseStack());
			if (etapa == RenderLevelStageEvent.Stage.AFTER_LEVEL) WorldRenderEvents.LAST.oyentes().forEach(o -> o.onLast(contexto));
			else if (etapa == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) WorldRenderEvents.AFTER_TRANSLUCENT.oyentes().forEach(o -> o.afterTranslucent(contexto));
			else if (etapa == RenderLevelStageEvent.Stage.AFTER_ENTITIES) WorldRenderEvents.AFTER_ENTITIES.oyentes().forEach(o -> o.afterEntities(contexto));
		});
		juego.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingOut.class, e -> {
			Minecraft mc = Minecraft.getInstance();
			ClientPlayConnectionEvents.DISCONNECT.oyentes().forEach(o -> o.onPlayDisconnect(mc.getConnection(), mc));
		});
		juego.addListener(EventPriority.NORMAL, false, ClientChatReceivedEvent.System.class,
				e -> ClientReceiveMessageEvents.GAME.oyentes().forEach(o -> o.onReceiveGameMessage(e.getMessage(), e.isOverlay())));
	}

	private static <E extends net.minecraft.world.entity.Entity> void renderer(EntityRenderersEvent.RegisterRenderers e, EntityRendererRegistry.Registro<E> r) {
		e.registerEntityRenderer(r.tipo(), r.fabrica());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void capas(EntityRenderersEvent.AddLayers e) {
		for (PlayerSkin.Model modelo : e.getSkins()) {
			EntityRenderer<?> dibujante = e.getSkin(modelo);
			if (dibujante instanceof LivingEntityRenderer<?, ?> vivo) llamarCapas(EntityType.PLAYER, vivo, e);
		}
		for (EntityType<?> tipo : e.getEntityTypes()) {
			EntityRenderer<?> dibujante = e.getRenderer(tipo);
			if (dibujante instanceof LivingEntityRenderer<?, ?> vivo) llamarCapas((EntityType<? extends LivingEntity>) tipo, vivo, e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void llamarCapas(EntityType<? extends LivingEntity> tipo, LivingEntityRenderer<?, ?> vivo, EntityRenderersEvent.AddLayers e) {
		LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper ayudante = new LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper() {
			@Override
			public <T extends LivingEntity> void register(RenderLayer<T, ? extends net.minecraft.client.model.EntityModel<T>> capa) {
				((LivingEntityRenderer) vivo).addLayer((RenderLayer) capa);
			}
		};
		for (var o : LivingEntityFeatureRendererRegistrationCallback.EVENT.oyentes()) o.registerRenderers(tipo, vivo, ayudante, e.getContext());
	}
}
