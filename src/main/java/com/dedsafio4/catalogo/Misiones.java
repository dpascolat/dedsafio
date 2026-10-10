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
 * Cada una pide crear un ítem una cantidad de veces (cuentan los crafteos desde que el jugador vio la misión por
 * primera vez). Al cumplirla, la tarjeta se pone verde con "¡Misión completada!" y se cobran las deditas (una sola
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

	public static final int MAX_TEXTO = 400, MAX_PREMIO = 18;

	/**
	 * Una misión: un id fijo (para saber quién la cobró), si es Principal y de qué día, el ítem a crear (su id,
	 * como "minecraft:diamond_helmet"), cuántos, cuántas deditas da y el texto de abajo (con colores).
	 * premio: lo que manda Eón cuando se entrega la Dedita de la Misión (solo la Principal).
	 */
	public record Mision(String id, boolean principal, int dia, String itemId, int cantidad, int deditas, String texto,
						 List<net.minecraft.world.item.ItemStack> premio) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Mision> CODEC = StreamCodec.of((buf, m) -> {
			buf.writeUtf(m.id);
			buf.writeBoolean(m.principal);
			buf.writeVarInt(m.dia);
			buf.writeUtf(m.itemId);
			buf.writeVarInt(m.cantidad);
			buf.writeVarInt(m.deditas);
			buf.writeUtf(m.texto, MAX_TEXTO * 4);
			net.minecraft.world.item.ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, m.premio);
		}, buf -> new Mision(buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
				buf.readUtf(MAX_TEXTO * 4), net.minecraft.world.item.ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf)));

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
			for (net.minecraft.world.item.ItemStack o : premio) if (!o.isEmpty()) l.add(o.save(registros));
			t.put("premio", l);
			return t;
		}

		static Mision leer(CompoundTag t, HolderLookup.Provider registros) {
			List<net.minecraft.world.item.ItemStack> premio = new ArrayList<>();
			for (Tag o : t.getList("premio", Tag.TAG_COMPOUND)) net.minecraft.world.item.ItemStack.parse(registros, o).ifPresent(premio::add);
			return new Mision(t.getString("id"), t.getBoolean("principal"), t.getInt("dia"), t.getString("item"),
					Math.max(1, t.getInt("cantidad")), t.getInt("deditas"), t.getString("texto"), premio);
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

	private static final String RULETA = ". Son muy importantes para sobrevivir a los peligros de la Ruleta.";

	/** Las de siempre (hasta que un editor las cambie). Los ids son los de antes, así nadie cobra dos veces. */
	private static List<Mision> porDefecto() {
		String candado = BuiltInRegistries.ITEM.getKey(ModItems.CANDADO).toString();
		return new ArrayList<>(List.of(
				new Mision("casco_diamante", false, 0, "minecraft:diamond_helmet", 1, 2, "Crea un {item}" + RULETA, List.of()),
				new Mision("pechera_diamante", false, 0, "minecraft:diamond_chestplate", 1, 3, "Crea una {item}" + RULETA, List.of()),
				new Mision("pantalones_diamante", false, 0, "minecraft:diamond_leggings", 1, 3, "Crea unos {item}" + RULETA, List.of()),
				new Mision("botas_diamante", false, 0, "minecraft:diamond_boots", 1, 2, "Crea unas {item}" + RULETA, List.of()),
				new Mision("candado", false, 0, candado, 1, 2, "Crea un {item}. Utilízalo para proteger tus cofres.", List.of())));
	}

	// --- Guardado ---

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		/** null = las de siempre. */
		List<Mision> misiones;
		/** Por jugador y misión: cuántos llevaba crafteados cuando la vio por primera vez. */
		final Map<UUID, Map<String, Integer>> bases = new HashMap<>();

		List<Mision> lista() {
			if (misiones == null) misiones = porDefecto();
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
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_misiones_catalogo");
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
			int hechos = p.getStats().getValue(Stats.ITEM_CRAFTED.get(m.item()));
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
		if (!Catalogo.esEditor(p)) return;
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
				// Si cambia el ítem es como una misión nueva (empieza de cero para todos).
				String id = i >= 0 && i < l.size() && l.get(i).itemId().equals(rl.toString()) ? l.get(i).id()
						: UUID.randomUUID().toString().substring(0, 8);
				List<net.minecraft.world.item.ItemStack> premio = new ArrayList<>();
				for (net.minecraft.world.item.ItemStack o : m.premio()) {
					if (o.isEmpty() || premio.size() >= MAX_PREMIO) continue;
					net.minecraft.world.item.ItemStack copia = o.copy();
					copia.setCount(Math.max(1, Math.min(copia.getMaxStackSize(), copia.getCount())));
					premio.add(copia);
				}
				Mision limpia = new Mision(id, m.principal(), Math.max(0, Math.min(9999, m.dia())), rl.toString(),
						Math.max(1, Math.min(9999, m.cantidad())), Math.max(0, Math.min(1_000_000, m.deditas())), texto,
						m.principal() ? premio : List.of());
				if (i >= 0 && i < l.size()) l.set(i, limpia);
				else l.add(limpia);
			}
			default -> {
				return;
			}
		}
		d.setDirty();
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
