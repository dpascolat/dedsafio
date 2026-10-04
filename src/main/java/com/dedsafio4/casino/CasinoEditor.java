package com.dedsafio4.casino;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * El editor de premios del Casino (/casino editar, solo admins): cada figura tiene 5 casillas para el premio chico
 * (2 iguales) y 5 para el grande (3 iguales). En cada casilla se elige el ítem y la cantidad; cuando sale esa
 * figura se da UNA de las casillas llenas, al azar. Si una fila nunca se tocó, da el premio de siempre.
 */
public final class CasinoEditor {
	private CasinoEditor() {}

	public static final int CASILLAS = 5, MAXIMO = 64;

	/** Una fila del editor: figura, 2 o 3 iguales, si ya la eligieron (o es la de siempre) y sus premios. */
	public record Fila(String figura, int iguales, boolean propio, List<ItemStack> items) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Fila> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Fila::figura,
				ByteBufCodecs.VAR_INT, Fila::iguales,
				ByteBufCodecs.BOOL, Fila::propio,
				ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Fila::items,
				Fila::new);
	}

	/** Servidor → admin: abre (o actualiza) el editor con todas las filas. */
	public record AbrirPayload(List<Fila> filas) implements CustomPacketPayload {
		public static final Type<AbrirPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "casino_editor"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AbrirPayload> CODEC = StreamCodec.composite(
				Fila.CODEC.apply(ByteBufCodecs.list()), AbrirPayload::filas, AbrirPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Admin → servidor: los premios nuevos de una fila (o volver a los de siempre). */
	public record GuardarPayload(String figura, int iguales, boolean restaurar, List<ItemStack> items) implements CustomPacketPayload {
		public static final Type<GuardarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "casino_editor_guardar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GuardarPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, GuardarPayload::figura,
				ByteBufCodecs.VAR_INT, GuardarPayload::iguales,
				ByteBufCodecs.BOOL, GuardarPayload::restaurar,
				ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), GuardarPayload::items,
				GuardarPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(AbrirPayload.TYPE, AbrirPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(GuardarPayload.TYPE, GuardarPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GuardarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> guardar(context.player(), payload)));
	}

	/** Le manda al admin el editor con los premios de ahora. */
	public static void abrir(ServerPlayer jugador) {
		CasinoPremios.Modo m = CasinoPremios.modo(jugador.server);
		List<Fila> filas = new ArrayList<>();
		for (String figura : CasinoPremios.FIGURAS.values()) {
			for (int iguales = 2; iguales <= 3; iguales++) {
				List<ItemStack> propio = m.propio(figura, iguales);
				List<ItemStack> items = propio != null ? propio : CasinoPremios.porDefecto(jugador.server, figura, iguales);
				filas.add(new Fila(figura, iguales, propio != null, items.stream().limit(CASILLAS).map(ItemStack::copy).toList()));
			}
		}
		ServerPlayNetworking.send(jugador, new AbrirPayload(filas));
	}

	private static void guardar(ServerPlayer jugador, GuardarPayload p) {
		if (!jugador.hasPermissions(2)) return;
		if (!CasinoPremios.FIGURAS.containsValue(p.figura()) || p.iguales() < 2 || p.iguales() > 3) return;
		CasinoPremios.Modo m = CasinoPremios.modo(jugador.server);
		String clave = p.figura() + "|" + p.iguales();
		if (p.restaurar()) {
			m.propios.remove(clave);
		} else {
			List<ItemStack> lista = new ArrayList<>();
			for (ItemStack item : p.items()) {
				if (item.isEmpty() || lista.size() >= CASILLAS) continue;
				ItemStack copia = item.copy();
				copia.setCount(Mth.clamp(item.getCount(), 1, MAXIMO));
				lista.add(copia);
			}
			m.propios.put(clave, lista);
		}
		m.setDirty();
		abrir(jugador);   // el editor se actualiza con lo guardado
	}
}
