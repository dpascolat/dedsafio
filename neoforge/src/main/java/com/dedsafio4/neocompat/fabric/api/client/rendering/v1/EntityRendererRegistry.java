package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public final class EntityRendererRegistry {
	private EntityRendererRegistry() {}

	public record Registro<E extends Entity>(EntityType<? extends E> tipo, EntityRendererProvider<E> fabrica) {}

	public static final List<Registro<?>> REGISTROS = new ArrayList<>();

	public static <E extends Entity> void register(EntityType<? extends E> tipo, EntityRendererProvider<E> fabrica) {
		REGISTROS.add(new Registro<>(tipo, fabrica));
	}
}
