package com.dedsafio4.bestias;

import com.dedsafio4.items.AltavozItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * Creeper Visceral: un creeper de carne de la Dimensión de los Órganos. Explota como un creeper normal y el daño
 * convencional no le hace nada: solo se lo puede matar con un Altavoz (o con /kill).
 */
public class CreeperVisceralEntity extends Creeper {
	public CreeperVisceralEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
	}

	@Override
	public boolean hurt(DamageSource fuente, float danio) {
		if (!fuente.is(AltavozItem.DANIO) && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		return super.hurt(fuente, danio);
	}

	/** Con cualquier luz también le sirve el lugar. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader mundo) {
		return 0;
	}
}
