package com.dedsafio4.marcos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Un marco como el de Minecraft pero de 4×4 bloques e invisible: solo se ve el ítem que tiene adentro, 4 veces más
 * grande. Ocupa el bloque donde se pone, 3 más a la derecha (mirando la pared) y 3 filas para arriba; en el piso o el
 * techo, 4×4 hacia +X y +Z. Al romperlo da un marco normal de Minecraft.
 */
public class MarcoGrandeEntity extends ItemFrame {
	/** Cuántas veces más grande que un marco normal (de lado). */
	public static final int ESCALA = 4;
	/** Qué tan lejos de la pared queda el centro (el de un marco normal, por la escala: se dibuja agrandado). */
	private static final double SEPARACION = 0.03125 * ESCALA;
	/** Cuánto se corre el centro desde el bloque donde se pone, para quedar en el medio de los 4×4. */
	private static final double CORRIMIENTO = (ESCALA - 1) / 2.0;

	public MarcoGrandeEntity(EntityType<? extends ItemFrame> tipo, Level mundo) {
		super(tipo, mundo);
	}

	public MarcoGrandeEntity(Level mundo, BlockPos pos, Direction direccion) {
		super(ModMarcos.MARCO_GRANDE, mundo, pos, direccion);
	}

	/** El centro del marco: en el medio de los 4×4 bloques, pegado a la pared. */
	public static Vec3 centro(BlockPos pos, Direction direccion) {
		Vec3 c = Vec3.atCenterOf(pos).relative(direccion, -0.5 + SEPARACION);
		if (direccion.getAxis().isHorizontal()) {
			Direction lado = direccion.getCounterClockWise();
			return c.add(lado.getStepX() * CORRIMIENTO, CORRIMIENTO, lado.getStepZ() * CORRIMIENTO);
		}
		return c.add(CORRIMIENTO, 0, CORRIMIENTO);
	}

	@Override
	protected AABB calculateBoundingBox(BlockPos pos, Direction direccion) {
		Direction.Axis eje = direccion.getAxis();
		// Como la caja de un marco normal (12×12 píxeles y 1 de grueso), por la escala.
		double lado = 0.75 * ESCALA, grueso = 0.0625 * ESCALA;
		double dx = eje == Direction.Axis.X ? grueso : lado;
		double dy = eje == Direction.Axis.Y ? grueso : lado;
		double dz = eje == Direction.Axis.Z ? grueso : lado;
		return AABB.ofSize(centro(pos, direccion), dx, dy, dz);
	}

	/** Se sostiene si los 4 bloques de atrás son sólidos y no hay otro marco o cuadro encima. */
	@Override
	public boolean survives() {
		if (!level().noCollision(this)) return false;
		AABB atras = getBoundingBox().move(direction.getStepX() * -0.5, direction.getStepY() * -0.5, direction.getStepZ() * -0.5);
		for (BlockPos p : BlockPos.betweenClosed(Mth.floor(atras.minX + 1e-4), Mth.floor(atras.minY + 1e-4), Mth.floor(atras.minZ + 1e-4),
				Mth.floor(atras.maxX - 1e-4), Mth.floor(atras.maxY - 1e-4), Mth.floor(atras.maxZ - 1e-4))) {
			BlockState estado = level().getBlockState(p);
			if (!estado.isSolid() && !(direction.getAxis().isHorizontal() && DiodeBlock.isDiode(estado))) return false;
		}
		return level().getEntities(this, getBoundingBox(), HANGING_ENTITY).isEmpty();
	}

	/** Siempre invisible: solo se ve el ítem. */
	@Override
	public boolean isInvisible() {
		return true;
	}

	/** Al romperlo da un marco normal de Minecraft. */
	@Override
	protected ItemStack getFrameItemStack() {
		return new ItemStack(net.minecraft.world.item.Items.ITEM_FRAME);
	}
}
