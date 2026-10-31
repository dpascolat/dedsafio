package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.HuevoConDescripcion;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * /mob <nombre>: ese mob del mod empieza a aparecer solo en su lugar (antes no aparece, solo con el huevo), y a todos
 * les llega el anuncio al chat: "Nuevo Mob", su nombre, su descripción y dónde aparece. /mob quitar <nombre> lo vuelve
 * a sacar y /mob lista dice cuáles están. Queda guardado en el mundo. SpawnPlacementsMixin es el que no los deja
 * aparecer si no están habilitados.
 */
public final class AnuncioMob {
	private AnuncioMob() {}

	private static final ResourceLocation FUENTE_ICONOS = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos");
	private static final String ICONO = String.valueOf((char) 0xE001);
	private static final int COLOR_TITULO = 0x7CFF8A, COLOR_TEXTO = 0xE8F4E0, COLOR_LUGAR = 0xE0609F;

	/** Los mobs que aparecen solos, y dónde (id → lugar). */
	public static final Map<String, String> LUGARES = new LinkedHashMap<>();
	static {
		LUGARES.put("dromoraptor_rojo", "la Sabana del Centro de Quiu, en manada");
		LUGARES.put("dromoraptor_azul", "la Sabana del Centro de Quiu, en manada");
		LUGARES.put("walker", "la Jungla del Centro de Quiu");
		LUGARES.put("dactylo_bebe", "el Centro de Quiu");
		LUGARES.put("mira", "las cuevas del Centro de Quiu");
		LUGARES.put("zarinosa_espigueya", "las cuevas del Centro de Quiu");
		LUGARES.put("narval", "el agua del Centro de Quiu");
		LUGARES.put("creeper_nuclear", "los biomas de nieve y el desierto");
		LUGARES.put("creeper_pastel", "todo el Overworld, a oscuras");
		LUGARES.put("creeper_papel", "todo el Overworld, de día");
		LUGARES.put("creeper_resorte", "todo el Overworld, de día");
		LUGARES.put("creeper_acuatico", "el agua del Overworld");
		LUGARES.put("flashbang", "el Limbito, el bioma del Limbo");
		LUGARES.put("bomba_warden", "la Oscuridad Profunda y las cuevas");
		LUGARES.put("creeper_amarillo", "la Dimensión de los Órganos");
		LUGARES.put("nautilus_oseo", "la Dimensión de los Órganos, en grupo");
		LUGARES.put("nautilus_oseo_grande", "la Dimensión de los Órganos");
		LUGARES.put("parasito_volador", "las Verrugas Claras y Oscuras de Gleba");
		LUGARES.put("gusano_carne", "los Jugos Gástricos de Gleba");
		LUGARES.put("cerebro_amarillo", "la Dimensión de los Órganos");
		LUGARES.put("garrapata_cerebral", "la Dimensión de los Órganos");
		LUGARES.put("wraith", "todo el Overworld, a oscuras");
		LUGARES.put("nave", "el bioma Phora");
		LUGARES.put("nave_veloz", "el bioma Phora");
		LUGARES.put("fantasma_amarillo", "el Limbo y el Limbito");
		LUGARES.put("fantasma_blanco", "el Limbo y el Limbito");
		LUGARES.put("fantasma_rojo", "el Limbo y el Limbito");
		LUGARES.put("fantasma_negro", "el Limbo y el Limbito");
	}

	/** Mobs cuyo id interno no es su nombre: en el comando se escriben con el nombre (id → nombre). */
	private static final Map<String, String> APODOS = Map.of("walker", "plumosaurio");

	private static String apodo(String id) {
		return APODOS.getOrDefault(id, id);
	}

	/** Del nombre escrito en el comando al id del mob (acepta los dos). */
	private static String idDe(String nombre) {
		String n = nombre.toLowerCase(java.util.Locale.ROOT);
		for (Map.Entry<String, String> e : APODOS.entrySet()) if (e.getValue().equals(n)) return e.getKey();
		return n;
	}

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		final Set<String> activos = new HashSet<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("activos", Tag.TAG_STRING)) d.activos.add(t.getAsString());
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag lista = new ListTag();
			for (String s : activos) lista.add(StringTag.valueOf(s));
			tag.put("activos", lista);
			return tag;
		}
	}

	public static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_mobs");
	}

	/** ¿Es uno de los mobs que se habilitan con /mob? */
	public static boolean controla(EntityType<?> tipo) {
		ResourceLocation id = EntityType.getKey(tipo);
		return id.getNamespace().equals(Dedsafio4.MOD_ID) && LUGARES.containsKey(id.getPath());
	}

	public static boolean activo(MinecraftServer server, EntityType<?> tipo) {
		return server != null && datos(server).activos.contains(EntityType.getKey(tipo).getPath());
	}

	public static void registrar() {
		AparicionQuiu.registrar();
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mob").requires(s -> s.hasPermission(2))
				.then(Commands.literal("lista").executes(AnuncioMob::lista))
				.then(Commands.literal("quitar").then(Commands.argument("nombre", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(datos(c.getSource().getServer()).activos.stream().map(AnuncioMob::apodo), b))
						.executes(c -> quitar(c, StringArgumentType.getString(c, "nombre")))))
				.then(Commands.argument("nombre", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(LUGARES.keySet().stream().map(AnuncioMob::apodo), b))
						.executes(c -> activar(c, StringArgumentType.getString(c, "nombre")))));
	}

	private static EntityType<?> tipo(String nombre) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre)).orElse(null);
	}

	private static int activar(CommandContext<CommandSourceStack> c, String nombre) {
		nombre = idDe(nombre);
		EntityType<?> tipo = tipo(nombre);
		if (tipo == null || !LUGARES.containsKey(nombre)) {
			c.getSource().sendFailure(Component.literal("No existe el mob \"" + nombre + "\" (o todavía no tiene un lugar donde aparecer)."));
			return 0;
		}
		Datos d = datos(c.getSource().getServer());
		d.activos.add(nombre);
		d.setDirty();
		// El anuncio en el chat, como los de /cambio.
		Component mensaje = Component.empty()
				.append(Component.literal(ICONO).withStyle(Style.EMPTY.withFont(FUENTE_ICONOS)))
				.append(Component.literal(" Nuevo Mob: ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_TITULO)).withBold(true)))
				.append(tipo.getDescription().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_TITULO)).withBold(true)));
		String descripcion = descripcion(tipo);
		if (!descripcion.isEmpty()) {
			mensaje = mensaje.copy().append(Component.literal("\n" + descripcion).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_TEXTO))));
		}
		mensaje = mensaje.copy().append(Component.literal("\nAparece en " + LUGARES.get(nombre) + ".")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR_LUGAR))));
		if (!com.dedsafio4.cambios.TextosOcultos.oculto(c.getSource().getServer(), "mob")) {
			c.getSource().getServer().getPlayerList().broadcastSystemMessage(mensaje, false);
		} else {
			c.getSource().sendSuccess(() -> Component.literal("Mob habilitado (sin anuncio).").withStyle(ChatFormatting.GRAY), true);
		}
		com.dedsafio4.catalogo.LibroMobs.enviarATodos(c.getSource().getServer());
		return 1;
	}

	/** La primera parte de la descripción del huevo (hasta el renglón vacío), en un solo párrafo. */
	private static String descripcion(EntityType<?> tipo) {
		List<Component> lineas = new ArrayList<>();
		if (SpawnEggItem.byId(tipo) instanceof HuevoConDescripcion huevo) huevo.describir(lineas);
		StringBuilder texto = new StringBuilder();
		for (Component linea : lineas) {
			String s = linea.getString().trim();
			if (s.isEmpty()) {
				if (texto.length() > 0) break;
				continue;
			}
			if (texto.length() > 0) texto.append(' ');
			texto.append(s);
		}
		return texto.toString();
	}

	private static int quitar(CommandContext<CommandSourceStack> c, String nombre) {
		Datos d = datos(c.getSource().getServer());
		if (!d.activos.remove(idDe(nombre))) {
			c.getSource().sendFailure(Component.literal("Ese mob no estaba habilitado."));
			return 0;
		}
		d.setDirty();
		com.dedsafio4.catalogo.LibroMobs.enviarATodos(c.getSource().getServer());
		c.getSource().sendSuccess(() -> Component.literal("El mob " + nombre + " ya no aparece solo.").withStyle(ChatFormatting.GOLD), true);
		return 1;
	}

	private static int lista(CommandContext<CommandSourceStack> c) {
		List<String> activos = datos(c.getSource().getServer()).activos.stream().map(AnuncioMob::apodo).toList();
		List<String> faltan = LUGARES.keySet().stream().map(AnuncioMob::apodo).filter(n -> !activos.contains(n)).toList();
		c.getSource().sendSuccess(() -> Component.literal("Aparecen solos: " + (activos.isEmpty() ? "ninguno" : String.join(", ", activos))
				+ "\nTodavía no: " + (faltan.isEmpty() ? "ninguno" : String.join(", ", faltan))).withStyle(ChatFormatting.GOLD), false);
		return 1;
	}
}
