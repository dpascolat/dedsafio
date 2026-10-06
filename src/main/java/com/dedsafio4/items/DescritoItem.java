package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

/**
 * Un ítem con el nombre en celeste y una descripción debajo, como en las imágenes del usuario.
 * Los colores son los mismos del Huevo Eburia.
 */
public class DescritoItem extends Item {
	public static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6, NARANJA = 0xFFA23C, AMARILLO = 0xF0D86A, ROJO = 0xFF5555;
	public static final int SALMON = 0xFF6B5A, CELESTE_CLARO = 0x9CE5F6, VIOLETA = 0xD98CFF;

	private final Consumer<List<Component>> descripcion;

	public DescritoItem(Properties propiedades, Consumer<List<Component>> descripcion) {
		super(propiedades);
		this.descripcion = descripcion;
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		descripcion.accept(texto);
	}

	public static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	public static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}

	/** Billete de Autobús (la Tarjeta Rosa). */
	public static void billeteAutobus(List<Component> t) {
		t.add(parte("Necesitas uno para", GRIS));
		t.add(parte("viajar en ", GRIS).append(parte("Autobús", 0xC3E84E)).append(parte(".", GRIS)));
	}

	/** Cápsula de Pastilla Vacía. */
	public static void capsulaVacia(List<Component> t) {
		t.add(parte("Utilízala para crear diferentes", GRIS));
		t.add(parte("tipos de Pastillas o Píldoras.", GRIS));
	}

	/** Pimpollo de Qumara. */
	public static void pimpolloQumara(List<Component> t) {
		t.add(parte("Pimpollo de la antigua planta", GRIS));
		t.add(parte("Qumara. Venerada por los", GRIS));
		t.add(parte("habitantes del Centro de Quiu.", GRIS));
	}

	/** Tarjeta de Acceso Phora (la rosa y la verde): abren las puertas de su color. */
	public static void tarjetaAccesoPhora(List<Component> t) {
		t.add(parte("Utilízala para abrir puertas.", GRIS));
	}

	/** Bloque de Dilitio: de él sale el Dilitio; está en los Cubos Phora. */
	public static void bloqueDilitio(List<Component> t) {
		int verde = 0x4AA852, dorado = 0xFFAA00, rosa = 0xE58FE5;
		t.add(Component.empty());
		t.add(parte("Utiliza un ", GRIS).append(parte("Pico de Diamante", dorado)));
		t.add(parte("o superior sobre él para", GRIS));
		t.add(parte("extraer ", GRIS).append(parte("Dilitio", verde)).append(parte(".", GRIS)));
		t.add(Component.empty());
		t.add(parte("➤ ", verde).append(parte("Se obtiene en: ", 0xFFFFFF)).append(parte("Cubos Phora.", rosa)));
	}

	/** Batería de Dilitio: recarga la Linterna. */
	public static void bateriaDilitio(List<Component> t) {
		t.add(parte("Importante fuente de energía", GRIS));
		t.add(parte("creada a partir de ", GRIS).append(parte("Dilitio", 0x4AA852)).append(parte(",", GRIS)));
		t.add(parte("Bioxita", CELESTE).append(parte(" y ", GRIS)).append(parte("Netherita", 0xFFFFFF)).append(parte(".", GRIS)));
	}

	/** Racimo de Dilitio: 9 de Dilitio en la mesa de crafteo. */
	public static void racimoDilitio(List<Component> t) {
		int verde = 0x4AA852, dorado = 0xFFAA00;
		t.add(parte("Combina 9 de ", GRIS).append(parte("Dilitio", verde)).append(parte(" en una ", GRIS)).append(parte("Mesa de", dorado)));
		t.add(parte("Crafteo", dorado).append(parte(" para conseguirlo.", GRIS)));
	}

	/** Excremento: sale del Plumosaurio al darle Fruta Solaria. */
	public static void excremento(List<Component> t) {
		t.add(parte("Dale de comer la ", GRIS).append(parte("Fruta Solaria", CELESTE)).append(parte(" a un", GRIS)));
		t.add(parte("Plumosaurio", ROJO).append(parte(" para tener la ", GRIS)).append(parte("probabilidad", AMARILLO)));
		t.add(parte("de que suelte ", GRIS).append(parte("Excremento", CELESTE)).append(parte(".", GRIS)));
		t.add(Component.empty());
		t.add(parte("Utiliza un ", GRIS).append(parte("Pico", NARANJA)).append(parte(" para extraerlo.", GRIS)));
	}

	/** Escupitajo de Dáctylo: lo sueltan los Dáctylos. */
	public static void escupitajoDactylo(List<Component> t) {
		t.add(parte("Consíguelo al Eliminar:", GRIS));
		t.add(Component.empty());
		t.add(parte("◆ Dáctylo Bebé", SALMON));
		t.add(parte("◆ Dáctylo Adulto", SALMON));
	}

	/** Fibra Glebanoide: se hace con Tejido Glebanoide Profundo y Verruga Oscura Glebanoide. */
	public static void fibraGlebanoide(List<Component> t) {
		t.add(parte("Combina ", GRIS).append(parte("Tejido Glebanoide", CELESTE)));
		t.add(parte("Profundo", CELESTE).append(parte(" y ", GRIS)).append(parte("Verruga Oscura", CELESTE)));
		t.add(parte("Glebanoide", CELESTE).append(parte(" para conseguir.", GRIS)));
	}

	/** Pelos Glebanoides (el pasto de la Dimensión de los Órganos): con una espada se consigue Pelo Glebanoide. */
	public static void pelosGlebanoides(List<Component> t) {
		t.add(parte("Utiliza una ", GRIS).append(parte("Espada", VIOLETA)));
		t.add(parte("para conseguir ", GRIS).append(parte("Pelo", CELESTE)));
		t.add(parte("Glebanoide", CELESTE).append(parte(".", GRIS)));
	}

	/** Dentadura Glebanoide (bloque de dientes): con una espada se consigue Diente Glebanoide. */
	public static void dentaduraGlebanoide(List<Component> t) {
		t.add(parte("Utiliza una ", GRIS).append(parte("Espada", VIOLETA)));
		t.add(parte("para conseguir ", GRIS).append(parte("Diente", CELESTE)));
		t.add(parte("Glebanoide", CELESTE).append(parte(".", GRIS)));
	}

	/** Verruga Clara y Oscura Glebanoide (ids bloque_carne_rosa y bloque_carne): se sacan con espada; al romperlas sale un Parásito Volador. */
	public static void verrugaClaraGlebanoide(List<Component> t) {
		t.add(parte("Utiliza una ", GRIS).append(parte("Espada", VIOLETA)).append(parte(" para", GRIS)));
		t.add(parte("conseguirla.", GRIS));
		t.add(Component.empty());
		t.add(parte("Combínalo con otros ingredientes", GRIS));
		t.add(parte("para conseguir nuevos objetos.", GRIS));
		t.add(Component.empty());
		t.add(parte("⚠ Atención: Al romperla saldrá", AMARILLO));
		t.add(parte("un ", AMARILLO).append(parte("Parásito Volador", ROJO)).append(parte(".", AMARILLO)));
	}

	/** Carne Glebanoide: saca la Levitación, da Caída Lenta 1 segundo y cocinada da Oro en Bruto. */
	public static void carneGlebanoide(List<Component> t) {
		t.add(parte("Caída lenta", 0x5555FF));
		t.add(Component.empty());
		t.add(parte("Combina ", GRIS).append(parte("Grasa", CELESTE)).append(parte(" y ", GRIS)).append(parte("Fibra", CELESTE)));
		t.add(parte("Glebanoide", CELESTE).append(parte(" para conseguir ", GRIS)).append(parte("4", AMARILLO)).append(parte(".", GRIS)));
		t.add(Component.empty());
		t.add(parte("Consúmelo para ", GRIS).append(parte("eliminar", VIOLETA)).append(parte(" el", GRIS)));
		t.add(parte("efecto de Levitación", GRIS));
		t.add(Component.empty());
		t.add(parte("Aplica ", GRIS).append(parte("Caída Lenta", VIOLETA)).append(parte(" durante", GRIS)));
		t.add(parte("1 segundo", AMARILLO).append(parte(". Utilízala con cuidado.", GRIS)));
		t.add(Component.empty());
		t.add(parte("Si la cocinas, se convierte en", GRIS));
		t.add(parte("Oro en Bruto", CELESTE).append(parte(".", GRIS)));
	}

	/** Diente Glebanoide: sale de romper una Dentadura Glebanoide con espada (y lo suelta el Parásito). */
	public static void dienteGlebanoide(List<Component> t) {
		t.add(parte("Utiliza una ", GRIS).append(parte("Espada", VIOLETA)).append(parte(" sobre una", GRIS)));
		t.add(parte("Dentadura Glebanoide", CELESTE).append(parte(" para obtenerlo.", GRIS)));
		t.add(Component.empty());
		t.add(parte("Combínalo con otros ingredientes", GRIS));
		t.add(parte("para conseguir nuevos objetos.", GRIS));
		t.add(Component.empty());
		t.add(parte("Consíguelo al Eliminar:", GRIS));
		t.add(Component.empty());
		t.add(parte("◆ Parásito", SALMON));
	}

	/** Caparazón: lo sueltan los Nautilus Óseos. */
	public static void caparazon(List<Component> t) {
		t.add(parte("Combínalo con otros ingredientes", GRIS));
		t.add(parte("para conseguir nuevos objetos.", GRIS));
		t.add(Component.empty());
		t.add(parte("Consíguelo al Eliminar:", GRIS));
		t.add(parte("◆ Nautilus Óseo Pequeño", SALMON));
		t.add(parte("◆ Nautilus Óseo Grande", SALMON));
	}

	/** Cuerno de Narval (id jeringa): lo sueltan el Narval y el Narval con Reptisaurio. */
	public static void cuernoNarval(List<Component> t) {
		t.add(parte("Consíguelo al Eliminar:", GRIS));
		t.add(Component.empty());
		t.add(parte("◆ Narval", SALMON));
		t.add(parte("◆ Narval con Reptisaurio", SALMON));
	}

	/** Cuero Glebanoide: lo sueltan el Gusano de Carne y el Parásito Volador. */
	public static void cueroGlebanoide(List<Component> t) {
		t.add(parte("Combínalo con otros ingredientes", GRIS));
		t.add(parte("para conseguir nuevos objetos.", GRIS));
		t.add(Component.empty());
		t.add(parte("Consíguelo al Eliminar:", GRIS));
		t.add(Component.empty());
		t.add(parte("◆ Gusano de Carne", SALMON));
		t.add(parte("◆ Parásito Volador", SALMON));
	}

	/** Sangre de Reptisaurio: la sueltan los Reptisaurios. */
	public static void sangreReptisaurio(List<Component> t) {
		t.add(parte("Sangre de las criaturas", GRIS));
		t.add(parte("salvajes del Centro de", GRIS));
		t.add(parte("Quiu.", GRIS));
		t.add(Component.empty());
		t.add(parte("Consíguelo al Eliminar:", GRIS));
		t.add(Component.empty());
		t.add(parte("◆ Reptisaurios", SALMON));
	}

	/** Insecto: a veces lo sueltan los Dromoraptores. */
	public static void insecto(List<Component> t) {
		t.add(parte("Diminuto insecto atrapado", GRIS));
		t.add(parte("en las Criaturas del Centro", GRIS));
		t.add(parte("de Quiu.", GRIS));
		t.add(Component.empty());
		t.add(parte("Probabilidad", AMARILLO).append(parte(" de conseguirlo", GRIS)));
		t.add(parte("al eliminar:", GRIS));
		t.add(Component.empty());
		t.add(parte("◆ ", CELESTE_CLARO).append(parte("Dromoraptores", SALMON)));
	}

	/** Fruta Solaria: se la das al Plumosaurio; con un hacha sale la semilla. */
	public static void frutaSolaria(List<Component> t) {
		t.add(parte("Encuéntralo y coséchalo en", GRIS));
		t.add(parte("los árboles del Centro de Quiu.", GRIS));
		t.add(Component.empty());
		t.add(parte("Dale de comer la ", GRIS).append(parte("Fruta", CELESTE)).append(parte(" a un", GRIS)));
		t.add(parte("Plumosaurio", ROJO).append(parte(" para tener la", GRIS)));
		t.add(parte("probabilidad", AMARILLO).append(parte(" de que suelte", GRIS)));
		t.add(parte("Excremento", CELESTE).append(parte(".", GRIS)));
		t.add(Component.empty());
		t.add(parte("⚠ Atención: ", AMARILLO).append(parte("Al alcanzar su punto", GRIS)));
		t.add(parte("máximo de crecimiento debes", GRIS));
		t.add(parte("recogerlo lo antes posible.", GRIS));
		t.add(parte("Después de un tiempo se pudrirá", GRIS));
		t.add(parte("si no lo cosechas.", GRIS));
		t.add(Component.empty());
		t.add(parte("Utiliza un ", GRIS).append(parte("Hacha", NARANJA)).append(parte(" mientras tienes", GRIS)));
		t.add(parte("la ", GRIS).append(parte("Fruta", CELESTE)).append(parte(" en la ", GRIS)).append(parte("Mano Secundaria", NARANJA)));
		t.add(parte("para extraer la ", GRIS).append(parte("Semilla", CELESTE)).append(parte(".", GRIS)));
	}

	/** Semilla de Fruta Solaria: sale de la fruta con un hacha. */
	public static void semillaSolaria(List<Component> t) {
		t.add(parte("Puedes plantarla en ", GRIS).append(parte("Troncos", CELESTE)));
		t.add(parte("de Abedúl", CELESTE).append(parte(" y ", GRIS)).append(parte("Roble de Quiu", CELESTE)));
		t.add(parte("para hacerla crecer.", GRIS));
		t.add(Component.empty());
		t.add(parte("⚠ Atención: ", AMARILLO).append(parte("Al alcanzar su punto", GRIS)));
		t.add(parte("máximo de crecimiento debes", GRIS));
		t.add(parte("recogerlo lo antes posible.", GRIS));
		t.add(parte("Después de un tiempo se pudrirá", GRIS));
		t.add(parte("si no lo cosechas.", GRIS));
		t.add(Component.empty());
		t.add(parte("Utiliza un ", GRIS).append(parte("Hacha", NARANJA)).append(parte(" mientras tienes", GRIS)));
		t.add(parte("la ", GRIS).append(parte("Fruta Solaria", CELESTE)).append(parte(" en la ", GRIS)).append(parte("Mano", NARANJA)));
		t.add(parte("Secundaria", NARANJA).append(parte(" para extraerla.", GRIS)));
	}

	/** Baya Uvina: fruta de los árboles; con un hacha sale la semilla. */
	public static void bayaUvina(List<Component> t) {
		t.add(parte("Encuéntralo y coséchalo en", GRIS));
		t.add(parte("los árboles del Centro de Quiu.", GRIS));
		t.add(Component.empty());
		t.add(parte("⚠ Atención: ", AMARILLO).append(parte("Al alcanzar su punto", GRIS)));
		t.add(parte("máximo de crecimiento debes", GRIS));
		t.add(parte("recogerlo lo antes posible.", GRIS));
		t.add(parte("Después de un tiempo se pudrirá", GRIS));
		t.add(parte("si no lo cosechas.", GRIS));
		t.add(Component.empty());
		t.add(parte("Utiliza un ", GRIS).append(parte("Hacha", NARANJA)).append(parte(" mientras tienes", GRIS)));
		t.add(parte("la ", GRIS).append(parte("Baya", CELESTE)).append(parte(" en la ", GRIS)).append(parte("Mano Secundaria", NARANJA)));
		t.add(parte("para extraer la ", GRIS).append(parte("Semilla", CELESTE)).append(parte(".", GRIS)));
	}

	/** Semilla de Baya Uvina: sale de la baya con un hacha. */
	public static void semillaUvina(List<Component> t) {
		t.add(parte("Puedes plantarla en ", GRIS).append(parte("Troncos", CELESTE)));
		t.add(parte("de Abedúl", CELESTE).append(parte(" y ", GRIS)).append(parte("Roble de Quiu", CELESTE)));
		t.add(parte("para hacerla crecer.", GRIS));
		t.add(Component.empty());
		t.add(parte("⚠ Atención: ", AMARILLO).append(parte("Al alcanzar su punto", GRIS)));
		t.add(parte("máximo de crecimiento debes", GRIS));
		t.add(parte("recogerlo lo antes posible.", GRIS));
		t.add(parte("Después de un tiempo se pudrirá", GRIS));
		t.add(parte("si no lo cosechas.", GRIS));
		t.add(Component.empty());
		t.add(parte("Utiliza un ", GRIS).append(parte("Hacha", NARANJA)).append(parte(" mientras tienes", GRIS)));
		t.add(parte("la ", GRIS).append(parte("Baya Uvina", CELESTE)).append(parte(" en la ", GRIS)).append(parte("Mano", NARANJA)));
		t.add(parte("Secundaria", NARANJA).append(parte(" para extraerla.", GRIS)));
	}

	/** Pegamento Primitivo: un material para combinar con otros. */
	public static void pegamentoPrimitivo(List<Component> t) {
		t.add(parte("Fuerte pegamento utilizado", GRIS));
		t.add(parte("por las criaturas del Centro", GRIS));
		t.add(parte("de Quiu.", GRIS));
		t.add(Component.empty());
		t.add(parte("Combínalo con otros materiales", GRIS));
		t.add(parte("para conseguir nuevos objetos.", GRIS));
	}
}
