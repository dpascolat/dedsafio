package com.dedsafio4.cambios;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

/** Cambios de reglas que se activan con /cambio durante la partida. */
public final class Cambios {
	private Cambios() {}

	/**
	 * @param id           clave con la que se guarda el nivel en el mundo
	 * @param comando      palabras del comando, por ejemplo "respirar agua" → /cambio respirar agua <nivel>
	 * @param anuncios     texto que se anuncia a todos al activar cada nivel
	 */
	public record Cambio(String id, String comando, Map<Integer, String> anuncios) {}

	/** arboles 1: las hojas no sueltan brotes. */
	public static final String ARBOLES = "arboles";
	/**
	 * wardens 1: los Wardens aparecen naturalmente (como los zombies) en la oscuridad profunda.
	 * wardens 2: además, cada Warden que muere deja un Chillador de Sculk que puede invocar más.
	 */
	public static final String WARDENS = "wardens";
	/** respirar agua 1: no se puede respirar bajo el agua metiendo la cabeza en puertas, trampillas, antorchas, etc. */
	public static final String RESPIRAR_AGUA = "respirar_agua";
	/** vacas 1: no se puede sacar leche de las vacas. */
	public static final String VACAS = "vacas";
	/** golem 1: no se pueden construir Gólems de Hierro ni de Nieve. */
	public static final String GOLEM = "golem";
	/**
	 * enderperlas 1: el golpe al teletransportarse con una Perla del End ignora la armadura.
	 * enderperlas 2: además, el golpe hace 10 en vez de 5.
	 */
	public static final String ENDERPERLAS = "enderperlas";
	/** creepers electricos 1: todos los Creepers son eléctricos (cargados). */
	public static final String CREEPERS_ELECTRICOS = "creepers_electricos";
	/** magma 1: tocar un Bloque de Magma de cualquier lado te mata (una vez por segundo); te salvan Resistencia V, o Resistencia al Fuego + Paso Helado. */
	public static final String MAGMA = "magma";
	/** herobrine 1: Herobrine aparece durante las noches; si no le apuntás con la Linterna en 5 segundos te saca 10 corazones. */
	public static final String HEROBRINE = "herobrine";
	/** esqueleto_bogged 1: los Bogged aparecen de noche en todo el Overworld (normalmente solo en los pantanos). */
	public static final String ESQUELETO_BOGGED = "esqueleto_bogged";
	/** esqueleto_stray 1: los Stray aparecen de noche en todo el Overworld (normalmente solo en la nieve). */
	public static final String ESQUELETO_STRAY = "esqueleto_stray";
	/** cobweb 1: al romper una telaraña hay 33% de que aparezca una araña venenosa (araña de cueva). */
	public static final String COBWEB = "cobweb";
	/**
	 * zombi 1: la mitad de los zombis escala paredes como las arañas; zombi 2: además todos tienen Espada de
	 * Hierro con Filo V; zombi 3: además aparecen en el Nether sin importar la luz.
	 */
	public static final String ZOMBI = "zombi";
	/** miel 1: no se puede sacar miel de los panales, y romper un panal hace la explosión de un Creeper. */
	public static final String MIEL = "miel";
	/** creepers_pasto 1: al romper pasto (o si una explosión lo rompe) puede aparecer un Creeper de Pasto (33%). */
	public static final String CREEPERS_PASTO = "creepers_pasto";
	/** blaze 1: los Blaze aparecen naturalmente en todo el Nether, no sólo en las fortalezas. */
	public static final String BLAZE = "blaze";
	/** bambu 1: al romper bambú puede aparecer una araña de cueva (10%). */
	public static final String BAMBU = "bambu";
	/** limbo 1: al romper una cama aparece un Wraith. */
	public static final String LIMBO = "limbo";
	/** zombi_casco 1: los zombis aparecen más seguido con casco (y con casco el sol no los quema). */
	public static final String ZOMBI_CASCO = "zombi_casco";
	/** perlas_fallan 1: cada Perla del End que se tira tiene 1% de fallar (se rompe y no te lleva). */
	public static final String PERLAS_FALLAN = "perlas_fallan";
	/** menos_perlas 1: los Endermen sueltan Perlas del End un 20% menos. */
	public static final String MENOS_PERLAS = "menos_perlas";

	/** zombi_planta 1: los Zombies Planta (id zombie_plata) aparecen en el bioma Forest. */
	public static final String ZOMBI_PLANTA = "zombi_planta";
	/** azalea_plains 1: los Creepers Raíz de Azalea aparecen de día en el bioma Plains. */
	public static final String AZALEA_PLAINS = "azalea_plains";
	/** parasitos_agua 1: los Parásitos (id gusano_carne) aparecen en el agua del Overworld. */
	public static final String PARASITOS_AGUA = "parasitos_agua";
	/** aranas 1: las Arañas de Cueva aparecen de noche en el Overworld y todas las arañas tienen Velocidad infinita. */
	public static final String ARANAS = "aranas";

	/** invisibilidad 1: la invisibilidad no esconde de los mobs. */
	public static final String INVISIBILIDAD = "invisibilidad";
	/** aranas_telas 1: las Arañas y Arañas de Cueva dejan una telaraña al golpearte y al morir. */
	public static final String ARANAS_TELAS = "aranas_telas";
	/** fuego_armadura 1: en el Nether, sin las cuatro piezas de armadura te prendes fuego sin parar. */
	public static final String FUEGO_ARMADURA = "fuego_armadura";
	/** fuego_nether 1: si te quemas en el Nether, el fuego no se apaga solo. */
	public static final String FUEGO_NETHER = "fuego_nether";
	/** fuego_danio 1: el fuego y la lava hacen 10 veces más daño. */
	public static final String FUEGO_DANIO = "fuego_danio";
	/** interactuar 1: tocar Botones, Puertas o Placas de Presión mata (solo te salva un tótem). */
	public static final String INTERACTUAR = "interactuar";
	/** respiracion 1: el aire se acaba 5 veces más rápido y ahogarse hace 10 veces más daño. */
	public static final String RESPIRACION = "respiracion";
	/** carne 1: la carne, el pollo y el pescado del inventario se pudren. */
	public static final String CARNE = "carne";
	/** manzanas 1: las Manzanas de Oro y Encantadas ya no dan Absorción. */
	public static final String MANZANAS = "manzanas";
	/** gas_toxico 1: por encima de la capa 150 del Overworld se pierde vida hasta morir (ver GasToxico). */
	public static final String GAS_TOXICO = "gas_toxico";
	/** zombi_pico 1: los zombis aparecen con Pico de Diamante (que no sueltan). */
	public static final String ZOMBI_PICO = "zombi_pico";
	/** luna_roja 1: de noche, los zombis con pico pican y construyen (ver ZombiMinero). */
	public static final String LUNA_ROJA = "luna_roja";

	/** Tipo de daño propio del mod para la Perla del End: no lo frena la armadura ni sus encantamientos. */
	public static final ResourceKey<DamageType> DANIO_ENDERPERLA = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "enderperla"));

	/** Tipo de daño del Bloque de Magma con el cambio activo. */
	public static final ResourceKey<DamageType> DANIO_MAGMA = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "magma"));

	public static DamageSource danioMagma(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO_MAGMA));
	}

	/** Tipo de daño de tocar Botones, Puertas o Placas: no pasa por la armadura (con tanto daño, colgaría el servidor). */
	public static final ResourceKey<DamageType> DANIO_INTERACTUAR = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "interactuar"));

	public static DamageSource danioInteractuar(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO_INTERACTUAR));
	}

	public static DamageSource danioEnderperla(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO_ENDERPERLA));
	}

	public static final List<Cambio> TODOS = List.of(
			new Cambio(ARBOLES, "arboles", Map.of(
					1, "Ahora no caen brotes de los Árboles. Replantar ya no es una opción.")),
			new Cambio(WARDENS, "wardens", Map.of(
					1, "La Oscuridad Profunda despertó. Ahora los Wardens aparecen en su bioma como cualquier otro monstruo.",
					2, "La Oscuridad Profunda se propaga. Ahora cada Warden que muere deja un Chillador de Sculk donde cayó.")),
			new Cambio(RESPIRAR_AGUA, "respirar agua", Map.of(
					1, "Reviil alteró las corrientes de agua. Ahora no pueden utilizarse las Puertas y Trampillas para respirar bajo el agua.")),
			new Cambio(VACAS, "vacas", Map.of(
					1, "Reviil ha ordeñado a todas las vacas en un segundo. Ahora NO se le puede sacar leche a las vacas.")),
			new Cambio(GOLEM, "golem", Map.of(
					1, "Reviil lanzó una extraña maldición que impide que las calabazas cobren vida. Los Gólems de Hierro y de Nieve ya no pueden ser invocados.")),
			new Cambio(ENDERPERLAS, "enderperlas", Map.of(
					1, "Reviil maldijo las Perlas del End. Ahora el golpe al teletransportarse atraviesa las armaduras.",
					2, "Reviil ha utilizado el poder de la Ruleta para corromper la composición de las Ender Pearls. Ahora su daño aumentó a 10.")),
			new Cambio(CREEPERS_ELECTRICOS, "creepers electricos", Map.of(
					1, "Una tormenta sin fin cargó a todas las criaturas. Ahora todos los Creepers son eléctricos.")),
			new Cambio(MAGMA, "magma", Map.of(
					1, "Reviil hirvió las profundidades. Ahora tocar un Bloque de Magma, de CUALQUIER lado, te mata. Solo Resistencia al Fuego junto con Paso Helado te protege.")),
			new Cambio(BLAZE, "blaze", Map.of(
					1, "El Inframundo no descansa. Ahora los Blaze aparecen naturalmente en el Nether.")),
			new Cambio(CREEPERS_PASTO, "creepers_pasto", Map.of(
					1, "El pasto cobró vida. Ahora al romper pasto, o si una explosión lo rompe, puede aparecer un Creeper de Pasto.")),
			new Cambio(MIEL, "miel", Map.of(
					1, "Reviil envenenó a las abejas. Ahora ya no se puede conseguir miel, y romper un panal hace la explosión de un Creeper.")),
			new Cambio(ZOMBI, "zombi", Map.of(
					1, "Los muertos aprendieron a trepar. Ahora la mitad de los Zombis escala las paredes como las arañas.",
					2, "Reviil armó a los muertos. Ahora todos los Zombis tienen una Espada de Hierro con Filo V.",
					3, "Ahora los Zombies aparecen en el Nether sin importar la luz.")),
			new Cambio(COBWEB, "cobweb", Map.of(
					1, "Las telarañas guardan sorpresas. Ahora al romper una telaraña puede aparecer una Araña Venenosa.")),
			new Cambio(HEROBRINE, "herobrine", Map.of(
					1, "Ahora Herobrine aparece durante las noches.")),
			new Cambio(ESQUELETO_BOGGED, "esqueleto_bogged", Map.of(
					1, "Los esqueletos Bogged pueden salir durante la noche en el Overworld.")),
			new Cambio(ESQUELETO_STRAY, "esqueleto_stray", Map.of(
					1, "Los esqueletos Stray pueden salir durante la noche en el Overworld.")),
			new Cambio(BAMBU, "bambu", Map.of(
					1, "El bambú esconde algo. Ahora al romper bambú puede aparecer una Araña de Cueva.")),
			new Cambio(LIMBO, "limbo", Map.of(
					1, "El Limbo se acerca. Ahora al romper una cama aparece un Wraith.")),
			new Cambio(GAS_TOXICO, "gas_toxico", Map.of(
					1, "Reviil liberó un gas tóxico permanente en la Capa 150 del Overworld. Estar por encima de esa capa causa daño hasta morir. Dentro de una nave no recibes daño, y la Montaña está protegida por Eón.")),
			new Cambio(ZOMBI_PICO, "zombi_pico", Map.of(
					1, "Ahora los Zombies aparecen con Pico de Diamante. Durante la Luna Roja pueden picar y construir.")),
			new Cambio(LUNA_ROJA, "luna_roja", Map.of(
					1, "Ha llegado la Luna Roja. De noche, los Zombies con pico pueden picar y construir.")),
			new Cambio(ZOMBI_CASCO, "zombi_casco", Map.of(
					1, "Los muertos se protegen del sol. Ahora los Zombis aparecen más seguido con casco.")),
			new Cambio(PERLAS_FALLAN, "perlas_fallan", Map.of(
					1, "Reviil desestabilizó las Perlas del End. Ahora cada perla tiene un 1% de fallar.")),
			new Cambio(MENOS_PERLAS, "menos_perlas", Map.of(
					1, "Los Endermen esconden sus perlas. Ahora sueltan Perlas del End un 20% menos.")),
			new Cambio(ZOMBI_PLANTA, "zombi_planta", Map.of(
					1, "Ahora los Zombies Planta aparecen en el bioma Forest.")),
			new Cambio(AZALEA_PLAINS, "azalea_plains", Map.of(
					1, "Ahora los Creepers Raíz de Azalea aparecen naturalmente en el bioma Plains (durante el día).")),
			new Cambio(PARASITOS_AGUA, "parasitos_agua", Map.of(
					1, "¡NO PUEDE SER POSIBLE! El día de ayer ALK4PON3OP trajo en su nave a un Parásito Glebanoide y comenzaron a reproducirse. Ahora aparecen en el agua del Overworld.")),
			new Cambio(ARANAS, "aranas", Map.of(
					1, "Ahora las Arañas de Cueva aparecen en el Overworld durante la Noche. Además, todas las arañas tienen Velocidad infinita.")),
			new Cambio(INVISIBILIDAD, "invisibilidad", Map.of(
					1, "Reviil ha hecho que todas las criaturas desarrollen su olfato. Ahora pueden detectar la presencia de entidades invisibles. La invisibilidad ya no sirve para esconderse de los mobs.")),
			new Cambio(ARANAS_TELAS, "aranas_telas", Map.of(
					1, "Ahora las Arañas y Arañas de Cueva dejan una Tela de Araña al golpearte y al morir.")),
			new Cambio(FUEGO_ARMADURA, "fuego_armadura", Map.of(
					1, "Ahora debes tener las cuatro piezas de la Armadura mientras estés en el Nether, de lo contrario recibirás Fuego Infinito.")),
			new Cambio(FUEGO_NETHER, "fuego_nether", Map.of(
					1, "Si te quemas en el Nether el Fuego es Infinito.")),
			new Cambio(FUEGO_DANIO, "fuego_danio", Map.of(
					1, "El daño del Fuego y de la Lava han sido multiplicados x10.")),
			new Cambio(INTERACTUAR, "interactuar", Map.of(
					1, "Interactuar con Botones, Puertas o Placas de Presión ahora te hace 4 mil millones de millones de millones de millones de daño.")),
			new Cambio(RESPIRACION, "respiracion", Map.of(
					1, "Ahora pierdes la respiración 5 veces más rápido cuando te sumerges en agua y el daño por ahogamiento ha sido multiplicado x10.")),
			new Cambio(CARNE, "carne", Map.of(
					1, "¡Todos los tipos de Carne, Pollo y Pescado se pudren!")),
			new Cambio(MANZANAS, "manzanas", Map.of(
					1, "La Manzana de Oro y la Manzana Encantada ya no dan el efecto de Absorción."))
	);

	public static CambiosData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(CambiosData.FACTORY, "dedsafio4_cambios");
	}

	public static int nivel(MinecraftServer server, String cambio) {
		return data(server).nivel(cambio);
	}
}
