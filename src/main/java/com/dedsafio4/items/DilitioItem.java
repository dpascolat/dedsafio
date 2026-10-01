package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import static com.dedsafio4.items.DescritoItem.parte;

/** Dilitio: cristal verde que sale del Bloque de Dilitio (con Pico de Diamante o mejor). */
public class DilitioItem extends Item {
	private static final int VERDE = 0x4AA852, DORADO = 0xFFAA00, BLANCO = 0xFFFFFF;
	private static final Style ICONOS = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "iconos"));
	private static final String ICONO_DEDITA = String.valueOf((char) 0xE002);

	public DilitioItem(Properties propiedades) {
		super(propiedades);
	}

	private static MutableComponent dedita() {
		// En blanco, así el ícono se ve con sus colores (si no, toma el color del texto).
		return Component.literal(ICONO_DEDITA).withStyle(ICONOS.withColor(BLANCO));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withStyle(DescritoItem.color(VERDE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(parte("Valor de venta: ", DORADO).append(parte("1 ", BLANCO)).append(dedita()).append(parte(" por 2", DORADO)));
		texto.add(parte(" - Por unidad: ", DORADO).append(parte("0.50 ", BLANCO)).append(dedita()));
		texto.add(Component.empty());
		texto.add(parte("Utiliza un ", DescritoItem.GRIS).append(parte("Pico de Diamante", DORADO)));
		texto.add(parte("o superior sobre un ", DescritoItem.GRIS).append(parte("Bloque", VERDE)));
		texto.add(parte("de Dilitio", VERDE).append(parte(" para extraerlo.", DescritoItem.GRIS)));
	}
}
