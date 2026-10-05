package com.dedsafio4.client.bestias;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.bestias.FlashbangEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * El Flashbang en el cliente: el modelo del Dedsafío 3 (GeckoLib: geo/entity/flashbang.geo.json, con sus
 * animaciones de caminar y de estar quieto) y la pantalla blanca de 3 segundos cuando explota cerca, que se
 * desvanece al final con un zumbido en los oídos.
 */
public final class FlashbangCliente {
	private FlashbangCliente() {}

	private static final int DURA = 60, DESVANECE = 15;
	private static int quedan;

	public static class Dibujante extends GeoEntityRenderer<FlashbangEntity> {
		public Dibujante(EntityRendererProvider.Context contexto) {
			super(contexto, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "flashbang")));
			this.shadowRadius = 0.4f;
		}
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(FlashbangEntity.DestelloPayload.TYPE, (payload, context) ->
				context.client().execute(() -> quedan = DURA));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (quedan <= 0) return;
			if (--quedan == 0) mc.getSoundManager().play(SimpleSoundInstance.forUI(FlashbangEntity.SONIDO_ZUMBIDO, 1f, 0.8f));
		});
		HudRenderCallback.EVENT.register((g, contador) -> {
			if (quedan <= 0) return;
			int alfa = quedan > DESVANECE ? 255 : 255 * quedan / DESVANECE;
			Minecraft mc = Minecraft.getInstance();
			g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), alfa << 24 | 0xFFFFFF);
		});
	}
}
