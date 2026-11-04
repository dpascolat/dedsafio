package com.dedsafio4.almas;

import com.dedsafio4.items.ModItems;
import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Recolector de Almas: empieza gastado (0 %). Cada Fantasma Negro o Fantasma Amarillo que mata el jugador que lo lleva
 * en la mano secundaria le suma 1 %; los otros fantasmas no cuentan. Lleno (100 %, o sea 100 fantasmas),
 * con clic derecho sobre tu propia Almita del Limbo (AlmitasLimbo) recuperas tu alma para revivir, y se vuelve a gastar.
 * La carga se guarda en el ítem (custom_data "dedsafio4_carga") y se ve en la barrita y en la descripción.
 */
public class RecolectorAlmasItem extends Item {
	public static final int LLENO = 100;
	private static final String CLAVE = "dedsafio4_carga";
	private static final int CELESTE = 0x5FE3F0, GRIS = 0xC6CFD6;

	public RecolectorAlmasItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.EPIC));
	}

	public static int carga(ItemStack pila) {
		CustomData c = pila.get(DataComponents.CUSTOM_DATA);
		return c == null ? 0 : Mth.clamp(c.copyTag().getInt(CLAVE), 0, LLENO);
	}

	public static void ponerCarga(ItemStack pila, int carga) {
		CustomData.update(DataComponents.CUSTOM_DATA, pila, tag -> tag.putInt(CLAVE, Mth.clamp(carga, 0, LLENO)));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(CELESTE);
	}

	@Override
	public boolean isBarVisible(ItemStack pila) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack pila) {
		return Math.round(13f * carga(pila) / LLENO);
	}

	@Override
	public int getBarColor(ItemStack pila) {
		return CELESTE;
	}

	@Override
	public boolean isFoil(ItemStack pila) {
		return carga(pila) >= LLENO;
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		int carga = carga(pila);
		texto.add(Component.empty());
		texto.add(Component.literal("Carga: ").withColor(GRIS).append(Component.literal(carga + "%").withColor(CELESTE)));
		if (carga < LLENO) {
			texto.add(Component.literal("Está gastado. En la mano secundaria,").withColor(GRIS));
			texto.add(Component.literal("mata Fantasmas Negros o Amarillos (1% cada uno).").withColor(GRIS));
		} else {
			texto.add(Component.literal("¡Lleno! Haz clic derecho en tu Almita").withColor(GRIS));
			texto.add(Component.literal("del Limbo para recuperar tu alma.").withColor(GRIS));
		}
	}

	public static void registrar() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad.getType() != ModEntidades.FANTASMA_NEGRO && entidad.getType() != ModEntidades.FANTASMA_AMARILLO) return;
			if (!(fuente.getEntity() instanceof ServerPlayer jugador)) return;
			// Sólo carga el que está en la mano secundaria.
			ItemStack pila = jugador.getOffhandItem();
			if (!pila.is(ModItems.RECOLECTOR_ALMAS) || carga(pila) >= LLENO) return;
			int nueva = carga(pila) + 1;
			ponerCarga(pila, nueva);
			jugador.displayClientMessage(Component.literal("Recolector de Almas: ").withColor(CELESTE)
					.append(Component.literal(nueva + "%" + (nueva >= LLENO ? " ¡lleno!" : "")).withColor(0xFFFFFF)), true);
		});
	}
}
