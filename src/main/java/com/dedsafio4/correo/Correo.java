package com.dedsafio4.correo;

import com.dedsafio4.Dedsafio4;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * El correo del Buzón (el bloque Mensajero). Con el "+" cualquier jugador escribe un mensaje a otro y pone objetos
 * en la grilla (se le sacan al mandarlo); los admins también pueden mandarlo a "todos" (cada uno recibe una copia de
 * los objetos). El que lo recibe lo abre en el buzón, saca los objetos de la grilla y lo puede borrar.
 * Arriba a la derecha, debajo de las deditas, aparece un sobre con cuántos mensajes tiene sin leer.
 * Todo queda guardado en el mundo.
 */
public final class Correo {
	private Correo() {}

	public static final int MAX_TEXTO = 500, CASILLAS = 27;
	public static final String TODOS = "todos";

	public static final MenuType<CorreoMenu> MENU = Registry.register(BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "correo"), new MenuType<>(CorreoMenu::new, FeatureFlags.VANILLA_SET));

	/** Un mensaje. para == null: es para todos. asunto: el subtítulo (puede estar vacío). */
	record Carta(int id, UUID autor, String autorNombre, UUID para, String asunto, String texto, List<ItemStack> objetos, long fecha) {}

	/** Lo que ve cada jugador de un mensaje en la lista. segundos: hace cuánto llegó. */
	public record CartaVista(int id, String autor, String asunto, String texto, long segundos, boolean leida, boolean conObjetos) {
		public static final StreamCodec<RegistryFriendlyByteBuf, CartaVista> CODEC = StreamCodec.of((buf, c) -> {
			buf.writeVarInt(c.id);
			buf.writeUtf(c.autor);
			buf.writeUtf(c.asunto);
			buf.writeUtf(c.texto, MAX_TEXTO * 4);
			buf.writeVarLong(c.segundos);
			buf.writeBoolean(c.leida);
			buf.writeBoolean(c.conObjetos);
		}, buf -> new CartaVista(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readUtf(MAX_TEXTO * 4), buf.readVarLong(), buf.readBoolean(),
				buf.readBoolean()));
	}

	// --- En el cliente ---

	/** El mensaje que se está abriendo (-1 = escribiendo uno nuevo); lo usa el menú del lado del cliente. */
	public static volatile int ABIERTA_CLIENTE = -1;

	// --- Red ---

	/** Servidor → jugador: todos sus mensajes (el más nuevo primero). */
	public record BuzonPayload(List<CartaVista> cartas) implements CustomPacketPayload {
		public static final Type<BuzonPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "correo_buzon"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BuzonPayload> CODEC =
				CartaVista.CODEC.apply(ByteBufCodecs.list()).map(BuzonPayload::new, BuzonPayload::cartas);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor → jugador: justo antes de abrir el menú, cuál es (-1 = uno nuevo para escribir). */
	public record AbiertaPayload(int id) implements CustomPacketPayload {
		public static final Type<AbiertaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "correo_abierta"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AbiertaPayload> CODEC =
				ByteBufCodecs.INT.<RegistryFriendlyByteBuf>cast().map(AbiertaPayload::new, AbiertaPayload::id);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Jugador → servidor: "abrir" un mensaje (id -1 = escribir uno nuevo) o "borrar" uno. */
	public record AccionPayload(String accion, int id) implements CustomPacketPayload {
		public static final Type<AccionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "correo_accion"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AccionPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, AccionPayload::accion, ByteBufCodecs.VAR_INT, AccionPayload::id, AccionPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Jugador → servidor: "Enviar" (los objetos son los que puso en la grilla del menú abierto). */
	public record EnviarPayload(String para, String texto) implements CustomPacketPayload {
		public static final Type<EnviarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "correo_enviar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EnviarPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, EnviarPayload::para, ByteBufCodecs.stringUtf8(MAX_TEXTO * 4), EnviarPayload::texto,
				EnviarPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor → jugador: cómo salió el envío (texto vacío = salió bien). */
	public record ResultadoPayload(String error) implements CustomPacketPayload {
		public static final Type<ResultadoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "correo_resultado"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ResultadoPayload> CODEC =
				ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast().map(ResultadoPayload::new, ResultadoPayload::error);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// --- Guardado ---

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		int siguiente = 1;
		final List<Carta> cartas = new ArrayList<>();
		/** Por jugador: los que leyó y los que borró. */
		final Map<UUID, Set<Integer>> leidas = new HashMap<>(), borradas = new HashMap<>();
		/** Por jugador y mensaje: los objetos que le quedan por sacar (si no está, son todos los del mensaje). */
		final Map<UUID, Map<Integer, List<ItemStack>>> restantes = new HashMap<>();

		static boolean tiene(Map<UUID, Set<Integer>> m, UUID j, int id) {
			Set<Integer> s = m.get(j);
			return s != null && s.contains(id);
		}

		void marcar(Map<UUID, Set<Integer>> m, UUID j, int id) {
			if (m.computeIfAbsent(j, k -> new HashSet<>()).add(id)) setDirty();
		}

		List<ItemStack> restantes(UUID j, Carta c) {
			List<ItemStack> l = restantes.getOrDefault(j, Map.of()).get(c.id());
			if (l != null) return l;
			List<ItemStack> copia = new ArrayList<>();
			for (ItemStack o : c.objetos()) copia.add(o.copy());
			return copia;
		}

		private static List<ItemStack> leerObjetos(ListTag l, HolderLookup.Provider registros) {
			List<ItemStack> objetos = new ArrayList<>();
			for (Tag o : l) ItemStack.parse(registros, o).ifPresent(objetos::add);
			return objetos;
		}

		private static ListTag guardarObjetos(List<ItemStack> objetos, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (ItemStack o : objetos) if (!o.isEmpty()) l.add(o.save(registros));
			return l;
		}

		private static Datos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.siguiente = Math.max(1, tag.getInt("siguiente"));
			for (Tag t : tag.getList("cartas", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				d.cartas.add(new Carta(c.getInt("id"), c.getUUID("autor"), c.getString("autor_nombre"),
						c.hasUUID("para") ? c.getUUID("para") : null, c.getString("asunto"), c.getString("texto"),
						leerObjetos(c.getList("objetos", Tag.TAG_COMPOUND), registros), c.getLong("fecha")));
			}
			leerMapa(tag.getCompound("leidas"), d.leidas);
			leerMapa(tag.getCompound("borradas"), d.borradas);
			CompoundTag r = tag.getCompound("restantes");
			for (String j : r.getAllKeys()) {
				Map<Integer, List<ItemStack>> m = new HashMap<>();
				CompoundTag rj = r.getCompound(j);
				for (String id : rj.getAllKeys()) m.put(Integer.parseInt(id), leerObjetos(rj.getList(id, Tag.TAG_COMPOUND), registros));
				d.restantes.put(UUID.fromString(j), m);
			}
			return d;
		}

		private static void leerMapa(CompoundTag tag, Map<UUID, Set<Integer>> m) {
			for (String k : tag.getAllKeys()) {
				Set<Integer> s = new HashSet<>();
				for (int i : tag.getIntArray(k)) s.add(i);
				m.put(UUID.fromString(k), s);
			}
		}

		private static CompoundTag guardarMapa(Map<UUID, Set<Integer>> m) {
			CompoundTag tag = new CompoundTag();
			m.forEach((j, s) -> tag.put(j.toString(), new IntArrayTag(s.stream().mapToInt(Integer::intValue).toArray())));
			return tag;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("siguiente", siguiente);
			ListTag l = new ListTag();
			for (Carta c : cartas) {
				CompoundTag t = new CompoundTag();
				t.putInt("id", c.id());
				t.putUUID("autor", c.autor());
				t.putString("autor_nombre", c.autorNombre());
				if (c.para() != null) t.putUUID("para", c.para());
				t.putString("asunto", c.asunto());
				t.putString("texto", c.texto());
				t.put("objetos", guardarObjetos(c.objetos(), registros));
				t.putLong("fecha", c.fecha());
				l.add(t);
			}
			tag.put("cartas", l);
			tag.put("leidas", guardarMapa(leidas));
			tag.put("borradas", guardarMapa(borradas));
			CompoundTag r = new CompoundTag();
			restantes.forEach((j, m) -> {
				CompoundTag rj = new CompoundTag();
				m.forEach((id, objetos) -> rj.put(String.valueOf(id), guardarObjetos(objetos, registros)));
				r.put(j.toString(), rj);
			});
			tag.put("restantes", r);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_correo");
	}

	// --- Lógica ---

	private static boolean esDe(Datos d, Carta c, UUID jugador) {
		return (c.para() == null || c.para().equals(jugador)) && !Datos.tiene(d.borradas, jugador, c.id());
	}

	private static Carta buscar(Datos d, ServerPlayer p, int id) {
		for (Carta c : d.cartas) if (c.id() == id && esDe(d, c, p.getUUID())) return c;
		return null;
	}

	public static void enviarBuzon(ServerPlayer p) {
		Datos d = datos(p.server);
		long ahora = System.currentTimeMillis();
		List<CartaVista> l = new ArrayList<>();
		for (int i = d.cartas.size() - 1; i >= 0; i--) {
			Carta c = d.cartas.get(i);
			if (!esDe(d, c, p.getUUID())) continue;
			boolean conObjetos = d.restantes(p.getUUID(), c).stream().anyMatch(o -> !o.isEmpty());
			l.add(new CartaVista(c.id(), c.autorNombre(), c.asunto(), c.texto(), Math.max(0, (ahora - c.fecha()) / 1000),
					Datos.tiene(d.leidas, p.getUUID(), c.id()), conObjetos));
		}
		ServerPlayNetworking.send(p, new BuzonPayload(l));
	}

	/** El autor y el texto del mensaje que tiene abierto (para el menú). */
	static Carta carta(ServerPlayer p, int id) {
		return buscar(datos(p.server), p, id);
	}

	/** Al cerrar un mensaje: lo que quedó en la grilla es lo que le queda por sacar. */
	static void guardarRestantes(ServerPlayer p, int id, List<ItemStack> objetos) {
		Datos d = datos(p.server);
		Carta c = buscar(d, p, id);
		if (c == null) return;
		d.restantes.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(id, objetos);
		d.setDirty();
		enviarBuzon(p);
	}

	private static void accion(ServerPlayer p, AccionPayload a) {
		Datos d = datos(p.server);
		if (a.accion().equals("abrir") && a.id() < 0) {
			ServerPlayNetworking.send(p, new AbiertaPayload(-1));
			p.openMenu(new SimpleMenuProvider((id, inv, j) -> new CorreoMenu(id, inv, -1, List.of()), Component.literal("Mensaje")));
			return;
		}
		Carta c = buscar(d, p, a.id());
		if (c == null) return;
		UUID j = p.getUUID();
		switch (a.accion()) {
			case "abrir" -> {
				d.marcar(d.leidas, j, c.id());
				enviarBuzon(p);
				ServerPlayNetworking.send(p, new AbiertaPayload(c.id()));
				List<ItemStack> objetos = d.restantes(j, c);
				p.openMenu(new SimpleMenuProvider((id, inv, jj) -> new CorreoMenu(id, inv, c.id(), objetos), Component.literal("Mensaje")));
			}
			case "borrar" -> {
				// Si quedaban objetos, no se pierden: van al inventario.
				if (p.containerMenu instanceof CorreoMenu m && m.carta == c.id()) p.closeContainer();
				for (ItemStack o : d.restantes(j, c)) {
					ItemStack copia = o.copy();
					if (!copia.isEmpty() && !p.getInventory().add(copia)) p.drop(copia, false);
				}
				d.marcar(d.borradas, j, c.id());
				Map<Integer, List<ItemStack>> r = d.restantes.get(j);
				if (r != null) r.remove(c.id());
				limpiar(d, c);
				d.setDirty();
				enviarBuzon(p);
			}
			default -> {}
		}
	}

	/** Un mensaje para una sola persona que ya lo borró, se saca del todo. */
	private static void limpiar(Datos d, Carta c) {
		if (c.para() == null || !Datos.tiene(d.borradas, c.para(), c.id())) return;
		d.cartas.remove(c);
		Set<Integer> s = d.leidas.get(c.para());
		if (s != null) s.remove(c.id());
		s = d.borradas.get(c.para());
		if (s != null) s.remove(c.id());
	}

	private static String enviar(ServerPlayer p, EnviarPayload e) {
		if (!(p.containerMenu instanceof CorreoMenu menu) || menu.carta != -1) return "Abre el buzón para mandar un mensaje.";
		String texto = e.texto().strip(), para = e.para().strip();
		List<ItemStack> objetos = menu.objetos();
		if (para.isEmpty()) return "Escribe a quién se lo mandas.";
		if (texto.isEmpty() && objetos.isEmpty()) return "El mensaje está vacío.";
		if (texto.length() > MAX_TEXTO) texto = texto.substring(0, MAX_TEXTO);
		boolean aTodos = para.equalsIgnoreCase(TODOS);
		if (aTodos && !p.hasPermissions(2)) return "Solo los admins pueden mandar mensajes a todos.";
		UUID destino = null;
		String nombre = para;
		if (!aTodos) {
			ServerPlayer conectado = p.server.getPlayerList().getPlayerByName(para);
			if (conectado != null) {
				destino = conectado.getUUID();
				nombre = conectado.getGameProfile().getName();
			} else {
				Optional<GameProfile> perfil = p.server.getProfileCache() == null ? Optional.empty() : p.server.getProfileCache().get(para);
				if (perfil.isEmpty()) return "No existe el jugador " + para + ".";
				destino = perfil.get().getId();
				nombre = perfil.get().getName();
			}
			if (destino.equals(p.getUUID())) return "No te puedes mandar un mensaje a ti mismo.";
		}
		menu.vaciar();   // los objetos se van con el mensaje
		Datos d = datos(p.server);
		Carta c = new Carta(d.siguiente++, p.getUUID(), p.getGameProfile().getName(), destino, "", texto, objetos, System.currentTimeMillis());
		d.cartas.add(c);
		d.setDirty();
		// A los que les llegó: el aviso y el buzón al día.
		for (ServerPlayer otro : p.server.getPlayerList().getPlayers()) {
			if (otro == p || (destino != null && !otro.getUUID().equals(destino))) continue;
			enviarBuzon(otro);
			otro.sendSystemMessage(Component.literal("✉ Tienes un mensaje nuevo de ").withStyle(ChatFormatting.AQUA)
					.append(Component.literal(c.autorNombre()).withStyle(ChatFormatting.WHITE))
					.append(Component.literal(". Ábrelo en un Buzón.").withStyle(ChatFormatting.AQUA)));
			otro.level().playSound(null, otro.getX(), otro.getY(), otro.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 0.8f);
		}
		enviarBuzon(p);
		p.sendSystemMessage(Component.literal("✉ Mensaje enviado a " + (aTodos ? "todos" : nombre) + ".").withStyle(ChatFormatting.AQUA));
		p.closeContainer();
		return "";
	}

	/**
	 * Un mensaje que no manda un jugador (por ejemplo, el de Eón con el premio de la Misión Principal). Le llega a
	 * ese jugador con el aviso en el chat.
	 */
	public static void mandar(MinecraftServer server, UUID paraId, String autor, String asunto, String texto, List<ItemStack> objetos) {
		Datos d = datos(server);
		List<ItemStack> copia = new ArrayList<>();
		for (ItemStack o : objetos) if (!o.isEmpty() && copia.size() < CASILLAS) copia.add(o.copy());
		Carta c = new Carta(d.siguiente++, new UUID(0, 0), autor, paraId, asunto, texto, copia, System.currentTimeMillis());
		d.cartas.add(c);
		d.setDirty();
		// Si no está conectado, lo ve cuando entra.
		ServerPlayer para = server.getPlayerList().getPlayer(paraId);
		if (para == null) return;
		enviarBuzon(para);
		para.sendSystemMessage(Component.literal("✉ Tienes un mensaje nuevo de ").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(autor).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(". Ábrelo en un Buzón.").withStyle(ChatFormatting.AQUA)));
		para.level().playSound(null, para.getX(), para.getY(), para.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 0.8f);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(BuzonPayload.TYPE, BuzonPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AbiertaPayload.TYPE, AbiertaPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ResultadoPayload.TYPE, ResultadoPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AccionPayload.TYPE, AccionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EnviarPayload.TYPE, EnviarPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AccionPayload.TYPE, (payload, context) ->
				context.server().execute(() -> accion(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(EnviarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> ServerPlayNetworking.send(context.player(), new ResultadoPayload(enviar(context.player(), payload)))));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enviarBuzon(handler.player));
	}
}
