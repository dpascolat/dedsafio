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
	/** magma 1: tocar un Bloque de Magma de cualquier lado te mata al instante. */
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

	/** Tipo de daño propio del mod para la Perla del End: no lo frena la armadura ni sus encantamientos. */
	public static final ResourceKey<DamageType> DANIO_ENDERPERLA = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "enderperla"));

	/** Tipo de daño del Bloque de Magma con el cambio activo. */
	public static final ResourceKey<DamageType> DANIO_MAGMA = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "magma"));

	public static DamageSource danioMagma(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO_MAGMA));
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
					1, "Reviil hirvió las profundidades. Ahora tocar un Bloque de Magma, de CUALQUIER lado, te mata al instante.")),
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
					1, "Los esqueletos Stray pueden salir durante la noche en el Overworld."))
	);

	public static CambiosData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(CambiosData.FACTORY, "dedsafio4_cambios");
	}

	public static int nivel(MinecraftServer server, String cambio) {
		return data(server).nivel(cambio);
	}
}
