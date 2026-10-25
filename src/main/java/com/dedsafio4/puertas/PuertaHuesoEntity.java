package com.dedsafio4.puertas;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * La Puerta de Hueso: una reja de cárcel de 7 × 7 bloques de hueso (los postes girados 45°, tocándose por las
 * puntas, entre una fila de bloques abajo y otra arriba); las 3 columnas del medio son la puerta. Se abre y se cierra
 * con click derecho; cerrada no se puede pasar (abierta, se pasa por toda la reja). El Triceratops no la rompe de
 * una: las dos primeras embestidas la abomban y a la tercera se desarma bloque por bloque; a los 6 segundos
 * desaparece dejando huesos. Su grosor y su alto se eligen con /puertita <grueso> <alto> (en bloques; lo normal es
 * 1 y 7); el ancho es siempre de 7 bloques. Es una entidad (no bloques) para poder tener cualquier tamaño.
 */
public class PuertaHuesoEntity extends Entity {
	private static final EntityDataAccessor<Float> GRUESO = SynchedEntityData.defineId(PuertaHuesoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ALTO = SynchedEntityData.defineId(PuertaHuesoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> ABIERTA = SynchedEntityData.defineId(PuertaHuesoEntity.class, EntityDataSerializers.BOOLEAN);
	/** Cuántas embestidas recibió (a las 3 se cae) y de qué lado vino la última (1: desde atrás, -1: desde adelante). */
	private static final EntityDataAccessor<Integer> GOLPES = SynchedEntityData.defineId(PuertaHuesoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LADO = SynchedEntityData.defineId(PuertaHuesoEntity.class, EntityDataSerializers.INT);

	public static final float GRUESO_NORMAL = 1f, ALTO_NORMAL = 7f, MEDIO_ANCHO = 3.5f;
	public static final int EMBESTIDAS = 3;
	/** Cuánto aguanta los golpes de los jugadores (como un bloque duro), y cuánto tarda en desaparecer caída. */
	private static final float VIDA = 30;
	private static final int TIEMPO_CAIDA = 120;

	private float vida = VIDA;
	private int caidaTicks;

	/** En el cliente: la animación (ver PuertaHuesoAnimacion). */
	public final PuertaHuesoAnimacion animacion = new PuertaHuesoAnimacion();

	public PuertaHuesoEntity(EntityType<? extends PuertaHuesoEntity> tipo, Level level) {
		super(tipo, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(GRUESO, GRUESO_NORMAL);
		datos.define(ALTO, ALTO_NORMAL);
		datos.define(ABIERTA, false);
		datos.define(GOLPES, 0);
		datos.define(LADO, 1);
	}

	public float grueso() {
		return entityData.get(GRUESO);
	}

	public float alto() {
		return entityData.get(ALTO);
	}

	public boolean abierta() {
		return entityData.get(ABIERTA);
	}

	public int golpes() {
		return entityData.get(GOLPES);
	}

	public int lado() {
		return entityData.get(LADO);
	}

	public boolean caida() {
		return golpes() >= EMBESTIDAS;
	}

	public void tamaño(float grueso, float alto) {
		entityData.set(GRUESO, grueso);
		entityData.set(ALTO, alto);
		setBoundingBox(makeBoundingBox());
	}

	/** Hacia dónde mira el frente de la reja. */
	public Direction frente() {
		return Direction.fromYRot(getYRot());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
		super.onSyncedDataUpdated(dato);
		if (dato == GRUESO || dato == ALTO) setBoundingBox(makeBoundingBox());
		if (dato == GOLPES && level().isClientSide) animacion.golpe(golpes(), lado());
	}

	/** 7 bloques de ancho, el grosor y el alto elegidos, según hacia dónde mira. */
	@Override
	protected AABB makeBoundingBox() {
		double medioGrueso = grueso() / 2, alto = alto();
		boolean alongX = frente().getAxis() == Direction.Axis.X;
		double mx = alongX ? medioGrueso : MEDIO_ANCHO, mz = alongX ? MEDIO_ANCHO : medioGrueso;
		Vec3 p = position();
		return new AABB(p.x - mx, p.y, p.z - mz, p.x + mx, p.y + alto, p.z + mz);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().inflate(4, 1, 4);
	}

	/** Cerrada no se puede pasar; abierta o caída, sí. */
	@Override
	public boolean canBeCollidedWith() {
		return !abierta() && !caida();
	}

	@Override
	public boolean isPickable() {
		return !isRemoved() && !caida();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			animacion.tick(abierta());
			return;
		}
		if (caida() && ++caidaTicks >= TIEMPO_CAIDA) {
			// Se deshace: quedan unos huesos y algo de chatarra.
			spawnAtLocation(new ItemStack(Items.BONE, 6));
			spawnAtLocation(new ItemStack(Items.BONE_MEAL, 8));
			((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 40, 3, 0.2, 3, 0.02);
			discard();
		}
	}

	/** Click derecho: abre o cierra. */
	@Override
	public InteractionResult interact(Player jugador, InteractionHand mano) {
		if (caida()) return InteractionResult.PASS;
		if (!level().isClientSide) {
			boolean abrir = !abierta();
			entityData.set(ABIERTA, abrir);
			level().playSound(null, getX(), getY() + 1, getZ(), abrir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
					SoundSource.BLOCKS, 1f, 0.8f);
		}
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	/** Los jugadores la pueden romper a golpes (en creativo, de un golpe); al romperla la dejan caer como ítem. */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (level().isClientSide || isRemoved() || caida()) return false;
		if (!(fuente.getEntity() instanceof Player jugador)) return false;
		level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.BONE_BLOCK_HIT, SoundSource.BLOCKS, 1f, 0.8f);
		vida -= jugador.isCreative() ? VIDA : Math.max(cantidad, 1);
		if (vida > 0) return true;
		if (!jugador.isCreative()) spawnAtLocation(PuertaHuesoItem.con(grueso(), alto()));
		level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
		discard();
		return true;
	}

	/**
	 * La embistió un Triceratops que venía en esa dirección: tiembla y se abomba; a la tercera revienta y se cae.
	 * Devuelve true si la puerta lo frenó.
	 */
	public boolean embestida(Vec3 direccion) {
		if (caida() || abierta()) return false;
		Vec3 f = Vec3.atLowerCornerOf(frente().getNormal());
		entityData.set(LADO, direccion.dot(f) >= 0 ? 1 : -1);
		int golpes = golpes() + 1;
		entityData.set(GOLPES, golpes);
		ServerLevel mundo = (ServerLevel) level();
		var hueso = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());
		double alto = alto();
		if (golpes < EMBESTIDAS) {
			mundo.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.BLOCKS, 2f, 0.6f);
			mundo.sendParticles(hueso, getX(), getY() + alto / 2, getZ(), 20, 2, alto / 3, 0.3, 0.1);
			return true;
		}
		mundo.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2f, 0.7f);
		mundo.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.BLOCKS, 2f, 0.7f);
		// Los pedazos salen volando hacia donde empujó el Triceratops.
		for (int i = 0; i < 46; i++) {
			double x = getX() + (random.nextDouble() - 0.5) * 7, y = getY() + random.nextDouble() * alto, z = getZ() + (random.nextDouble() - 0.5) * 7;
			mundo.sendParticles(hueso, x, y, z, 0, direccion.x, 0.4 + random.nextDouble() * 0.6, direccion.z, 0.6);
		}
		mundo.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 40, 3, 1, 1, 0.08);
		return true;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag datos) {
		float g = datos.contains("Grueso") ? datos.getFloat("Grueso") : GRUESO_NORMAL;
		float a = datos.contains("Alto") ? datos.getFloat("Alto") : ALTO_NORMAL;
		tamaño(Mth.clamp(g, 0.05f, 8f), Mth.clamp(a, 0.5f, 32f));
		entityData.set(ABIERTA, datos.getBoolean("Abierta"));
		entityData.set(GOLPES, datos.getInt("Golpes"));
		if (datos.contains("Vida")) vida = datos.getFloat("Vida");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag datos) {
		datos.putFloat("Grueso", grueso());
		datos.putFloat("Alto", alto());
		datos.putBoolean("Abierta", abierta());
		datos.putInt("Golpes", golpes());
		datos.putFloat("Vida", vida);
	}
}
