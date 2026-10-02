package com.dedsafio4.neocompat.fabric.api.object.builder.v1.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Anota los atributos de cada bicho; Puente los da en EntityAttributeCreationEvent. */
public final class FabricDefaultAttributeRegistry {
	private FabricDefaultAttributeRegistry() {}

	public static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> ATRIBUTOS = new LinkedHashMap<>();

	public static void register(EntityType<? extends LivingEntity> tipo, AttributeSupplier.Builder atributos) {
		register(tipo, atributos.build());
	}

	public static void register(EntityType<? extends LivingEntity> tipo, AttributeSupplier atributos) {
		ATRIBUTOS.put(tipo, atributos);
	}
}
