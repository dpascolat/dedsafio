package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	/** Cambio "arboles 1": las hojas (rotas o al desaparecer solas) no sueltan brotes. */
	@Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$sinBrotes(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
		if (!state.is(BlockTags.LEAVES)) return;
		if (Cambios.nivel(params.getLevel().getServer(), Cambios.ARBOLES) < 1) return;
		cir.setReturnValue(cir.getReturnValue().stream()
				.filter(stack -> !stack.is(ItemTags.SAPLINGS))
				.toList());
	}
}
