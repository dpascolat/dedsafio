package com.dedsafio4.neocompat.fabric.api.client.rendering.v1;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/** Lo que el mod usa del contexto de dibujado del mundo de Fabric. */
public record WorldRenderContext(Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, DeltaTracker tickCounter,
		PoseStack matrixStack) {
	public ClientLevel world() {
		return Minecraft.getInstance().level;
	}

	public MultiBufferSource consumers() {
		return Minecraft.getInstance().renderBuffers().bufferSource();
	}
}
