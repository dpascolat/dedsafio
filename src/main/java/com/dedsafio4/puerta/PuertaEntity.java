package com.dedsafio4.puerta;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * La reja de la Puerta Rastrillo mientras se mueve (3 segundos): no tiene bloques, sólo se dibuja subiendo o bajando
 * suave (ver PuertaRenderer), temblando un poquito. Al llegar le avisa a PuertaRastrillo para que ponga los bloques
 * y desaparece. Su posición es la esquina de abajo a la izquierda de la puerta cerrada.
 */
public class PuertaEntity extends Entity {
	private static final EntityDataAccessor<Integer> DERECHA = SynchedEntityData.defineId(PuertaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SUBE = SynchedEntityData.defineId(PuertaEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> INICIO = SynchedEntityData.defineId(PuertaEntity.class, EntityDataSerializers.LONG);

	private String nombre = "";

	public PuertaEntity(EntityType<?> tipo, Level level) {
		super(tipo, level);
		noPhysics = true;
		setNoGravity(true);
	}

	void preparar(String nombre, BlockPos origen, Direction derecha, boolean sube, long ahora) {
		this.nombre = nombre;
		moveTo(origen.getX(), origen.getY(), origen.getZ(), 0, 0);
		entityData.set(DERECHA, derecha.get3DDataValue());
		entityData.set(SUBE, sube);
		entityData.set(INICIO, ahora);
	}

	public String nombre() {
		return nombre;
	}

	public Direction derecha() {
		return Direction.from3DDataValue(entityData.get(DERECHA));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(DERECHA, Direction.EAST.get3DDataValue());
		datos.define(SUBE, true);
		datos.define(INICIO, 0L);
	}

	/** Cuánto subió la reja (en bloques, de 0 a 32), con arranque y frenada suaves. */
	public float altura(float parcial) {
		float t = Mth.clamp((level().getGameTime() - entityData.get(INICIO) + parcial) / PuertaRastrillo.DURA, 0, 1);
		float e = t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
		return PuertaRastrillo.SUBE * (entityData.get(SUBE) ? e : 1 - e);
	}

	/** El temblor mientras se mueve (en bloques, hacia los costados). */
	public float temblor(float parcial) {
		return Mth.sin((tickCount + parcial) * 2.7f) * 0.03f;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel mundo)) return;
		long pasaron = mundo.getGameTime() - entityData.get(INICIO);
		if (pasaron % 6 == 0 && pasaron < PuertaRastrillo.DURA) {
			mundo.playSound(null, getX() + derecha().getStepX() * 9.5, getY() + altura(0) + 2, getZ() + derecha().getStepZ() * 9.5,
					SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 2.5f, 0.6f);
		}
		if (pasaron >= PuertaRastrillo.DURA) {
			PuertaRastrillo.llego(mundo, nombre, entityData.get(SUBE));
			discard();
		}
	}

	/** Se dibuja aunque su "caja" (chica, en la esquina) no esté en pantalla. */
	@Override
	public AABB getBoundingBoxForCulling() {
		Direction d = derecha();
		return new AABB(getX(), getY(), getZ(), getX() + d.getStepX() * PuertaRastrillo.ANCHO + 1, getY() + PuertaRastrillo.ALTO + PuertaRastrillo.SUBE,
				getZ() + d.getStepZ() * PuertaRastrillo.ANCHO + 1).inflate(1);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 256 * 256;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		nombre = tag.getString("puerta");
		entityData.set(DERECHA, tag.getInt("derecha"));
		entityData.set(SUBE, tag.getBoolean("sube"));
		entityData.set(INICIO, tag.getLong("inicio"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putString("puerta", nombre);
		tag.putInt("derecha", entityData.get(DERECHA));
		tag.putBoolean("sube", entityData.get(SUBE));
		tag.putLong("inicio", entityData.get(INICIO));
	}
}
