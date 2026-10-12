package com.dedsafio4.client.mixin;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para mostrar en la descripción qué va en cada lugar de una receta de la mesa de herrería. */
@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformAccessor {
	@Accessor("template")
	Ingredient dedsafio4$plantilla();

	@Accessor("base")
	Ingredient dedsafio4$base();

	@Accessor("addition")
	Ingredient dedsafio4$agregado();
}
