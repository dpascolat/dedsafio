package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Tótem Montaña: si vas a morir con él en la mano (o en la mano secundaria), te salva y se gasta, como el Nutritótem,
 * y además te da Resistencia al máximo (V: nada de daño) durante 2 segundos. Si tienes otro tótem en la otra mano, se
 * gasta el de la mano principal (ver TotemManos).
 */
public class TotemMontanaItem extends Item {
	/** El amarillo del nombre y el violeta de la Resistencia, como en la imagen de Patricio. */
	private static final int AMARILLO = 0xFFD84A, VIOLETA = 0xD98CFF, GRIS = 0xE8E8E8;

	public TotemMontanaItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(AMARILLO);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Otorga ").withColor(GRIS).append(Component.literal("Resistencia (Nivel").withColor(VIOLETA)));
		texto.add(Component.literal("Máx. 2s)").withColor(VIOLETA).append(Component.literal(" al popear.").withColor(GRIS)));
	}

	public static void registrar() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (TotemEspacioItem.enEspacio(entidad)) return true;   // ahí sólo sirve el Tótem del Espacio
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			if (!TotemManos.leToca(jugador, ModItems.TOTEM_MONTANA)) return true;
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack pila = jugador.getItemInHand(mano);
				if (!pila.is(ModItems.TOTEM_MONTANA)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_MONTANA));
				pila.shrink(1);
				jugador.setHealth(1f);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
				// Resistencia al máximo (V: nada de daño) 2 segundos.
				jugador.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
				// El aviso 35 hace la animación y el sonido del tótem (el cliente muestra éste: ver ClientPacketListenerMixin).
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				AvisoTotem.consumido(jugador, ModItems.TOTEM_MONTANA);   // "X ha consumido un ..." en el chat de todos
				return false;
			}
			return true;
		});
	}
}
