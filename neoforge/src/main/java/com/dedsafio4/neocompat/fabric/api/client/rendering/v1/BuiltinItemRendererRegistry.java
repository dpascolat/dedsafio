package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ítems que se dibujan con código (el modelo del ítem tiene que tener "parent": "builtin/entity"). */
public final class BuiltinItemRendererRegistry {
	public static final BuiltinItemRendererRegistry INSTANCE = new BuiltinItemRendererRegistry();

	public interface DynamicItemRenderer {
		void render(ItemStack pila, ItemDisplayContext modo, PoseStack pose, MultiBufferSource buffers, int luz, int overlay);
	}

	public final Map<Item, DynamicItemRenderer> DIBUJANTES = new LinkedHashMap<>();

	public void register(ItemLike item, DynamicItemRenderer dibujante) {
		DIBUJANTES.put(item.asItem(), dibujante);
	}
}
