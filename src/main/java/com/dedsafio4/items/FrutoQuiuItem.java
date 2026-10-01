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

import static com.dedsafio4.items.DescritoItem.AMARILLO;
import static com.dedsafio4.items.DescritoItem.CELESTE;
import static com.dedsafio4.items.DescritoItem.GRIS;
import static com.dedsafio4.items.DescritoItem.color;
import static com.dedsafio4.items.DescritoItem.parte;

/**
 * Fruto de Quiu: al comerlo, la próxima vez que te morís no perdés nada (como el Keep Inventory).
 * Sirve una sola vez: después de esa muerte se elimina y hay que comer otro.
 * Que no se caigan las cosas ni la experiencia lo hace PlayerGuardarInventarioMixin.
 */
public class FrutoQuiuItem extends Item {
	private static final String MARCA = "dedsafio4_guarda_inventario";
	private static final int MORADO = 0xC77DFF;

	public FrutoQuiuItem(Properties propiedades) {
		super(propiedades);
	}

	/** ¿Tiene la protección y le sirve? (con el keepInventory prendido no hace falta gastarla) */
	public static boolean protegido(Player p) {
		return p.getTags().contains(MARCA) && !p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide && quien instanceof ServerPlayer p) {
			boolean yaLoTenia = !p.addTag(MARCA);
			level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
			p.sendSystemMessage(yaLoTenia
					? parte("Ya tenías ", GRIS).append(parte("Keep Inventory", MORADO)).append(parte(".", GRIS))
					: parte("Conseguiste ", GRIS).append(parte("Keep Inventory", MORADO))
							.append(parte(": se elimina si mueres.", GRIS)));
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Consúmelo para conseguir", GRIS));
		texto.add(parte("Keep Inventory", MORADO).append(parte(".", GRIS)));
		texto.add(Component.empty());
		texto.add(parte("⚠ Atención: Se elimina si mueres.", AMARILLO));
		texto.add(parte("En caso de revivir debes consumirlo", AMARILLO));
		texto.add(parte("nuevamente.", AMARILLO));
	}

	/** Al reaparecer: le devuelve las cosas y la experiencia, y se elimina la protección. */
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
			nuevo.sendSystemMessage(parte("Conservaste tus cosas. El ", GRIS).append(parte("Keep Inventory", MORADO))
					.append(parte(" se eliminó: consume otro ", GRIS)).append(parte("Fruto de Quiu", CELESTE))
					.append(parte(".", GRIS)));
		});
	}
}
