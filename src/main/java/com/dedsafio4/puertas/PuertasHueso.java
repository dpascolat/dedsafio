package com.dedsafio4.puertas;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** La Puerta de Hueso y Chatarra (ver PuertaHuesoEntity) y /puertita. */
public final class PuertasHueso {
	private PuertasHueso() {}

	public static final EntityType<PuertaHuesoEntity> PUERTA = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("puerta_hueso"),
			EntityType.Builder.<PuertaHuesoEntity>of(PuertaHuesoEntity::new, MobCategory.MISC).sized(2f, 2f)
					.clientTrackingRange(10).updateInterval(20).build("puerta_hueso"));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("puerta_hueso"),
			new PuertaHuesoItem(new Item.Properties()));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Sólo carga la clase (registra lo de arriba). */
	public static void registrar() {}

	/**
	 * /puertita <grueso> <alto> (en bloques): si estás mirando una Puerta de Hueso (a menos de 10 bloques), le cambia el
	 * tamaño; si no, te da una con ese tamaño.
	 */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("puertita").requires(s -> s.hasPermission(2))
				.then(Commands.argument("grueso", FloatArgumentType.floatArg(0.05f, 8f))
						.then(Commands.argument("alto", FloatArgumentType.floatArg(0.5f, 32f))
								.executes(c -> {
									ServerPlayer jugador = c.getSource().getPlayerOrException();
									float grueso = FloatArgumentType.getFloat(c, "grueso"), alto = FloatArgumentType.getFloat(c, "alto");
									String tam = "grosor " + PuertaHuesoItem.numero(grueso) + " y alto " + PuertaHuesoItem.numero(alto);
									PuertaHuesoEntity mirada = mirada(jugador);
									if (mirada != null) {
										mirada.tamaño(grueso, alto);
										c.getSource().sendSuccess(() -> Component.literal("La Puerta de Hueso ahora tiene " + tam + ".")
												.withStyle(ChatFormatting.GREEN), false);
										return 1;
									}
									var pila = PuertaHuesoItem.con(grueso, alto);
									if (!jugador.getInventory().add(pila)) jugador.drop(pila, false);
									c.getSource().sendSuccess(() -> Component.literal("Te di una Puerta de Hueso con " + tam + ".")
											.withStyle(ChatFormatting.GREEN), false);
									return 1;
								}))));
	}

	/** La Puerta de Hueso que está mirando el jugador (a menos de 10 bloques), si hay. */
	private static PuertaHuesoEntity mirada(ServerPlayer jugador) {
		Vec3 ojos = jugador.getEyePosition(), fin = ojos.add(jugador.getLookAngle().scale(10));
		EntityHitResult r = ProjectileUtil.getEntityHitResult(jugador, ojos, fin, jugador.getBoundingBox().expandTowards(fin.subtract(ojos)).inflate(1),
				e -> e instanceof PuertaHuesoEntity, 100);
		return r != null && r.getEntity() instanceof PuertaHuesoEntity p ? p : null;
	}
}
