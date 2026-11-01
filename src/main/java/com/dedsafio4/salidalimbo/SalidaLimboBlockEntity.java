package com.dedsafio4.salidalimbo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No guarda nada: está para que el bloque mire cada tanto si tiene a alguien adelante (ver SalidaLimbo). */
public class SalidaLimboBlockEntity extends BlockEntity {
	public SalidaLimboBlockEntity(BlockPos pos, BlockState estado) {
		super(SalidaLimbo.ENTIDAD, pos, estado);
	}
}
