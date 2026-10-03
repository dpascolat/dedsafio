package com.dedsafio4.banco;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import com.dedsafio4.items.ModItems;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** El Cajero: bloque, menú de ingresar dinero y el paquete para abrirlo. */
public final class ModCajero {
	private ModCajero() {}

	public static final Block CAJERO = Registry.register(BuiltInRegistries.BLOCK, id("cajero"),
			new CajeroBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 1200f)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(estado -> 6)));
	public static final Item CAJERO_ITEM = Registry.register(BuiltInRegistries.ITEM, id("cajero"),
			new BlockItem(CAJERO, new Item.Properties()));

	public static final MenuType<CajeroMenu> MENU = Registry.register(BuiltInRegistries.MENU, id("cajero"),
			new MenuType<>(CajeroMenu::new, FeatureFlags.VANILLA_SET));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Cliente → servidor: "Ingresar Dinero" en el Cajero de esa posición. */
	public record IngresarPayload(BlockPos pos) implements CustomPacketPayload {
		public static final Type<IngresarPayload> TYPE = new Type<>(id("cajero_ingresar"));
		public static final StreamCodec<ByteBuf, IngresarPayload> CODEC =
				BlockPos.STREAM_CODEC.map(IngresarPayload::new, IngresarPayload::pos);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: "Sacar" tantas Rojas, Verdes y Deditas del Cajero de esa posición. */
	public record SacarPayload(BlockPos pos, int rojas, int verdes, int deditas) implements CustomPacketPayload {
		public static final Type<SacarPayload> TYPE = new Type<>(id("cajero_sacar"));
		public static final StreamCodec<ByteBuf, SacarPayload> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, SacarPayload::pos,
				ByteBufCodecs.VAR_INT, SacarPayload::rojas,
				ByteBufCodecs.VAR_INT, SacarPayload::verdes,
				ByteBufCodecs.VAR_INT, SacarPayload::deditas,
				SacarPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Lo máximo que se saca de cada moneda por vez (un inventario lleno). */
	public static final int MAXIMO_POR_MONEDA = 36 * 99;

	public static void registrar() {
		PayloadTypeRegistry.playC2S().register(IngresarPayload.TYPE, IngresarPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SacarPayload.TYPE, SacarPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(IngresarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> abrirIngresar(context.player(), payload.pos())));
		ServerPlayNetworking.registerGlobalReceiver(SacarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> sacar(context.player(), payload)));
	}

	private static boolean cercaDelCajero(ServerPlayer jugador, BlockPos pos) {
		return jugador.level().getBlockState(pos).is(CAJERO) && jugador.distanceToSqr(pos.getCenter()) <= 64;
	}

	/** Descuenta del saldo y le da las monedas (si no entran en el inventario, quedan tiradas a sus pies). */
	private static void sacar(ServerPlayer jugador, SacarPayload pedido) {
		if (!cercaDelCajero(jugador, pedido.pos())) return;
		int[] cantidades = {pedido.rojas(), pedido.verdes(), pedido.deditas()};
		Item[] monedas = {ModItems.DEDITA_ROJA, ModItems.DEDITA_VERDE, ModItems.DEDITA};
		long costo = 0;
		for (int i = 0; i < 3; i++) {
			if (cantidades[i] < 0 || cantidades[i] > MAXIMO_POR_MONEDA) return;
			costo += cantidades[i] * CajeroMenu.valor(new ItemStack(monedas[i]));
		}
		if (costo <= 0) return;
		if (!Banco.cobrar(jugador, costo)) {
			jugador.displayClientMessage(Component.literal("No tienes suficientes deditas.").withColor(0xFF5555), true);
			return;
		}
		for (int i = 0; i < 3; i++) {
			for (int quedan = cantidades[i]; quedan > 0; ) {
				int tanda = Math.min(monedas[i].getDefaultMaxStackSize(), quedan);
				quedan -= tanda;
				ItemStack pila = new ItemStack(monedas[i], tanda);
				if (!jugador.getInventory().add(pila)) jugador.drop(pila, false);
			}
		}
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 0.9f);
	}

	/** Abre la grilla para ingresar deditas, si el jugador está al lado de un Cajero. */
	private static void abrirIngresar(ServerPlayer jugador, BlockPos pos) {
		if (!cercaDelCajero(jugador, pos)) return;
		ContainerLevelAccess acceso = ContainerLevelAccess.create(jugador.level(), pos);
		jugador.openMenu(new SimpleMenuProvider((id, inventario, j) -> new CajeroMenu(id, inventario, acceso),
				Component.literal("Cajero")));
	}
}
