package com.dedsafio4.herobrine;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.cambios.Cambios;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cambio "herobrine 1": durante las noches, cada 3 a 6 minutos, Herobrine aparece cerca de cada jugador
 * (8 a 12 bloques), quieto y mirándolo. Hay que apuntarle con la Linterna prendida antes de 5 segundos.
 * De día la cuenta se frena.
 */
public final class ModHerobrine {
	private ModHerobrine() {}

	public static final EntityType<HerobrineEntity> HEROBRINE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("herobrine"),
			EntityType.Builder.<HerobrineEntity>of(HerobrineEntity::new, MobCategory.MISC).sized(0.6f, 1.8f)
					.clientTrackingRange(10).build("herobrine"));

	/** Su golpe: 10 corazones justos (no lo frenan la armadura, los encantamientos ni los efectos). */
	public static final ResourceKey<DamageType> DANIO_HEROBRINE = ResourceKey.create(Registries.DAMAGE_TYPE, id("herobrine"));

	/** Cuánto falta (en ticks) para que le aparezca a cada jugador. */
	private static final Map<UUID, Integer> ESPERA = new HashMap<>();
	private static final int ESPERA_MIN = 3 * 60 * 20, ESPERA_MAX = 6 * 60 * 20;
	/** En el Eclipse aparece más seguido: cada 40 a 80 segundos; la primera vez, después de la animación (15 s). */
	private static final int ESPERA_ECLIPSE_MIN = 40 * 20, ESPERA_ECLIPSE_MAX = 80 * 20, PRIMERA_VEZ_ECLIPSE = 15 * 20;
	/** Al activar el cambio, la primera vez (de noche) tarda solo esto. */
	private static final int PRIMERA_VEZ = 10 * 20;

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(HEROBRINE, HerobrineEntity.crearAtributos());
		ServerTickEvents.END_SERVER_TICK.register(ModHerobrine::tick);
	}

	/** Al activar el cambio: aparece pronto a todos. */
	public static void alActivar(MinecraftServer server) {
		ESPERA.clear();
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) ESPERA.put(jugador.getUUID(), PRIMERA_VEZ);
	}

	private static void tick(MinecraftServer server) {
		boolean eclipse = com.dedsafio4.eclipse.Eclipse.activo(server);
		boolean deNoche = Cambios.nivel(server, Cambios.HEROBRINE) >= 1;
		if (!eclipse && !deNoche) {
			ESPERA.clear();
			return;
		}
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
			if (jugador.isSpectator() || jugador.isCreative() || !jugador.isAlive()) continue;
			// En el Eclipse (en el Overworld) aparece seguido; con el cambio, solo de noche.
			boolean enEclipse = eclipse && jugador.level().dimension() == net.minecraft.world.level.Level.OVERWORLD;
			if (!enEclipse && !(deNoche && jugador.level().isNight())) continue;
			int min = enEclipse ? ESPERA_ECLIPSE_MIN : ESPERA_MIN, max = enEclipse ? ESPERA_ECLIPSE_MAX : ESPERA_MAX;
			RandomSource r = jugador.getRandom();
			int falta = Math.min(max, ESPERA.computeIfAbsent(jugador.getUUID(), u -> min + r.nextInt(max - min))) - 1;
			if (falta > 0) {
				ESPERA.put(jugador.getUUID(), falta);
				continue;
			}
			ESPERA.put(jugador.getUUID(), min + r.nextInt(max - min));
			if (!yaTiene(jugador)) aparecer(jugador);
		}
	}

	/** Al empezar el Eclipse: aparece enseguida a todos. */
	public static void alEmpezarEclipse(MinecraftServer server) {
		for (ServerPlayer jugador : server.getPlayerList().getPlayers()) ESPERA.put(jugador.getUUID(), PRIMERA_VEZ_ECLIPSE);
	}

	/** Al terminar el Eclipse: se van todos los Herobrine. */
	public static void sacarTodos(MinecraftServer server) {
		for (ServerLevel mundo : server.getAllLevels()) {
			for (HerobrineEntity h : mundo.getEntities(HEROBRINE, e -> true)) h.discard();
		}
	}

	private static boolean yaTiene(ServerPlayer jugador) {
		return !jugador.serverLevel().getEntities(HEROBRINE, h -> jugador.getUUID().equals(h.objetivo())).isEmpty();
	}

	/** Busca un lugar con piso a 8-12 bloques del jugador y lo hace aparecer ahí. */
	public static HerobrineEntity aparecer(ServerPlayer jugador) {
		ServerLevel mundo = jugador.serverLevel();
		RandomSource r = jugador.getRandom();
		for (int intento = 0; intento < 20; intento++) {
			double angulo = r.nextDouble() * Math.PI * 2, distancia = 8 + r.nextDouble() * 4;
			BlockPos columna = BlockPos.containing(jugador.getX() + Math.cos(angulo) * distancia, jugador.getY() + 4,
					jugador.getZ() + Math.sin(angulo) * distancia);
			for (int dy = 0; dy < 10; dy++) {
				BlockPos pies = columna.below(dy);
				if (mundo.getBlockState(pies).isAir() && mundo.getBlockState(pies.above()).isAir()
						&& mundo.getBlockState(pies.below()).isFaceSturdy(mundo, pies.below(), net.minecraft.core.Direction.UP)) {
					return aparecerEn(jugador, Vec3.atBottomCenterOf(pies));
				}
			}
		}
		return null;
	}

	public static HerobrineEntity aparecerEn(ServerPlayer jugador, Vec3 pos) {
		ServerLevel mundo = jugador.serverLevel();
		HerobrineEntity herobrine = new HerobrineEntity(HEROBRINE, mundo);
		herobrine.moveTo(pos.x, pos.y, pos.z, 0, 0);
		herobrine.setObjetivo(jugador);
		mundo.addFreshEntity(herobrine);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1f, 0.8f);
		return herobrine;
	}
}
