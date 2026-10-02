package com.dedsafio4.neocompat;

import com.dedsafio4.neocompat.fabric.api.biome.v1.BiomasFabric;
import com.dedsafio4.neocompat.fabric.api.command.v2.CommandRegistrationCallback;
import com.dedsafio4.neocompat.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import com.dedsafio4.neocompat.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import com.dedsafio4.neocompat.fabric.api.entity.event.v1.ServerPlayerEvents;
import com.dedsafio4.neocompat.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import com.dedsafio4.neocompat.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.dedsafio4.neocompat.fabric.api.event.player.PlayerBlockBreakEvents;
import com.dedsafio4.neocompat.fabric.api.event.player.UseBlockCallback;
import com.dedsafio4.neocompat.fabric.api.event.player.UseEntityCallback;
import com.dedsafio4.neocompat.fabric.api.event.player.UseItemCallback;
import com.dedsafio4.neocompat.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import com.dedsafio4.neocompat.fabric.api.itemgroup.v1.ItemGroupEvents;
import com.dedsafio4.neocompat.fabric.api.networking.v1.PacketSender;
import com.dedsafio4.neocompat.fabric.api.networking.v1.PayloadTypeRegistry;
import com.dedsafio4.neocompat.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.dedsafio4.neocompat.fabric.api.networking.v1.ServerPlayNetworking;
import com.dedsafio4.neocompat.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import com.dedsafio4.neocompat.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Une el código de la versión Fabric con NeoForge: arranca el mod cuando NeoForge deja registrar cosas (el primer
 * RegisterEvent) y pasa cada evento de NeoForge a los oyentes que el mod anotó en la capa compatible.
 */
public final class Puente {
	private Puente() {}

	private static boolean iniciado;
	/** El jugador de antes de reaparecer (lo pide AFTER_RESPAWN de Fabric). */
	private static final Map<UUID, ServerPlayer> ANTES_DE_REAPARECER = new HashMap<>();

	public static void iniciar(IEventBus mod, Dist lado) {
		mod.addListener(EventPriority.HIGHEST, false, RegisterEvent.class, e -> {
			if (!iniciado) {
				iniciado = true;
				new com.dedsafio4.Dedsafio4().onInitialize();
				if (lado.isClient()) PuenteCliente.iniciarCliente();
			}
			e.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
					ResourceLocation.fromNamespaceAndPath("dedsafio4", "fabric_spawns"), () -> BiomasFabric.CODEC);
		});
		mod.addListener(EventPriority.NORMAL, false, EntityAttributeCreationEvent.class,
				e -> FabricDefaultAttributeRegistry.ATRIBUTOS.forEach(e::put));
		mod.addListener(EventPriority.NORMAL, false, RegisterPayloadHandlersEvent.class, Puente::paquetes);
		mod.addListener(EventPriority.NORMAL, false, BuildCreativeModeTabContentsEvent.class, e -> {
			var evento = ItemGroupEvents.POR_PESTANIA.get(e.getTabKey());
			if (evento != null) evento.oyentes().forEach(o -> o.modifyEntries(new FabricItemGroupEntries(e)));
		});
		mod.addListener(EventPriority.HIGHEST, false, RegisterCapabilitiesEvent.class, Puente::capacidades);
		eventosDelJuego(NeoForge.EVENT_BUS);
		if (lado.isClient()) PuenteCliente.iniciar(mod);
	}

	// ---------------- Red ----------------

	private static void paquetes(RegisterPayloadHandlersEvent e) {
		PayloadRegistrar registrar = e.registrar("1").optional();
		Set<CustomPacketPayload.Type<?>> alServidor = new HashSet<>();
		for (var r : PayloadTypeRegistry.AL_SERVIDOR) alServidor.add(r.tipo());
		Set<CustomPacketPayload.Type<?>> hechos = new HashSet<>();
		for (var r : PayloadTypeRegistry.AL_CLIENTE) {
			if (!hechos.add(r.tipo())) continue;
			if (alServidor.contains(r.tipo())) ambos(registrar, r);
			else alCliente(registrar, r);
		}
		for (var r : PayloadTypeRegistry.AL_SERVIDOR) {
			if (hechos.add(r.tipo())) alServidor(registrar, r);
		}
	}

	private static <T extends CustomPacketPayload> void alCliente(PayloadRegistrar registrar, PayloadTypeRegistry.Registro<T> r) {
		registrar.playToClient(r.tipo(), r.codec(), (payload, ctx) -> PuenteCliente.recibir(payload, ctx));
	}

	private static <T extends CustomPacketPayload> void alServidor(PayloadRegistrar registrar, PayloadTypeRegistry.Registro<T> r) {
		registrar.playToServer(r.tipo(), r.codec(), Puente::recibirEnServidor);
	}

	private static <T extends CustomPacketPayload> void ambos(PayloadRegistrar registrar, PayloadTypeRegistry.Registro<T> r) {
		registrar.playBidirectional(r.tipo(), r.codec(), (payload, ctx) -> {
			if (ctx.flow() == PacketFlow.CLIENTBOUND) PuenteCliente.recibir(payload, ctx);
			else recibirEnServidor(payload, ctx);
		});
	}

	@SuppressWarnings("unchecked")
	private static void recibirEnServidor(CustomPacketPayload payload, IPayloadContext ctx) {
		var receptor = (ServerPlayNetworking.PlayPayloadHandler<CustomPacketPayload>) ServerPlayNetworking.RECEPTORES.get(payload.type());
		if (receptor == null || !(ctx.player() instanceof ServerPlayer jugador)) return;
		receptor.receive(payload, new ServerPlayNetworking.Context() {
			@Override
			public ServerPlayer player() {
				return jugador;
			}

			@Override
			public MinecraftServer server() {
				return jugador.server;
			}

			@Override
			public PacketSender responseSender() {
				return p -> PacketDistributor.sendToPlayer(jugador, p);
			}
		});
	}

	private static void capacidades(RegisterCapabilitiesEvent e) {
		for (var p : ItemStorage.SIDED.PROVEEDORES) {
			e.registerBlock(Capabilities.ItemHandler.BLOCK,
					(mundo, pos, estado, bloqueEntidad, lado) -> p.proveedor().find(mundo, pos, estado, bloqueEntidad, lado) != null
							? EmptyItemHandler.INSTANCE : null,
					p.bloques());
		}
	}

	// ---------------- Eventos del juego ----------------

	private static void eventosDelJuego(IEventBus juego) {
		juego.addListener(EventPriority.NORMAL, false, ServerTickEvent.Pre.class,
				e -> ServerTickEvents.START_SERVER_TICK.oyentes().forEach(o -> o.onStartTick(e.getServer())));
		juego.addListener(EventPriority.NORMAL, false, ServerTickEvent.Post.class,
				e -> ServerTickEvents.END_SERVER_TICK.oyentes().forEach(o -> o.onEndTick(e.getServer())));
		juego.addListener(EventPriority.NORMAL, false, LevelTickEvent.Pre.class, e -> {
			if (e.getLevel() instanceof ServerLevel mundo) ServerTickEvents.START_WORLD_TICK.oyentes().forEach(o -> o.onStartTick(mundo));
		});
		juego.addListener(EventPriority.NORMAL, false, LevelTickEvent.Post.class, e -> {
			if (e.getLevel() instanceof ServerLevel mundo) ServerTickEvents.END_WORLD_TICK.oyentes().forEach(o -> o.onEndTick(mundo));
		});
		juego.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, e -> {
			if (!(e.getLevel() instanceof ServerLevel mundo)) return;
			for (var o : ServerEntityEvents.ENTITY_LOAD.oyentes()) o.onLoad(e.getEntity(), mundo);
			// En Fabric se podía sacar la entidad ahí mismo; en NeoForge se cancela su entrada.
			if (e.getEntity().isRemoved()) e.setCanceled(true);
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedInEvent.class, e -> {
			if (!(e.getEntity() instanceof ServerPlayer jugador)) return;
			PacketSender enviar = p -> PacketDistributor.sendToPlayer(jugador, p);
			ServerPlayConnectionEvents.JOIN.oyentes().forEach(o -> o.onPlayReady(jugador.connection, enviar, jugador.server));
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedOutEvent.class, e -> {
			if (!(e.getEntity() instanceof ServerPlayer jugador)) return;
			ServerPlayConnectionEvents.DISCONNECT.oyentes().forEach(o -> o.onPlayDisconnect(jugador.connection, jugador.server));
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerEvent.Clone.class, e -> {
			if (!(e.getOriginal() instanceof ServerPlayer viejo) || !(e.getEntity() instanceof ServerPlayer nuevo)) return;
			ServerPlayerEvents.COPY_FROM.oyentes().forEach(o -> o.copyFromPlayer(viejo, nuevo, !e.isWasDeath()));
			ANTES_DE_REAPARECER.put(nuevo.getUUID(), viejo);
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerRespawnEvent.class, e -> {
			if (!(e.getEntity() instanceof ServerPlayer nuevo)) return;
			ServerPlayer viejo = ANTES_DE_REAPARECER.remove(nuevo.getUUID());
			ServerPlayerEvents.AFTER_RESPAWN.oyentes().forEach(o -> o.afterRespawn(viejo != null ? viejo : nuevo, nuevo, e.isEndConquered()));
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
			if (!(e.getEntity() instanceof ServerPlayer jugador)) return;
			ServerLevel antes = jugador.server.getLevel(e.getFrom()), despues = jugador.server.getLevel(e.getTo());
			ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.oyentes().forEach(o -> o.afterChangeWorld(jugador, antes, despues));
		});
		juego.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, e -> {
			if (e.getEntity().level().isClientSide) return;
			for (var o : ServerLivingEntityEvents.ALLOW_DEATH.oyentes()) {
				if (!o.allowDeath(e.getEntity(), e.getSource(), 0f)) {
					e.setCanceled(true);
					return;
				}
			}
		});
		juego.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
			if (e.getEntity().level().isClientSide) return;
			ServerLivingEntityEvents.AFTER_DEATH.oyentes().forEach(o -> o.afterDeath(e.getEntity(), e.getSource()));
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickItem.class, e -> {
			for (var o : UseItemCallback.EVENT.oyentes()) {
				InteractionResultHolder<ItemStack> r = o.interact(e.getEntity(), e.getLevel(), e.getHand());
				if (r.getResult() != InteractionResult.PASS) {
					e.setCancellationResult(r.getResult());
					e.setCanceled(true);
					return;
				}
			}
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickBlock.class, e -> {
			for (var o : UseBlockCallback.EVENT.oyentes()) {
				InteractionResult r = o.interact(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
				if (r != InteractionResult.PASS) {
					e.setCancellationResult(r);
					e.setCanceled(true);
					return;
				}
			}
		});
		juego.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.EntityInteract.class, e -> {
			for (var o : UseEntityCallback.EVENT.oyentes()) {
				InteractionResult r = o.interact(e.getEntity(), e.getLevel(), e.getHand(), e.getTarget(), null);
				if (r != InteractionResult.PASS) {
					e.setCancellationResult(r);
					e.setCanceled(true);
					return;
				}
			}
		});
		juego.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, e -> {
			if (!(e.getLevel() instanceof Level mundo)) return;
			var bloqueEntidad = mundo.getBlockEntity(e.getPos());
			for (var o : PlayerBlockBreakEvents.BEFORE.oyentes()) {
				if (!o.beforeBlockBreak(mundo, e.getPlayer(), e.getPos(), e.getState(), bloqueEntidad)) {
					e.setCanceled(true);
					return;
				}
			}
		});
		juego.addListener(EventPriority.NORMAL, false, RegisterCommandsEvent.class, e ->
				CommandRegistrationCallback.EVENT.oyentes().forEach(o -> o.register(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection())));
	}
}
