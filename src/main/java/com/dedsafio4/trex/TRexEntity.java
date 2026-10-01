package com.dedsafio4.trex;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * T-Rex gigante (40 bloques de alto): un jefe que maneja un admin. Va lento hacia donde mira el jinete;
 * con R ruge y con G (más adelante) abre una puerta. Es una carrera: si pisa a un jugador, ese jugador
 * pasa a modo espectador. No se lo puede lastimar.
 *
 * La animación de caminar se calcula igual en el servidor y en el cliente (así el servidor sabe dónde
 * están las patas para ver a quién pisa).
 */
public class TRexEntity extends PathfinderMob {
	public static final byte EVENTO_RUGIDO = 61;
	/** El rugido dura 3,2 segundos. */
	public static final int TICKS_RUGIDO = 64;
	/** 2,4 radianes por segundo, como en el diseño. */
	private static final float FASE_POR_TICK = 2.4f / 20f;
	/** El jinete va parado arriba de la cabeza, al lado del cuerno (lo único con vista hacia adelante). */
	/** El pivote de la cabeza (relativo al cuerpo, que está 5 bloques adelante) y los ojos en la cabeza. */
	private static final Vec3 PIVOTE_CUERPO_CABEZA = new Vec3(0, 32, 13), OJOS = new Vec3(0, 5, 4.5);

	/** Animación: fase de la caminata y qué tanto está caminando (0 quieto, 1 caminando). */
	public float fase, faseAntes, mezcla, mezclaAntes;
	/** Ticks desde que empezó a rugir (-1 si no está rugiendo). */
	public int rugido = -1;

	public TRexEntity(EntityType<? extends PathfinderMob> tipo, Level level) {
		super(tipo, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1000)
				.add(Attributes.MOVEMENT_SPEED, 0.08)
				.add(Attributes.STEP_HEIGHT, 3)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	// --- Jinete ---

	@Override
	protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
		if (!getPassengers().isEmpty() || !(jugador.hasPermissions(2) || jugador.isCreative()) || com.dedsafio4.AdminComandos.bloqueado(jugador)) return InteractionResult.PASS;
		if (!level().isClientSide) jugador.startRiding(this);
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player jugador ? jugador : null;
	}

	@Override
	protected void tickRidden(Player jugador, Vec3 movimiento) {
		super.tickRidden(jugador, movimiento);
		setRot(jugador.getYRot(), 0);
		yRotO = yBodyRot = yHeadRot = getYRot();
	}

	/** Solo adelante (o un poco para atrás); se dobla mirando. */
	@Override
	protected Vec3 getRiddenInput(Player jugador, Vec3 movimiento) {
		float adelante = jugador.zza > 0 ? 1 : jugador.zza < 0 ? -0.4f : 0;
		return new Vec3(0, 0, adelante);
	}

	@Override
	protected float getRiddenSpeed(Player jugador) {
		return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
	}

	/** Cuánto del rugido está en marcha (0 a 1), como en el diseño. */
	public float fuerzaRugido(float parcial) {
		if (rugido < 0) return 0;
		float tau = (rugido + parcial) / 20f;
		return suave(tau / 0.5f) * (1 - suave((tau - 2.4f) / 0.8f));
	}

	public static float suave(float x) {
		return x < 0 ? 0 : x > 1 ? 1 : x * x * (3 - 2 * x);
	}

	/**
	 * El jinete ve desde los ojos del T-Rex: sus ojos quedan justo donde están los del T-Rex, siguiendo lo
	 * mismo que el dibujo (el cuerpo que sube y baja al caminar; al rugir, el cuerpo que se inclina y baja
	 * y la cabeza que se levanta). Para el jinete la cabeza no se dibuja (ver TRexRenderer).
	 */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions dimensiones, float escala) {
		float r = fuerzaRugido(0);
		double cuerpoY = (0.25 - Math.abs(Math.sin(fase)) * 0.5) * mezcla * (1 - r) - r * 0.6;
		Vec3 enElCuerpo = PIVOTE_CUERPO_CABEZA.add(rotX(OJOS, -0.45 * r));
		Vec3 ojos = new Vec3(0, cuerpoY, 5).add(rotX(enElCuerpo, -0.12 * r));
		// Los pies del jinete van más abajo, para que sus ojos queden en los del T-Rex.
		double bajar = pasajero.getEyeHeight() - pasajero.getVehicleAttachmentPoint(this).y;
		return ojos.subtract(0, bajar, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	/** Al bajarse, queda en el suelo al costado (y no cae 30 bloques). */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity pasajero) {
		return position().add(new Vec3(14, 0, 5).yRot(-getYRot() * Mth.DEG_TO_RAD));
	}

	// --- Animación y pisotones ---

	@Override
	public void tick() {
		super.tick();
		faseAntes = fase;
		mezclaAntes = mezcla;
		boolean camina = Math.hypot(getX() - xo, getZ() - zo) > 0.01;
		mezcla = Mth.approach(mezcla, camina ? 1 : 0, 0.08f);
		fase += FASE_POR_TICK * mezcla;
		if (rugido >= 0 && ++rugido > TICKS_RUGIDO) rugido = -1;
		if (level() instanceof ServerLevel mundo) pisar(mundo);
	}

	/** Dónde está el centro de un pie (0 izquierdo, 1 derecho), en bloques, relativo al T-Rex y sin girar. */
	public static Vec3 pie(int lado, float fase, float mezcla) {
		double ph = fase + lado * Math.PI;
		double balanceo = Math.sin(ph) * 0.42 * mezcla, doblez = Math.max(0, Math.cos(ph)) * 0.7 * mezcla;
		double cadera = balanceo - doblez * 0.5, rodilla = doblez;
		double cuerpoY = (0.25 - Math.abs(Math.sin(fase)) * 0.5) * mezcla;
		Vec3 p = new Vec3((lado == 0 ? -1 : 1) * 8, 19 + cuerpoY, 5);
		p = p.add(rotX(new Vec3(0, -8, -0.5), cadera));
		p = p.add(rotX(new Vec3(0, -8, 0), cadera + rodilla));
		return p.add(0, -2, 2);
	}

	/** Giro alrededor del eje x como en three.js (positivo: lo de abajo va para atrás). */
	private static Vec3 rotX(Vec3 v, double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
	}

	/** Si un pie apoyado cae sobre un jugador, ese jugador pasa a espectador. */
	private void pisar(ServerLevel mundo) {
		if (mezcla < 0.1f) return;
		for (int lado = 0; lado < 2; lado++) {
			Vec3 local = pie(lado, fase, mezcla);
			if (local.y - 1 > 1.2) continue;   // el pie está levantado
			Vec3 centro = position().add(new Vec3(local.x, 0, local.z).yRot(-getYRot() * Mth.DEG_TO_RAD));
			AABB zona = new AABB(centro.x - 4, getY() - 0.5, centro.z - 4, centro.x + 4, getY() + 3, centro.z + 4);
			for (ServerPlayer jugador : mundo.getEntitiesOfClass(ServerPlayer.class, zona)) {
				if (jugador.isSpectator() || jugador.isCreative() || hasPassenger(jugador)) continue;
				aplastar(mundo, jugador);
			}
		}
	}

	private void aplastar(ServerLevel mundo, ServerPlayer jugador) {
		jugador.setGameMode(GameType.SPECTATOR);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f);
		mundo.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
				jugador.getX(), jugador.getY() + 0.3, jugador.getZ(), 40, 0.6, 0.2, 0.6, 0.2);
		Component aviso = Component.literal("¡El T-Rex aplastó a " + jugador.getName().getString() + "!").withColor(0xF07A1C);
		mundo.getServer().getPlayerList().broadcastSystemMessage(aviso, false);
	}

	// --- Rugido ---

	/** Ruge: animación para todos, sonido fuerte y "¡ROAAARRR!" en pantalla a los que están cerca. */
	public void rugir() {
		if (rugido >= 0 || !(level() instanceof ServerLevel mundo)) return;
		rugido = 0;
		mundo.broadcastEntityEvent(this, EVENTO_RUGIDO);
		mundo.playSound(null, getX(), getY() + 30, getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8f, 0.5f);
		mundo.playSound(null, getX(), getY() + 30, getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 8f, 0.5f);
		Component texto = Component.literal("¡ROAAARRR!").withColor(0x7B3FC4);
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.distanceToSqr(this) < 128 * 128) jugador.displayClientMessage(texto, true);
		}
	}

	@Override
	public void handleEntityEvent(byte evento) {
		if (evento == EVENTO_RUGIDO) rugido = 0;
		else super.handleEntityEvent(evento);
	}

	// --- Jefe: no se lastima, no se empuja, no desaparece ---

	@Override
	public boolean isInvulnerableTo(DamageSource fuente) {
		return !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 256 * 256;
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}
}
