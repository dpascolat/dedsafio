package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ArmaduraCedronida;
import com.dedsafio4.items.ArmaduraLimbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Armaduras con modelo propio ("outer_armor" de Patricio), en jugadores, mobs y soportes de armadura:
 * <ul>
 *   <li>La del Limbo (textures/models/armor/limbo_armadura.png): capa de adentro +0,3 y de afuera +0,5, más finas que en Blockbench.</li>
 *   <li>La Cedrónida, con el modelo de la armadura de Hami (textures/models/armor/cedronida_hami.png): igual de fina que la
 *   del Limbo y el casco con dos cuernos y la gema en la frente.</li>
 * </ul>
 * El casco pone la cabeza, la pechera el cuerpo (las dos capas) y los brazos, los pantalones la mitad de arriba de las
 * piernas y las botas la mitad de abajo. La armadura normal de Minecraft no se dibuja (HumanoidArmorLimboMixin).
 */
public class ArmaduraLimboLayer<E extends LivingEntity, M extends HumanoidModel<E>> extends RenderLayer<E, M> {
	private static final Modelo LIMBO = new Modelo(0.3f, 0.5f, 12, cabeza -> {});
	private static final Modelo HAMI = new Modelo(0.3f, 0.5f, 10, ArmaduraLimboLayer::cuernosYGema);

	private final ResourceLocation textura;
	private final Modelo modelo;
	private final Predicate<ItemStack> esDeEsta;

	private ArmaduraLimboLayer(RenderLayerParent<E, M> padre, String textura, Modelo modelo, Predicate<ItemStack> esDeEsta) {
		super(padre);
		this.textura = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/models/armor/" + textura + ".png");
		this.modelo = modelo;
		this.esDeEsta = esDeEsta;
	}

	/** Sin tipos: se registra para cualquier dibujante con cuerpo de persona. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static ArmaduraLimboLayer limbo(RenderLayerParent padre) {
		return new ArmaduraLimboLayer(padre, "limbo_armadura", LIMBO, (Predicate<ItemStack>) p -> p.getItem() instanceof ArmaduraLimbo.Pieza);
	}

	/** Sin tipos: se registra para cualquier dibujante con cuerpo de persona. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static ArmaduraLimboLayer cedronida(RenderLayerParent padre) {
		return new ArmaduraLimboLayer(padre, "cedronida_hami", HAMI, (Predicate<ItemStack>) p -> p.getItem() instanceof ArmaduraCedronida.Pieza);
	}

	/**
	 * Lo que el casco de Hami tiene arriba de la cabeza (pasado de Blockbench: x y y dados vuelta, el giro en z igual).
	 * Cada cuerno es una caja que sale del costado, inclinada 35°, con una punta plana inclinada 60° más.
	 */
	private static void cuernosYGema(PartDefinition cabeza) {
		PartDefinition der = cabeza.addOrReplaceChild("cuerno_der", CubeListBuilder.create()
				.texOffs(40, 36).addBox(-1.5f, -6, -1.5f, 3, 7, 3), PartPose.offsetAndRotation(-6, -4.5f, 0, 0, 0, (float) Math.toRadians(-35)));
		der.addOrReplaceChild("punta_der", CubeListBuilder.create()
				.texOffs(52, 36).addBox(-2.5f, -7, -0.05f, 5, 7, 0.1f), PartPose.offsetAndRotation(0, -6, 0, 0, 0, (float) Math.toRadians(60)));
		PartDefinition izq = cabeza.addOrReplaceChild("cuerno_izq", CubeListBuilder.create().mirror()
				.texOffs(40, 36).addBox(-1.5f, -6, -1.5f, 3, 7, 3), PartPose.offsetAndRotation(6, -4.5f, 0, 0, 0, (float) Math.toRadians(35)));
		izq.addOrReplaceChild("punta_izq", CubeListBuilder.create().mirror()
				.texOffs(52, 36).addBox(-2.5f, -7, -0.05f, 5, 7, 0.1f), PartPose.offsetAndRotation(0, -6, 0, 0, 0, (float) Math.toRadians(-60)));
		// La gema: un rombo (cuadrado girado 45°) y el centro rosa adelante, pegada a la frente del casco fino.
		cabeza.addOrReplaceChild("gema", CubeListBuilder.create()
				.texOffs(40, 32).addBox(-1, -1, -0.25f, 2, 2, 0.5f)
				.texOffs(46, 32).addBox(-0.5f, -0.5f, -0.5f, 1, 1, 0.25f), PartPose.offsetAndRotation(0, -5, -4.5f, 0, 0, (float) Math.toRadians(45)));
	}

	/** Las cajas de una armadura con modelo propio. */
	private static final class Modelo {
		final ModelPart cabeza, cuerpo, brazoDer, brazoIzq, pantalonDer, pantalonIzq, botaDer, botaIzq;

		/**
		 * @param brazoAdentro alto de la capa de adentro de los brazos (la de afuera siempre es de 12)
		 * @param extras       lo que se le agrega a la cabeza (los cuernos de Hami)
		 */
		Modelo(float adentro, float afuera, int brazoAdentro, Consumer<PartDefinition> extras) {
			CubeDeformation in = new CubeDeformation(adentro), out = new CubeDeformation(afuera);
			// Las botas apenas más grandes, para que donde se juntan con los pantalones no titile.
			CubeDeformation botaIn = new CubeDeformation(adentro + 0.05f), botaOut = new CubeDeformation(afuera + 0.05f);
			MeshDefinition malla = new MeshDefinition();
			var raiz = malla.getRoot();
			extras.accept(raiz.addOrReplaceChild("cabeza", CubeListBuilder.create()
					.texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, in)
					.texOffs(0, 16).addBox(-4, -8, -4, 8, 8, 8, out), PartPose.ZERO));
			raiz.addOrReplaceChild("cuerpo", CubeListBuilder.create()
					.texOffs(0, 32).addBox(-4, 0, -2, 8, 12, 4, in)
					.texOffs(32, 0).addBox(-4, 0, -2, 8, 12, 4, out), PartPose.ZERO);
			raiz.addOrReplaceChild("brazo_der", CubeListBuilder.create()
					.texOffs(0, 48).addBox(-4, -2, -2, 4, brazoAdentro, 4, in)
					.texOffs(48, 16).addBox(-4, -2, -2, 4, 12, 4, out), PartPose.ZERO);
			raiz.addOrReplaceChild("brazo_izq", CubeListBuilder.create().mirror()
					.texOffs(0, 48).addBox(0, -2, -2, 4, brazoAdentro, 4, in)
					.texOffs(48, 16).addBox(0, -2, -2, 4, 12, 4, out), PartPose.ZERO);
			// Las piernas se parten a la mitad (6 píxeles de 12): arriba los pantalones, abajo las botas. Cada mitad usa
			// la parte de la textura de la pierna que le toca (la de abajo empieza 6 filas más abajo en la textura).
			raiz.addOrReplaceChild("pantalon_der", CubeListBuilder.create()
					.texOffs(24, 32).addBox(-2, 0, -2, 4, 6, 4, in)
					.texOffs(48, 48).addBox(-2, 0, -2, 4, 6, 4, out), PartPose.ZERO);
			raiz.addOrReplaceChild("pantalon_izq", CubeListBuilder.create().mirror()
					.texOffs(24, 32).addBox(-2, 0, -2, 4, 6, 4, in)
					.texOffs(48, 48).addBox(-2, 0, -2, 4, 6, 4, out), PartPose.ZERO);
			raiz.addOrReplaceChild("bota_der", CubeListBuilder.create()
					.texOffs(24, 38).addBox(-2, 6, -2, 4, 6, 4, botaIn)
					.texOffs(48, 54).addBox(-2, 6, -2, 4, 6, 4, botaOut), PartPose.ZERO);
			raiz.addOrReplaceChild("bota_izq", CubeListBuilder.create().mirror()
					.texOffs(24, 38).addBox(-2, 6, -2, 4, 6, 4, botaIn)
					.texOffs(48, 54).addBox(-2, 6, -2, 4, 6, 4, botaOut), PartPose.ZERO);
			ModelPart todo = LayerDefinition.create(malla, 64, 64).bakeRoot();
			cabeza = todo.getChild("cabeza");
			cuerpo = todo.getChild("cuerpo");
			brazoDer = todo.getChild("brazo_der");
			brazoIzq = todo.getChild("brazo_izq");
			pantalonDer = todo.getChild("pantalon_der");
			pantalonIzq = todo.getChild("pantalon_izq");
			botaDer = todo.getChild("bota_der");
			botaIzq = todo.getChild("bota_izq");
		}
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int luz, E entidad, float giro, float paso,
					   float parcial, float edad, float cabeza, float inclinacion) {
		if (entidad.isInvisible()) return;
		// Con el Traje Espacial puesto no se ve la armadura.
		if (entidad instanceof net.minecraft.world.entity.player.Player jugador && com.dedsafio4.brazalete.TrajeEspacial.puesto(jugador)) return;
		ItemStack[] piezas = {entidad.getItemBySlot(EquipmentSlot.HEAD), entidad.getItemBySlot(EquipmentSlot.CHEST),
				entidad.getItemBySlot(EquipmentSlot.LEGS), entidad.getItemBySlot(EquipmentSlot.FEET)};
		boolean alguna = false;
		for (ItemStack p : piezas) alguna |= esDeEsta.test(p);
		if (!alguna) return;
		M m = getParentModel();
		VertexConsumer v = buffers.getBuffer(RenderType.armorCutoutNoCull(textura));
		VertexConsumer brillo = null;
		for (int i = 0; i < piezas.length; i++) {
			if (!esDeEsta.test(piezas[i])) continue;
			dibujarPieza(pose, v, luz, m, i);
			// El brillo de los encantamientos, solo en las piezas encantadas.
			if (piezas[i].hasFoil()) {
				if (brillo == null) brillo = buffers.getBuffer(RenderType.armorEntityGlint());
				dibujarPieza(pose, brillo, luz, m, i);
			}
		}
	}

	/** 0 casco, 1 pechera, 2 pantalones, 3 botas. */
	private void dibujarPieza(PoseStack pose, VertexConsumer v, int luz, HumanoidModel<?> m, int pieza) {
		switch (pieza) {
			case 0 -> pegado(pose, v, luz, m.head, modelo.cabeza);
			case 1 -> {
				pegado(pose, v, luz, m.body, modelo.cuerpo);
				pegado(pose, v, luz, m.rightArm, modelo.brazoDer);
				pegado(pose, v, luz, m.leftArm, modelo.brazoIzq);
			}
			case 2 -> {
				pegado(pose, v, luz, m.rightLeg, modelo.pantalonDer);
				pegado(pose, v, luz, m.leftLeg, modelo.pantalonIzq);
			}
			default -> {
				pegado(pose, v, luz, m.rightLeg, modelo.botaDer);
				pegado(pose, v, luz, m.leftLeg, modelo.botaIzq);
			}
		}
	}

	private static void pegado(PoseStack pose, VertexConsumer v, int luz, ModelPart parte, ModelPart caja) {
		if (!parte.visible) return;
		pose.pushPose();
		parte.translateAndRotate(pose);
		caja.render(pose, v, luz, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
