package com.dedsafio4.cofres;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El Cofre Amarillo: un cofre como el de Minecraft (27 espacios, y si pones dos juntos se hace doble con 54), con
 * patas y el pestillo amarillo del diseño. Lo dibuja ChestRendererMixin (simple o doble) con las mallas
 * cofre_amarillo.json y cofre_amarillo_doble.json.
 */
public class CofreAmarilloBlock extends ChestBlock {
	public static final MapCodec<CofreAmarilloBlock> CODEC = simpleCodec(CofreAmarilloBlock::new);

	public CofreAmarilloBlock(Properties propiedades) {
		super(propiedades, () -> ModCofres.COFRE_AMARILLO_ENTIDAD);
	}

	@Override
	public MapCodec<? extends ChestBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new Entidad(pos, estado);
	}

	/** La entidad: la del cofre de Minecraft, con su propio tipo. */
	public static class Entidad extends ChestBlockEntity {
		public Entidad(BlockPos pos, BlockState estado) {
			super(ModCofres.COFRE_AMARILLO_ENTIDAD, pos, estado);
		}
	}
}
