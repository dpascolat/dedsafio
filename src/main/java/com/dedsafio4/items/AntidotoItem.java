package com.dedsafio4.items;

import com.dedsafio4.reptisaurios.VenenoPrimitivo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Antídoto Primitivo (la pastilla): al consumirla te da Antibiótico por 3 minutos (te saca el Veneno
 * Primitivo y sos inmune a él). En el soporte para pociones hace la Poción arrojadiza de Antibiótico.
 */
public class AntidotoItem extends Item {
	public AntidotoItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack pila) {
		return UseAnim.EAT;
	}

	@Override
	public int getUseDuration(ItemStack pila, LivingEntity quien) {
		return 32;
	}

	@Override
	public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player jugador,
																	  net.minecraft.world.InteractionHand mano) {
		return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, jugador, mano);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide) {
			quien.removeEffect(VenenoPrimitivo.EFECTO);
			quien.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.dedsafio4.pociones.ModPociones.EFECTO_ANTIBIOTICO, 3 * 60 * 20));
			level.playSound(null, quien.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.4f);
		}
		if (quien instanceof Player jugador && !jugador.getAbilities().instabuild) pila.shrink(1);
		return pila;
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		ChatFormatting B = ChatFormatting.WHITE, V = ChatFormatting.LIGHT_PURPLE, R = ChatFormatting.RED;
		texto.add(Component.literal("Antibiótico (03:00)").withStyle(ChatFormatting.BLUE));
		texto.add(Component.empty());
		texto.add(Component.literal("Consúmelo para ser inmune al").withStyle(B));
		texto.add(Component.literal("Veneno Primitivo").withStyle(V).append(Component.literal(" durante un").withStyle(B)));
		texto.add(Component.literal("tiempo.").withStyle(B));
		texto.add(Component.empty());
		texto.add(Component.literal("⚠ ").withStyle(ChatFormatting.YELLOW).append(Component.literal("Atención:").withStyle(ChatFormatting.GOLD))
				.append(Component.literal(" El ").withStyle(B)).append(Component.literal("Veneno Primitivo").withStyle(V)));
		texto.add(Component.literal("no permite que te cures, por lo").withStyle(B));
		texto.add(Component.literal("que recibirás daño hasta morir.").withStyle(B));
		texto.add(Component.literal("Puedes evitarlo con la ").withStyle(B).append(Component.literal("Máscara").withStyle(V)));
		texto.add(Component.literal("Anti-Esporas").withStyle(V).append(Component.literal(" o el ").withStyle(B))
				.append(Component.literal("Antídoto Primitivo").withStyle(ChatFormatting.AQUA)).append(Component.literal(".").withStyle(B)));
		texto.add(Component.empty());
		texto.add(Component.literal("El ").withStyle(B).append(Component.literal("Veneno Primitivo").withStyle(V)).append(Component.literal(" es aplicado").withStyle(B)));
		texto.add(Component.literal("por los ").withStyle(B).append(Component.literal("Reptisaurios").withStyle(R))
				.append(Component.literal(" y las ").withStyle(B)).append(Component.literal("Nubes").withStyle(B)));
		texto.add(Component.literal("Esporas").withStyle(B).append(Component.literal(" de la ").withStyle(B))
				.append(Component.literal("Planta Tóxica").withStyle(R)).append(Component.literal(".").withStyle(B)));
	}
}
