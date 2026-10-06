package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * /mob <nombre>: a todos les aparece arriba de la pantalla el cartel "Nuevo Mob - Nombre" con el mob, su nombre y su
 * descripción (la del huevo), como los anuncios del Dedsafío. Sirve para cualquier mob del mod.
 */
public final class AnuncioMob {
	private AnuncioMob() {}

	public record Payload(String mob) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "anuncio_mob"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Payload::mob, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
	}

	/** Los mobs del mod que se pueden anunciar (los bichos, no las cosas como las naves o las raíces). */
	private static java.util.stream.Stream<EntityType<?>> mobs() {
		return BuiltInRegistries.ENTITY_TYPE.stream()
				.filter(t -> EntityType.getKey(t).getNamespace().equals(Dedsafio4.MOD_ID) && t.getCategory() != MobCategory.MISC);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mob").requires(s -> s.hasPermission(2))
				.then(Commands.argument("nombre", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(mobs().map(t -> EntityType.getKey(t).getPath()), b))
						.executes(c -> {
							String nombre = StringArgumentType.getString(c, "nombre").toLowerCase(java.util.Locale.ROOT);
							ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
							EntityType<?> tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
							if (tipo == null) {
								c.getSource().sendFailure(Component.literal("No existe el mob \"" + nombre + "\"."));
								return 0;
							}
							for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
								ServerPlayNetworking.send(p, new Payload(id.toString()));
							}
							c.getSource().sendSuccess(() -> Component.literal("Anuncio: Nuevo Mob - ").append(tipo.getDescription())
									.withStyle(ChatFormatting.GOLD), true);
							return 1;
						})));
	}
}
