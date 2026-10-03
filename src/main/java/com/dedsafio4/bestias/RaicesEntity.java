package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Las raíces del Creeper Raíz de Azalea: crecen alrededor del jugador y lo dejan quieto (sin caminar ni
 * saltar), haciéndole medio corazón cada 2 segundos. Cada click (o salto) suma 12% a la barra para
 * librarse, que baja un 30% por segundo; al llegar a 100% las raíces se rompen. Si no se libera en
 * 7 segundos, se secan solas.
 */
public class RaicesEntity extends Entity {
	public static final int TIEMPO_CRECER = 18, TIEMPO_ROMPER = 12, DURACION = 140;
	private static final float POR_CLICK = 0.12f, BAJA_POR_TICK = 0.3f / 20;
	private static final ResourceLocation MODIFICADOR = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "raices");

	private static final EntityDataAccessor<Integer> OBJETIVO = SynchedEntityData.defineId(RaicesEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> PROGRESO = SynchedEntityData.defineId(RaicesEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> ROTA = SynchedEntityData.defineId(RaicesEntity.class, EntityDataSerializers.BOOLEAN);

	private int rotaDesde = -1;

	public RaicesEntity(EntityType<? extends RaicesEntity> tipo, Level level) {
		super(tipo, level);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(OBJETIVO, -1);
		datos.define(PROGRESO, 0f);
		datos.define(ROTA, false);
	}

	public int objetivo() {
		return entityData.get(OBJETIVO);
	}

	public float progreso() {
		return entityData.get(PROGRESO);
	}

	public boolean rota() {
		return entityData.get(ROTA);
	}

	/** En el cliente: ticks desde que se rompieron (-1 si siguen enteras). */
	public int ticksRota() {
		return rotaDesde < 0 ? -1 : tickCount - rotaDesde;
	}

	/** ¿Este jugador ya está atrapado por unas raíces? */
	public static boolean atrapado(Player jugador) {
		return !jugador.level().getEntitiesOfClass(RaicesEntity.class, jugador.getBoundingBox().inflate(2),
				r -> r.objetivo() == jugador.getId() && !r.rota()).isEmpty();
	}

	public void atrapar(ServerPlayer jugador) {
		moveTo(jugador.getX(), jugador.getY(), jugador.getZ(), 0, 0);
		entityData.set(OBJETIVO, jugador.getId());
		jugador.setDeltaMovement(Vec3.ZERO);
		jugador.hurtMarked = true;
		frenar(jugador, true);
		level().playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.HOSTILE, 1.5f, 0.6f);
		level().playSound(null, blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.5f, 0.8f);
	}

	/** Le saca (o le devuelve) las ganas de caminar y saltar. */
	private static void frenar(Player jugador, boolean quieto) {
		for (Holder<Attribute> atributo : java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
			AttributeInstance instancia = jugador.getAttribute(atributo);
			if (instancia == null) continue;
			instancia.removeModifier(MODIFICADOR);
			if (quieto) instancia.addTransientModifier(new AttributeModifier(MODIFICADOR, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private Player jugador() {
		return level().getEntity(objetivo()) instanceof Player j ? j : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (rota() && rotaDesde < 0) rotaDesde = tickCount;
		if (level().isClientSide) return;
		Player jugador = jugador();
		if (rota()) {
			if (tickCount - rotaDesde >= TIEMPO_ROMPER) discard();
			return;
		}
		if (jugador == null || !jugador.isAlive() || jugador.isSpectator()) {
			romper(null);
			return;
		}
		// Si algo lo empujó, vuelve al centro de las raíces.
		if (jugador.distanceToSqr(position()) > 0.36 && jugador instanceof ServerPlayer sp) {
			sp.teleportTo(getX(), getY(), getZ());
		}
		entityData.set(PROGRESO, Math.max(0, progreso() - BAJA_POR_TICK));
		if (tickCount % 40 == 0) jugador.hurt(damageSources().cactus(), 1);
		if (tickCount >= DURACION) romper(jugador);
	}

	/** Un click del jugador atrapado. */
	private void forcejear(ServerPlayer jugador) {
		if (rota() || jugador.getId() != objetivo()) return;
		entityData.set(PROGRESO, Math.min(1, progreso() + POR_CLICK));
		if (tickCount % 3 == 0) level().playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_HIT, SoundSource.PLAYERS, 0.8f, 1.2f);
		if (progreso() >= 1) romper(jugador);
	}

	private void romper(Player jugador) {
		entityData.set(ROTA, true);
		rotaDesde = tickCount;
		if (jugador != null) frenar(jugador, false);
		level().playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.5f, 1f);
	}

	@Override
	public void remove(RemovalReason razon) {
		Player jugador = jugador();
		if (jugador != null && !level().isClientSide) frenar(jugador, false);
		super.remove(razon);
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag datos) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag datos) {
	}

	// ---------------------------------------------------------------- Red

	/** Cliente → servidor: el jugador atrapado hizo click (o saltó) para librarse. */
	public record ForcejeoPayload() implements CustomPacketPayload {
		public static final Type<ForcejeoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "forcejeo"));
		public static final StreamCodec<ByteBuf, ForcejeoPayload> CODEC = StreamCodec.unit(new ForcejeoPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playC2S().register(ForcejeoPayload.TYPE, ForcejeoPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ForcejeoPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			ServerPlayer jugador = context.player();
			if (!(jugador.level() instanceof ServerLevel mundo)) return;
			for (RaicesEntity raices : mundo.getEntitiesOfClass(RaicesEntity.class, jugador.getBoundingBox().inflate(2))) {
				raices.forcejear(jugador);
			}
		}));
	}
}
