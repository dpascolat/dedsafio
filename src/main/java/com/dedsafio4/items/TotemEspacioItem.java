package com.dedsafio4.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Tótem del Espacio entre Dimensiones: sólo sirve en esa dimensión (dedsafio4:espacio_entre_dimensiones, se agrega más
 * adelante) y salta desde cualquier lugar del inventario. Ahí es el único tótem que funciona: los demás (el de Minecraft,
 * el de Gólem, el Glebanoide, el de Eón, el Limbo) no hacen nada (ver LivingEntityTotemEspacioMixin). Afuera no sirve.
 */
public class TotemEspacioItem extends Item {
	public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "espacio_entre_dimensiones"));

	public TotemEspacioItem(Properties propiedades) {
		super(propiedades.stacksTo(1));
	}

	/** ¿Está en el Espacio entre Dimensiones? */
	public static boolean enEspacio(Entity entidad) {
		return entidad.level().dimension().equals(DIMENSION);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0xFFD94A);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		int gris = 0xE8E8E8, amarillo = 0xFFE14A;
		texto.add(Component.empty());
		texto.add(Component.literal("Sólo sirve en Espacio entre").withColor(gris));
		texto.add(Component.literal("Dimensiones. Se consume desde").withColor(gris));
		texto.add(Component.literal("el inventario.").withColor(gris));
		texto.add(Component.empty());
		texto.add(Component.literal("⚠ Atención: Es el ÚNICO Tótem").withColor(amarillo));
		texto.add(Component.literal("que funciona en el Espacio entre").withColor(amarillo));
		texto.add(Component.literal("Dimensiones. NO funciona en otro").withColor(amarillo));
		texto.add(Component.literal("sitio.").withColor(amarillo));
	}

	/** En el Espacio entre Dimensiones, a punto de morir: si tiene este tótem en el inventario, lo salva. */
	public static boolean salvar(ServerPlayer jugador) {
		var inventario = jugador.getInventory();
		for (int i = 0; i < inventario.getContainerSize(); i++) {
			ItemStack pila = inventario.getItem(i);
			if (!pila.is(ModItems.TOTEM_ESPACIO)) continue;
			jugador.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(ModItems.TOTEM_ESPACIO));
			// El aviso 35 (animación y sonido del tótem) va antes de gastarlo, para que el cliente lo encuentre.
			jugador.level().broadcastEntityEvent(jugador, (byte) 35);
			pila.shrink(1);
			jugador.setHealth(1f);
			jugador.removeAllEffects();
			com.dedsafio4.marcas.BloqueoMinimapa.aplicarEfectos(jugador);   // el bloqueo del minimapa no se va
			jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
			jugador.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
			jugador.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
			return true;
		}
		return false;
	}
}
