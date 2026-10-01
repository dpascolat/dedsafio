package com.dedsafio4.catalogo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Las Misiones (la pestaña del pergamino del Catálogo): "Misión Guía | + N" con un ítem para crear. Se
 * cumplen al craftear ese ítem (lo cuentan las estadísticas de Minecraft, así queda guardado); entonces la
 * tarjeta se pone verde con "¡Misión completada!". Las deditas que dice no se dan solas.
 */
public final class Misiones {
	private Misiones() {}

	/** Una misión: el ítem, cuántas hay que crear, la recompensa que muestra, y la descripción. */
	public record Mision(Item item, int cantidad, int deditas, String antes, String despues) {
		/** "Crea un " + [Casco de Diamante] + ". Son muy importantes..." */
		public Component descripcion() {
			return Component.literal(antes).withColor(0xFFFFFF)
					.append(Component.translatable(item.getDescriptionId()).withColor(0x6FA8FF))
					.append(Component.literal(despues).withColor(0xFFFFFF));
		}
	}

	private static final String RULETA = ". Son muy importantes para sobrevivir a los peligros de la Ruleta.";
	private static List<Mision> lista;

	public static List<Mision> lista() {
		if (lista == null) lista = List.of(
				new Mision(Items.DIAMOND_HELMET, 1, 2, "Crea un ", RULETA),
				new Mision(Items.DIAMOND_CHESTPLATE, 1, 3, "Crea una ", RULETA),
				new Mision(Items.DIAMOND_LEGGINGS, 1, 3, "Crea unos ", RULETA),
				new Mision(Items.DIAMOND_BOOTS, 1, 2, "Crea unas ", RULETA),
				new Mision(ModItems.CANDADO, 1, 2, "Crea un ", ". Utilízalo para proteger tus cofres."));
		return lista;
	}

	// --- Progreso ---

	/** En el cliente: cuánto lleva de cada misión (lo manda el servidor). */
	public static volatile int[] PROGRESO_CLIENTE = new int[0];

	public static int progresoCliente(int i) {
		int[] p = PROGRESO_CLIENTE;
		return i < p.length ? p[i] : 0;
	}

	public record Payload(List<Integer> progreso) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "misiones"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).<RegistryFriendlyByteBuf>cast().map(Payload::new, Payload::progreso);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static List<Integer> progreso(ServerPlayer p) {
		List<Integer> l = new ArrayList<>();
		for (Mision m : lista()) l.add(Math.min(m.cantidad(), p.getStats().getValue(Stats.ITEM_CRAFTED.get(m.item()))));
		return l;
	}

	/** Lo último que se le mandó a cada jugador (para avisar solo cuando cambia). */
	private static final Map<UUID, List<Integer>> ENVIADO = new HashMap<>();

	private static void revisar(ServerPlayer p, boolean avisar) {
		List<Integer> ahora = progreso(p), antes = ENVIADO.get(p.getUUID());
		if (ahora.equals(antes)) return;
		if (avisar && antes != null) {
			for (int i = 0; i < ahora.size(); i++) {
				Mision m = lista().get(i);
				if (ahora.get(i) >= m.cantidad() && antes.get(i) < m.cantidad()) {
					p.displayClientMessage(Component.literal("¡Misión completada! ").withColor(0x7CFC6A)
							.append(Component.translatable(m.item().getDescriptionId()).withColor(0x6FA8FF)), true);
					p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
				}
			}
		}
		ENVIADO.put(p.getUUID(), ahora);
		ServerPlayNetworking.send(p, new Payload(ahora));
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ENVIADO.remove(handler.player.getUUID());
			revisar(handler.player, false);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ENVIADO.remove(handler.player.getUUID()));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) revisar(p, true);
		});
	}
}
