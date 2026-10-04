package com.dedsafio4.casino;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;

/**
 * Servidor → admin: la tabla de premios del Casino para mostrarla en pantalla al hacer /casino 1
 * (qué da cada figura con 2 y con 3 iguales, cuánto y de qué nivel si viene encantado).
 */
public record TablaPremiosPayload(int tabla, List<Fila> filas) implements CustomPacketPayload {
	/** Una opción de premio: el ítem (para el dibujito) y el texto ("5 Lingotes de hierro"). */
	public record Opcion(ItemStack item, String texto) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opcion> CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC, Opcion::item,
				ByteBufCodecs.STRING_UTF8, Opcion::texto,
				Opcion::new);
	}

	/** Una figura de las ruedas, con las opciones del premio chico (2 iguales) y del grande (3 iguales). */
	public record Fila(ItemStack icono, String nombre, List<Opcion> dos, List<Opcion> tres) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Fila> CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC, Fila::icono,
				ByteBufCodecs.STRING_UTF8, Fila::nombre,
				Opcion.CODEC.apply(ByteBufCodecs.list()), Fila::dos,
				Opcion.CODEC.apply(ByteBufCodecs.list()), Fila::tres,
				Fila::new);
	}

	public static final Type<TablaPremiosPayload> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "casino_tabla"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TablaPremiosPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TablaPremiosPayload::tabla,
			Fila.CODEC.apply(ByteBufCodecs.list()), TablaPremiosPayload::filas,
			TablaPremiosPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static Opcion op(ItemStack item, String texto) {
		return new Opcion(item, texto);
	}

	private static Opcion op(net.minecraft.world.item.Item item, int cantidad, String texto) {
		return new Opcion(new ItemStack(item, cantidad), cantidad + " " + texto);
	}

	/**
	 * La tabla 1, tal como la reparte CasinoPremios.dos() y tres().
	 * Si se cambian los premios allá, hay que cambiar también este resumen.
	 */
	public static TablaPremiosPayload tabla1(CasinoPremios.Modo modo) {
		ItemStack libro = new ItemStack(Items.ENCHANTED_BOOK);
		return new TablaPremiosPayload(1, cambiar(modo, List.of(
				new Fila(new ItemStack(Items.IRON_INGOT), "Hierro",
						List.of(op(Items.IRON_INGOT, 5, "Lingotes de hierro"), op(Items.DIAMOND, 1, "Diamante")),
						List.of(op(Items.DIAMOND, 5, "Diamantes"), op(Items.NETHERITE_SCRAP, 1, "Chatarra de netherita"))),
				new Fila(new ItemStack(Items.EXPERIENCE_BOTTLE), "Botella de experiencia",
						List.of(op(Items.EXPERIENCE_BOTTLE, 16, "Botellas de experiencia")),
						List.of(op(Items.EXPERIENCE_BOTTLE, 64, "Botellas de experiencia"))),
				new Fila(new ItemStack(Items.IRON_CHESTPLATE), "Pechera de hierro",
						List.of(op(new ItemStack(Items.DIAMOND_CHESTPLATE), "1 Pieza de armadura de diamante encantada (nivel 1)")),
						List.of(op(new ItemStack(Items.DIAMOND_CHESTPLATE), "1 Pieza de armadura de diamante encantada (nivel 3 a 4)"))),
				new Fila(new ItemStack(Items.IRON_PICKAXE), "Pico de hierro",
						List.of(op(Items.ARROW, 16, "Flechas"), op(Items.SHIELD, 1, "Escudo")),
						List.of(op(new ItemStack(Items.CROSSBOW), "1 Ballesta encantada (nivel 1 a 2)"),
								op(new ItemStack(Items.BOW), "1 Arco encantado (nivel 1 a 2)"),
								op(new ItemStack(Items.DIAMOND_PICKAXE), "1 Pico de diamante encantado (nivel 1 a 2)"))),
				new Fila(libro.copy(), "Libro de encantamientos",
						List.of(op(libro.copy(), "1 Libro encantado (nivel 1 a 2)")),
						List.of(op(libro.copy(), "1 Libro encantado (nivel 3 a 4)"))),
				new Fila(new ItemStack(Items.COOKED_BEEF), "Filete",
						List.of(op(Items.COOKED_BEEF, 16, "Filetes"), op(Items.APPLE, 16, "Manzanas"),
								op(Items.COOKED_PORKCHOP, 16, "Chuletas de cerdo")),
						List.of(op(Items.GOLDEN_CARROT, 10, "Zanahorias de oro"), op(Items.GOLDEN_APPLE, 1, "Manzana de oro"))),
				new Fila(new ItemStack(Items.POTION), "Poción",
						List.of(op(PotionContents.createItemStack(Items.POTION, Potions.HEALING), "1 Poción de curación instantánea"),
								op(PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE), "1 Poción de resistencia al fuego"),
								op(PotionContents.createItemStack(Items.POTION, Potions.STRENGTH), "1 Poción de fuerza")),
						List.of(op(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING), "1 Poción arrojadiza de curación instantánea"),
								op(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE), "1 Poción arrojadiza de resistencia al fuego"),
								op(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRENGTH), "1 Poción arrojadiza de fuerza"))),
				new Fila(new ItemStack(ModItems.CORAZON), "Corazón",
						List.of(),
						List.of(op(ModItems.CORAZON, 1, "Corazón"))))));
	}

	/** Las figuras de la tabla en el orden de las filas (como las llama CasinoPremios). */
	private static final String[] FIGURAS = {"hierro", "botella de experiencia", "pechera de hierro", "pico de hierro",
			"libro de encantamientos", "filete", "poción", "corazon"};

	/** Pone los premios elegidos con /casino premio en lugar de los de siempre (texto vacío: lo arma el cliente). */
	private static List<Fila> cambiar(CasinoPremios.Modo modo, List<Fila> filas) {
		List<Fila> r = new java.util.ArrayList<>();
		for (int i = 0; i < filas.size(); i++) {
			Fila f = filas.get(i);
			List<ItemStack> dos = modo.propio(FIGURAS[i], 2), tres = modo.propio(FIGURAS[i], 3);
			r.add(new Fila(f.icono(), f.nombre(), dos == null ? f.dos() : propias(dos), tres == null ? f.tres() : propias(tres)));
		}
		return r;
	}

	private static List<Opcion> propias(List<ItemStack> items) {
		return items.stream().map(it -> new Opcion(it.copy(), "")).toList();
	}
}
