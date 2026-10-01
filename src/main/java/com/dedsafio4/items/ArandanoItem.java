package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Arándano Nocturno: la fruta de los árboles del Centro de Quiu. Si te lo comés te da Visión
 * nocturna por dos minutos. (La semilla y cómo crece en el árbol vienen después.)
 */
public class ArandanoItem extends Item {
	public static final int DURACION = 20 * 60 * 2;
	/** Los mismos colores que el Huevo Eburia. */
	private static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6, AMARILLO = 0xFFD24A, NARANJA = 0xFFA23C;

	public ArandanoItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide) {
			quien.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURACION, 0, false, true, true));
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(parte("Visión nocturna (02:00)", CELESTE));
		texto.add(Component.empty());
		texto.add(parte("Encuéntralo y coséchalo en", GRIS));
		texto.add(parte("los árboles del Centro de Quiu.", GRIS));
		texto.add(Component.empty());
		texto.add(parte("⚠ Atención: ", AMARILLO).append(parte("Al alcanzar su punto", GRIS)));
		texto.add(parte("máximo de crecimiento debes", GRIS));
		texto.add(parte("recogerlo lo antes posible.", GRIS));
		texto.add(parte("Después de un tiempo se pudrirá", GRIS));
		texto.add(parte("si no lo cosechas.", GRIS));
		texto.add(Component.empty());
		texto.add(parte("Utiliza un ", GRIS).append(parte("Hacha", NARANJA)).append(parte(" mientras tienes", GRIS)));
		texto.add(parte("el ", GRIS).append(parte("Arándano", CELESTE)).append(parte(" en la ", GRIS))
				.append(parte("Mano Secundaria", NARANJA)));
		texto.add(parte("para extraer la ", GRIS).append(parte("Semilla", CELESTE)).append(parte(".", GRIS)));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}
}
