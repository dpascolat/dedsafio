package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Tremorsaurio (el Tremorsaurus del mod Alex's Caves, con su modelo, sus texturas y sus sonidos): un dinosaurio enorme
 * que persigue a los jugadores. Al verte ruge, y cada mordida es como una explosión (lastima mucho y empuja a todos los
 * que estén cerca, pero no rompe bloques). Cada 2 segundos puede volver a morder. Hay 3 pieles al azar (la normal,
 * la retro y la tectónica) y, si se llama "princess" o "princesa", la rosa con lentes.
 * /tremorsaurio fuerza <n> cambia la fuerza de la mordida (2 es la normal; es el tamaño de la explosión).
 */
public class TremorsaurioEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<Integer> PIEL = SynchedEntityData.defineId(TremorsaurioEntity.class, EntityDataSerializers.INT);
	public static final int NORMAL = 0, RETRO = 1, TECTONICA = 2;

	/** Cada cuánto puede morder (2 segundos) y cuánto se queda quieto rugiendo. */
	private static final int CADA_MORDIDA = 40, RUGIDO = 40;
	public static final float FUERZA_NORMAL = 2f;

	public static final SoundEvent SONIDO_IDLE = sonido("tremorsaurio.idle"), SONIDO_HURT = sonido("tremorsaurio.hurt"),
			SONIDO_DEATH = sonido("tremorsaurio.death"), SONIDO_ROAR = sonido("tremorsaurio.roar"),
			SONIDO_STOMP = sonido("tremorsaurio.stomp"), SONIDO_BITE = sonido("tremorsaurio.bite");

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);
	private LivingEntity objetivoAntes;
	private int rugiendo;

	public TremorsaurioEntity(EntityType<? extends TremorsaurioEntity> tipo, Level level) {
		super(tipo, level);
		this.xpReward = 30;
	}

	private static SoundEvent sonido(String nombre) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 150.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.ATTACK_DAMAGE, 14.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.STEP_HEIGHT, 1.5)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(PIEL, NORMAL);
	}

	public int piel() {
		return entityData.get(PIEL);
	}

	/** La rosa con lentes: si se llama "princess" o "princesa". */
	public boolean princesa() {
		if (!hasCustomName()) return false;
		String n = getCustomName().getString();
		return n.equalsIgnoreCase("princess") || n.equalsIgnoreCase("princesa");
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor mundo, DifficultyInstance dificultad, MobSpawnType razon, SpawnGroupData datos) {
		int r = random.nextInt(10);
		entityData.set(PIEL, r == 0 ? RETRO : r == 1 ? TECTONICA : NORMAL);
		return super.finalizeSpawn(mundo, dificultad, razon, datos);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
			@Override
			public boolean canUse() {
				return rugiendo <= 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return rugiendo <= 0 && super.canContinueToUse();
			}

			@Override
			protected int getAttackInterval() {
				return adjustedTickDelay(CADA_MORDIDA);
			}
		});
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) return;
		// Cuando encuentra a alguien nuevo, ruge (se queda quieto 2 segundos).
		LivingEntity objetivo = getTarget();
		if (objetivo != null && objetivo != objetivoAntes && objetivo.isAlive()) {
			rugiendo = RUGIDO;
			getNavigation().stop();
			playSound(SONIDO_ROAR, 4f, 1f);
			triggerAnim("accion", "rugido");
		}
		objetivoAntes = objetivo;
		if (rugiendo > 0) {
			rugiendo--;
			getNavigation().stop();
			if (objetivo != null) getLookControl().setLookAt(objetivo, 30f, 30f);
		}
	}

	/** La mordida: una explosión en el que muerde (lastima mucho a él y a los de al lado; no rompe bloques). */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		if (!(level() instanceof ServerLevel mundo)) return false;
		triggerAnim("accion", "mordida");
		playSound(SONIDO_BITE, 2f, 1f);
		Vec3 p = objetivo.position().add(0, objetivo.getBbHeight() / 2, 0);
		mundo.explode(this, damageSources().explosion(this, this), null, p.x, p.y, p.z, fuerza(mundo.getServer()), false,
				Level.ExplosionInteraction.NONE);
		return true;
	}

	/** Las explosiones (la suya y las de otros) casi no le hacen nada. */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (fuente.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) cantidad *= 0.2f;
		return super.hurt(fuente, cantidad);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState bloque) {
		playSound(SONIDO_STOMP, 1f, 0.9f + random.nextFloat() * 0.2f);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SONIDO_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SONIDO_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SONIDO_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 2f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag datos) {
		super.addAdditionalSaveData(datos);
		datos.putInt("Piel", piel());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag datos) {
		super.readAdditionalSaveData(datos);
		entityData.set(PIEL, datos.getInt("Piel"));
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		RawAnimation camina = RawAnimation.begin().thenLoop("animation.tremorsaurio.walk");
		RawAnimation quieto = RawAnimation.begin().thenLoop("animation.tremorsaurio.idle");
		controladores.add(new AnimationController<>(this, "movimiento", 4,
				estado -> estado.setAndContinue(estado.isMoving() ? camina : quieto)));
		controladores.add(new AnimationController<>(this, "accion", 2, estado -> PlayState.STOP)
				.triggerableAnim("mordida", RawAnimation.begin().thenPlay("animation.tremorsaurio.bite"))
				.triggerableAnim("rugido", RawAnimation.begin().thenPlay("animation.tremorsaurio.roar")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}

	// --- La fuerza de la mordida (se guarda con el mundo) ---

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		float fuerza = FUERZA_NORMAL;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("fuerza")) d.fuerza = tag.getFloat("fuerza");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putFloat("fuerza", fuerza);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_tremorsaurio");
	}

	private static float fuerza(MinecraftServer server) {
		return datos(server).fuerza;
	}

	/** /tremorsaurio fuerza [n] (admins): sin número dice la actual; 2 es la normal, de 0,5 a 8. */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tremorsaurio").requires(s -> s.hasPermission(2))
				.then(Commands.literal("fuerza")
						.executes(c -> {
							float f = fuerza(c.getSource().getServer());
							c.getSource().sendSuccess(() -> Component.literal("La mordida del Tremorsaurio tiene fuerza " + f + " (la normal es "
									+ FUERZA_NORMAL + ").").withStyle(ChatFormatting.GOLD), false);
							return 1;
						})
						.then(Commands.argument("n", FloatArgumentType.floatArg(0.5f, 8f))
								.executes(c -> {
									float f = FloatArgumentType.getFloat(c, "n");
									Datos d = datos(c.getSource().getServer());
									d.fuerza = f;
									d.setDirty();
									c.getSource().sendSuccess(() -> Component.literal("Listo: la mordida del Tremorsaurio ahora tiene fuerza " + f + ".")
											.withStyle(ChatFormatting.GOLD), true);
									return 1;
								}))));
	}
}
