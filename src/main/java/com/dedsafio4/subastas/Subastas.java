package com.dedsafio4.subastas;

import com.dedsafio4.banco.Banco;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * /ah: la casa de subastas (menú MenuSubastas, pantalla SubastasScreen): pestañas Comprar, Vender, Mis ventas y
 * Recuperar. También se puede vender lo que tienes en la mano con /ah vender <precio>. El vendedor cobra al instante,
 * aunque no esté conectado. Lo que sacas de la venta queda en Recuperar hasta que lo tomes.
 */
public final class Subastas {
	private Subastas() {}

	/** Cuántas cosas puede tener a la venta cada jugador al mismo tiempo. */
	public static final int MAXIMO_POR_JUGADOR = 14;
	public static final long PRECIO_MAXIMO = 1_000_000_000L;

	public static final net.minecraft.world.inventory.MenuType<MenuSubastas> MENU = net.minecraft.core.Registry.register(
			net.minecraft.core.registries.BuiltInRegistries.MENU,
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "subastas"),
			new net.minecraft.world.inventory.MenuType<>(MenuSubastas::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

	/** Pantalla → servidor: poner a la venta lo de la casilla, a ese precio. */
	public record PrecioPayload(long precio) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<PrecioPayload> TYPE = new Type<>(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.dedsafio4.Dedsafio4.MOD_ID, "subastas_precio"));
		public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, PrecioPayload> CODEC =
				net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.map(PrecioPayload::new, PrecioPayload::precio);

		@Override
		public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** El menú y la red (al arrancar). */
	public static void registrarRed() {
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(PrecioPayload.TYPE, PrecioPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(PrecioPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> {
					if (contexto.player().containerMenu instanceof MenuSubastas menu) {
						menu.venderDesdeCasilla(Math.max(1, Math.min(PRECIO_MAXIMO, payload.precio())));
					}
				}));
	}

	static SubastasData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(SubastasData.FACTORY, "dedsafio4_subastas");
	}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ah")
				.executes(ctx -> {
					abrir(ctx.getSource().getPlayerOrException(), 0);
					return 1;
				})
				.then(Commands.literal("vender")
						.then(Commands.argument("precio", LongArgumentType.longArg(1, PRECIO_MAXIMO))
								.executes(ctx -> vender(ctx.getSource().getPlayerOrException(), LongArgumentType.getLong(ctx, "precio"))))));
	}

	public static void abrir(ServerPlayer jugador, int pagina) {
		jugador.openMenu(new SimpleMenuProvider((id, inventario, p) -> MenuSubastas.abrir(id, inventario),
				Component.literal("Subastas")));
	}

	/** /ah vender <precio>: lo que tiene en la mano. */
	private static int vender(ServerPlayer jugador, long precio) {
		ItemStack enMano = jugador.getMainHandItem();
		if (enMano.isEmpty()) {
			jugador.sendSystemMessage(Component.literal("Tienes que tener en la mano lo que quieres vender.").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!vender(jugador, enMano.copy(), precio)) return 0;
		enMano.setCount(0);
		return 1;
	}

	/** Pone a la venta ese objeto (que ya se le sacó al jugador si sale bien). Devuelve false si no se pudo. */
	static boolean vender(ServerPlayer jugador, ItemStack item, long precio) {
		SubastasData data = data(jugador.server);
		if (data.cuantasDe(jugador.getUUID()) >= MAXIMO_POR_JUGADOR) {
			jugador.sendSystemMessage(Component.literal("Ya tienes " + MAXIMO_POR_JUGADOR + " cosas a la venta. Saca alguna desde Mis ventas.").withStyle(ChatFormatting.RED));
			return false;
		}
		data.agregar(new SubastasData.Venta(UUID.randomUUID(), jugador.getUUID(), jugador.getGameProfile().getName(), item, precio,
				jugador.serverLevel().getGameTime()));
		jugador.sendSystemMessage(Component.literal("Pusiste a la venta ").withStyle(ChatFormatting.GREEN)
				.append(item.getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" x" + item.getCount() + " por " + precio + " deditas.").withStyle(ChatFormatting.GREEN)));
		jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
		return true;
	}

	/** Recuperar: el objeto número i vuelve al inventario (si no entra, queda guardado). */
	static void recuperar(ServerPlayer jugador, int i) {
		SubastasData data = data(jugador.server);
		ItemStack item = data.sacarDeRecuperar(jugador.getUUID(), i);
		if (item.isEmpty()) return;
		ItemStack copia = item.copy();
		if (!jugador.getInventory().add(copia)) {
			if (!copia.isEmpty()) data.guardarParaRecuperar(jugador.getUUID(), copia);
			jugador.displayClientMessage(Component.literal("No tienes lugar en el inventario.").withStyle(ChatFormatting.RED), true);
			return;
		}
		jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1f);
	}

	/** Comprar una venta (o sacarla, si es propia). Devuelve true si cambió algo. */
	static boolean tocar(ServerPlayer jugador, UUID idVenta) {
		SubastasData data = data(jugador.server);
		SubastasData.Venta venta = data.buscar(idVenta);
		if (venta == null) {
			jugador.sendSystemMessage(Component.literal("Esa venta ya no está.").withStyle(ChatFormatting.RED));
			return true;
		}
		if (venta.vendedor().equals(jugador.getUUID())) {
			// Es suya: la saca de la venta y queda en Recuperar.
			data.quitar(venta);
			data.guardarParaRecuperar(jugador.getUUID(), venta.item().copy());
			jugador.sendSystemMessage(Component.literal("Sacaste de la venta ").withStyle(ChatFormatting.YELLOW)
					.append(venta.item().getHoverName()).append(Component.literal(". Está en Recuperar.").withStyle(ChatFormatting.YELLOW)));
			return true;
		}
		if (!Banco.cobrar(jugador, venta.precio())) {
			jugador.sendSystemMessage(Component.literal("No te alcanzan las deditas: cuesta " + venta.precio()
					+ " y tienes " + Banco.saldo(jugador) + ".").withStyle(ChatFormatting.RED));
			jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1f);
			return false;
		}
		data.quitar(venta);
		darItem(jugador, venta.item().copy());
		Banco.sumar(jugador.server, venta.vendedor(), venta.precio());
		jugador.sendSystemMessage(Component.literal("Compraste ").withStyle(ChatFormatting.GREEN)
				.append(venta.item().getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" x" + venta.item().getCount() + " por " + venta.precio() + " deditas.").withStyle(ChatFormatting.GREEN)));
		jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
		ServerPlayer vendedor = jugador.server.getPlayerList().getPlayer(venta.vendedor());
		if (vendedor != null) {
			vendedor.sendSystemMessage(Component.literal(jugador.getGameProfile().getName() + " te compró ").withStyle(ChatFormatting.GOLD)
					.append(venta.item().getHoverName().copy().withStyle(ChatFormatting.WHITE))
					.append(Component.literal(" por " + venta.precio() + " deditas.").withStyle(ChatFormatting.GOLD)));
		}
		return true;
	}

	/** Al inventario; si no entra, al piso a sus pies. */
	private static void darItem(ServerPlayer jugador, ItemStack item) {
		if (!jugador.getInventory().add(item) && !item.isEmpty()) jugador.drop(item, false);
	}
}
