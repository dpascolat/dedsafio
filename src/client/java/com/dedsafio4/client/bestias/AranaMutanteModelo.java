package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;

/**
 * La Araña Mutante: el modelo de assets/dedsafio4/cem/arana_mutante.jem (ModeloJem) dibujado sobre las partes de la
 * araña de Minecraft (head, neck, body, leg1..leg8), así se mueve con la animación normal (mirar, caminar).
 */
public class AranaMutanteModelo<T extends Entity> extends SpiderModel<T> {
	private static final ResourceLocation ARCHIVO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cem/arana_mutante.jem");
	/** De los nombres de OptiFine a las partes de la araña de Minecraft. */
	private static final Map<String, String> PARTES = Map.ofEntries(Map.entry("head", "head"), Map.entry("neck", "body0"),
			Map.entry("body", "body1"), Map.entry("leg1", "right_hind_leg"), Map.entry("leg2", "left_hind_leg"),
			Map.entry("leg3", "right_middle_hind_leg"), Map.entry("leg4", "left_middle_hind_leg"),
			Map.entry("leg5", "right_middle_front_leg"), Map.entry("leg6", "left_middle_front_leg"),
			Map.entry("leg7", "right_front_leg"), Map.entry("leg8", "left_front_leg"));

	private final ModeloJem jem;

	public AranaMutanteModelo(ModelPart raiz) {
		super(raiz);
		this.jem = new ModeloJem(ARCHIVO, PARTES, raiz);
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		jem.dibujar(pose, vc, luz, overlay, color, null);
	}
}
