package com.dedsafio4.items;

import com.dedsafio4.eburia.Eburia;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Huevo Eburia: si te lo comés, te da Salud mejorada: +4 de salud máxima por un minuto.
 * No se planta: lo que se planta en los troncos es la Semilla de Eburia.
 */
public class HuevoEburiaItem extends Item {
	private static final int CELESTE = 0x55D9F0, VIOLETA = 0xC96BE0, GRIS = 0xC6CFD6;
	private static final int AMARILLO = 0xFFD24A, NARANJA = 0xFFA23C;

	public HuevoEburiaItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId())
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(CELESTE)));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide) {
			quien.addEffect(new MobEffectInstance(Eburia.SALUD_MEJORADA, Eburia.DURACION, 0, false, true, true));
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		Style celeste = Style.EMPTY.withColor(TextColor.fromRgb(CELESTE));
		Style violeta = Style.EMPTY.withColor(TextColor.fromRgb(VIOLETA));
		Style gris = Style.EMPTY.withColor(TextColor.fromRgb(GRIS));
		Style amarillo = Style.EMPTY.withColor(TextColor.fromRgb(AMARILLO));
		Style naranja = Style.EMPTY.withColor(TextColor.fromRgb(NARANJA));

		texto.add(Component.literal("Salud mejorada (01:00)").withStyle(celeste));
		texto.add(Component.empty());
		texto.add(Component.literal("Al aplicarse:").withStyle(violeta));
		texto.add(Component.literal("Salud máxima: +4").withStyle(celeste));
		texto.add(Component.empty());
		texto.add(Component.literal("Encuéntralo y coséchalo en").withStyle(gris));
		texto.add(Component.literal("los árboles del Centro de Quiu.").withStyle(gris));
		texto.add(Component.empty());
		texto.add(Component.literal("⚠ Atención: ").withStyle(amarillo)
				.append(Component.literal("Al alcanzar su punto").withStyle(gris)));
		texto.add(Component.literal("máximo de crecimiento debes").withStyle(gris));
		texto.add(Component.literal("recogerlo lo antes posible.").withStyle(gris));
		texto.add(Component.literal("Después de un tiempo se pudrirá").withStyle(gris));
		texto.add(Component.literal("si no lo cosechas.").withStyle(gris));
		texto.add(Component.empty());
		texto.add(Component.literal("Utiliza unas ").withStyle(gris)
				.append(Component.literal("Tijeras").withStyle(naranja))
				.append(Component.literal(" mientras tienes").withStyle(gris)));
		texto.add(Component.literal("el ").withStyle(gris)
				.append(Component.literal("Huevo").withStyle(celeste))
				.append(Component.literal(" en la ").withStyle(gris))
				.append(Component.literal("Mano Secundaria").withStyle(naranja)));
		texto.add(Component.literal("para extraer ").withStyle(gris)
				.append(Component.literal("Pelo de Eburia").withStyle(celeste))
				.append(Component.literal(".").withStyle(gris)));
		texto.add(Component.empty());
		texto.add(Component.literal("Utiliza un ").withStyle(gris)
				.append(Component.literal("Hacha").withStyle(naranja))
				.append(Component.literal(" mientras tienes").withStyle(gris)));
		texto.add(Component.literal("un ").withStyle(gris)
				.append(Component.literal("Huevo").withStyle(celeste))
				.append(Component.literal(" en la ").withStyle(gris))
				.append(Component.literal("Mano Secundaria").withStyle(naranja)));
		texto.add(Component.literal("para extraer la ").withStyle(gris)
				.append(Component.literal("Semilla").withStyle(celeste))
				.append(Component.literal(".").withStyle(gris)));
	}

	/** Comerlo es lo que da el efecto. */
	public static boolean sePuedeComer(Player jugador) {
		return true;
	}
}
