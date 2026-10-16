package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * El mapa de la pestaña Ubicaciones de la Hermandad: visto desde arriba, como un mapa de Minecraft, alrededor del
 * jugador y con lo que el juego tiene cargado. Donde hay techo (Nether, Quiu) se ve el piso cerca de la altura del
 * jugador. Se arma de nuevo cada un rato mientras está abierto.
 */
public final class MapaHermandad {
	private MapaHermandad() {}

	public static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "hermandad_mapa");
	private static final int SIN_CARGAR = 0xFF14100E;

	private static DynamicTexture textura;
	private static int ancho, alto;
	private static long armado;
	private static int ultimaEscala;

	/** Arma (o rearma, si pasó un rato o cambió el zoom) el mapa centrado en (cx, cz): cada píxel son "escala" bloques. */
	public static void actualizar(int w, int h, int cx, int cz, int escala) {
		long ahora = net.minecraft.Util.getMillis();
		if (textura != null && w == ancho && h == alto && escala == ultimaEscala && ahora - armado < 1500) return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) return;
		armado = ahora;
		ultimaEscala = escala;
		int yJugador = mc.player.getBlockY();
		NativeImage img = new NativeImage(w, h, false);
		int[] alturaArriba = new int[w];
		for (int pz = 0; pz < h; pz++) {
			int z = cz + (pz - h / 2) * escala;
			for (int px = 0; px < w; px++) {
				int x = cx + (px - w / 2) * escala;
				if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
					img.setPixelRGBA(px, pz, SIN_CARGAR);
					alturaArriba[px] = Integer.MIN_VALUE;
					continue;
				}
				int y = altura(level, x, z, yJugador);
				BlockState estado = level.getBlockState(new BlockPos(x, y, z));
				MapColor color = estado.getMapColor(level, new BlockPos(x, y, z));
				MapColor.Brightness brillo = MapColor.Brightness.NORMAL;
				if (alturaArriba[px] != Integer.MIN_VALUE && pz > 0) {
					if (y > alturaArriba[px]) brillo = MapColor.Brightness.HIGH;
					else if (y < alturaArriba[px]) brillo = MapColor.Brightness.LOW;
				}
				alturaArriba[px] = y;
				img.setPixelRGBA(px, pz, color == MapColor.NONE ? SIN_CARGAR : color.calculateRGBColor(brillo));
			}
		}
		if (textura == null || w != ancho || h != alto) {
			if (textura != null) textura.close();
			textura = new DynamicTexture(img);
			mc.getTextureManager().register(TEXTURA, textura);
			ancho = w;
			alto = h;
		} else {
			textura.setPixels(img);
			textura.upload();
		}
	}

	/** Para que se vuelva a armar enseguida (al abrir la pestaña o cambiar el zoom). */
	public static void olvidar() {
		armado = 0;
	}

	/**
	 * La altura del piso en (x, z): lo más alto que hay; pero si eso queda muy por encima del jugador (un techo), el
	 * primer piso que hay bajando desde un poco arriba del jugador.
	 */
	public static int altura(ClientLevel level, int x, int z, int yJugador) {
		int arriba = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
		if (arriba <= yJugador + 30) return Math.max(level.getMinBuildHeight(), arriba);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, yJugador + 10, z);
		int minimo = level.getMinBuildHeight();
		// Si arranca adentro de la roca, baja hasta encontrar aire; después, hasta encontrar el piso.
		for (int i = 0; i < 40 && pos.getY() > minimo && !level.getBlockState(pos).isAir(); i++) pos.move(0, -1, 0);
		for (int i = 0; i < 120 && pos.getY() > minimo && level.getBlockState(pos).isAir(); i++) pos.move(0, -1, 0);
		return pos.getY();
	}
}
