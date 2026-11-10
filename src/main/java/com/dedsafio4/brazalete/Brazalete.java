package com.dedsafio4.brazalete;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.DescritoItem;
import com.dedsafio4.items.ModItems;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

/**
 * Brazalete Interdimensional: en la Mano Secundaria, con J se entra o se sale del Modo de Vuelo (como el vuelo de
 * creativo). El combustible es la barra de durabilidad: lleno alcanza para 5 minutos volando; solo se gasta mientras
 * se está volando de verdad. Si se acaba en el aire, Caída Lenta para no matarse. La Batería de Dilitio (cursor +
 * Click Derecho sobre el Brazalete en el inventario) recarga el 50%; es la misma que recarga la Linterna.
 * El Modo de Vuelo se recuerda con la marca dedsafio4_brazalete_vuelo; el vuelo solo se saca si lo puso el Brazalete
 * (no toca el de creativo ni el de otros mods, como el /fly del servidor).
 */
public final class Brazalete {
	private Brazalete() {}

	/** Segundos de vuelo con el combustible lleno (= durabilidad del ítem). */
	public static final int COMBUSTIBLE = 5 * 60;
	public static final String MARCA = "dedsafio4_brazalete_vuelo";
	private static final int DORADO = 0xF3AD24, GRIS = 0xC7C4C7, CELESTE = 0x49C8E5;

	/** Lo manda el cliente al apretar J. */
	public record TeclaPayload() implements CustomPacketPayload {
		public static final Type<TeclaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "brazalete_vuelo"));
		public static final StreamCodec<ByteBuf, TeclaPayload> CODEC = StreamCodec.unit(new TeclaPayload());

		@Override
		public Type<TeclaPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playC2S().register(TeclaPayload.TYPE, TeclaPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TeclaPayload.TYPE, (payload, contexto) -> {
			ServerPlayer jugador = contexto.player();
			jugador.server.execute(() -> alternar(jugador));
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) tick(jugador);
		});
	}

	private static boolean puedeVolarSolo(Player jugador) {
		return jugador.isCreative() || jugador.isSpectator();
	}

	public static int combustible(ItemStack brazalete) {
		return brazalete.getMaxDamage() - brazalete.getDamageValue();
	}

	private static void ponerCombustible(ItemStack brazalete, int segundos) {
		brazalete.setDamageValue(brazalete.getMaxDamage() - Math.max(0, Math.min(COMBUSTIBLE, segundos)));
	}

	/** J: entra o sale del Modo de Vuelo (solo con el Brazalete en la Mano Secundaria y con combustible). */
	private static void alternar(ServerPlayer jugador) {
		if (jugador.getTags().contains(MARCA)) {
			apagar(jugador, "Saliste del Modo de Vuelo.");
			return;
		}
		ItemStack brazalete = jugador.getOffhandItem();
		if (!brazalete.is(ModItems.BRAZALETE_INTERDIMENSIONAL)) {
			jugador.displayClientMessage(Component.literal("Coloca el Brazalete Interdimensional en tu Mano Secundaria.").withColor(DORADO), true);
			return;
		}
		if (combustible(brazalete) <= 0) {
			jugador.displayClientMessage(Component.literal("El Brazalete no tiene combustible. Recárgalo con una Batería de Dilitio.")
					.withStyle(ChatFormatting.RED), true);
			return;
		}
		jugador.addTag(MARCA);
		TrajeEspacial.avisar(jugador.server, jugador.getUUID(), true);
		if (!puedeVolarSolo(jugador)) {
			jugador.getAbilities().mayfly = true;
			jugador.onUpdateAbilities();
		}
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
		jugador.displayClientMessage(Component.literal("Modo de Vuelo activado (salta dos veces para volar).").withColor(CELESTE), true);
	}

	private static void apagar(ServerPlayer jugador, String mensaje) {
		jugador.removeTag(MARCA);
		TrajeEspacial.avisar(jugador.server, jugador.getUUID(), false);
		if (!puedeVolarSolo(jugador)) {
			boolean enElAire = jugador.getAbilities().flying;
			jugador.getAbilities().mayfly = false;
			jugador.getAbilities().flying = false;
			jugador.onUpdateAbilities();
			// Para no morir de la caída.
			if (enElAire) jugador.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 15 * 20, 0, false, true, true));
		}
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
		jugador.displayClientMessage(Component.literal(mensaje).withColor(DORADO), true);
	}

	private static void tick(ServerPlayer jugador) {
		if (!jugador.getTags().contains(MARCA)) return;
		ItemStack brazalete = jugador.getOffhandItem();
		if (!jugador.isAlive() || !brazalete.is(ModItems.BRAZALETE_INTERDIMENSIONAL)) {
			apagar(jugador, "Saliste del Modo de Vuelo (el Brazalete no está en tu Mano Secundaria).");
			return;
		}
		// Si algo le sacó el vuelo (por ejemplo, cambió de modo de juego), se lo vuelve a dar.
		if (!puedeVolarSolo(jugador) && !jugador.getAbilities().mayfly) {
			jugador.getAbilities().mayfly = true;
			jugador.onUpdateAbilities();
		}
		if (!jugador.getAbilities().flying || jugador.tickCount % 20 != 0 || jugador.isCreative()) return;
		int queda = combustible(brazalete) - 1;
		ponerCombustible(brazalete, queda);
		if (queda == 30) jugador.displayClientMessage(Component.literal("¡Queda poco combustible en el Brazalete!").withStyle(ChatFormatting.RED), true);
		if (queda <= 0) apagar(jugador, "El Brazalete se quedó sin combustible.");
	}

	// --- El Brazalete (ítem) ---

	public static class BrazaleteItem extends DescritoItem {
		public BrazaleteItem(Properties propiedades, Consumer<List<Component>> descripcion) {
			super(propiedades, descripcion);
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withStyle(color(DORADO));
		}

		@Override
		public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
			super.appendHoverText(pila, contexto, texto, bandera);
			texto.add(Component.empty());
			int porcentaje = Math.round(100f * combustible(pila) / COMBUSTIBLE);
			texto.add(parte("Combustible: ", GRIS).append(parte(porcentaje + "%", porcentaje <= 10 ? 0xFF5555 : DORADO)));
		}

		/** Que la barra de "durabilidad" (el combustible) se vea siempre, también llena. */
		@Override
		public boolean isBarVisible(ItemStack pila) {
			return true;
		}

		@Override
		public int getBarColor(ItemStack pila) {
			return CELESTE;
		}

		/** El Brazalete no se rompe ni se repara: el combustible solo lo llena la Batería. */
		@Override
		public boolean isValidRepairItem(ItemStack pila, ItemStack material) {
			return false;
		}

		/** Batería en el cursor + Click Derecho sobre el Brazalete: recarga. */
		@Override
		public boolean overrideOtherStackedOnMe(ItemStack brazalete, ItemStack otro, Slot lugar, ClickAction accion, Player jugador, SlotAccess cursor) {
			if (accion != ClickAction.SECONDARY || !otro.is(ModItems.BATERIA_DILITIO)) return false;
			return recargar(brazalete, otro, jugador);
		}
	}

	/** Suma el 50% del combustible y gasta una batería. Si ya está lleno, no gasta nada. */
	private static boolean recargar(ItemStack brazalete, ItemStack bateria, Player jugador) {
		if (combustible(brazalete) >= COMBUSTIBLE) {
			if (!jugador.level().isClientSide) {
				jugador.displayClientMessage(Component.literal("El Brazalete ya tiene el combustible lleno.").withColor(DORADO), true);
			}
			return true;
		}
		ponerCombustible(brazalete, combustible(brazalete) + COMBUSTIBLE / 2);
		bateria.shrink(1);
		jugador.playSound(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.8f);
		return true;
	}
}
