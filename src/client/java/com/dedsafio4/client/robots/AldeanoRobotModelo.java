package com.dedsafio4.client.robots;

import com.dedsafio4.robots.AldeanoRobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;

/**
 * Modelo del Aldeano Robot: el "Monitor Mob" del diseño (ver {@link MonitorRig}). Flota medio bloque,
 * respira, mueve los brazos al caminar, mira a los jugadores y saluda mientras comercian con él.
 */
public class AldeanoRobotModelo extends EntityModel<AldeanoRobotEntity> {
	private final MonitorRig rig = new MonitorRig();
	private float segundos;

	@Override
	public void setupAnim(AldeanoRobotEntity robot, float limbSwing, float limbSwingAmount, float ageInTicks,
						  float netHeadYaw, float headPitch) {
		segundos = ageInTicks / 20f;
		float camina = Math.min(1f, limbSwingAmount * 4f);
		MonitorRig.Animacion anim = robot.estaComerciando() ? MonitorRig.Animacion.SALUDAR : MonitorRig.Animacion.RESPIRAR;
		rig.posar(anim, segundos, camina, -netHeadYaw * Mth.DEG_TO_RAD * 0.7f, headPitch * Mth.DEG_TO_RAD * 0.4f);
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int luz, int overlay, int color) {
		pose.pushPose();
		// Del espacio de Minecraft (Y abajo, frente -Z, origen 1,5 bloques arriba) al del diseño.
		pose.translate(0f, 1.501f, 0f);
		pose.scale(1f, -1f, -1f);
		rig.dibujar(pose, buffer, luz, overlay, color, false, segundos);
		pose.popPose();
	}
}
