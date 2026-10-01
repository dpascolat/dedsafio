package com.dedsafio4.puertas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Entidad del bloque principal de la puerta: recuerda cuándo empezó a abrirse, para la animación. */
public class PuertaBlockEntity extends BlockEntity {
	/** Lo que tarda en abrirse (1,2 segundos). */
	public static final float TICKS_APERTURA = 24f;

	/** Tick del mundo en que se abrió (del lado del cliente); -1 si ya estaba abierta al cargarla. */
	private long inicio = -1;

	public PuertaBlockEntity(BlockPos pos, BlockState estado) {
		super(ModPuertas.PUERTA_ENTIDAD, pos, estado);
	}

	@Override
	public void setBlockState(BlockState nuevo) {
		boolean antes = getBlockState().getValue(PuertaBlock.ABIERTA);
		super.setBlockState(nuevo);
		if (level != null && level.isClientSide && !antes && nuevo.getValue(PuertaBlock.ABIERTA)) inicio = level.getGameTime();
	}

	/** Cuánto está abierta, de 0 (cerrada) a 1 (abierta del todo). */
	public float apertura(float parcial) {
		if (!getBlockState().getValue(PuertaBlock.ABIERTA)) return 0;
		if (inicio < 0 || level == null) return 1;
		return Math.min(1f, (level.getGameTime() - inicio + parcial) / TICKS_APERTURA);
	}
}
