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
 * /ah: la casa de subastas. Cada jugador pone a la venta lo que tiene en la mano (/ah vender <precio>) y los demás lo
 * compran con deditas desde el menú (/ah). El vendedor cobra al instante, aunque no esté conectado.
 */
public final class Subastas {
	private Subastas() {}

	/** Cuántas cosas puede tener a la venta cada jugador al mismo tiempo. */
	public static final int MAXIMO_POR_JUGADOR = 14;
	public static final long PRECIO_MAXIMO = 1_000_000_000L;

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
		jugador.openMenu(new SimpleMenuProvider((id, inventario, p) -> new MenuSubastas(id, inventario, (ServerPlayer) p, pagina),
				Component.literal("Subastas")));
	}

	private static int vender(ServerPlayer jugador, long precio) {
		ItemStack enMano = jugador.getMainHandItem();
		if (enMano.isEmpty()) {
			jugador.sendSystemMessage(Component.literal("Tenés que tener en la mano lo que querés vender.").withStyle(ChatFormatting.RED));
			return 0;
		}
		SubastasData data = data(jugador.server);
		if (data.cuantasDe(jugador.getUUID()) >= MAXIMO_POR_JUGADOR) {
			jugador.sendSystemMessage(Component.literal("Ya tenés " + MAXIMO_POR_JUGADOR + " cosas a la venta. Sacá alguna desde /ah.").withStyle(ChatFormatting.RED));
			return 0;
		}
		ItemStack item = enMano.copy();
		enMano.setCount(0);
		data.agregar(new SubastasData.Venta(UUID.randomUUID(), jugador.getUUID(), jugador.getGameProfile().getName(), item, precio,
				jugador.serverLevel().getGameTime()));
		jugador.sendSystemMessage(Component.literal("Pusiste a la venta ").withStyle(ChatFormatting.GREEN)
				.append(item.getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" x" + item.getCount() + " por " + precio + " deditas.").withStyle(ChatFormatting.GREEN)));
		jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
		return 1;
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
			// Es suya: la saca de la venta y le devuelve el ítem.
			data.quitar(venta);
			darItem(jugador, venta.item().copy());
			jugador.sendSystemMessage(Component.literal("Sacaste de la venta ").withStyle(ChatFormatting.YELLOW)
					.append(venta.item().getHoverName()).append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
			return true;
		}
		if (!Banco.cobrar(jugador, venta.precio())) {
			jugador.sendSystemMessage(Component.literal("No te alcanzan las deditas: cuesta " + venta.precio()
					+ " y tenés " + Banco.saldo(jugador) + ".").withStyle(ChatFormatting.RED));
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
