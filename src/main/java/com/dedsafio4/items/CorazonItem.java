package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Corazón: al comerlo te da un corazón más para siempre (se van sumando, y no se pierden al morir).
 * Se puede comer aunque no tengas hambre.
 */
public class CorazonItem extends Item {
	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "corazones_comidos");
	/** 1 corazón = 2 puntos de vida. */
	private static final double POR_CORAZON = 2;

	public CorazonItem(Properties propiedades) {
		super(propiedades);
	}

	/** Cuántos puntos de vida extra le dieron los corazones que comió. */
	public static double extra(LivingEntity quien) {
		AttributeInstance vida = quien.getAttribute(Attributes.MAX_HEALTH);
		AttributeModifier m = vida == null ? null : vida.getModifier(ID);
		return m == null ? 0 : m.amount();
	}

	private static void ponerExtra(LivingEntity quien, double cantidad) {
		AttributeInstance vida = quien.getAttribute(Attributes.MAX_HEALTH);
		if (vida == null) return;
		vida.removeModifier(ID);
		if (cantidad > 0) vida.addPermanentModifier(new AttributeModifier(ID, cantidad, AttributeModifier.Operation.ADD_VALUE));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack pila, Level level, LivingEntity quien) {
		if (!level.isClientSide) {
			ponerExtra(quien, extra(quien) + POR_CORAZON);
			quien.heal((float) POR_CORAZON);   // el corazón nuevo viene lleno
			level.playSound(null, quien.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.6f);
		}
		return super.finishUsingItem(pila, level, quien);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Cómelo para tener un corazón").withColor(0xC6CFD6));
		texto.add(Component.literal("más para siempre.").withColor(0xC6CFD6));
	}

	/** Al morir (o volver del End) se conservan los corazones comidos. */
	public static void registrar() {
		ServerPlayerEvents.COPY_FROM.register((viejo, nuevo, vivo) -> {
			double extra = extra(viejo);
			if (extra > 0) {
				ponerExtra(nuevo, extra);
				if (vivo) nuevo.setHealth(viejo.getHealth());
				else nuevo.setHealth(nuevo.getMaxHealth());
			}
		});
	}
}
