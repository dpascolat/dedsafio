package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.hermandad.ManuscritoDatos;
import com.dedsafio4.nave.ModEntidades;
import com.dedsafio4.pociones.ModPociones;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;

public final class ModItems {
	private ModItems() {}

	public static final Item MANUSCRITO_HERMANDAD = registrar("manuscrito_hermandad",
			new ManuscritoHermandadItem(new Item.Properties().stacksTo(1), ManuscritoDatos.MINIMO_INSCRITOS, false));

	/** Igual al normal pero alcanza con 1 inscrito, para que un admin pueda probar solo. */
	public static final Item MANUSCRITO_HERMANDAD_ADMIN = registrar("manuscrito_hermandad_admin",
			new ManuscritoHermandadItem(new Item.Properties().stacksTo(1), 1, true));

	public static final List<Item> MANUSCRITOS = List.of(MANUSCRITO_HERMANDAD, MANUSCRITO_HERMANDAD_ADMIN);

	// Deditas (se apilan de a 99).
	public static final Item DEDITA = registrar("dedita",
			new Item(new Item.Properties().stacksTo(99).rarity(Rarity.COMMON)));
	public static final Item DEDITA_VERDE = registrar("dedita_verde",
			new Item(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));
	public static final Item DEDITA_ROJA = registrar("dedita_roja",
			new Item(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));
	public static final Item DEDITA_MERCADO_NEGRO = registrar("dedita_mercado_negro",
			new Item(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));
	public static final Item DEDITA_CASINO = registrar("dedita_casino",
			new Item(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));
	/** Dedita de Misión (la del dedsafio3: la celeste en azul oscuro). */
	public static final Item DEDITA_MISION = registrar("dedita_mision",
			new Item(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));

	/** Huevo generador de la nave: negro con manchas verdes, como el casco. */
	public static final Item NAVE_SPAWN_EGG = registrar("nave_spawn_egg",
			new SpawnEggItem(ModEntidades.NAVE, 0x0F1214, 0x1F9E66, new Item.Properties()));
	public static final Item NAVE_VELOZ_SPAWN_EGG = registrar("nave_veloz_spawn_egg",
			new SpawnEggItem(ModEntidades.NAVE_VELOZ, 0x26282C, 0xF2C230, new Item.Properties()));

	/** Martillo de Netherite: repara durabilidad a cambio de experiencia (casi no sirve para pelear). */
	public static final Item MARTILLO_NETHERITE = registrar("martillo_netherite",
			new MartilloItem(new Item.Properties().fireResistant().rarity(Rarity.RARE)
					.durability(2031)
					.attributes(ItemAttributeModifiers.builder()
							// En la mano principal: 1 de daño por golpe y 1 de velocidad de ataque.
							.add(Attributes.ATTACK_DAMAGE,
									new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0.0, AttributeModifier.Operation.ADD_VALUE),
									EquipmentSlotGroup.MAINHAND)
							.add(Attributes.ATTACK_SPEED,
									new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
									EquipmentSlotGroup.MAINHAND)
							.build())));

	// Cosas de los Reptisaurios.
	/** Lo que sueltan los Reptisaurios al morir. */
	public static final Item SANGRE_REPTISAURIO = registrar("sangre_reptisaurio",
			new DescritoItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), DescritoItem::sangreReptisaurio));

	/** Saca el Veneno Primitivo. */
	public static final Item ANTIDOTO_PRIMITIVO = registrar("antidoto_primitivo",
			new AntidotoItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE)));

	/** Puesta en la cabeza, evita y saca el Veneno Primitivo. */
	public static final Item MASCARA_ANTI_ESPORAS = registrar("mascara_anti_esporas",
			new ArmorItem(com.dedsafio4.reptisaurios.MaterialMascara.MATERIAL, ArmorItem.Type.HELMET,
					new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15)).rarity(Rarity.UNCOMMON)));

	/** Chip Phora: por ahora solo el ítem; los Cofres Protegidos y los Cubos Phora vienen después. */
	public static final Item CHIP_PHORA = registrar("chip_phora",
			new ChipPhoraItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

	/** Llave de Cobre: activa al Aldeano Phora dormido (y se gasta). */
	public static final Item LLAVE = registrar("llave", new LlaveItem(new Item.Properties().stacksTo(1)));

	/** Tela Primitiva: tejido de extrema resistencia, de los Huevos Eburia. */
	public static final Item TELA_PRIMITIVA = registrar("tela_primitiva", new TelaPrimitivaItem(new Item.Properties()));

	/** Cuerda Resistente: fibra de alta resistencia y durabilidad extrema. */
	public static final Item CUERDA_RESISTENTE = registrar("cuerda_resistente", new CuerdaResistenteItem(new Item.Properties()));

	/** Excremento: se saca con pico del montón que deja el Plumosaurio. */
	public static final Item EXCREMENTO = registrar("excremento",
			new DescritoItem(new Item.Properties(), DescritoItem::excremento));

	/** Escupitajo de Dáctylo: lo sueltan los Dáctylos (bebé y adulto). */
	public static final Item ESCUPITAJO_DACTYLO = registrar("escupitajo_dactylo",
			new DescritoItem(new Item.Properties(), DescritoItem::escupitajoDactylo));

	/** Insecto: a veces lo sueltan los Dromoraptores. */
	public static final Item INSECTO = registrar("insecto", new DescritoItem(new Item.Properties(), DescritoItem::insecto));

	/** Pegamento Primitivo: material de las criaturas del Centro de Quiu. */
	public static final Item PEGAMENTO_PRIMITIVO = registrar("pegamento_primitivo",
			new DescritoItem(new Item.Properties(), DescritoItem::pegamentoPrimitivo));

	/** Bolsa Primitiva (id bolsa_de_tela): lleva lo mismo que un cofre simple. */
	public static final Item BOLSA_DE_TELA = registrar("bolsa_de_tela", new BolsaItem(new Item.Properties().stacksTo(1)));

	/** Semilla de Fruta Solaria: se planta en el costado de los troncos del Centro de Quiu. */
	public static final Item SEMILLA_SOLARIA = registrar("semilla_solaria",
			new SemillaDescritaItem(com.dedsafio4.bloques.ModBloques.FRUTA_SOLARIA, new Item.Properties(),
					DescritoItem::semillaSolaria));

	/** Baya Uvina: fruta morada de los árboles del Centro de Quiu. */
	public static final Item BAYA_UVINA = registrar("baya_uvina", new DescritoItem(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build()),
			DescritoItem::bayaUvina));

	/** Semilla de Baya Uvina: se planta en el costado de los troncos del Centro de Quiu. */
	public static final Item SEMILLA_UVINA = registrar("semilla_uvina",
			new SemillaDescritaItem(com.dedsafio4.bloques.ModBloques.BAYA_UVINA, new Item.Properties(), DescritoItem::semillaUvina));

	/** Fruta del Espacio entre Dimensiones: al comerla te teletransporta cerca, al azar. */
	public static final Item FRUTA_ESPACIO = registrar("fruta_espacio", new FrutaEspacioItem(new Item.Properties()
			.food(net.minecraft.world.food.Foods.CHORUS_FRUIT)));

	/** Fruta Solaria: se la das al Plumosaurio y la procesa al instante. */
	public static final Item FRUTA_SOLARIA = registrar("fruta_solaria", new DescritoItem(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()),
			DescritoItem::frutaSolaria));

	/** Manzana de Ámbar: la suelta el Plumosaurio. */
	public static final Item MANZANA_DE_AMBAR = registrar("manzana_de_ambar", new Item(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build())));

	/** Llave de Candado: le saca el candado a un cofre tuyo. */
	public static final Item LLAVE_CANDADO = registrar("llave_candado", new LlaveCandadoItem(new Item.Properties().stacksTo(1)));

	/** Píldora: contrarresta la Zombificación durante 24 horas. */
	public static final Item PILDORA = registrar("pildora", new PildoraItem(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(0).saturationModifier(0f)
					.alwaysEdible().fast().build())));

	/** Jeringa (por ahora solo el ítem). */
	public static final Item JERINGA = registrar("jeringa", new Item(new Item.Properties().stacksTo(16)));

	/** Cápsula de Pastilla Vacía: para crear diferentes tipos de Pastillas o Píldoras. */
	public static final Item PILDORA_VACIA = registrar("pildora_vacia", new DescritoItem(new Item.Properties(), DescritoItem::capsulaVacia));

	// Ítems nuevos con nombre provisorio (todavía sin nombre ni función definidos).
	public static final Item CUCHARA_MADERA = registrar("cuchara_madera", new Item(new Item.Properties().stacksTo(1)));
	public static final Item CUCHARA_HOJAS = registrar("cuchara_hojas", new Item(new Item.Properties().stacksTo(1)));
	public static final Item TENEDOR_HOJAS = registrar("tenedor_hojas", new Item(new Item.Properties().stacksTo(1)));
	public static final Item CUCHARA_DORADA_HOJAS = registrar("cuchara_dorada_hojas", new Item(new Item.Properties().stacksTo(1)));
	/** Billete de Autobús (el ítem se llama "tarjeta_rosa" desde antes): hace falta para viajar en Autobús. */
	public static final Item TARJETA_ROSA = registrar("tarjeta_rosa", new DescritoItem(new Item.Properties(), DescritoItem::billeteAutobus));
	/** Tarjeta de Acceso Phora rosa: abre la Puerta Rosa (y se gasta). Es distinta de la Tarjeta Rosa de arriba. */
	public static final Item TARJETA_PUERTA_ROSA = registrar("tarjeta_puerta_rosa",
			new DescritoItem(new Item.Properties(), DescritoItem::tarjetaAccesoPhora));
	/** Tarjeta de Acceso Phora verde: abre la Puerta Verde (y se gasta). */
	public static final Item TARJETA_PUERTA_VERDE = registrar("tarjeta_puerta_verde",
			new DescritoItem(new Item.Properties(), DescritoItem::tarjetaAccesoPhora));
	public static final Item AMULETO_VERDE = registrar("amuleto_verde", new Item(new Item.Properties()));

	/** Linterna: ilumina adonde apunta y quema a las criaturas sensibles a la luz. */
	public static final Item LINTERNA = registrar("linterna", new LinternaItem(new Item.Properties()));

	/** Batería de Dilitio: recarga la Linterna (en la mano secundaria). */
	public static final Item BATERIA_DILITIO = registrar("bateria_dilitio",
			new DescritoItem(new Item.Properties().stacksTo(16), DescritoItem::bateriaDilitio));

	/** Totem Frerico: al usarlo da 2 corazones más, para siempre. */
	/** Dilitio: sale del Bloque de Dilitio con Pico de Diamante o mejor. */
	public static final Item DILITIO = registrar("dilitio", new DilitioItem(new Item.Properties()));

	/** Cristal Verde: lo suelta el Mineral Verde. */
	public static final Item CRISTAL_VERDE = registrar("cristal_verde", new Item(new Item.Properties()));

	/** Pimpollo de Qumara (por ahora solo el ítem, con su descripción). */
	public static final Item PIMPOLLO_QUMARA = registrar("pimpollo_qumara",
			new DescritoItem(new Item.Properties(), DescritoItem::pimpolloQumara));

	/** Barra de Cristal Verde (por ahora solo el ítem). */
	public static final Item BARRA_CRISTAL_VERDE = registrar("barra_cristal_verde", new Item(new Item.Properties()));

	/** Racimo de Dilitio: 9 de Dilitio en la mesa de crafteo. */
	public static final Item RACIMO_DILITIO = registrar("racimo_dilitio",
			new DescritoItem(new Item.Properties(), DescritoItem::racimoDilitio));

	public static final Item TOTEM_FRERICO = registrar("totem_frerico", new TotemFrericoItem(new Item.Properties()));
	/** Tótem de Concha: te salva de la muerte, como el Tótem de la Inmortalidad. */
	public static final Item TOTEM_CONCHA = registrar("totem_concha", new TotemConchaItem(new Item.Properties()));
	/** Tótem del Ídolo: por ahora no hace nada (se apila de a 1, como el tótem de Minecraft). */
	/** Corazón Violeta: por ahora no hace nada. */
	public static final Item CORAZON_VIOLETA = registrar("corazon_violeta", new Item(new Item.Properties()));
	/** Corazón Dorado: por ahora no hace nada. */
	public static final Item CORAZON_DORADO = registrar("corazon_dorado", new Item(new Item.Properties()));
	/** Sin Alma: con click derecho abre la G. */
	public static final Item SIN_ALMA = registrar("sin_alma", new SinAlmaItem(new Item.Properties().stacksTo(1)));
	/** Alma: con click derecho abre la G (como el Sin Alma). */
	public static final Item ALMA = registrar("alma", new SinAlmaItem(new Item.Properties().stacksTo(1)));
	public static final Item TOTEM_IDOLO = registrar("totem_idolo", new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	/** Candado: cierra un cofre con un código. */
	public static final Item CANDADO = registrar("candado",
			new CandadoItem(new Item.Properties().stacksTo(16)));

	/** Huevo generador de la Bomba Warden: azul oscuro con el brillo del sculk. */
	public static final Item BOMBA_WARDEN_SPAWN_EGG = registrar("bomba_warden_spawn_egg",
			new SpawnEggItem(ModEntidades.BOMBA_WARDEN, 0x0F3A44, 0x46F0E6, new Item.Properties()));

	/** Huevo generador del Soarer. */
	public static final Item SOARER_SPAWN_EGG = registrar("soarer_spawn_egg",
			new SpawnEggItem(ModEntidades.SOARER, 0x2B3A55, 0xE8A33D, new Item.Properties()));

	// Huevos de los cuatro lagartos.
	public static final Item MIRA_SPAWN_EGG = registrar("mira_spawn_egg",
			new HuevoConDescripcion(ModEntidades.MIRA, 0x2F5A1E, 0x6B7A3A, new Item.Properties(),
					HuevoConDescripcion::reptisaurioSalvaje));

	public static final Item REPTISAURIO_GUERRERO_SPAWN_EGG = registrar("reptisaurio_guerrero_spawn_egg",
			new SpawnEggItem(ModEntidades.REPTISAURIO_GUERRERO, 0x5A3A22, 0xD8C23A, new Item.Properties()));

	public static final Item REPTISAURIO_ARQUERO_SPAWN_EGG = registrar("reptisaurio_arquero_spawn_egg",
			new SpawnEggItem(ModEntidades.REPTISAURIO_ARQUERO, 0x1F9A4A, 0xE8ECE6, new Item.Properties()));

	public static final Item REPTISAURIO_LANZA_SPAWN_EGG = registrar("reptisaurio_lanza_spawn_egg",
			new SpawnEggItem(ModEntidades.REPTISAURIO_LANZA, 0xC07A1E, 0xE8C870, new Item.Properties()));

	/** Huevo generador del Plumosaurio (id walker). */
	public static final Item WALKER_SPAWN_EGG = registrar("walker_spawn_egg",
			new HuevoConDescripcion(ModEntidades.WALKER, 0x6B5A46, 0xC9B189, new Item.Properties(),
					HuevoConDescripcion::plumosaurio));

	/** Huevos de los bichos cubo del Enfriamiento de Qumara. */
	public static final Item BICHO_NO_CABEZA_SPAWN_EGG = registrar("bicho_no_cabeza_spawn_egg",
			new SpawnEggItem(com.dedsafio4.qumara.ModQumara.BICHO_NO_CABEZA, 0xE99A4D, 0x8D3B86, new Item.Properties()));
	public static final Item BICHO_CABEZA_SPAWN_EGG = registrar("bicho_cabeza_spawn_egg",
			new SpawnEggItem(com.dedsafio4.qumara.ModQumara.BICHO_CABEZA, 0xEFE0D4, 0x9E1A4F, new Item.Properties()));

	public static final Item CAPULLO_SPAWN_EGG = registrar("capullo_spawn_egg",
			new SpawnEggItem(com.dedsafio4.qumara.ModQumara.CAPULLO, 0x5A34C4, 0x4F9A2E, new Item.Properties()));

	/** Huevo generador de Qumara (la Flor Mutante): aparece como una rosa gigante cerrada. */
	public static final Item QUMARA_SPAWN_EGG = registrar("qumara_spawn_egg",
			new SpawnEggItem(com.dedsafio4.qumara.ModQumara.QUMARA, 0xF0418F, 0x8A4AD8, new Item.Properties()));

	/** Huevo generador del T-Rex gigante (el jefe que maneja un admin). */
	public static final Item TREX_SPAWN_EGG = registrar("trex_spawn_egg",
			new SpawnEggItem(com.dedsafio4.trex.ModTRex.TREX, 0xF07A1C, 0x7B3FC4, new Item.Properties()));

	/** Huevo generador de Herobrine (va por el jugador más cercano). */
	public static final Item HEROBRINE_SPAWN_EGG = registrar("herobrine_spawn_egg",
			new SpawnEggItem(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, 0x00A8A8, 0x463AA5, new Item.Properties()));

	/** Huevo generador del Dáctylo Bebé. */
	public static final Item DACTYLO_BEBE_SPAWN_EGG = registrar("dactylo_bebe_spawn_egg",
			new SpawnEggItem(com.dedsafio4.dactylos.ModDactylos.DACTYLO_BEBE, 0x8A4A2A, 0xE8913A, new Item.Properties()));

	/** Huevo generador del Creeper Amarillo. */
	public static final Item CREEPER_AMARILLO_SPAWN_EGG = registrar("creeper_amarillo_spawn_egg",
			new SpawnEggItem(ModEntidades.CREEPER_AMARILLO, 0xFFF4A8, 0x362E12, new Item.Properties()));

	/** Huevo generador del Creeper de Pasto. */
	public static final Item CREEPER_PASTO_SPAWN_EGG = registrar("creeper_pasto_spawn_egg",
			new SpawnEggItem(ModEntidades.CREEPER_PASTO, 0x437524, 0x141A10, new Item.Properties()));

	/** Huevo generador del Aldeano Robot. */
	public static final Item ALDEANO_ROBOT_SPAWN_EGG = registrar("aldeano_robot_spawn_egg",
			new SpawnEggItem(ModEntidades.ALDEANO_ROBOT, 0x141615, 0x35E04A, new Item.Properties()));

	// Lo del Huevo Eburia.
	public static final Item HUEVO_EBURIA = registrar("huevo_eburia",
			new HuevoEburiaItem(new Item.Properties()
					.food(new net.minecraft.world.food.FoodProperties.Builder()
							.nutrition(4).saturationModifier(0.3f).alwaysEdible().build())));

	/** Arándano Nocturno: al comerlo da Visión nocturna por 2 minutos. */
	public static final Item ARANDANO_NOCTURNO = registrar("arandano_nocturno", new ArandanoItem(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).alwaysEdible().build())));

	/** Corazón: al comerlo da un corazón más para siempre. */
	public static final Item CORAZON = registrar("corazon", new CorazonItem(new Item.Properties().rarity(Rarity.RARE)
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build())));

	/** Fruto de Quiu: al comerlo, la próxima vez que te morís no perdés el inventario (una sola vez). */
	public static final Item FRUTO_QUIU = registrar("fruto_quiu", new FrutoQuiuItem(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible().build())));

	/**
	 * Espada de Hoja de Qumara: 10 de daño, 1,6 de velocidad y 2030 de durabilidad.
	 */
	private static final net.minecraft.world.item.Tier HOJA_QUMARA = new net.minecraft.world.item.Tier() {
		private final net.minecraft.world.item.Tier base = net.minecraft.world.item.Tiers.NETHERITE;
		@Override public int getUses() { return 2030; }
		@Override public float getSpeed() { return base.getSpeed(); }
		@Override public float getAttackDamageBonus() { return base.getAttackDamageBonus(); }
		@Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
			return base.getIncorrectBlocksForDrops();
		}
		@Override public int getEnchantmentValue() { return base.getEnchantmentValue(); }
		@Override public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() { return base.getRepairIngredient(); }
	};
	// Daño: 1 (el de la mano) + 5 + 4 (el bonus de netherita) = 10. Velocidad: 4 - 2,4 = 1,6.
	public static final Item ESPADA_HOJA_QUMARA = registrar("espada_hoja_qumara",
			new net.minecraft.world.item.SwordItem(HOJA_QUMARA, new Item.Properties()
					.attributes(net.minecraft.world.item.SwordItem.createAttributes(HOJA_QUMARA, 5, -2.4f))));

	/** Semilla de Arándano Nocturno: se planta en los troncos del Centro de Quiu. */
	public static final Item SEMILLA_ARANDANO = registrar("semilla_arandano",
			new SemillaArandanoItem(com.dedsafio4.bloques.ModBloques.ARANDANO_NOCTURNO, new Item.Properties().stacksTo(64)));

	public static final Item PELO_EBURIA = registrar("pelo_eburia",
			new PeloEburiaItem(new Item.Properties().stacksTo(64)));

	/** La semilla es la que se planta en los troncos (como los granos de cacao) y da el Huevo Eburia. */
	public static final Item SEMILLA_EBURIA = registrar("semilla_eburia",
			new SemillaEburiaItem(com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA, new Item.Properties().stacksTo(64)));

	// El ámbar de la dimensión nueva.
	public static final Item AMBAR_EN_BRUTO = registrar("ambar_en_bruto",
			new AmbarEnBrutoItem(new Item.Properties()));

	public static final Item AMBAR = registrar("ambar",
			new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

	/** Casco de Netherita con recubrimiento de planta (la armadura nueva; el resto de las piezas viene después). */
	public static final Item CASCO_NETHERITA_PLANTA = registrar("casco_netherita_planta",
			new NetheritaPlanta.Pieza(ArmorItem.Type.HELMET, new Item.Properties()));

	public static final Item PECHERA_NETHERITA_PLANTA = registrar("pechera_netherita_planta",
			new NetheritaPlanta.Pieza(ArmorItem.Type.CHESTPLATE, new Item.Properties()));

	public static final Item PANTALONES_NETHERITA_PLANTA = registrar("pantalones_netherita_planta",
			new NetheritaPlanta.Pieza(ArmorItem.Type.LEGGINGS, new Item.Properties()));

	public static final Item BOTAS_NETHERITA_PLANTA = registrar("botas_netherita_planta",
			new NetheritaPlanta.Pieza(ArmorItem.Type.BOOTS, new Item.Properties()));

	/** Bolsa de Ender: abre tu Cofre de Ender desde cualquier lado. */
	public static final Item BOLSA_ENDER = registrar("bolsa_ender", new BolsaEnderItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	/** Signo de Interrogación: no hace nada. */
	public static final Item SIGNO_INTERROGACION = registrar("signo_interrogacion", new Item(new Item.Properties()) {
		/** Se llama "???", en dorado. */
		@Override
		public net.minecraft.network.chat.Component getName(ItemStack pila) {
			return net.minecraft.network.chat.Component.translatable(this.getDescriptionId()).withColor(0xE8C547);
		}
	});

	/** Ámbar con Insecto: ámbar con un insecto atrapado en el medio (por ahora solo el ítem). */
	public static final Item AMBAR_CON_INSECTO = registrar("ambar_con_insecto", new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

	private static Item registrar(String nombre, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre), item);
	}

	/** Pestaña "Dedsafío 4" del creativo con todo lo que agrega el mod. */
	public static final CreativeModeTab PESTANA = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "dedsafio4"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.dedsafio4"))
					.icon(() -> new ItemStack(DEDITA))
					.displayItems((parametros, entradas) -> {
						entradas.accept(DEDITA);
						entradas.accept(DEDITA_VERDE);
						entradas.accept(DEDITA_ROJA);
						entradas.accept(DEDITA_MERCADO_NEGRO);
						entradas.accept(DEDITA_MISION);
						entradas.accept(com.dedsafio4.banco.ModCajero.CAJERO_ITEM);
						entradas.accept(com.dedsafio4.puertas.ModPuertas.PUERTA_ROSA_ITEM);
						entradas.accept(com.dedsafio4.puertas.ModPuertas.PUERTA_VERDE_ITEM);
						entradas.accept(DEDITA_CASINO);
						entradas.accept(MARTILLO_NETHERITE);
						entradas.accept(CANDADO);
						entradas.accept(LLAVE_CANDADO);
						entradas.accept(LINTERNA);
						entradas.accept(TOTEM_FRERICO);
						entradas.accept(TOTEM_CONCHA);
						entradas.accept(TOTEM_IDOLO);
						entradas.accept(SIN_ALMA);
						entradas.accept(ALMA);
						entradas.accept(com.dedsafio4.baneos.Baneos.ITEM);
						entradas.accept(BATERIA_DILITIO);
						entradas.accept(com.dedsafio4.cofres.ModCofres.COFRE_ITEM);
						entradas.accept(com.dedsafio4.cofres.ModCofres.COFRE_HUESOS_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MINERAL_DE_AMBAR_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MINERAL_VERDE_ITEM);
						entradas.accept(CRISTAL_VERDE);
						entradas.accept(BARRA_CRISTAL_VERDE);
						entradas.accept(PIMPOLLO_QUMARA);
						entradas.accept(DILITIO);
						entradas.accept(RACIMO_DILITIO);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_CELESTE_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_AMARILLO_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_VERDE_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_ROJO_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_PIXELES_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_DILITIO_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.CARNE_ROSA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.CARNE_ROJA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.CARNE_CON_VENAS_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_CARNE_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MUSCULO_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.GELATINA_ROJA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.GELATINA_ROSA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.PASTO_ROSA_ITEM);
						entradas.accept(com.dedsafio4.robots.ModRobots.ROBOT_DORMIDO_ITEM);
						entradas.accept(LLAVE);
						entradas.accept(FRUTA_SOLARIA);
						entradas.accept(SEMILLA_SOLARIA);
						entradas.accept(BAYA_UVINA);
						entradas.accept(SEMILLA_UVINA);
						entradas.accept(MANZANA_DE_AMBAR);
						entradas.accept(AMBAR_EN_BRUTO);
						entradas.accept(AMBAR);
						entradas.accept(AMBAR_CON_INSECTO);
						entradas.accept(CASCO_NETHERITA_PLANTA);
						entradas.accept(PECHERA_NETHERITA_PLANTA);
						entradas.accept(PANTALONES_NETHERITA_PLANTA);
						entradas.accept(BOTAS_NETHERITA_PLANTA);
						entradas.accept(SIGNO_INTERROGACION);
						entradas.accept(BOLSA_ENDER);
						entradas.accept(com.dedsafio4.marcos.ModMarcos.MARCO_GRANDE_ITEM);
						entradas.accept(CHIP_PHORA);
						entradas.accept(HUEVO_EBURIA);
						entradas.accept(ARANDANO_NOCTURNO);
						entradas.accept(CORAZON);
						entradas.accept(FRUTO_QUIU);
						entradas.accept(ESPADA_HOJA_QUMARA);
						entradas.accept(CORAZON_VIOLETA);
						entradas.accept(CORAZON_DORADO);
						entradas.accept(SEMILLA_ARANDANO);
						entradas.accept(PELO_EBURIA);
						entradas.accept(SEMILLA_EBURIA);
						entradas.accept(TELA_PRIMITIVA);
						entradas.accept(CUERDA_RESISTENTE);
						entradas.accept(PEGAMENTO_PRIMITIVO);
						entradas.accept(ESCUPITAJO_DACTYLO);
						entradas.accept(INSECTO);
						entradas.accept(CUCHARA_MADERA);
						entradas.accept(CUCHARA_HOJAS);
						entradas.accept(TENEDOR_HOJAS);
						entradas.accept(CUCHARA_DORADA_HOJAS);
						entradas.accept(TARJETA_ROSA);
						entradas.accept(TARJETA_PUERTA_ROSA);
						entradas.accept(TARJETA_PUERTA_VERDE);
						entradas.accept(AMULETO_VERDE);
						entradas.accept(EXCREMENTO);
						entradas.accept(BOLSA_DE_TELA);
						entradas.accept(FRUTA_ESPACIO);
						entradas.accept(PILDORA_VACIA);
						entradas.accept(JERINGA);
						entradas.accept(PILDORA);
						entradas.accept(SANGRE_REPTISAURIO);
						entradas.accept(ANTIDOTO_PRIMITIVO);
						entradas.accept(MASCARA_ANTI_ESPORAS);
						entradas.accept(com.dedsafio4.bloques.ModBloques.CACA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.AGUA_PORTAL_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.ROBLE_CLARO_LOG_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.ABETO_CLARO_LOG_ITEM);
						entradas.accept(MANUSCRITO_HERMANDAD);
						entradas.accept(MANUSCRITO_HERMANDAD_ADMIN);
						entradas.accept(NAVE_SPAWN_EGG);
						entradas.accept(NAVE_VELOZ_SPAWN_EGG);
						entradas.accept(BOMBA_WARDEN_SPAWN_EGG);
						entradas.accept(SOARER_SPAWN_EGG);
						entradas.accept(WALKER_SPAWN_EGG);
						entradas.accept(com.dedsafio4.casino.ModCasino.CASINO_SPAWN_EGG);
						entradas.accept(com.dedsafio4.nave.PartesNave.CABINA);
						entradas.accept(com.dedsafio4.nave.PartesNave.MOTOR);
						entradas.accept(com.dedsafio4.nave.PartesNave.ALERON_IZQUIERDO);
						entradas.accept(com.dedsafio4.nave.PartesNave.ALERON_DERECHO);
						entradas.accept(com.dedsafio4.nave.PartesNave.NAVE_BIPLAZA);
						entradas.accept(com.dedsafio4.despegue.ModDespegue.PLATAFORMA_DESPEGUE_ITEM);
						entradas.accept(com.dedsafio4.despegue.ModDespegue.PLATAFORMA_METAL_ITEM);
						entradas.accept(com.dedsafio4.despegue.ModDespegue.PLATAFORMA_BORDE_ITEM);
						entradas.accept(ALDEANO_ROBOT_SPAWN_EGG);
						entradas.accept(CREEPER_PASTO_SPAWN_EGG);
						entradas.accept(CREEPER_AMARILLO_SPAWN_EGG);
						entradas.accept(DACTYLO_BEBE_SPAWN_EGG);
						entradas.accept(HEROBRINE_SPAWN_EGG);
						entradas.accept(TREX_SPAWN_EGG);
						entradas.accept(QUMARA_SPAWN_EGG);
						entradas.accept(BICHO_NO_CABEZA_SPAWN_EGG);
						entradas.accept(BICHO_CABEZA_SPAWN_EGG);
						entradas.accept(CAPULLO_SPAWN_EGG);
						entradas.accept(MIRA_SPAWN_EGG);
						entradas.accept(REPTISAURIO_GUERRERO_SPAWN_EGG);
						entradas.accept(REPTISAURIO_ARQUERO_SPAWN_EGG);
						entradas.accept(REPTISAURIO_LANZA_SPAWN_EGG);
						for (var pocion : List.of(ModPociones.ROJIZO, ModPociones.NEGRO_PURO, ModPociones.CORAZONES_OCULTOS)) {
							for (Item tipo : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
								entradas.accept(PotionContents.createItemStack(tipo, pocion));
							}
						}
					})
					.build());

	/** Fuerza la carga de la clase para que se registren los ítems y la pestaña. */
	public static void registrar() {}
}
