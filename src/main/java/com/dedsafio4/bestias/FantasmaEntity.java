package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.nave.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Los fantasmas del Limbo (del Dedsafío 3). Vuelan y persiguen a los jugadores:
 * - Fantasma Amarillo (el dorado): si te golpea, te mata al instante (solo te salva un tótem) y desaparece.
 * - Fantasma Blanco: golpea a gran velocidad y te da Lentitud.
 * - Fantasma Rojo: golpea a gran velocidad.
 * Cada uno suelta su gema el 20% de las veces: Roja (Rojo), Blanca (Blanco), Gris (Negro) y Dorada (Amarillo).
 * - Fantasma Negro: cada golpe te quita un corazón para siempre (como el agua del Limbo; vuelve con /limbo devolver).
 * Cuando ven a un jugador vuelan derecho hacia él, suave y rápido (el triple que al principio), y como son
 * fantasmas atraviesan las paredes. Sin nadie cerca, flotan despacio por ahí.
 */
public class FantasmaEntity extends Monster implements GeoEntity {
	public static final SoundEvent SONIDO_AMBIENTE = sonido("fantasma_ambiente"), SONIDO_GOLPE = sonido("fantasma_golpe");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	public FantasmaEntity(EntityType<? extends FantasmaEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.xpReward = 8;
		this.moveControl = new FlyingMoveControl(this, 10, true);
		setNoGravity(true);
	}

	private static SoundEvent sonido(String nombre) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private boolean amarillo() {
		return getType() == ModEntidades.FANTASMA_AMARILLO;
	}

	private boolean blanco() {
		return getType() == ModEntidades.FANTASMA_BLANCO;
	}

	private boolean negro() {
		return getType() == ModEntidades.FANTASMA_NEGRO;
	}

	private boolean rojo() {
		return getType() == ModEntidades.FANTASMA_ROJO;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				// El triple de vida que al principio (16). La velocidad de persecución está en VELOCIDAD.
				.add(Attributes.MAX_HEALTH, 48.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.4)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
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
		// La persecución y el ataque los hace customServerAiStep; esto es solo para cuando no hay nadie.
		goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Bloques por tick al perseguir (0,45 = 9 por segundo: el triple de lo que iban al principio). */
	private static final double VELOCIDAD = 0.45;
	/** A qué distancia pega (desde el centro de cada uno). */
	private static final double ALCANCE = 1.6;
	private int espera;

	/**
	 * Si tiene a quién perseguir: vuela derecho hacia el medio del cuerpo del jugador, acelerando y frenando suave,
	 * mirándolo, y atravesando paredes. Pega cuando está cerca: el Blanco y el Rojo cada 6 ticks, el Amarillo y el Negro
	 * cada 20.
	 */
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (espera > 0) espera--;
		LivingEntity objetivo = getTarget();
		boolean persigue = objetivo != null && objetivo.isAlive() && !(objetivo instanceof Player p && (p.isCreative() || p.isSpectator()))
				&& distanceToSqr(objetivo) < 48 * 48;
		noPhysics = persigue;
		if (!persigue) return;
		getNavigation().stop();
		net.minecraft.world.phys.Vec3 hacia = objetivo.position().add(0, objetivo.getBbHeight() * 0.5, 0)
				.subtract(position().add(0, getBbHeight() * 0.5, 0));
		double distancia = hacia.length();
		if (distancia > 0.01) {
			// Cerca, frena para no pasarse de largo.
			double rapidez = Math.min(VELOCIDAD, distancia * 0.25);
			net.minecraft.world.phys.Vec3 quiere = hacia.scale(rapidez / distancia);
			setDeltaMovement(getDeltaMovement().lerp(quiere, 0.25));
			float giro = (float) (Math.toDegrees(Math.atan2(hacia.z, hacia.x)) - 90);
			setYRot(giro);
			yBodyRot = giro;
			yHeadRot = giro;
			setXRot((float) -Math.toDegrees(Math.atan2(hacia.y, Math.sqrt(hacia.x * hacia.x + hacia.z * hacia.z))));
		}
		if (distancia < ALCANCE + objetivo.getBbWidth() * 0.5 && espera <= 0) {
			espera = amarillo() || negro() ? 20 : 6;
			swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			doHurtTarget(objetivo);
		}
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		triggerAnim("ataque", "golpe");
		level().playSound(null, getX(), getY(), getZ(), SONIDO_GOLPE, SoundSource.HOSTILE, 1f, 1f);
		if (amarillo() && objetivo instanceof LivingEntity vivo) {
			// Un golpe que mata (la armadura no alcanza), pero el tótem sí te salva. Después el fantasma desaparece.
			vivo.invulnerableTime = 0;
			vivo.hurt(damageSources().mobAttack(this), Float.MAX_VALUE);
			if (level() instanceof ServerLevel mundo) {
				mundo.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.9, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
				mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.9, getZ(), 15, 0.3, 0.5, 0.3, 0.02);
			}
			discard();
			return true;
		}
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && negro() && objetivo instanceof net.minecraft.server.level.ServerPlayer jugador && !jugador.isCreative()) {
			com.dedsafio4.dimension.Limbo.quitarCorazon(jugador, "El Fantasma Negro te quitó un corazón para siempre.");
		}
		if (pego && blanco() && objetivo instanceof LivingEntity vivo) {
			vivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
		}
		return pego;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel mundo, DamageSource fuente, boolean jugadorLoMato) {
		super.dropCustomDeathLoot(mundo, fuente, jugadorLoMato);
		if (random.nextFloat() >= 0.2f) return;
		if (rojo()) spawnAtLocation(com.dedsafio4.items.ModItems.GEMA_ROJA);
		else if (blanco()) spawnAtLocation(com.dedsafio4.items.ModItems.GEMA_BLANCA);
		else if (negro()) spawnAtLocation(com.dedsafio4.items.ModItems.GEMA_GRIS);
		else if (amarillo()) spawnAtLocation(com.dedsafio4.items.ModItems.GEMA_DORADA);
	}

	// Vuela: no se cae ni le pasa nada al caer.
	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {}

	@Override
	protected SoundEvent getAmbientSound() {
		return SONIDO_AMBIENTE;
	}

	// --- Animaciones (las del Dedsafío 3) ---

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		String mueve = rojo() ? "fly" : "walk", golpe = rojo() ? "hit" : "attack";
		RawAnimation moviendo = RawAnimation.begin().thenLoop(mueve), quieto = RawAnimation.begin().thenLoop("idle");
		controladores.add(new AnimationController<>(this, "movimiento", 4,
				estado -> estado.setAndContinue(estado.isMoving() ? moviendo : quieto)));
		controladores.add(new AnimationController<>(this, "ataque", 2, estado -> software.bernie.geckolib.animation.PlayState.STOP)
				.triggerableAnim("golpe", RawAnimation.begin().thenPlay(golpe)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
