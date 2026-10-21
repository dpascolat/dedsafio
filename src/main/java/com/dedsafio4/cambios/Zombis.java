package com.dedsafio4.cambios;

import com.dedsafio4.mixin.MobAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Cambio "zombi":
 * - zombi 1: la mitad de los zombis (al azar, una sola vez por zombi) escalan paredes como las arañas.
 * - zombi 2: además, todos los zombis llevan una Espada de Hierro con Filo V (que no sueltan).
 * - zombi 3: además, aparecen en el Nether sin importar la luz (ver SpawnPlacementsMixin).
 * Cuentan los zombis, zombis aldeanos, momias y ahogados; no los piglins zombificados.
 */
public final class Zombis {
	private Zombis() {}

	private static final String DECIDIDO = "dedsafio4_zombi_decidido";
	private static final String ESCALA = "dedsafio4_zombi_escala";
	private static final String CASCO_DECIDIDO = "dedsafio4_zombi_casco";
	/** Con zombi_casco: la mitad aparece con casco (sin el cambio, en Minecraft son muy pocos). */
	private static final float PROBABILIDAD_CASCO = 0.5f;
	/** La mitad escala. */
	private static final float PROBABILIDAD_ESCALAR = 0.5f;

	public static boolean esZombi(Entity entidad) {
		return entidad instanceof Zombie && !(entidad instanceof ZombifiedPiglin);
	}

	/** Al aparecer (o al activar el cambio): decide si escala y, en nivel 2, le da la espada. */
	public static void preparar(Zombie zombi, ServerLevel level) {
		casco(zombi, level);
		int nivel = Cambios.nivel(level.getServer(), Cambios.ZOMBI);
		if (nivel < 1) return;
		if (!zombi.getTags().contains(DECIDIDO)) {
			zombi.addTag(DECIDIDO);
			if (zombi.getRandom().nextFloat() < PROBABILIDAD_ESCALAR) zombi.addTag(ESCALA);
		}
		if (zombi.getTags().contains(ESCALA) && !(zombi.getNavigation() instanceof WallClimberNavigation)) {
			((MobAccessor) zombi).dedsafio4$setNavigation(new WallClimberNavigation(zombi, level));
		}
		if (nivel >= 2 && !tieneLaEspada(zombi)) {
			ItemStack espada = new ItemStack(Items.IRON_SWORD);
			espada.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 5);
			zombi.setItemSlot(EquipmentSlot.MAINHAND, espada);
			zombi.setDropChance(EquipmentSlot.MAINHAND, 0f);
		}
	}

	/** zombi_casco 1: una sola vez por zombi, la mitad recibe un casco (cuero, cota de malla, hierro u oro). */
	private static void casco(Zombie zombi, ServerLevel level) {
		if (Cambios.nivel(level.getServer(), Cambios.ZOMBI_CASCO) < 1 || zombi.getTags().contains(CASCO_DECIDIDO)) return;
		zombi.addTag(CASCO_DECIDIDO);
		if (!zombi.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || zombi.getRandom().nextFloat() >= PROBABILIDAD_CASCO) return;
		net.minecraft.world.item.Item[] cascos = {Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET};
		zombi.setItemSlot(EquipmentSlot.HEAD, new ItemStack(cascos[zombi.getRandom().nextInt(cascos.length)]));
	}

	private static boolean tieneLaEspada(Zombie zombi) {
		ItemStack mano = zombi.getMainHandItem();
		return mano.is(Items.IRON_SWORD) && mano.isEnchanted();
	}

	/** ¿Este zombi está trepando una pared ahora? (lo usa el mixin de onClimbable). */
	public static boolean trepando(LivingEntity entidad) {
		if (!esZombi(entidad) || !entidad.getTags().contains(ESCALA)) return false;
		if (!(entidad.level() instanceof ServerLevel level) || Cambios.nivel(level.getServer(), Cambios.ZOMBI) < 1) return false;
		// Chocó contra una pared, o está pegado a una (así no se suelta a mitad de camino). Solo se miran los bloques,
		// no las otras entidades: es más liviano y no se cruza con los mods que procesan entidades en paralelo (Async).
		return entidad.horizontalCollision || !level.noBlockCollision(entidad, entidad.getBoundingBox().inflate(0.06, -0.1, 0.06));
	}
}
