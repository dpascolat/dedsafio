package com.dedsafio4.dimension;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Lo que agrega el mod a la generación del mundo. */
public final class ModGeneracion {
	private ModGeneracion() {}

	public static final Feature<ArbolAltoFeature.Config> ARBOL_ALTO = Registry.register(BuiltInRegistries.FEATURE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "arbol_alto"), new ArbolAltoFeature());

	/** Los árboles de gelatina de la Dimensión de los Órganos. */
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> ARBOL_ORGANO =
			Registry.register(BuiltInRegistries.FEATURE,
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "arbol_organo"), new ArbolOrganoFeature());

	/** Ya no se usa en la generación, pero los mundos creados antes lo tienen guardado. */
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> CIRCULO_PORTAL =
			Registry.register(BuiltInRegistries.FEATURE,
					ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "circulo_portal"), new CirculoPortalFeature());

	/** Los Pulmones en las minas de Gleba. */
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> PULMONES =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "pulmones"), new PulmonesFeature());

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
