package com.dedsafio4.client.cofres;

import com.dedsafio4.cofres.CofresRellenables;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * La flecha blanca arriba de los cofres rellenables que todavía no abriste desde que se rellenaron
 * (CofresRellenables). Apunta para abajo, al cofre; sube y baja despacio, late un poco y siempre te mira de frente.
 */
public final class FlechaCofreCliente {
	private FlechaCofreCliente() {}

	private static final List<BlockPos> COFRES = new ArrayList<>();
	private static ResourceKey<Level> dimension;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(CofresRellenables.FlechasPayload.TYPE, (payload, contexto) -> contexto.client().execute(() -> {
			COFRES.clear();
			for (long p : payload.cofres()) COFRES.add(BlockPos.of(p));
			dimension = contexto.client().level != null ? contexto.client().level.dimension() : null;
		}));
		WorldRenderEvents.AFTER_TRANSLUCENT.register(FlechaCofreCliente::dibujar);
	}

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		if (COFRES.isEmpty() || mc.level == null || !mc.level.dimension().equals(dimension)) return;
		float t = mc.level.getGameTime() + contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		Vec3 camara = contexto.camera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
		for (BlockPos pos : COFRES) {
			double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
			// Sube y baja (y late un poco más grande cuando está abajo, "señalando").
			float onda = Mth.sin(t * 0.15f);
			double y = pos.getY() + 1.25 + 0.15 * onda;
			float escala = 1f + 0.08f * Math.max(0, -onda);
			float giro = (float) Math.atan2(camara.x - x, camara.z - z);
			Matrix4f m = new Matrix4f().translate((float) (x - camara.x), (float) (y - camara.y), (float) (z - camara.z))
					.rotateY(giro).scale(escala);
			// Un borde gris un poco más grande atrás (para que se vea sobre fondos claros) y la flecha blanca adelante.
			flecha(vc, m, 1.12f, -0.012f, 0xFF8A8A8A);
			flecha(vc, m, 1f, 0f, 0xFFFFFFFF);
		}
		buffers.endBatch(RenderType.debugQuads());
	}

	/** La flecha (punta abajo, en y = 0), de frente a +Z. g: tamaño; z: un poco adelante o atrás. */
	private static void flecha(VertexConsumer vc, Matrix4f m, float g, float z, int color) {
		float ancho = 0.1f * g, alto = 0.75f * g, cabeza = 0.3f * g, base = 0.32f * g;
		// El palo.
		cuadro(vc, m, -ancho, base - 0.01f, ancho, base - 0.01f, ancho, alto, -ancho, alto, z, color);
		// La punta (un triángulo: dos esquinas en el mismo lugar).
		cuadro(vc, m, -cabeza, base, cabeza, base, 0, 0, 0, 0, z, color);
	}

	/** Un cuadrilátero vertical de las dos caras. */
	private static void cuadro(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2,
							   float x3, float y3, float z, int color) {
		float[][] v = {{x0, y0}, {x1, y1}, {x2, y2}, {x3, y3}};
		int a = color >>> 24, r = color >> 16 & 255, gr = color >> 8 & 255, b = color & 255;
		for (int n : new int[]{0, 1, 2, 3, 3, 2, 1, 0}) vc.addVertex(m, v[n][0], v[n][1], z).setColor(r, gr, b, a);
	}
}
