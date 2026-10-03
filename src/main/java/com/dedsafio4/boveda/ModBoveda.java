package com.dedsafio4.boveda;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.banco.Banco;
import com.dedsafio4.banco.CajeroMenu;
import com.dedsafio4.banco.ModCajero;
import com.dedsafio4.hermandad.Hermandades;
import com.dedsafio4.hermandad.HermandadesData;
import com.dedsafio4.items.ModItems;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * La Bóveda de la Hermandad: una puerta de 2x3 que se abre con click derecho. Adentro se depositan
 * deditas (físicas en las casillas, o virtuales de tu cuenta del banco) en el balance de tu Hermandad,
 * y el Maestro y los Líderes pueden retirarlas. Cada movimiento se avisa en el chat de la Hermandad.
 */
public final class ModBoveda {
	private ModBoveda() {}

	public static final Block BOVEDA = Registry.register(BuiltInRegistries.BLOCK, id("boveda"),
			new BovedaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8f, 1200f)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().noLootTable()
					.pushReaction(PushReaction.BLOCK).isSuffocating((e, m, p) -> false).isViewBlocking((e, m, p) -> false)));
	public static final Item BOVEDA_ITEM = Registry.register(BuiltInRegistries.ITEM, id("boveda"),
			new BlockItem(BOVEDA, new Item.Properties()) {
				@Override
				public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
					texto.add(Component.literal("Puerta de 2x3. Guarda las deditas de tu Hermandad.").withColor(0xC6CFD6));
				}
			});

	public static final BlockEntityType<BovedaBlockEntity> ENTIDAD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, id("boveda"),
			FabricBlockEntityTypeBuilder.create(BovedaBlockEntity::new, BOVEDA).build());

	public static final MenuType<BovedaMenu> MENU = Registry.register(BuiltInRegistries.MENU, id("boveda"),
			new MenuType<>(BovedaMenu::new, FeatureFlags.VANILLA_SET));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Cliente → servidor: "Depositar" o "Retirar" con las Rojas, Verdes y Deditas escritas. */
	public record AccionPayload(int accion, int rojas, int verdes, int deditas) implements CustomPacketPayload {
		public static final int DEPOSITAR = 0, RETIRAR = 1;
		public static final Type<AccionPayload> TYPE = new Type<>(id("boveda_accion"));
		public static final StreamCodec<ByteBuf, AccionPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, AccionPayload::accion,
				ByteBufCodecs.VAR_INT, AccionPayload::rojas,
				ByteBufCodecs.VAR_INT, AccionPayload::verdes,
				ByteBufCodecs.VAR_INT, AccionPayload::deditas,
				AccionPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor → cliente: el balance de la Hermandad para la pantalla de la Bóveda, y un aviso (puede ir vacío). */
	public record EstadoPayload(String hermandad, long balance, String mensaje, boolean error) implements CustomPacketPayload {
		public static final Type<EstadoPayload> TYPE = new Type<>(id("boveda_estado"));
		public static final StreamCodec<ByteBuf, EstadoPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, EstadoPayload::hermandad,
				ByteBufCodecs.VAR_LONG, EstadoPayload::balance,
				ByteBufCodecs.STRING_UTF8, EstadoPayload::mensaje,
				ByteBufCodecs.BOOL, EstadoPayload::error,
				EstadoPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Lo máximo que se escribe de cada moneda para depositar desde la cuenta. */
	public static final int MAXIMO_VIRTUAL = 1_000_000;

	public static void registrar() {
		PayloadTypeRegistry.playC2S().register(AccionPayload.TYPE, AccionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EstadoPayload.TYPE, EstadoPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AccionPayload.TYPE, (payload, context) ->
				context.server().execute(() -> alAccion(context.player(), payload)));
	}

	/** 12358 → "12.358" */
	public static String formatear(long cantidad) {
		return String.format(Locale.ROOT, "%,d", cantidad).replace(',', '.');
	}

	/** Abre la pantalla de la Bóveda (la puerta ya se abrió). */
	static void abrirMenu(ServerPlayer jugador, BlockPos pos) {
		Optional<HermandadesData.Hermandad> h = Hermandades.data(jugador).deJugador(jugador.getUUID());
		if (h.isEmpty() || jugador.distanceToSqr(pos.above().getCenter()) > 64) return;
		jugador.openMenu(new SimpleMenuProvider((id, inventario, j) -> new BovedaMenu(id, inventario, pos),
				Component.literal("Bóveda")));
		enviarEstado(jugador, h.get(), "", false);
	}

	private static void enviarEstado(ServerPlayer jugador, HermandadesData.Hermandad h, String mensaje, boolean error) {
		ServerPlayNetworking.send(jugador, new EstadoPayload(h.nombre(), h.balance(), mensaje, error));
	}

	/** A los demás miembros que tengan la Bóveda abierta les llega el balance nuevo. */
	private static void refrescarAbiertas(ServerPlayer quien, HermandadesData.Hermandad h) {
		for (ServerPlayer otro : quien.server.getPlayerList().getPlayers()) {
			if (otro != quien && otro.containerMenu instanceof BovedaMenu && h.esMiembro(otro.getUUID())) {
				enviarEstado(otro, h, "", false);
			}
		}
	}

	private static void alAccion(ServerPlayer jugador, AccionPayload accion) {
		if (!(jugador.containerMenu instanceof BovedaMenu menu) || !menu.stillValid(jugador)) return;
		Optional<HermandadesData.Hermandad> hermandad = Hermandades.data(jugador).deJugador(jugador.getUUID());
		if (hermandad.isEmpty()) {
			jugador.closeContainer();
			return;
		}
		HermandadesData.Hermandad h = hermandad.get();
		int[] cantidades = {accion.rojas(), accion.verdes(), accion.deditas()};
		for (int c : cantidades) if (c < 0 || c > MAXIMO_VIRTUAL) return;
		long escrito = cantidades[0] * 10_000L + cantidades[1] * 100L + cantidades[2];

		if (accion.accion() == AccionPayload.DEPOSITAR) depositar(jugador, menu, h, escrito);
		else if (accion.accion() == AccionPayload.RETIRAR) retirar(jugador, h, cantidades, escrito);
	}

	/** Lo de las casillas más lo escrito (que se descuenta de la cuenta del jugador). */
	private static void depositar(ServerPlayer jugador, BovedaMenu menu, HermandadesData.Hermandad h, long virtual) {
		long fisico = menu.valorCasillas();
		if (fisico + virtual <= 0) {
			error(jugador, h, "Pon deditas en las casillas o escribe cuántas depositar.");
			return;
		}
		if (virtual > 0 && !Banco.cobrar(jugador, virtual)) {
			error(jugador, h, "No tienes " + formatear(virtual) + " deditas en tu cuenta.");
			return;
		}
		menu.vaciarCasillas();
		long total = fisico + virtual;
		Hermandades.data(jugador).setBalance(h, h.balance() + total);
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.3f);
		Hermandades.avisarMiembros(jugador.server, h, jugador.getGameProfile().getName() + " colocó "
				+ formatear(total) + " deditas en la Bóveda.", ChatFormatting.GOLD);
		enviarEstado(jugador, h, "Depositaste " + formatear(total) + " deditas.", false);
		refrescarAbiertas(jugador, h);
	}

	/** Solo el Maestro y los Líderes. Las monedas van al inventario (si no entran, quedan a sus pies). */
	private static void retirar(ServerPlayer jugador, HermandadesData.Hermandad h, int[] cantidades, long costo) {
		if (!h.puedePublicar(jugador.getUUID())) {
			error(jugador, h, "Solo el Maestro y los Líderes pueden retirar.");
			return;
		}
		if (costo <= 0) {
			error(jugador, h, "Escribe cuántas monedas retirar.");
			return;
		}
		for (int c : cantidades) {
			if (c > ModCajero.MAXIMO_POR_MONEDA) {
				error(jugador, h, "Máximo " + ModCajero.MAXIMO_POR_MONEDA + " de cada moneda por vez.");
				return;
			}
		}
		if (costo > h.balance()) {
			error(jugador, h, "La Bóveda no tiene tantas deditas.");
			return;
		}
		Hermandades.data(jugador).setBalance(h, h.balance() - costo);
		Item[] monedas = {ModItems.DEDITA_ROJA, ModItems.DEDITA_VERDE, ModItems.DEDITA};
		for (int i = 0; i < 3; i++) {
			for (int quedan = cantidades[i]; quedan > 0; ) {
				int tanda = Math.min(monedas[i].getDefaultMaxStackSize(), quedan);
				quedan -= tanda;
				ItemStack pila = new ItemStack(monedas[i], tanda);
				if (!jugador.getInventory().add(pila)) jugador.drop(pila, false);
			}
		}
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 0.9f);
		Hermandades.avisarMiembros(jugador.server, h, jugador.getGameProfile().getName() + " sacó "
				+ formatear(costo) + " deditas de la Bóveda.", ChatFormatting.YELLOW);
		enviarEstado(jugador, h, "Retiraste " + formatear(costo) + " deditas.", false);
		refrescarAbiertas(jugador, h);
	}

	private static void error(ServerPlayer jugador, HermandadesData.Hermandad h, String mensaje) {
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.2f);
		enviarEstado(jugador, h, mensaje, true);
	}

	/** Cuánto vale una moneda para la Bóveda (las mismas que acepta el Cajero). */
	static long valor(ItemStack pila) {
		return CajeroMenu.valor(pila);
	}
}
