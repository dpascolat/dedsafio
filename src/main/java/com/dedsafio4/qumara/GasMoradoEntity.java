package com.dedsafio4.qumara;

import com.dedsafio4.reptisaurios.VenenoPrimitivo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * El Gas Morado (del diseño): una nube de 2×1×1 bloques hecha de cubitos translúcidos que "respira".
 * Qumara lo tira por la boca al que tiene agarrado (soplos que salen chiquitos, vuelan frenándose y se
 * agrandan) y queda una nube quieta donde cae el que tira. Al que lo toca le da el Veneno Primitivo
 * (que solo se saca con el Antídoto Primitivo; la Máscara Anti-Esporas y el Antibiótico protegen).
 */
public class GasMoradoEntity extends Entity {
	public static final int SOPLO = 0, NUBE = 1;

	private static final EntityDataAccessor<Integer> MODO = SynchedEntityData.defineId(GasMoradoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VIDA = SynchedEntityData.defineId(GasMoradoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> TAMANIO = SynchedEntityData.defineId(GasMoradoEntity.class, EntityDataSerializers.FLOAT);

	/** Cada tick la velocidad del soplo baja así (0,4 por segundo, como en el diseño). */
	private static final double FRENO = Math.pow(0.4, 1 / 20.0);

	public GasMoradoEntity(EntityType<? extends GasMoradoEntity> tipo, Level level) {
		super(tipo, level);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(MODO, SOPLO);
		builder.define(VIDA, 40);
		builder.define(TAMANIO, 1f);
	}

	/** Un soplo: sale de la boca con esa velocidad (bloques por tick), vive tantos ticks. */
	public static GasMoradoEntity soplo(Level level, Vec3 desde, Vec3 velocidad, int vida, float tamanio) {
		GasMoradoEntity g = new GasMoradoEntity(ModQumara.GAS_MORADO, level);
		g.entityData.set(MODO, SOPLO);
		g.entityData.set(VIDA, vida);
		g.entityData.set(TAMANIO, tamanio);
		g.moveTo(desde.x, desde.y, desde.z, (float) (Mth.atan2(velocidad.z, velocidad.x) * Mth.RAD_TO_DEG), 0);
		g.setDeltaMovement(velocidad);
		return g;
	}

	/** Una nube quieta (tamaño 1,6 en el diseño), flotando un bloque arriba del lugar (los soplos de la boca no). */
	public static GasMoradoEntity nube(Level level, Vec3 donde, int vida, float tamanio) {
		GasMoradoEntity g = new GasMoradoEntity(ModQumara.GAS_MORADO, level);
		g.entityData.set(MODO, NUBE);
		g.entityData.set(VIDA, vida);
		g.entityData.set(TAMANIO, tamanio);
		g.moveTo(donde.x, donde.y + 1, donde.z, level.random.nextFloat() * 360, 0);
		return g;
	}

	public int modo() { return entityData.get(MODO); }
	public int vida() { return entityData.get(VIDA); }

	/** Qué tan grande se ve ahora (multiplica la nube de 2×1×1). */
	public float escala(float parcial) {
		float t = (tickCount + parcial) / 20f, vida = vida() / 20f, base = entityData.get(TAMANIO);
		if (modo() == NUBE) {
			return base * QumaraEntity.suave(t / 0.5f) * (1 - QumaraEntity.suave((t - (vida - 0.8f)) / 0.8f));
		}
		// El soplo sale chico, crece, y al final se achica.
		return base * (0.25f + 0.8f * t) * Math.min(1, (1 - t / vida) * 3);
	}

	@Override
	public void tick() {
		super.tick();
		// Se mueve igual en el servidor y en el cliente (sin chocar con nada).
		Vec3 v = getDeltaMovement();
		if (v.lengthSqr() > 1e-6) {
			setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
			setDeltaMovement(v.scale(FRENO));
		}
		if (!(level() instanceof ServerLevel mundo)) return;
		if (tickCount >= vida()) {
			discard();
			return;
		}
		if (tickCount % 2 == 0) envenenar(mundo);
	}

	/** A los jugadores que están dentro de la nube les da el Veneno Primitivo. */
	private void envenenar(ServerLevel mundo) {
		float s = escala(0);
		if (s < 0.2f) return;
		AABB zona = new AABB(getX() - s, getY(), getZ() - s, getX() + s, getY() + s, getZ() + s);
		for (Player p : mundo.getEntitiesOfClass(Player.class, zona, p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
			// El que maneja a Qumara no.
			if (p.getVehicle() instanceof QumaraEntity q && q.getControllingPassenger() == p) continue;
			if (!VenenoPrimitivo.envenenado(p)) VenenoPrimitivo.aplicar(p);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 96 * 96;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}
}
