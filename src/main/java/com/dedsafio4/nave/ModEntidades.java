package com.dedsafio4.nave;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.SoarerEntity;
import com.dedsafio4.bestias.WalkerEntity;
import com.dedsafio4.bombas.BombaWardenEntity;
import com.dedsafio4.robots.AldeanoRobotEntity;
import com.dedsafio4.lagartos.LagartoEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntidades {
	private ModEntidades() {}

	/** Unos 1,5 bloques de largo y 1 de alto (el modelo se dibuja a su tamaño de Blockbench). */
	public static final EntityType<NaveEntity> NAVE = registrar("nave",
			EntityType.Builder.of(NaveEntity::new, MobCategory.MONSTER).sized(1.3f, 1.0f).clientTrackingRange(10));

	/** Nave Veloz (el modelo "Nave 2", dibujado al doble): más o menos 2 bloques de ancho y 1,4 de alto. */
	public static final EntityType<NaveVelozEntity> NAVE_VELOZ = registrar("nave_veloz",
			EntityType.Builder.of(NaveVelozEntity::new, MobCategory.MONSTER).sized(2.2f, 1.5f).clientTrackingRange(10));

	/** Bomba Warden: un poco más chica que un bloque de alto, con las alas hacia los costados. */
	public static final EntityType<BombaWardenEntity> BOMBA_WARDEN = registrar("bomba_warden",
			EntityType.Builder.of(BombaWardenEntity::new, MobCategory.MONSTER).sized(1.0f, 1.3f).clientTrackingRange(8));

	/** Soarer: la bestia voladora, los nuevos Phantoms (el modelo mide unos 7 bloques de punta a punta de las alas). */
	public static final EntityType<SoarerEntity> SOARER = registrar("soarer",
			EntityType.Builder.of(SoarerEntity::new, MobCategory.MONSTER).sized(2.6f, 3.0f).clientTrackingRange(12));

	// Los cuatro lagartos: mismo cuerpo, distintos colores y adornos.
	/** Reptisaurio Salvaje (id "mira"): el chiquito con caparazón que se entierra. */
	public static final EntityType<com.dedsafio4.bestias.ReptisaurioEntity> MIRA = registrar("mira",
			EntityType.Builder.of(com.dedsafio4.bestias.ReptisaurioEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 1.0f).clientTrackingRange(10));

	public static final EntityType<LagartoEntity> REPTISAURIO_GUERRERO = registrar("reptisaurio_guerrero",
			EntityType.Builder.<LagartoEntity>of((tipo, level) -> new LagartoEntity(tipo, level, LagartoEntity.Variante.REPTISAURIO_GUERRERO), MobCategory.CREATURE)
					.sized(1.2f, 2.0f).clientTrackingRange(10));

	public static final EntityType<LagartoEntity> REPTISAURIO_ARQUERO = registrar("reptisaurio_arquero",
			EntityType.Builder.<LagartoEntity>of((tipo, level) -> new LagartoEntity(tipo, level, LagartoEntity.Variante.REPTISAURIO_ARQUERO), MobCategory.CREATURE)
					.sized(1.2f, 2.0f).clientTrackingRange(10));

	/** La lanza que tira el Reptisaurio con Lanza. */
	public static final EntityType<com.dedsafio4.lagartos.LanzaLanzadaEntity> LANZA_LANZADA = registrar("lanza_lanzada",
			EntityType.Builder.<com.dedsafio4.lagartos.LanzaLanzadaEntity>of(com.dedsafio4.lagartos.LanzaLanzadaEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f).clientTrackingRange(6).updateInterval(20));

	public static final EntityType<LagartoEntity> REPTISAURIO_LANZA = registrar("reptisaurio_lanza",
			EntityType.Builder.<LagartoEntity>of((tipo, level) -> new LagartoEntity(tipo, level, LagartoEntity.Variante.REPTISAURIO_LANZA), MobCategory.CREATURE)
					.sized(1.2f, 2.0f).clientTrackingRange(10));

	/** Plumosaurio (id "walker"): la bestia de seis patas, unos 3,5 bloques de ancho y 9 de alto. */
	public static final EntityType<WalkerEntity> WALKER = registrar("walker",
			EntityType.Builder.of(WalkerEntity::new, MobCategory.CREATURE).sized(3.5f, 9.0f).clientTrackingRange(16));

	/** Aldeano Robot: flota, así que se dibuja un poco más arriba de lo que ocupa. */
	public static final EntityType<AldeanoRobotEntity> ALDEANO_ROBOT = registrar("aldeano_robot",
			EntityType.Builder.of(AldeanoRobotEntity::new, MobCategory.MISC).sized(0.7f, 1.9f).clientTrackingRange(10));

	/** Creeper de Pasto: 1,5 veces 1/4 de un creeper normal (0,64 bloques de alto). */
	/** Creeper Amarillo: el creeper de siempre, amarillo clarito. */
	public static final EntityType<com.dedsafio4.bestias.CreeperAmarilloEntity> CREEPER_AMARILLO = registrar("creeper_amarillo",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperAmarilloEntity::new, MobCategory.MONSTER).sized(0.6f, 1.7f).clientTrackingRange(8));

	/** Los zombis nuevos (común, del desierto y de nieve): ver ZombieDedsafioEntity. */
	public static final EntityType<com.dedsafio4.bestias.ZombieDedsafioEntity> ZOMBIE_COMUN = registrar("zombie_comun",
			EntityType.Builder.<com.dedsafio4.bestias.ZombieDedsafioEntity>of((tipo, level) -> new com.dedsafio4.bestias.ZombieDedsafioEntity(tipo, level, com.dedsafio4.bestias.ZombieDedsafioEntity.Tipo.COMUN), MobCategory.MONSTER)
					.sized(0.6f, 1.95f).clientTrackingRange(8));
	public static final EntityType<com.dedsafio4.bestias.ZombieDedsafioEntity> ZOMBIE_DESIERTO = registrar("zombie_desierto",
			EntityType.Builder.<com.dedsafio4.bestias.ZombieDedsafioEntity>of((tipo, level) -> new com.dedsafio4.bestias.ZombieDedsafioEntity(tipo, level, com.dedsafio4.bestias.ZombieDedsafioEntity.Tipo.DESIERTO), MobCategory.MONSTER)
					.sized(0.6f, 1.95f).clientTrackingRange(8));
	public static final EntityType<com.dedsafio4.bestias.ZombieDedsafioEntity> ZOMBIE_NIEVE = registrar("zombie_nieve",
			EntityType.Builder.<com.dedsafio4.bestias.ZombieDedsafioEntity>of((tipo, level) -> new com.dedsafio4.bestias.ZombieDedsafioEntity(tipo, level, com.dedsafio4.bestias.ZombieDedsafioEntity.Tipo.NIEVE), MobCategory.MONSTER)
					.sized(0.6f, 1.95f).clientTrackingRange(8));

	/** Creeper de Papel: una solicitud de empleo con patas, del alto de un creeper. */
	public static final EntityType<com.dedsafio4.bestias.CreeperPapelEntity> CREEPER_PAPEL = registrar("creeper_papel",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperPapelEntity::new, MobCategory.MONSTER).sized(0.8f, 1.7f).clientTrackingRange(8));

	public static final EntityType<com.dedsafio4.bestias.CreeperPastoEntity> CREEPER_PASTO = registrar("creeper_pasto",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperPastoEntity::new, MobCategory.MONSTER).sized(0.45f, 0.65f).clientTrackingRange(8));

	/** 5x5 píxeles de sección. */
	public static final EntityType<RayoEntity> RAYO = registrar("rayo",
			EntityType.Builder.<RayoEntity>of(RayoEntity::new, MobCategory.MISC).sized(0.3125f, 0.3125f)
					.clientTrackingRange(8).updateInterval(1));

	/** Dromoraptor Rojo (el raptor naranja): 1,6 de alto y ~1,8 de largo con la cola. */
	public static final EntityType<com.dedsafio4.bestias.RaptorEntity> DROMORAPTOR_ROJO = registrar("dromoraptor_rojo",
			EntityType.Builder.of(com.dedsafio4.bestias.RaptorEntity::new, MobCategory.MONSTER).sized(0.9f, 1.6f).clientTrackingRange(10));
	/** Dromoraptor Azul (el raptor turquesa, de brazos largos). */
	public static final EntityType<com.dedsafio4.bestias.RaptorEntity> DROMORAPTOR_AZUL = registrar("dromoraptor_azul",
			EntityType.Builder.of(com.dedsafio4.bestias.RaptorEntity::new, MobCategory.MONSTER).sized(0.9f, 1.6f).clientTrackingRange(10));
	/** Creeper Nuclear: la bomba andante (12 px de ancho y ~19 de alto con el botón). */
	public static final EntityType<com.dedsafio4.bestias.CreeperNuclearEntity> CREEPER_NUCLEAR = registrar("creeper_nuclear",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperNuclearEntity::new, MobCategory.MONSTER).sized(0.8f, 1.2f).clientTrackingRange(8));

	/** Creeper Pastel: 1 bloque de alto. */
	public static final EntityType<com.dedsafio4.bestias.CreeperPastelEntity> CREEPER_PASTEL = registrar("creeper_pastel",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperPastelEntity::new, MobCategory.MONSTER).sized(0.8f, 1.0f).clientTrackingRange(8));
	/** Zarinosa Espigüeya: ~10 px de alto y ~24 de largo con la cola. */
	public static final EntityType<com.dedsafio4.bestias.ZarinosaEntity> ZARINOSA = registrar("zarinosa_espigueya",
			EntityType.Builder.of(com.dedsafio4.bestias.ZarinosaEntity::new, MobCategory.MONSTER).sized(0.6f, 0.6f).clientTrackingRange(8));

	/** Creeper Raíz de Azalea: 1,5 bloques de alto con la flor. */
	public static final EntityType<com.dedsafio4.bestias.CreeperAzaleaEntity> CREEPER_AZALEA = registrar("creeper_raiz_azalea",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperAzaleaEntity::new, MobCategory.MONSTER).sized(0.8f, 1.5f).clientTrackingRange(8));
	/** Las raíces que atrapan al jugador (las crea el Creeper Raíz de Azalea al explotar). */
	public static final EntityType<com.dedsafio4.bestias.RaicesEntity> RAICES = registrar("raices",
			EntityType.Builder.<com.dedsafio4.bestias.RaicesEntity>of(com.dedsafio4.bestias.RaicesEntity::new, MobCategory.MISC)
					.sized(1.2f, 1.6f).clientTrackingRange(8).updateInterval(2));
	/** Zombie Plata: tamaño de zombie. */
	public static final EntityType<com.dedsafio4.bestias.ZombiePlataEntity> ZOMBIE_PLATA = registrar("zombie_plata",
			EntityType.Builder.of(com.dedsafio4.bestias.ZombiePlataEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8));

	/** Nautilus Óseo (pequeño): vuela; la caja de choque es chica aunque el caparazón y los tentáculos son largos. */
	public static final EntityType<com.dedsafio4.bestias.NautilusOseoEntity> NAUTILUS_OSEO = registrar("nautilus_oseo",
			EntityType.Builder.of(com.dedsafio4.bestias.NautilusOseoEntity::new, MobCategory.MONSTER).sized(0.7f, 0.7f).clientTrackingRange(8));
	/** Parásito Volador: sale de las Verrugas de Gleba al romperlas; chiquito y muy rápido. */
	public static final EntityType<com.dedsafio4.bestias.ParasitoVoladorEntity> PARASITO_VOLADOR = registrar("parasito_volador",
			EntityType.Builder.of(com.dedsafio4.bestias.ParasitoVoladorEntity::new, MobCategory.MONSTER).sized(0.9f, 0.9f).clientTrackingRange(8));
	/** Nautilus Óseo Grande: vuela y embiste; la caja de choque abarca el caparazón y los tentáculos que cuelgan. */
	public static final EntityType<com.dedsafio4.bestias.NautilusOseoGrandeEntity> NAUTILUS_OSEO_GRANDE = registrar("nautilus_oseo_grande",
			EntityType.Builder.of(com.dedsafio4.bestias.NautilusOseoGrandeEntity::new, MobCategory.MONSTER).sized(2.1f, 3.1f).clientTrackingRange(10));

	/** Cerebro Amarillo: una medusa-cerebro de algo más de un bloque. */
	public static final EntityType<com.dedsafio4.bestias.CerebroAmarilloEntity> CEREBRO_AMARILLO = registrar("cerebro_amarillo",
			EntityType.Builder.of(com.dedsafio4.bestias.CerebroAmarilloEntity::new, MobCategory.MONSTER).sized(0.9f, 1.2f).clientTrackingRange(8));

	/** Garrapata Cerebral Pequeña: el mismo cuerpo de medusa-cerebro que el Cerebro Amarillo. */
	public static final EntityType<com.dedsafio4.bestias.GarrapataCerebralEntity> GARRAPATA_CEREBRAL = registrar("garrapata_cerebral",
			EntityType.Builder.of(com.dedsafio4.bestias.GarrapataCerebralEntity::new, MobCategory.MONSTER).sized(0.9f, 1.2f).clientTrackingRange(8));

	/** Flashbang: flaquito y alto (casi 3 bloques con la cabeza). */
	public static final EntityType<com.dedsafio4.bestias.FlashbangEntity> FLASHBANG = registrar("flashbang",
			EntityType.Builder.of(com.dedsafio4.bestias.FlashbangEntity::new, MobCategory.MONSTER).sized(0.7f, 2.9f).clientTrackingRange(8));

	/** Los fantasmas del Limbo (vuelan). */
	public static final EntityType<com.dedsafio4.bestias.FantasmaEntity> FANTASMA_AMARILLO = registrar("fantasma_amarillo",
			EntityType.Builder.of(com.dedsafio4.bestias.FantasmaEntity::new, MobCategory.MONSTER).sized(0.8f, 1.9f).clientTrackingRange(8));
	public static final EntityType<com.dedsafio4.bestias.FantasmaEntity> FANTASMA_BLANCO = registrar("fantasma_blanco",
			EntityType.Builder.of(com.dedsafio4.bestias.FantasmaEntity::new, MobCategory.MONSTER).sized(0.8f, 1.9f).clientTrackingRange(8));
	public static final EntityType<com.dedsafio4.bestias.FantasmaEntity> FANTASMA_ROJO = registrar("fantasma_rojo",
			EntityType.Builder.of(com.dedsafio4.bestias.FantasmaEntity::new, MobCategory.MONSTER).sized(0.8f, 1.9f).clientTrackingRange(8));
	public static final EntityType<com.dedsafio4.bestias.FantasmaEntity> FANTASMA_NEGRO = registrar("fantasma_negro",
			EntityType.Builder.of(com.dedsafio4.bestias.FantasmaEntity::new, MobCategory.MONSTER).sized(0.8f, 1.9f).clientTrackingRange(8));

	/** Tung Tung Tung Sahur (del mod italian_brainrots_ft): sobre todo para /cambiarmob. */
	public static final EntityType<com.dedsafio4.bestias.TungSahurEntity> TUNG_TUNG_TUNG_SAHUR = registrar("tung_tung_tung_sahur",
			EntityType.Builder.of(com.dedsafio4.bestias.TungSahurEntity::new, MobCategory.CREATURE).sized(0.6f, 2.1f).clientTrackingRange(8));

	/** Hornet (de Silksong): sobre todo para /cambiarmob. Unos 2 bloques de alto. */
	public static final EntityType<com.dedsafio4.bestias.HornetEntity> HORNET = registrar("hornet",
			EntityType.Builder.of(com.dedsafio4.bestias.HornetEntity::new, MobCategory.CREATURE).sized(0.6f, 1.9f).clientTrackingRange(8));

	/** Rana Gigante (de Dedsafío 3): agarra con la lengua. Unos 2,4 bloques de ancho y 3,4 de alto. */
	public static final EntityType<com.dedsafio4.bestias.RanaGiganteEntity> RANA_GIGANTE = registrar("rana_gigante",
			EntityType.Builder.of(com.dedsafio4.bestias.RanaGiganteEntity::new, MobCategory.MONSTER).sized(2.4f, 3.4f).clientTrackingRange(10));

	/** Triceratops (del mod Fossils and Archeology): embiste cada 20 segundos rompiendo bloques. */
	public static final EntityType<com.dedsafio4.bestias.TriceratopsEntity> TRICERATOPS = registrar("triceratops",
			EntityType.Builder.of(com.dedsafio4.bestias.TriceratopsEntity::new, MobCategory.MONSTER).sized(2.2f, 2.4f).clientTrackingRange(10));

	/** Tremorsaurio (el Tremorsaurus de Alex's Caves): 2,5 de ancho y casi 4 de alto; cada mordida es una explosión. */
	public static final EntityType<com.dedsafio4.bestias.TremorsaurioEntity> TREMORSAURIO = registrar("tremorsaurio",
			EntityType.Builder.of(com.dedsafio4.bestias.TremorsaurioEntity::new, MobCategory.MONSTER).sized(2.5f, 3.85f).clientTrackingRange(10));

	/** Wraith: el espectro con la guadaña (2,5 bloques de alto, flota). */
	public static final EntityType<com.dedsafio4.bestias.WraithEntity> WRAITH = registrar("wraith",
			EntityType.Builder.of(com.dedsafio4.bestias.WraithEntity::new, MobCategory.MONSTER).sized(0.8f, 2.5f).clientTrackingRange(8));

	/** Almita: el alma de un jugador (Steve fantasma ángel). Flota, no ataca. */
	public static final EntityType<com.dedsafio4.bestias.AlmitaEntity> ALMITA = registrar("almita",
			EntityType.Builder.of(com.dedsafio4.bestias.AlmitaEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f).clientTrackingRange(10));

	/** Gusano de Carne: chiquito y largo (la caja de choque es la de la cabeza). */
	public static final EntityType<com.dedsafio4.bestias.GusanoCarneEntity> GUSANO_CARNE = registrar("gusano_carne",
			EntityType.Builder.of(com.dedsafio4.bestias.GusanoCarneEntity::new, MobCategory.MONSTER).sized(0.5f, 0.45f).clientTrackingRange(8));

	/** Narval: unos 3 bloques de largo con el cuerno. */
	public static final EntityType<com.dedsafio4.bestias.NarvalEntity> NARVAL = registrar("narval",
			EntityType.Builder.of(com.dedsafio4.bestias.NarvalEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE).sized(1.4f, 0.9f).clientTrackingRange(10));

	public static final java.util.List<EntityType<LagartoEntity>> LAGARTOS = java.util.List.of(REPTISAURIO_GUERRERO, REPTISAURIO_ARQUERO, REPTISAURIO_LANZA);

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registrar(String nombre, EntityType.Builder<T> builder) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(id.toString()));
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(NAVE, NaveEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(NAVE_VELOZ, NaveVelozEntity.crearAtributos());
		// En el bioma Phora, de día o de noche.
		SpawnPlacements.register(NAVE, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkAnyLightMonsterSpawnRules);
		SpawnPlacements.register(NAVE_VELOZ, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkAnyLightMonsterSpawnRules);
		FabricDefaultAttributeRegistry.register(BOMBA_WARDEN, BombaWardenEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(SOARER, SoarerEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(WALKER, WalkerEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(ALDEANO_ROBOT, AldeanoRobotEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(CREEPER_PASTO, net.minecraft.world.entity.monster.Creeper.createAttributes());
		FabricDefaultAttributeRegistry.register(ZOMBIE_COMUN, net.minecraft.world.entity.monster.Zombie.createAttributes());
		SpawnPlacements.register(ZOMBIE_COMUN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
		FabricDefaultAttributeRegistry.register(ZOMBIE_DESIERTO, net.minecraft.world.entity.monster.Zombie.createAttributes());
		SpawnPlacements.register(ZOMBIE_DESIERTO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
		FabricDefaultAttributeRegistry.register(ZOMBIE_NIEVE, net.minecraft.world.entity.monster.Zombie.createAttributes());
		SpawnPlacements.register(ZOMBIE_NIEVE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
		FabricDefaultAttributeRegistry.register(CREEPER_PAPEL, net.minecraft.world.entity.monster.Creeper.createAttributes());
		// De día, a cielo abierto (ver CreeperPapelEntity.puedeAparecer).
		SpawnPlacements.register(CREEPER_PAPEL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				com.dedsafio4.bestias.CreeperPapelEntity::puedeAparecer);
		FabricDefaultAttributeRegistry.register(CREEPER_AMARILLO, net.minecraft.world.entity.monster.Creeper.createAttributes());
		FabricDefaultAttributeRegistry.register(DROMORAPTOR_ROJO, com.dedsafio4.bestias.RaptorEntity.atributosDromoraptor());
		FabricDefaultAttributeRegistry.register(DROMORAPTOR_AZUL, com.dedsafio4.bestias.RaptorEntity.atributosDromoraptorAzul());
		// Dromoraptor Azul: en la Sabana del Centro de Quiu, de día o de noche.
		SpawnPlacements.register(DROMORAPTOR_AZUL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				net.minecraft.world.entity.Mob::checkMobSpawnRules);
		FabricDefaultAttributeRegistry.register(CREEPER_NUCLEAR, net.minecraft.world.entity.monster.Creeper.createAttributes());
		SpawnPlacements.register(CREEPER_NUCLEAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		FabricDefaultAttributeRegistry.register(CREEPER_PASTEL, net.minecraft.world.entity.monster.Creeper.createAttributes());
		// Creeper Pastel: en todo el Overworld, a oscuras (como los creepers).
		SpawnPlacements.register(CREEPER_PASTEL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		// Creeper Amarillo: en la Dimensión de los Órganos, con cualquier luz.
		SpawnPlacements.register(CREEPER_AMARILLO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkAnyLightMonsterSpawnRules);
		// Dromoraptor Rojo: en la Sabana del Centro de Quiu, de día o de noche.
		SpawnPlacements.register(DROMORAPTOR_ROJO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				net.minecraft.world.entity.Mob::checkMobSpawnRules);
		FabricDefaultAttributeRegistry.register(ZARINOSA, com.dedsafio4.bestias.ZarinosaEntity.crearAtributos());
		// Zarinosa: en las cuevas del Centro de Quiu.
		SpawnPlacements.register(ZARINOSA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				com.dedsafio4.bestias.ZarinosaEntity::puedeAparecerEnCueva);
		FabricDefaultAttributeRegistry.register(CREEPER_AZALEA, net.minecraft.world.entity.monster.Creeper.createAttributes());
		FabricDefaultAttributeRegistry.register(ZOMBIE_PLATA, com.dedsafio4.bestias.ZombiePlataEntity.crearAtributos());
		com.dedsafio4.bestias.RaicesEntity.registrar();
		FabricDefaultAttributeRegistry.register(NARVAL, com.dedsafio4.bestias.NarvalEntity.crearAtributos());
		// Narval: en el agua de las cuevas del Centro de Quiu (acuíferos).
		SpawnPlacements.register(NARVAL, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
				com.dedsafio4.bestias.NarvalEntity::puedeAparecer);
		for (EntityType<LagartoEntity> lagarto : LAGARTOS) {
			FabricDefaultAttributeRegistry.register(lagarto,
					lagarto == REPTISAURIO_GUERRERO ? LagartoEntity.atributosGuerrero() : LagartoEntity.crearAtributos());
		}
		// Aparece sola en la Oscuridad Profunda y en las cuevas, en la oscuridad, sin necesitar suelo (vuela).
		SpawnPlacements.register(BOMBA_WARDEN, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				BombaWardenEntity::puedeAparecer);
		// Plumosaurio: en el pasto de la jungla del Centro de Quiu.
		SpawnPlacements.register(WALKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				WalkerEntity::puedeAparecer);
		// Reptisaurio Salvaje: en las cuevas del Centro de Quiu (sin cielo y a oscuras).
		SpawnPlacements.register(MIRA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				com.dedsafio4.bestias.ReptisaurioEntity::puedeAparecerEnCueva);
		FabricDefaultAttributeRegistry.register(MIRA, com.dedsafio4.bestias.ReptisaurioEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(NAUTILUS_OSEO, com.dedsafio4.bestias.NautilusOseoEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(NAUTILUS_OSEO_GRANDE, com.dedsafio4.bestias.NautilusOseoGrandeEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(PARASITO_VOLADOR, com.dedsafio4.bestias.ParasitoVoladorEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(CEREBRO_AMARILLO, com.dedsafio4.bestias.CerebroAmarilloEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(GARRAPATA_CEREBRAL, com.dedsafio4.bestias.GarrapataCerebralEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(FLASHBANG, com.dedsafio4.bestias.FlashbangEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(ALMITA, com.dedsafio4.bestias.AlmitaEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(WRAITH, com.dedsafio4.bestias.WraithEntity.crearAtributos());
		// Wraith: en el Overworld, a oscuras.
		SpawnPlacements.register(WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		FabricDefaultAttributeRegistry.register(TUNG_TUNG_TUNG_SAHUR, com.dedsafio4.bestias.TungSahurEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(HORNET, com.dedsafio4.bestias.HornetEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(RANA_GIGANTE, com.dedsafio4.bestias.RanaGiganteEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(TRICERATOPS, com.dedsafio4.bestias.TriceratopsEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(TREMORSAURIO, com.dedsafio4.bestias.TremorsaurioEntity.crearAtributos());
		for (EntityType<com.dedsafio4.bestias.FantasmaEntity> fantasma : java.util.List.of(FANTASMA_AMARILLO, FANTASMA_BLANCO, FANTASMA_ROJO, FANTASMA_NEGRO)) {
			FabricDefaultAttributeRegistry.register(fantasma, com.dedsafio4.bestias.FantasmaEntity.crearAtributos());
			// En el Limbo, volando sobre el piso de piedra (allá siempre está oscuro).
			SpawnPlacements.register(fantasma, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					Monster::checkAnyLightMonsterSpawnRules);
		}
		FabricDefaultAttributeRegistry.register(GUSANO_CARNE, com.dedsafio4.bestias.GusanoCarneEntity.crearAtributos());
		com.dedsafio4.bestias.FlashbangEntity.registrar();
		// Flashbang: en el Limbito (bioma del Limbo), a oscuras, pero nunca sobre la piedra del piso del Limbo.
		SpawnPlacements.register(FLASHBANG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(tipo, mundo, razon, pos, azar) -> Monster.checkMonsterSpawnRules(tipo, mundo, razon, pos, azar)
						&& !mundo.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.STONE));
		// Garrapata Cerebral: en el piso de la Dimensión de los Órganos, con cualquier luz.
		SpawnPlacements.register(GARRAPATA_CEREBRAL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkAnyLightMonsterSpawnRules);
		// Cerebro Amarillo: en el piso de la Dimensión de los Órganos, con cualquier luz.
		SpawnPlacements.register(CEREBRO_AMARILLO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkAnyLightMonsterSpawnRules);
		// Nautilus Óseo: en la Dimensión de los Órganos, en grupos, volando (no necesita piso) y con cualquier luz.
		SpawnPlacements.register(NAUTILUS_OSEO, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				com.dedsafio4.bestias.NautilusOseoEntity::puedeAparecer);
		SpawnPlacements.register(NAUTILUS_OSEO_GRANDE, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				com.dedsafio4.bestias.NautilusOseoEntity::puedeAparecer);
	}
}
