package com.dedsafio4.lagartos;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import com.dedsafio4.items.ModItems;
import com.dedsafio4.nave.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import com.dedsafio4.reptisaurios.VenenoPrimitivo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import net.minecraft.world.level.Level;

/**
 * Los cuatro lagartos: Mira, Tizón, Jade y Ámbar. Comparten el cuerpo y cambian los colores
 * y algún adorno (cuernos, cresta alta, hombreras). Por ahora caminan y se defienden si los atacan.
 */
public class LagartoEntity extends PathfinderMob {
	/** Cada hermano: colores (escamas, oscuro, panza, cresta, garras) y su adorno. */
	public enum Variante {
		MIRA("mira", 0x2F5A1E, 0x1D3A12, 0x6B7A3A, 0x3F6B25, 0xF2F1EA),
		/** El guerrero: se lanza de frente, aguanta mucho y pega seguido. */
		REPTISAURIO_GUERRERO("reptisaurio_guerrero", 0x5A3A22, 0x3A2414, 0xC9A45A, 0xD8C23A, 0xF2F1EA),
		/** El del arco: dispara de lejos y sus flechas también envenenan. */
		REPTISAURIO_ARQUERO("reptisaurio_arquero", 0x1F9A4A, 0x0F5A2A, 0xB9E0C0, 0xE8ECE6, 0xE8ECE6),
		/** El de la lanza: pelea de lejos y envenena. */
		REPTISAURIO_LANZA("reptisaurio_lanza", 0xC07A1E, 0x7A4A10, 0xE8C870, 0x4F7A2A, 0xF2F1EA);

		public final String nombre;
		public final int escamas, oscuro, panza, cresta, garras;

		Variante(String nombre, int escamas, int oscuro, int panza, int cresta, int garras) {
			this.nombre = nombre;
			this.escamas = escamas;
			this.oscuro = oscuro;
			this.panza = panza;
			this.cresta = cresta;
			this.garras = garras;
		}
	}

	private final Variante variante;

	public LagartoEntity(EntityType<? extends LagartoEntity> tipo, Level level, Variante variante) {
		super(tipo, level);
		this.variante = variante;
	}

	public Variante variante() {
		return variante;
	}

	/** El Reptisaurio Salvaje aparece en las cuevas: donde no llega el cielo, a oscuras y sobre piso firme. */
	public static boolean puedeAparecerEnCueva(EntityType<LagartoEntity> tipo, LevelAccessor mundo, MobSpawnType razon,
											   BlockPos pos, RandomSource azar) {
		if (razon == MobSpawnType.SPAWNER) return true;
		return mundo.getBrightness(LightLayer.SKY, pos) == 0 && mundo.getBrightness(LightLayer.BLOCK, pos) < 8
				&& mundo.getBlockState(pos.below()).isSolidRender(mundo, pos.below());
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	/** El Guerrero es el que aguanta: más vida, algo de armadura y golpes más fuertes. */
	public static AttributeSupplier.Builder atributosGuerrero() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	/** Dispara una flecha con Veneno Primitivo. */
	public void disparar(LivingEntity objetivo) {
		Arrow flecha = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
		flecha.setBaseDamage(2.0);
		flecha.addEffect(new MobEffectInstance(VenenoPrimitivo.EFECTO, VenenoPrimitivo.DURACION, 0, false, true, true));
		double dx = objetivo.getX() - this.getX();
		double dy = objetivo.getY(0.6) - flecha.getY();
		double dz = objetivo.getZ() - this.getZ();
		double plano = Math.sqrt(dx * dx + dz * dz);
		flecha.shoot(dx, dy + plano * 0.12, dz, 1.6f, 2f);
		this.level().addFreshEntity(flecha);
		this.playSound(SoundEvents.ARROW_SHOOT, 1f, 1f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
	}

	/** Dispara de lejos y se aleja si lo acorralan. */
	static class ArcoGoal extends Goal {
		private static final double ALCANCE = 16.0, IDEAL = 8.0, MUY_CERCA = 5.0;
		private static final int CADENCIA = 40;

		private final LagartoEntity lagarto;
		private int enfriamiento = CADENCIA;

		ArcoGoal(LagartoEntity lagarto) {
			this.lagarto = lagarto;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = lagarto.getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = lagarto.getTarget();
			if (objetivo == null) return;
			lagarto.getLookControl().setLookAt(objetivo, 30f, 30f);
			double distancia = lagarto.distanceTo(objetivo);
			boolean loVe = lagarto.hasLineOfSight(objetivo);

			if (distancia < MUY_CERCA) {
				Vec3 atras = lagarto.position().subtract(objetivo.position()).normalize().scale(4);
				Vec3 destino = lagarto.position().add(atras);
				lagarto.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.2);
			} else if (distancia > IDEAL || !loVe) {
				lagarto.getNavigation().moveTo(objetivo, 1.0);
			} else {
				lagarto.getNavigation().stop();
			}

			if (--enfriamiento <= 0 && distancia <= ALCANCE && loVe) {
				enfriamiento = CADENCIA;
				lagarto.swing(InteractionHand.MAIN_HAND);
				lagarto.disparar(objetivo);
			}
		}

		@Override
		public void stop() {
			lagarto.getNavigation().stop();
		}
	}

	/**
	 * Pelea con la lanza: se queda a distancia (ni muy lejos ni pegado) y estoca.
	 * Si el objetivo se le acerca demasiado, retrocede.
	 */
	static class LanzaGoal extends Goal {
		/** Alcance de la lanza y distancia a la que trata de quedarse. */
		private static final double ALCANCE = 4.5, IDEAL = 3.6, MUY_CERCA = 2.6;
		private static final int CADENCIA = 30;

		private final LagartoEntity lagarto;
		private int enfriamiento;

		LanzaGoal(LagartoEntity lagarto) {
			this.lagarto = lagarto;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity objetivo = lagarto.getTarget();
			return objetivo != null && objetivo.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity objetivo = lagarto.getTarget();
			if (objetivo == null) return;
			lagarto.getLookControl().setLookAt(objetivo, 30f, 30f);
			double distancia = lagarto.distanceTo(objetivo);

			if (distancia < MUY_CERCA) {
				// Demasiado cerca: retrocede sin dejar de mirarlo.
				Vec3 atras = lagarto.position().subtract(objetivo.position()).normalize().scale(3);
				Vec3 destino = lagarto.position().add(atras);
				lagarto.getNavigation().moveTo(destino.x, destino.y, destino.z, 1.1);
			} else if (distancia > IDEAL) {
				lagarto.getNavigation().moveTo(objetivo, 1.0);
			} else {
				lagarto.getNavigation().stop();
			}

			if (--enfriamiento <= 0 && distancia <= ALCANCE && lagarto.hasLineOfSight(objetivo)) {
				enfriamiento = CADENCIA;
				lagarto.swing(InteractionHand.MAIN_HAND);
				lagarto.doHurtTarget(objetivo);
			}
		}

		@Override
		public void stop() {
			lagarto.getNavigation().stop();
		}
	}

	/**
	 * ¿Es el Reptisaurio con Lanza? Se mira por el tipo y no por el campo, porque Minecraft
	 * arma las metas del mob dentro del constructor de arriba, antes de guardar la variante.
	 */
	public boolean conLanza() {
		return this.getType() == com.dedsafio4.nave.ModEntidades.REPTISAURIO_LANZA;
	}

	/** ¿Es el Reptisaurio Arquero? */
	public boolean conArco() {
		return this.getType() == com.dedsafio4.nave.ModEntidades.REPTISAURIO_ARQUERO;
	}

	/** ¿Es el Reptisaurio Guerrero? */
	public boolean esGuerrero() {
		return this.getType() == com.dedsafio4.nave.ModEntidades.REPTISAURIO_GUERRERO;
	}

	/** Los Reptisaurios: pelean, envenenan y sueltan sangre. */
	public boolean esReptisaurio() {
		return conLanza() || conArco() || esGuerrero();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (conLanza()) {
			this.goalSelector.addGoal(1, new LanzaGoal(this));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else if (conArco()) {
			this.goalSelector.addGoal(1, new ArcoGoal(this));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else if (esGuerrero()) {
			// Va de frente y pega seguido.
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else {
			// Reptisaurio Salvaje: muy territorial, ataca al instante.
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		}
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	/** Con la lanza pega de lejos y deja Veneno Primitivo. */
	@Override
	public boolean doHurtTarget(Entity objetivo) {
		boolean pego = super.doHurtTarget(objetivo);
		if (pego && esReptisaurio() && objetivo instanceof LivingEntity victima) {
			VenenoPrimitivo.aplicar(victima);
		}
		return pego;
	}

	/** Siempre suelta Sangre de Reptisaurio. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource fuente, boolean muertePorJugador) {
		super.dropCustomDeathLoot(level, fuente, muertePorJugador);
		if (esReptisaurio() || this.getType() == ModEntidades.MIRA) {
			this.spawnAtLocation(new ItemStack(ModItems.SANGRE_REPTISAURIO));
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SNIFFER_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.SNIFFER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SNIFFER_DEATH;
	}
}
