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
			new DeditaMisionItem(new Item.Properties().stacksTo(99).rarity(Rarity.RARE)));

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
	/** Cuero Glebanoide: un ingrediente (lo sueltan el Gusano de Carne y el Parásito Volador). */
	public static final Item CUERO_GLEBANOIDE = registrar("cuero_glebanoide",
			new DescritoItem(new Item.Properties(), DescritoItem::cueroGlebanoide));
	/** Diente Glebanoide: sale de romper una Dentadura Glebanoide con espada. */
	public static final Item DIENTE_GLEBANOIDE = registrar("diente_glebanoide",
			new DescritoItem(new Item.Properties(), DescritoItem::dienteGlebanoide));
	/** Carne Glebanoide: comida que saca la Levitación (da Caída Lenta 1 segundo); cocinada da Oro en Bruto. */
	public static final Item CARNE_GLEBANOIDE = registrar("carne_glebanoide", new CarneGlebanoideItem(new Item.Properties()
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).alwaysEdible().build()),
			DescritoItem::carneGlebanoide));
	/** Fibra Glebanoide: se hace con Tejido Glebanoide Profundo y Verruga Oscura Glebanoide. */
	public static final Item FIBRA_GLEBANOIDE = registrar("fibra_glebanoide",
			new DescritoItem(new Item.Properties(), DescritoItem::fibraGlebanoide));

	/** Insecto: a veces lo sueltan los Dromoraptores. */
	public static final Item INSECTO = registrar("insecto", new DescritoItem(new Item.Properties(), DescritoItem::insecto));

	/** Pegamento Primitivo: material de las criaturas del Centro de Quiu. */
	public static final Item PEGAMENTO_PRIMITIVO = registrar("pegamento_primitivo",
			new DescritoItem(new Item.Properties(), DescritoItem::pegamentoPrimitivo));

	/** Bolsa Primitiva (id bolsa_de_tela): lleva lo mismo que un cofre simple. */
	public static final Item BOLSA_DE_TELA = registrar("bolsa_de_tela", new BolsaItem(new Item.Properties().stacksTo(1)));
	/** Bolsa Glebanoide (id saco): como la Bolsa Primitiva pero con 9 lugares más (4 filas, 36). */
	public static final Item SACO = registrar("saco", new BolsaItem(new Item.Properties().stacksTo(1), 4, "a"));
	/** Arco Glebanoide: sus flechas rebotan en los enemigos cercanos. */
	public static final Item ARCO_GLEBANOIDE = registrar("arco_glebanoide", new ArcoGlebanoideItem(new Item.Properties().durability(384)));

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
	/** Cuerno de Narval (id jeringa): lo sueltan el Narval y el Narval con Reptisaurio. */
	public static final Item JERINGA = registrar("jeringa", new DescritoItem(new Item.Properties().stacksTo(16), DescritoItem::cuernoNarval));

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
	/** Trozo Amarillo: ítem nuevo (por ahora sin uso; el nombre y lo que hace los dice el usuario). */
	public static final Item TROZO_AMARILLO = registrar("trozo_amarillo", new Item(new Item.Properties()));
	/** Tótem Glebanoide: variante del Nutritótem; por ahora te salva de morir si lo tienes en la mano. */
	public static final Item TOTEM_GLEBANOIDE = registrar("totem_glebanoide", new TotemGlebanoideItem(new Item.Properties()));
	/** Tótem Limbo: te salva de morir (en la mano) y deja la barra de salud en 20 (devuelve los corazones del Limbo, saca los extra). */
	public static final Item TOTEM_LIMBO = registrar("totem_limbo", new TotemLimboItem(new Item.Properties()));
	/** Corazón del Limbo (el violeta): al comerlo, la barra de salud vuelve a 20 (devuelve los corazones del Limbo, saca los extra). */
	public static final Item CORAZON_VIOLETA = registrar("corazon_violeta", new CorazonLimboItem(new Item.Properties().rarity(Rarity.RARE)
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build())));
	/** Corazón de Absorción (el dorado): al comerlo da Absorción V por 3 minutos. */
	public static final Item CORAZON_DORADO = registrar("corazon_dorado", new CorazonAbsorcionItem(new Item.Properties().rarity(Rarity.RARE)
			.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build())));
	/** Sin Alma: con click derecho abre la G. */
	public static final Item SIN_ALMA = registrar("sin_alma", new SinAlmaItem(new Item.Properties().stacksTo(1)));
	/** Alma: con click derecho abre la G (como el Sin Alma). */
	public static final Item ALMA = registrar("alma", new SinAlmaItem(new Item.Properties().stacksTo(1)));
	/** Tótem del Ídolo: te salva de morir aunque esté en cualquier lugar del inventario. */
	public static final Item TOTEM_IDOLO = registrar("totem_idolo", new TotemIdoloItem(new Item.Properties()));

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

	public static final Item DROMORAPTOR_ROJO_SPAWN_EGG = registrar("dromoraptor_rojo_spawn_egg",
			new HuevoConDescripcion(ModEntidades.DROMORAPTOR_ROJO, 0xB5532A, 0x2A3F8F, new Item.Properties(),
					HuevoConDescripcion::dromoraptorRojo));
	public static final Item DROMORAPTOR_AZUL_SPAWN_EGG = registrar("dromoraptor_azul_spawn_egg",
			new HuevoConDescripcion(ModEntidades.DROMORAPTOR_AZUL, 0x2E9C9A, 0xC41E24, new Item.Properties(),
					HuevoConDescripcion::dromoraptorAzul));
	public static final Item CREEPER_NUCLEAR_SPAWN_EGG = registrar("creeper_nuclear_spawn_egg",
			new HuevoConDescripcion(ModEntidades.CREEPER_NUCLEAR, 0xE2C21C, 0xE0261C, new Item.Properties(),
					HuevoConDescripcion::creeperNuclear));
	public static final Item CREEPER_PASTEL_SPAWN_EGG = registrar("creeper_pastel_spawn_egg",
			new HuevoConDescripcion(ModEntidades.CREEPER_PASTEL, 0xB8692E, 0xF4F0EA, new Item.Properties(),
					HuevoConDescripcion::creeperPastel));
	public static final Item ZARINOSA_SPAWN_EGG = registrar("zarinosa_espigueya_spawn_egg",
			new HuevoConDescripcion(ModEntidades.ZARINOSA, 0x2FAE2A, 0xD8C21A, new Item.Properties(),
					HuevoConDescripcion::zarinosa));
	public static final Item CREEPER_AZALEA_SPAWN_EGG = registrar("creeper_raiz_azalea_spawn_egg",
			new HuevoConDescripcion(ModEntidades.CREEPER_AZALEA, 0x1F8A2A, 0xE274AC, new Item.Properties(),
					HuevoConDescripcion::creeperAzalea));
	public static final Item ZOMBIE_PLATA_SPAWN_EGG = registrar("zombie_plata_spawn_egg",
			new HuevoConDescripcion(ModEntidades.ZOMBIE_PLATA, 0x4A4A1E, 0xD474D4, new Item.Properties(),
					HuevoConDescripcion::zombiePlata));
	public static final Item NAUTILUS_OSEO_SPAWN_EGG = registrar("nautilus_oseo_spawn_egg",
			new HuevoConDescripcion(ModEntidades.NAUTILUS_OSEO, 0xE9DCC6, 0xE0898A, new Item.Properties(),
					HuevoConDescripcion::nautilusOseo));
	public static final Item CEREBRO_AMARILLO_SPAWN_EGG = registrar("cerebro_amarillo_spawn_egg",
			new HuevoConDescripcion(ModEntidades.CEREBRO_AMARILLO, 0xE3949A, 0xF2C230, new Item.Properties(),
					HuevoConDescripcion::cerebroAmarillo));
	public static final Item GARRAPATA_CEREBRAL_SPAWN_EGG = registrar("garrapata_cerebral_spawn_egg",
			new HuevoConDescripcion(ModEntidades.GARRAPATA_CEREBRAL, 0xE3949A, 0x6B2D8A, new Item.Properties(),
					HuevoConDescripcion::garrapataCerebral));
	public static final Item FLASHBANG_SPAWN_EGG = registrar("flashbang_spawn_egg",
			new HuevoConDescripcion(ModEntidades.FLASHBANG, 0x2B2B2B, 0xF4F4F4, new Item.Properties(),
					HuevoConDescripcion::flashbang));
	public static final Item FANTASMA_AMARILLO_SPAWN_EGG = registrar("fantasma_amarillo_spawn_egg",
			new HuevoConDescripcion(ModEntidades.FANTASMA_AMARILLO, 0xF2C230, 0x2B2B2B, new Item.Properties(),
					HuevoConDescripcion::fantasmaAmarillo));
	public static final Item FANTASMA_BLANCO_SPAWN_EGG = registrar("fantasma_blanco_spawn_egg",
			new HuevoConDescripcion(ModEntidades.FANTASMA_BLANCO, 0xF2F2F6, 0x2B2B2B, new Item.Properties(),
					HuevoConDescripcion::fantasmaBlanco));
	public static final Item FANTASMA_ROJO_SPAWN_EGG = registrar("fantasma_rojo_spawn_egg",
			new HuevoConDescripcion(ModEntidades.FANTASMA_ROJO, 0xD8261C, 0x5A0E0E, new Item.Properties(),
					HuevoConDescripcion::fantasmaRojo));
	public static final Item FANTASMA_NEGRO_SPAWN_EGG = registrar("fantasma_negro_spawn_egg",
			new HuevoConDescripcion(ModEntidades.FANTASMA_NEGRO, 0x1E1E22, 0x6A6A72, new Item.Properties(),
					HuevoConDescripcion::fantasmaNegro));
	public static final Item WRAITH_SPAWN_EGG = registrar("wraith_spawn_egg",
			new HuevoConDescripcion(ModEntidades.WRAITH, 0x2A2D33, 0x9AA0A6, new Item.Properties(),
					HuevoConDescripcion::wraith));
	/** Gema Roja: la sueltan los Fantasmas Rojos (20%). La textura brilla (animada). */
	public static final Item GEMA_ROJA = registrar("gema_roja",
			new DescritoItem(new Item.Properties().rarity(Rarity.UNCOMMON), DescritoItem::gemaRoja));
	/** Lo que sueltan los fantasmas (20%): Esencia Blanca (Blanco), Esencia Negra (Negro) y Esencia Amarilla (Amarillo). Brillan (animadas). */
	public static final Item GEMA_BLANCA = registrar("gema_blanca",
			new DescritoItem(new Item.Properties().rarity(Rarity.UNCOMMON), DescritoItem::gemaBlanca));
	public static final Item GEMA_GRIS = registrar("gema_gris",
			new DescritoItem(new Item.Properties().rarity(Rarity.UNCOMMON), DescritoItem::gemaGris));
	public static final Item GEMA_DORADA = registrar("gema_dorada",
			new DescritoItem(new Item.Properties().rarity(Rarity.UNCOMMON), DescritoItem::gemaDorada));
	/** Espada Glebanoide: 11 de daño, ya encantada con Perdición de Gleba (33% más contra las criaturas de Gleba). */
	public static final Item ESPADA_GLEBANOIDE = registrar("espada_glebanoide", new EspadaGlebanoideItem(new Item.Properties().rarity(Rarity.RARE)));
	/** Corazón Glebanoide: con click derecho golpea a las criaturas de Gleba cercanas (enfrente o debajo). */
	public static final Item CORAZON_GLEBANOIDE = registrar("corazon_glebanoide", new CorazonGlebanoideItem(new Item.Properties().rarity(Rarity.RARE)));
	/** Pechera Glebanoide: 9 de armadura, 3 de resistencia y medio corazón extra. */
	/** Casco Glebanoide: 4 de armadura, 3 de resistencia y medio corazón extra. */
	public static final Item CASCO_GLEBANOIDE = registrar("casco_glebanoide",
			new ArmaduraGlebanoide.Pieza(ArmorItem.Type.HELMET, 907, 16, new Item.Properties().rarity(Rarity.RARE)));
	public static final Item PECHERA_GLEBANOIDE = registrar("pechera_glebanoide",
			new ArmaduraGlebanoide.Pieza(ArmorItem.Type.CHESTPLATE, 592, 36, new Item.Properties().rarity(Rarity.RARE)));
	/** Pantalones Glebanoides: 7 de armadura, 3 de resistencia y medio corazón extra. */
	public static final Item PANTALONES_GLEBANOIDES = registrar("pantalones_glebanoides",
			new ArmaduraGlebanoide.Pieza(ArmorItem.Type.LEGGINGS, 1055, 28, new Item.Properties().rarity(Rarity.RARE)));
	/** Botas Glebanoides: 4 de armadura, 3 de resistencia y medio corazón extra. */
	public static final Item BOTAS_GLEBANOIDES = registrar("botas_glebanoides",
			new ArmaduraGlebanoide.Pieza(ArmorItem.Type.BOOTS, 981, 16, new Item.Properties().rarity(Rarity.RARE)));
	/** Tenedor Simple: de hierro; se usa para el Tridente del Limbo. */
	public static final Item TENEDOR_SIMPLE = registrar("tenedor_simple",
			new DescritoItem(new Item.Properties().stacksTo(16), DescritoItem::tenedorSimple));
	public static final Item ALMITA_SPAWN_EGG = registrar("almita_spawn_egg",
			new HuevoConDescripcion(ModEntidades.ALMITA, 0x5FE3E6, 0xF3F2F2, new Item.Properties(),
					HuevoConDescripcion::almita));
	public static final Item GUSANO_CARNE_SPAWN_EGG = registrar("gusano_carne_spawn_egg",
			new HuevoConDescripcion(ModEntidades.GUSANO_CARNE, 0x9A3A22, 0xB7C23A, new Item.Properties(),
					HuevoConDescripcion::gusanoCarne));
	public static final Item NARVAL_SPAWN_EGG = registrar("narval_spawn_egg",
			new HuevoConDescripcion(ModEntidades.NARVAL, 0x26337E, 0xDCDCD2, new Item.Properties(),
					HuevoConDescripcion::narval));

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
	/** Pelo Glebanoide: con una espada sobre los Pelos Glebanoides (el pasto de los Órganos); es un ingrediente. */
	public static final Item PELO_GLEBANOIDE = registrar("pelo_glebanoide",
			new DescritoItem(new Item.Properties(), DescritoItem::peloGlebanoide));
	/** Cerebro Glebanoide: lo suelta la Garrapata Cerebral; es un ingrediente (la textura es provisoria). */
	public static final Item CEREBRO_GLEBANOIDE = registrar("cerebro_glebanoide",
			new DescritoItem(new Item.Properties(), DescritoItem::cerebroGlebanoide));
	/** Caparazón: lo suelta el Nautilus Óseo; es un ingrediente. */
	public static final Item CAPARAZON = registrar("caparazon", new DescritoItem(new Item.Properties(), DescritoItem::caparazon));

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
						entradas.accept(com.dedsafio4.subastas.ModVitrina.VITRINA_ITEM);
						entradas.accept(com.dedsafio4.boveda.ModBoveda.BOVEDA_ITEM);
						entradas.accept(com.dedsafio4.despegue.ModDespegue.COMBUSTIBLE);
						entradas.accept(com.dedsafio4.puertas.ModPuertas.PUERTA_ROSA_ITEM);
						entradas.accept(com.dedsafio4.puertas.ModPuertas.PUERTA_VERDE_ITEM);
						entradas.accept(DEDITA_CASINO);
						entradas.accept(MARTILLO_NETHERITE);
						entradas.accept(CANDADO);
						entradas.accept(LLAVE_CANDADO);
						entradas.accept(LINTERNA);
						entradas.accept(TOTEM_FRERICO);
						entradas.accept(TOTEM_GLEBANOIDE);
						entradas.accept(TROZO_AMARILLO);
						entradas.accept(TOTEM_IDOLO);
						entradas.accept(SIN_ALMA);
						entradas.accept(ALMA);
						entradas.accept(com.dedsafio4.baneos.Baneos.ITEM);
						entradas.accept(BATERIA_DILITIO);
						entradas.accept(com.dedsafio4.cofres.ModCofres.COFRE_ITEM);
						entradas.accept(com.dedsafio4.cofres.ModCofres.COFRE_HUESOS_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MINERAL_DE_AMBAR_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MINERAL_DE_GRASA_ITEM);
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
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_CARNE_ROSA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.BLOQUE_DIENTES_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MENSAJERO_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.ENTREGA_MISION_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.MUSCULO_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.GELATINA_ROJA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.GELATINA_ROSA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.GRASA_LIQUIDA_ITEM);
						entradas.accept(com.dedsafio4.bloques.ModBloques.GRASA_COAGULADA_ITEM);
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
						entradas.accept(PELO_GLEBANOIDE);
						entradas.accept(CAPARAZON);
						entradas.accept(CEREBRO_GLEBANOIDE);
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
						entradas.accept(CUERO_GLEBANOIDE);
						entradas.accept(FIBRA_GLEBANOIDE);
						entradas.accept(CARNE_GLEBANOIDE);
						entradas.accept(DIENTE_GLEBANOIDE);
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
						entradas.accept(SACO);
						entradas.accept(ARCO_GLEBANOIDE);
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
						entradas.accept(DROMORAPTOR_ROJO_SPAWN_EGG);
						entradas.accept(DROMORAPTOR_AZUL_SPAWN_EGG);
						entradas.accept(NARVAL_SPAWN_EGG);
						entradas.accept(CREEPER_NUCLEAR_SPAWN_EGG);
						entradas.accept(CREEPER_PASTEL_SPAWN_EGG);
						entradas.accept(ZARINOSA_SPAWN_EGG);
						entradas.accept(CREEPER_AZALEA_SPAWN_EGG);
						entradas.accept(ZOMBIE_PLATA_SPAWN_EGG);
						entradas.accept(NAUTILUS_OSEO_SPAWN_EGG);
						entradas.accept(CEREBRO_AMARILLO_SPAWN_EGG);
						entradas.accept(GARRAPATA_CEREBRAL_SPAWN_EGG);
						entradas.accept(TOTEM_LIMBO);
						entradas.accept(ESPADA_GLEBANOIDE);
						entradas.accept(CORAZON_GLEBANOIDE);
						entradas.accept(CASCO_GLEBANOIDE);
						entradas.accept(PECHERA_GLEBANOIDE);
						entradas.accept(PANTALONES_GLEBANOIDES);
						entradas.accept(BOTAS_GLEBANOIDES);
						entradas.accept(TENEDOR_SIMPLE);
						entradas.accept(GEMA_ROJA);
						entradas.accept(GEMA_BLANCA);
						entradas.accept(GEMA_GRIS);
						entradas.accept(GEMA_DORADA);
						entradas.accept(FLASHBANG_SPAWN_EGG);
						entradas.accept(ALMITA_SPAWN_EGG);
						entradas.accept(WRAITH_SPAWN_EGG);
						entradas.accept(FANTASMA_AMARILLO_SPAWN_EGG);
						entradas.accept(FANTASMA_BLANCO_SPAWN_EGG);
						entradas.accept(FANTASMA_ROJO_SPAWN_EGG);
						entradas.accept(FANTASMA_NEGRO_SPAWN_EGG);
						entradas.accept(GUSANO_CARNE_SPAWN_EGG);
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
