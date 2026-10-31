package com.dedsafio4.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** La luz de los bloques, para apagarla en el Vacío Cósmico (EntregaCliente). */
@Mixin(LightTexture.class)
public interface LightTextureAccessor {
	@Accessor("lightPixels")
	NativeImage dedsafio4$imagen();

	@Accessor("lightTexture")
	DynamicTexture dedsafio4$textura();
}
