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
 * Tótem de Eón: como un Tótem de la Inmortalidad, pero salta aunque esté guardado en cualquier lugar del
 * inventario (no hace falta tenerlo en la mano). Si tienes otro tótem en la mano, se usa ése primero.
 */
public class TotemEonItem extends Item {
	public TotemEonItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Popea aunque esté en").withColor(0xE8E8E8));
		texto.add(Component.literal("tu inventario.").withColor(0xE8E8E8));
	}

	public static void registrar() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			// Si tiene un tótem en la mano, se usa ése (el de Minecraft, el de Gólem o el de Concha).
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack otra = jugador.getItemInHand(mano);
				if (otra.is(Items.TOTEM_OF_UNDYING) || otra.is(ModItems.TOTEM_FRERICO) || otra.is(ModItems.TOTEM_CONCHA)) return true;
			}
			var inventario = jugador.getInventory();
			for (int i = 0; i < inventario.getContainerSize(); i++) {
				ItemStack pila = inventario.getItem(i);
				if (!pila.is(ModItems.TOTEM_EON)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_EON));
				// El aviso 35 (animación y sonido del tótem) va antes de gastarlo, para que el cliente lo encuentre.
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				pila.shrink(1);
				jugador.setHealth(1f);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
				return false;
			}
			return true;
		});
	}
}
