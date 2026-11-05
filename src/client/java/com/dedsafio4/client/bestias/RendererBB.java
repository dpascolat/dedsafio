package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Dibuja un bicho con un modelo de Blockbench (ver ModeloBB) y sus animaciones idle / walk / death:
 * quieto usa "idle", moviéndose "walk" y al morir "death". Las subclases pueden mover más cosas a mano.
 */
public abstract class RendererBB<T extends LivingEntity> extends EntityRenderer<T> {
	protected final ModeloBB modelo;
	private final ResourceLocation textura;
	private final float escala, giroExtra;

	/**
	 * @param nombre    el modelo (assets/dedsafio4/modelos/NOMBRE.json) y su textura (textures/entity/NOMBRE.png)
	 * @param escala    1 = el tamaño del diseño
	 * @param giroExtra 180 si el frente del diseño mira al norte (-Z, lo normal en Blockbench), 0 si mira al sur
	 */
	protected RendererBB(EntityRendererProvider.Context contexto, String nombre, float escala, float giroExtra, float sombra) {
		super(contexto);
		this.modelo = ModeloBB.cargar(nombre);
		this.textura = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/entity/" + nombre + ".png");
		this.escala = escala;
		this.giroExtra = giroExtra;
		this.shadowRadius = sombra;
	}

	@Override
	public void render(T bicho, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
		pose.pushPose();
		antes(bicho, parcial, pose);
		pose.mulPose(Axis.YP.rotationDegrees(giroExtra - Mth.rotLerp(parcial, bicho.yBodyRotO, bicho.yBodyRot)));
		pose.scale(escala / 16f, escala / 16f, escala / 16f);
		float[][] estado = modelo.estado();
		float t = (bicho.tickCount + parcial) / 20f;
		if (bicho.deathTime > 0 || !bicho.isAlive()) modelo.animar(estado, "death", (bicho.deathTime + parcial) / 20f, 1);
		else modelo.animar(estado, seMueve(bicho, parcial) ? "walk" : "idle", t, 1);
		animar(bicho, parcial, estado, pose);
		modelo.dibujar(pose, buffers.getBuffer(tipo(textura)), estado, luz, LivingEntityRenderer.getOverlayCoords(bicho, 0), color(bicho, parcial));
		pose.popPose();
		super.render(bicho, yaw, parcial, pose, buffers, luz);
	}

	/** ¿Se está moviendo? (caminando o volando). */
	protected boolean seMueve(T bicho, float parcial) {
		return bicho.walkAnimation.speed(parcial) > 0.05f || bicho.getDeltaMovement().lengthSqr() > 0.0004;
	}

	/** Antes de girar el bicho para donde mira (por ejemplo, para bajarlo). */
	protected void antes(T bicho, float parcial, PoseStack pose) {}

	/** Para mover a mano grupos del modelo o todo el bicho (ya girado y en píxeles). */
	protected void animar(T bicho, float parcial, float[][] estado, PoseStack pose) {}

	/** El color (ARGB) con que se tiñe el bicho. */
	protected int color(T bicho, float parcial) {
		return 0xFFFFFFFF;
	}

	protected RenderType tipo(ResourceLocation textura) {
		return RenderType.entityCutoutNoCull(textura);
	}

	@Override
	public ResourceLocation getTextureLocation(T bicho) {
		return textura;
	}
}
