package com.dedsafio4.client.cofres;

import com.dedsafio4.cofres.CofreBlock;
import com.dedsafio4.cofres.CofreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Dibuja el cofre nuevo con la malla del diseño (assets/dedsafio4/modelos/cofre.json). Las líneas de
 * energía (naranja, amarillo y rojo) brillan siempre; el interior se enciende a medida que se abre.
 */
public class CofreRenderer implements BlockEntityRenderer<CofreBlockEntity> {
	private static final MallaCofre MALLA = new MallaCofre("cofre.json", Set.of(3, 4, 5), 6);

	public CofreRenderer(BlockEntityRendererProvider.Context contexto) {}

	@Override
	public void render(CofreBlockEntity cofre, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
		BlockState estado = cofre.getBlockState();
		if (!(estado.getBlock() instanceof CofreBlock)) return;
		float abierto = cofre.getOpenNess(parcial);
		dibujar(pose, buffers, luz, overlay, estado.getValue(CofreBlock.FACING), abierto, true);
	}

	/** También lo usa el ítem (cerrado). */
	public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay,
							   Direction frente, float abierto, boolean enElMundo) {
		pose.pushPose();
		if (enElMundo) {
			// El centro del modelo queda entre las dos mitades (la otra está a la derecha).
			Direction derecha = frente.getClockWise();
			pose.translate(0.5 + derecha.getStepX() * 0.5, 0, 0.5 + derecha.getStepZ() * 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));   // el frente del modelo es +Z
		}
		MALLA.dibujar(pose, buffers, luz, overlay, abierto);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(CofreBlockEntity cofre) {
		return true;
	}
}
