package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public interface LivingEntityFeatureRendererRegistrationCallback {
	Event<LivingEntityFeatureRendererRegistrationCallback> EVENT = new Event<>();

	void registerRenderers(EntityType<? extends LivingEntity> tipo, LivingEntityRenderer<?, ?> dibujante, RegistrationHelper registro,
			EntityRendererProvider.Context contexto);

	interface RegistrationHelper {
		<T extends LivingEntity> void register(RenderLayer<T, ? extends EntityModel<T>> capa);
	}
}
