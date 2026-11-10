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

/**
 * Dibuja al robot dormido: el modelo del Aldeano Phora apoyado en el piso del bloque, con la cabeza para abajo, los
 * brazos caídos y las luces verdes apagadas (textura aldeano_phora_dormido).
 */
public class RobotDormidoRenderer implements BlockEntityRenderer<RobotDormidoBlockEntity> {
	private static final ResourceLocation DORMIDO =
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/entity/aldeano_phora_dormido.png");
	private static final AldeanoPhoraModelo MODELO = new AldeanoPhoraModelo();

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
		// Como dibuja Minecraft a los bichos (y para abajo, mirando para donde mira el bloque), pero apoyado en el
		// piso: el cuerpo termina 8 píxeles arriba de los pies, así que se baja medio bloque.
		pose.translate(0.5, 1.0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(180f - frente.toYRot()));
		pose.scale(-1f, -1f, 1f);
		MODELO.posarDormido(t);
		MODELO.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DORMIDO)), luz, overlay, 0xFFFFFFFF);
		pose.popPose();
	}
}
