package com.dedsafio4.bestias;

import com.dedsafio4.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reptisaurio Salvaje (id "mira"): criatura pequeña con caparazón de columnas que ataca directamente a
 * quien se le acerque (a 6 bloques). Si te acercas demasiado (2,5 bloques) y está sobre tierra, pasto,
 * arena o grava, se entierra (no se lo puede lastimar), te acecha por debajo del suelo hasta llegar a
 * ti y sale de nuevo para atacar. Siempre suelta Sangre de Reptisaurio.
 */
public class ReptisaurioEntity extends Monster {
	public static final int NORMAL = 0, ENTERRANDOSE = 1, ACECHANDO = 2, SALIENDO = 3;
	/** Lo que dura cada parte (en ticks): enterrarse 1,2 s, salir 0,5 s y el mordisco 0,9 s. */
	public static final int TIEMPO_ENTERRARSE = 24, TIEMPO_SALIR = 10, TIEMPO_ATAQUE = 18;
	private static final byte EVENTO_ATAQUE = 62;

	private static final EntityDataAccessor<Integer> ESTADO =
			SynchedEntityData.defineId(ReptisaurioEntity.class, EntityDataSerializers.INT);

	/** Ticks desde que cambió de estado (en el servidor y en el cliente, cada uno el suyo). */
	public int ticksEstado;
	private int estadoAntes;
	/** En el cliente: ticks del mordisco (-1 = no está mordiendo). */
	public int ataque = -1;
	private int espera;

	public ReptisaurioEntity(EntityType<? extends ReptisaurioEntity> tipo, Level level) {
		super(tipo, level);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 16.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.FOLLOW_RANGE, 8.0);
	}

	/** Aparece en las cuevas: donde no llega el cielo, a oscuras y sobre piso firme. */
	public static boolean puedeAparecerEnCueva(EntityType<ReptisaurioEntity> tipo, LevelAccessor mundo, MobSpawnType razon,
											   BlockPos pos, RandomSource azar) {
		if (razon == MobSpawnType.SPAWNER) return true;
		return mundo.getBrightness(LightLayer.SKY, pos) == 0 && mundo.getBrightness(LightLayer.BLOCK, pos) < 8
				&& mundo.getBlockState(pos.below()).isSolidRender(mundo, pos.below());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(ESTADO, NORMAL);
	}

	public int estado() {
		return entityData.get(ESTADO);
	}

	private void estado(int nuevo) {
		entityData.set(ESTADO, nuevo);
		ticksEstado = 0;
		estadoAntes = nuevo;
	}

	public boolean enterrado() {
		return estado() == ENTERRANDOSE || estado() == ACECHANDO;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
			@Override
			public boolean canUse() {
				return estado() == NORMAL && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return estado() == NORMAL && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
			@Override
			public boolean canUse() {
				return estado() == NORMAL && super.canUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Solo se entierra en bloques blandos. */
	private boolean sueloBlando() {
		BlockState debajo = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
		return debajo.is(BlockTags.DIRT) || debajo.is(BlockTags.SAND) || debajo.is(Blocks.GRAVEL);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			if (estado() != estadoAntes) {
				estadoAntes = estado();
				ticksEstado = 0;
			}
			ticksEstado++;
			if (ataque >= 0 && ++ataque >= TIEMPO_ATAQUE) ataque = -1;
			return;
		}
		ticksEstado++;
		if (espera > 0) espera--;
		LivingEntity objetivo = getTarget();
		switch (estado()) {
			case NORMAL -> {
				if (objetivo != null && espera == 0 && distanceTo(objetivo) < 2.5f && onGround() && sueloBlando()) {
					estado(ENTERRANDOSE);
					getNavigation().stop();
					playSound(SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.7f);
				}
			}
			case ENTERRANDOSE -> {
				setDeltaMovement(0, getDeltaMovement().y, 0);
				tierra(6);
				if (ticksEstado >= TIEMPO_ENTERRARSE) estado(ACECHANDO);
			}
			case ACECHANDO -> {
				if (objetivo != null && objetivo.isAlive()) getNavigation().moveTo(objetivo, 1.1);
				if (ticksEstado % 2 == 0) tierra(2);
				if (ticksEstado % 10 == 0) playSound(SoundEvents.GRAVEL_STEP, 0.6f, 0.8f);
				boolean llego = objetivo != null && ticksEstado > 40 && distanceTo(objetivo) < 1.3f;
				if (llego || objetivo == null || !objetivo.isAlive() || ticksEstado > 200) {
					estado(SALIENDO);
					getNavigation().stop();
					playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 1f);
					tierra(20);
				}
			}
			case SALIENDO -> {
				if (ticksEstado >= TIEMPO_SALIR) {
					estado(NORMAL);
					espera = 80;
					if (objetivo != null && distanceTo(objetivo) < 2.5f) doHurtTarget(objetivo);
				}
			}
			default -> estado(NORMAL);
		}
	}

	/** Partículas del bloque de abajo. */
	private void tierra(int cuantas) {
		if (!(level() instanceof ServerLevel mundo)) return;
		BlockState debajo = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
		if (debajo.isAir()) return;
		mundo.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, debajo), getX(), getY() + 0.1, getZ(),
				cuantas, 0.3, 0.05, 0.3, 0.1);
	}

	@Override
	public boolean doHurtTarget(Entity objetivo) {
		level().broadcastEntityEvent(this, EVENTO_ATAQUE);
		return super.doHurtTarget(objetivo);
	}

	@Override
	public void handleEntityEvent(byte evento) {
		if (evento == EVENTO_ATAQUE) ataque = 0;
		else super.handleEntityEvent(evento);
	}

	/** Enterrado no se lo puede lastimar (salvo /kill y el vacío). */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (enterrado() && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurt(fuente, cantidad);
	}

	@Override
	public boolean isPushable() {
		return !enterrado() && super.isPushable();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource fuente, boolean muertePorJugador) {
		super.dropCustomDeathLoot(level, fuente, muertePorJugador);
		spawnAtLocation(new ItemStack(ModItems.SANGRE_REPTISAURIO));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag datos) {
		super.readAdditionalSaveData(datos);
		estado(NORMAL);
	}
}
