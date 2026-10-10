package com.dedsafio4.catalogo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bloques.ModBloques;
import com.dedsafio4.cofres.ModCofres;
import com.dedsafio4.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * El Catálogo (se abre con la G): todos los ítems del mod ordenados por familia, en dos paneles como en la
 * imagen. Los que un admin ocultó (/catalogo ocultar ...) se ven como el Signo de Interrogación hasta que
 * los muestre (/catalogo mostrar ...). Lo oculto se guarda en el mundo y es igual para todos.
 *
 * Los editores (los que tienen /opop, y los admins) pueden cambiar qué hay en cada casilla desde la misma
 * pantalla: sacar ítems, moverlos y poner nuevos (desde la lupa). El diseño también se guarda en el mundo.
 */
public final class Catalogo {
	private Catalogo() {}

	private static List<List<Item>> izquierda, derecha;

	private static List<Item> fila(ItemLike... cosas) {
		List<Item> l = new ArrayList<>();
		for (ItemLike c : cosas) l.add(c.asItem());
		return l;
	}

	/** Panel izquierdo: objetos y herramientas. */
	public static List<List<Item>> izquierda() {
		if (izquierda == null) izquierda = List.of(
				fila(ModItems.CUCHARA_MADERA, ModItems.CUCHARA_HOJAS, ModItems.TENEDOR_HOJAS, ModItems.CUCHARA_DORADA_HOJAS, ModItems.MANUSCRITO_HERMANDAD, ModItems.BOLSA_ENDER),
				fila(ModItems.DEDITA, ModItems.DEDITA_VERDE, ModItems.DEDITA_ROJA, ModItems.DEDITA_MERCADO_NEGRO, ModItems.DEDITA_CASINO, com.dedsafio4.banco.ModCajero.CAJERO_ITEM),
				fila(com.dedsafio4.boveda.ModBoveda.BOVEDA_ITEM, com.dedsafio4.despegue.ModDespegue.COMBUSTIBLE, com.dedsafio4.despegue.ModDespegue.BATERIA_NAVE),
				fila(ModItems.CHIP_PHORA, com.dedsafio4.robots.ModRobots.ROBOT_DORMIDO_ITEM, ModItems.LINTERNA, ModItems.BATERIA_DILITIO, ModItems.TOTEM_FRERICO, ModItems.MANUSCRITO_HERMANDAD_ADMIN),
				fila(ModItems.MARTILLO_NETHERITE, ModItems.CANDADO, ModItems.LLAVE_CANDADO, ModItems.LLAVE, ModItems.TARJETA_ROSA, ModItems.AMULETO_VERDE),
				fila(ModItems.TARJETA_PUERTA_ROSA, ModItems.TARJETA_PUERTA_VERDE, com.dedsafio4.puertas.ModPuertas.PUERTA_ROSA_ITEM,
						com.dedsafio4.puertas.ModPuertas.PUERTA_VERDE_ITEM, ModCofres.COFRE_ITEM, ModCofres.COFRE_HUESOS_ITEM),
				fila(ModItems.CASCO_NETHERITA_PLANTA, ModItems.PECHERA_NETHERITA_PLANTA, ModItems.PANTALONES_NETHERITA_PLANTA, ModItems.BOTAS_NETHERITA_PLANTA,
						ModItems.MASCARA_ANTI_ESPORAS));
		return izquierda;
	}

	/** Panel derecho: materiales, comidas y bloques. */
	public static List<List<Item>> derecha() {
		if (derecha == null) derecha = List.of(
				fila(ModItems.SEMILLA_SOLARIA, ModItems.SEMILLA_UVINA, ModItems.SEMILLA_ARANDANO, ModItems.SEMILLA_EBURIA),
				fila(ModItems.HUEVO_EBURIA, ModItems.ARANDANO_NOCTURNO, ModItems.FRUTA_SOLARIA, ModItems.BAYA_UVINA, ModItems.FRUTA_ESPACIO, ModItems.CORAZON),
				fila(ModBloques.MINERAL_VERDE_ITEM, ModItems.CRISTAL_VERDE, ModItems.BARRA_CRISTAL_VERDE),
				fila(ModBloques.MINERAL_DE_AMBAR_ITEM, ModItems.AMBAR_EN_BRUTO, ModItems.AMBAR, ModItems.AMBAR_CON_INSECTO, ModItems.MANZANA_DE_AMBAR, ModItems.INSECTO),
				fila(ModBloques.BLOQUE_DILITIO_ITEM, ModItems.DILITIO, ModItems.RACIMO_DILITIO),
				fila(ModItems.BOLSA_DE_TELA, ModItems.PELO_EBURIA, ModItems.TELA_PRIMITIVA, ModItems.CUERDA_RESISTENTE, ModItems.PEGAMENTO_PRIMITIVO, ModItems.EXCREMENTO),
				fila(ModItems.PILDORA, ModItems.ANTIDOTO_PRIMITIVO, ModItems.SANGRE_REPTISAURIO, ModItems.ESCUPITAJO_DACTYLO, ModItems.PIMPOLLO_QUMARA),
				fila(ModBloques.BLOQUE_CELESTE_ITEM, ModBloques.BLOQUE_AMARILLO_ITEM, ModBloques.BLOQUE_VERDE_ITEM, ModBloques.BLOQUE_ROJO_ITEM,
						ModBloques.BLOQUE_PIXELES_ITEM, ModBloques.CACA_ITEM),
				fila(ModBloques.ROBLE_CLARO_LOG_ITEM, ModBloques.ABETO_CLARO_LOG_ITEM, ModBloques.AGUA_PORTAL_ITEM));
		return derecha;
	}

	/** Todos los ítems del catálogo (los del diseño de siempre, sin el signo de pregunta). */
	public static Set<Item> todos() {
		Set<Item> s = new LinkedHashSet<>();
		for (String id : disenoPorDefecto().values()) {
			Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
			if (i != ModItems.SIGNO_INTERROGACION) s.add(i);
		}
		return s;
	}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	// --- Lo oculto (en el cliente, lo que mandó el servidor) ---

	public static final Set<String> OCULTOS_CLIENTE = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public static boolean ocultoEnCliente(Item item) {
		return OCULTOS_CLIENTE.contains(id(item));
	}

	/** Servidor → cliente: lo oculto, qué hay en cada casilla ("panel:fila:columna=ítem") y si este jugador puede editar. */
	public record Payload(List<String> ocultos, List<String> celdas, boolean puedeEditar) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "catalogo"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::ocultos,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::celdas,
				ByteBufCodecs.BOOL, Payload::puedeEditar,
				Payload::new).<RegistryFriendlyByteBuf>cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: un editor puso (o sacó, con "") un ítem en una casilla. */
	public record EditarPayload(int panel, int fila, int columna, String item) implements CustomPacketPayload {
		public static final Type<EditarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "catalogo_editar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditarPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, EditarPayload::panel, ByteBufCodecs.VAR_INT, EditarPayload::fila,
				ByteBufCodecs.VAR_INT, EditarPayload::columna, ByteBufCodecs.STRING_UTF8, EditarPayload::item,
				EditarPayload::new).<RegistryFriendlyByteBuf>cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// --- El diseño (qué ítem hay en cada casilla) ---

	public static final int COLUMNAS = 6, MAX_FILAS = 60;

	public static String clave(int panel, int fila, int columna) {
		return panel + ":" + fila + ":" + columna;
	}

	/** El diseño que armó el usuario (con /G guardar): es el que trae el Catálogo. */
	private static final String DISENO_POR_DEFECTO = "G1:eNp1VO1upDAM_N-n4Wt7ElKeJTLB3c0exFwStu09_U2SsqU6EL-Ix5Px2E7d132tbis_RPOweksvOOgrRQNH0WYiL3qS60vVv_admq1j4-kt9o7jjb2NrG_COdoeRsNNHjxlQHMIoI-SXh9GF2t-F0SH_GCvTrR1kb2XKxkrLudWx1e_ix8Rr_qLGmQKpNmN7DNXfcJVpSLJrcGAQjSY8DdSomkgwNzsopebeMos1SlLq8xqbuRJjwCPBJfuFDJLrbwMElNgtqNkeKMiO8bJDlepaaIH53j9pNvi-J5nM6Gs1LhfUH-kqO5beIBK_pKDIpg-D-RxfIFQ_jCeZ3ZRMkOtOJh1sZHuokcy8XOSTNCpnKWNONAHNjnhkrVPpBdvZxvto-iAM-SunrXnJdpAmKyCvkA1exjiOdiAqiPnhFa9-TWiRWFJmjO2UwtfKUt70heRjbqjx-5apnXLDYJxzRPcooyilp0eEJQMbBTsQjthkBMTV-9KaWV-PE17b1rw7uusoWb6sSWdGuiT9PqwrlzaqG9jq6-ZA2PyB2epZWj9xLu1qnPzA88WzdabOhw3O1EPGMYZWj-hXzKqbNPxFKbmHkdSMyP5O8M0L6HQNKfgtkyiNnkVJKPPFii5MvKI8QE8WCeZAZuzy61ObzpnbTZWjwXIWPhDPsIMNHPb-QytN2jxrcrvWTmCia8nhdb5DVrslBYWfx3KHggIix7aCbMnGdOdZrcnkfRwYCqwqHhUQtw1NL0gwyR_Vt5dkZ6grcvbaKUHZTv7nvL0lvxP2SiPm2f5obraKtMPBEt98OkL8Q8Mug7A";
	/** Si un mundo tiene un diseño editado de antes de esta versión, se cambia (una vez) al de arriba. */
	private static final int VERSION_DISENO = 2;
	private static java.util.Map<String, String> porDefecto;

	/** El diseño de siempre (hasta que un editor lo cambie). */
	public static java.util.Map<String, String> disenoPorDefecto() {
		if (porDefecto == null) {
			java.util.Map<String, String> m = desdeCodigo(DISENO_POR_DEFECTO);
			porDefecto = m != null && !m.isEmpty() ? m : disenoViejo();
		}
		return porDefecto;
	}

	/** El primer diseño (por si el código no se pudiera leer). */
	private static java.util.Map<String, String> disenoViejo() {
		java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
		for (int panel = 0; panel < 2; panel++) {
			List<List<Item>> filas = panel == 0 ? izquierda() : derecha();
			for (int f = 0; f < filas.size(); f++) {
				for (int c = 0; c < filas.get(f).size(); c++) m.put(clave(panel, f, c), id(filas.get(f).get(c)));
			}
		}
		return m;
	}

	/**
	 * El diseño como un código para copiar: "G1:" + las casillas ("panel:fila:columna=ítem", una por línea;
	 * los del mod sin "dedsafio4:") comprimidas y en Base64.
	 */
	public static String aCodigo(java.util.Map<String, String> diseno) {
		StringBuilder sb = new StringBuilder();
		diseno.forEach((k, v) -> sb.append(k).append('=').append(v.startsWith(Dedsafio4.MOD_ID + ":") ? v.substring(Dedsafio4.MOD_ID.length() + 1) : v).append('\n'));
		try {
			java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
			try (java.util.zip.DeflaterOutputStream z = new java.util.zip.DeflaterOutputStream(bytes, new java.util.zip.Deflater(9))) {
				z.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
			return "G1:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** El diseño de un código de /G guardar (o null si no es válido). */
	@org.jetbrains.annotations.Nullable
	public static java.util.Map<String, String> desdeCodigo(String codigo) {
		try {
			codigo = codigo.trim();
			if (!codigo.startsWith("G1:")) return null;
			byte[] datos = java.util.Base64.getUrlDecoder().decode(codigo.substring(3));
			String texto;
			try (java.util.zip.InflaterInputStream z = new java.util.zip.InflaterInputStream(new java.io.ByteArrayInputStream(datos))) {
				texto = new String(z.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			}
			java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
			for (String linea : texto.split("\n")) {
				int i = linea.indexOf('=');
				if (i < 0) continue;
				String id = linea.substring(i + 1);
				if (!id.contains(":")) id = Dedsafio4.MOD_ID + ":" + id;
				if (BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))) m.put(linea.substring(0, i), id);
			}
			return m;
		} catch (Exception e) {
			return null;
		}
	}

	/** En el cliente: el diseño que mandó el servidor (o el de siempre) y si puede editarlo. */
	public static volatile java.util.Map<String, Item> DISENO_CLIENTE;
	public static volatile boolean PUEDE_EDITAR_CLIENTE;

	public static void recibirDiseno(List<String> celdas) {
		java.util.Map<String, Item> m = new java.util.HashMap<>();
		for (String e : celdas) {
			int i = e.indexOf('=');
			if (i < 0) continue;
			ResourceLocation rl = ResourceLocation.tryParse(e.substring(i + 1));
			if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) m.put(e.substring(0, i), BuiltInRegistries.ITEM.get(rl));
		}
		DISENO_CLIENTE = m;
	}

	private static java.util.Map<String, Item> disenoCliente() {
		if (DISENO_CLIENTE == null) {
			List<String> l = new ArrayList<>();
			disenoPorDefecto().forEach((k, v) -> l.add(k + "=" + v));
			recibirDiseno(l);
		}
		return DISENO_CLIENTE;
	}

	/** El ítem de esa casilla en el cliente (o null si está vacía). */
	public static Item itemEnCliente(int panel, int fila, int columna) {
		return disenoCliente().get(clave(panel, fila, columna));
	}

	/** Cambia una casilla en el cliente (al toque, antes de que conteste el servidor). */
	public static void ponerEnCliente(int panel, int fila, int columna, @org.jetbrains.annotations.Nullable Item item) {
		java.util.Map<String, Item> m = new java.util.HashMap<>(disenoCliente());
		if (item == null) m.remove(clave(panel, fila, columna)); else m.put(clave(panel, fila, columna), item);
		DISENO_CLIENTE = m;
	}

	/** Cuántas filas usa ese panel en el cliente. */
	public static int filasEnCliente(int panel) {
		int max = 0;
		for (String k : disenoCliente().keySet()) {
			String[] p = k.split(":");
			if (Integer.parseInt(p[0]) == panel) max = Math.max(max, Integer.parseInt(p[1]) + 1);
		}
		return max;
	}

	// --- Lo oculto, el diseño y los editores (en el servidor, guardado en el mundo) ---

	public static class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		final Set<String> ocultos = new HashSet<>();
		/** Si un editor ya lo cambió (si no, se usa el de siempre). */
		boolean editado;
		final java.util.Map<String, String> celdas = new java.util.LinkedHashMap<>();
		/** Los que pueden editar el catálogo (UUID), además de los admins. */
		final Set<String> editores = new HashSet<>();

		private static Datos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("ocultos", Tag.TAG_STRING)) d.ocultos.add(t.getAsString());
			for (Tag t : tag.getList("editores", Tag.TAG_STRING)) d.editores.add(t.getAsString());
			d.editado = tag.getBoolean("editado");
			CompoundTag c = tag.getCompound("celdas");
			for (String k : c.getAllKeys()) d.celdas.put(k, c.getString(k));
			// Un diseño editado con una versión vieja del mod pasa al diseño nuevo (una sola vez).
			if (tag.getInt("version") < VERSION_DISENO) {
				d.editado = false;
				d.celdas.clear();
				d.setDirty();
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (String s : ocultos) l.add(StringTag.valueOf(s));
			tag.put("ocultos", l);
			ListTag e = new ListTag();
			for (String s : editores) e.add(StringTag.valueOf(s));
			tag.put("editores", e);
			tag.putBoolean("editado", editado);
			tag.putInt("version", VERSION_DISENO);
			CompoundTag c = new CompoundTag();
			celdas.forEach(c::putString);
			tag.put("celdas", c);
			return tag;
		}

		java.util.Map<String, String> diseno() {
			return editado ? celdas : disenoPorDefecto();
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_catalogo");
	}

	public static boolean esEditor(ServerPlayer p) {
		return p.hasPermissions(2) || datos(p.server).editores.contains(p.getUUID().toString());
	}

	private static void enviar(ServerPlayer jugador) {
		Datos d = datos(jugador.server);
		List<String> celdas = new ArrayList<>();
		d.diseno().forEach((k, v) -> celdas.add(k + "=" + v));
		ServerPlayNetworking.send(jugador, new Payload(new ArrayList<>(d.ocultos), celdas, esEditor(jugador)));
	}

	/** Un editor cambió una casilla: se guarda y se les manda a todos. */
	private static void editar(ServerPlayer p, EditarPayload e) {
		if (!esEditor(p) || e.panel() < 0 || e.panel() > 1 || e.fila() < 0 || e.fila() >= MAX_FILAS || e.columna() < 0 || e.columna() >= COLUMNAS) return;
		Datos d = datos(p.server);
		if (!d.editado) {
			d.celdas.clear();
			d.celdas.putAll(disenoPorDefecto());
			d.editado = true;
		}
		String k = clave(e.panel(), e.fila(), e.columna());
		ResourceLocation rl = e.item().isEmpty() ? null : ResourceLocation.tryParse(e.item());
		if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) d.celdas.remove(k);
		else d.celdas.put(k, rl.toString());
		d.setDirty();
		enviarATodos(p.server);
	}

	private static void enviarATodos(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) enviar(p);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		PayloadTypeRegistry.playC2S().register(EditarPayload.TYPE, EditarPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(EditarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> editar(context.player(), payload)));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enviar(handler.player));
	}

	/**
	 * /catalogo ocultar <ítem | todo>  y  /catalogo mostrar <ítem | todo>  (admins).
	 * El ítem se escribe con su nombre del juego (ej. dedsafio4:cristal_verde); lo sugiere al escribir.
	 */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String nombre : new String[]{"catalogo", "catálogo"}) {
			LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal(nombre).requires(s -> s.hasPermission(2));
			for (boolean ocultar : new boolean[]{true, false}) {
				raiz.then(Commands.literal(ocultar ? "ocultar" : "mostrar")
						.then(Commands.argument("item", StringArgumentType.greedyString())
								.suggests((c, b) -> {
									List<String> ids = new ArrayList<>();
									ids.add("todo");
									for (Item i : todos()) ids.add(id(i));
									return SharedSuggestionProvider.suggest(ids, b);
								})
								.executes(c -> cambiar(c, ocultar))));
			}
			// /catalogo reiniciar: vuelve al diseño de siempre.
			raiz.then(Commands.literal("reiniciar").executes(c -> {
				Datos d = datos(c.getSource().getServer());
				d.editado = false;
				d.celdas.clear();
				d.setDirty();
				enviarATodos(c.getSource().getServer());
				c.getSource().sendSuccess(() -> Component.literal("Catálogo: volvió al diseño de siempre.").withColor(0x5AD8FF), true);
				return 1;
			}));
			dispatcher.register(raiz);
		}
		// /g guardar: el código del diseño actual (click para copiarlo). /g cargar <código>: lo pone.
		for (String nombre : new String[]{"g"}) {
			dispatcher.register(Commands.literal(nombre)
					.requires(s -> s.hasPermission(2) || (s.getPlayer() != null && esEditor(s.getPlayer())))
					.then(Commands.literal("guardar").executes(c -> {
						String codigo = aCodigo(datos(c.getSource().getServer()).diseno());
						Dedsafio4.LOGGER.info("Código del Catálogo (G): {}", codigo);
						c.getSource().sendSuccess(() -> Component.literal("Código del Catálogo (" + codigo.length() + " letras): ").withColor(0x5AD8FF)
								.append(Component.literal("[Click para copiarlo]").withStyle(st -> st.withColor(0x7CFC6A).withUnderlined(true)
										.withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, codigo))
										.withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
												Component.literal(codigo.length() > 120 ? codigo.substring(0, 120) + "..." : codigo))))), false);
						return 1;
					}))
					.then(Commands.literal("cargar").then(Commands.argument("codigo", StringArgumentType.greedyString()).executes(c -> {
						java.util.Map<String, String> m = desdeCodigo(StringArgumentType.getString(c, "codigo"));
						if (m == null) {
							c.getSource().sendFailure(Component.literal("Ese código no es válido."));
							return 0;
						}
						Datos d = datos(c.getSource().getServer());
						d.celdas.clear();
						d.celdas.putAll(m);
						d.editado = true;
						d.setDirty();
						enviarATodos(c.getSource().getServer());
						c.getSource().sendSuccess(() -> Component.literal("Catálogo cargado (" + m.size() + " casillas).").withColor(0x5AD8FF), true);
						return 1;
					}))));
		}
		// /opop <jugador>: puede cambiar el Catálogo (la G). /deopop <jugador>: ya no.
		for (boolean dar : new boolean[]{true, false}) {
			dispatcher.register(Commands.literal(dar ? "opop" : "deopop").requires(s -> s.hasPermission(2))
					.then(Commands.argument("jugador", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
						ServerPlayer p = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "jugador");
						Datos d = datos(c.getSource().getServer());
						if (dar) d.editores.add(p.getUUID().toString()); else d.editores.remove(p.getUUID().toString());
						d.setDirty();
						enviar(p);
						c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName()
								+ (dar ? " ahora puede cambiar el Catálogo (G)." : " ya no puede cambiar el Catálogo (G).")).withColor(0x5AD8FF), true);
						if (dar) p.sendSystemMessage(Component.literal("Ya puedes cambiar el Catálogo (G): haz clic para tomar y poner ítems, clic fuera para quitarlos, y la lupa para agregar nuevos.").withColor(0x5AD8FF));
						return 1;
					})));
		}
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, boolean ocultar) {
		String texto = StringArgumentType.getString(c, "item").trim();
		Datos d = datos(c.getSource().getServer());
		List<String> ids = new ArrayList<>();
		if (texto.equalsIgnoreCase("todo")) {
			for (Item i : todos()) ids.add(id(i));
		} else {
			ResourceLocation rl = ResourceLocation.tryParse(texto.contains(":") ? texto : Dedsafio4.MOD_ID + ":" + texto);
			if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
				c.getSource().sendFailure(Component.literal("No existe el ítem " + texto + "."));
				return 0;
			}
			ids.add(rl.toString());
		}
		if (ocultar) d.ocultos.addAll(ids); else ids.forEach(d.ocultos::remove);
		d.setDirty();
		enviarATodos(c.getSource().getServer());
		String cual = ids.size() == 1 ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(ids.get(0))).getDescription().getString() : "todos los ítems";
		c.getSource().sendSuccess(() -> Component.literal("Catálogo: " + (ocultar ? "se ocultó " : "ya se ve ") + cual + ".").withColor(0x5AD8FF), true);
		return ids.size();
	}
}
