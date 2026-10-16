package com.dedsafio4.catalogo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.AnuncioMob;
import com.mojang.brigadier.CommandDispatcher;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * El libro de la G ("Cambios & Mobs"): una fila por día, con la ruleta adelante y los mobs de ese día. Los editores
 * del Catálogo eligen qué mob va en cada casilla. Un mob se ve de verdad cuando se habilita con /mob; si no, sale "?".
 * Los mobs que no van con /mob (Herobrine, T-Rex...) se muestran con "/libro revelar <mob>" (y "/libro ocultar <mob>").
 */
public final class LibroMobs {
	private LibroMobs() {}

	/** Cuántos días y cuántos mobs por día como mucho. */
	public static final int MAX_DIAS = 60, POR_DIA = 8;

	// --- Guardado ---

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		/** Día → los ids de los mobs ("dedsafio4:walker"), con "" en las casillas vacías. */
		final TreeMap<Integer, List<String>> dias = new TreeMap<>();
		final Set<String> revelados = new HashSet<>();
		/** La información de cada mob (lo que sale al pasarle el mouse), con colores {color:palabras}; | = otra línea. */
		final TreeMap<String, String> info = new TreeMap<>();
		/** El texto de cada día debajo de la ruleta (si no tiene, "Día N"). */
		final TreeMap<Integer, String> titulos = new TreeMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			CompoundTag dias = tag.getCompound("dias");
			for (String k : dias.getAllKeys()) {
				List<String> l = new ArrayList<>();
				for (Tag t : dias.getList(k, Tag.TAG_STRING)) l.add(t.getAsString());
				d.dias.put(Integer.parseInt(k), l);
			}
			for (Tag t : tag.getList("revelados", Tag.TAG_STRING)) d.revelados.add(t.getAsString());
			CompoundTag info = tag.getCompound("info");
			for (String k : info.getAllKeys()) d.info.put(k, info.getString(k));
			CompoundTag titulos = tag.getCompound("titulos");
			for (String k : titulos.getAllKeys()) d.titulos.put(Integer.parseInt(k), titulos.getString(k));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			CompoundTag dias = new CompoundTag();
			this.dias.forEach((dia, l) -> {
				ListTag lista = new ListTag();
				for (String s : l) lista.add(StringTag.valueOf(s));
				dias.put(String.valueOf(dia), lista);
			});
			tag.put("dias", dias);
			ListTag rev = new ListTag();
			for (String s : revelados) rev.add(StringTag.valueOf(s));
			tag.put("revelados", rev);
			CompoundTag info = new CompoundTag();
			this.info.forEach(info::putString);
			tag.put("info", info);
			CompoundTag titulos = new CompoundTag();
			this.titulos.forEach((dia, t) -> titulos.putString(String.valueOf(dia), t));
			tag.put("titulos", titulos);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_libro_mobs");
	}

	// --- Red ---

	/**
	 * Servidor → jugadores: los días ("dia=id,id,,id"), los mobs que ya se ven, la información de cada mob
	 * ("id=texto") y el texto de cada día ("dia=texto").
	 */
	public record Payload(List<String> dias, List<String> vistos, List<String> info, List<String> titulos) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "libro_mobs"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::dias,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::vistos,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::info,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::titulos, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Un editor → servidor: en ese día y casilla va ese mob ("" = vaciar). */
	public record EditarPayload(int dia, int casilla, String mob) implements CustomPacketPayload {
		public static final Type<EditarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "libro_mobs_editar"));
		public static final StreamCodec<ByteBuf, EditarPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, EditarPayload::dia, ByteBufCodecs.VAR_INT, EditarPayload::casilla,
				ByteBufCodecs.STRING_UTF8, EditarPayload::mob, EditarPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Un editor → servidor: la información de un mob (tipo "mob", clave = id) o el texto de un día (tipo "dia", clave = número). */
	public record TextoPayload(String tipo, String clave, String texto) implements CustomPacketPayload {
		public static final Type<TextoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "libro_mobs_texto"));
		public static final StreamCodec<ByteBuf, TextoPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, TextoPayload::tipo, ByteBufCodecs.STRING_UTF8, TextoPayload::clave,
				ByteBufCodecs.STRING_UTF8, TextoPayload::texto, TextoPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Lo más largo que puede ser la información de un mob y la descripción de un día. */
	public static final int MAX_INFO = 600, MAX_TITULO = 600;

	// --- En el cliente (lo llena el Payload) ---

	public static volatile TreeMap<Integer, List<String>> DIAS_CLIENTE = new TreeMap<>();
	public static volatile Set<String> VISTOS_CLIENTE = Set.of();
	public static volatile java.util.Map<String, String> INFO_CLIENTE = java.util.Map.of();
	public static volatile java.util.Map<Integer, String> TITULOS_CLIENTE = java.util.Map.of();

	/** El nombre de un día: siempre "Día N". */
	public static String tituloDia(int dia) {
		return "Día " + dia;
	}

	/** La descripción de un día (la que escribió un editor; vacía si no hay). */
	public static String descripcionDia(int dia) {
		String t = TITULOS_CLIENTE.get(dia);
		return t == null ? "" : t;
	}

	public static void recibir(Payload p) {
		TreeMap<Integer, List<String>> dias = new TreeMap<>();
		for (String linea : p.dias()) {
			int igual = linea.indexOf('=');
			if (igual < 0) continue;
			List<String> l = new ArrayList<>(List.of(linea.substring(igual + 1).split(",", -1)));
			dias.put(Integer.parseInt(linea.substring(0, igual)), l);
		}
		DIAS_CLIENTE = dias;
		VISTOS_CLIENTE = Set.copyOf(p.vistos());
		java.util.Map<String, String> info = new java.util.HashMap<>();
		for (String linea : p.info()) {
			int igual = linea.indexOf('=');
			if (igual > 0) info.put(linea.substring(0, igual), linea.substring(igual + 1));
		}
		INFO_CLIENTE = info;
		java.util.Map<Integer, String> titulos = new java.util.HashMap<>();
		for (String linea : p.titulos()) {
			int igual = linea.indexOf('=');
			if (igual > 0) titulos.put(Integer.parseInt(linea.substring(0, igual)), linea.substring(igual + 1));
		}
		TITULOS_CLIENTE = titulos;
	}

	// --- Servidor ---

	private static Payload armar(MinecraftServer server) {
		Datos d = datos(server);
		List<String> dias = new ArrayList<>();
		d.dias.forEach((dia, l) -> dias.add(dia + "=" + String.join(",", l)));
		Set<String> vistos = new HashSet<>(d.revelados);
		for (String nombre : AnuncioMob.LUGARES.keySet()) {
			EntityType<?> tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre)).orElse(null);
			if (tipo != null && AnuncioMob.activo(server, tipo)) vistos.add(Dedsafio4.MOD_ID + ":" + nombre);
		}
		List<String> info = new ArrayList<>(), titulos = new ArrayList<>();
		d.info.forEach((id, t) -> info.add(id + "=" + t));
		d.titulos.forEach((dia, t) -> titulos.add(dia + "=" + t));
		return new Payload(dias, new ArrayList<>(vistos), info, titulos);
	}

	/** A todos los conectados (cuando cambia algo: el libro, /mob o /libro). */
	public static void enviarATodos(MinecraftServer server) {
		Payload p = armar(server);
		for (ServerPlayer j : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(j, Payload.TYPE)) ServerPlayNetworking.send(j, p);
		}
	}

	private static void editar(ServerPlayer p, EditarPayload e) {
		if (!Catalogo.esEditor(p) || e.dia() < 1 || e.dia() > MAX_DIAS || e.casilla() < 0 || e.casilla() >= POR_DIA) return;
		String mob = e.mob();
		if (!mob.isEmpty()) {
			ResourceLocation rl = ResourceLocation.tryParse(mob);
			if (rl == null || BuiltInRegistries.ENTITY_TYPE.getOptional(rl).isEmpty()) return;
		}
		Datos d = datos(p.server);
		List<String> fila = d.dias.computeIfAbsent(e.dia(), k -> new ArrayList<>());
		while (fila.size() < POR_DIA) fila.add("");
		fila.set(e.casilla(), mob);
		if (fila.stream().allMatch(String::isEmpty)) d.dias.remove(e.dia());
		d.setDirty();
		enviarATodos(p.server);
	}

	/** Un editor cambió la información de un mob o el texto de un día. */
	private static void editarTexto(ServerPlayer p, TextoPayload t) {
		if (!Catalogo.esEditor(p)) return;
		Datos d = datos(p.server);
		String texto = t.texto().strip();
		if (t.tipo().equals("mob")) {
			ResourceLocation rl = ResourceLocation.tryParse(t.clave());
			if (rl == null || BuiltInRegistries.ENTITY_TYPE.getOptional(rl).isEmpty()) return;
			if (texto.length() > MAX_INFO) texto = texto.substring(0, MAX_INFO);
			if (texto.isEmpty()) d.info.remove(rl.toString());
			else d.info.put(rl.toString(), texto);
		} else if (t.tipo().equals("dia")) {
			int dia;
			try {
				dia = Integer.parseInt(t.clave());
			} catch (NumberFormatException e) {
				return;
			}
			if (dia < 1 || dia > MAX_DIAS) return;
			if (texto.length() > MAX_TITULO) texto = texto.substring(0, MAX_TITULO);
			if (texto.isEmpty()) d.titulos.remove(dia);
			else d.titulos.put(dia, texto);
		} else return;
		d.setDirty();
		enviarATodos(p.server);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		PayloadTypeRegistry.playC2S().register(EditarPayload.TYPE, EditarPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TextoPayload.TYPE, TextoPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TextoPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> editarTexto(contexto.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(EditarPayload.TYPE, (payload, contexto) ->
				contexto.server().execute(() -> editar(contexto.player(), payload)));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, Payload.TYPE)) ServerPlayNetworking.send(handler.player, armar(server));
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("libro").requires(s -> s.hasPermission(2))
				.then(Commands.literal("revelar").then(Commands.argument("mob", ResourceLocationArgument.id())
						.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b))
						.executes(c -> revelar(c.getSource(), ResourceLocationArgument.getId(c, "mob"), true))))
				.then(Commands.literal("ocultar").then(Commands.argument("mob", ResourceLocationArgument.id())
						.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b))
						.executes(c -> revelar(c.getSource(), ResourceLocationArgument.getId(c, "mob"), false)))));
	}

	private static int revelar(CommandSourceStack fuente, ResourceLocation id, boolean ver) {
		if (BuiltInRegistries.ENTITY_TYPE.getOptional(id).isEmpty()) {
			fuente.sendFailure(Component.literal("No existe el mob \"" + id + "\"."));
			return 0;
		}
		Datos d = datos(fuente.getServer());
		if (ver) d.revelados.add(id.toString());
		else d.revelados.remove(id.toString());
		d.setDirty();
		enviarATodos(fuente.getServer());
		fuente.sendSuccess(() -> Component.literal(ver ? "En el libro ya se ve " + id + "." : id + " vuelve a ser \"?\" en el libro.")
				.withStyle(ChatFormatting.AQUA), true);
		return 1;
	}
}
