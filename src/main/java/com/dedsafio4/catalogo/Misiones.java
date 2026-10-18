package com.dedsafio4.catalogo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Las Misiones (la pestaña del pergamino del Catálogo). Hay dos clases:
 *   Misión Guía       "📖 Misión Guía | + N 🪙"
 *   Misión Principal  "★ Misión Principal - Día N"
 * Cada una pide, con un ítem y una cantidad, uno de estos objetivos:
 *   craftear  crearlo (cuentan los crafteos desde que el jugador vio la misión por primera vez);
 *   obtener   tenerlo en el inventario (cuenta lo que tiene en ese momento);
 *   romper    romper ese bloque, o gastar hasta romper esa herramienta (también desde que la vio). Al cumplirla, la tarjeta se pone verde con "¡Misión completada!" y se cobran las deditas (una sola
 * vez por jugador); la Principal da además la "Dedita de la Misión (Día N)".
 * Los editores del Catálogo (admins y /opop) las crean, cambian, mueven y borran desde la misma pantalla. El texto
 * acepta colores: {morado:Casco Dimensional} pinta esas palabras, y {item} pone el nombre del ítem en azul.
 */
public final class Misiones {
	private Misiones() {}

	/** Los colores que se pueden usar en el texto: {color:palabras}. También sirve {#FF8800:palabras}. */
	public static final Map<String, Integer> COLORES = new LinkedHashMap<>();
	static {
		COLORES.put("blanco", 0xFFFFFF);
		COLORES.put("azul", 0x6FA8FF);
		COLORES.put("celeste", 0x55D9F0);
		COLORES.put("morado", 0xC77DFF);
		COLORES.put("amarillo", 0xFFE14A);
		COLORES.put("naranja", 0xFF9A2E);
		COLORES.put("rojo", 0xFF5050);
		COLORES.put("rosa", 0xFF7AC8);
		COLORES.put("verde", 0x7CFC6A);
		COLORES.put("gris", 0xA0A0A0);
	}

	public static final int MAX_TEXTO = 400, MAX_PREMIO = 27;

	/**
	 * Una misión: un id fijo (para saber quién la cobró), si es Principal y de qué día, el ítem a crear (su id,
	 * como "minecraft:diamond_helmet"), cuántos, cuántas deditas da y el texto de abajo (con colores).
	 * premio: lo que manda Eón cuando se entrega la Dedita de la Misión (solo la Principal).
	 */
	public record Mision(String id, boolean principal, int dia, String itemId, int cantidad, int deditas, String texto,
						 List<net.minecraft.world.item.ItemStack> premio, String objetivo) {
		/** Los objetivos posibles (ver arriba). */
		public static final List<String> OBJETIVOS = List.of("craftear", "obtener", "romper");

		public Mision {
			if (!OBJETIVOS.contains(objetivo)) objetivo = "craftear";
		}

		public static final StreamCodec<RegistryFriendlyByteBuf, Mision> CODEC = StreamCodec.of((buf, m) -> {
			buf.writeUtf(m.id);
			buf.writeBoolean(m.principal);
			buf.writeVarInt(m.dia);
			buf.writeUtf(m.itemId);
			buf.writeVarInt(m.cantidad);
			buf.writeVarInt(m.deditas);
			buf.writeUtf(m.texto, MAX_TEXTO * 4);
			net.minecraft.world.item.ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, m.premio);
			buf.writeUtf(m.objetivo);
		}, buf -> new Mision(buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
				buf.readUtf(MAX_TEXTO * 4), net.minecraft.world.item.ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf), buf.readUtf()));

		public Item item() {
			ResourceLocation rl = ResourceLocation.tryParse(itemId);
			return rl == null ? Items.BARRIER : BuiltInRegistries.ITEM.getOptional(rl).filter(i -> i != Items.AIR).orElse(Items.BARRIER);
		}

		/** El texto de abajo, con sus colores. */
		public Component descripcion() {
			return Misiones.descripcion(texto, item());
		}

		/** "Misión Guía | + 75" o "Misión Principal - Día 15" (sin el ícono). */
		public String titulo() {
			return principal ? "Misión Principal - Día " + dia : "Misión Guía | + " + deditas;
		}

		CompoundTag guardar(HolderLookup.Provider registros) {
			CompoundTag t = new CompoundTag();
			t.putString("id", id);
			t.putBoolean("principal", principal);
			t.putInt("dia", dia);
			t.putString("item", itemId);
			t.putInt("cantidad", cantidad);
			t.putInt("deditas", deditas);
			t.putString("texto", texto);
			ListTag l = new ListTag();
			// Cada cosa con su casilla (el premio es una grilla de 3×9 y puede tener huecos).
			for (int i = 0; i < premio.size(); i++) {
				if (premio.get(i).isEmpty()) continue;
				CompoundTag casilla = new CompoundTag();
				casilla.putByte("Slot", (byte) i);
				l.add(premio.get(i).save(registros, casilla));
			}
			t.put("premio", l);
			t.putString("objetivo", objetivo);
			return t;
		}

		static Mision leer(CompoundTag t, HolderLookup.Provider registros) {
			List<net.minecraft.world.item.ItemStack> premio = new ArrayList<>();
			for (Tag o : t.getList("premio", Tag.TAG_COMPOUND)) {
				var pila = net.minecraft.world.item.ItemStack.parse(registros, o);
				if (pila.isEmpty()) continue;
				// Los guardados antes no tienen casilla: van uno atrás del otro.
				int casilla = ((CompoundTag) o).contains("Slot") ? ((CompoundTag) o).getByte("Slot") & 0xFF : premio.size();
				if (casilla >= MAX_PREMIO) continue;
				while (premio.size() <= casilla) premio.add(net.minecraft.world.item.ItemStack.EMPTY);
				premio.set(casilla, pila.get());
			}
			return new Mision(t.getString("id"), t.getBoolean("principal"), t.getInt("dia"), t.getString("item"),
					Math.max(1, t.getInt("cantidad")), t.getInt("deditas"), t.getString("texto"), premio,
					t.contains("objetivo") ? t.getString("objetivo") : "craftear");
		}
	}

	/** El color de un nombre ("morado") o de "#RRGGBB"; -1 si no se entiende. */
	public static int color(String c) {
		c = c.trim().toLowerCase(java.util.Locale.ROOT);
		if (COLORES.containsKey(c)) return COLORES.get(c);
		String hex = c.startsWith("#") ? c.substring(1) : c;
		if (hex.matches("[0-9a-f]{6}")) return Integer.parseInt(hex, 16);
		return -1;
	}

	/** Arma el texto: lo normal en blanco, {color:palabras} en ese color y {item} con el nombre del ítem en azul. */
	public static Component descripcion(String texto, Item item) {
		MutableComponent c = Component.empty();
		int i = 0;
		while (i < texto.length()) {
			int abre = texto.indexOf('{', i);
			int cierra = abre < 0 ? -1 : texto.indexOf('}', abre);
			if (abre < 0 || cierra < 0) {
				c.append(Component.literal(texto.substring(i)).withColor(0xFFFFFF));
				break;
			}
			if (abre > i) c.append(Component.literal(texto.substring(i, abre)).withColor(0xFFFFFF));
			String dentro = texto.substring(abre + 1, cierra);
			int dos = dentro.indexOf(':');
			if (dentro.trim().equalsIgnoreCase("item")) {
				c.append(Component.translatable(item.getDescriptionId()).withColor(COLORES.get("azul")));
			} else if (dos > 0 && color(dentro.substring(0, dos)) >= 0) {
				c.append(Component.literal(dentro.substring(dos + 1)).withColor(color(dentro.substring(0, dos))));
			} else {
				c.append(Component.literal(texto.substring(abre, cierra + 1)).withColor(0xFFFFFF));
			}
			i = cierra + 1;
		}
		return c;
	}

	/**
	 * Copia de las misiones fuera del mundo (en la carpeta del juego o del servidor): cada vez que se editan en la G
	 * se guardan ahí también, y un mundo que todavía no tiene misiones empieza con éstas. No hay misiones fijas: sólo
	 * las que se armen en la G.
	 */
	private static final java.util.Set<String> FIJAS_VIEJAS = java.util.Set.of("casco_diamante", "pechera_diamante",
			"pantalones_diamante", "botas_diamante", "candado");

	private static java.nio.file.Path archivo(MinecraftServer server) {
		return server.getServerDirectory().resolve("config").resolve("dedsafio4_misiones.dat");
	}

	private static List<Mision> desdeArchivo(MinecraftServer server) {
		List<Mision> l = new ArrayList<>();
		java.nio.file.Path p = archivo(server);
		if (!java.nio.file.Files.exists(p)) return l;
		try {
			CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(p, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
			for (Tag t : tag.getList("misiones", Tag.TAG_COMPOUND)) l.add(Mision.leer((CompoundTag) t, server.registryAccess()));
		} catch (Exception e) {
			Dedsafio4.LOGGER.warn("No se pudieron leer las misiones guardadas: {}", e.toString());
		}
		return l;
	}

	private static void aArchivo(MinecraftServer server, List<Mision> misiones) {
		try {
			CompoundTag tag = new CompoundTag();
			ListTag l = new ListTag();
			for (Mision m : misiones) l.add(m.guardar(server.registryAccess()));
			tag.put("misiones", l);
			java.nio.file.Path p = archivo(server);
			java.nio.file.Files.createDirectories(p.getParent());
			net.minecraft.nbt.NbtIo.writeCompressed(tag, p);
		} catch (Exception e) {
			Dedsafio4.LOGGER.warn("No se pudieron guardar las misiones: {}", e.toString());
		}
	}

	// --- Guardado ---

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		/** null = este mundo todavía no tiene (se toman las de la copia, ver desdeArchivo). */
		List<Mision> misiones;
		/** Por jugador y misión: cuántos llevaba crafteados cuando la vio por primera vez. */
		final Map<UUID, Map<String, Integer>> bases = new HashMap<>();

		List<Mision> lista() {
			if (misiones == null) misiones = new ArrayList<>();
			return misiones;
		}

		private static Datos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("misiones")) {
				d.misiones = new ArrayList<>();
				for (Tag t : tag.getList("misiones", Tag.TAG_COMPOUND)) d.misiones.add(Mision.leer((CompoundTag) t, registros));
			}
			CompoundTag b = tag.getCompound("bases");
			for (String jugador : b.getAllKeys()) {
				CompoundTag dj = b.getCompound(jugador);
				Map<String, Integer> m = new HashMap<>();
				for (String id : dj.getAllKeys()) m.put(id, dj.getInt(id));
				d.bases.put(UUID.fromString(jugador), m);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			if (misiones != null) {
				ListTag l = new ListTag();
				for (Mision m : misiones) l.add(m.guardar(registros));
				tag.put("misiones", l);
			}
			CompoundTag b = new CompoundTag();
			bases.forEach((jugador, m) -> {
				CompoundTag dj = new CompoundTag();
				m.forEach(dj::putInt);
				b.put(jugador.toString(), dj);
			});
			tag.put("bases", b);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		Datos d = server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_misiones_catalogo");
		if (d.misiones == null) {
			d.misiones = desdeArchivo(server);
			d.setDirty();
		}
		// Las misiones fijas de antes ya no van (si un mundo las tenía guardadas, se sacan).
		if (d.misiones.removeIf(m -> FIJAS_VIEJAS.contains(m.id()))) {
			d.setDirty();
			aArchivo(server, d.misiones);
		}
		return d;
	}

	// --- En el cliente ---

	/** Las misiones y cuánto lleva de cada una (lo manda el servidor). */
	public static volatile List<Mision> LISTA_CLIENTE = List.of();
	public static volatile int[] PROGRESO_CLIENTE = new int[0];

	public static List<Mision> lista() {
		return LISTA_CLIENTE;
	}

	public static int progresoCliente(int i) {
		int[] p = PROGRESO_CLIENTE;
		return i < p.length ? p[i] : 0;
	}

	// --- Red ---

	/** Servidor → jugador: cuánto lleva de cada misión. */
	public record Payload(List<Integer> progreso) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "misiones"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).<RegistryFriendlyByteBuf>cast().map(Payload::new, Payload::progreso);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor → jugador: todas las misiones. */
	public record ListaPayload(List<Mision> misiones) implements CustomPacketPayload {
		public static final Type<ListaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "misiones_lista"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ListaPayload> CODEC =
				Mision.CODEC.apply(ByteBufCodecs.list()).map(ListaPayload::new, ListaPayload::misiones);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Editor → servidor. accion: "guardar" (indice -1 = nueva), "borrar", "subir" o "bajar".
	 */
	public record EditarPayload(String accion, int indice, Mision mision) implements CustomPacketPayload {
		public static final Type<EditarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "misiones_editar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditarPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, EditarPayload::accion, ByteBufCodecs.VAR_INT, EditarPayload::indice,
				Mision.CODEC, EditarPayload::mision, EditarPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// --- Progreso ---

	/** Cuánto lleva de cada misión; si alguna se acaba de cumplir, la cobra. */
	private static List<Integer> progreso(ServerPlayer p) {
		Datos d = datos(p.server);
		Map<String, Integer> bases = d.bases.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
		List<Integer> l = new ArrayList<>();
		for (Mision m : d.lista()) {
			if (com.dedsafio4.misiones.Misiones.completada(p, m.id())) {
				l.add(m.cantidad());
				continue;
			}
			if (m.objetivo().equals("obtener")) {
				// Lo que tiene ahora en el inventario.
				int tiene = p.getInventory().countItem(m.item());
				int prog = Math.min(m.cantidad(), tiene);
				if (prog >= m.cantidad()) cobrar(p, m);
				l.add(prog);
				continue;
			}
			int hechos = hechos(p, m);
			Integer base = bases.get(m.id());
			if (base == null) {
				bases.put(m.id(), hechos);
				d.setDirty();
				base = hechos;
			}
			int prog = Math.max(0, Math.min(m.cantidad(), hechos - base));
			if (prog >= m.cantidad()) cobrar(p, m);
			l.add(prog);
		}
		return l;
	}

	/** Cuántas veces lo crafteó, o lo rompió (el bloque minado, o la herramienta gastada hasta romperse), en total. */
	private static int hechos(ServerPlayer p, Mision m) {
		Item item = m.item();
		if (m.objetivo().equals("romper")) {
			int rotos = p.getStats().getValue(Stats.ITEM_BROKEN.get(item));
			if (item instanceof net.minecraft.world.item.BlockItem bloque) rotos += p.getStats().getValue(Stats.BLOCK_MINED.get(bloque.getBlock()));
			return rotos;
		}
		return p.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
	}

	private static void cobrar(ServerPlayer p, Mision m) {
		// La Principal no da deditas al completarla: da la Dedita de la Misión, que se cambia por el premio.
		if (!com.dedsafio4.misiones.Misiones.completar(p, m.id(), m.principal(), m.item(), m.principal() ? 0 : m.deditas())) return;
		// La Misión Principal da además la "Dedita de la Misión (Día N)".
		if (m.principal()) {
			net.minecraft.world.item.ItemStack dedita = new net.minecraft.world.item.ItemStack(ModItems.DEDITA_MISION);
			CompoundTag datos = new CompoundTag();
			datos.putString("mision", m.id());
			datos.putInt("dia", m.dia());
			dedita.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(datos));
			if (!p.getInventory().add(dedita)) p.drop(dedita, false);
		}
		p.displayClientMessage(Component.literal("¡Misión completada! ").withColor(0x7CFC6A)
				.append(Component.translatable(m.item().getDescriptionId()).withColor(0x6FA8FF)), true);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	/** La misión de una Dedita de la Misión (la que dio al completarla), o null si no es una o ya no existe. */
	public static Mision deDedita(MinecraftServer server, net.minecraft.world.item.ItemStack dedita) {
		if (!dedita.is(ModItems.DEDITA_MISION)) return null;
		net.minecraft.world.item.component.CustomData datos = dedita.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		if (datos == null) return null;
		String id = datos.copyTag().getString("mision");
		for (Mision m : datos(server).lista()) if (m.id().equals(id)) return m;
		return null;
	}

	/** El día que dice una Dedita de la Misión (-1 si no tiene). */
	public static int diaDeDedita(net.minecraft.world.item.ItemStack dedita) {
		net.minecraft.world.item.component.CustomData datos = dedita.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		return datos == null || !datos.copyTag().contains("dia") ? -1 : datos.copyTag().getInt("dia");
	}

	/** Lo último que se le mandó a cada jugador (para mandar solo cuando cambia). */
	private static final Map<UUID, List<Integer>> ENVIADO = new HashMap<>();

	private static void revisar(ServerPlayer p) {
		List<Integer> ahora = progreso(p);
		if (ahora.equals(ENVIADO.get(p.getUUID()))) return;
		ENVIADO.put(p.getUUID(), ahora);
		ServerPlayNetworking.send(p, new Payload(ahora));
	}

	private static void enviarLista(MinecraftServer server) {
		ListaPayload l = new ListaPayload(List.copyOf(datos(server).lista()));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, l);
			ENVIADO.remove(p.getUUID());
			revisar(p);
		}
	}

	// --- Editar ---

	private static void editar(ServerPlayer p, EditarPayload e) {
		// Crear, cambiar (y su recompensa), mover o borrar misiones: solo los OP (los de /opop no).
		if (!p.hasPermissions(2)) return;
		Datos d = datos(p.server);
		List<Mision> l = d.lista();
		int i = e.indice();
		switch (e.accion()) {
			case "borrar" -> {
				if (i < 0 || i >= l.size()) return;
				l.remove(i);
			}
			case "subir", "bajar" -> {
				int j = e.accion().equals("subir") ? i - 1 : i + 1;
				if (i < 0 || i >= l.size() || j < 0 || j >= l.size()) return;
				l.set(j, l.set(i, l.get(j)));
			}
			case "guardar" -> {
				Mision m = e.mision();
				ResourceLocation rl = ResourceLocation.tryParse(m.itemId());
				if (rl == null || BuiltInRegistries.ITEM.getOptional(rl).filter(it -> it != Items.AIR).isEmpty()) return;
				String texto = m.texto().length() > MAX_TEXTO ? m.texto().substring(0, MAX_TEXTO) : m.texto();
				// Si cambia el ítem (o el objetivo) es como una misión nueva (empieza de cero para todos).
				String id = i >= 0 && i < l.size() && l.get(i).itemId().equals(rl.toString())
						&& l.get(i).objetivo().equals(m.objetivo()) ? l.get(i).id()
						: UUID.randomUUID().toString().substring(0, 8);
				List<net.minecraft.world.item.ItemStack> premio = new ArrayList<>();
				// Cada cosa queda en su casilla (con los huecos del medio); los del final no hacen falta.
				for (int k = 0; k < m.premio().size() && k < MAX_PREMIO; k++) {
					net.minecraft.world.item.ItemStack o = m.premio().get(k);
					if (o.isEmpty()) {
						premio.add(net.minecraft.world.item.ItemStack.EMPTY);
						continue;
					}
					net.minecraft.world.item.ItemStack copia = o.copy();
					copia.setCount(Math.max(1, Math.min(copia.getMaxStackSize(), copia.getCount())));
					premio.add(copia);
				}
				while (!premio.isEmpty() && premio.get(premio.size() - 1).isEmpty()) premio.remove(premio.size() - 1);
				Mision limpia = new Mision(id, m.principal(), Math.max(0, Math.min(9999, m.dia())), rl.toString(),
						Math.max(1, Math.min(9999, m.cantidad())), Math.max(0, Math.min(1_000_000, m.deditas())), texto,
						m.principal() ? premio : List.of(), m.objetivo());
				if (i >= 0 && i < l.size()) l.set(i, limpia);
				else l.add(limpia);
			}
			default -> {
				return;
			}
		}
		d.setDirty();
		aArchivo(p.server, l);
		enviarLista(p.server);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		PayloadTypeRegistry.playS2C().register(ListaPayload.TYPE, ListaPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EditarPayload.TYPE, EditarPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(EditarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> editar(context.player(), payload)));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayNetworking.send(handler.player, new ListaPayload(List.copyOf(datos(server).lista())));
			ENVIADO.remove(handler.player.getUUID());
			revisar(handler.player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ENVIADO.remove(handler.player.getUUID()));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer p : server.getPlayerList().getPlayers()) revisar(p);
		});
	}
}
