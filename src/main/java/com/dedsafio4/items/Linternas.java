package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Lo que hace la Linterna prendida del lado del servidor: lastima a las criaturas sensibles a la luz que
 * estén en el haz y va gastando la batería. La luz en sí (el haz que sale de la linterna y lo que alumbra)
 * la dibuja el cliente (ver LinternaLuz).
 */
public final class Linternas {
	private Linternas() {}

	/** Hasta dónde llega el haz, en bloques. */
	public static final double ALCANCE = 16;
	/** Medio ancho del haz, en grados. */
	public static final double APERTURA = 12;
	/** Cada cuántos ticks gasta un punto de batería (33 puntos = unos 5 minutos y medio prendida). */
	public static final int TICKS_POR_PUNTO = 200;
	/** Las criaturas a las que lastima el haz (se pueden agregar más en el tag). */
	public static final TagKey<EntityType<?>> SENSIBLES = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "sensibles_a_la_luz"));
	private static final float DANIO = 3f;

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(Linternas::tick);
	}

	/** La linterna prendida que tiene el jugador en alguna mano, o vacío. */
	public static ItemStack linternaPrendida(Player jugador) {
		for (ItemStack pila : new ItemStack[]{jugador.getMainHandItem(), jugador.getOffhandItem()}) {
			if (pila.is(ModItems.LINTERNA) && LinternaItem.prendida(pila)) return pila;
		}
		return ItemStack.EMPTY;
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			if (!jugador.isAlive() || jugador.isSpectator()) continue;
			ItemStack linterna = linternaPrendida(jugador);
			if (linterna.isEmpty()) continue;
			ServerLevel mundo = jugador.serverLevel();
			if (mundo.getGameTime() % 10 == 0) quemar(mundo, jugador);
			// Batería.
			if (!jugador.getAbilities().instabuild && mundo.getGameTime() % TICKS_POR_PUNTO == 0) {
				LinternaItem.gastar(linterna);
				if (LinternaItem.sinBateria(linterna)) {
					LinternaItem.prender(linterna, false);
					jugador.displayClientMessage(net.minecraft.network.chat.Component.literal("La Linterna se quedó sin batería.")
							.withColor(0xFF5555), true);
				}
			}
		}
	}

	/** ¿El jugador tiene la Linterna prendida y le está apuntando a esa entidad (dentro del haz y sin nada en el medio)? */
	public static boolean ilumina(Player jugador, Entity entidad) {
		if (linternaPrendida(jugador).isEmpty() || jugador.level() != entidad.level()) return false;
		Vec3 hacia = entidad.getBoundingBox().getCenter().subtract(jugador.getEyePosition());
		double distancia = hacia.length();
		if (distancia > ALCANCE || distancia < 0.1) return false;
		if (hacia.normalize().dot(jugador.getLookAngle()) < Math.cos(Math.toRadians(APERTURA))) return false;
		return jugador.hasLineOfSight(entidad);
	}

	/** Las criaturas sensibles a la luz dentro del haz se queman y reciben daño. */
	private static void quemar(ServerLevel mundo, ServerPlayer jugador) {
		for (LivingEntity bicho : mundo.getEntitiesOfClass(LivingEntity.class, jugador.getBoundingBox().inflate(ALCANCE),
				e -> e.getType().is(SENSIBLES) && e.isAlive())) {
			if (!ilumina(jugador, bicho)) continue;
			bicho.igniteForSeconds(2);
			bicho.hurt(mundo.damageSources().indirectMagic(jugador, jugador), DANIO);
		}
	}
}
