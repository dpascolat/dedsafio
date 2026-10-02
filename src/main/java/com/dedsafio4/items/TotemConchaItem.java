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
 * Tótem de Concha: como un Tótem de la Inmortalidad. Si vas a morir con él en la mano (o en la mano secundaria),
 * te salva y se gasta. Si también tenés un Tótem de la Inmortalidad o el Totem Frerico en la mano, se usa ése primero.
 */
public class TotemConchaItem extends Item {
	public TotemConchaItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Te salva de la muerte (y se gasta).").withColor(0xC6CFD6));
	}

	public static void registrar() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack otra = jugador.getItemInHand(mano);
				if (otra.is(Items.TOTEM_OF_UNDYING) || otra.is(ModItems.TOTEM_FRERICO)) return true;
			}
			for (InteractionHand mano : InteractionHand.values()) {
				ItemStack pila = jugador.getItemInHand(mano);
				if (!pila.is(ModItems.TOTEM_CONCHA)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_CONCHA));
				pila.shrink(1);
				jugador.setHealth(1f);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
				jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
				// El aviso 35 hace la animación y el sonido del tótem (el cliente muestra éste: ver ClientPacketListenerMixin).
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				return false;
			}
			return true;
		});
	}
}
