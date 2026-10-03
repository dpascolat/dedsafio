package com.dedsafio4.hermandad;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ManuscritoHermandadItem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fundar una Hermandad:
 * 1. En el Manuscrito se escribe el nombre y se inscriben al menos 3 jugadores.
 * 2. "Crear Hermandad" sella el Manuscrito (queda como libro firmado, ya no se edita).
 * 3. Con el Manuscrito sellado en la mano, el Maestro (el primer inscrito) usa /guild found:
 *    se funda la Hermandad y el Manuscrito se consume.
 * Después, desde la interfaz (tecla H) el Maestro invita y expulsa miembros.
 */
public final class Hermandades {
	private Hermandades() {}

	public static HermandadesData data(ServerPlayer jugador) {
		return jugador.server.overworld().getDataStorage().computeIfAbsent(HermandadesData.FACTORY, "dedsafio4_hermandades");
	}

	public static void alAccion(ServerPlayer jugador, ManuscritoAccionPayload accion) {
		InteractionHand mano = accion.manoPrincipal() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack manuscrito = jugador.getItemInHand(mano);
		if (!ManuscritoHermandadItem.es(manuscrito) || ManuscritoDatos.sellado(manuscrito)) return;

		String nombre = ManuscritoDatos.limpiarNombre(accion.nombre());
		if (!nombre.equals(ManuscritoDatos.nombre(manuscrito))) ManuscritoDatos.setNombre(manuscrito, nombre);

		switch (accion.accion()) {
			case ManuscritoAccionPayload.INSCRIBIRSE -> inscribirse(jugador, manuscrito);
			case ManuscritoAccionPayload.CREAR -> sellar(jugador, manuscrito, nombre);
			default -> {}
		}
	}

	private static void inscribirse(ServerPlayer jugador, ItemStack manuscrito) {
		if (ManuscritoDatos.estaInscrito(manuscrito, jugador.getUUID())) {
			error(jugador, "Ya estás inscrito en este Manuscrito.");
		} else if (ManuscritoDatos.inscritos(manuscrito).size() >= ManuscritoDatos.MAXIMO_INSCRITOS) {
			error(jugador, "El Manuscrito ya no tiene lugar para más firmas.");
		} else if (data(jugador).deJugador(jugador.getUUID()).isPresent()) {
			error(jugador, "Ya perteneces a una Hermandad.");
		} else {
			ManuscritoDatos.inscribir(manuscrito, jugador.getUUID(), jugador.getGameProfile().getName());
		}
	}

	private static void sellar(ServerPlayer jugador, ItemStack manuscrito, String nombre) {
		if (!puedeFundarse(jugador, manuscrito, nombre)) return;
		ManuscritoDatos.sellar(manuscrito);
		jugador.sendSystemMessage(Component.empty()
				.append(Component.literal("El manuscrito ha sido sellado. Sostenlo y usa /guild found para fundar ")
						.withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(nombre).withStyle(ChatFormatting.GOLD))
				.append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("guild")
				.then(Commands.literal("found").executes(ctx -> {
					fundar(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("accept").executes(ctx -> {
					aceptar(ctx.getSource().getPlayerOrException());
					return 1;
				})));
	}

	private static void fundar(ServerPlayer jugador) {
		ItemStack manuscrito = manuscritoSellado(jugador);
		if (manuscrito == null) {
			error(jugador, "Debes sostener un Manuscrito de Hermandad sellado.");
			return;
		}
		String nombre = ManuscritoDatos.nombre(manuscrito);
		// Se vuelve a validar: pudo pasar tiempo desde que se selló.
		if (!puedeFundarse(jugador, manuscrito, nombre)) return;

		List<ManuscritoDatos.Inscrito> inscritos = ManuscritoDatos.inscritos(manuscrito);
		// El Maestro es el primero que se inscribió; solo él puede fundarla.
		ManuscritoDatos.Inscrito maestro = inscritos.get(0);
		if (!maestro.uuid().equals(jugador.getUUID())) {
			error(jugador, "Solo el Maestro de la Hermandad (" + maestro.nombre() + ") puede usar /guild found.");
			return;
		}
		data(jugador).agregar(new HermandadesData.Hermandad(nombre, maestro.uuid(), inscritos));
		manuscrito.shrink(1);
		sincronizarJugadores(jugador.server);

		String miembros = inscritos.stream().map(ManuscritoDatos.Inscrito::nombre).collect(Collectors.joining(", "));
		jugador.server.getPlayerList().broadcastSystemMessage(Component.empty()
				.append(Component.literal("¡Se ha fundado la Hermandad ").withStyle(ChatFormatting.GOLD))
				.append(Component.literal(nombre).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
				.append(Component.literal("!").withStyle(ChatFormatting.GOLD))
				.append(Component.literal("\nMaestro: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(maestro.nombre()).withStyle(ChatFormatting.AQUA))
				.append(Component.literal("\nMiembros: " + miembros).withStyle(ChatFormatting.GRAY)), false);
	}

	// ---------------------------------------------------------------- Interfaz (tecla H)

	/** Invitaciones pendientes: jugador invitado → nombre de la Hermandad. Se pierden al reiniciar el server. */
	private static final Map<UUID, String> INVITACIONES = new HashMap<>();

	public static void alAccionInterfaz(ServerPlayer jugador, HermandadAccionPayload accion) {
		Optional<HermandadesData.Hermandad> hermandad = data(jugador).deJugador(jugador.getUUID());
		if (hermandad.isEmpty()) {
			error(jugador, "No perteneces a ninguna Hermandad.");
			return;
		}
		switch (accion.accion()) {
			case HermandadAccionPayload.PEDIR_INFO -> {
				enviarInfo(jugador, hermandad.get());
				ServerPlayNetworking.send(jugador, new HermandadChatPayload(hermandad.get().chat()));
				enviarEstandartes(jugador, hermandad.get());
				ServerPlayNetworking.send(jugador, new HermandadTablonPayload(hermandad.get().anuncios()));
			}
			case HermandadAccionPayload.ANUNCIO_PUBLICAR -> publicarAnuncio(jugador, hermandad.get(), accion.texto());
			case HermandadAccionPayload.ANUNCIO_BORRAR -> borrarAnuncio(jugador, hermandad.get(), accion.texto());
			case HermandadAccionPayload.SUBIR_RANGO -> cambiarRango(jugador, hermandad.get(), accion.texto(), true);
			case HermandadAccionPayload.BAJAR_RANGO -> cambiarRango(jugador, hermandad.get(), accion.texto(), false);
			case HermandadAccionPayload.ESTANDARTE_GUARDAR -> guardarEstandarte(jugador, hermandad.get());
			case HermandadAccionPayload.ESTANDARTE_ACTIVAR, HermandadAccionPayload.ESTANDARTE_RECUPERAR,
				 HermandadAccionPayload.ESTANDARTE_BORRAR, HermandadAccionPayload.ESTANDARTE_QUITAR ->
					manejarEstandarte(jugador, hermandad.get(), accion.accion(), accion.texto());
			case HermandadAccionPayload.CHAT -> chatear(jugador, hermandad.get(), accion.texto());
			case HermandadAccionPayload.INVITAR -> invitar(jugador, hermandad.get(), accion.texto().strip());
			case HermandadAccionPayload.EXPULSAR -> expulsar(jugador, hermandad.get(), accion.texto());
			case HermandadAccionPayload.COLOR -> cambiarColor(jugador, hermandad.get(), accion.texto());
			default -> {}
		}
	}

	private static void enviarInfo(ServerPlayer jugador, HermandadesData.Hermandad h) {
		List<HermandadInfoPayload.Miembro> miembros = h.miembros().stream()
				.map(m -> new HermandadInfoPayload.Miembro(m.uuid(), m.nombre(), h.esLider(m.uuid()))).toList();
		ServerPlayNetworking.send(jugador, new HermandadInfoPayload(
				h.nombre(), h.color(), h.maestro(), miembros, h.balance(), h.nivelBanco(), h.motd()));
	}

	private static void cambiarColor(ServerPlayer maestro, HermandadesData.Hermandad h, String hex) {
		if (!h.maestro().equals(maestro.getUUID())) {
			error(maestro, "Solo el Maestro puede cambiar el color de la Hermandad.");
			return;
		}
		try {
			data(maestro).setColor(h, Integer.parseInt(hex, 16));
		} catch (NumberFormatException e) {
			return;
		}
		refrescarMiembros(maestro.server, h);
		sincronizarJugadores(maestro.server);
	}

	// ---------------------------------------------------------------- Nombre arriba de la cabeza

	private static HermandadesJugadoresPayload payloadJugadores(MinecraftServer server) {
		HermandadesData data = server.overworld().getDataStorage().computeIfAbsent(HermandadesData.FACTORY, "dedsafio4_hermandades");
		List<HermandadesJugadoresPayload.Entrada> entradas = new ArrayList<>();
		for (HermandadesData.Hermandad h : data.todas()) {
			for (ManuscritoDatos.Inscrito m : h.miembros()) {
				entradas.add(new HermandadesJugadoresPayload.Entrada(m.uuid(), h.nombre(), h.color()));
			}
		}
		List<HermandadesJugadoresPayload.Activo> activos = new ArrayList<>();
		for (HermandadesData.Hermandad h : data.todas()) {
			ItemStack activo = h.estandarteActivo();
			if (!activo.isEmpty()) activos.add(new HermandadesJugadoresPayload.Activo(h.nombre(), activo.copy()));
		}
		return new HermandadesJugadoresPayload(entradas, activos);
	}

	/** Manda a todos los conectados a qué Hermandad pertenece cada jugador. */
	public static void sincronizarJugadores(MinecraftServer server) {
		HermandadesJugadoresPayload payload = payloadJugadores(server);
		for (ServerPlayer conectado : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(conectado, payload);
		}
	}

	/** Al entrar, el jugador recibe la lista completa. */
	public static void sincronizarJugador(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, payloadJugadores(jugador.server));
	}

	/** Refresca la interfaz de todos los miembros conectados. */
	private static void refrescarMiembros(MinecraftServer server, HermandadesData.Hermandad h) {
		for (ManuscritoDatos.Inscrito m : h.miembros()) {
			ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
			if (conectado != null) enviarInfo(conectado, h);
		}
	}

	private static void invitar(ServerPlayer maestro, HermandadesData.Hermandad h, String nombreInvitado) {
		if (!h.maestro().equals(maestro.getUUID())) {
			error(maestro, "Solo el Maestro puede invitar jugadores.");
			return;
		}
		ServerPlayer invitado = maestro.server.getPlayerList().getPlayerByName(nombreInvitado);
		if (invitado == null) {
			error(maestro, "No hay ningún jugador conectado llamado " + nombreInvitado + ".");
			return;
		}
		if (data(maestro).deJugador(invitado.getUUID()).isPresent()) {
			error(maestro, invitado.getGameProfile().getName() + " ya pertenece a una Hermandad.");
			return;
		}
		INVITACIONES.put(invitado.getUUID(), h.nombre());
		maestro.sendSystemMessage(Component.literal("Invitaste a " + invitado.getGameProfile().getName()
				+ " a la Hermandad.").withStyle(ChatFormatting.GREEN));
		invitado.sendSystemMessage(Component.empty()
				.append(Component.literal(maestro.getGameProfile().getName()).withStyle(ChatFormatting.GOLD))
				.append(Component.literal(" te invitó a la Hermandad ").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(h.nombre()).withStyle(ChatFormatting.GOLD))
				.append(Component.literal(". ").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal("[Aceptar]").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild accept"))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Unirse a " + h.nombre()))))));
	}

	private static void aceptar(ServerPlayer jugador) {
		String nombre = INVITACIONES.remove(jugador.getUUID());
		HermandadesData data = data(jugador);
		Optional<HermandadesData.Hermandad> hermandad = nombre == null ? Optional.empty() : data.porNombre(nombre);
		if (hermandad.isEmpty()) {
			error(jugador, "No tienes ninguna invitación pendiente.");
			return;
		}
		if (data.deJugador(jugador.getUUID()).isPresent()) {
			error(jugador, "Ya perteneces a una Hermandad.");
			return;
		}
		HermandadesData.Hermandad h = hermandad.get();
		data.agregarMiembro(h, jugador.getUUID(), jugador.getGameProfile().getName());
		avisarMiembros(jugador.server, h, jugador.getGameProfile().getName() + " se unió a la Hermandad.", ChatFormatting.GREEN);
		refrescarMiembros(jugador.server, h);
		sincronizarJugadores(jugador.server);
	}

	private static void expulsar(ServerPlayer maestro, HermandadesData.Hermandad h, String uuidTexto) {
		if (!h.maestro().equals(maestro.getUUID())) {
			error(maestro, "Solo el Maestro puede expulsar miembros.");
			return;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(uuidTexto);
		} catch (IllegalArgumentException e) {
			return;
		}
		if (uuid.equals(h.maestro()) || !h.esMiembro(uuid)) return;

		String nombre = h.miembros().stream().filter(m -> m.uuid().equals(uuid))
				.map(ManuscritoDatos.Inscrito::nombre).findFirst().orElse("?");
		data(maestro).quitarMiembro(h, uuid);
		avisarMiembros(maestro.server, h, nombre + " fue expulsado de la Hermandad.", ChatFormatting.RED);
		refrescarMiembros(maestro.server, h);
		sincronizarJugadores(maestro.server);

		ServerPlayer expulsado = maestro.server.getPlayerList().getPlayer(uuid);
		if (expulsado != null) {
			expulsado.sendSystemMessage(Component.literal("Fuiste expulsado de la Hermandad " + h.nombre() + ".")
					.withStyle(ChatFormatting.RED));
		}
	}

	/** Avisa a los miembros conectados (en el chat normal) y lo deja en el chat de la Hermandad. */
	public static void avisarMiembros(MinecraftServer server, HermandadesData.Hermandad h, String mensaje, ChatFormatting color) {
		for (ManuscritoDatos.Inscrito m : h.miembros()) {
			ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
			if (conectado != null) conectado.sendSystemMessage(Component.literal(mensaje).withStyle(color));
		}
		// También queda en el chat de la Hermandad, como aviso.
		agregarAlChat(server, h, "", mensaje);
	}

	// ---------------------------------------------------------------- Tablón y rangos

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

	private static void refrescarTablon(MinecraftServer server, HermandadesData.Hermandad h) {
		HermandadTablonPayload payload = new HermandadTablonPayload(h.anuncios());
		for (ManuscritoDatos.Inscrito m : h.miembros()) {
			ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
			if (conectado != null) ServerPlayNetworking.send(conectado, payload);
		}
	}

	/** El Maestro o un Líder publica un anuncio; se avisa a todos por el chat de la Hermandad. */
	private static void publicarAnuncio(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
		if (!h.puedePublicar(jugador.getUUID())) {
			error(jugador, "Solo el Maestro y los Líderes pueden publicar en el Tablón.");
			return;
		}
		String limpio = texto.replace("§", "").strip();
		if (limpio.isEmpty()) return;
		if (limpio.length() > HermandadAccionPayload.LARGO_CHAT) limpio = limpio.substring(0, HermandadAccionPayload.LARGO_CHAT);
		data(jugador).publicarAnuncio(h, new HermandadTablonPayload.Anuncio(
				java.time.LocalDateTime.now().format(FECHA), jugador.getGameProfile().getName(), limpio));
		avisarMiembros(jugador.server, h, "Nuevo anuncio en el tablón: " + limpio, ChatFormatting.AQUA);
		refrescarTablon(jugador.server, h);
	}

	private static void borrarAnuncio(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
		if (!h.puedePublicar(jugador.getUUID())) {
			error(jugador, "Solo el Maestro y los Líderes pueden borrar anuncios.");
			return;
		}
		try {
			if (data(jugador).borrarAnuncio(h, Integer.parseInt(texto))) refrescarTablon(jugador.server, h);
		} catch (NumberFormatException ignorado) {
		}
	}

	/** Solo el Maestro sube a alguien a Líder o lo vuelve a miembro. */
	private static void cambiarRango(ServerPlayer maestro, HermandadesData.Hermandad h, String uuidTexto, boolean subir) {
		if (!h.maestro().equals(maestro.getUUID())) {
			error(maestro, "Solo el Maestro puede cambiar los rangos.");
			return;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(uuidTexto);
		} catch (IllegalArgumentException e) {
			return;
		}
		if (uuid.equals(h.maestro()) || !h.esMiembro(uuid) || h.esLider(uuid) == subir) return;
		String nombre = h.miembros().stream().filter(m -> m.uuid().equals(uuid))
				.map(ManuscritoDatos.Inscrito::nombre).findFirst().orElse("?");
		data(maestro).setLider(h, uuid, subir);
		avisarMiembros(maestro.server, h, subir ? nombre + " ahora es Líder de la Hermandad."
				: nombre + " ya no es Líder de la Hermandad.", subir ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY);
		refrescarMiembros(maestro.server, h);
	}

	// ---------------------------------------------------------------- Estandartes

	private static void enviarEstandartes(ServerPlayer jugador, HermandadesData.Hermandad h) {
		ServerPlayNetworking.send(jugador, new HermandadEstandartesPayload(
				h.estandartes().stream().map(ItemStack::copy).toList(), h.activo()));
	}

	private static void refrescarEstandartes(MinecraftServer server, HermandadesData.Hermandad h) {
		for (ManuscritoDatos.Inscrito m : h.miembros()) {
			ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
			if (conectado != null) enviarEstandartes(conectado, h);
		}
	}

	/** Cualquier miembro guarda en la colección el estandarte que tiene en la mano. */
	private static void guardarEstandarte(ServerPlayer jugador, HermandadesData.Hermandad h) {
		ItemStack enMano = jugador.getMainHandItem();
		if (!(enMano.getItem() instanceof net.minecraft.world.item.BannerItem)) {
			error(jugador, "Tienes que tener un estandarte en la mano.");
			return;
		}
		if (!data(jugador).guardarEstandarte(h, enMano)) {
			error(jugador, "La Colección de Estandartes está llena.");
			return;
		}
		if (!jugador.getAbilities().instabuild) enMano.shrink(1);
		jugador.displayClientMessage(Component.literal("Estandarte guardado en la colección.").withStyle(ChatFormatting.GREEN), true);
		refrescarEstandartes(jugador.server, h);
	}

	/** Activar, recuperar, borrar o quitar el activo: solo el Maestro. */
	private static void manejarEstandarte(ServerPlayer maestro, HermandadesData.Hermandad h, int accion, String texto) {
		if (!h.maestro().equals(maestro.getUUID())) {
			error(maestro, "Solo el Maestro puede administrar los estandartes.");
			return;
		}
		HermandadesData data = data(maestro);
		int lugar;
		try {
			lugar = texto.isEmpty() ? -1 : Integer.parseInt(texto);
		} catch (NumberFormatException e) {
			return;
		}
		boolean cambiaActivo;
		switch (accion) {
			case HermandadAccionPayload.ESTANDARTE_ACTIVAR -> {
				if (lugar < 0 || lugar >= h.estandartes().size()) return;
				data.activarEstandarte(h, lugar);
				avisarMiembros(maestro.server, h, "La Hermandad tiene un nuevo estandarte.", ChatFormatting.AQUA);
				cambiaActivo = true;
			}
			case HermandadAccionPayload.ESTANDARTE_QUITAR -> {
				if (h.activo() < 0) return;
				data.activarEstandarte(h, -1);
				cambiaActivo = true;
			}
			case HermandadAccionPayload.ESTANDARTE_RECUPERAR, HermandadAccionPayload.ESTANDARTE_BORRAR -> {
				boolean eraActivo = lugar == h.activo();
				ItemStack sacado = data.sacarEstandarte(h, lugar);
				if (sacado.isEmpty()) return;
				if (accion == HermandadAccionPayload.ESTANDARTE_RECUPERAR && !maestro.getInventory().add(sacado)) {
					maestro.drop(sacado, false);
				}
				cambiaActivo = eraActivo;
			}
			default -> {
				return;
			}
		}
		refrescarEstandartes(maestro.server, h);
		if (cambiaActivo) sincronizarJugadores(maestro.server);
	}

	// ---------------------------------------------------------------- Chat de la Hermandad

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	/**
	 * Un mensaje escrito en el mini chat de la interfaz: lo ven solo los miembros, en la interfaz y
	 * en el chat normal, con el nombre de quien lo escribió en el color de la Hermandad.
	 */
	private static void chatear(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
		// Sin códigos de formato ni caracteres raros, como el chat normal.
		String limpio = texto.replace("§", "").strip();
		limpio = limpio.chars().filter(c -> c >= ' ' && c != 127)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
		if (limpio.isEmpty()) return;
		if (limpio.length() > HermandadAccionPayload.LARGO_CHAT) limpio = limpio.substring(0, HermandadAccionPayload.LARGO_CHAT);

		String nombre = jugador.getGameProfile().getName();
		Component mensaje = Component.empty()
				.append(Component.literal("[" + h.nombre() + "] " + nombre).withColor(h.color()))
				.append(Component.literal(": " + limpio).withStyle(ChatFormatting.WHITE));
		for (ManuscritoDatos.Inscrito m : h.miembros()) {
			ServerPlayer conectado = jugador.server.getPlayerList().getPlayer(m.uuid());
			if (conectado != null) conectado.sendSystemMessage(mensaje);
		}
		Dedsafio4.LOGGER.info("[Hermandad {}] {}: {}", h.nombre(), nombre, limpio);
		agregarAlChat(jugador.server, h, nombre, limpio);
	}

	/** Guarda la línea y se la manda a los miembros conectados para su interfaz. */
	private static void agregarAlChat(MinecraftServer server, HermandadesData.Hermandad h, String autor, String texto) {
		HermandadesData data = server.overworld().getDataStorage().computeIfAbsent(HermandadesData.FACTORY, "dedsafio4_hermandades");
		data.agregarChat(h, new HermandadChatPayload.Linea(LocalTime.now().format(HORA), autor, texto));
		HermandadChatPayload payload = new HermandadChatPayload(h.chat());
		for (ManuscritoDatos.Inscrito m : h.miembros()) {
			ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
			if (conectado != null) ServerPlayNetworking.send(conectado, payload);
		}
	}

	private static ItemStack manuscritoSellado(ServerPlayer jugador) {
		for (InteractionHand mano : InteractionHand.values()) {
			ItemStack stack = jugador.getItemInHand(mano);
			if (ManuscritoHermandadItem.es(stack) && ManuscritoDatos.sellado(stack)) return stack;
		}
		return null;
	}

	/** Chequeos para sellar y para fundar. Si algo falla, le avisa al jugador y devuelve false. */
	private static boolean puedeFundarse(ServerPlayer jugador, ItemStack manuscrito, String nombre) {
		List<ManuscritoDatos.Inscrito> inscritos = ManuscritoDatos.inscritos(manuscrito);
		HermandadesData data = data(jugador);

		if (nombre.isEmpty()) {
			return error(jugador, "Escribe el nombre de la Hermandad arriba del Manuscrito.");
		}
		int minimo = ManuscritoHermandadItem.minimoInscritos(manuscrito);
		if (inscritos.size() < minimo) {
			return error(jugador, "Se necesitan al menos " + minimo + " inscritos.");
		}
		if (!ManuscritoDatos.estaInscrito(manuscrito, jugador.getUUID())) {
			return error(jugador, "Debes estar inscrito en el Manuscrito.");
		}
		if (data.nombreUsado(nombre)) {
			return error(jugador, "Ya existe una Hermandad llamada " + nombre + ".");
		}
		for (ManuscritoDatos.Inscrito inscrito : inscritos) {
			if (data.deJugador(inscrito.uuid()).isPresent()) {
				return error(jugador, inscrito.nombre() + " ya pertenece a otra Hermandad.");
			}
		}
		return true;
	}

	private static boolean error(ServerPlayer jugador, String mensaje) {
		jugador.sendSystemMessage(Component.literal(mensaje).withStyle(ChatFormatting.RED));
		return false;
	}
}
