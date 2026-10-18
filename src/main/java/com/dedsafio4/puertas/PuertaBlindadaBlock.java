package com.dedsafio4.puertas;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * La Puerta Blindada: una puerta de dos bloques (de acero, con volante de bóveda, teclado, bisagras y cerrojos) que
 * se abre con la mano. Con un Candado se le pone un código, igual que a los cofres (ver Candados): el que la cerró
 * la abre siempre y los demás necesitan el código. No se abre con redstone.
 */
public class PuertaBlindadaBlock extends DoorBlock {
	public static final MapCodec<PuertaBlindadaBlock> CODEC = simpleCodec(PuertaBlindadaBlock::new);

	/** Se abre con la mano, con los sonidos de hierro. */
	public static final BlockSetType TIPO = new BlockSetType("dedsafio4_blindada", true, false, false,
			BlockSetType.PressurePlateSensitivity.MOBS, SoundType.METAL,
			SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
			SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
			SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

	public PuertaBlindadaBlock(Properties propiedades) {
		super(TIPO, propiedades);
	}

	@Override
	public MapCodec<? extends DoorBlock> codec() {
		return CODEC;
	}

	/** La redstone no la abre (si no, una puerta con candado se podría abrir con una palanca). */
	@Override
	protected void neighborChanged(BlockState estado, Level level, BlockPos pos, Block bloque, BlockPos desde, boolean movido) {
	}
}
