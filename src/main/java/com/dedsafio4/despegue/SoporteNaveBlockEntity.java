package com.dedsafio4.despegue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Entidad del bloque principal del Soporte de Nave: sólo está para que se dibuje el modelo. */
public class SoporteNaveBlockEntity extends BlockEntity {
	public SoporteNaveBlockEntity(BlockPos pos, BlockState estado) {
		super(ModDespegue.SOPORTE_NAVE_ENTIDAD, pos, estado);
	}
}
