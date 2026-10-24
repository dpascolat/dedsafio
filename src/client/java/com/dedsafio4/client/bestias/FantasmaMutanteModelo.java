package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;

import java.util.Map;

/**
 * El Fantasma Mutante: el modelo de assets/dedsafio4/cem/fantasma_mutante.jem (ModeloJem) dibujado sobre las partes del
 * fantasma de Minecraft (el cuerpo, las alas con sus puntas, la cola y la cabeza), así aletea con la animación normal.
 * La mandíbula se mueve sola (como dice el archivo: un poquito, con el tiempo).
 */
public class FantasmaMutanteModelo<T extends Phantom> extends PhantomModel<T> {
	private static final ResourceLocation ARCHIVO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cem/fantasma_mutante.jem");
	/** De los nombres de OptiFine a las partes del fantasma de Minecraft (las alas y la cola cuelgan del cuerpo). */
	private static final Map<String, String> PARTES = Map.ofEntries(Map.entry("body", "body"), Map.entry("head", "body/head"),
			Map.entry("left_wing", "body/left_wing_base"), Map.entry("left_wing_tip", "body/left_wing_base/left_wing_tip"),
			Map.entry("right_wing", "body/right_wing_base"), Map.entry("right_wing_tip", "body/right_wing_base/right_wing_tip"),
			Map.entry("tail", "body/tail_base"), Map.entry("tail2", "body/tail_base/tail_tip"));

	private final ModeloJem jem;
	private float tiempo;

	public FantasmaMutanteModelo(ModelPart raiz) {
		super(raiz);
		this.jem = new ModeloJem(ARCHIVO, PARTES, raiz);
	}

	@Override
	public void setupAnim(T fantasma, float paso, float velocidad, float edad, float cabezaY, float cabezaX) {
		super.setupAnim(fantasma, paso, velocidad, edad, cabezaY, cabezaX);
		this.tiempo = edad;
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vc, int luz, int overlay, int color) {
		float mandibula = (float) Math.sin(tiempo / 8f) / 12f;
		jem.dibujar(pose, vc, luz, overlay, color, id -> id.startsWith("jaw") ? new float[]{mandibula, 0, 0} : null);
	}
}
