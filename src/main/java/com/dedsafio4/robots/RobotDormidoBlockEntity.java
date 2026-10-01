package com.dedsafio4.robots;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Sólo existe para poder dibujar al robot dormido con su modelo. */
public class RobotDormidoBlockEntity extends BlockEntity {
	public RobotDormidoBlockEntity(BlockPos pos, BlockState estado) {
		super(ModRobots.ROBOT_DORMIDO_ENTIDAD, pos, estado);
	}
}
