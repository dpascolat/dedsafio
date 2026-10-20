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
 * Recuperar. También se puede vender lo que tienes en la mano con /ah vender <precio>. Al comprar, al comprador le
 * llega un mensaje de la Subasta al Buzón ("Compraste ..."); al vendedor también ("Vendiste ..."), con las Deditas
 * de la venta en mano adentro (aunque no esté conectado). Lo que sacas de la venta queda en Recuperar hasta que lo tomes.
 * Un admin la cierra con /ahah desactivar: a cada jugador le llega en un mensaje todo lo que tenía a la venta y en
 * Recuperar, y nadie la puede usar hasta /ahah activar.
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
		dispatcher.register(Commands.literal("ahah").requires(s -> s.hasPermission(2))
				.executes(ctx -> {
					boolean cerrada = data(ctx.getSource().getServer()).cerrada();
					ctx.getSource().sendSuccess(() -> Component.literal(cerrada ? "La Subasta está cerrada." : "La Subasta está abierta.")
							.withStyle(ChatFormatting.GOLD), false);
					return 1;
				})
				.then(Commands.literal("activar").executes(ctx -> {
					data(ctx.getSource().getServer()).setCerrada(false);
					ctx.getSource().sendSuccess(() -> Component.literal("Listo: la Subasta (/ah) está abierta otra vez.").withStyle(ChatFormatting.GREEN), true);
					return 1;
				}))
				.then(Commands.literal("desactivar").executes(ctx -> cerrar(ctx.getSource()))));
	}

	/** /ahah desactivar: cierra la Subasta y le devuelve a cada uno lo suyo en mensajes (27 casillas por mensaje). */
	private static int cerrar(CommandSourceStack fuente) {
		MinecraftServer server = fuente.getServer();
		SubastasData data = data(server);
		data.setCerrada(true);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.containerMenu instanceof MenuSubastas) p.closeContainer();
		java.util.Map<UUID, java.util.List<ItemStack>> todo = data.vaciar();
		int objetos = 0;
		for (var e : todo.entrySet()) {
			java.util.List<ItemStack> l = e.getValue();
			objetos += l.size();
			for (int i = 0; i < l.size(); i += com.dedsafio4.correo.Correo.CASILLAS) {
				java.util.List<ItemStack> parte = l.subList(i, Math.min(l.size(), i + com.dedsafio4.correo.Correo.CASILLAS));
				com.dedsafio4.correo.Correo.mandar(server, e.getKey(), REMITENTE, "Subasta cerrada",
						"La Subasta se cerró. Aquí tienes de vuelta lo que tenías a la venta y lo que te quedaba por recuperar.", parte);
			}
		}
		int cuantos = todo.size(), total = objetos;
		fuente.sendSuccess(() -> Component.literal("Listo: la Subasta está cerrada. Se devolvieron " + total + " objetos a "
				+ cuantos + " jugadores en mensajes.").withStyle(ChatFormatting.GOLD), true);
		return 1;
	}

	/** Si la Subasta está cerrada, se lo dice al jugador. */
	private static boolean cerradaPara(ServerPlayer jugador) {
		if (!data(jugador.server).cerrada()) return false;
		jugador.sendSystemMessage(Component.literal("La Subasta está cerrada por ahora.").withStyle(ChatFormatting.RED));
		return true;
	}

	public static void abrir(ServerPlayer jugador, int pagina) {
		if (cerradaPara(jugador)) return;
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
		if (cerradaPara(jugador)) return false;
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
		jugador.sendSystemMessage(Component.literal("Compraste ").withStyle(ChatFormatting.GREEN)
				.append(venta.item().getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" x" + venta.item().getCount() + " por " + venta.precio() + " deditas.").withStyle(ChatFormatting.GREEN)));
		jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
		// Los mensajes de la Subasta: al que compró, y al que vendió con sus deditas en mano.
		String que = venta.item().getHoverName().getString() + " x" + venta.item().getCount();
		com.dedsafio4.correo.Correo.mandar(jugador.server, jugador.getUUID(), REMITENTE, "Compraste " + que,
				"Compraste " + que + " por " + venta.precio() + " deditas en el /ah (lo vendía " + venta.nombreVendedor() + ").",
				java.util.List.of());
		java.util.List<ItemStack> deditas = new java.util.ArrayList<>();
		long sobra = enDeditas(venta.precio(), deditas);
		if (sobra > 0) Banco.sumar(jugador.server, venta.vendedor(), sobra);
		com.dedsafio4.correo.Correo.mandar(jugador.server, venta.vendedor(), REMITENTE, "Vendiste " + que,
				jugador.getGameProfile().getName() + " te compró " + que + " por " + venta.precio() + " deditas en el /ah. Aquí tienes tus deditas."
						+ (sobra > 0 ? " (" + sobra + " no entraban en el mensaje: van a tu saldo del banco.)" : ""),
				deditas);
		return true;
	}

	/** Quién firma los mensajes de compra y venta. */
	public static final String REMITENTE = "Subasta";

	/**
	 * Pasa una cantidad a deditas en mano (Rojas de 10.000, Verdes de 100 y Deditas de 1, de a 99 por casilla). Lo que
	 * no entra en las casillas de un mensaje se devuelve (va al saldo del banco).
	 */
	static long enDeditas(long cantidad, java.util.List<ItemStack> pilas) {
		long[] valores = {10_000, 100, 1};
		net.minecraft.world.item.Item[] items = {com.dedsafio4.items.ModItems.DEDITA_ROJA, com.dedsafio4.items.ModItems.DEDITA_VERDE,
				com.dedsafio4.items.ModItems.DEDITA};
		long resto = cantidad;
		for (int i = 0; i < valores.length; i++) {
			long cuantas = resto / valores[i];
			while (cuantas > 0 && pilas.size() < com.dedsafio4.correo.Correo.CASILLAS) {
				int esta = (int) Math.min(99, cuantas);
				pilas.add(new ItemStack(items[i], esta));
				cuantas -= esta;
				resto -= esta * valores[i];
			}
		}
		return resto;
	}

	/** Al inventario; si no entra, al piso a sus pies. */
	private static void darItem(ServerPlayer jugador, ItemStack item) {
		if (!jugador.getInventory().add(item) && !item.isEmpty()) jugador.drop(item, false);
	}
}
