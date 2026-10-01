package com.dedsafio4.client.mixin;

import com.dedsafio4.client.PocionesCliente;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {
	/** Se volvió a subir la textura de los bloques (F3+T, paquete de recursos): la copia guardada ya no sirve. */
	@Inject(method = "upload", at = @At("RETURN"))
	private void dedsafio4$recargada(SpriteLoader.Preparations preparaciones, CallbackInfo ci) {
		if (((TextureAtlas) (Object) this).location().equals(TextureAtlas.LOCATION_BLOCKS)) PocionesCliente.atlasRecargado();
	}
}
