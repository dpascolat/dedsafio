package com.dedsafio4.client.despegue;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.despegue.ModDespegue;
import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * El cielo del Espacio (la dimensión vacía por donde viaja la nave): negro, lleno de estrellas, con los dos planetas.
 * La nave queda quieta y lo que se mueve son los planetas: el de salida está abajo y se va alejando, el de destino
 * está arriba, chiquito, y se acerca hasta ocupar casi todo el cielo. Se dibujan en el cielo (a 90 bloques, con el
 * tamaño que tendrían a su distancia de verdad), así nunca quedan cortados por la distancia de renderizado.
 */
public final class EspacioCielo {
	private EspacioCielo() {}

	private static final ResourceLocation TIERRA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/planeta_overworld.png");
	private static final ResourceLocation ROJO = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "textures/gui/planeta_rojo.png");
	/** Radio de los planetas (en bloques) y a qué distancia se dibuja el cielo. */
	private static final float RADIO_PLANETA = 50, CIELO = 90;

	private static final int ESTRELLAS = 1500;
	private static final float[][] ESTRELLA = new float[ESTRELLAS][5];   // dirección x, y, z, tamaño, fase

	static {
		RandomSource azar = RandomSource.create(4567L);
		for (int i = 0; i < ESTRELLAS; ) {
			float x = azar.nextFloat() * 2 - 1, y = azar.nextFloat() * 2 - 1, z = azar.nextFloat() * 2 - 1;
			float largo = Mth.sqrt(x * x + y * y + z * z);
			if (largo < 0.01f || largo > 1) continue;
			ESTRELLA[i] = new float[]{x / largo, y / largo, z / largo, 0.12f + azar.nextFloat() * 0.22f, azar.nextFloat() * 6.28f};
			i++;
		}
	}

	public static void registrar() {
		DimensionRenderingRegistry.registerSkyRenderer(ModDespegue.ESPACIO, EspacioCielo::dibujar);
		DimensionRenderingRegistry.registerCloudRenderer(ModDespegue.ESPACIO, contexto -> {});
		DimensionRenderingRegistry.registerWeatherRenderer(ModDespegue.ESPACIO, contexto -> {});
	}

	private static void dibujar(WorldRenderContext contexto) {
		Minecraft mc = Minecraft.getInstance();
		Matrix4f m = contexto.positionMatrix();
		Vec3 camara = contexto.camera().getPosition();
		float parcial = contexto.tickCounter().getGameTimeDeltaPartialTick(false);
		float tiempo = (mc.level == null ? 0 : mc.level.getGameTime() % 100000L) + parcial;

		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Las estrellas: cuadraditos blancos que titilan.
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder estrellas = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (float[] e : ESTRELLA) {
			int brillo = (int) (170 + 85 * Mth.sin(tiempo * 0.08f + e[4]));
			cuadro(estrellas, m, e[0] * 100, e[1] * 100, e[2] * 100, e[3], brillo, null);
		}
		BufferUploader.drawWithShader(estrellas.buildOrThrow());

		// Los planetas, según por dónde va el viaje.
		NaveViajeEntity nave = mc.player != null && mc.player.getVehicle() instanceof NaveViajeEntity n ? n : null;
		boolean haciaRojo = nave == null || nave.haciaRojo();
		float p = NaveCinematica.progresoEspacio(parcial);
		float e = p * p * (3 - 2 * p);
		Vec3 base = nave != null ? nave.getPosition(parcial) : camara;
		Vec3 salida = base.add(-20, -(60 + 900 * e), 10);
		Vec3 destino = base.add(25 * (1 - e), 950 - 870 * e, 15 * (1 - e));
		ResourceLocation texSalida = haciaRojo ? TIERRA : ROJO, texDestino = haciaRojo ? ROJO : TIERRA;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		// Primero el más lejano.
		if (salida.distanceTo(camara) > destino.distanceTo(camara)) {
			planeta(m, camara, salida, texSalida);
			planeta(m, camara, destino, texDestino);
		} else {
			planeta(m, camara, destino, texDestino);
			planeta(m, camara, salida, texSalida);
		}

		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
	}

	/** Un planeta: un cuadro con su dibujo, mirando a la cámara, del tamaño que se vería a su distancia. */
	private static void planeta(Matrix4f m, Vec3 camara, Vec3 centro, ResourceLocation textura) {
		Vec3 hacia = centro.subtract(camara);
		double distancia = hacia.length();
		if (distancia < 1) return;
		Vec3 dir = hacia.scale(1 / distancia);
		float tam = (float) Math.min(CIELO * 3, CIELO * RADIO_PLANETA / Math.max(distancia, RADIO_PLANETA * 1.05));
		RenderSystem.setShaderTexture(0, textura);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		cuadro(b, m, (float) dir.x * CIELO, (float) dir.y * CIELO, (float) dir.z * CIELO, tam, 0, new float[1]);
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	/**
	 * Un cuadro de lado 2×{@code tam} en ese punto, mirando al centro (a la cámara). Con {@code uv} != null lleva
	 * coordenadas de textura; si no, color blanco con ese brillo.
	 */
	private static void cuadro(BufferBuilder b, Matrix4f m, float x, float y, float z, float tam, int brillo, float[] uv) {
		Vec3 d = new Vec3(x, y, z).normalize();
		Vec3 arriba = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 a = d.cross(arriba).normalize().scale(tam), c = d.cross(a).normalize().scale(tam);
		float[][] esquinas = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
		for (float[] q : esquinas) {
			float vx = (float) (x + a.x * q[0] + c.x * q[1]), vy = (float) (y + a.y * q[0] + c.y * q[1]), vz = (float) (z + a.z * q[0] + c.z * q[1]);
			if (uv != null) b.addVertex(m, vx, vy, vz).setUv(q[2], q[3]);
			else b.addVertex(m, vx, vy, vz).setColor(brillo, brillo, Math.min(255, brillo + 15), 255);
		}
	}
}
