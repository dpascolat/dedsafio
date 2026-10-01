package com.dedsafio4.despegue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El centro de la Plataforma de Despegue. Al ponerlo arma la plataforma entera alrededor (5×5): chapas de metal
 * pegadas al centro y el borde con franjas amarillas y negras. Sólo ocupa lugares libres (no rompe nada).
 * Con la Nave Espacial Biplaza en la mano y click derecho sobre el centro, aparece la nave parada, lista para despegar.
 */
public class PlataformaBlock extends Block {
	public PlataformaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
		super.setPlacedBy(level, pos, estado, quien, pila);
		if (level.isClientSide) return;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx == 0 && dz == 0) continue;
				BlockPos lugar = pos.offset(dx, 0, dz);
				if (!level.getBlockState(lugar).canBeReplaced()) continue;
				boolean borde = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
				level.setBlockAndUpdate(lugar, (borde ? ModDespegue.PLATAFORMA_BORDE : ModDespegue.PLATAFORMA_METAL)
						.defaultBlockState());
			}
		}
	}
}
