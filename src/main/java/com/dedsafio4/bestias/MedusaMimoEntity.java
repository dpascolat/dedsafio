package com.dedsafio4.bestias;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Medusa Mimo: una medusa morada grande que flota tranquila. Cuando ve a un jugador a 16 bloques o menos se convierte
 * en él: su nick, su skin, su armadura y una copia de sus ítems (al jugador no le quita nada y al morir no suelta las
 * copias). Pelea con MedusaMimoPelea. Si se queda 30 s sin nadie cerca, vuelve a ser medusa.
 */
public class MedusaMimoEntity extends Monster {
	private static final EntityDataAccessor<Optional<UUID>> COPIA = SynchedEntityData.defineId(MedusaMimoEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final ResourceLocation CRITICO = ResourceLocation.fromNamespaceAndPath("dedsafio4", "medusa_critico");
	private static final double VISTA = 16, LEJOS = 32;
	private static final int ABURRIDA = 600;
	/** Las veces que se cura aunque el jugador copiado no tenga nada para comer. */
	private static final int CURAS_PROPIAS = 3;

	/** Lo que copió del inventario del jugador. */
	final List<ItemStack> mochila = new ArrayList<>();
	int curasPropias = CURAS_PROPIAS;
	private int sinNadie;
	private Vec3 rumbo = Vec3.ZERO;

	public MedusaMimoEntity(EntityType<? extends Monster> tipo, Level level) {
		super(tipo, level);
		for (EquipmentSlot lugar : EquipmentSlot.values()) setDropChance(lugar, 0f);
		xpReward = 30;
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.ATTACK_SPEED, 4).add(Attributes.FOLLOW_RANGE, LEJOS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		super.defineSynchedData(datos);
		datos.define(COPIA, Optional.empty());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MedusaMimoPelea(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** El jugador que está copiando (null: es medusa). */
	@Nullable
	public UUID copiado() {
		return entityData.get(COPIA).orElse(null);
	}

	public boolean esCopia() {
		return copiado() != null;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
		super.onSyncedDataUpdated(dato);
		if (COPIA.equals(dato)) refreshDimensions();
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return esCopia() ? EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f) : super.getDefaultDimensions(pose);
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!esCopia()) {
			flotar();
			if (getTarget() instanceof Player jugador && distanceTo(jugador) <= VISTA && hasLineOfSight(jugador)) convertirseEn(jugador);
			return;
		}
		sinNadie = level().getNearestPlayer(this, LEJOS) instanceof Player jugador && !jugador.isSpectator() && !jugador.isCreative() ? 0 : sinNadie + 1;
		if (sinNadie >= ABURRIDA) volverAMedusa();
	}

	/** Como medusa: va a la deriva, a unos bloques del piso. */
	private void flotar() {
		setNoGravity(true);
		if (tickCount % 80 == 0) rumbo = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).scale(0.05);
		BlockPos pos = blockPosition();
		int alto = 0;
		while (alto < 7 && level().getBlockState(pos.below(alto + 1)).isAir()) alto++;
		double sube = alto >= 7 ? -0.03 : alto < 3 ? 0.03 : 0.015 * Math.sin(tickCount * 0.06);
		setDeltaMovement(rumbo.x, sube, rumbo.z);
	}

	private void efecto() {
		if (!(level() instanceof ServerLevel mundo)) return;
		mundo.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + getBbHeight() / 2, getZ(), 60, 0.7, 1, 0.7, 0.04);
		mundo.sendParticles(ParticleTypes.PORTAL, getX(), getY() + getBbHeight() / 2, getZ(), 80, 0.7, 1, 0.7, 0.4);
		playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 1.1f);
	}

	private void convertirseEn(Player jugador) {
		efecto();
		entityData.set(COPIA, Optional.of(jugador.getUUID()));
		setNoGravity(false);
		setDeltaMovement(Vec3.ZERO);
		resetFallDistance();
		setCustomName(Component.literal(jugador.getGameProfile().getName()));
		setCustomNameVisible(true);
		setHealth(getMaxHealth());
		mochila.clear();
		for (EquipmentSlot lugar : EquipmentSlot.values()) {
			if (lugar.isArmor()) setItemSlot(lugar, jugador.getItemBySlot(lugar).copy());
		}
		for (ItemStack pila : jugador.getInventory().items) {
			if (!pila.isEmpty()) mochila.add(pila.copy());
		}
		setItemSlot(EquipmentSlot.OFFHAND, jugador.getOffhandItem().copy());
		equiparArma();
	}

	private void volverAMedusa() {
		efecto();
		entityData.set(COPIA, Optional.empty());
		setCustomName(null);
		setCustomNameVisible(false);
		for (EquipmentSlot lugar : EquipmentSlot.values()) setItemSlot(lugar, ItemStack.EMPTY);
		mochila.clear();
		curasPropias = CURAS_PROPIAS;
		sinNadie = 0;
		setTarget(null);
	}

	/** Cuánto pega esa pila en la mano. */
	private static double danioDe(ItemStack pila) {
		double[] suma = {0};
		pila.forEachModifier(EquipmentSlot.MAINHAND, (atributo, mod) -> {
			if (atributo.equals(Attributes.ATTACK_DAMAGE) && mod.operation() == AttributeModifier.Operation.ADD_VALUE) suma[0] += mod.amount();
		});
		return suma[0];
	}

	/** En la mano, lo que más pega de la mochila. */
	void equiparArma() {
		mochila.removeIf(ItemStack::isEmpty);
		ItemStack mejor = ItemStack.EMPTY;
		for (ItemStack pila : mochila) {
			if (danioDe(pila) > danioDe(mejor)) mejor = pila;
		}
		setItemSlot(EquipmentSlot.MAINHAND, mejor);
	}

	/** Lo primero de la mochila que sea ese ítem. */
	ItemStack buscar(net.minecraft.world.item.Item item) {
		for (ItemStack pila : mochila) {
			if (pila.is(item)) return pila;
		}
		return ItemStack.EMPTY;
	}

	private static int valorCura(ItemStack pila) {
		if (pila.is(Items.ENCHANTED_GOLDEN_APPLE)) return 100;
		if (pila.is(Items.GOLDEN_APPLE)) return 80;
		if (pila.is(Items.POTION)) {
			PotionContents pocion = pila.get(DataComponents.POTION_CONTENTS);
			if (pocion == null) return 0;
			for (MobEffectInstance efecto : pocion.getAllEffects()) {
				if (efecto.getEffect().equals(MobEffects.HEAL) || efecto.getEffect().equals(MobEffects.REGENERATION)) return 70;
			}
			return 0;
		}
		var comida = pila.get(DataComponents.FOOD);
		return comida != null ? comida.nutrition() : 0;
	}

	/** Lo mejor para curarse: de la mochila, o una manzana dorada propia si le quedan (vacía si no hay nada). */
	ItemStack mejorCura() {
		ItemStack mejor = ItemStack.EMPTY;
		for (ItemStack pila : mochila) {
			if (valorCura(pila) > valorCura(mejor)) mejor = pila;
		}
		if (mejor.isEmpty() && curasPropias > 0) {
			curasPropias--;
			return new ItemStack(Items.GOLDEN_APPLE);
		}
		return mejor;
	}

	/** Golpe crítico si viene cayendo, como un jugador. */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		boolean critico = fallDistance > 0 && !onGround() && !isInWater();
		var danio = getAttribute(Attributes.ATTACK_DAMAGE);
		if (critico) danio.addTransientModifier(new AttributeModifier(CRITICO, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		boolean pego = super.doHurtTarget(objetivo);
		if (critico) {
			danio.removeModifier(CRITICO);
			if (pego && level() instanceof ServerLevel mundo) {
				mundo.sendParticles(ParticleTypes.CRIT, objetivo.getX(), objetivo.getY(0.6), objetivo.getZ(), 14, 0.3, 0.4, 0.3, 0.2);
				level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1f, 1f);
			}
		}
		return pego;
	}

	/** Mientras usa algo, no cambia de mano por error. */
	void llevarEnMano(ItemStack pila) {
		setItemSlot(EquipmentSlot.MAINHAND, pila);
	}

	void usarMano() {
		startUsingItem(InteractionHand.MAIN_HAND);
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return true;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource origen) {
		return esCopia() ? SoundEvents.PLAYER_HURT : SoundEvents.SLIME_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return esCopia() ? SoundEvents.PLAYER_DEATH : SoundEvents.SLIME_DEATH;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource origen) {
		return esCopia() && super.causeFallDamage(distancia, multiplicador, origen);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		copiadoGuardar(tag);
		tag.putInt("curas", curasPropias);
		ListTag lista = new ListTag();
		for (ItemStack pila : mochila) {
			if (!pila.isEmpty()) lista.add(pila.save(registryAccess()));
		}
		tag.put("mochila", lista);
	}

	private void copiadoGuardar(CompoundTag tag) {
		if (esCopia()) tag.putUUID("copia", copiado());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(COPIA, tag.hasUUID("copia") ? Optional.of(tag.getUUID("copia")) : Optional.empty());
		curasPropias = tag.contains("curas") ? tag.getInt("curas") : CURAS_PROPIAS;
		mochila.clear();
		for (Tag t : tag.getList("mochila", Tag.TAG_COMPOUND)) ItemStack.parse(registryAccess(), t).ifPresent(mochila::add);
		if (esCopia()) setNoGravity(false);
	}

	/** Para la prueba y para quien la mire: si el objetivo es un jugador vivo. */
	boolean tieneObjetivo() {
		LivingEntity objetivo = getTarget();
		return objetivo != null && objetivo.isAlive();
	}
}
