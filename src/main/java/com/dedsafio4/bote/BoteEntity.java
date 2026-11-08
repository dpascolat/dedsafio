package com.dedsafio4.bote;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * El Bote: un bote largo (unos 12 bloques) que anda en el agua como el de Minecraft, pero con lugar para 11. El
 * primero que se sube maneja (con las teclas de caminar, igual que un bote común) y se sienta atrás; los demás se
 * van sentando en los bancos, de atrás para adelante. Todos los botes andan a la velocidad de /bote_velocidad.
 * Es irrompible (sólo lo rompe alguien en creativo).
 */
public class BoteEntity extends Boat implements GeoEntity {
	public static final int LUGARES = 11;
	/**
	 * Los 11 bancos del modelo: a lo largo del bote, cada 13,5 píxeles, empezando por el de atrás (el del que maneja).
	 * ALTO_ASIENTO es la altura del banco sobre el piso del bote.
	 */
	private static final double PRIMER_BANCO = -66 / 16.0, ENTRE_BANCOS = 13.5 / 16.0, ALTO_ASIENTO = 7 / 16.0;

	private static final EntityDataAccessor<Float> VELOCIDAD = SynchedEntityData.defineId(BoteEntity.class, EntityDataSerializers.FLOAT);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public BoteEntity(EntityType<? extends Boat> tipo, Level level) {
		super(tipo, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(VELOCIDAD, 1f);
	}

	@Override
	public void tick() {
		// El servidor le pasa a cada bote la velocidad de /bote_velocidad (así el que maneja, en su juego, la sabe).
		if (level() instanceof ServerLevel servidor) entityData.set(VELOCIDAD, (float) ModBote.velocidad(servidor.getServer()));
		super.tick();
	}

	/** Lo que el bote se mueve solo (manejado o a la deriva) se multiplica por la velocidad de /bote_velocidad. */
	@Override
	public void move(MoverType tipo, Vec3 movimiento) {
		if (tipo == MoverType.SELF) {
			double v = entityData.get(VELOCIDAD);
			movimiento = new Vec3(movimiento.x * v, movimiento.y, movimiento.z * v);
		}
		super.move(tipo, movimiento);
	}

	/**
	 * Irrompible: no se rompe con golpes, flechas, explosiones ni nada. Sólo lo puede romper alguien en creativo
	 * (o con /kill).
	 */
	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource fuente, float cantidad) {
		if (!fuente.isCreativePlayer() && !fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurt(fuente, cantidad);
	}

	@Override
	protected int getMaxPassengers() {
		return LUGARES;
	}

	/** Cada uno en su banco: el primero (el que maneja) atrás de todo, y los demás hacia adelante. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity pasajero, EntityDimensions tamano, float escala) {
		int i = Math.max(0, getPassengers().indexOf(pasajero));
		double z = PRIMER_BANCO + ENTRE_BANCOS * i;
		return new Vec3(0, ALTO_ASIENTO, z).yRot(-getYRot() * ((float) Math.PI / 180f));
	}

	@Override
	public Item getDropItem() {
		return ModBote.BOTE_ITEM;
	}

	/** El bote mide 12 bloques: que se siga dibujando aunque su caja (chica, en el medio) no se vea en pantalla. */
	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().inflate(7, 2.5, 7);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 160 * 160;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
