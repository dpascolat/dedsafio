package com.dedsafio4.items;

import com.dedsafio4.dimension.Limbo;
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
 * Nutritótem (antes "Tótem Limbo"): si vas a morir con él en la mano (o en la mano secundaria), te salva como un tótem y además deja tu
 * barra de salud en 10 corazones (20), llena: te devuelve los corazones que te sacaron para siempre el agua del Limbo o
 * el Fantasma Negro y te saca los corazones extra (los de comer Corazones). Si también tienes otro tótem en la mano,
 * se usa ése primero.
 */
public class TotemLimboItem extends Item {
	public TotemLimboItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0xF0D86A);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(Component.literal("Al popear ").withColor(0xE8E8E8).append(Component.literal("reestablece tu").withColor(0xF0D86A)));
		texto.add(Component.literal("Barra de Salud al máximo (20).").withColor(0xE07AE6));
		texto.add(Component.empty());
		texto.add(Component.literal("⚠ Atención: ").withColor(0xF0D86A).append(Component.literal("Si tienes Corazones").withColor(0xFF7A7A)));
		texto.add(Component.literal("Extra, los PERDERÁS al popear.").withColor(0xFF7A7A));
	}

	public static void registrar() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack otra = jugador.getItemInHand(mano);
				if (otra.is(Items.TOTEM_OF_UNDYING) || otra.is(ModItems.TOTEM_FRERICO) || otra.is(ModItems.TOTEM_GLEBANOIDE)) return true;
			}
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack pila = jugador.getItemInHand(mano);
				if (!pila.is(ModItems.TOTEM_LIMBO)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_LIMBO));
				pila.shrink(1);
				// La barra de salud vuelve a 20, llena.
				Limbo.devolverCorazones(jugador);
				CorazonItem.sacarExtra(jugador);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.setHealth(jugador.getMaxHealth());
				jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
				// El aviso 35 hace la animación y el sonido del tótem (el cliente muestra éste: ver ClientPacketListenerMixin).
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				return false;
			}
			return true;
		});
	}
}
