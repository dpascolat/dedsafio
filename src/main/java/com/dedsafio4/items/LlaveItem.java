package com.dedsafio4.items;

import com.dedsafio4.robots.ModRobots;
import com.dedsafio4.robots.RobotDormidoBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Llave de Cobre: con click derecho sobre un Aldeano Phora dormido, lo activa. Se gasta al usarla. */
public class LlaveItem extends Item {
	/** Colores de la imagen: nombre celeste, texto gris claro, "Click Derecho" naranja y "Aldeano Phora" violeta. */
	private static final int CELESTE = 0x55C6E8, TEXTO = 0xC6C6C6, NARANJA = 0xF2B35A, VIOLETA = 0xD65AF2;

	public LlaveItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId(pila)).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(parte("Haz ", TEXTO).append(parte("Click Derecho", NARANJA)).append(parte(" sobre", TEXTO)));
		texto.add(parte("un ", TEXTO).append(parte("Aldeano Phora", VIOLETA)).append(parte(" para", TEXTO)));
		texto.add(parte("activarlo.", TEXTO));
		texto.add(Component.empty());
		texto.add(parte("Se consume al utilizarla.", TEXTO));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}

	@Override
	public InteractionResult useOn(UseOnContext contexto) {
		if (!contexto.getLevel().getBlockState(contexto.getClickedPos()).is(ModRobots.ROBOT_DORMIDO)) {
			return InteractionResult.PASS;
		}
		if (!contexto.getLevel().isClientSide) {
			RobotDormidoBlock.despertar(contexto.getLevel(), contexto.getClickedPos());
			Player jugador = contexto.getPlayer();
			if (jugador == null || !jugador.getAbilities().instabuild) contexto.getItemInHand().shrink(1);
		}
		return InteractionResult.sidedSuccess(contexto.getLevel().isClientSide);
	}
}
