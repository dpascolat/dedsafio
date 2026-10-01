package com.dedsafio4.dactylos;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/** El Dáctylo Bebé, su tinta y el efecto "Tinta" (la visión tapada). */
public final class ModDactylos {
	private ModDactylos() {}

	/** Mide unos 2 bloques de punta a punta de las alas. */
	public static final EntityType<DactyloBebeEntity> DACTYLO_BEBE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("dactylo_bebe"), EntityType.Builder.<DactyloBebeEntity>of(DactyloBebeEntity::new, MobCategory.MONSTER)
					.sized(0.9f, 0.7f).clientTrackingRange(10).build("dactylo_bebe"));

	public static final EntityType<TintaDactyloEntity> TINTA_ENTIDAD = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("tinta_dactylo"), EntityType.Builder.<TintaDactyloEntity>of(TintaDactyloEntity::new, MobCategory.MISC)
					.sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(2).build("tinta_dactylo"));

	/** Tinta: la pantalla se oscurece (lo dibuja el cliente). */
	public static final Holder<MobEffect> EFECTO_TINTA = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			id("tinta"), new EfectoTinta());

	private static final class EfectoTinta extends MobEffect {
		EfectoTinta() {
			super(MobEffectCategory.HARMFUL, 0x3A2A4A);
		}
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(DACTYLO_BEBE, DactyloBebeEntity.crearAtributos());
		SpawnPlacements.register(DACTYLO_BEBE, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
				DactyloBebeEntity::puedeAparecer);
	}
}
