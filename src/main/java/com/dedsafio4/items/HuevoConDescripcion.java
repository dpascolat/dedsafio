package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

/**
 * Huevo generador que muestra la descripción del mob, con el formato de las imágenes del usuario:
 * la historia, el drop y dónde aparece.
 */
public class HuevoConDescripcion extends SpawnEggItem {
	public static final int GRIS = 0xC6C6C6, VIOLETA = 0xC864E0, CELESTE = 0x5CC8E8, DROP = 0x8FC4B0;
	public static final int OBJETO = 0x88CECA, PORCENTAJE = 0xE9D063, APARECE = 0xE0609F, BLANCO = 0xFAFAFA;

	private final Consumer<List<Component>> descripcion;

	public HuevoConDescripcion(EntityType<? extends Mob> tipo, int fondo, int manchas, Properties propiedades,
							   Consumer<List<Component>> descripcion) {
		super(tipo, fondo, manchas, propiedades);
		this.descripcion = descripcion;
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		descripcion.accept(texto);
	}

	public static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
	}

	/** El Plumosaurio (Walker). */
	public static void dromoraptorRojo(List<Component> t) {
		t.add(parte("Depredador que posee una fuerza brutal.", GRIS));
		t.add(parte("Se desplaza en manada.", GRIS));
	}

	public static void creeperNuclear(List<Component> t) {
		t.add(parte("Provoca grandes explosiones.", GRIS));
		t.add(Component.empty());
		t.add(parte("❄ Aparece en: ", APARECE).append(parte("la Nieve.", BLANCO)));
	}

	public static void narval(List<Component> t) {
		t.add(parte("Habita los acuíferos del Centro de Quiu.", GRIS));
		t.add(parte("Si detecta intrusos embestirá a gran", GRIS));
		t.add(parte("velocidad, con la probabilidad de", GRIS));
		t.add(parte("empujarte y jugar contigo.", GRIS));
		t.add(Component.empty());
		t.add(parte("🌴 Aparece en: ", APARECE).append(parte("Acuíferos del Centro", BLANCO)));
		t.add(parte("de Quiu.", BLANCO));
	}

	public static void plumosaurio(List<Component> t) {
		t.add(parte("El coloso herbívoro más grande del", GRIS));
		t.add(parte("núcleo de Quiu. ", GRIS).append(parte("Al alimentarlo con una", VIOLETA)));
		t.add(parte("Fruta Solaria", CELESTE).append(parte(", procesará el alimento al", GRIS)));
		t.add(parte("instante, generando ", GRIS).append(parte("Excremento", CELESTE)).append(parte(".", GRIS)));
		t.add(Component.empty());
		t.add(parte("El Plumosaurio es pacífico, pero si te", GRIS));
		t.add(parte("chocas con él o lo golpeas te hará", GRIS));
		t.add(parte("MUCHO daño.", GRIS));
		drop(t, "Manzana de Ámbar");
		t.add(Component.empty());
		t.add(parte("🌴 Aparece en: ", APARECE).append(parte("Jungla del Centro", BLANCO)));
		t.add(parte("de Quiu.", BLANCO));
	}

	/** El Reptisaurio Salvaje (Mira). */
	public static void reptisaurioSalvaje(List<Component> t) {
		t.add(parte("Criatura pequeña que ataca directamente", GRIS));
		t.add(parte("a quien se le acerque. ", GRIS).append(parte("Si te acercas", VIOLETA)));
		t.add(parte("demasiado, se entierra y te acecha", VIOLETA));
		t.add(parte("hasta encontrarte, saliendo de nuevo", VIOLETA));
		t.add(parte("para atacar.", VIOLETA));
		drop(t, "Sangre de Reptisaurio");
		t.add(Component.empty());
		t.add(parte("🌴 Aparece en: ", APARECE).append(parte("Cuevas del", BLANCO)));
		t.add(parte("Centro de Quiu", BLANCO));
	}

	private static void drop(List<Component> t, String objeto) {
		t.add(Component.empty());
		t.add(parte("🎁 Drop:", DROP));
		t.add(Component.empty());
		t.add(parte("✦ " + objeto + " ", OBJETO).append(parte("(100%)", PORCENTAJE)));
	}
}
