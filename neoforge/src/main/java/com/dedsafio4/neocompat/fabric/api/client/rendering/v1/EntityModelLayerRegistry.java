package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityModelLayerRegistry {
	private EntityModelLayerRegistry() {}

	public interface TexturedModelDataProvider {
		LayerDefinition createModelData();
	}

	public static final Map<ModelLayerLocation, TexturedModelDataProvider> CAPAS = new LinkedHashMap<>();

	public static void registerModelLayer(ModelLayerLocation capa, TexturedModelDataProvider proveedor) {
		CAPAS.put(capa, proveedor);
	}
}
