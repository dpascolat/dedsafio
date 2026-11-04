package com.dedsafio4.cofres;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * "/item_desaparecer <tiempo>" con un ítem en la mano: ese ítem desaparece cuando pasa ese tiempo. El reloj arranca
 * cuando el ítem está en el inventario de un jugador (así, puesto en un cofre rellenable, a cada uno le empieza a
 * contar cuando lo agarra). En la descripción aparece cuánto le falta. "/item_desaparecer quitar" se lo saca.
 * Se guarda en el ítem (custom_data "dedsafio4_desaparece": dura = cuánto dura, fin = cuándo desaparece, en ticks
 * del mundo).
 */
public final class ItemsQueDesaparecen {
	private ItemsQueDesaparecen() {}

	private static final String CLAVE = "dedsafio4_desaparece";

	private static CompoundTag datos(ItemStack pila) {
		CustomData c = pila.get(DataComponents.CUSTOM_DATA);
		if (c == null || !c.contains(CLAVE)) return null;
		return c.copyTag().getCompound(CLAVE);
	}

	/** El mismo ítem sin el reloj ya arrancado (se vuelve a contar desde que alguien lo agarre). */
	public static ItemStack sinReloj(ItemStack pila) {
		CompoundTag d = datos(pila);
		if (d != null && d.contains("fin")) CustomData.update(DataComponents.CUSTOM_DATA, pila, tag -> tag.getCompound(CLAVE).remove("fin"));
		return pila;
	}

	/** "30s", "5m", "2h", "1d", "1h30m"... (sólo un número son segundos). En ticks; 0 si no se entiende. */
	public static long leerTiempo(String texto) {
		texto = texto.trim().toLowerCase(java.util.Locale.ROOT);
		if (texto.matches("\\d+")) return Long.parseLong(texto) * 20;
		var m = java.util.regex.Pattern.compile("(\\d+)([smhd])").matcher(texto);
		long total = 0;
		int fin = 0;
		while (m.find()) {
			if (m.start() != fin) return 0;
			long n = Long.parseLong(m.group(1));
			total += n * switch (m.group(2)) {
				case "s" -> 1;
				case "m" -> 60;
				case "h" -> 3600;
				default -> 86400;
			};
			fin = m.end();
		}
		return fin == texto.length() ? total * 20 : 0;
	}

	/** Un tiempo en ticks escrito lindo: "1d 2h", "4m 32s", "8s". */
	public static String escribirTiempo(long ticks) {
		long s = Math.max(0, (ticks + 19) / 20);
		long d = s / 86400, h = s / 3600 % 24, m = s / 60 % 60, seg = s % 60;
		StringBuilder b = new StringBuilder();
		if (d > 0) b.append(d).append("d ");
		if (h > 0) b.append(h).append("h ");
		if (m > 0 && d == 0) b.append(m).append("m ");
		if (d == 0 && h == 0 && (seg > 0 || b.isEmpty())) b.append(seg).append("s");
		return b.toString().trim();
	}

	/** La línea de la descripción (null si el ítem no desaparece). jugador puede ser null. */
	public static Component linea(ItemStack pila, Player jugador) {
		CompoundTag d = datos(pila);
		if (d == null) return null;
		if (d.contains("fin") && jugador != null) {
			long falta = d.getLong("fin") - jugador.level().getGameTime();
			return Component.literal("⌛ Desaparece en " + escribirTiempo(falta))
					.withStyle(falta < 20 * 60 ? ChatFormatting.RED : ChatFormatting.GOLD);
		}
		return Component.literal("⌛ Desaparece " + escribirTiempo(d.getLong("dura")) + " después de agarrarlo").withStyle(ChatFormatting.GOLD);
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			long ahora = CofresRellenables.ahora(server);
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (jugador.isSpectator()) continue;
				Inventory inv = jugador.getInventory();
				for (int i = 0; i < inv.getContainerSize(); i++) {
					ItemStack pila = inv.getItem(i);
					if (revisar(jugador, pila, ahora)) inv.setItem(i, ItemStack.EMPTY);
				}
				ItemStack mano = jugador.containerMenu.getCarried();
				if (revisar(jugador, mano, ahora)) jugador.containerMenu.setCarried(ItemStack.EMPTY);
			}
		});
	}

	/** Arranca el reloj si no tenía; true si ya se le terminó el tiempo (hay que sacarlo). */
	private static boolean revisar(ServerPlayer jugador, ItemStack pila, long ahora) {
		if (pila.isEmpty()) return false;
		CompoundTag d = datos(pila);
		if (d == null) return false;
		if (!d.contains("fin")) {
			long fin = ahora + d.getLong("dura");
			CustomData.update(DataComponents.CUSTOM_DATA, pila, tag -> tag.getCompound(CLAVE).putLong("fin", fin));
			return false;
		}
		if (ahora < d.getLong("fin")) return false;
		jugador.displayClientMessage(Component.literal("✖ ").append(pila.getHoverName()).append(" desapareció.").withStyle(ChatFormatting.GRAY), true);
		return true;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("item_desaparecer").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar").executes(c -> cambiar(c, 0)))
				.then(Commands.argument("tiempo", StringArgumentType.word()).executes(c -> {
					long ticks = leerTiempo(StringArgumentType.getString(c, "tiempo"));
					if (ticks <= 0) {
						c.getSource().sendFailure(Component.literal("Tiempo no válido. Ejemplos: 30s, 5m, 2h, 1d o 1h30m."));
						return 0;
					}
					return cambiar(c, ticks);
				})));
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, long ticks) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer jugador = c.getSource().getPlayerOrException();
		ItemStack pila = jugador.getMainHandItem();
		if (pila.isEmpty()) {
			c.getSource().sendFailure(Component.literal("Agarra en la mano el ítem."));
			return 0;
		}
		CustomData.update(DataComponents.CUSTOM_DATA, pila, tag -> {
			if (ticks <= 0) tag.remove(CLAVE);
			else {
				CompoundTag d = new CompoundTag();
				d.putLong("dura", ticks);
				tag.put(CLAVE, d);
			}
		});
		c.getSource().sendSuccess(() -> Component.literal(ticks <= 0 ? "Ese ítem ya no desaparece."
				: "Ese ítem desaparece " + escribirTiempo(ticks) + " después de agarrarlo (el reloj arranca en el inventario).")
				.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	/** Para el mixin de la descripción. */
	public static List<Component> conLinea(List<Component> lineas, ItemStack pila, Player jugador) {
		Component l = linea(pila, jugador);
		if (l == null) return lineas;
		List<Component> nuevas = new java.util.ArrayList<>(lineas);
		nuevas.add(Math.min(1, nuevas.size()), l);
		return nuevas;
	}
}
