package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/** Como el de Fabric: los colores de los ítems se anotan acá y PuenteCliente los registra en RegisterColorHandlersEvent. */
public final class ColorProviderRegistry<T, P> {
	public record Registro<T, P>(P proveedor, T[] cosas) {}

	public static final ColorProviderRegistry<ItemLike, ItemColor> ITEM = new ColorProviderRegistry<>();

	public final List<Registro<T, P>> registros = new ArrayList<>();

	private ColorProviderRegistry() {}

	@SafeVarargs
	public final void register(P proveedor, T... cosas) {
		registros.add(new Registro<>(proveedor, cosas));
	}
}
