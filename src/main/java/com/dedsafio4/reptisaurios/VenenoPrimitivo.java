package com.dedsafio4.reptisaurios;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Veneno Primitivo: mientras lo tenés no podés curarte y vas recibiendo daño hasta morir.
 * Se saca con el Antídoto Primitivo o poniéndote la Máscara Anti-Esporas; con el Antibiótico no te agarra.
 */
public class VenenoPrimitivo extends MobEffect {
	/** Una hora: en la práctica, hasta que te cures o te mueras. */
	public static final int DURACION = 20 * 60 * 60;
	/** Un corazón cada 2 segundos. */
	private static final int CADA = 40;

	public static final Holder<MobEffect> EFECTO = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "veneno_primitivo"), new VenenoPrimitivo());

	private VenenoPrimitivo() {
		super(MobEffectCategory.HARMFUL, 0x6FA83C);
	}

	/** Se lo aplica a quien no tenga puesta la Máscara Anti-Esporas. */
	public static void aplicar(LivingEntity victima) {
		if (protegido(victima)) return;
		victima.addEffect(new MobEffectInstance(EFECTO, DURACION, 0, false, true, true));
	}

	public static boolean protegido(LivingEntity quien) {
		return quien.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MASCARA_ANTI_ESPORAS)
				|| quien.hasEffect(com.dedsafio4.pociones.ModPociones.EFECTO_ANTIBIOTICO);
	}

	public static boolean envenenado(LivingEntity quien) {
		return quien.hasEffect(EFECTO);
	}

	@Override
	public boolean applyEffectTick(LivingEntity victima, int nivel) {
		// La máscara lo saca en cuanto te la ponés.
		if (protegido(victima)) {
			victima.removeEffect(EFECTO);
			return true;
		}
		if (victima.tickCount % CADA == 0) victima.hurt(victima.damageSources().magic(), 1f + nivel);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duracion, int nivel) {
		return true;   // se revisa siempre; el daño va cada 2 segundos, arriba
	}

	/** Fuerza la carga de la clase para que se registre. */
	public static void registrar() {}
}
