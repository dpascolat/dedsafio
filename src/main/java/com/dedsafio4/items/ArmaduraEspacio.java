package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

import java.util.List;

/**
 * Las piezas Dimensionales (Casco, Pechera, Pantalones y Botas; íconos y descripción de Patricio) y la Espada del
 * Espacio. Las piezas no se ponen: son para combinarlas y armar el Conjunto Interdimensional (todavía no existe).
 * La espada pega igual que la de netherita y no se quema. Por ahora todo sale sólo en creativo o con /give.
 */
public final class ArmaduraEspacio {
	private ArmaduraEspacio() {}

	/**
	 * Una pieza del Conjunto Interdimensional: no es una armadura, no se puede poner.
	 * Se guarda para combinarla con las otras piezas y armar el Conjunto. El nombre va en violeta.
	 */
	public static class PiezaConjunto extends DescritoItem {
		public PiezaConjunto(Properties propiedades, java.util.function.Consumer<List<Component>> descripcion) {
			super(propiedades, descripcion);
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(VIOLETA_CONJUNTO);
		}
	}

	/** Los colores de la descripción del diseño de Patricio. */
	private static final int VIOLETA_CONJUNTO = 0xCB79F0, GRIS = 0xC3BCC4, AMARILLO = 0xFCE83B;

	public static void piezaConjunto(List<Component> t) {
		t.add(DescritoItem.parte("Pieza del Conjunto ", GRIS).append(DescritoItem.parte("Interdimensional.", AMARILLO)));
		t.add(Component.empty());
		t.add(DescritoItem.parte("No es una Armadura. Consérvalo", GRIS));
		t.add(DescritoItem.parte("para combinarlo con otras piezas", GRIS));
		t.add(DescritoItem.parte("y crear el Conjunto.", GRIS));
	}

	/** El celeste claro del nombre de la espada. */
	public static final int COLOR = 0xB8E8FF;

	/** La espada: como la de netherita (8 de daño, 1,6 de velocidad). */
	public static class Espada extends SwordItem {
		public Espada(Properties propiedades) {
			super(Tiers.NETHERITE, propiedades.fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.4f)));
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(COLOR);
		}
	}
}
