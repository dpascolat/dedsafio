package com.dedsafio4.client;

import com.dedsafio4.cambios.LunaRoja;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * La Luna Roja vista desde el cliente (cambio "luna_roja 1"): de noche, en el Overworld, la luna se dibuja roja
 * (LevelRendererMixin) y el cielo y la niebla se tiñen de rojo oscuro (CieloFogataMixin / FogRendererMixin).
 */
public final class LunaRojaCliente {
	private LunaRojaCliente() {}

	private static boolean activa;
	/** El rojo de la luna. */
	public static final float LUNA_R = 1f, LUNA_G = 0.22f, LUNA_B = 0.18f;
	/** El rojo oscuro del cielo de noche. */
	private static final Vec3 CIELO = new Vec3(0.30, 0.03, 0.03);

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(LunaRoja.Payload.TYPE, (payload, contexto) -> contexto.client().execute(() -> activa = payload.activa()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> activa = false);
	}

	/** ¿Se ve la Luna Roja ahora? (activa, en el Overworld). */
	public static boolean visible() {
		Minecraft mc = Minecraft.getInstance();
		return activa && mc.level != null && mc.level.dimension() == Level.OVERWORLD;
	}

	/** Cuánto de noche es (0 de día, 1 en plena noche), para que el rojo aparezca al anochecer. */
	public static float noche(float parcial) {
		Minecraft mc = Minecraft.getInstance();
		if (!visible()) return 0;
		float angulo = mc.level.getTimeOfDay(parcial);
		float luz = Mth.cos(angulo * Mth.TWO_PI) * 2f + 0.5f;   // como el brillo del cielo de Minecraft
		return Mth.clamp(1f - luz, 0f, 1f);
	}

	/** El color del cielo (o de la niebla) mezclado con el rojo según qué tan de noche es. */
	public static Vec3 pintar(Vec3 color, float parcial) {
		float k = noche(parcial) * 0.85f;
		if (k <= 0) return color;
		return color.lerp(CIELO, k);
	}
}
