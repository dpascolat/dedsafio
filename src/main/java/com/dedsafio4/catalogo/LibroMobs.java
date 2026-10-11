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

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			CompoundTag dias = tag.getCompound("dias");
			for (String k : dias.getAllKeys()) {
				List<String> l = new ArrayList<>();
				for (Tag t : dias.getList(k, Tag.TAG_STRING)) l.add(t.getAsString());
				d.dias.put(Integer.parseInt(k), l);
			}
			for (Tag t : tag.getList("revelados", Tag.TAG_STRING)) d.revelados.add(t.getAsString());
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
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_libro_mobs");
	}

	// --- Red ---

	/** Servidor → jugadores: los días ("dia=id,id,,id") y los mobs que ya se ven. */
	public record Payload(List<String> dias, List<String> vistos) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "libro_mobs"));
		public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::dias,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Payload::vistos, Payload::new);

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

	// --- En el cliente (lo llena el Payload) ---

	public static volatile TreeMap<Integer, List<String>> DIAS_CLIENTE = new TreeMap<>();
	public static volatile Set<String> VISTOS_CLIENTE = Set.of();

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
		return new Payload(dias, new ArrayList<>(vistos));
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

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		PayloadTypeRegistry.playC2S().register(EditarPayload.TYPE, EditarPayload.CODEC);
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
