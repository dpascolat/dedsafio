package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Soarer: la bestia voladora, los nuevos Phantoms. Vuela dando vueltas alto sobre el jugador, cada tanto se tira en
 * picada, lo AGARRA con las patas, se lo lleva para arriba y lo suelta desde lo alto (no te podés bajar con shift; si
 * le pegás mientras te lleva puede que te suelte antes). Sin nadie a la vista pasea planeando.
 * Animaciones (las que pasó el usuario): planear, aletear (subiendo o llevando a alguien), picada y atacar (al agarrar).
 */
public class SoarerEntity extends Monster implements GeoEntity {
	private static final RawAnimation PLANEAR = RawAnimation.begin().thenLoop("move.glide");
	private static final RawAnimation ALETEAR = RawAnimation.begin().thenLoop("move.wing_flap");
	private static final RawAnimation PICADA = RawAnimation.begin().thenLoop("move.dive");
	private static final RawAnimation ATACAR = RawAnimation.begin().thenPlay("attack.attack");

	/** Qué está haciendo (para la animación). */
	public static final int PLANEA = 0, ALETEA = 1, EN_PICADA = 2, LLEVA = 3;
	private static final EntityDataAccessor<Integer> ESTADO =
			SynchedEntityData.defineId(SoarerEntity.class, EntityDataSerializers.INT);
	/** El evento que manda el servidor cuando agarra a alguien (para la animación de ataque). */
	private static final byte EVENTO_AGARRE = 60;

	/** Volando en círculos, en picada, llevando a alguien y paseando (bloques por tick). */
	private static final double VELOCIDAD_CIRCULO = 0.5, VELOCIDAD_PICADA = 0.95, VELOCIDAD_LLEVANDO = 0.4, VELOCIDAD_PASEO = 0.25;
	/** Radio y altura de las vueltas sobre el jugador, y a qué distancia lo agarra en la picada. */
	private static final double RADIO_CIRCULO = 14, ALTURA_CIRCULO = 13, ALCANCE_AGARRE = 2.6;

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	/** Ticks que dura la animación de ataque en pantalla. */
	private int atacando;
	/** Ticks hasta la próxima picada, ticks en picada y ticks llevando a alguien. */
	private int espera = 60, ticksPicada, ticksLlevando;
	/** Por dónde va de la vuelta alrededor del jugador, y a qué altura suelta al que lleva. */
	private double angulo, alturaSoltar;
	private Vec3 destinoPaseo;

	public SoarerEntity(EntityType<? extends SoarerEntity> tipo, Level level) {
		super(tipo, level);
		this.setNoGravity(true);
		this.xpReward = 20;
		this.angulo = random.nextDouble() * Math.PI * 2;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(ESTADO, PLANEA);
	}

	public int estado() {
		return entityData.get(ESTADO);
	}

	private void estado(int nuevo) {
		if (estado() != nuevo) entityData.set(ESTADO, nuevo);
	}

	@Override
	protected void registerGoals() {
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	/** El vuelo lo maneja él mismo: vueltas sobre el jugador, picada, agarre y subida. */
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		Vec3 centro = position();
		Vec3 hacia;
		double velocidad;
		LivingEntity blanco = getTarget();
		if (getFirstPassenger() instanceof Player llevado) {
			// Se lo lleva para arriba (y un poco para adelante) y lo suelta desde lo alto.
			estado(LLEVA);
			ticksLlevando++;
			Vec3 adelante = Vec3.directionFromRotation(0, getYRot());
			hacia = centro.add(adelante.scale(4)).add(0, 3, 0);
			velocidad = VELOCIDAD_LLEVANDO;
			if (getY() >= alturaSoltar || ticksLlevando > 160 || !llevado.isAlive()) soltar();
		} else if (blanco != null && blanco.isAlive()) {
			if (estado() == EN_PICADA) {
				hacia = blanco.position().add(0, blanco.getBbHeight() * 0.5, 0);
				velocidad = VELOCIDAD_PICADA;
				if (centro.distanceTo(blanco.position()) < ALCANCE_AGARRE + blanco.getBbWidth() / 2) agarrar(blanco);
				else if (++ticksPicada > 80) volverACircular(60);
			} else {
				// Da vueltas alto sobre el jugador, como los Phantoms; cada tanto se tira en picada.
				angulo += 0.035;
				hacia = blanco.position().add(Math.cos(angulo) * RADIO_CIRCULO, ALTURA_CIRCULO, Math.sin(angulo) * RADIO_CIRCULO);
				velocidad = VELOCIDAD_CIRCULO;
				estado(hacia.y > getY() + 2 ? ALETEA : PLANEA);
				if (--espera <= 0 && hasLineOfSight(blanco)) {
					estado(EN_PICADA);
					ticksPicada = 0;
					playSound(SoundEvents.PHANTOM_SWOOP, 2f, 0.6f);
				}
			}
		} else {
			// Nadie a la vista: pasea planeando, bien alto sobre el terreno.
			if (destinoPaseo == null || destinoPaseo.distanceTo(centro) < 3 || tickCount % 200 == 0) {
				double x = getX() + (random.nextDouble() - 0.5) * 40, z = getZ() + (random.nextDouble() - 0.5) * 40;
				double suelo = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
				destinoPaseo = new Vec3(x, suelo + 18 + random.nextDouble() * 10, z);
			}
			hacia = destinoPaseo;
			velocidad = VELOCIDAD_PASEO;
			estado(hacia.y > getY() + 2 ? ALETEA : PLANEA);
		}
		Vec3 direccion = hacia.subtract(centro);
		if (direccion.lengthSqr() > 0.01) {
			Vec3 deseado = direccion.normalize().scale(velocidad);
			// Gira suave hacia donde quiere ir (no frena en seco).
			setDeltaMovement(getDeltaMovement().scale(0.8).add(deseado.scale(0.2)));
			float giro = (float) (Mth.atan2(direccion.z, direccion.x) * Mth.RAD_TO_DEG) - 90f;
			setYRot(Mth.approachDegrees(getYRot(), giro, 12f));
			yBodyRot = yHeadRot = getYRot();
		}
		if (tickCount % 30 == 0) level().playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.5f, 0.5f);
	}

	/** Lo agarra con las patas (si es un jugador) y se lo lleva; a otro bicho sólo le pega. */
	private void agarrar(LivingEntity blanco) {
		doHurtTarget(blanco);
		level().broadcastEntityEvent(this, EVENTO_AGARRE);
		level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 2f, 0.8f);
		if (blanco instanceof Player p && blanco.isAlive() && !p.isPassenger() && !isVehicle() && !p.isSpectator()) {
			p.startRiding(this, true);
			alturaSoltar = getY() + 20 + random.nextDouble() * 10;
			ticksLlevando = 0;
			estado(LLEVA);
		} else {
			volverACircular(80);
		}
	}

	/** Suelta al que lleva (se cae desde lo alto) y vuelve a dar vueltas. */
	private void soltar() {
		ejectPassengers();
		level().playSound(null, blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 2f, 0.6f);
		volverACircular(100 + random.nextInt(80));
	}

	private void volverACircular(int ticks) {
		espera = ticks;
		estado(ALETEA);
	}

	/** Si le pegan mientras lleva a alguien, a veces lo suelta antes. */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		boolean lastimado = super.hurt(fuente, cantidad);
		if (lastimado && !level().isClientSide && isVehicle() && random.nextFloat() < 0.35f) soltar();
		return lastimado;
	}

	/** El agarrado cuelga de las patas, abajo del cuerpo. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions dimensiones, float escala) {
		return new Vec3(0, -pasajero.getBbHeight() + 0.2, 0.3).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public void handleEntityEvent(byte evento) {
		if (evento == EVENTO_AGARRE) atacando = 20;
		else super.handleEntityEvent(evento);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.atacando > 0) this.atacando--;
	}

	// Vuela: no se cae ni se lastima al caer.
	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PHANTOM_AMBIENT;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "principal", 5, this::animar));
	}

	private PlayState animar(AnimationState<SoarerEntity> estado) {
		if (this.atacando > 0) return estado.setAndContinue(ATACAR);
		return estado.setAndContinue(switch (estado()) {
			case EN_PICADA -> PICADA;
			case ALETEA, LLEVA -> ALETEAR;
			default -> PLANEAR;
		});
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
