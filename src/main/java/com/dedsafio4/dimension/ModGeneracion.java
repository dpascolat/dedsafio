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

	/** Lo que hay en el Limbo: árboles muertos, columnas, tumbas, faroles, arcos y ruinas. */
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> LIMBO_ARBOL =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_arbol"),
					new LimboFeature(LimboFeature.Tipo.ARBOL));
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> LIMBO_COLUMNA =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_columna"),
					new LimboFeature(LimboFeature.Tipo.COLUMNA));
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> LIMBO_TUMBA =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_tumba"),
					new LimboFeature(LimboFeature.Tipo.TUMBA));
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> LIMBO_FAROL =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_farol"),
					new LimboFeature(LimboFeature.Tipo.FAROL));
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> LIMBO_ARCO =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_arco"),
					new LimboFeature(LimboFeature.Tipo.ARCO));
	public static final Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> LIMBO_RUINA =
			Registry.register(BuiltInRegistries.FEATURE, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_ruina"),
					new LimboFeature(LimboFeature.Tipo.RUINA));

	/** Fuerza la carga de la clase. */
	public static void registrar() {}
}
