package com.dedsafio4.portal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El Portal del Espacio: un remolino redondo, parado, de 3 bloques de alto. El que se mete adentro aparece en el lugar
 * marcado con /spawn_del_espacio2 (ver DestinoPortal). Se pone con el ítem Portal del Espacio (mirando hacia quien lo
 * pone) y se saca pegándole en creativo.
 */
public class PortalEspacioEntity extends Entity {
	/** El radio del remolino y la altura de su centro desde el piso (bloques). */
	public static final float RADIO = 1.4f, CENTRO = 1.5f;
	/** Para no viajar dos veces seguidas (3 s), y cada cuánto se avisa si no hay destino. */
	private static final int ESPERA = 60;
	private static final Map<UUID, Long> ULTIMO_VIAJE = new HashMap<>();

	public PortalEspacioEntity(EntityType<? extends PortalEspacioEntity> tipo, Level level) {
		super(tipo, level);
		this.noPhysics = true;
	}

	public PortalEspacioEntity(Level level, Vec3 pos, float giro) {
		this(ModPortal.PORTAL_ESPACIO, level);
		this.setPos(pos);
		this.setYRot(giro);
		this.yRotO = giro;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	/** El medio del remolino. */
	public Vec3 centro() {
		return position().add(0, CENTRO, 0);
	}

	/** Hacia dónde mira (perpendicular al remolino). */
	public Vec3 frente() {
		return Vec3.directionFromRotation(0, getYRot());
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 c = centro(), n = frente();
		if (level().isClientSide) {
			// Unas chispitas que se van al centro.
			if (random.nextInt(3) == 0) {
				double a = random.nextDouble() * Math.PI * 2;
				Vec3 lado = new Vec3(n.z, 0, -n.x);
				Vec3 p = c.add(lado.scale(Math.cos(a) * RADIO)).add(0, Math.sin(a) * RADIO, 0);
				Vec3 hacia = c.subtract(p);
				level().addParticle(ParticleTypes.PORTAL, p.x, p.y, p.z, hacia.x, hacia.y, hacia.z);
			}
			return;
		}
		if (tickCount % 80 == 0) {
			level().playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.2f + random.nextFloat() * 0.3f);
		}
		ServerLevel mundo = (ServerLevel) level();
		for (ServerPlayer p : mundo.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(RADIO + 1))) {
			if (!p.isAlive() || p.isSpectator() || p.isPassenger()) continue;
			// ¿Está metido en el remolino? Cerca del plano y dentro del círculo.
			Vec3 d = p.position().add(0, p.getBbHeight() / 2, 0).subtract(c);
			double frente = d.dot(n);
			double alLado = d.subtract(n.scale(frente)).length();
			if (Math.abs(frente) > 0.6 || alLado > RADIO) continue;
			long ahora = mundo.getGameTime();
			Long antes = ULTIMO_VIAJE.get(p.getUUID());
			if (antes != null && ahora - antes < ESPERA) continue;
			ULTIMO_VIAJE.put(p.getUUID(), ahora);
			viajar(p);
		}
	}

	private void viajar(ServerPlayer p) {
		DestinoPortal.Destino destino = DestinoPortal.destino(p.server);
		ServerLevel hacia = destino == null ? null : p.server.getLevel(destino.mundo());
		if (hacia == null) {
			p.displayClientMessage(Component.literal("Este portal todavía no tiene destino (/spawn_del_espacio2 <x y z>).")
					.withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return;
		}
		Vec3 c = centro();
		level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
		((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
		p.teleportTo(hacia, destino.x(), destino.y(), destino.z(), p.getYRot(), p.getXRot());
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		hacia.playSound(null, destino.x(), destino.y(), destino.z(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25f, 1.4f);
		hacia.sendParticles(ParticleTypes.REVERSE_PORTAL, destino.x(), destino.y() + 1, destino.z(), 60, 0.5, 1, 0.5, 0.1);
		// Que no vuelva a entrar enseguida si el destino está al lado de otro portal.
		ULTIMO_VIAJE.put(p.getUUID(), hacia.getGameTime());
	}

	/** Solo se saca pegándole en creativo. */
	@Override
	public boolean hurt(DamageSource fuente, float danio) {
		if (level().isClientSide || isRemoved()) return false;
		if (fuente.getEntity() instanceof Player j && j.isCreative()) {
			Vec3 c = centro();
			level().playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
			((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 80, 0.8, 0.8, 0.8, 0.1);
			discard();
			return true;
		}
		return false;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** Las flechas, los rayos, etc. lo atraviesan (solo se le puede pegar con la mano, para sacarlo). */
	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return distancia < 128 * 128;
	}
}
