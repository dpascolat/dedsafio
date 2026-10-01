package com.dedsafio4.meteoritos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Meteorito: cae en diagonal con una estela de fuego y humo, choca, abre un cráter,
 * tira escombros y deja humo saliendo del cráter unos segundos. La explosión es de 40 bloques de
 * ancho y 10 de alto.
 *
 * El golpe está calculado para que a alguien con armadura completa de diamante con Protección IV le
 * saque 1 corazón (sin armadura saca casi 8).
 */
public class MeteoritoEntity extends Entity {
	/** Lo que tarda en caer (3 segundos). */
	public static final int TICKS_CAIDA = 60;
	/** Lo que sale humo del cráter después del choque (4,2 segundos). */
	private static final int TICKS_HUMO = 84;
	/** Daño en el centro: con diamante completo y Protección IV queda en 2 (un corazón). */
	public static final float DANIO = 15.6f;
	/** Hasta dónde llega el daño entero, y hasta dónde llega algo (baja de a poco). */
	private static final double RADIO_LLENO = 3, RADIO_DANIO = 20;
	/** La explosión: 40 bloques de ancho (radio 20) y 10 de alto (5 para abajo y 5 para arriba). */
	private static final double RADIO_ANCHO = 20, RADIO_ALTO = 5;

	private static final EntityDataAccessor<Vector3f> INICIO = SynchedEntityData.defineId(MeteoritoEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Vector3f> FIN = SynchedEntityData.defineId(MeteoritoEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Long> T0 = SynchedEntityData.defineId(MeteoritoEntity.class, EntityDataSerializers.LONG);
	/** false: cayendo; true: ya chocó (no se dibuja, solo sale humo). */
	private static final EntityDataAccessor<Boolean> CHOCO = SynchedEntityData.defineId(MeteoritoEntity.class, EntityDataSerializers.BOOLEAN);

	public MeteoritoEntity(EntityType<? extends MeteoritoEntity> tipo, Level level) {
		super(tipo, level);
		this.noPhysics = true;
	}

	/** Un meteorito que cae desde inicio hasta fin. */
	public static MeteoritoEntity crear(ServerLevel level, Vec3 inicio, Vec3 fin) {
		MeteoritoEntity m = new MeteoritoEntity(ModMeteoritos.METEORITO, level);
		m.entityData.set(INICIO, inicio.toVector3f());
		m.entityData.set(FIN, fin.toVector3f());
		m.entityData.set(T0, level.getGameTime());
		m.setPos(inicio);
		return m;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(INICIO, new Vector3f());
		builder.define(FIN, new Vector3f());
		builder.define(T0, 0L);
		builder.define(CHOCO, false);
	}

	public boolean choco() {
		return entityData.get(CHOCO);
	}

	/** Dónde va en ese momento de la caída (s de 0 a 1), acelerando como en el diseño. */
	private Vec3 posicionEn(float s) {
		Vec3 a = new Vec3(entityData.get(INICIO)), b = new Vec3(entityData.get(FIN));
		return a.lerp(b, Math.pow(Mth.clamp(s, 0, 1), 1.5));
	}

	private long edad() {
		return level().getGameTime() - entityData.get(T0);
	}

	@Override
	public void tick() {
		super.tick();
		if (choco()) {
			if (level() instanceof ServerLevel mundo) humo(mundo);
			return;
		}
		float s = edad() / (float) TICKS_CAIDA;
		Vec3 pos = posicionEn(s);
		setPos(pos);
		if (!(level() instanceof ServerLevel mundo)) return;

		estela(mundo, pos);
		// Choca con lo que se cruce: el piso, una pared o alguien.
		BlockState adentro = mundo.getBlockState(BlockPos.containing(pos));
		boolean pego = s > 0.05f && !adentro.isAir() && !adentro.getCollisionShape(mundo, BlockPos.containing(pos)).isEmpty();
		boolean aplasto = !mundo.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3), LivingEntity::isAlive).isEmpty();
		if (s >= 1 || pego || aplasto) chocar(mundo, s >= 1 ? posicionEn(1) : pos);
	}

	private void estela(ServerLevel mundo, Vec3 p) {
		mundo.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 8, 0.35, 0.35, 0.35, 0.02);
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.01);
		if (tickCount % 2 == 0) mundo.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0);
		if (tickCount % 10 == 0) mundo.playSound(null, p.x, p.y, p.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5f, 0.5f);
	}

	private void chocar(ServerLevel mundo, Vec3 p) {
		setPos(p);
		entityData.set(CHOCO, true);
		entityData.set(T0, mundo.getGameTime());

		mundo.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 12f, 0.5f);
		mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 0.3, p.z, 1, 0, 0, 0, 0);
		mundo.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.3, p.z, 400, 6, 2, 6, 0.4);
		mundo.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.3, p.z, 120, 8, 2, 8, 0);
		for (int i = 0; i < 12; i++) {
			mundo.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x + (random.nextFloat() - 0.5) * 24, p.y + random.nextFloat() * 4,
					p.z + (random.nextFloat() - 0.5) * 24, 1, 0, 0, 0, 0);
		}
		// Escombros: pasto, tierra y roca volando.
		for (BlockState escombro : new BlockState[]{Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
				Blocks.BLACKSTONE.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState()}) {
			mundo.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, escombro), p.x, p.y + 0.5, p.z, 300, 10, 3, 10, 0.5);
		}

		golpear(mundo, p);
		crater(mundo, BlockPos.containing(p.x, p.y - 0.01, p.z));
	}

	/** Daño lleno cerca del centro, que baja hasta desaparecer a 4 bloques; y empuja hacia afuera. */
	private void golpear(ServerLevel mundo, Vec3 p) {
		DamageSource fuente = new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(ModMeteoritos.DANIO_METEORITO), this);
		for (LivingEntity quien : mundo.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIO_DANIO), LivingEntity::isAlive)) {
			double d = quien.position().add(0, quien.getBbHeight() / 2, 0).distanceTo(p);
			if (d > RADIO_DANIO) continue;
			float parte = d <= RADIO_LLENO ? 1f : (float) (1 - (d - RADIO_LLENO) / (RADIO_DANIO - RADIO_LLENO));
			quien.hurt(fuente, DANIO * parte);
			Vec3 afuera = quien.position().subtract(p).multiply(1, 0, 1);
			if (afuera.lengthSqr() > 1e-4) quien.knockback(0.6 * parte, -afuera.x, -afuera.z);
		}
	}

	/**
	 * El cráter: se rompe todo lo que queda dentro de un elipsoide de 40 x 10 bloques centrado en el
	 * choque (5 para abajo y 5 para arriba). Solo rompe, no pone nada.
	 * No toca cofres ni nada con contenido, ni bloques irrompibles.
	 */
	private void crater(ServerLevel mundo, BlockPos centro) {
		int ancho = (int) Math.ceil(RADIO_ANCHO);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -ancho; dx <= ancho; dx++) {
			for (int dz = -ancho; dz <= ancho; dz++) {
				double h = (dx * dx + dz * dz) / (RADIO_ANCHO * RADIO_ANCHO);
				if (h > 1) continue;
				// Cuánto baja y sube el elipsoide en esta columna.
				int bajo = (int) Math.floor(RADIO_ALTO * Math.sqrt(1 - h));
				for (int dy = -bajo; dy <= bajo; dy++) {
					pos.set(centro.getX() + dx, centro.getY() + dy, centro.getZ() + dz);
					BlockState estado = mundo.getBlockState(pos);
					if (rompible(mundo, pos) || (!estado.isAir() && !estado.getFluidState().isEmpty())) {
						mundo.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
					}
				}
			}
		}
	}

	private static boolean rompible(ServerLevel mundo, BlockPos pos) {
		BlockState e = mundo.getBlockState(pos);
		if (e.isAir() || e.hasBlockEntity()) return false;
		float dureza = e.getDestroySpeed(mundo, pos);
		return dureza >= 0 && dureza < 30;
	}

	private void humo(ServerLevel mundo) {
		long edad = edad();
		if (edad > TICKS_HUMO) {
			discard();
			return;
		}
		Vec3 p = position();
		if (edad % 2 == 0) mundo.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y - 4, p.z, 6, 8, 0.5, 8, 0.01);
		if (edad % 5 == 0) mundo.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y - 3, p.z, 20, 8, 1, 8, 0.03);
	}

	/** No se guarda: si se descarga el mundo en plena caída, desaparece. */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}
}
