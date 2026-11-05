package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.CerebroAmarilloEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * El Cerebro Amarillo con el modelo de Patricio ("cerebroamarillo"): el cerebro con 6 patas.
 * Cuando ataca (o drena estando en tu cabeza) se tira para adelante.
 */
public class CerebroAmarilloRenderer extends RendererBB<CerebroAmarilloEntity> {
	/** En primera persona, cuánto se baja el cerebro que tienes en la cabeza para que lo veas arriba de la pantalla. */
	private static final float BAJA_PRIMERA_PERSONA = 0.1F;
	/** Visto desde afuera: cuánto se baja para que quede apoyado en la cabeza, con las patas agarrándola. */
	private static final float BAJA_EN_CABEZA = 0.4F;

	public CerebroAmarilloRenderer(EntityRendererProvider.Context contexto) {
		super(contexto, "cerebro_amarillo", 1f, 180f, 0.5f);
	}

	@Override
	protected void antes(CerebroAmarilloEntity cerebro, float parcial, PoseStack pose) {
		Minecraft mc = Minecraft.getInstance();
		if (cerebro.getVehicle() == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) pose.translate(0, -BAJA_PRIMERA_PERSONA, 0);
		else if (cerebro.getVehicle() instanceof Player) pose.translate(0, -BAJA_EN_CABEZA, 0);
	}

	@Override
	protected void animar(CerebroAmarilloEntity cerebro, float parcial, float[][] estado, PoseStack pose) {
		float ataque = cerebro.getAttackAnim(parcial);
		if (ataque > 0) {
			pose.translate(0, 6, 0);
			pose.mulPose(Axis.XP.rotationDegrees(-Mth.sin(ataque * Mth.PI) * 20f));
			pose.translate(0, -6, 0);
		}
	}
}
