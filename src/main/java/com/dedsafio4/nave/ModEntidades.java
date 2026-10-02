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
	public static final EntityType<LagartoEntity> MIRA = registrar("mira",
			EntityType.Builder.<LagartoEntity>of((tipo, level) -> new LagartoEntity(tipo, level, LagartoEntity.Variante.MIRA), MobCategory.MONSTER)
					.sized(1.2f, 2.0f).clientTrackingRange(10));

	public static final EntityType<LagartoEntity> REPTISAURIO_GUERRERO = registrar("reptisaurio_guerrero",
			EntityType.Builder.<LagartoEntity>of((tipo, level) -> new LagartoEntity(tipo, level, LagartoEntity.Variante.REPTISAURIO_GUERRERO), MobCategory.CREATURE)
					.sized(1.2f, 2.0f).clientTrackingRange(10));

	public static final EntityType<LagartoEntity> REPTISAURIO_ARQUERO = registrar("reptisaurio_arquero",
			EntityType.Builder.<LagartoEntity>of((tipo, level) -> new LagartoEntity(tipo, level, LagartoEntity.Variante.REPTISAURIO_ARQUERO), MobCategory.CREATURE)
					.sized(1.2f, 2.0f).clientTrackingRange(10));

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

	public static final EntityType<com.dedsafio4.bestias.CreeperPastoEntity> CREEPER_PASTO = registrar("creeper_pasto",
			EntityType.Builder.of(com.dedsafio4.bestias.CreeperPastoEntity::new, MobCategory.MONSTER).sized(0.45f, 0.65f).clientTrackingRange(8));

	/** 5x5 píxeles de sección. */
	public static final EntityType<RayoEntity> RAYO = registrar("rayo",
			EntityType.Builder.<RayoEntity>of(RayoEntity::new, MobCategory.MISC).sized(0.3125f, 0.3125f)
					.clientTrackingRange(8).updateInterval(1));

	public static final java.util.List<EntityType<LagartoEntity>> LAGARTOS = java.util.List.of(MIRA, REPTISAURIO_GUERRERO, REPTISAURIO_ARQUERO, REPTISAURIO_LANZA);

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registrar(String nombre, EntityType.Builder<T> builder) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(id.toString()));
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(NAVE, NaveEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(NAVE_VELOZ, NaveVelozEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(BOMBA_WARDEN, BombaWardenEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(SOARER, SoarerEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(WALKER, WalkerEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(ALDEANO_ROBOT, AldeanoRobotEntity.crearAtributos());
		FabricDefaultAttributeRegistry.register(CREEPER_PASTO, net.minecraft.world.entity.monster.Creeper.createAttributes());
		FabricDefaultAttributeRegistry.register(CREEPER_AMARILLO, net.minecraft.world.entity.monster.Creeper.createAttributes());
		for (EntityType<LagartoEntity> lagarto : LAGARTOS) {
			FabricDefaultAttributeRegistry.register(lagarto,
					lagarto == REPTISAURIO_GUERRERO ? LagartoEntity.atributosGuerrero() : LagartoEntity.crearAtributos());
		}
		// Aparece sola en la Oscuridad Profunda, en la oscuridad, sin necesitar suelo (vuela).
		SpawnPlacements.register(BOMBA_WARDEN, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		// Plumosaurio: en el pasto de la jungla del Centro de Quiu.
		SpawnPlacements.register(WALKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				WalkerEntity::puedeAparecer);
		// Reptisaurio Salvaje: en las cuevas del Centro de Quiu (sin cielo y a oscuras).
		SpawnPlacements.register(MIRA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				LagartoEntity::puedeAparecerEnCueva);
	}
}
