package com.dedsafio4.casino;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * El Casino (la máquina tragamonedas del Dedsafío 3): queda quieto, mirando hacia donde lo pusieron. No se mueve, no
 * lo empujan y solo lo puede romper alguien en creativo.
 * Con click derecho con una Dedita Casino se gasta la dedita y las ruedas giran: sale una tirada con las 3 figuras
 * iguales, con 2 iguales o sin ninguna repetida. Cuando paran las ruedas se da el premio (ver CasinoPremios; solo si
 * está prendido con /casino). Mientras gira no acepta otra dedita.
 */
public class CasinoEntity extends Mob implements GeoEntity {
	private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("idle");

	/**
	 * Una tirada (una animación): qué figura queda repetida, cuántas veces (1 = ninguna), en qué tick paran las ruedas
	 * (ahí se da el premio) y cuánto dura. Sale de dónde frena cada rueda en la animación.
	 */
	private record Resultado(String figura, int cantidad, int parada, int duracion) {}
	private static final java.util.Map<String, Resultado> RESULTADOS = java.util.Map.ofEntries(
			// Ninguna repetida.
			java.util.Map.entry("4", new Resultado("", 1, 202, 240)),
			java.util.Map.entry("5", new Resultado("", 1, 205, 240)),
			java.util.Map.entry("7", new Resultado("", 1, 179, 240)),
			// 2 iguales.
			java.util.Map.entry("1", new Resultado("botella de experiencia", 2, 230, 320)),
			java.util.Map.entry("3", new Resultado("corazon", 2, 201, 271)),
			java.util.Map.entry("6", new Resultado("libro de encantamientos", 2, 160, 199)),
			java.util.Map.entry("2 hierro", new Resultado("hierro", 2, 230, 320)),
			java.util.Map.entry("2 pechera de hierro", new Resultado("pechera de hierro", 2, 201, 271)),
			java.util.Map.entry("2 pico de hierro", new Resultado("pico de hierro", 2, 160, 199)),
			java.util.Map.entry("2 filete", new Resultado("filete", 2, 230, 320)),
			java.util.Map.entry("2 poción", new Resultado("poción", 2, 201, 271)),
			// 3 iguales.
			java.util.Map.entry("libro de encantamientos", new Resultado("libro de encantamientos", 3, 148, 188)),
			java.util.Map.entry("pico de hierro", new Resultado("pico de hierro", 3, 170, 220)),
			java.util.Map.entry("pechera de hierro", new Resultado("pechera de hierro", 3, 181, 220)),
			java.util.Map.entry("filete", new Resultado("filete", 3, 202, 235)),
			java.util.Map.entry("corazon", new Resultado("corazon", 3, 204, 240)),
			java.util.Map.entry("hierro", new Resultado("hierro", 3, 201, 240)),
			java.util.Map.entry("poción", new Resultado("poción", 3, 162, 200)),
			java.util.Map.entry("botella de experiencia", new Resultado("botella de experiencia", 3, 176, 220)));

	/**
	 * De cada 100 tiradas: cuántas salen con 3 iguales y cuántas con 2 (el resto, ninguna repetida). De las de 3
	 * iguales, los 3 corazones salen solo 1 de cada 100 tiradas (1 %); las otras 19 se reparten entre las demás figuras.
	 */
	private static final int PROBABILIDAD_TRES = 20, PROBABILIDAD_DOS = 30, PROBABILIDAD_TRES_CORAZONES = 1;
	/** Las animaciones que se pueden disparar con animar(nombre). */
	public static final java.util.List<String> ANIMACIONES = RESULTADOS.keySet().stream().sorted().toList();

	/** Hasta qué tick está girando (no acepta otra dedita). */
	private int girandoHasta = 0;
	/** La tirada que está girando, para quién es el premio y en qué tick se da. */
	private Resultado pendiente;
	private java.util.UUID jugadorPendiente;
	private int premioEn;

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	public CasinoEntity(EntityType<? extends CasinoEntity> tipo, Level mundo) {
		super(tipo, mundo);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	/**
	 * Al ponerlo queda derecho (norte, sur, este u oeste), mirando hacia el que lo puso: si lo ponés mirando al norte,
	 * queda mirando al sur.
	 */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor mundo, DifficultyInstance dificultad, MobSpawnType tipo,
										SpawnGroupData datos) {
		Player jugador = mundo.getNearestPlayer(this, 16);
		if (jugador != null) {
			float giro = jugador.getDirection().getOpposite().toYRot();
			setYRot(giro);
			yRotO = giro;
		}
		return super.finalizeSpawn(mundo, dificultad, tipo, datos);
	}

	@Override
	public void tick() {
		super.tick();
		// Cuando paran las ruedas, el premio (si la tabla de premios está prendida con /casino).
		if (!level().isClientSide && pendiente != null && tickCount >= premioEn) {
			if (level().getPlayerByUUID(jugadorPendiente) instanceof net.minecraft.server.level.ServerPlayer jugador) {
				CasinoPremios.dar(jugador, pendiente.figura(), pendiente.cantidad(), this);
			}
			pendiente = null;
		}
		// Una máquina: el cuerpo y la "cabeza" siempre para el mismo lado.
		yBodyRot = getYRot();
		yBodyRotO = getYRot();
		yHeadRot = getYRot();
		yHeadRotO = getYRot();
	}

	/** Solo lo rompe alguien en creativo (o /kill). */
	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		if (fuente.getEntity() instanceof Player p && p.isCreative()) return super.hurt(fuente, Float.MAX_VALUE);
		if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(fuente, cantidad);
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(net.minecraft.world.entity.Entity otro) {
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		AnimationController<CasinoEntity> principal =
				new AnimationController<>(this, "principal", 0, estado -> estado.setAndContinue(QUIETO));
		for (String nombre : ANIMACIONES) principal.triggerableAnim(nombre, RawAnimation.begin().thenPlay(nombre));
		controladores.add(principal);
	}

	/** Click derecho con una Dedita Casino: se gasta y las ruedas giran. */
	@Override
	protected net.minecraft.world.InteractionResult mobInteract(Player jugador, net.minecraft.world.InteractionHand mano) {
		net.minecraft.world.item.ItemStack item = jugador.getItemInHand(mano);
		if (!item.is(com.dedsafio4.items.ModItems.DEDITA_CASINO)) return super.mobInteract(jugador, mano);
		if (level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
		if (tickCount < girandoHasta) {
			jugador.displayClientMessage(net.minecraft.network.chat.Component.literal("El casino está girando...")
					.withStyle(net.minecraft.ChatFormatting.GOLD), true);
			return net.minecraft.world.InteractionResult.CONSUME;
		}
		var azar = getRandom();
		int suerte = azar.nextInt(100);
		int iguales = suerte < PROBABILIDAD_TRES ? 3 : suerte < PROBABILIDAD_TRES + PROBABILIDAD_DOS ? 2 : 1;
		String tirada;
		if (suerte < PROBABILIDAD_TRES_CORAZONES) {
			tirada = "corazon";
		} else {
			var nombres = ANIMACIONES.stream().filter(n -> RESULTADOS.get(n).cantidad() == iguales && !n.equals("corazon")).toList();
			tirada = nombres.get(azar.nextInt(nombres.size()));
		}
		String forzada = System.getenv("DEDSAFIO4_CASINO_FORZAR");   // solo para las pruebas automáticas
		if (forzada != null && RESULTADOS.containsKey(forzada)) tirada = forzada;
		animar(tirada);
		pendiente = RESULTADOS.get(tirada);
		girandoHasta = tickCount + pendiente.duracion();
		jugadorPendiente = jugador.getUUID();
		premioEn = tickCount + pendiente.parada();
		item.consume(1, jugador);
		level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.LEVER_CLICK,
				net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.8f);
		return net.minecraft.world.InteractionResult.CONSUME;
	}

	/** Hace girar las ruedas con una de las animaciones (del lado del servidor; la ven todos). */
	public void animar(String nombre) {
		triggerAnim("principal", nombre);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}

	/** Para que no dé vueltas buscando dónde mirar. */
	@Override
	protected void registerGoals() {
	}

	@Override
	public boolean isInvulnerableTo(DamageSource fuente) {
		return super.isInvulnerableTo(fuente) || fuente.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
				|| fuente.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING);
	}
}
