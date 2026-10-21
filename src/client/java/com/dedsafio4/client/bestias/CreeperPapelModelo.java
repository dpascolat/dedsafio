package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.CreeperPapelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * El Creeper de Papel: la hoja (cem/creeper_papel.jem) sobre las partes del creeper de Minecraft, que camina con sus
 * patas como siempre. La hoja se balancea un poco, como en el modelo original de OptiFine.
 */
public class CreeperPapelModelo extends CreeperModel<CreeperPapelEntity> {
	private static final ResourceLocation ARCHIVO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cem/creeper_papel.jem");
	private static final Map<String, String> PARTES = Map.of("head", "head", "body", "body", "leg1", "right_hind_leg",
			"leg2", "left_hind_leg", "leg3", "right_front_leg", "leg4", "left_front_leg");

	private final ModeloJem jem;
	private float edad;

	public CreeperPapelModelo(ModelPart raiz) {
		super(raiz);
		this.jem = new ModeloJem(ARCHIVO, PARTES, raiz);
	}

	@Override
	public void setupAnim(CreeperPapelEntity creeper, float paso, float velocidad, float edad, float cabezaY, float cabezaX) {
		super.setupAnim(creeper, paso, velocidad, edad, cabezaY, cabezaX);
		this.edad = edad;
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		float s = Mth.sin(edad * 0.03f);
		float balanceo = -s * s * 0.15f;
		jem.dibujar(pose, vc, luz, overlay, color, id -> id.equals("body2") ? new float[]{balanceo, 0, 0} : null);
	}
}
