package com.dedsafio4.despegue;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El Soporte de Nave: una base chata de 2×2 bloques (el modelo Soporte_nave.bbmodel) donde se coloca la Nave
 * Espacial Biplaza. Son 4 bloques; PARTE = 0 (el principal, el que se puso), 1 (al este), 2 (al sur), 3 (al sudeste).
 * Lo dibuja la entidad del bloque principal (SoporteNaveRenderer). Si se rompe una parte, se va el soporte entero.
 */
public class SoporteNaveBlock extends BaseEntityBlock {
	public static final MapCodec<SoporteNaveBlock> CODEC = simpleCodec(SoporteNaveBlock::new);
	public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 3);
	private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 1.5, 16);

	public SoporteNaveBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(PARTE, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PARTE);
	}

	/** Dónde va cada parte, desde el principal. */
	private static BlockPos posParte(BlockPos principal, int parte) {
		return principal.offset(parte % 2, 0, parte / 2);
	}

	/** El bloque principal del soporte (el de la esquina noroeste). */
	public static BlockPos principal(BlockPos pos, BlockState estado) {
		int parte = estado.getValue(PARTE);
		return pos.offset(-(parte % 2), 0, -(parte / 2));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		BlockPos pos = contexto.getClickedPos();
		Level level = contexto.getLevel();
		for (int parte = 1; parte < 4; parte++) {
			BlockPos p = posParte(pos, parte);
			if (!level.getBlockState(p).canBeReplaced(contexto) || !level.getWorldBorder().isWithinBounds(p)) return null;
		}
		return defaultBlockState();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
		super.setPlacedBy(level, pos, estado, quien, pila);
		if (level.isClientSide) return;
		for (int parte = 1; parte < 4; parte++) level.setBlock(posParte(pos, parte), estado.setValue(PARTE, parte), 3);
	}

	/** Si se saca una parte, se saca el soporte entero (sin soltar más de uno). */
	@Override
	protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
		if (!nuevo.is(this)) {
			BlockPos principal = principal(pos, estado);
			for (int parte = 0; parte < 4; parte++) {
				BlockPos p = posParte(principal, parte);
				if (p.equals(pos)) continue;
				BlockState otro = level.getBlockState(p);
				if (otro.is(this) && otro.getValue(PARTE) == parte) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		super.onRemove(estado, level, pos, nuevo, movido);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected RenderShape getRenderShape(BlockState estado) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return estado.getValue(PARTE) == 0 ? new SoporteNaveBlockEntity(pos, estado) : null;
	}
}
