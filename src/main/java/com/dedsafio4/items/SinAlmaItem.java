package com.dedsafio4.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Sin Alma y Alma: con click derecho abren el catálogo (la G), igual que la tecla. */
public class SinAlmaItem extends Item {
	/** Lo pone el cliente al arrancar: abre la pantalla de la G (el servidor no abre pantallas). */
	public static Runnable abrirCatalogo;

	public SinAlmaItem(Properties propiedades) {
		super(propiedades);
	}

	/** El Alma tiene el nombre en verde y dice para qué sirve. */
	@Override
	public Component getName(ItemStack pila) {
		Component nombre = super.getName(pila);
		return this == ModItems.ALMA ? nombre.copy().withColor(0x55FF55) : nombre;
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, java.util.List<Component> texto,
								net.minecraft.world.item.TooltipFlag bandera) {
		if (this != ModItems.ALMA) return;
		int blanco = 0xE8E8E8, naranja = 0xE8A33C;
		texto.add(Component.literal("Puedes utilizar tu Alma para").withColor(blanco));
		texto.add(Component.literal("revivir a otro jugador con una").withColor(blanco));
		texto.add(Component.literal("Cuchara de la Resurrección").withStyle(s -> s.withColor(naranja).withBold(true)));
		texto.add(Component.literal("en la Fogata de la Montaña.").withColor(blanco));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level mundo, Player jugador, InteractionHand mano) {
		if (mundo.isClientSide && abrirCatalogo != null) abrirCatalogo.run();
		return InteractionResultHolder.sidedSuccess(jugador.getItemInHand(mano), mundo.isClientSide);
	}
}
