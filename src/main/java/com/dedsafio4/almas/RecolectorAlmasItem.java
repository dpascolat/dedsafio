package com.dedsafio4.almas;

import com.dedsafio4.dimension.Limbo;
import com.dedsafio4.items.ModItems;
import com.dedsafio4.nave.ModEntidades;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Recolector de Almas: empieza gastado (0 almas). Cada Fantasma Negro o Fantasma Amarillo que mata el jugador que lo
 * lleva en la mano secundaria, estando en el Limbo, le suma 1 alma; los otros fantasmas no cuentan. Lleno (100), el
 * DUEÑO de un Almita del Limbo (AlmitasLimbo) le da clic derecho con él y recupera su alma para revivir; se vuelve a
 * gastar. Cada jugador puede recuperar su alma una sola vez. En la mano principal pega como un palo (2 de daño, 1,6 de
 * velocidad). La carga se guarda en el ítem (custom_data "dedsafio4_carga") y se ve en la barrita y en la descripción.
 */
public class RecolectorAlmasItem extends Item {
	public static final int LLENO = 100;
	private static final String CLAVE = "dedsafio4_carga";
	private static final int CELESTE = 0x5FE3F0, LILA = 0xC883FF, BLANCO = 0xFFFFFF, NARANJA = 0xFFB347, AGUA = 0x7DF9E0,
			ROJO = 0xFF5555, AMARILLO = 0xFFFF55;

	public RecolectorAlmasItem(Properties propiedades) {
		super(propiedades.stacksTo(1).rarity(Rarity.EPIC).attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 1.0, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build()));
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
		return Component.translatable(getDescriptionId()).withColor(LILA);
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

	private static MutableComponent t(String texto, int color) {
		return Component.literal(texto).withColor(color);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(t("Utilízalo en la ", BLANCO).append(t("Mano Secundaria", NARANJA)));
		texto.add(t("mientras estás en el Limbo.", BLANCO));
		texto.add(Component.empty());
		texto.add(t("Por cada una de estas criaturas", BLANCO));
		texto.add(t("que elimines, sumarás ", BLANCO).append(t("1 Alma", CELESTE)).append(t(" en el", BLANCO)));
		texto.add(t("Recolector. Llenarlo requiere ", BLANCO).append(t("100", AGUA)).append(t(".", BLANCO)));
		texto.add(Component.empty());
		texto.add(t("Cuando esté lleno, la persona", BLANCO));
		texto.add(t("que sea DUEÑA del Alma que", BLANCO));
		texto.add(t("encuentres en el Limbo debe", BLANCO));
		texto.add(t("darle ", BLANCO).append(t("Click Derecho", NARANJA)).append(t(" con el", BLANCO)));
		texto.add(t("Recolector para recuperarla.", BLANCO));
		texto.add(Component.empty());
		texto.add(t("Para cargarlo, elimina:", BLANCO));
		texto.add(Component.empty());
		texto.add(t("✦ Fantasma Amarillo", ROJO));
		texto.add(t("✦ Fantasma Negro", ROJO));
		texto.add(Component.empty());
		texto.add(t("⚠ Importante: Un jugador solo", AMARILLO));
		texto.add(t("podrá recuperar su Alma UNA VEZ.", AMARILLO));
		texto.add(Component.empty());
		texto.add(t("Almas: ", BLANCO).append(t(carga(pila) + " / " + LLENO, CELESTE)));
	}

	public static void registrar() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad.getType() != ModEntidades.FANTASMA_NEGRO && entidad.getType() != ModEntidades.FANTASMA_AMARILLO) return;
			if (!(fuente.getEntity() instanceof ServerPlayer jugador)) return;
			// Sólo carga el que está en la mano secundaria, y estando en el Limbo.
			if (jugador.level().dimension() != Limbo.DIMENSION) return;
			ItemStack pila = jugador.getOffhandItem();
			if (!pila.is(ModItems.RECOLECTOR_ALMAS) || carga(pila) >= LLENO) return;
			int nueva = carga(pila) + 1;
			ponerCarga(pila, nueva);
			jugador.displayClientMessage(Component.literal("Recolector de Almas: ").withColor(LILA)
					.append(Component.literal(nueva + " / " + LLENO + (nueva >= LLENO ? " ¡lleno!" : "")).withColor(BLANCO)), true);
		});
	}
}
