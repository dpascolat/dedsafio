package com.dedsafio4.gancho;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Lo que guarda cada Gancho de Huesos: hasta dónde (más o menos, en bloques) te lanza. */
public class GanchoBlockEntity extends BlockEntity {
	private int distancia = GanchoItem.DISTANCIA_NORMAL;

	public GanchoBlockEntity(BlockPos pos, BlockState estado) {
		super(ModGancho.ENTIDAD, pos, estado);
	}

	public int distancia() {
		return distancia;
	}

	public void setDistancia(int d) {
		distancia = d;
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.saveAdditional(tag, registros);
		tag.putInt("distancia", distancia);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.loadAdditional(tag, registros);
		if (tag.contains("distancia")) distancia = tag.getInt("distancia");
	}
}
