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

	/** La descripción del mob (la usa también el cartel de /mob). */
	public void describir(List<Component> texto) {
		descripcion.accept(texto);
	}

	public static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
	}

	/** El Plumosaurio (Walker). */
	public static void dromoraptorRojo(List<Component> t) {
		t.add(parte("Depredador que posee una fuerza brutal.", GRIS));
		t.add(parte("Se desplaza en manada.", GRIS));
		t.add(Component.empty());
		t.add(parte("🌴 Aparece en: ", APARECE).append(parte("Sabana del Centro", BLANCO)));
		t.add(parte("de Quiu.", BLANCO));
	}

	public static void dromoraptorAzul(List<Component> t) {
		t.add(parte("Se ha visto una nueva criatura en el", GRIS));
		t.add(parte("Centro de Quiu: ", GRIS).append(parte("Dromoraptor Azul", CELESTE)).append(parte(".", GRIS)));
		t.add(parte("Depredador extremadamente veloz", GRIS));
		t.add(parte("que se desplaza en manada.", GRIS));
		t.add(Component.empty());
		t.add(parte("🌴 Aparece en: ", APARECE).append(parte("Sabana del Centro", BLANCO)));
		t.add(parte("de Quiu.", BLANCO));
	}

	public static void creeperPastel(List<Component> t) {
		t.add(parte("Al explotar mancha la pantalla con", GRIS));
		t.add(parte("pastel por algunos segundos.", GRIS));
		t.add(Component.empty());
		t.add(parte("🌍 Aparece en: ", APARECE).append(parte("todo el Overworld.", BLANCO)));
	}

	public static void zarinosa(List<Component> t) {
		t.add(parte("Criatura pequeña pero astuta que", GRIS));
		t.add(parte("aparece en las Cuevas del Centro de", GRIS));
		t.add(parte("Quiu. ", GRIS).append(parte("Se abalanza sobre tu cabeza y", VIOLETA)));
		t.add(parte("drena tu vida hasta matarte.", VIOLETA));
		t.add(parte("Sólo puedes librarte con la ayuda", GRIS));
		t.add(parte("de otra persona.", GRIS));
		t.add(Component.empty());
		t.add(parte("🌴 Aparece en: ", APARECE).append(parte("Cuevas del", BLANCO)));
		t.add(parte("Centro de Quiu", BLANCO));
	}

	public static void creeperAzalea(List<Component> t) {
		t.add(parte("Al explotar, te atrapará con sus raíces.", GRIS));
		t.add(parte("Debes ser rápido para librarte de ellas.", VIOLETA));
		t.add(Component.empty());
		t.add(parte("🏰 Aparece en: ", APARECE).append(parte("una mazmorra.", BLANCO)));
	}

	public static void gusanoCarne(List<Component> t) {
		t.add(parte("Criatura pequeña y lenta que ataca", GRIS));
		t.add(parte("directamente a quien se le acerque.", GRIS));
		t.add(Component.empty());
		t.add(parte("🎁 Drop:", DROP));
		t.add(Component.empty());
		t.add(parte("✦ Cuero Glebanoide ", OBJETO).append(parte("(20%)", PORCENTAJE)));
		t.add(Component.empty());
		t.add(parte("♨ Aparece en: ", APARECE).append(parte("Gleba (al romper", BLANCO)));
		t.add(parte("Bloques de Grasa Líquida o Coagulada)", BLANCO));
	}

	public static void flashbang(List<Component> t) {
		t.add(parte("Aparece en la oscuridad.", GRIS));
		t.add(parte("Explota al instante si te toca: ", GRIS));
		t.add(parte("4 corazones", VIOLETA).append(parte(" aunque tengas armadura,", GRIS)));
		t.add(parte("y te deja ciego unos segundos.", GRIS));
	}

	public static void wraith(List<Component> t) {
		t.add(parte("Espectro con guadaña que aparece en la oscuridad.", GRIS));
		t.add(parte("Si te golpea, ", GRIS).append(parte("te arrastra al Limbo", VIOLETA)).append(parte(".", GRIS)));
	}

	public static void almita(List<Component> t) {
		t.add(parte("El alma de un jugador. No ataca:", GRIS));
		t.add(parte("solo flota rápido por ahí.", GRIS));
		t.add(parte("/almita <nombre>", CELESTE).append(parte(" le pone el nombre.", GRIS)));
	}

	public static void fantasmaAmarillo(List<Component> t) {
		t.add(parte("Fantasma volador del Limbo.", GRIS));
		t.add(parte("Si te golpea, ", GRIS).append(parte("te mata al instante", VIOLETA)));
		t.add(parte("y desaparece. Solo te salva un tótem.", GRIS));
	}

	public static void fantasmaBlanco(List<Component> t) {
		t.add(parte("Golpea a gran velocidad y aplica ", GRIS).append(parte("Lentitud", VIOLETA)).append(parte(".", GRIS)));
	}

	public static void fantasmaNegro(List<Component> t) {
		t.add(parte("Al golpearte te quitará un ", GRIS).append(parte("Corazón Permanente", VIOLETA)).append(parte(".", GRIS)));
	}

	public static void fantasmaRojo(List<Component> t) {
		t.add(parte("Entidad voladora del Limbo que ataca", GRIS));
		t.add(parte("a gran velocidad.", GRIS));
	}

	public static void garrapataCerebral(List<Component> t) {
		t.add(parte("Criatura que vaga por Gleba", GRIS));
		t.add(parte("buscando a su próxima víctima.", GRIS));
		t.add(parte("Si detecta una presencia", GRIS));
		t.add(parte("desconocida cerca, ", GRIS).append(parte("atacará", VIOLETA)));
		t.add(parte("de inmediato", VIOLETA).append(parte(". Puede escalar.", GRIS)));
		t.add(Component.empty());
		t.add(parte("🎁 Drop:", DROP));
		t.add(Component.empty());
		t.add(parte("✦ Cerebro Glebanoide ", OBJETO).append(parte("(20%)", PORCENTAJE)));
		t.add(Component.empty());
		t.add(parte("♨ Aparece en: ", APARECE).append(parte("Gleba", BLANCO)));
	}

	public static void cerebroAmarillo(List<Component> t) {
		t.add(parte("Se abalanza sobre tu cabeza,", GRIS));
		t.add(parte("cegándote y drenando tu vida", GRIS));
		t.add(parte("hasta matarte. ", GRIS).append(parte("Necesitas ayuda", VIOLETA)));
		t.add(parte("de otra persona para quitártelo.", VIOLETA));
	}

	public static void nautilusOseo(List<Component> t) {
		t.add(parte("Criatura voladora que atacará si", GRIS));
		t.add(parte("te acercas. No puedes empujarla.", GRIS));
		t.add(parte("Aplica ", GRIS).append(parte("Levitación", VIOLETA)).append(parte(".", GRIS)));
		t.add(parte("Son MUY peligrosos en grupo.", GRIS));
	}

	public static void zombiePlata(List<Component> t) {
		t.add(parte("Zombie consumido por Qumara.", GRIS));
		t.add(parte("Muy rápido y fuerte. ", GRIS).append(parte("Puede escalar", VIOLETA)));
		t.add(parte("paredes.", VIOLETA));
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
