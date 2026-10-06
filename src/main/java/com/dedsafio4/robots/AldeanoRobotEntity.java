package com.dedsafio4.robots;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Aldeano Robot: un robot con un monitor de cabeza que flota y comercia sin necesitar ninguna mesa de trabajo.
 * Los comercios están en OfertasPhora: cada aldeano tiene una de las listas (al azar) y 7 niveles; sube de nivel
 * comerciando y ahí se desbloquean los comercios de ese nivel. Repone la mercadería cada medio día de Minecraft.
 */
public class AldeanoRobotEntity extends PathfinderMob implements Merchant {
	/** Si alguien está comerciando con él (el cliente lo usa para que salude). */
	private static final EntityDataAccessor<Boolean> COMERCIANDO =
			SynchedEntityData.defineId(AldeanoRobotEntity.class, EntityDataSerializers.BOOLEAN);

	@Nullable
	private Player comprando;
	private MerchantOffers ofertas;
	/** Qué lista de OfertasPhora tiene (-1: todavía no se eligió), su nivel (1 a 7) y su experiencia. */
	private int juego = -1, nivel = 1, xp;
	/** Cuándo repuso la mercadería por última vez (tick del mundo). */
	private long repuso;

	public AldeanoRobotEntity(EntityType<? extends AldeanoRobotEntity> tipo, Level level) {
		super(tipo, level);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.5)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder constructor) {
		super.defineSynchedData(constructor);
		constructor.define(COMERCIANDO, false);
	}

	public boolean estaComerciando() {
		return this.entityData.get(COMERCIANDO);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 0.6));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// ---------- Comercio ----------

	/** Click derecho: abre el comercio, sin mesa ni profesión. */
	@Override
	protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
		if (this.isBaby() || !this.isAlive()) return InteractionResult.PASS;
		if (this.getTradingPlayer() != null) return InteractionResult.sidedSuccess(this.level().isClientSide);
		if (this.level().isClientSide) return InteractionResult.SUCCESS;
		if (this.getOffers().isEmpty()) return InteractionResult.CONSUME;
		this.reponer();
		this.setTradingPlayer(jugador);
		this.openTradingScreen(jugador, this.getDisplayName(), this.nivel);
		return InteractionResult.CONSUME;
	}

	/** Arma la lista: los comercios de su nivel o menos, listos; los de más arriba, bloqueados. */
	private MerchantOffers armarOfertas() {
		if (this.juego < 0 || this.juego >= OfertasPhora.JUEGOS.size()) this.juego = this.random.nextInt(OfertasPhora.JUEGOS.size());
		MerchantOffers lista = new MerchantOffers();
		for (OfertasPhora.Comercio c : OfertasPhora.JUEGOS.get(this.juego)) lista.add(c.nivel() <= this.nivel ? c.oferta() : c.bloqueada());
		return lista;
	}

	/** Al subir de nivel: los comercios de ese nivel se desbloquean (los que ya estaban quedan como están). */
	private void desbloquear() {
		List<OfertasPhora.Comercio> lista = OfertasPhora.JUEGOS.get(this.juego);
		MerchantOffers actuales = this.getOffers();
		for (int i = 0; i < lista.size() && i < actuales.size(); i++) {
			if (lista.get(i).nivel() <= this.nivel && actuales.get(i).getCostB().is(Items.BARRIER)) actuales.set(i, lista.get(i).oferta());
		}
	}

	/** Cada medio día de Minecraft repone la mercadería (los comercios desbloqueados se pueden volver a hacer). */
	private void reponer() {
		long ahora = this.level().getGameTime();
		if (ahora - this.repuso < 12000) return;
		this.repuso = ahora;
		for (MerchantOffer o : this.getOffers()) if (!o.getCostB().is(Items.BARRIER)) o.resetUses();
	}

	/** Suma la experiencia del comercio y, si alcanza, sube de nivel (hasta el 7). */
	private void ganarXp(int cuanto) {
		this.xp += cuanto;
		boolean subio = false;
		while (this.nivel < OfertasPhora.NIVEL_MAXIMO && this.xp >= OfertasPhora.XP_NIVEL[this.nivel + 1]) {
			this.nivel++;
			subio = true;
		}
		if (!subio) return;
		this.desbloquear();
		this.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 1.4f);
		if (this.comprando instanceof net.minecraft.server.level.ServerPlayer jugador
				&& jugador.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu) {
			jugador.sendMerchantOffers(menu.containerId, this.getOffers(), this.nivel, this.xp, this.showProgressBar(), this.canRestock());
		}
	}

	@Override
	public void setTradingPlayer(@Nullable Player jugador) {
		this.comprando = jugador;
		this.entityData.set(COMERCIANDO, jugador != null);
	}

	@Nullable
	@Override
	public Player getTradingPlayer() {
		return this.comprando;
	}

	@Override
	public MerchantOffers getOffers() {
		if (this.ofertas == null) this.ofertas = armarOfertas();
		return this.ofertas;
	}

	@Override
	public void overrideOffers(MerchantOffers ofertas) {
		this.ofertas = ofertas;
	}

	@Override
	public void notifyTrade(MerchantOffer oferta) {
		oferta.increaseUses();
		if (!this.level().isClientSide) this.ganarXp(oferta.getXp());
		this.ambientSoundTime = -this.getAmbientSoundInterval();
		this.playSound(this.getNotifyTradeSound(), 1f, 1f);
	}

	@Override
	public void notifyTradeUpdated(ItemStack pila) {
		if (this.level().isClientSide || this.ambientSoundTime <= -this.getAmbientSoundInterval() + 20) return;
		this.ambientSoundTime = -this.getAmbientSoundInterval();
		this.playSound(SoundEvents.VILLAGER_TRADE, 1f, 1.4f);
	}

	@Override
	public int getVillagerXp() {
		return this.xp;
	}

	@Override
	public void overrideXp(int experiencia) {
		this.xp = experiencia;
	}

	@Override
	public boolean showProgressBar() {
		return true;
	}

	@Override
	public boolean canRestock() {
		return true;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public boolean isClientSide() {
		return this.level().isClientSide;
	}

	// ---------- Varios ----------

	@Override
	public Component getDisplayName() {
		return this.hasCustomName() ? super.getDisplayName() : Component.translatable("entity.dedsafio4.aldeano_robot");
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		MerchantOffers ofertas = this.getOffers();
		tag.putInt("Juego", this.juego);
		tag.putInt("Nivel", this.nivel);
		tag.putInt("Xp", this.xp);
		tag.putLong("Repuso", this.repuso);
		if (!ofertas.isEmpty()) {
			tag.put("Offers", MerchantOffers.CODEC.encodeStart(
					this.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), ofertas)
					.getOrThrow());
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.nivel = Math.max(1, tag.contains("Nivel") ? tag.getInt("Nivel") : 1);
		this.xp = tag.getInt("Xp");
		this.repuso = tag.getLong("Repuso");
		// Los aldeanos de antes (sin lista) se quedan con los comercios nuevos, no con los de prueba.
		if (!tag.contains("Juego")) return;
		this.juego = tag.getInt("Juego");
		if (tag.contains("Offers")) {
			MerchantOffers.CODEC.parse(
					this.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("Offers"))
					.resultOrPartial(error -> {}).ifPresent(leidas -> this.ofertas = leidas);
		}
	}

	@Override
	public void die(DamageSource fuente) {
		super.die(fuente);
		if (this.comprando != null) this.comprando = null;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	/** Igual que un aldeano: no se ahoga ni recibe daño de caída extra, pero sí lo pueden matar. */
	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	/** Para que el modelo sepa si se está moviendo. */
	public boolean avanzando() {
		return this.walkAnimation.speed() > 0.01f;
	}

	/** Sirve para reemplazar a los aldeanos viejos: el tipo original solo se usa como referencia. */
	public static boolean esAldeanoViejo(net.minecraft.world.entity.Entity entidad) {
		return entidad instanceof Villager || entidad instanceof net.minecraft.world.entity.monster.ZombieVillager;
	}
}
