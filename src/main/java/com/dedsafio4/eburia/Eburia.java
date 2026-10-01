package com.dedsafio4.eburia;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Lo del Huevo Eburia: el efecto de Salud mejorada y las dos formas de sacarle cosas
 * (tijeras para el Pelo, hacha para la Semilla) teniéndolo en la mano secundaria.
 */
public final class Eburia {
	private Eburia() {}

	/** Cuánta vida extra da y cuánto dura. */
	public static final int VIDA_EXTRA = 4;
	public static final int DURACION = 20 * 60;

	public static final Holder<MobEffect> SALUD_MEJORADA = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "salud_mejorada"),
			new MobEffect(MobEffectCategory.BENEFICIAL, 0x7FE0FF) {}
					.addAttributeModifier(Attributes.MAX_HEALTH,
							ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "salud_mejorada"),
							VIDA_EXTRA, AttributeModifier.Operation.ADD_VALUE));

	public static void registrar() {
		// Tijeras o hacha con el Huevo en la mano secundaria.
		UseItemCallback.EVENT.register((jugador, level, mano) -> {
			ItemStack enMano = jugador.getItemInHand(mano);
			ItemStack secundaria = jugador.getOffhandItem();
			if (mano != InteractionHand.MAIN_HAND
					|| !(secundaria.is(ModItems.HUEVO_EBURIA) || secundaria.is(ModItems.ARANDANO_NOCTURNO)
					|| secundaria.is(ModItems.FRUTA_SOLARIA) || secundaria.is(ModItems.BAYA_UVINA))) {
				return InteractionResultHolder.pass(enMano);
			}
			ItemStack sale;
			if (!secundaria.is(ModItems.HUEVO_EBURIA)) {
				// Del Arándano, la Fruta Solaria y la Baya Uvina solo sale la semilla, con el hacha.
				if (!enMano.is(net.minecraft.tags.ItemTags.AXES)) return InteractionResultHolder.pass(enMano);
				sale = new ItemStack(secundaria.is(ModItems.ARANDANO_NOCTURNO) ? ModItems.SEMILLA_ARANDANO
						: secundaria.is(ModItems.FRUTA_SOLARIA) ? ModItems.SEMILLA_SOLARIA : ModItems.SEMILLA_UVINA);
			} else if (enMano.is(Items.SHEARS)) {
				sale = new ItemStack(ModItems.PELO_EBURIA);
			} else if (enMano.is(net.minecraft.tags.ItemTags.AXES)) {
				sale = new ItemStack(ModItems.SEMILLA_EBURIA);
			} else {
				return InteractionResultHolder.pass(enMano);
			}
			if (level.isClientSide) return InteractionResultHolder.success(enMano);

			secundaria.shrink(1);
			if (!jugador.getInventory().add(sale)) jugador.drop(sale, false);
			enMano.hurtAndBreak(1, jugador, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			level.playSound(null, jugador.blockPosition(),
					enMano.is(Items.SHEARS) ? SoundEvents.SHEEP_SHEAR : SoundEvents.WOOD_BREAK,
					SoundSource.PLAYERS, 0.8f, 1.1f);
			jugador.displayClientMessage(Component.literal("Sacaste " + sale.getHoverName().getString())
					.withStyle(ChatFormatting.AQUA), true);
			return InteractionResultHolder.consume(enMano);
		});
	}

	/** Para el texto del ítem. */
	public static Component nombreEfecto() {
		return Component.translatable("effect.dedsafio4.salud_mejorada");
	}

	public static void nada(Player jugador, Level level) {
	}
}
