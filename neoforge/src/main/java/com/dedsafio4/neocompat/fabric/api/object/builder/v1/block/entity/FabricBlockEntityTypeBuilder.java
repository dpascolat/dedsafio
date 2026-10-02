package com.dedsafio4.neocompat.fabric.api.object.builder.v1.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricBlockEntityTypeBuilder<T extends BlockEntity> {
	public interface Factory<T extends BlockEntity> {
		T create(BlockPos pos, BlockState estado);
	}

	private final Factory<? extends T> fabrica;
	private final Block[] bloques;

	private FabricBlockEntityTypeBuilder(Factory<? extends T> fabrica, Block[] bloques) {
		this.fabrica = fabrica;
		this.bloques = bloques;
	}

	public static <T extends BlockEntity> FabricBlockEntityTypeBuilder<T> create(Factory<? extends T> fabrica, Block... bloques) {
		return new FabricBlockEntityTypeBuilder<>(fabrica, bloques);
	}

	public BlockEntityType<T> build() {
		return build(null);
	}

	public BlockEntityType<T> build(com.mojang.datafixers.types.Type<?> tipo) {
		return BlockEntityType.Builder.<T>of(fabrica::create, bloques).build(tipo);
	}
}
