package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.NautilusOseoGrandeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * El Nautilus Óseo Grande con el modelo de Patricio ("NautilusOseoG" de Blockbench, formato libre): el caparazón con
 * sus espinas y 6 tentáculos de 2 partes. Cada grupo gira sobre su centro como en Blockbench, y cada cubo dibuja su
 * pedazo de textura en cada cara. Animaciones del diseño: idle (flota y las puntas de los tentáculos se mecen),
 * walk (cuando se mueve, los tentáculos se mecen más y el cuerpo se inclina) y death (se cae de costado).
 */
public class NautilusOseoGrandeRenderer extends EntityRenderer<NautilusOseoGrandeEntity> {
	private static final ResourceLocation TEXTURA =
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/nautilus_oseo_grande.png");
	/** El diseño mide 16×21×27 píxeles; se dibuja al doble para que sea "grande". */
	private static final float ESCALA = 2f;
	/** Para centrar el bicho en su caja de choque (el centro del cubo "hitbox" del diseño, ya girado). */
	private static final float CORRER_Z = -5.5f;

	/** Los grupos: padre, centro de giro (3), giro (3), balanceo en idle y en walk (grados en X). */
	private static final float[][] NODOS = {
			{-1f, 0f, 10f, 10f, 0f, -180f, 0f, 0f, 0f},  // nauti
			{0f, 0f, 25f, 20f, 0f, 0f, 0f, 0f, 0f},  // body
			{1f, 0f, 0f, 20f, 0f, 0f, 0f, 0f, 0f},  // espina
			{1f, 7f, 3f, 10f, 0f, 0f, -180f, 0f, 0f},  // espina3
			{1f, -3f, 6f, 20f, 0f, 0f, -90f, 0f, 0f},  // espina2
			{1f, 0f, 0f, 20f, 0f, 0f, 0f, 0f, 0f},  // group7
			{0f, 2.4f, 10f, 8f, 0f, 0f, 0f, 0f, -2.5f},  // tentaculo3
			{6f, 1.4f, 10.5f, 1f, 0f, 0f, 0f, -2.5f, -7.5f},  // group
			{0f, -2.4f, 9.9f, 8f, 0f, 0f, 60f, 0f, -2.5f},  // tentaculo7
			{8f, -2.4f, 10.4f, 1f, 0f, 0f, 0f, -2f, -7.5f},  // group8
			{0f, -3.9f, 6f, 8f, 0f, 0f, 105f, 0f, -2.5f},  // tentaculo8
			{10f, -3.9f, 6.5f, 1f, 0f, 0f, 0f, -5f, -7.5f},  // group2
			{0f, 4.1f, 6f, 8f, 0f, 0f, -75f, 0f, -2.5f},  // tentaculo2
			{12f, 4.1f, 5.5f, 1f, 0f, 0f, 0f, -5f, -7.5f},  // group3
			{0f, 2f, 2.2f, 8f, 0f, 0f, -125f, 0f, -2.5f},  // tentaculo4
			{14f, 2f, 2.7f, 1f, 0f, 0f, 0f, -2.5f, -7.5f},  // group4
			{0f, -2.4f, 2.1f, 8f, 0f, 0f, 172.5f, 0f, -2.5f},  // tentaculo5
			{16f, -2.4f, 1.6f, 1f, 0f, 0f, 0f, -2.5f, -7.5f},  // group5
			{0f, 0f, 9f, 10f, 0f, 0f, 0f, 0f, 0f}   // hitbox
	};
	/** Los cubos: grupo, desde (3), hasta (3), centro de giro (3), giro (3) y la textura de cada cara (u1, v1, u2, v2). */
	private static final float[][] CUBOS = {
			{2f, -10f, 5f, 10f, -9f, 8f, 10f, -10f, 5f, 10f, -90f, 0f, 0f, 2f, 2f, 3f, 5f, 2f, 2f, 2f, 5f, 3f, 2f, 4f, 5f, 3f, 2f, 3f, 5f, 3f, 2f, 2f, 2f, 4f, 2f, 3f, 2f},
			{2f, -9f, 5f, 11f, -8f, 8f, 11f, -9f, 5f, 11f, -90f, 0f, 0f, 2f, 2f, 3f, 5f, 2f, 2f, 2f, 5f, 3f, 2f, 4f, 5f, 3f, 2f, 3f, 5f, 3f, 2f, 2f, 2f, 4f, 2f, 3f, 2f},
			{2f, -8f, 4f, 11f, -6f, 7f, 11f, -7f, 5f, 11f, -90f, 0f, 0f, 2f, 2f, 4f, 5f, 2f, 2f, 2f, 5f, 4f, 2f, 6f, 5f, 4f, 2f, 4f, 5f, 4f, 2f, 2f, 2f, 6f, 2f, 4f, 2f},
			{3f, 4f, 3f, 10f, 5f, 6f, 10f, 4f, 3f, 10f, -90f, 0f, 0f, 2f, 2f, 3f, 5f, 2f, 2f, 2f, 5f, 3f, 2f, 4f, 5f, 3f, 2f, 3f, 5f, 3f, 2f, 2f, 2f, 4f, 2f, 3f, 2f},
			{3f, 5f, 3f, 11f, 6f, 6f, 11f, 5f, 3f, 11f, -90f, 0f, 0f, 2f, 2f, 3f, 5f, 2f, 2f, 2f, 5f, 3f, 2f, 4f, 5f, 3f, 2f, 3f, 5f, 3f, 2f, 2f, 2f, 4f, 2f, 3f, 2f},
			{3f, 6f, 2f, 11f, 8f, 5f, 11f, 7f, 3f, 11f, -90f, 0f, 0f, 2f, 2f, 4f, 5f, 2f, 2f, 2f, 5f, 4f, 2f, 6f, 5f, 4f, 2f, 4f, 5f, 4f, 2f, 2f, 2f, 6f, 2f, 4f, 2f},
			{4f, -13f, 11f, 10f, -12f, 14f, 10f, -13f, 11f, 10f, -90f, 0f, 0f, 2f, 2f, 3f, 5f, 2f, 2f, 2f, 5f, 3f, 2f, 4f, 5f, 3f, 2f, 3f, 5f, 3f, 2f, 2f, 2f, 4f, 2f, 3f, 2f},
			{4f, -12f, 11f, 11f, -11f, 14f, 11f, -12f, 11f, 11f, -90f, 0f, 0f, 2f, 2f, 3f, 5f, 2f, 2f, 2f, 5f, 3f, 2f, 4f, 5f, 3f, 2f, 3f, 5f, 3f, 2f, 2f, 2f, 4f, 2f, 3f, 2f},
			{4f, -11f, 10f, 11f, -9f, 13f, 11f, -10f, 11f, 11f, -90f, 0f, 0f, 2f, 2f, 4f, 5f, 2f, 2f, 2f, 5f, 4f, 2f, 6f, 5f, 4f, 2f, 4f, 5f, 4f, 2f, 2f, 2f, 6f, 2f, 4f, 2f},
			{1f, -6f, 0f, 8f, 6f, 12f, 13f, 0f, 1f, 12f, 0f, 0f, 0f, 5f, 40f, 17f, 52f, 0f, 40f, 5f, 52f, 22f, 40f, 34f, 52f, 17f, 40f, 22f, 52f, 17f, 40f, 5f, 35f, 29f, 35f, 17f, 40f},
			{1f, -5f, 1f, 13f, 5f, 21f, 28f, 0f, 1f, 20f, 0f, 0f, 0f, 15f, 15f, 25f, 35f, 0f, 15f, 15f, 35f, 40f, 15f, 50f, 35f, 25f, 15f, 40f, 35f, 25f, 15f, 15f, 0f, 35f, 0f, 25f, 15f},
			{5f, 3f, 21f, 23f, 3f, 22f, 25f, 4f, 21f, 24f, 0f, 0f, 0f, 4f, 2f, 4f, 3f, 2f, 2f, 4f, 3f, 6f, 2f, 6f, 3f, 4f, 2f, 6f, 3f, 4f, 2f, 4f, 0f, 4f, 0f, 4f, 2f},
			{5f, 3f, 21f, 17f, 3f, 22f, 21f, 4f, 21f, 20f, 0f, 0f, 0f, 4f, 2f, 4f, 3f, 0f, 2f, 4f, 3f, 8f, 2f, 8f, 3f, 4f, 2f, 8f, 3f, 4f, 2f, 4f, -2f, 4f, -2f, 4f, 2f},
			{5f, 3f, 22f, 17f, 3f, 23f, 20f, 4f, 22f, 20f, 0f, 0f, 0f, 4f, 2f, 4f, 3f, 1f, 2f, 4f, 3f, 7f, 2f, 7f, 3f, 4f, 2f, 7f, 3f, 4f, 2f, 4f, -1f, 4f, -1f, 4f, 2f},
			{5f, 3f, 22f, 22f, 3f, 23f, 24f, 4f, 22f, 23f, 0f, 0f, 0f, 4f, 2f, 4f, 3f, 2f, 2f, 4f, 3f, 6f, 2f, 6f, 3f, 4f, 2f, 6f, 3f, 4f, 2f, 4f, 0f, 4f, 0f, 4f, 2f},
			{5f, 3f, 23f, 16f, 3f, 24f, 19f, 4f, 23f, 18f, 0f, 0f, 0f, 4f, 2f, 4f, 3f, 1f, 2f, 4f, 3f, 7f, 2f, 7f, 3f, 4f, 2f, 7f, 3f, 4f, 2f, 4f, -1f, 4f, -1f, 4f, 2f},
			{6f, 0.4f, 8.5f, 1f, 3.4f, 11.5f, 9f, 1.4f, 8.5f, 7f, 0f, 0f, -15f, 57f, 7f, 60f, 10f, 49f, 7f, 57f, 10f, 68f, 7f, 71f, 10f, 60f, 7f, 68f, 10f, 60f, 7f, 57f, -1f, 63f, -1f, 60f, 7f},
			{7f, 1.9f, 11f, -7f, 1.9f, 12f, -6f, 1.4f, 8.5f, -8f, 0f, 0f, -15f, 49f, 64f, 49f, 65f, 48f, 64f, 49f, 65f, 50f, 64f, 50f, 65f, 49f, 64f, 50f, 65f, 49f, 64f, 49f, 63f, 49f, 63f, 49f, 64f},
			{7f, 1.9f, 11f, -9f, 1.9f, 14f, -7f, 1.4f, 8.5f, -8f, 0f, 0f, -15f, 6f, 64f, 6f, 67f, 4f, 64f, 6f, 67f, 8f, 64f, 8f, 67f, 6f, 64f, 8f, 67f, 6f, 64f, 6f, 62f, 6f, 62f, 6f, 64f},
			{7f, 1.9f, 14f, -8f, 1.9f, 15f, -6f, 1.4f, 8.5f, -8f, 0f, 0f, -15f, 30f, 54f, 30f, 55f, 28f, 54f, 30f, 55f, 32f, 54f, 32f, 55f, 30f, 54f, 32f, 55f, 30f, 54f, 30f, 52f, 30f, 52f, 30f, 54f},
			{7f, 1.9f, 15f, -7f, 1.9f, 16f, -6f, 1.4f, 8.5f, -8f, 0f, 0f, -15f, 51f, 64f, 51f, 65f, 50f, 64f, 51f, 65f, 52f, 64f, 52f, 65f, 51f, 64f, 52f, 65f, 51f, 64f, 51f, 63f, 51f, 63f, 51f, 64f},
			{7f, 1.9f, 10f, -8f, 1.9f, 11f, -7f, 1.4f, 8.5f, -8f, 0f, 0f, -15f, 65f, 63f, 65f, 64f, 64f, 63f, 65f, 64f, 66f, 63f, 66f, 64f, 65f, 63f, 66f, 64f, 65f, 63f, 65f, 62f, 65f, 62f, 65f, 63f},
			{7f, 1.9f, 9f, -7f, 1.9f, 11f, -5f, 1.4f, 8.5f, -6f, 0f, 0f, -15f, 62f, 64f, 62f, 66f, 60f, 64f, 62f, 66f, 64f, 64f, 64f, 66f, 62f, 64f, 64f, 66f, 62f, 64f, 62f, 62f, 62f, 62f, 62f, 64f},
			{7f, 0.9f, 9f, -5f, 2.9f, 11f, 2f, 1.4f, 8.5f, 0f, 0f, 0f, -15f, 60f, 52f, 62f, 54f, 53f, 52f, 60f, 54f, 69f, 52f, 71f, 54f, 62f, 52f, 69f, 54f, 62f, 52f, 60f, 45f, 64f, 45f, 62f, 52f},
			{8f, -4.4f, 8.4f, 1f, -1.4f, 11.4f, 9f, -3.4f, 8.4f, 7f, 0f, 0f, -15f, 57f, 7f, 60f, 10f, 49f, 7f, 57f, 10f, 68f, 7f, 71f, 10f, 60f, 7f, 68f, 10f, 60f, 7f, 57f, -1f, 63f, -1f, 60f, 7f},
			{9f, -2.9f, 10.9f, -7f, -2.9f, 11.9f, -6f, -3.4f, 8.4f, -8f, 0f, 0f, -15f, 49f, 64f, 49f, 65f, 48f, 64f, 49f, 65f, 50f, 64f, 50f, 65f, 49f, 64f, 50f, 65f, 49f, 64f, 49f, 63f, 49f, 63f, 49f, 64f},
			{9f, -2.9f, 10.9f, -9f, -2.9f, 13.9f, -7f, -3.4f, 8.4f, -8f, 0f, 0f, -15f, 6f, 64f, 6f, 67f, 4f, 64f, 6f, 67f, 8f, 64f, 8f, 67f, 6f, 64f, 8f, 67f, 6f, 64f, 6f, 62f, 6f, 62f, 6f, 64f},
			{9f, -2.9f, 13.9f, -8f, -2.9f, 14.9f, -6f, -3.4f, 8.4f, -8f, 0f, 0f, -15f, 30f, 54f, 30f, 55f, 28f, 54f, 30f, 55f, 32f, 54f, 32f, 55f, 30f, 54f, 32f, 55f, 30f, 54f, 30f, 52f, 30f, 52f, 30f, 54f},
			{9f, -2.9f, 14.9f, -7f, -2.9f, 15.9f, -6f, -3.4f, 8.4f, -8f, 0f, 0f, -15f, 51f, 64f, 51f, 65f, 50f, 64f, 51f, 65f, 52f, 64f, 52f, 65f, 51f, 64f, 52f, 65f, 51f, 64f, 51f, 63f, 51f, 63f, 51f, 64f},
			{9f, -2.9f, 9.9f, -8f, -2.9f, 10.9f, -7f, -3.4f, 8.4f, -8f, 0f, 0f, -15f, 65f, 63f, 65f, 64f, 64f, 63f, 65f, 64f, 66f, 63f, 66f, 64f, 65f, 63f, 66f, 64f, 65f, 63f, 65f, 62f, 65f, 62f, 65f, 63f},
			{9f, -2.9f, 8.9f, -7f, -2.9f, 10.9f, -5f, -3.4f, 8.4f, -6f, 0f, 0f, -15f, 62f, 64f, 62f, 66f, 60f, 64f, 62f, 66f, 64f, 64f, 64f, 66f, 62f, 64f, 64f, 66f, 62f, 64f, 62f, 62f, 62f, 62f, 62f, 64f},
			{9f, -3.9f, 8.9f, -5f, -1.9f, 10.9f, 2f, -3.4f, 8.4f, 0f, 0f, 0f, -15f, 60f, 52f, 62f, 54f, 53f, 52f, 60f, 54f, 69f, 52f, 71f, 54f, 62f, 52f, 69f, 54f, 62f, 52f, 60f, 45f, 64f, 45f, 62f, 52f},
			{10f, -5.9f, 4.5f, 1f, -2.9f, 7.5f, 9f, -4.9f, 4.5f, 7f, 0f, 0f, -15f, 57f, 7f, 60f, 10f, 49f, 7f, 57f, 10f, 68f, 7f, 71f, 10f, 60f, 7f, 68f, 10f, 60f, 7f, 57f, -1f, 63f, -1f, 60f, 7f},
			{11f, -4.4f, 7f, -7f, -4.4f, 8f, -6f, -4.9f, 4.5f, -8f, 0f, 0f, -15f, 49f, 64f, 49f, 65f, 48f, 64f, 49f, 65f, 50f, 64f, 50f, 65f, 49f, 64f, 50f, 65f, 49f, 64f, 49f, 63f, 49f, 63f, 49f, 64f},
			{11f, -4.4f, 7f, -9f, -4.4f, 10f, -7f, -4.9f, 4.5f, -8f, 0f, 0f, -15f, 6f, 64f, 6f, 67f, 4f, 64f, 6f, 67f, 8f, 64f, 8f, 67f, 6f, 64f, 8f, 67f, 6f, 64f, 6f, 62f, 6f, 62f, 6f, 64f},
			{11f, -4.4f, 10f, -8f, -4.4f, 11f, -6f, -4.9f, 4.5f, -8f, 0f, 0f, -15f, 30f, 54f, 30f, 55f, 28f, 54f, 30f, 55f, 32f, 54f, 32f, 55f, 30f, 54f, 32f, 55f, 30f, 54f, 30f, 52f, 30f, 52f, 30f, 54f},
			{11f, -4.4f, 11f, -7f, -4.4f, 12f, -6f, -4.9f, 4.5f, -8f, 0f, 0f, -15f, 51f, 64f, 51f, 65f, 50f, 64f, 51f, 65f, 52f, 64f, 52f, 65f, 51f, 64f, 52f, 65f, 51f, 64f, 51f, 63f, 51f, 63f, 51f, 64f},
			{11f, -4.4f, 6f, -8f, -4.4f, 7f, -7f, -4.9f, 4.5f, -8f, 0f, 0f, -15f, 65f, 63f, 65f, 64f, 64f, 63f, 65f, 64f, 66f, 63f, 66f, 64f, 65f, 63f, 66f, 64f, 65f, 63f, 65f, 62f, 65f, 62f, 65f, 63f},
			{11f, -4.4f, 5f, -7f, -4.4f, 7f, -5f, -4.9f, 4.5f, -6f, 0f, 0f, -15f, 62f, 64f, 62f, 66f, 60f, 64f, 62f, 66f, 64f, 64f, 64f, 66f, 62f, 64f, 64f, 66f, 62f, 64f, 62f, 62f, 62f, 62f, 62f, 64f},
			{11f, -5.4f, 5f, -5f, -3.4f, 7f, 3f, -4.9f, 4.5f, 0f, 0f, 0f, -15f, 60f, 52f, 62f, 54f, 52f, 52f, 60f, 54f, 70f, 52f, 72f, 54f, 62f, 52f, 70f, 54f, 62f, 52f, 60f, 44f, 64f, 44f, 62f, 52f},
			{12f, 2.1f, 4.5f, 1f, 5.1f, 7.5f, 9f, 3.1f, 4.5f, 7f, 0f, 0f, -15f, 57f, 7f, 60f, 10f, 49f, 7f, 57f, 10f, 68f, 7f, 71f, 10f, 60f, 7f, 68f, 10f, 60f, 7f, 57f, -1f, 63f, -1f, 60f, 7f},
			{13f, 3.6f, 7f, -7f, 3.6f, 8f, -6f, 3.1f, 4.5f, -8f, 0f, 0f, -15f, 49f, 64f, 49f, 65f, 48f, 64f, 49f, 65f, 50f, 64f, 50f, 65f, 49f, 64f, 50f, 65f, 49f, 64f, 49f, 63f, 49f, 63f, 49f, 64f},
			{13f, 3.6f, 7f, -9f, 3.6f, 10f, -7f, 3.1f, 4.5f, -8f, 0f, 0f, -15f, 6f, 64f, 6f, 67f, 4f, 64f, 6f, 67f, 8f, 64f, 8f, 67f, 6f, 64f, 8f, 67f, 6f, 64f, 6f, 62f, 6f, 62f, 6f, 64f},
			{13f, 3.6f, 10f, -8f, 3.6f, 11f, -6f, 3.1f, 4.5f, -8f, 0f, 0f, -15f, 30f, 54f, 30f, 55f, 28f, 54f, 30f, 55f, 32f, 54f, 32f, 55f, 30f, 54f, 32f, 55f, 30f, 54f, 30f, 52f, 30f, 52f, 30f, 54f},
			{13f, 3.6f, 11f, -7f, 3.6f, 12f, -6f, 3.1f, 4.5f, -8f, 0f, 0f, -15f, 51f, 64f, 51f, 65f, 50f, 64f, 51f, 65f, 52f, 64f, 52f, 65f, 51f, 64f, 52f, 65f, 51f, 64f, 51f, 63f, 51f, 63f, 51f, 64f},
			{13f, 3.6f, 6f, -8f, 3.6f, 7f, -7f, 3.1f, 4.5f, -8f, 0f, 0f, -15f, 65f, 63f, 65f, 64f, 64f, 63f, 65f, 64f, 66f, 63f, 66f, 64f, 65f, 63f, 66f, 64f, 65f, 63f, 65f, 62f, 65f, 62f, 65f, 63f},
			{13f, 3.6f, 5f, -7f, 3.6f, 7f, -5f, 3.1f, 4.5f, -6f, 0f, 0f, -15f, 62f, 64f, 62f, 66f, 60f, 64f, 62f, 66f, 64f, 64f, 64f, 66f, 62f, 64f, 64f, 66f, 62f, 64f, 62f, 62f, 62f, 62f, 62f, 64f},
			{13f, 2.6f, 5f, -5f, 4.6f, 7f, 2f, 3.1f, 4.5f, 0f, 0f, 0f, -15f, 60f, 52f, 62f, 54f, 53f, 52f, 60f, 54f, 69f, 52f, 71f, 54f, 62f, 52f, 69f, 54f, 62f, 52f, 60f, 45f, 64f, 45f, 62f, 52f},
			{14f, 0f, 0.7f, 1f, 3f, 3.7f, 9f, 1f, 0.7f, 7f, 0f, 0f, -15f, 57f, 7f, 60f, 10f, 49f, 7f, 57f, 10f, 68f, 7f, 71f, 10f, 60f, 7f, 68f, 10f, 60f, 7f, 57f, -1f, 63f, -1f, 60f, 7f},
			{15f, 1.5f, 3.2f, -7f, 1.5f, 4.2f, -6f, 1f, 0.7f, -8f, 0f, 0f, -15f, 49f, 64f, 49f, 65f, 48f, 64f, 49f, 65f, 50f, 64f, 50f, 65f, 49f, 64f, 50f, 65f, 49f, 64f, 49f, 63f, 49f, 63f, 49f, 64f},
			{15f, 1.5f, 3.2f, -9f, 1.5f, 6.2f, -7f, 1f, 0.7f, -8f, 0f, 0f, -15f, 6f, 64f, 6f, 67f, 4f, 64f, 6f, 67f, 8f, 64f, 8f, 67f, 6f, 64f, 8f, 67f, 6f, 64f, 6f, 62f, 6f, 62f, 6f, 64f},
			{15f, 1.5f, 6.2f, -8f, 1.5f, 7.2f, -6f, 1f, 0.7f, -8f, 0f, 0f, -15f, 30f, 54f, 30f, 55f, 28f, 54f, 30f, 55f, 32f, 54f, 32f, 55f, 30f, 54f, 32f, 55f, 30f, 54f, 30f, 52f, 30f, 52f, 30f, 54f},
			{15f, 1.5f, 7.2f, -7f, 1.5f, 8.2f, -6f, 1f, 0.7f, -8f, 0f, 0f, -15f, 51f, 64f, 51f, 65f, 50f, 64f, 51f, 65f, 52f, 64f, 52f, 65f, 51f, 64f, 52f, 65f, 51f, 64f, 51f, 63f, 51f, 63f, 51f, 64f},
			{15f, 1.5f, 2.2f, -8f, 1.5f, 3.2f, -7f, 1f, 0.7f, -8f, 0f, 0f, -15f, 65f, 63f, 65f, 64f, 64f, 63f, 65f, 64f, 66f, 63f, 66f, 64f, 65f, 63f, 66f, 64f, 65f, 63f, 65f, 62f, 65f, 62f, 65f, 63f},
			{15f, 1.5f, 1.2f, -7f, 1.5f, 3.2f, -5f, 1f, 0.7f, -6f, 0f, 0f, -15f, 62f, 64f, 62f, 66f, 60f, 64f, 62f, 66f, 64f, 64f, 64f, 66f, 62f, 64f, 64f, 66f, 62f, 64f, 62f, 62f, 62f, 62f, 62f, 64f},
			{15f, 0.5f, 1.2f, -5f, 2.5f, 3.2f, 2f, 1f, 0.7f, 0f, 0f, 0f, -15f, 60f, 52f, 62f, 54f, 53f, 52f, 60f, 54f, 69f, 52f, 71f, 54f, 62f, 52f, 69f, 54f, 62f, 52f, 60f, 45f, 64f, 45f, 62f, 52f},
			{16f, -4.4f, 0.6f, 1f, -1.4f, 3.6f, 9f, -3.4f, 0.6f, 7f, 0f, 0f, -15f, 57f, 7f, 60f, 10f, 49f, 7f, 57f, 10f, 68f, 7f, 71f, 10f, 60f, 7f, 68f, 10f, 60f, 7f, 57f, -1f, 63f, -1f, 60f, 7f},
			{17f, -2.9f, 3.1f, -7f, -2.9f, 4.1f, -6f, -3.4f, 0.6f, -8f, 0f, 0f, -15f, 49f, 64f, 49f, 65f, 48f, 64f, 49f, 65f, 50f, 64f, 50f, 65f, 49f, 64f, 50f, 65f, 49f, 64f, 49f, 63f, 49f, 63f, 49f, 64f},
			{17f, -2.9f, 3.1f, -9f, -2.9f, 6.1f, -7f, -3.4f, 0.6f, -8f, 0f, 0f, -15f, 6f, 64f, 6f, 67f, 4f, 64f, 6f, 67f, 8f, 64f, 8f, 67f, 6f, 64f, 8f, 67f, 6f, 64f, 6f, 62f, 6f, 62f, 6f, 64f},
			{17f, -2.9f, 6.1f, -8f, -2.9f, 7.1f, -6f, -3.4f, 0.6f, -8f, 0f, 0f, -15f, 30f, 54f, 30f, 55f, 28f, 54f, 30f, 55f, 32f, 54f, 32f, 55f, 30f, 54f, 32f, 55f, 30f, 54f, 30f, 52f, 30f, 52f, 30f, 54f},
			{17f, -2.9f, 7.1f, -7f, -2.9f, 8.1f, -6f, -3.4f, 0.6f, -8f, 0f, 0f, -15f, 51f, 64f, 51f, 65f, 50f, 64f, 51f, 65f, 52f, 64f, 52f, 65f, 51f, 64f, 52f, 65f, 51f, 64f, 51f, 63f, 51f, 63f, 51f, 64f},
			{17f, -2.9f, 2.1f, -8f, -2.9f, 3.1f, -7f, -3.4f, 0.6f, -8f, 0f, 0f, -15f, 65f, 63f, 65f, 64f, 64f, 63f, 65f, 64f, 66f, 63f, 66f, 64f, 65f, 63f, 66f, 64f, 65f, 63f, 65f, 62f, 65f, 62f, 65f, 63f},
			{17f, -2.9f, 1.1f, -7f, -2.9f, 3.1f, -5f, -3.4f, 0.6f, -6f, 0f, 0f, -15f, 62f, 64f, 62f, 66f, 60f, 64f, 62f, 66f, 64f, 64f, 64f, 66f, 62f, 64f, 64f, 66f, 62f, 64f, 62f, 62f, 62f, 62f, 62f, 64f},
			{17f, -3.9f, 1.1f, -5f, -1.9f, 3.1f, 3f, -3.4f, 0.6f, 0f, 0f, 0f, -15f, 60f, 52f, 62f, 54f, 52f, 52f, 60f, 54f, 70f, 52f, 72f, 54f, 62f, 52f, 70f, 54f, 62f, 52f, 60f, 44f, 64f, 44f, 62f, 52f}
	};

	/** Los hijos de cada grupo (grupos y cubos), armados una sola vez. */
	private static final List<List<Integer>> HIJOS = new ArrayList<>(), CUBOS_DE = new ArrayList<>();

	static {
		for (int i = 0; i < NODOS.length; i++) {
			HIJOS.add(new ArrayList<>());
			CUBOS_DE.add(new ArrayList<>());
		}
		for (int i = 0; i < NODOS.length; i++) if (NODOS[i][0] >= 0) HIJOS.get((int) NODOS[i][0]).add(i);
		for (int i = 0; i < CUBOS.length; i++) CUBOS_DE.get((int) CUBOS[i][0]).add(i);
	}

	public NautilusOseoGrandeRenderer(EntityRendererProvider.Context contexto) {
		super(contexto);
		this.shadowRadius = 1.0f;
	}

	@Override
	public void render(NautilusOseoGrandeEntity bicho, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		// Los tentáculos (el frente) quedan para donde mira el bicho.
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(parcial, bicho.yBodyRotO, bicho.yBodyRot)));
		pose.scale(ESCALA / 16f, ESCALA / 16f, ESCALA / 16f);
		pose.translate(0, 0, CORRER_Z);
		float t = (bicho.tickCount + parcial) / 20f;
		// Se mueve (o embiste): animación walk; quieto: idle. Embistiendo va el doble de rápido.
		boolean camina = bicho.getDeltaMovement().lengthSqr() > 0.0004 || bicho.embistiendo();
		float tw = bicho.embistiendo() ? t * 2 : t;
		Animacion a = new Animacion(triangulo(t), triangulo(tw), camina,
				bicho.deathTime > 0 ? Math.min(1f, (bicho.deathTime + parcial) / 15f) * -30f : 0);
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
		int overlay = LivingEntityRenderer.getOverlayCoords(bicho, 0);
		for (int i = 0; i < NODOS.length; i++) if (NODOS[i][0] < 0) grupo(pose, vc, i, a, luz, overlay);
		pose.popPose();
		super.render(bicho, yaw, parcial, pose, buffers, luz);
	}

	private record Animacion(float idle, float walk, boolean camina, float muerte) {}

	/** Sube de 0 a 1 en el primer segundo y baja a 0 en el segundo (las animaciones del diseño duran 2 segundos). */
	private static float triangulo(float t) {
		float f = t % 2f;
		return f < 1 ? f : 2 - f;
	}

	/** Un grupo: gira sobre su centro (más la animación) y dibuja sus cubos y sus grupos hijos. */
	private static void grupo(PoseStack pose, VertexConsumer vc, int i, Animacion a, int luz, int overlay) {
		float[] n = NODOS[i];
		pose.pushPose();
		float rx = n[4], ry = n[5], rz = n[6];
		// Blockbench resta el giro en X de la animación y suma el de Z.
		rx -= a.camina() ? n[8] * a.walk() : n[7] * a.idle();
		if (i == 0) {
			if (a.camina()) rx -= -2.5f;            // walk: el cuerpo inclinado
			else pose.translate(0, 0.3f * a.idle(), 0); // idle: sube y baja
			rz += a.muerte();                       // death: se cae de costado
		}
		pose.translate(n[1], n[2], n[3]);
		if (rz != 0) pose.mulPose(Axis.ZP.rotationDegrees(rz));
		if (ry != 0) pose.mulPose(Axis.YP.rotationDegrees(ry));
		if (rx != 0) pose.mulPose(Axis.XP.rotationDegrees(rx));
		pose.translate(-n[1], -n[2], -n[3]);
		for (int c : CUBOS_DE.get(i)) cubo(pose, vc, CUBOS[c], luz, overlay);
		for (int h : HIJOS.get(i)) grupo(pose, vc, h, a, luz, overlay);
		pose.popPose();
	}

	/** Un cubo: lo gira alrededor de su centro (Z, Y, X como en Blockbench) y dibuja sus 6 caras. */
	private static void cubo(PoseStack pose, VertexConsumer vc, float[] c, int luz, int overlay) {
		pose.pushPose();
		pose.translate(c[7], c[8], c[9]);
		if (c[12] != 0) pose.mulPose(Axis.ZP.rotationDegrees(c[12]));
		if (c[11] != 0) pose.mulPose(Axis.YP.rotationDegrees(c[11]));
		if (c[10] != 0) pose.mulPose(Axis.XP.rotationDegrees(c[10]));
		pose.translate(-c[7], -c[8], -c[9]);
		float x0 = c[1], y0 = c[2], z0 = c[3], x1 = c[4], y1 = c[5], z1 = c[6];
		PoseStack.Pose p = pose.last();
		cara(vc, p, c, 13, luz, overlay, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);   // norte
		cara(vc, p, c, 17, luz, overlay, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);    // este
		cara(vc, p, c, 21, luz, overlay, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);    // sur
		cara(vc, p, c, 25, luz, overlay, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);   // oeste
		cara(vc, p, c, 29, luz, overlay, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);    // arriba
		cara(vc, p, c, 33, luz, overlay, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);   // abajo
		pose.popPose();
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose p, float[] c, int i, int luz, int overlay, float nx, float ny, float nz,
							 float... q) {
		float u1 = c[i] / 128f, v1 = c[i + 1] / 128f, u2 = c[i + 2] / 128f, v2 = c[i + 3] / 128f;
		float[][] uv = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
		for (int k = 0; k < 4; k++) {
			vc.addVertex(p, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).setColor(255, 255, 255, 255).setUv(uv[k][0], uv[k][1])
					.setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(NautilusOseoGrandeEntity bicho) {
		return TEXTURA;
	}
}
