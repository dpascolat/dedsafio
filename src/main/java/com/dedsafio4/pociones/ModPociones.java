package com.dedsafio4.pociones;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

/**
 * Pociones que solo cambian lo que ve el jugador (el efecto se dibuja en el cliente):
 * - Rojizo: se ve todo teñido de rojo.
 * - Negro Puro: todos los bloques se ven negros.
 * - Corazones Ocultos: no ves tus corazones (ni los tuyos ni cuánta vida te queda).
 * Y el Antibiótico (solo arrojadiza, 5 minutos; se hace con el Antídoto Primitivo en el soporte para
 * pociones): te saca el Veneno Primitivo y mientras dura sos inmune a él.
 */
public final class ModPociones {
	private ModPociones() {}

	/** 3 minutos, como las pociones normales. */
	private static final int DURACION = 3 * 60 * 20;

	public static final Holder<MobEffect> EFECTO_ROJIZO = efecto("rojizo", 0xC4161C);
	public static final Holder<MobEffect> EFECTO_NEGRO_PURO = efecto("negro_puro", 0x101010);
	public static final Holder<MobEffect> EFECTO_CORAZONES_OCULTOS = efecto("corazones_ocultos", 0x7A1F2B);

	public static final Holder<Potion> ROJIZO = pocion("rojizo", EFECTO_ROJIZO);
	public static final Holder<Potion> NEGRO_PURO = pocion("negro_puro", EFECTO_NEGRO_PURO);
	public static final Holder<Potion> CORAZONES_OCULTOS = pocion("corazones_ocultos", EFECTO_CORAZONES_OCULTOS);

	public static final Holder<MobEffect> EFECTO_ANTIBIOTICO = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "antibiotico"), new Antibiotico());
	/** 5 minutos. Solo existe arrojadiza (las otras formas se sacan del inventario creativo). */
	public static final Holder<Potion> ANTIBIOTICO = Registry.registerForHolder(BuiltInRegistries.POTION,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "antibiotico"),
			new Potion("antibiotico", new MobEffectInstance(EFECTO_ANTIBIOTICO, 5 * 60 * 20)));

	/** Antibiótico: inmune al Veneno Primitivo mientras dura (si lo tenías, te lo saca). */
	private static final class Antibiotico extends MobEffect {
		Antibiotico() {
			super(MobEffectCategory.BENEFICIAL, 0x2FB85A);
		}

		@Override
		public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity quien, int nivel) {
			if (quien.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO)) quien.removeEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO);
			return true;
		}

		@Override
		public boolean shouldApplyEffectTickThisTick(int duracion, int nivel) {
			return true;
		}
	}

	private static Holder<MobEffect> efecto(String nombre, int color) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
				ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre), new EfectoVisual(color));
	}

	/** Sin partículas: no se nota quién tomó la poción. */
	private static Holder<Potion> pocion(String nombre, Holder<MobEffect> efecto) {
		return Registry.registerForHolder(BuiltInRegistries.POTION,
				ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre),
				new Potion(nombre, new MobEffectInstance(efecto, DURACION, 0, false, false, true)));
	}

	/** Efecto sin nada del lado del servidor: el cliente mira si el jugador lo tiene. */
	private static final class EfectoVisual extends MobEffect {
		EfectoVisual(int color) {
			super(MobEffectCategory.HARMFUL, color);
		}
	}

	/** Fuerza la carga de la clase para que se registren, y deja el Antibiótico solo como arrojadiza. */
	public static void registrar() {
		for (var tab : java.util.List.of(net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS, net.minecraft.world.item.CreativeModeTabs.COMBAT)) {
			net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(tab).register(entradas -> {
				entradas.getDisplayStacks().removeIf(ModPociones::antibioticoNoArrojadizo);
				entradas.getSearchTabStacks().removeIf(ModPociones::antibioticoNoArrojadizo);
			});
		}
	}

	private static boolean antibioticoNoArrojadizo(net.minecraft.world.item.ItemStack stack) {
		var contenido = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
		return contenido != null && contenido.is(ANTIBIOTICO) && !stack.is(net.minecraft.world.item.Items.SPLASH_POTION);
	}
}
