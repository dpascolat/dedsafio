package com.dedsafio4.nave;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Las 4 partes de la nave como ítems (cabina, motor, alerón izquierdo y alerón derecho) y la nave entera. */
public final class PartesNave {
	private PartesNave() {}

	private static final int VIOLETA = 0xC883FF;

	public static final ParteNaveItem CABINA = registrar("nave_cabina");
	public static final ParteNaveItem MOTOR = registrar("nave_motor");
	public static final ParteNaveItem ALERON_IZQUIERDO = registrar("nave_aleron_izquierdo");
	public static final ParteNaveItem ALERON_DERECHO = registrar("nave_aleron_derecho");
	/** La nave entera (por ahora solo el ítem con su descripción). */
	public static final ParteNaveItem NAVE_BIPLAZA = Registry.register(BuiltInRegistries.ITEM,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "nave_biplaza"),
			new ParteNaveItem("nave_biplaza", new Item.Properties().stacksTo(1), VIOLETA, PartesNave::descripcionBiplaza));

	private static ParteNaveItem registrar(String nombre) {
		return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre),
				new ParteNaveItem(nombre, new Item.Properties()));
	}

	/** Nave Espacial Biplaza, como en la imagen del usuario. */
	private static void descripcionBiplaza(java.util.List<net.minecraft.network.chat.Component> t) {
		int gris = com.dedsafio4.items.DescritoItem.GRIS, celeste = com.dedsafio4.items.DescritoItem.CELESTE;
		int amarillo = com.dedsafio4.items.DescritoItem.AMARILLO, naranja = com.dedsafio4.items.DescritoItem.NARANJA;
		t.add(com.dedsafio4.items.DescritoItem.parte("Requiere una ", gris)
				.append(com.dedsafio4.items.DescritoItem.parte("Plataforma de", celeste)));
		t.add(com.dedsafio4.items.DescritoItem.parte("Despegue", celeste)
				.append(com.dedsafio4.items.DescritoItem.parte(". Colócala en ella", gris)));
		t.add(com.dedsafio4.items.DescritoItem.parte("para subirte.", gris));
		t.add(net.minecraft.network.chat.Component.empty());
		t.add(com.dedsafio4.items.DescritoItem.parte("⚠ Atención: La Plataforma sólo", amarillo));
		t.add(com.dedsafio4.items.DescritoItem.parte("funciona en el Overworld.", amarillo));
		t.add(net.minecraft.network.chat.Component.empty());
		t.add(com.dedsafio4.items.DescritoItem.parte("Las naves tienen dueño. Haz", gris));
		t.add(com.dedsafio4.items.DescritoItem.parte("Shift + Click Derecho", naranja)
				.append(com.dedsafio4.items.DescritoItem.parte(" para", gris)));
		t.add(com.dedsafio4.items.DescritoItem.parte("guardarla en tu inventario.", gris));
	}

	/** Solo para que se carguen los registros al arrancar. */
	public static void registrar() {}
}
