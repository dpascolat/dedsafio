package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** El cielo, las nubes y el clima propios de una dimensión (los usa LevelRendererCieloMixin). */
public final class DimensionRenderingRegistry {
	private DimensionRenderingRegistry() {}

	public interface SkyRenderer {
		void render(WorldRenderContext contexto);
	}

	public interface CloudRenderer {
		void render(WorldRenderContext contexto);
	}

	public interface WeatherRenderer {
		void render(WorldRenderContext contexto);
	}

	public static final Map<ResourceKey<Level>, SkyRenderer> CIELOS = new HashMap<>();
	public static final Map<ResourceKey<Level>, CloudRenderer> NUBES = new HashMap<>();
	public static final Map<ResourceKey<Level>, WeatherRenderer> CLIMAS = new HashMap<>();

	public static void registerSkyRenderer(ResourceKey<Level> dimension, SkyRenderer r) {
		CIELOS.put(dimension, r);
	}

	public static void registerCloudRenderer(ResourceKey<Level> dimension, CloudRenderer r) {
		NUBES.put(dimension, r);
	}

	public static void registerWeatherRenderer(ResourceKey<Level> dimension, WeatherRenderer r) {
		CLIMAS.put(dimension, r);
	}
}
