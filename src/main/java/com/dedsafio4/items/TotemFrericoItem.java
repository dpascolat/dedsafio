package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Tótem de Gólem (id totem_frerico): mientras lo tienes en la mano secundaria tienes 2 corazones más de
 * vida máxima (al sacarlo, se van). Y como un Tótem de la Inmortalidad: si vas a morir con él en la
 * mano, te salva y se gasta.
 */
public class TotemFrericoItem extends Item {
	/** 2 corazones = 4 puntos de vida. */
	public static final double VIDA = 4;
	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "totem_frerico");

	public TotemFrericoItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	private static boolean loTiene(Player jugador) {
		return jugador.getOffhandItem().is(ModItems.TOTEM_FRERICO);
	}

	/** Pone o saca los 2 corazones según tenga el tótem en alguna mano. */
	private static void actualizar(ServerPlayer jugador) {
		AttributeInstance vida = jugador.getAttribute(Attributes.MAX_HEALTH);
		if (vida == null) return;
		AttributeModifier actual = vida.getModifier(ID);
		if (loTiene(jugador)) {
			if (actual == null || actual.amount() != VIDA) {
				vida.removeModifier(ID);
				vida.addTransientModifier(new AttributeModifier(ID, VIDA, AttributeModifier.Operation.ADD_VALUE));
			}
		} else if (actual != null) {
			// (También limpia los corazones que daba la versión vieja, que se "comía".)
			vida.removeModifier(ID);
			if (jugador.getHealth() > jugador.getMaxHealth()) jugador.setHealth(jugador.getMaxHealth());
		}
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		int blanco = 0xE8E8E8, amarillo = 0xFFD84A, rosa = 0xE070E0;
		texto.add(Component.empty());
		texto.add(Component.literal("Al utilizarlo en la ").withColor(blanco).append(Component.literal("Mano").withColor(amarillo)));
		texto.add(Component.literal("Secundaria").withColor(amarillo).append(Component.literal(" obtienes ").withColor(blanco))
				.append(Component.literal("dos").withColor(rosa)));
		texto.add(Component.literal("corazones extra.").withColor(blanco));
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) actualizar(jugador);
		});
		// Te salva de morir, igual que el Tótem de la Inmortalidad (tampoco salva del vacío ni de /kill).
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entidad, fuente, danio) -> {
			if (!(entidad instanceof ServerPlayer jugador)) return true;
			if (fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			// Si también tiene un Tótem de la Inmortalidad, que lo use Minecraft como siempre.
			if (jugador.getMainHandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)
					|| jugador.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) return true;
			for (net.minecraft.world.InteractionHand mano : net.minecraft.world.InteractionHand.values()) {
				ItemStack pila = jugador.getItemInHand(mano);
				if (!pila.is(ModItems.TOTEM_FRERICO)) continue;
				jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_FRERICO));
				pila.shrink(1);
				actualizar(jugador);   // sin el tótem en la mano, se van los 2 corazones
				jugador.setHealth(1f);
				jugador.removeAllEffects();
				com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
				jugador.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 900, 1));
				jugador.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 1));
				jugador.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 800, 0));
				// El aviso 35 hace la animación y el sonido del tótem (el cliente muestra el Frerico: ver ClientPacketListenerMixin).
				jugador.level().broadcastEntityEvent(jugador, (byte) 35);
				return false;
			}
			return true;
		});
	}
}
