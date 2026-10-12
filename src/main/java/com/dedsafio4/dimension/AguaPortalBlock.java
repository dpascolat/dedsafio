package com.dedsafio4.dimension;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El Agua Rara: por dentro es agua de verdad (como un bloque inundado), así que tiene todo lo del agua: se ve igual,
 * se nada, frena las caídas, hace burbujas y salpica. Pero no corre ni se derrama, y quedarse 3 segundos adentro te
 * lleva a la otra dimensión (lo maneja {@link Portales}, con la pantalla que se pone negra).
 * No se puede romper ni juntar con un balde, salvo los OP (ellos la ven como un bloque y la juntan con un balde vacío).
 */
public class AguaPortalBlock extends Block {
	public AguaPortalBlock(Properties propiedades) {
		super(propiedades);
	}

	/** Por dentro es agua quieta (la dibuja Minecraft como el agua, con su superficie y todo). */
	@Override
	protected FluidState getFluidState(BlockState estado) {
		return Fluids.WATER.getSource(false);
	}

	/** El bloque en sí no se dibuja: sólo se ve el agua. */
	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
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
