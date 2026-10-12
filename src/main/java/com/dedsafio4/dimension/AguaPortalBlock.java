package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El Agua Rara: se ve como el agua. Si entras te lleva a la otra dimensión (lo maneja {@link Portales}: tarda 3
 * segundos, con la pantalla que se pone negra). Como el agua, el mouse la atraviesa: no se puede romper ni agarrar,
 * salvo los OP (ellos la ven como un bloque, la rompen y la juntan con un balde vacío).
 */
public class AguaPortalBlock extends Block {
	public AguaPortalBlock(Properties propiedades) {
		super(propiedades);
	}

	private static boolean esAdmin(CollisionContext contexto) {
		return contexto instanceof EntityCollisionContext e && e.getEntity() instanceof Player p && p.hasPermissions(2);
	}

	/** La forma para apuntarla con el mouse: sólo los OP. */
	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return esAdmin(contexto) ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return Shapes.empty();
	}

	/** Entre dos bloques de Agua Rara no se dibuja la cara de en medio (como el agua). */
	@Override
	protected boolean skipRendering(BlockState estado, BlockState vecino, Direction lado) {
		return vecino.is(this) || super.skipRendering(estado, vecino, lado);
	}

	/** Un OP con un balde vacío la junta (le queda el bloque para ponerla en otro lado). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack pila, BlockState estado, Level mundo, BlockPos pos, Player jugador,
											  InteractionHand mano, BlockHitResult golpe) {
		if (!pila.is(Items.BUCKET)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (mundo.isClientSide) return ItemInteractionResult.SUCCESS;
		if (!jugador.hasPermissions(2)) {
			jugador.displayClientMessage(Component.literal("El Agua Rara no se puede agarrar."), true);
			return ItemInteractionResult.FAIL;
		}
		mundo.removeBlock(pos, false);
		ItemStack agua = new ItemStack(asItem());
		if (!jugador.getInventory().add(agua)) jugador.drop(agua, false);
		mundo.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1f, 1f);
		return ItemInteractionResult.SUCCESS;
	}
}
