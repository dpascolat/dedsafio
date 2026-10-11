package com.dedsafio4.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Con cualquier espada los bloques de Gleba (tag dedsafio4:bloques_gleba: carne, músculo, grasa, gelatina...) se rompen
 * más rápido: tan rápido como con un pico (o hacha, o pala) del mismo material (madera 2, piedra 4, hierro 6, diamante 8,
 * netherite 9, oro 12). Los demás bloques (la piedra del Overworld, etc.) se rompen como siempre. Lo que suelta el
 * bloque no cambia.
 */
@Mixin(ItemStack.class)
public abstract class EspadaRompeMixin {
	@org.spongepowered.asm.mixin.Unique
	private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BLOQUES_GLEBA = net.minecraft.tags.TagKey.create(
			net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "bloques_gleba"));

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$espadaRapida(BlockState estado, CallbackInfoReturnable<Float> cir) {
		ItemStack pila = (ItemStack) (Object) this;
		if (!pila.is(net.minecraft.tags.ItemTags.SWORDS) || !(pila.getItem() instanceof TieredItem conMaterial)) return;
		if (!estado.is(BLOQUES_GLEBA)) return;
		float velocidad = conMaterial.getTier().getSpeed();
		if (velocidad > cir.getReturnValueF()) cir.setReturnValue(velocidad);
	}
}
