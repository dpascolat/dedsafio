package com.dedsafio4.client.mixin;

import com.dedsafio4.dimension.Limbo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * La descripción del Tridente del Limbo: abajo del nombre "Puedes utilizarlo únicamente en el Limbo." y, si no estás
 * en el Limbo, al final (antes de la durabilidad) "No se puede usar en este mundo" en rojo.
 */
@Mixin(ItemStack.class)
public abstract class TridenteTooltipMixin {
	@Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
	private void dedsafio4$descripcion(Item.TooltipContext contexto, Player jugador, TooltipFlag bandera,
									   CallbackInfoReturnable<List<Component>> cir) {
		ItemStack pila = (ItemStack) (Object) this;
		if (!pila.is(Items.TRIDENT)) return;
		List<Component> lineas = new ArrayList<>(cir.getReturnValue());
		int despuesDelNombre = Math.min(1, lineas.size());
		lineas.add(despuesDelNombre, Component.empty());
		lineas.add(despuesDelNombre, Component.literal("únicamente en el Limbo.").withColor(0xE8E8E8));
		lineas.add(despuesDelNombre, Component.literal("Puedes utilizarlo").withColor(0xE8E8E8));
		lineas.add(despuesDelNombre, Component.empty());
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && !mc.level.dimension().equals(Limbo.DIMENSION)) {
			int donde = lineas.size();
			for (int i = 0; i < lineas.size(); i++) {
				if (lineas.get(i).getContents() instanceof TranslatableContents t && t.getKey().equals("item.durability")) donde = i;
			}
			lineas.add(donde, Component.literal("No se puede usar en este mundo").withColor(0xFF5555));
			lineas.add(donde, Component.empty());
		}
		cir.setReturnValue(lineas);
	}
}
