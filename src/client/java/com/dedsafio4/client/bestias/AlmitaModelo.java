package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.AlmitaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Almita: Steve como fantasma ángel (el diseño "Steve Ángel Fantasma"): cuerpo cian transparente con la capa de afuera,
 * aureola celeste arriba de la cabeza y alas de plumas blancas en la espalda. Brilla (no le afecta la oscuridad),
 * flota subiendo y bajando y mueve las alas.
 * La textura (128×64): a la izquierda la de Steve pintada de cian; a la derecha el blanco de las plumas (arriba) y el
 * celeste de la aureola (abajo).
 */
public class AlmitaModelo extends HierarchicalModel<AlmitaEntity> {
	public static final ModelLayerLocation CAPA = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "almita"), "main");
	private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/almita.png");

	private final ModelPart raiz, cuerpo, alaDer, alaIzq, brazoDer, brazoIzq, piernaDer, piernaIzq;

	public AlmitaModelo(ModelPart raiz) {
		this.raiz = raiz;
		this.cuerpo = raiz.getChild("cuerpo");
		this.alaDer = cuerpo.getChild("ala_der");
		this.alaIzq = cuerpo.getChild("ala_izq");
		this.brazoDer = cuerpo.getChild("brazo_der");
		this.brazoIzq = cuerpo.getChild("brazo_izq");
		this.piernaDer = cuerpo.getChild("pierna_der");
		this.piernaIzq = cuerpo.getChild("pierna_izq");
	}

	@Override
	public ModelPart root() {
		return raiz;
	}

	private static float g(float grados) {
		return grados * Mth.DEG_TO_RAD;
	}

	/** Un cubo con su capa de afuera (como el gorro, la chaqueta, las mangas y el pantalón del jugador). */
	private static CubeListBuilder conCapa(int u, int v, int uCapa, int vCapa, float x, float y, float z, float w, float h, float d) {
		return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d)
				.texOffs(uCapa, vCapa).addBox(x, y, z, w, h, d, new CubeDeformation(0.5f));
	}

	public static LayerDefinition crearCapa() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition cuerpo = malla.getRoot().addOrReplaceChild("cuerpo", CubeListBuilder.create(), PartPose.ZERO);
		cuerpo.addOrReplaceChild("cabeza", conCapa(0, 0, 32, 0, -4, -8, -4, 8, 8, 8),
				PartPose.offsetAndRotation(0, 0, 0, g(-6), g(-12), 0));
		cuerpo.addOrReplaceChild("torso", conCapa(16, 16, 16, 32, -4, 0, -2, 8, 12, 4), PartPose.ZERO);
		cuerpo.addOrReplaceChild("brazo_der", conCapa(40, 16, 40, 32, -2, -2, -2, 4, 12, 4),
				PartPose.offsetAndRotation(-6, 2, 0, g(-18), 0, g(14)));
		cuerpo.addOrReplaceChild("brazo_izq", conCapa(32, 48, 48, 48, -2, -2, -2, 4, 12, 4),
				PartPose.offsetAndRotation(6, 2, 0, g(10), 0, g(-10)));
		cuerpo.addOrReplaceChild("pierna_der", conCapa(0, 16, 0, 32, -2, 0, -2, 4, 12, 4),
				PartPose.offsetAndRotation(-2, 12, 0, g(6), 0, 0));
		cuerpo.addOrReplaceChild("pierna_izq", conCapa(16, 48, 0, 48, -2, 0, -2, 4, 12, 4),
				PartPose.offsetAndRotation(2, 12, 0, g(-6), 0, 0));
		// La aureola: un anillo cuadrado de 4 cubitos, arriba de la cabeza (inclinada como ella).
		cuerpo.addOrReplaceChild("aureola", CubeListBuilder.create().texOffs(64, 16)
						.addBox(-4, -0.5f, -4, 8, 1, 1).addBox(-4, -0.5f, 3, 8, 1, 1)
						.addBox(3, -0.5f, -3, 1, 1, 6).addBox(-4, -0.5f, -3, 1, 1, 6),
				PartPose.offsetAndRotation(0, -11, 0, g(-6), g(-12), 0));
		// Las alas: filas de plumas escalonadas, en la espalda.
		int[][] filas = {{16, 3, 6}, {20, 3, 3}, {18, 3, 0}, {14, 3, -3}, {10, 3, -6}, {6, 3, -9}};
		for (int lado : new int[]{-1, 1}) {
			CubeListBuilder plumas = CubeListBuilder.create().texOffs(64, 0);
			for (int i = 0; i < filas.length; i++) {
				float largo = filas[i][0], alto = filas[i][1], y = filas[i][2];
				float centro = lado * (largo / 2 + i * 0.5f);
				plumas.addBox(centro - largo / 2, -y - alto / 2, -0.5f, largo, alto, 1);
			}
			cuerpo.addOrReplaceChild(lado < 0 ? "ala_der" : "ala_izq", plumas,
					PartPose.offsetAndRotation(lado * 2, 3, 2.6f, g(-8), g(lado * 28), g(lado * 14)));
		}
		return LayerDefinition.create(malla, 128, 64);
	}

	@Override
	public void setupAnim(AlmitaEntity e, float limbSwing, float limbSwingAmount, float edad, float giroCabeza, float inclinacionCabeza) {
		root().getAllParts().forEach(ModelPart::resetPose);
		// Flota subiendo y bajando, aletea y se le mueven un poco los brazos y las piernas.
		cuerpo.y = Mth.sin(edad * 0.1f) * 1.5f;
		float aleteo = Mth.sin(edad * 0.35f) * g(14);
		alaDer.yRot -= aleteo;
		alaIzq.yRot += aleteo;
		brazoDer.zRot += Mth.sin(edad * 0.08f) * g(5);
		brazoIzq.zRot -= Mth.sin(edad * 0.08f) * g(5);
		piernaDer.xRot += Mth.sin(edad * 0.07f) * g(6);
		piernaIzq.xRot -= Mth.sin(edad * 0.07f) * g(6);
	}

	public static class Dibujante extends MobRenderer<AlmitaEntity, AlmitaModelo> {
		public Dibujante(EntityRendererProvider.Context contexto) {
			super(contexto, new AlmitaModelo(contexto.bakeLayer(CAPA)), 0.3f);
		}

		@Override
		public ResourceLocation getTextureLocation(AlmitaEntity e) {
			return TEXTURA;
		}

		/** Transparente. */
		@Override
		protected RenderType getRenderType(AlmitaEntity e, boolean visible, boolean traslucido, boolean brillo) {
			return RenderType.entityTranslucent(TEXTURA);
		}

		/** Brilla: siempre con toda la luz. */
		@Override
		protected int getBlockLightLevel(AlmitaEntity e, BlockPos pos) {
			return 15;
		}

		@Override
		protected void scale(AlmitaEntity e, PoseStack pose, float parcial) {
			pose.scale(0.9375f, 0.9375f, 0.9375f);
		}
	}
}
