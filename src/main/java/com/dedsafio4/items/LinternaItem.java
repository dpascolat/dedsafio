package com.dedsafio4.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Linterna: con click derecho se prende o se apaga. Prendida ilumina adonde apunta y lastima a las
 * criaturas sensibles a la luz (eso lo hace {@link com.dedsafio4.items.Linternas}); gasta batería.
 * Con una Batería de Dilitio en la mano secundaria, el click derecho la recarga.
 */
public class LinternaItem extends Item {
	public static final int BATERIA = 33;
	private static final int CELESTE = 0x55D9F0, GRIS = 0xC6CFD6, VIOLETA = 0xC864E0, NARANJA = 0xFFA23C;

	public LinternaItem(Properties propiedades) {
		super(propiedades.stacksTo(1));
	}

	/**
	 * La carga de la batería (0 a 33). Se guarda aparte y no como durabilidad, porque Minecraft
	 * rompe las herramientas que llegan a 0 y la Linterna se tiene que poder recargar.
	 */
	public static int bateria(ItemStack pila) {
		CustomData datos = pila.get(DataComponents.CUSTOM_DATA);
		return datos != null && datos.contains("bateria") ? datos.copyTag().getInt("bateria") : BATERIA;
	}

	public static void setBateria(ItemStack pila, int carga) {
		CustomData.update(DataComponents.CUSTOM_DATA, pila, tag -> tag.putInt("bateria", Math.max(0, Math.min(BATERIA, carga))));
	}

	// La barrita de carga debajo del ítem, como la de durabilidad.
	@Override
	public boolean isBarVisible(ItemStack pila) {
		return bateria(pila) < BATERIA;
	}

	@Override
	public int getBarWidth(ItemStack pila) {
		return Math.round(13f * bateria(pila) / BATERIA);
	}

	@Override
	public int getBarColor(ItemStack pila) {
		return net.minecraft.util.Mth.hsvToRgb(Math.max(0f, (float) bateria(pila) / BATERIA) / 3f, 1f, 1f);
	}

	public static boolean prendida(ItemStack pila) {
		CustomData datos = pila.get(DataComponents.CUSTOM_DATA);
		return datos != null && datos.copyTag().getBoolean("prendida");
	}

	public static void prender(ItemStack pila, boolean prendida) {
		CustomData.update(DataComponents.CUSTOM_DATA, pila, tag -> tag.putBoolean("prendida", prendida));
	}

	public static boolean sinBateria(ItemStack pila) {
		return bateria(pila) <= 0;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack linterna = jugador.getItemInHand(mano);
		if (level.isClientSide) return InteractionResultHolder.success(linterna);

		// Recargar con la Batería de Dilitio en la mano secundaria.
		ItemStack otra = jugador.getOffhandItem();
		if (mano == InteractionHand.MAIN_HAND && otra.is(ModItems.BATERIA_DILITIO)) {
			if (bateria(linterna) >= BATERIA) {
				jugador.displayClientMessage(Component.literal("La Linterna ya tiene la batería llena.").withColor(GRIS), true);
				return InteractionResultHolder.fail(linterna);
			}
			otra.consume(1, jugador);
			setBateria(linterna, BATERIA);
			level.playSound(null, jugador.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.6f);
			jugador.displayClientMessage(Component.literal("Linterna recargada.").withColor(CELESTE), true);
			return InteractionResultHolder.consume(linterna);
		}

		// Prender o apagar.
		if (!prendida(linterna) && sinBateria(linterna)) {
			jugador.displayClientMessage(Component.literal("La Linterna no tiene batería.").withColor(0xFF5555), true);
			level.playSound(null, jugador.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.PLAYERS, 0.6f, 0.7f);
			return InteractionResultHolder.fail(linterna);
		}
		boolean prender = !prendida(linterna);
		prender(linterna, prender);
		level.playSound(null, jugador.blockPosition(), prender ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
				SoundSource.PLAYERS, 0.6f, prender ? 1.4f : 1.1f);
		return InteractionResultHolder.consume(linterna);
	}

	@Override
	public boolean isEnchantable(ItemStack pila) {
		return false;
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId()).withStyle(color(CELESTE));
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.empty());
		texto.add(parte("Dispositivo utilizado para", GRIS));
		texto.add(parte("iluminar zonas muy oscuras.", GRIS));
		texto.add(parte("También es útil para ", GRIS).append(parte("eliminar", VIOLETA)));
		texto.add(parte("criaturas sensibles a la luz", VIOLETA).append(parte(".", GRIS)));
		texto.add(Component.empty());
		texto.add(parte("Recargable con: ", GRIS).append(parte("Batería", CELESTE)));
		texto.add(parte("de Dilitio", CELESTE).append(parte(". Debes tenerla", GRIS)));
		texto.add(parte("en la ", GRIS).append(parte("Mano Secundaria", NARANJA)).append(parte(" mientras", GRIS)));
		texto.add(parte("tienes la Linterna en la mano principal.", GRIS));
		texto.add(parte("Durabilidad: " + bateria(pila) + " / " + BATERIA, GRIS));
	}

	private static Style color(int rgb) {
		return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
	}

	private static MutableComponent parte(String texto, int rgb) {
		return Component.literal(texto).withStyle(color(rgb));
	}

	/** Gasta un punto de batería; en 0 queda sin batería (no se rompe: se recarga). */
	public static void gastar(ItemStack pila) {
		setBateria(pila, bateria(pila) - 1);
	}
}
