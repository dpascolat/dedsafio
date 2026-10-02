package com.dedsafio4.despegue;

import com.dedsafio4.dimension.Organos;
import com.dedsafio4.dimension.Portales;
import com.dedsafio4.nave.PartesNave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La Nave Espacial Biplaza parada sobre la Plataforma de Despegue, con la punta para arriba.
 * - Click derecho: te subís (entran 2). Apenas sube alguien empieza la cuenta regresiva (5 segundos); si se bajan
 *   todos, se cancela.
 * - Despegue: fuego y humo abajo, sube cada vez más rápido y, bien arriba, viaja a la Dimensión de los Órganos.
 * - Llegada: aparece en el cielo de la otra dimensión y baja despacio hasta que toca el piso. Ahí se queda quieta
 *   y te bajás vos (Shift). No vuelve a despegar hasta que alguien se baje y se suba de nuevo.
 * - Desde la Dimensión de los Órganos, subirse de nuevo te lleva de vuelta al Overworld.
 * - Shift + click derecho: el dueño la guarda en el inventario (sólo cuando está quieta y vacía).
 */
public class NaveViajeEntity extends Entity implements GeoEntity {
	private static final EntityDataAccessor<Integer> ESTADO =
			SynchedEntityData.defineId(NaveViajeEntity.class, EntityDataSerializers.INT);
	public static final int QUIETA = 0, CUENTA = 1, DESPEGANDO = 2, ATERRIZANDO = 3;

	/** Cuenta regresiva: 5 segundos. Despegue: 7 segundos subiendo antes de saltar a la otra dimensión. */
	public static final int TIEMPO_CUENTA = 100, TIEMPO_DESPEGUE = 140;
	/** A cuántos bloques del piso aparece al llegar. */
	private static final double ALTURA_LLEGADA = 45;
	private static final int AMARILLO = 0xF0D86A, NARANJA = 0xFFA23C, VIOLETA = 0xC883FF;
	/** Dónde van los pasajeros dentro de la cabina, corridos a mano (1 píxel = 1/16 de bloque). */
	private static final double AJUSTE_X = 9 / 16.0, AJUSTE_Y = 7 / 16.0;

	private UUID dueno;
	private String nombreDueno = "";
	private int tiempo;
	private double velocidad;
	/** Dónde se apoya al terminar de aterrizar (NaN = calcularlo cuando haga falta). */
	private double sueloLlegada = Double.NaN;
	/** Recién aterrizó con gente adentro: no arranca otra cuenta regresiva hasta que se bajen todos. */
	private boolean recienLlegada;
	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	public NaveViajeEntity(EntityType<? extends NaveViajeEntity> tipo, Level level) {
		super(tipo, level);
		this.noCulling = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(ESTADO, QUIETA);
	}

	public int estado() {
		return entityData.get(ESTADO);
	}

	private void estado(int nuevo) {
		entityData.set(ESTADO, nuevo);
		tiempo = 0;
	}

	// --- Ponerla en la plataforma (lo llama la Nave Espacial Biplaza) ---

	public static InteractionResult colocar(UseOnContext contexto) {
		Level level = contexto.getLevel();
		BlockPos pos = contexto.getClickedPos();
		Player jugador = contexto.getPlayer();
		if (!level.getBlockState(pos).is(ModDespegue.PLATAFORMA_DESPEGUE)) {
			if (!level.isClientSide && jugador != null) {
				jugador.displayClientMessage(Component.literal("Colócala en el centro de una Plataforma de Despegue.")
						.withColor(AMARILLO), true);
			}
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!level.dimension().equals(Level.OVERWORLD)) {
			if (jugador != null) jugador.displayClientMessage(
					Component.literal("La Plataforma sólo funciona en el Overworld.").withColor(AMARILLO), true);
			return InteractionResult.FAIL;
		}
		if (!level.getEntitiesOfClass(NaveViajeEntity.class, new AABB(pos.above()).inflate(0.5, 3, 0.5)).isEmpty()) {
			if (jugador != null) jugador.displayClientMessage(
					Component.literal("Ya hay una nave en esta plataforma.").withColor(AMARILLO), true);
			return InteractionResult.FAIL;
		}
		NaveViajeEntity nave = ModDespegue.NAVE_VIAJE.create(level);
		if (nave == null) return InteractionResult.FAIL;
		nave.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, jugador == null ? 0 : jugador.getYRot(), 0);
		if (jugador != null) {
			nave.dueno = jugador.getUUID();
			nave.nombreDueno = jugador.getGameProfile().getName();
		}
		level.addFreshEntity(nave);
		level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.4f);
		if (jugador == null || !jugador.getAbilities().instabuild) contexto.getItemInHand().shrink(1);
		return InteractionResult.CONSUME;
	}

	// --- Subirse, bajarse y guardarla ---

	@Override
	public InteractionResult interact(Player jugador, InteractionHand mano) {
		if (level().isClientSide) return InteractionResult.SUCCESS;
		if (jugador.isSecondaryUseActive()) {
			if (dueno != null && !dueno.equals(jugador.getUUID()) && !jugador.getAbilities().instabuild) {
				jugador.displayClientMessage(Component.literal("Esta nave es de " + nombreDueno + ".").withColor(AMARILLO), true);
				return InteractionResult.CONSUME;
			}
			if (estado() != QUIETA || isVehicle()) {
				jugador.displayClientMessage(Component.literal("No se puede guardar mientras está en uso.").withColor(AMARILLO), true);
				return InteractionResult.CONSUME;
			}
			ItemStack nave = new ItemStack(PartesNave.NAVE_BIPLAZA);
			if (!jugador.getInventory().add(nave)) jugador.drop(nave, false);
			level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.8f);
			discard();
			return InteractionResult.CONSUME;
		}
		if (estado() == DESPEGANDO || estado() == ATERRIZANDO) return InteractionResult.PASS;
		if (!canAddPassenger(jugador)) {
			jugador.displayClientMessage(Component.literal("La nave está llena (entran 2).").withColor(AMARILLO), true);
			return InteractionResult.CONSUME;
		}
		return jugador.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	protected boolean canAddPassenger(Entity pasajero) {
		return getPassengers().size() < 2;
	}

	/** Los dos van adentro de la cabina, uno al lado del otro, acostados boca arriba (ver PlayerRendererNaveMixin). */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions dimensiones, float escala) {
		int lugar = Math.max(0, getPassengers().indexOf(pasajero));
		return new Vec3((lugar == 0 ? -0.4 : 0.4) + AJUSTE_X, 2.0 + AJUSTE_Y, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	/** Al bajarse queda parado al costado de la nave (busca un lado libre). */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity pasajero) {
		for (int i = 0; i < 4; i++) {
			Vec3 lugar = position().add(new Vec3(2.2, 0, 0).yRot((-getYRot() + 90 * i) * Mth.DEG_TO_RAD));
			BlockPos pie = BlockPos.containing(lugar);
			if (level().getBlockState(pie).getCollisionShape(level(), pie).isEmpty()
					&& level().getBlockState(pie.above()).getCollisionShape(level(), pie.above()).isEmpty()) {
				return lugar;
			}
		}
		return super.getDismountLocationForPassenger(pasajero);
	}

	private boolean tienePiloto() {
		for (Entity p : getPassengers()) if (p instanceof Player) return true;
		return false;
	}

	// --- La animación: cuenta regresiva, despegue y aterrizaje ---

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo)) return;
		tiempo++;
		switch (estado()) {
			case QUIETA -> {
				if (!isVehicle()) recienLlegada = false;
				else if (recienLlegada && tiempo % 60 == 1) aviso("Aterrizaste. Apretá Shift para bajarte.");
				if (tienePiloto() && !recienLlegada) estado(CUENTA);
			}
			case CUENTA -> {
				if (!tienePiloto()) {
					estado(QUIETA);
					return;
				}
				if (tiempo % 20 == 1) {
					int falta = 5 - (tiempo - 1) / 20;
					titulo(String.valueOf(falta), NARANJA, "Despegue en...");
					sonido(mundo, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 0.6f + (5 - falta) * 0.15f);
				}
				// Humo en la base, cada vez más, y la nave que empieza a temblar (ver NaveViajeModelo).
				mundo.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(),
						1 + tiempo / 25, 0.7, 0.05, 0.7, 0.01);
				if (tiempo > 60) mundo.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 2, 0.2, 0.05, 0.2, 0.01);
				if (tiempo >= TIEMPO_CUENTA) {
					estado(DESPEGANDO);
					velocidad = 0;
					titulo("¡Despegue!", NARANJA, "");
					sonido(mundo, SoundEvents.FIREWORK_ROCKET_LAUNCH, 4f, 0.5f);
					sonido(mundo, SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.6f);
					mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
					mundo.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 60, 1.5, 0.2, 1.5, 0.08);
				}
			}
			case DESPEGANDO -> {
				velocidad = Math.min(1.6, velocidad + 0.01 + tiempo * 0.0004);
				setPos(getX(), getY() + velocidad, getZ());
				fuego(mundo, 1f);
				if (tiempo % 6 == 0) sonido(mundo, SoundEvents.BLAZE_SHOOT, 2f, 0.5f);
				if (tiempo == 70) titulo("", NARANJA, "Saliendo de la atmósfera...");
				if (tiempo >= TIEMPO_DESPEGUE) viajar(mundo);
			}
			case ATERRIZANDO -> {
				if (Double.isNaN(sueloLlegada)) {
					sueloLlegada = mundo.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition()).getY();
				}
				double falta = getY() - sueloLlegada;
				double bajada = Mth.clamp(falta * 0.05, 0.08, 0.9);
				if (falta <= 0) bajada = 0.08;
				// Frena cuando el motor toca el piso de verdad (si está más abajo de lo calculado al llegar, sigue
				// bajando; si no hay piso, frena en el fondo del mundo).
				double piso = pisoDebajo(mundo, bajada);
				boolean fondo = getY() - bajada <= mundo.getMinBuildHeight();
				if (!Double.isNaN(piso) || fondo) {
					setPos(getX(), Double.isNaN(piso) ? getY() : piso, getZ());
					estado(QUIETA);
					recienLlegada = isVehicle();
					sonido(mundo, SoundEvents.ANVIL_LAND, 1f, 0.6f);
					mundo.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 40, 1.5, 0.1, 1.5, 0.05);
					titulo("Aterrizaste", VIOLETA, "Apretá Shift para bajarte");
				} else {
					setPos(getX(), getY() - bajada, getZ());
					fuego(mundo, 0.4f);
					if (tiempo % 10 == 0) sonido(mundo, SoundEvents.BLAZE_SHOOT, 1f, 0.4f);
				}
			}
			default -> estado(QUIETA);
		}
	}

	/**
	 * Si al bajar {@code bajada} bloques el centro (donde está el motor) toca algo sólido o agua, devuelve la altura
	 * donde se apoya; si no, NaN. Sólo el centro: si mirara las puntas, en una loma quedaría flotando.
	 */
	private double pisoDebajo(ServerLevel mundo, double bajada) {
		BlockPos debajo = BlockPos.containing(getX(), getY() - bajada - 0.01, getZ());
		boolean solido = !mundo.getBlockState(debajo).getCollisionShape(mundo, debajo).isEmpty()
				|| !mundo.getFluidState(debajo).isEmpty();
		if (!solido) return Double.NaN;
		// Se apoya arriba de lo que tenga (un bloque entero, una losa, etc.).
		double alto = mundo.getBlockState(debajo).getCollisionShape(mundo, debajo).max(net.minecraft.core.Direction.Axis.Y);
		return debajo.getY() + (Double.isFinite(alto) && alto > 0 ? alto : 1);
	}

	private void aviso(String texto) {
		for (Entity p : getPassengers()) {
			if (p instanceof ServerPlayer jugador) jugador.displayClientMessage(Component.literal(texto).withColor(VIOLETA), true);
		}
	}

	/** Fuego y humo saliendo del motor (abajo). */
	private void fuego(ServerLevel mundo, float fuerza) {
		int mucho = Math.max(1, (int) (10 * fuerza));
		mundo.sendParticles(ParticleTypes.FLAME, getX(), getY() - 0.2, getZ(), mucho, 0.25, 0.3, 0.25, 0.04);
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() - 0.6, getZ(), mucho / 2 + 1, 0.4, 0.4, 0.4, 0.02);
		mundo.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY() - 1, getZ(), 1, 0.3, 0.3, 0.3, 0.01);
	}

	/** Bien arriba: pasa a la otra dimensión con los pasajeros y aparece en el cielo, lista para aterrizar. */
	private void viajar(ServerLevel mundo) {
		ResourceKey<Level> hacia = mundo.dimension().equals(Organos.DIMENSION) ? Level.OVERWORLD : Organos.DIMENSION;
		ServerLevel destino = mundo.getServer().getLevel(hacia);
		NaveViajeEntity nueva = destino == null ? null : ModDespegue.NAVE_VIAJE.create(destino);
		if (nueva == null) {
			estado(ATERRIZANDO);
			sueloLlegada = Double.NaN;
			return;
		}
		List<Entity> pasajeros = new ArrayList<>(getPassengers());
		ejectPassengers();
		BlockPos suelo = Portales.lugarSeguro(destino, blockPosition());
		double llegadaY = Math.min(suelo.getY() + ALTURA_LLEGADA, destino.getMaxBuildHeight() - 10);
		nueva.moveTo(suelo.getX() + 0.5, llegadaY, suelo.getZ() + 0.5, getYRot(), 0);
		nueva.dueno = dueno;
		nueva.nombreDueno = nombreDueno;
		nueva.sueloLlegada = suelo.getY();
		destino.addFreshEntity(nueva);
		nueva.estado(ATERRIZANDO);
		for (Entity pasajero : pasajeros) {
			if (pasajero instanceof ServerPlayer jugador) {
				jugador.teleportTo(destino, nueva.getX(), llegadaY, nueva.getZ(), jugador.getYRot(), jugador.getXRot());
				jugador.startRiding(nueva, true);
			}
		}
		nueva.titulo(hacia.equals(Organos.DIMENSION) ? "Dimensión de los Órganos" : "Overworld", VIOLETA, "Aterrizando...");
		discard();
	}

	private void titulo(String texto, int color, String subtitulo) {
		for (Entity p : getPassengers()) {
			if (!(p instanceof ServerPlayer jugador)) continue;
			jugador.connection.send(new ClientboundSetTitlesAnimationPacket(0, 22, 6));
			jugador.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitulo).withColor(0xC6CFD6)));
			jugador.connection.send(new ClientboundSetTitleTextPacket(Component.literal(texto).withColor(color)));
		}
	}

	private void sonido(ServerLevel mundo, SoundEvent sonido, float volumen, float tono) {
		mundo.playSound(null, getX(), getY(), getZ(), sonido, SoundSource.NEUTRAL, volumen, tono);
	}

	// --- Lo demás: no se rompe, no se empuja, se guarda con el mundo ---

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag datos) {
		if (datos.hasUUID("Dueno")) dueno = datos.getUUID("Dueno");
		nombreDueno = datos.getString("NombreDueno");
		int guardado = datos.getInt("Estado");
		// Si se guardó en el aire, al volver baja hasta el piso; si estaba contando, vuelve a esperar.
		if (guardado == DESPEGANDO || guardado == ATERRIZANDO) {
			sueloLlegada = datos.contains("Suelo") ? datos.getDouble("Suelo") : Double.NaN;
			if (guardado == DESPEGANDO) sueloLlegada = Double.NaN;
			estado(ATERRIZANDO);
		} else {
			estado(QUIETA);
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag datos) {
		if (dueno != null) datos.putUUID("Dueno", dueno);
		datos.putString("NombreDueno", nombreDueno);
		datos.putInt("Estado", estado());
		if (!Double.isNaN(sueloLlegada)) datos.putDouble("Suelo", sueloLlegada);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}
}
