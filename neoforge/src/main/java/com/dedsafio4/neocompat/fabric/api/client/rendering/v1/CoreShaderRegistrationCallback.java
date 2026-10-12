package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/** Los shaders propios del mod (PuenteCliente los carga en el RegisterShadersEvent de NeoForge). */
public interface CoreShaderRegistrationCallback {
	Event<CoreShaderRegistrationCallback> EVENT = new Event<>();

	void registerShaders(RegistrationContext context) throws IOException;

	interface RegistrationContext {
		void register(ResourceLocation id, VertexFormat vertexFormat, Consumer<ShaderInstance> loadCallback) throws IOException;
	}
}
