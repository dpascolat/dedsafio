package com.dedsafio4.bloques;

import com.dedsafio4.catalogo.Misiones;
import com.dedsafio4.correo.Correo;
import com.dedsafio4.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Entrega de Misiones: un bloque invisible pero sólido (como la barrera: se choca con él, no hace sombra en el piso y
 * se pone y se saca en creativo). Al tocarlo con la
 * Dedita de la Misión en la mano, se la queda y Eón te manda un mensaje al Buzón ("Misión diaria") con el premio
 * que se eligió para esa Misión Principal en el editor de Misiones.
 */
public class EntregaMisionBlock extends Block {
	/** Quién manda el mensaje. */
	public static final String REMITENTE = "Eón";

	public EntregaMisionBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
	}

	/** Deja pasar la luz del cielo (si no, el piso de abajo queda oscuro). */
	@Override
	protected boolean propagatesSkylightDown(BlockState estado, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
		return true;
	}

	/** Sin la sombrita en los bloques de al lado. */
	@Override
	protected float getShadeBrightness(BlockState estado, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
		return 1f;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack pila, BlockState estado, Level level, BlockPos pos, Player jugador,
											  InteractionHand mano, BlockHitResult golpe) {
		if (!pila.is(ModItems.DEDITA_MISION)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!(jugador instanceof ServerPlayer servidor)) return ItemInteractionResult.SUCCESS;
		Misiones.Mision mision = Misiones.deDedita(servidor.server, pila);
		int dia = mision != null ? mision.dia() : Misiones.diaDeDedita(pila);
		List<ItemStack> premio = mision != null ? mision.premio() : List.of();
		String texto = (dia >= 0 ? "¡Completaste la Misión Principal del Día " + dia + "!" : "¡Completaste una Misión Principal!")
				+ (premio.isEmpty() ? " Gracias por entregar tu Dedita de la Misión." : " Aquí tienes tu premio.");
		pila.consume(1, jugador);
		Correo.mandar(servidor, REMITENTE, "Misión diaria", texto, premio);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (!level.isClientSide) {
			jugador.displayClientMessage(Component.literal("Trae tu Dedita de la Misión para recibir el premio.").withColor(0x55FFFF), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
