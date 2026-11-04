package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Sodium sólo anima las texturas que ve dibujarse, y no se da cuenta cuando un ítem se dibuja con el brillo del
 * encantamiento: la Espada Phora encantada se quedaba quieta (si había una normal a la vista, sí se movía). Cada tick
 * se le avisa a Sodium que las texturas animadas de los ítems del mod están en uso. Sin Sodium no hace nada.
 */
public final class AnimacionesSodium {
	private AnimacionesSodium() {}

	/** Las texturas de ítems del mod que tienen animación (su .png.mcmeta). */
	private static final List<ResourceLocation> ANIMADAS = List.of(
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "item/espada_phora"),
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "item/corazon_glebanoide"));

	private static Object sodium;
	private static Method marcar;
	private static boolean buscado;

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(AnimacionesSodium::marcar);
	}

	private static void marcar(Minecraft mc) {
		if (!buscado) {
			buscado = true;
			try {
				Class<?> clase = Class.forName("net.caffeinemc.mods.sodium.api.texture.SpriteUtil");
				sodium = clase.getField("INSTANCE").get(null);
				marcar = clase.getMethod("markSpriteActive", TextureAtlasSprite.class);
			} catch (ReflectiveOperationException | LinkageError e) {
				sodium = null;   // no hay Sodium
			}
		}
		if (sodium == null || mc.level == null) return;
		var atlas = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
		try {
			for (ResourceLocation id : ANIMADAS) marcar.invoke(sodium, atlas.apply(id));
		} catch (ReflectiveOperationException e) {
			sodium = null;
		}
	}
}
