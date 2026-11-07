package com.dedsafio4.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Escopeta Phora (el modelo "escopeta_minecraft" de Patricio): clic derecho dispara 6 perdigones en abanico
 * (3 de daño cada uno, 4,5 si dan en la cabeza; no rompe bloques). Un disparo cada 0,4 segundos y 8 en el cargador.
 * Se recarga con Proyectil Phora (uno por bala) en 2,5 segundos: sola al quedarse sin balas, o con Shift + clic derecho.
 * Las balas y el fin de la recarga se guardan en el ítem (custom_data).
 */
public class EscopetaPhoraItem extends Item {
	public static final float DANIO = 3f, DANIO_CABEZA = 4.5f;
	public static final int CARGADOR = 8, PERDIGONES = 6;
	/** 0,4 segundos entre disparos y 2,5 segundos de recarga (en ticks). */
	public static final int ENTRE_DISPAROS = 8, RECARGA = 50;
	private static final double ALCANCE = 24, DISPERSION = 0.035;
	private static final String BALAS = "dedsafio4_balas", FIN_RECARGA = "dedsafio4_fin_recarga";
	private static final int BLANCO = 0xFFFFFF, CELESTE = 0x55FFFF, NARANJA = 0xFF5A14, LILA = 0xD58CFF;

	public EscopetaPhoraItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.EPIC));
	}

	// ---------------------------------------------------------------- Datos guardados en el ítem

	private static CompoundTag datos(ItemStack pila) {
		CustomData c = pila.get(DataComponents.CUSTOM_DATA);
		return c == null ? new CompoundTag() : c.copyTag();
	}

	public static int balas(ItemStack pila) {
		return Mth.clamp(datos(pila).getInt(BALAS), 0, CARGADOR);
	}

	private static void ponerBalas(ItemStack pila, int balas) {
		CustomData.update(DataComponents.CUSTOM_DATA, pila, t -> t.putInt(BALAS, Mth.clamp(balas, 0, CARGADOR)));
	}

	private static long finRecarga(ItemStack pila) {
		return datos(pila).getLong(FIN_RECARGA);
	}

	private static void ponerFinRecarga(ItemStack pila, long fin) {
		CustomData.update(DataComponents.CUSTOM_DATA, pila, t -> t.putLong(FIN_RECARGA, fin));
	}

	public static boolean recargando(ItemStack pila) {
		return finRecarga(pila) > 0;
	}

	// ---------------------------------------------------------------- Uso

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (level.isClientSide || !(jugador instanceof ServerPlayer sp)) return InteractionResultHolder.consume(pila);
		if (recargando(pila)) return InteractionResultHolder.consume(pila);
		if (jugador.isShiftKeyDown() || balas(pila) == 0) {
			empezarRecarga(sp, pila);
			return InteractionResultHolder.consume(pila);
		}
		disparar(sp, pila);
		return InteractionResultHolder.consume(pila);
	}

	private boolean tieneMunicion(Player jugador) {
		return jugador.getAbilities().instabuild || jugador.getInventory().countItem(ModItems.PROYECTIL_PHORA) > 0;
	}

	private void empezarRecarga(ServerPlayer jugador, ItemStack pila) {
		if (balas(pila) >= CARGADOR) return;
		if (!tieneMunicion(jugador)) {
			jugador.displayClientMessage(Component.literal("Sin ").withColor(BLANCO)
					.append(Component.literal("Proyectil Phora").withColor(CELESTE)).append(Component.literal(" para recargar.").withColor(BLANCO)), true);
			jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.4f);
			return;
		}
		ponerFinRecarga(pila, jugador.level().getGameTime() + RECARGA);
		jugador.getCooldowns().addCooldown(this, RECARGA);
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 1f, 0.8f);
	}

	/** Termina la recarga cuando pasan los 2,5 segundos: carga una bala por cada Proyectil Phora que haya. */
	@Override
	public void inventoryTick(ItemStack pila, Level level, Entity duenio, int lugar, boolean enMano) {
		if (level.isClientSide || !(duenio instanceof ServerPlayer jugador)) return;
		long fin = finRecarga(pila);
		if (fin <= 0 || level.getGameTime() < fin) return;
		int faltan = CARGADOR - balas(pila);
		int cargadas = faltan;
		if (!jugador.getAbilities().instabuild) {
			cargadas = 0;
			var inv = jugador.getInventory();
			for (int i = 0; i < inv.getContainerSize() && cargadas < faltan; i++) {
				ItemStack m = inv.getItem(i);
				if (!m.is(ModItems.PROYECTIL_PHORA)) continue;
				int usar = Math.min(m.getCount(), faltan - cargadas);
				m.shrink(usar);
				cargadas += usar;
			}
		}
		ponerBalas(pila, balas(pila) + cargadas);
		ponerFinRecarga(pila, 0);
		level.playSound(null, jugador.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1f, 0.7f);
		level.playSound(null, jugador.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 1.6f);
	}

	private void disparar(ServerPlayer jugador, ItemStack pila) {
		ServerLevel mundo = jugador.serverLevel();
		ponerBalas(pila, balas(pila) - 1);
		jugador.getCooldowns().addCooldown(this, ENTRE_DISPAROS);
		Vec3 ojos = jugador.getEyePosition();
		Vec3 mira = jugador.getLookAngle();
		// La boca del cañón: un poco adelante y a la derecha de los ojos.
		Vec3 costado = mira.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 boca = ojos.add(mira.scale(0.9)).add(costado.scale(jugador.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.3 : -0.3)).add(0, -0.2, 0);
		mundo.sendParticles(ParticleTypes.FLAME, boca.x, boca.y, boca.z, 8, 0.05, 0.05, 0.05, 0.04);
		mundo.sendParticles(ParticleTypes.LARGE_SMOKE, boca.x, boca.y, boca.z, 4, 0.05, 0.05, 0.05, 0.02);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.7f);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1f, 0.6f);

		// Cada perdigón: un rayo con un poco de desvío; se suma el daño por bicho y se aplica una sola vez
		// (si no, los perdigones del mismo disparo chocarían con la invulnerabilidad de medio segundo).
		Map<LivingEntity, Float> danios = new HashMap<>();
		var azar = jugador.getRandom();
		for (int i = 0; i < PERDIGONES; i++) {
			Vec3 dir = mira.add(azar.nextGaussian() * DISPERSION, azar.nextGaussian() * DISPERSION, azar.nextGaussian() * DISPERSION).normalize();
			Vec3 fin = ojos.add(dir.scale(ALCANCE));
			HitResult bloque = mundo.clip(new ClipContext(ojos, fin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jugador));
			if (bloque.getType() != HitResult.Type.MISS) fin = bloque.getLocation();
			EntityHitResult golpe = ProjectileUtil.getEntityHitResult(jugador, ojos, fin, jugador.getBoundingBox().expandTowards(fin.subtract(ojos)).inflate(1),
					e -> e instanceof LivingEntity v && v.isAlive() && !e.isSpectator() && e != jugador, ojos.distanceToSqr(fin));
			if (golpe != null) {
				fin = golpe.getLocation();
				LivingEntity v = (LivingEntity) golpe.getEntity();
				boolean cabeza = fin.y >= v.getEyeY() - 0.25;
				danios.merge(v, cabeza ? DANIO_CABEZA : DANIO, Float::sum);
				mundo.sendParticles(cabeza ? ParticleTypes.CRIT : ParticleTypes.DAMAGE_INDICATOR, fin.x, fin.y, fin.z, 3, 0.05, 0.05, 0.05, 0.1);
			} else if (bloque.getType() != HitResult.Type.MISS) {
				mundo.sendParticles(ParticleTypes.SMOKE, fin.x, fin.y, fin.z, 2, 0.02, 0.02, 0.02, 0.01);
			}
			// Una estela de chispas naranjas del cañón al impacto.
			double largo = boca.distanceTo(fin);
			for (double d = 1.0; d < largo; d += 1.5) {
				Vec3 p = boca.add(fin.subtract(boca).scale(d / largo));
				mundo.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}
		}
		for (Map.Entry<LivingEntity, Float> e : danios.entrySet()) {
			e.getKey().invulnerableTime = 0;
			e.getKey().hurt(mundo.damageSources().playerAttack(jugador), e.getValue());
		}
		// Retroceso: empuja un poco para atrás.
		jugador.push(-mira.x * 0.25, 0.02, -mira.z * 0.25);
		jugador.hurtMarked = true;
	}

	// ---------------------------------------------------------------- Dibujo y descripción

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(LILA);
	}

	@Override
	public boolean isBarVisible(ItemStack pila) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack pila) {
		return Math.round(13f * balas(pila) / CARGADOR);
	}

	@Override
	public int getBarColor(ItemStack pila) {
		return NARANJA;
	}

	private static MutableComponent t(String texto, int color) {
		return Component.literal(texto).withColor(color);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(t("Utiliza ", BLANCO).append(t("Proyectil Phora", CELESTE)));
		texto.add(t("para recargarla.", BLANCO));
		texto.add(Component.empty());
		texto.add(t("Daño: 3.00", BLANCO));
		texto.add(t("Daño a la cabeza: 4.50", BLANCO));
		texto.add(t("Daño a bloques: 0.00", BLANCO));
		texto.add(t("Frecuencia de disparo: 0.40", BLANCO));
		texto.add(t("Tiempo de recarga: 2.50", BLANCO));
		texto.add(t("Tamaño del cargador: " + CARGADOR, BLANCO));
		texto.add(t("Tipo de munición: ", BLANCO).append(t("Proyectil Phora", CELESTE)));
		texto.add(Component.empty());
		texto.add(t("Munición: ", BLANCO).append(t(balas(pila) + " / " + CARGADOR, NARANJA))
				.append(recargando(pila) ? t("  (recargando...)", 0xAAAAAA) : Component.empty()));
	}

	@Override
	public boolean isFoil(ItemStack pila) {
		return false;
	}

	/**
	 * Que no se baje y vuelva a subir en la mano cada vez que cambian las balas (Fabric: FabricItem; NeoForge:
	 * IItemExtension). Van sin @Override porque cada método existe sólo en uno de los dos.
	 */
	public boolean allowComponentsUpdateAnimation(Player jugador, InteractionHand mano, ItemStack antes, ItemStack ahora) {
		return false;
	}

	public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack ahora, boolean cambioDeLugar) {
		return cambioDeLugar || !ItemStack.isSameItem(antes, ahora);
	}
}
