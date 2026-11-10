package com.dedsafio4.client.robots;

import com.dedsafio4.robots.AldeanoRobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Modelo del Aldeano Phora (pa_aldeano_robot_onixestudio, ver AldeanoPhoraMalla): flota, mira a los jugadores, al
 * caminar mueve los brazos y sube y baja un poco (como la animación "move" de Patricio), mece los brazos quieto
 * ("bob") y saluda mientras comercian con él. Dormido (RobotDormidoRenderer) baja la cabeza y deja caer los brazos.
 */
public class AldeanoPhoraModelo extends EntityModel<AldeanoRobotEntity> {
	private final ModelPart todo, raiz, cabeza, brazoIzq, brazoDer;

	public AldeanoPhoraModelo() {
		todo = AldeanoPhoraMalla.crear().bakeRoot();
		raiz = todo.getChild("root");
		cabeza = todo.getChild("head");
		brazoIzq = todo.getChild("leftArm");
		brazoDer = todo.getChild("rightArm");
	}

	private void derecho() {
		todo.getAllParts().forEach(ModelPart::resetPose);
	}

	@Override
	public void setupAnim(AldeanoRobotEntity robot, float limbSwing, float limbSwingAmount, float ageInTicks,
						  float netHeadYaw, float headPitch) {
		derecho();
		cabeza.yRot = netHeadYaw * Mth.DEG_TO_RAD;
		cabeza.xRot = headPitch * Mth.DEG_TO_RAD;
		float camina = Math.min(1f, limbSwingAmount * 2f);
		float vaiven = Mth.cos(limbSwing * 0.6662f) * camina;
		// Caminando: los brazos van y vienen (al revés uno del otro) y todo sube y baja 2 píxeles.
		brazoIzq.xRot = vaiven * 0.35f - 0.2f * camina * Math.abs(Mth.sin(limbSwing * 0.6662f));
		brazoDer.xRot = -vaiven * 0.35f - 0.2f * camina * Math.abs(Mth.sin(limbSwing * 0.6662f));
		float sube = -2f * camina * Math.abs(Mth.sin(limbSwing * 0.6662f));
		// Quieto: los brazos se mecen despacio de costado.
		float mece = (Mth.cos(ageInTicks * 0.09f) * 0.05f + 0.05f) * (1f - camina);
		brazoIzq.zRot -= mece;
		brazoDer.zRot += mece;
		// Flota: sube y baja suave todo el tiempo.
		float flota = Mth.sin(ageInTicks * 0.1f) * 0.6f;
		for (ModelPart p : new ModelPart[]{raiz, cabeza, brazoIzq, brazoDer}) p.y += sube + flota;
		// Comerciando: saluda con el brazo derecho.
		if (robot.estaComerciando()) {
			brazoDer.xRot = -2.4f;
			brazoDer.zRot = Mth.sin(ageInTicks * 0.4f) * 0.35f;
		}
	}

	/** La pose dormido: la cabeza para abajo, los brazos colgando hacia adentro y respirando despacio. */
	public void posarDormido(float segundos) {
		derecho();
		float respira = Mth.sin(segundos * 1.2f) * 0.03f;
		cabeza.xRot = 0.55f + respira;
		brazoIzq.xRot = 0.15f;
		brazoDer.xRot = 0.15f;
		brazoIzq.zRot = 0.12f;
		brazoDer.zRot = -0.12f;
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int luz, int overlay, int color) {
		todo.render(pose, buffer, luz, overlay, color);
	}
}
