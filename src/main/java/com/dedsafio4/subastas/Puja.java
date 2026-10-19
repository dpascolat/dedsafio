package com.dedsafio4.subastas;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.banco.Banco;
import com.dedsafio4.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La Puja (subasta en vivo): un admin pone un objeto con /puja_crear <mínimo> (lo que tiene en la mano) y los
 * jugadores que tienen una Tarjeta de Subasta pujan deditas de su saldo del banco: con /pujar <cantidad>, o con click
 * derecho en el Bloque de Puja (puja lo justo: la puja actual más el mínimo). Cada puja tiene que superar a la anterior
 * por lo menos en el mínimo (de 1 a 20 deditas) y sólo se puede pujar lo que se tiene. Cada puja sale en el chat de
 * todos: "Jugador (saldo de Hermandad) ha pujado 82 [verde] 20 [azul]". Nadie paga hasta que el admin hace
 * /puja_final: ahí se le cobra al que ganó (si ya no le alcanza, al anterior que pueda pagar) y le llega el objeto
 * en un mensaje. /puja_cancelar la termina sin ganador y el objeto vuelve al admin.
 */
public final class Puja {
	private Puja() {}

	public static final String REMITENTE = "Subasta";
	public static final int MINIMO_MAX = 20;
	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	/** Las deditas en la fuente de íconos: roja (10000), verde (100) y la de siempre (1). */
	private static final String ICONO_ROJA = String.valueOf((char) 0xE00A), ICONO_VERDE = String.valueOf((char) 0xE009),
			ICONO_AZUL = String.valueOf((char) 0xE002);
	private static final int COLOR_NOMBRE = 0x55FFAA;

	/** Una puja: quién y cuánto. */
	public record Oferta(UUID jugador, String nombre, String hermandad, long cantidad) {}

	public static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean activa;
		ItemStack item = ItemStack.EMPTY;
		long minimo = 1;
		UUID creador;
		/** Todas las pujas, de la primera a la última (la última es la que va ganando). */
		final List<Oferta> ofertas = new ArrayList<>();

		public boolean estaActiva() {
			return activa;
		}

		public long actual() {
			return ofertas.isEmpty() ? 0 : ofertas.get(ofertas.size() - 1).cantidad();
		}

		/** Lo menos que se puede pujar ahora. */
		public long siguiente() {
			return ofertas.isEmpty() ? minimo : actual() + minimo;
		}

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.activa = tag.getBoolean("activa");
			if (tag.contains("item")) d.item = ItemStack.parse(registros, tag.getCompound("item")).orElse(ItemStack.EMPTY);
			d.minimo = Math.max(1, tag.getLong("minimo"));
			if (tag.hasUUID("creador")) d.creador = tag.getUUID("creador");
			for (Tag t : tag.getList("ofertas", Tag.TAG_COMPOUND)) {
				CompoundTag o = (CompoundTag) t;
				d.ofertas.add(new Oferta(o.getUUID("jugador"), o.getString("nombre"), o.getString("hermandad"), o.getLong("cantidad")));
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("activa", activa);
			if (!item.isEmpty()) tag.put("item", item.save(registros));
			tag.putLong("minimo", minimo);
			if (creador != null) tag.putUUID("creador", creador);
			ListTag lista = new ListTag();
			for (Oferta o : ofertas) {
				CompoundTag c = new CompoundTag();
				c.putUUID("jugador", o.jugador());
				c.putString("nombre", o.nombre());
				c.putString("hermandad", o.hermandad());
				c.putLong("cantidad", o.cantidad());
				lista.add(c);
			}
			tag.put("ofertas", lista);
			return tag;
		}
	}

	public static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_puja");
	}

	// ---------------------------------------------------------------- Deditas con íconos

	/** "82 [verde] 20 [azul]": la cantidad en deditas rojas (10000), verdes (100) y normales (1), con sus íconos. */
	public static MutableComponent cantidad(long deditas) {
		long rojas = deditas / 10000, verdes = deditas % 10000 / 100, azules = deditas % 100;
		MutableComponent c = Component.empty();
		boolean algo = false;
		if (rojas > 0) {
			parte(c, rojas, ICONO_ROJA);
			algo = true;
		}
		if (verdes > 0) {
			if (algo) c.append(" ");
			parte(c, verdes, ICONO_VERDE);
			algo = true;
		}
		if (azules > 0 || !algo) {
			if (algo) c.append(" ");
			parte(c, azules, ICONO_AZUL);
		}
		return c;
	}

	private static void parte(MutableComponent c, long n, String icono) {
		c.append(Component.literal(n + " ").withStyle(ChatFormatting.WHITE));
		c.append(Component.literal(icono).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS).withColor(ChatFormatting.WHITE)));
	}

	// ---------------------------------------------------------------- Comandos

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("puja_crear").requires(s -> s.hasPermission(2))
				.then(Commands.argument("minimo", IntegerArgumentType.integer(1, MINIMO_MAX))
						.executes(c -> crear(c, IntegerArgumentType.getInteger(c, "minimo")))));
		dispatcher.register(Commands.literal("puja_final").requires(s -> s.hasPermission(2)).executes(Puja::finalizar));
		dispatcher.register(Commands.literal("puja_cancelar").requires(s -> s.hasPermission(2)).executes(Puja::cancelar));
		dispatcher.register(Commands.literal("pujar")
				.then(Commands.argument("cantidad", LongArgumentType.longArg(1))
						.executes(c -> {
							pujar(c.getSource().getPlayerOrException(), LongArgumentType.getLong(c, "cantidad"));
							return 1;
						})));
	}

	private static int crear(CommandContext<CommandSourceStack> c, int minimo) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer admin = c.getSource().getPlayerOrException();
		Datos d = datos(admin.server);
		if (d.activa) {
			c.getSource().sendFailure(Component.literal("Ya hay una puja abierta: termínala con /puja_final o /puja_cancelar."));
			return 0;
		}
		ItemStack mano = admin.getItemInHand(InteractionHand.MAIN_HAND);
		if (mano.isEmpty()) {
			c.getSource().sendFailure(Component.literal("Ten en la mano el objeto que quieres subastar."));
			return 0;
		}
		d.activa = true;
		d.item = mano.copy();
		d.minimo = minimo;
		d.creador = admin.getUUID();
		d.ofertas.clear();
		d.setDirty();
		admin.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		admin.server.getPlayerList().broadcastSystemMessage(Component.literal("¡Empieza la puja por ").withStyle(ChatFormatting.GOLD)
				.append(nombreItem(d.item)).append(Component.literal("! Se puja desde ").withStyle(ChatFormatting.GOLD))
				.append(cantidad(minimo)).append(Component.literal(" con /pujar o en el Bloque de Puja.").withStyle(ChatFormatting.GOLD)), false);
		return 1;
	}

	private static Component nombreItem(ItemStack item) {
		MutableComponent n = item.getHoverName().copy().withStyle(ChatFormatting.AQUA);
		return item.getCount() > 1 ? n.append(Component.literal(" x" + item.getCount()).withStyle(ChatFormatting.AQUA)) : n;
	}

	/** Puja por el jugador. Devuelve false (y le dice por qué) si no puede. */
	public static boolean pujar(ServerPlayer jugador, long cuanto) {
		Datos d = datos(jugador.server);
		if (!d.activa) {
			jugador.sendSystemMessage(Component.literal("No hay ninguna puja abierta ahora.").withStyle(ChatFormatting.RED));
			return false;
		}
		if (!jugador.getInventory().contains(new ItemStack(ModItems.TARJETA_SUBASTA))) {
			jugador.sendSystemMessage(Component.literal("Necesitas una Tarjeta de Subasta para pujar.").withStyle(ChatFormatting.RED));
			return false;
		}
		if (cuanto < d.siguiente()) {
			jugador.sendSystemMessage(Component.literal("Tienes que pujar por lo menos ").withStyle(ChatFormatting.RED)
					.append(cantidad(d.siguiente())).append(Component.literal(".").withStyle(ChatFormatting.RED)));
			return false;
		}
		if (Banco.saldo(jugador) < cuanto) {
			jugador.sendSystemMessage(Component.literal("No tienes suficientes deditas: tienes ").withStyle(ChatFormatting.RED)
					.append(cantidad(Banco.saldo(jugador))).append(Component.literal(" en el banco.").withStyle(ChatFormatting.RED)));
			return false;
		}
		String hermandad = com.dedsafio4.hermandad.Hermandades.data(jugador).deJugador(jugador.getUUID())
				.map(h -> h.nombre()).orElse("");
		Oferta o = new Oferta(jugador.getUUID(), jugador.getGameProfile().getName(), hermandad, cuanto);
		d.ofertas.add(o);
		d.setDirty();
		jugador.server.getPlayerList().broadcastSystemMessage(mensajePuja(o), false);
		jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
		return true;
	}

	/** "Farfadox (saldo de Team Mafia) ha pujado 82 [verde] 20 [azul]". */
	private static Component mensajePuja(Oferta o) {
		String quien = o.nombre() + (o.hermandad().isEmpty() ? "" : " (saldo de " + o.hermandad() + ")");
		return Component.empty()
				.append(Component.literal(quien).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_NOMBRE))))
				.append(Component.literal(" ha pujado ").withStyle(ChatFormatting.WHITE))
				.append(cantidad(o.cantidad()));
	}

	private static int finalizar(CommandContext<CommandSourceStack> c) {
		MinecraftServer server = c.getSource().getServer();
		Datos d = datos(server);
		if (!d.activa) {
			c.getSource().sendFailure(Component.literal("No hay ninguna puja abierta."));
			return 0;
		}
		// Gana la puja más alta que se pueda pagar (si al último ya no le alcanza, el anterior, y así).
		Oferta ganadora = null;
		for (int i = d.ofertas.size() - 1; i >= 0 && ganadora == null; i--) {
			Oferta o = d.ofertas.get(i);
			if (Banco.cobrar(server, o.jugador(), o.cantidad())) ganadora = o;
		}
		ItemStack item = d.item.copy();
		cerrar(d);
		if (ganadora == null) {
			devolver(server, item);
			server.getPlayerList().broadcastSystemMessage(Component.literal("La puja por ").withStyle(ChatFormatting.GOLD)
					.append(nombreItem(item)).append(Component.literal(" terminó sin ganador.").withStyle(ChatFormatting.GOLD)), false);
			return 1;
		}
		String que = item.getHoverName().getString() + (item.getCount() > 1 ? " x" + item.getCount() : "");
		com.dedsafio4.correo.Correo.mandar(server, ganadora.jugador(), REMITENTE, "Ganaste la puja: " + que,
				"¡Ganaste la puja por " + que + "! Pagaste " + ganadora.cantidad() + " deditas de tu saldo del banco. Aquí tienes tu premio.",
				List.of(item));
		server.getPlayerList().broadcastSystemMessage(Component.empty()
				.append(Component.literal("¡" + ganadora.nombre() + " ganó la puja por ").withStyle(ChatFormatting.GOLD))
				.append(nombreItem(item)).append(Component.literal(" con ").withStyle(ChatFormatting.GOLD))
				.append(cantidad(ganadora.cantidad())).append(Component.literal("!").withStyle(ChatFormatting.GOLD)), false);
		ServerPlayer conectado = server.getPlayerList().getPlayer(ganadora.jugador());
		if (conectado != null) {
			conectado.sendSystemMessage(Component.literal("¡Ganaste la puja! Te llegó un mensaje con tu premio.").withStyle(ChatFormatting.GREEN));
			conectado.serverLevel().playSound(null, conectado.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
		}
		return 1;
	}

	private static int cancelar(CommandContext<CommandSourceStack> c) {
		MinecraftServer server = c.getSource().getServer();
		Datos d = datos(server);
		if (!d.activa) {
			c.getSource().sendFailure(Component.literal("No hay ninguna puja abierta."));
			return 0;
		}
		ItemStack item = d.item.copy();
		cerrar(d);
		devolver(server, item);
		server.getPlayerList().broadcastSystemMessage(Component.literal("Se canceló la puja por ").withStyle(ChatFormatting.GOLD)
				.append(nombreItem(item)).append(Component.literal(".").withStyle(ChatFormatting.GOLD)), false);
		return 1;
	}

	private static void cerrar(Datos d) {
		UUID creador = d.creador;
		d.activa = false;
		d.item = ItemStack.EMPTY;
		d.ofertas.clear();
		d.creador = creador;
		d.setDirty();
	}

	/** El objeto vuelve al admin que lo puso (en un mensaje). */
	private static void devolver(MinecraftServer server, ItemStack item) {
		UUID creador = datos(server).creador;
		if (creador == null || item.isEmpty()) return;
		com.dedsafio4.correo.Correo.mandar(server, creador, REMITENTE, "Puja sin ganador",
				"La puja terminó sin ganador: aquí tienes de vuelta el objeto.", List.of(item));
	}
}
