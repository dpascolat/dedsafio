package com.dedsafio4.cofres;

import com.mojang.serialization.MapCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** El Cofre de Gleba: cofre doble (54 espacios) de carne, con costillas, ojos y órganos adentro. */
public class CofreGlebaBlock extends CofreBlock {
	public static final MapCodec<CofreGlebaBlock> CODEC = simpleCodec(CofreGlebaBlock::new);

	public CofreGlebaBlock(Properties propiedades) {
		super(propiedades);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected BlockEntityType<CofreBlockEntity> tipoEntidad() {
		return ModCofres.COFRE_GLEBA_ENTIDAD;
	}

	@Override
	public SoundEvent sonidoAbrir() {
		return ModCofres.SONIDO_GLEBA_ABRIR;
	}

	@Override
	public SoundEvent sonidoCerrar() {
		return ModCofres.SONIDO_GLEBA_CERRAR;
	}
}
