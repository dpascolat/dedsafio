package com.dedsafio4.herobrine;

import com.dedsafio4.items.Linternas;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Herobrine: aparece cerca de un jugador y se queda quieto mirándolo. NO hay que mirarlo a los ojos: al
 * que lo mira (sin alumbrarlo) le saca 10 corazones y desaparece. Si le apuntan con la Linterna
 * prendida se va poniendo rojo y a los 2 segundos de luz desaparece. Si nadie hace nada, al minuto se va.
 * No se lo puede lastimar.
 */
public class HerobrineEntity extends PathfinderMob {
	/** Medio segundo mirándolo a los ojos: ataca. */
	public static final int TICKS_MIRADA = 10;
	/** Si nadie hace nada, al minuto se va. */
	public static final int VIDA = 60 * 20;
	/** 2 segundos de luz: desaparece. */
	public static final int TICKS_CON_LUZ = 40;
	/** 10 corazones. */
	public static final float DANIO = 20;

	private static final EntityDataAccessor<Float> ROJO = SynchedEntityData.defineId(HerobrineEntity.class, EntityDataSerializers.FLOAT);

	private UUID objetivo;
	private int conLuz;
	/** Cuántos ticks seguidos lo viene mirando a los ojos cada jugador. */
	private final java.util.Map<UUID, Integer> miradas = new java.util.HashMap<>();

	public HerobrineEntity(EntityType<? extends PathfinderMob> tipo, Level level) {
		super(tipo, level);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ROJO, 0f);
	}

	/** Qué tan rojo está (0 normal, 1 del todo rojo). */
	public float rojo() {
		return entityData.get(ROJO);
	}

	public void setObjetivo(ServerPlayer jugador) {
		this.objetivo = jugador.getUUID();
	}

	public UUID objetivo() {
		return objetivo;
	}

	/** ¿El jugador lo está mirando a los ojos? (como con los Endermen: la mira justo en sus ojos y sin nada en el medio). */
	public boolean loMira(Player jugador) {
		net.minecraft.world.phys.Vec3 mira = jugador.getViewVector(1f).normalize();
		net.minecraft.world.phys.Vec3 hacia = new net.minecraft.world.phys.Vec3(getX() - jugador.getX(), getEyeY() - jugador.getEyeY(), getZ() - jugador.getZ());
		double distancia = hacia.length();
		return distancia < 64 && mira.dot(hacia.normalize()) > 1 - 0.025 / distancia && jugador.hasLineOfSight(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo)) return;
		// Si apareció sin jugador (con el huevo), va por el jugador más cercano.
		if (objetivo == null && mundo.getNearestPlayer(this, 64) instanceof ServerPlayer cercano) objetivo = cercano.getUUID();
		ServerPlayer jugador = objetivo == null ? null : mundo.getServer().getPlayerList().getPlayer(objetivo);
		if (jugador == null || !jugador.isAlive() || jugador.level() != mundo || distanceToSqr(jugador) > 64 * 64) {
			desaparecer(mundo);
			return;
		}
		// Siempre mirando al jugador.
		getLookControl().setLookAt(jugador, 180, 180);
		double dx = jugador.getX() - getX(), dz = jugador.getZ() - getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);

		// Alumbrado con la Linterna: se pone rojo y desaparece.
		boolean iluminado = mundo.players().stream().anyMatch(p -> Linternas.ilumina(p, this));
		if (iluminado) {
			miradas.clear();
			conLuz++;
			entityData.set(ROJO, Math.min(1f, conLuz / (float) TICKS_CON_LUZ));
			if (conLuz % 4 == 0) mundo.sendParticles(ParticleTypes.SMOKE, getX(), getY(1), getZ(), 3, 0.2, 0.4, 0.2, 0.01);
			if (conLuz >= TICKS_CON_LUZ) desaparecer(mundo);
			return;
		}
		// Mirarlo a los ojos (sin alumbrarlo): al que lo mira, lo ataca.
		for (ServerPlayer p : mundo.players()) {
			if (p.isSpectator() || p.isCreative() || !p.isAlive() || !loMira(p)) {
				miradas.remove(p.getUUID());
				continue;
			}
			if (miradas.merge(p.getUUID(), 1, Integer::sum) >= TICKS_MIRADA) {
				atacar(mundo, p);
				return;
			}
		}
		if (tickCount > VIDA) desaparecer(mundo);
	}

	private void atacar(ServerLevel mundo, ServerPlayer jugador) {
		DamageSource fuente = new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(ModHerobrine.DANIO_HEROBRINE), this);
		jugador.hurt(fuente, DANIO);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1f, 0.6f);
		desaparecer(mundo);
	}

	private void desaparecer(ServerLevel mundo) {
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 25, 0.3, 0.8, 0.3, 0.02);
		mundo.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.5f);
		discard();
	}

	/** No se lo puede lastimar (solo con /kill). */
	@Override
	public boolean isInvulnerableTo(DamageSource fuente) {
		return !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(net.minecraft.world.entity.Entity otro) {}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	/** No se guarda: si se cierra el mundo, se va. */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	/** Solo lo ve y lo ataca a su jugador; los demás no lo "provocan". */
	@Override
	public boolean canAttack(net.minecraft.world.entity.LivingEntity otro) {
		return otro instanceof Player && otro.getUUID().equals(objetivo);
	}
}
