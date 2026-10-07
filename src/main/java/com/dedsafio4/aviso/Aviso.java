package com.dedsafio4.aviso;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /aviso <color> <texto>: a todos les aparece arriba a la izquierda un cartel con ese texto en ese color (el mismo
 * tamaño con cualquier escala de interfaz). Si el texto tiene una "|", lo de antes es el título: /aviso rojo ¡Nos
 * convertimos en Nutrias | Los Guardianes y Gigi... Cada uno lo oculta con la O. /aviso quitar lo saca a todos.
 * Queda guardado: los que entran después también lo ven.
 */
public final class Aviso {
	private Aviso() {}

	public static final Map<String, Integer> COLORES = new LinkedHashMap<>();
	static {
		COLORES.put("rojo", 0xFF3B30);
		COLORES.put("naranja", 0xFF9A2E);
		COLORES.put("amarillo", 0xFFE14A);
		COLORES.put("verde", 0x5BE35B);
		COLORES.put("celeste", 0x55D9F0);
		COLORES.put("azul", 0x5A7CFF);
		COLORES.put("morado", 0xB35CFF);
		COLORES.put("rosa", 0xFF7AC8);
		COLORES.put("blanco", 0xFFFFFF);
		COLORES.put("gris", 0xBDBDBD);
	}

	/** Servidor → jugador: el aviso (texto vacío = sacarlo). */
	public record Payload(int color, String titulo, String texto) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "aviso"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, Payload::color, ByteBufCodecs.STRING_UTF8, Payload::titulo, ByteBufCodecs.STRING_UTF8, Payload::texto,
				Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		int color;
		String titulo = "", texto = "";

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.color = tag.getInt("color");
			d.titulo = tag.getString("titulo");
			d.texto = tag.getString("texto");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("color", color);
			tag.putString("titulo", titulo);
			tag.putString("texto", texto);
			return tag;
		}

		Payload payload() {
			return new Payload(color, titulo, texto);
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_aviso");
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Datos d = datos(server);
			if (!d.texto.isEmpty()) ServerPlayNetworking.send(handler.player, d.payload());
		});
	}

	private static void enviarATodos(MinecraftServer server, Payload p) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(jugador, p);
	}

	/** El color: uno de los nombres o #RRGGBB. Devuelve -1 si no se entiende. */
	private static int color(String c) {
		c = c.toLowerCase(java.util.Locale.ROOT);
		if (COLORES.containsKey(c)) return COLORES.get(c);
		String hex = c.startsWith("#") ? c.substring(1) : c;
		if (hex.matches("[0-9a-f]{6}")) return Integer.parseInt(hex, 16);
		return -1;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("aviso").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.texto = "";
					d.titulo = "";
					d.setDirty();
					enviarATodos(c.getSource().getServer(), d.payload());
					c.getSource().sendSuccess(() -> Component.literal("Aviso quitado.").withStyle(ChatFormatting.GOLD), true);
					return 1;
				}))
				.then(Commands.argument("color", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(COLORES.keySet(), b))
						.then(Commands.argument("texto", StringArgumentType.greedyString()).executes(c -> {
							int color = color(StringArgumentType.getString(c, "color"));
							if (color < 0) {
								c.getSource().sendFailure(Component.literal("Ese color no existe. Usa: " + String.join(", ", COLORES.keySet())
										+ " o uno como #FF8800."));
								return 0;
							}
							String todo = StringArgumentType.getString(c, "texto");
							int barra = todo.indexOf('|');
							Datos d = datos(c.getSource().getServer());
							d.color = color;
							d.titulo = barra >= 0 ? todo.substring(0, barra).trim() : "";
							d.texto = (barra >= 0 ? todo.substring(barra + 1) : todo).trim();
							d.setDirty();
							enviarATodos(c.getSource().getServer(), d.payload());
							c.getSource().sendSuccess(() -> Component.literal("Aviso puesto.").withStyle(ChatFormatting.GOLD), true);
							return 1;
						}))));
	}
}
