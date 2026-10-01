package com.dedsafio4.client.robots;

import com.dedsafio4.robots.RobotDormidoBlock;
import com.dedsafio4.robots.RobotDormidoBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al robot dormido: el mismo modelo del Aldeano Robot, plegado en un bloque. */
public class RobotDormidoRenderer implements BlockEntityRenderer<RobotDormidoBlockEntity> {
	private static final ResourceLocation BLANCO =
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/entity/blanco.png");
	private static final MonitorRig RIG = new MonitorRig();

	public RobotDormidoRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(RobotDormidoBlockEntity robot, float parcial, PoseStack pose, MultiBufferSource buffers,
					   int luz, int overlay) {
		float t = robot.getLevel() == null ? 0 : (robot.getLevel().getGameTime() + parcial) / 20f;
		dibujar(pose, buffers, luz, overlay, robot.getBlockState().getValue(RobotDormidoBlock.FACING), t);
	}

	/** También lo usa el ítem. */
	public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay, Direction frente, float t) {
		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));   // la pantalla mira a +Z
		RIG.posar(MonitorRig.Animacion.DORMIDO, t, 0, 0, 0);
		RIG.dibujar(pose, buffers.getBuffer(RenderType.entitySolid(BLANCO)), luz, overlay, 0xFFFFFFFF, true, t);
		pose.popPose();
	}
}
