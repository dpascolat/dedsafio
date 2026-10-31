package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Tótem Yunque: como un Tótem de la Inmortalidad (en la mano o en la mano secundaria), y al saltar repara TODA la
 * durabilidad de la armadura que tienes puesta. Si también tienes el de Minecraft o el de Gólem en la mano, se usa ése.
 */
public class TotemYunqueItem extends Item {
	public TotemYunqueItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Al Popear repara TODA").withColor(0xE8E8E8));
		texto.add(Component.literal("la durabilidad de tu").withColor(0xE8E8E8));
		texto.add(Component.literal("Armadura.").withColor(0xE8E8E8));
	}

	public static void registrar() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (TotemEspacioItem.enEspacio(entidad)) return true;   // ahí sólo sirve el Tótem del Espacio
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			if (!TotemManos.leToca(jugador, ModItems.TOTEM_YUNQUE)) return true;
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack pila = jugador.getItemInHand(mano);
				if (!pila.is(ModItems.TOTEM_YUNQUE)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_YUNQUE));
				// El aviso 35 (animación y sonido del tótem) va antes de gastarlo, para que el cliente lo encuentre.
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				AvisoTotem.consumido(jugador, ModItems.TOTEM_YUNQUE);   // "X ha consumido un ..." en el chat de todos
				pila.shrink(1);
				jugador.setHealth(1f);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
				// La armadura puesta queda como nueva.
				for (EquipmentSlot lugar : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
					ItemStack pieza = jugador.getItemBySlot(lugar);
					if (pieza.isDamageableItem()) pieza.setDamageValue(0);
				}
				jugador.level().playSound(null, jugador.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE,
						net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
				return false;
			}
			return true;
		});
	}
}
