package com.dedsafio4.cofres;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** El Cofre del Limbo: cofre doble (54 espacios) oscuro, con grietas violetas que brillan y humo de almas. */
public class CofreLimboBlock extends CofreBlock {
	public static final MapCodec<CofreLimboBlock> CODEC = simpleCodec(CofreLimboBlock::new);

	public CofreLimboBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected BlockEntityType<CofreBlockEntity> tipoEntidad() {
		return ModCofres.COFRE_LIMBO_ENTIDAD;
	}

	@Override
	public SoundEvent sonidoAbrir() {
		return ModCofres.SONIDO_LIMBO_ABRIR;
	}

	@Override
	public SoundEvent sonidoCerrar() {
		return ModCofres.SONIDO_LIMBO_CERRAR;
	}

	@Override
	public void animateTick(BlockState estado, Level level, BlockPos pos, RandomSource azar) {
		if (azar.nextInt(4) != 0) return;
		level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + azar.nextDouble(), pos.getY() + 1.05, pos.getZ() + azar.nextDouble(), 0, 0.02, 0);
	}
}
