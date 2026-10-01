package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Píldora: al consumirla contrarresta los efectos de la Zombificación durante la duración actual
 * (24 horas de juego conectado). Da el efecto "Protección contra Zombificación".
 */
public class PildoraItem extends Item {
	/** Cuántas horas dura la protección (la "Duración Actual" del tooltip). */
	public static final int HORAS = 24;
	public static final int DURACION = HORAS * 60 * 60 * 20;

	public static final Holder<MobEffect> PROTECCION_ZOMBIFICACION = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "proteccion_zombificacion"),
			new MobEffect(MobEffectCategory.BENEFICIAL, 0x4F9BE8) {});

	private static final int CELESTE = 0x55C6E8, TEXTO = 0xC6C6C6, VIOLETA = 0xB86BD8, VERDE = 0x6DBE6D;

	public PildoraItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide) {
			quien.addEffect(new MobEffectInstance(PROTECCION_ZOMBIFICACION, DURACION, 0, false, true, true));
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Consume esta Píldora para", TEXTO));
		texto.add(parte("contrarrestar los efectos", TEXTO));
		texto.add(parte("de la ", TEXTO).append(parte("Zombificación", VIOLETA)).append(parte(".", TEXTO)));
		texto.add(Component.empty());
		texto.add(parte("Duración Actual: ", VERDE).append(parte(HORAS + " Horas", TEXTO)));
	}

	/** Fuerza la carga de la clase (registra el efecto). */
	public static void registrar() {}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}
}
