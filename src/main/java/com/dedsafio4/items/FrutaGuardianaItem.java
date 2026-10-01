package com.dedsafio4.items;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Fruta Guardiana: al comerla, la próxima vez que te morís no perdés nada (como el keepInventory).
 * Sirve una sola vez: después de esa muerte ya no la tenés y hay que comer otra.
 * Que no se caigan las cosas ni la experiencia lo hace PlayerGuardarInventarioMixin.
 */
public class FrutaGuardianaItem extends Item {
	private static final String MARCA = "dedsafio4_guarda_inventario";

	public FrutaGuardianaItem(Properties propiedades) {
		super(propiedades);
	}

	/** ¿Tiene la protección y le sirve? (con el keepInventory prendido no hace falta gastarla) */
	public static boolean protegido(Player p) {
		return p.getTags().contains(MARCA) && !p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide && quien instanceof ServerPlayer p) {
			boolean yaLaTenia = !p.addTag(MARCA);
			level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
			p.sendSystemMessage(Component.literal(yaLaTenia
					? "Ya estabas protegido: la próxima vez que mueras no vas a perder nada."
					: "La próxima vez que mueras no vas a perder nada (sirve una sola vez).").withColor(0xF2D45C));
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Cómela y la próxima vez que").withColor(0xC6CFD6));
		texto.add(Component.literal("mueras no perdés el inventario.").withColor(0xC6CFD6));
		texto.add(Component.literal("Sirve para una sola muerte.").withColor(0xF2D45C));
	}

	/** Al reaparecer: le devuelve las cosas y la experiencia, y se gasta la protección. */
	public static void registrar() {
		ServerPlayerEvents.COPY_FROM.register((viejo, nuevo, vivo) -> {
			if (vivo) {
				// Volver del End no es morir: la protección sigue.
				if (viejo.getTags().contains(MARCA)) nuevo.addTag(MARCA);
				return;
			}
			if (!protegido(viejo)) return;
			nuevo.getInventory().replaceWith(viejo.getInventory());
			nuevo.experienceLevel = viejo.experienceLevel;
			nuevo.totalExperience = viejo.totalExperience;
			nuevo.experienceProgress = viejo.experienceProgress;
			nuevo.setScore(viejo.getScore());
			nuevo.removeTag(MARCA);
			nuevo.sendSystemMessage(Component.literal(
					"La Fruta Guardiana te salvó las cosas. Ya se gastó: comé otra para la próxima.").withColor(0xF2D45C));
		});
	}
}
