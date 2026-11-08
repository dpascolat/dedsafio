package com.dedsafio4.bestias;

import com.dedsafio4.items.Linternas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ojo Volador (modelo "OjoVolador" de Patricio): un ojo gigante que flota, te persigue y te muerde.
 * Si te acercas demasiado (AURA bloques) te hace mucho daño. Es invulnerable: solo lo daña la Linterna (si un
 * jugador le apunta con la Linterna prendida, se va poniendo blanco y al segundo de luz desaparece en una nube de
 * humo, sin soltar nada). /kill igual lo mata.
 * Con "/mob ojo_volador" aparece solo en TODO el Limbo (también donde no aparecen otras criaturas): no usa la
 * aparición normal de Minecraft, lo pone {@link #registrar()} cerca de los jugadores que están en el Limbo.
 */
public class OjoVoladorEntity extends Monster {
	/** Un segundo con la luz encima: desaparece. */
	public static final int TICKS_CON_LUZ = 20;
	/** Qué tan alumbrado está (0 nada, 1 a punto de desaparecer), para el dibujo. */
	private static final EntityDataAccessor<Float> LUZ = SynchedEntityData.defineId(OjoVoladorEntity.class, EntityDataSerializers.FLOAT);

	/** A cuántos bloques lastima, cada cuánto (ticks) y cuánto. */
	public static final double AURA = 3.5;
	private static final int CADA_GOLPE = 20;
	private static final float DANIO_AURA = 10f;
	/** Aparición en el Limbo: cada cuánto se prueba por jugador, la chance, y cuántos ojos como máximo cerca de cada uno. */
	private static final int CADA_APARICION = 20 * 20;
	private static final float CHANCE_APARICION = 0.35f;
	private static final int MAXIMO_CERCA = 2;

	private int conLuz;

	public OjoVoladorEntity(EntityType<? extends OjoVoladorEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.setNoGravity(true);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(LUZ, 0f);
	}

	public float luz() {
		return entityData.get(LUZ);
	}

	@Override
	protected PathNavigation createNavigation(Level mundo) {
		FlyingPathNavigation navegacion = new FlyingPathNavigation(this, mundo);
		navegacion.setCanOpenDoors(false);
		navegacion.setCanFloat(true);
		navegacion.setCanPassDoors(true);
		return navegacion;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo) || !isAlive()) return;
		// Alumbrado con la Linterna: se pone blanco y al segundo desaparece. Si le sacan la luz, se va recuperando.
		boolean alumbrado = mundo.players().stream().anyMatch(p -> !p.isSpectator() && Linternas.ilumina(p, this));
		conLuz = alumbrado ? conLuz + 1 : Math.max(0, conLuz - 2);
		entityData.set(LUZ, Math.min(1f, conLuz / (float) TICKS_CON_LUZ));
		if (alumbrado && conLuz % 4 == 0) mundo.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.5), getZ(), 4, 0.4, 0.5, 0.4, 0.01);
		if (conLuz >= TICKS_CON_LUZ) {
			desaparecer(mundo);
			return;
		}
		// Si te acercas demasiado, mucho daño.
		if (tickCount % CADA_GOLPE == 0) {
			for (Player p : mundo.getEntitiesOfClass(Player.class, getBoundingBox().inflate(AURA))) {
				if (p.isCreative() || p.isSpectator() || !p.isAlive() || distanceTo(p) > AURA) continue;
				p.hurt(damageSources().mobAttack(this), DANIO_AURA);
			}
		}
	}

	/** Invulnerable: solo la Linterna lo hace desaparecer (y /kill, o el vacío). */
	@Override
	public boolean isInvulnerableTo(DamageSource fuente) {
		return !fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	/** No muerde: lastima con el aura (ver tick). */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return true;
	}

	/** Lo pone solo cerca de los jugadores que están en el Limbo, si está habilitado con /mob ojo_volador. */
	public static void registrar() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CADA_APARICION != 0) return;
			ServerLevel limbo = server.getLevel(com.dedsafio4.dimension.Limbo.DIMENSION);
			if (limbo == null || limbo.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
			if (!AnuncioMob.activo(server, com.dedsafio4.nave.ModEntidades.OJO_VOLADOR)) return;
			for (Player p : limbo.players()) {
				if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
				if (limbo.random.nextFloat() >= CHANCE_APARICION) continue;
				if (limbo.getEntitiesOfClass(OjoVoladorEntity.class, p.getBoundingBox().inflate(48)).size() >= MAXIMO_CERCA) continue;
				aparecerCerca(limbo, p);
			}
		});
	}

	private static void aparecerCerca(ServerLevel limbo, Player p) {
		var azar = limbo.random;
		for (int intento = 0; intento < 10; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2, distancia = 14 + azar.nextDouble() * 14;
			double x = p.getX() + Math.cos(angulo) * distancia, z = p.getZ() + Math.sin(angulo) * distancia;
			double y = p.getY() + 1 + azar.nextInt(5);
			OjoVoladorEntity ojo = com.dedsafio4.nave.ModEntidades.OJO_VOLADOR.create(limbo);
			if (ojo == null) return;
			ojo.moveTo(x, y, z, azar.nextFloat() * 360f, 0f);
			if (!limbo.noCollision(ojo) || !limbo.isLoaded(ojo.blockPosition())) continue;
			ojo.finalizeSpawn(limbo, limbo.getCurrentDifficultyAt(ojo.blockPosition()), net.minecraft.world.entity.MobSpawnType.EVENT, null);
			limbo.addFreshEntity(ojo);
			return;
		}
	}

	private void desaparecer(ServerLevel mundo) {
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 30, 0.5, 0.7, 0.5, 0.02);
		mundo.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.6f);
		discard();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity otro) {
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PHANTOM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7f;
	}
}
