package com.dedsafio4.client.cielo2;

import java.util.function.Supplier;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Un sonido del cielo rojo 2 que se repite y sigue el estado que le dan ({volumen, tono}; null = se apaga de a poco).
 * Los cambios de volumen y tono son suaves.
 */
final class SonidoCielo2 extends AbstractTickableSoundInstance {
	private final Supplier<float[]> estado;

	SonidoCielo2(ResourceLocation evento, Supplier<float[]> estado, boolean repetir) {
		super(SoundEvent.createVariableRangeEvent(evento), SoundSource.MASTER, SoundInstance.createUnseededRandom());
		this.estado = estado;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.x = this.y = this.z = 0;
		float[] ahora = estado.get();
		this.pitch = ahora != null ? ahora[1] : 1f;
		this.volume = 0f;
		this.looping = repetir;
		this.delay = 0;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		float[] ahora = estado.get();
		if (ahora == null) {
			volume *= 0.6f;
			if (volume < 0.005f) stop();
		} else {
			volume += (ahora[0] - volume) * 0.15f;
			pitch += (ahora[1] - pitch) * 0.08f;
		}
	}
}
