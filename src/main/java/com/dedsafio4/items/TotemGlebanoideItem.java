package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Tótem Glebanoide: si vas a morir con él en la mano (o en la mano secundaria), te salva y se gasta, como el
 * Nutritótem, pero además al saltar empuja lejos a los mobs que tienes alrededor (para que tengas espacio y te
 * puedas salvar) y te deja 1 segundo sin recibir daño. Si también tienes el de Minecraft o el de Gólem en la mano,
 * se usa ése primero.
 */
public class TotemGlebanoideItem extends Item {
	/** Hasta dónde empuja a los mobs, y con cuánta fuerza. */
	private static final double RADIO_EMPUJE = 6, FUERZA_EMPUJE = 2.2;

	public TotemGlebanoideItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Al salvarte empuja lejos a los").withColor(0xE8E8E8));
		texto.add(Component.literal("mobs que te rodean y te da").withColor(0xE8E8E8));
		texto.add(Component.literal("1 segundo de inmortalidad.").withColor(0xE8E8E8));
	}

	public static void registrar() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (TotemEspacioItem.enEspacio(entidad)) return true;   // ahí sólo sirve el Tótem del Espacio
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack otra = jugador.getItemInHand(mano);
				if (otra.is(Items.TOTEM_OF_UNDYING) || otra.is(ModItems.TOTEM_FRERICO)) return true;
			}
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack pila = jugador.getItemInHand(mano);
				if (!pila.is(ModItems.TOTEM_GLEBANOIDE)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_GLEBANOIDE));
				pila.shrink(1);
				jugador.setHealth(1f);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
				// 1 segundo de inmortalidad: Resistencia V (nada de daño) 20 ticks.
				jugador.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4));
				empujar(jugador);
				// El aviso 35 hace la animación y el sonido del tótem (el cliente muestra éste: ver ClientPacketListenerMixin).
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				AvisoTotem.consumido(jugador, ModItems.TOTEM_GLEBANOIDE);   // "X ha consumido un ..." en el chat de todos
				return false;
			}
			return true;
		});
	}

	/** Empuja lejos (y un poco hacia arriba) a los mobs que están alrededor del jugador. */
	private static void empujar(ServerPlayer jugador) {
		var mundo = jugador.serverLevel();
		var zona = jugador.getBoundingBox().inflate(RADIO_EMPUJE);
		for (net.minecraft.world.entity.Mob mob : mundo.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, zona,
				m -> m.isAlive() && m.distanceTo(jugador) <= RADIO_EMPUJE)) {
			var dir = mob.position().subtract(jugador.position()).multiply(1, 0, 1);
			if (dir.lengthSqr() < 1.0E-4) dir = new net.minecraft.world.phys.Vec3(1, 0, 0);
			// Más fuerte cuanto más cerca está.
			double fuerza = FUERZA_EMPUJE * (1 - mob.distanceTo(jugador) / (RADIO_EMPUJE + 1));
			var v = dir.normalize().scale(fuerza);
			mob.setDeltaMovement(mob.getDeltaMovement().add(v.x, 0.45 + fuerza * 0.2, v.z));
			mob.hurtMarked = true;
		}
		mundo.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, jugador.getX(), jugador.getY(0.5), jugador.getZ(),
				1, 0, 0, 0, 0);
		mundo.playSound(null, jugador.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
				net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
	}
}
