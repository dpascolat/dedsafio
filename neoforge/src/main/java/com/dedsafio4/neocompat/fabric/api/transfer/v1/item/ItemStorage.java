package com.dedsafio4.neocompat.fabric.api.transfer.v1.item;

import com.dedsafio4.neocompat.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class ItemStorage {
	private ItemStorage() {}

	public interface BlockApiProvider {
		Storage<ItemVariant> find(Level mundo, BlockPos pos, BlockState estado, BlockEntity bloqueEntidad, Direction lado);
	}

	public record Proveedor(BlockApiProvider proveedor, Block[] bloques) {}

	public static final Lado SIDED = new Lado();

	public static final class Lado {
		public final List<Proveedor> PROVEEDORES = new ArrayList<>();

		public void registerForBlocks(BlockApiProvider proveedor, Block... bloques) {
			PROVEEDORES.add(new Proveedor(proveedor, bloques));
		}
	}
}
