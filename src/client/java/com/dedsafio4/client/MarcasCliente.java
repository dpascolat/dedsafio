package com.dedsafio4.client;

import com.dedsafio4.marcas.MarcasPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Dibuja en el mundo las marcas de los admins, como waypoints: se ven a través de las paredes,
 * siempre del mismo tamaño en pantalla, con un rombo, el nombre y la distancia.
 */
public final class MarcasCliente {
	private MarcasCliente() {}

	/** Más lejos que esto, la marca se dibuja a esta distancia (en la misma dirección) para no salirse del render. */
	private static final double DISTANCIA_DIBUJO = 32.0;
	/** Tamaño en pantalla: escala del texto por bloque de distancia. */
	private static final float TAMANIO = 0.0065f;

	private static List<MarcasPayload.Marca> marcas = List.of();

	public static void actualizar(MarcasPayload payload) {
		marcas = List.copyOf(payload.marcas());
	}

	public static void limpiar() {
		marcas = List.of();
	}

	public static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (marcas.isEmpty() || mc.level == null || mc.options.hideGui) return;
		String dimension = mc.level.dimension().location().toString();
		Vec3 camara = contexto.camera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		Font font = mc.font;

		for (MarcasPayload.Marca marca : marcas) {
			if (!marca.dimension().equals(dimension)) continue;
			Vec3 relativa = new Vec3(marca.x() + 0.5, marca.y() + 1.2, marca.z() + 0.5).subtract(camara);
			double distancia = relativa.length();
			if (distancia < 1.5) continue;
			double dibujo = Math.min(distancia, DISTANCIA_DIBUJO);
			Vec3 punto = relativa.scale(dibujo / distancia);
			float escala = (float) (dibujo * TAMANIO);

			// La rotación de la cámara ya la aplica Minecraft (como con los nombres de las entidades):
			// acá solo va la posición relativa y girar el texto hacia la cámara.
			PoseStack pose = new PoseStack();
			pose.translate(punto.x, punto.y, punto.z);
			pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
			pose.scale(escala, -escala, escala);
			Matrix4f m = pose.last().pose();

			int color = 0xFF000000 | marca.color();
			linea(font, buffers, m, Component.literal("◆"), -22, color);
			linea(font, buffers, m, Component.literal(marca.nombre()), -11, color);
			linea(font, buffers, m, Component.literal((int) distancia + " m"), 0, 0xFFCCCCCC);
		}
		buffers.endBatch();
	}

	private static void linea(Font font, MultiBufferSource buffers, Matrix4f m, Component texto, float y, int color) {
		float x = -font.width(texto) / 2f;
		// A través de las paredes (con fondo) y encima nítido con sombra, como los nombres de los jugadores.
		font.drawInBatch(texto, x, y, color, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0x90000000, LightTexture.FULL_BRIGHT);
		font.drawInBatch(texto, x, y, color, true, m, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
	}
}
