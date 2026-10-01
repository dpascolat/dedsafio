package com.dedsafio4.estructuras;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * /struck 1 → arma la estructura 1 delante del que lo usa, hacia donde mira: cuadrados unidos por tubos, armados en
 * cuadrado para que ocupe poco. Se entra por el primer cuadrado grande (tiene la puerta del lado del jugador), que da
 * al segundo, al tercero y al cuarto; el cuarto se une con dos: el quinto grande (adelante) y el cuadrado chiquito y
 * bajito (a la izquierda).
 * Los pasillos entre grandes van 10 bloques arriba del piso (con escaleras de mano para llegar); el del chiquito va
 * a ras del piso.
 * Todo con bloques de pizarra profunda (deepslate).
 */
public final class Estructuras {
	private Estructuras() {}

	/** Los cuadrados grandes: 23×23 de lado (17×17 por dentro) y 21 de alto (con piso y techo). */
	private static final int GRANDE = 23, ALTO_GRANDE = 21;
	/** El cuadrado chiquito: 13×13 (7×7 por dentro) y bajito, 7 de alto. */
	private static final int CHICO = 13, ALTO_CHICO = 7;
	/** Las paredes tienen 3 bloques de grosor, el piso 2 y el techo 1. */
	private static final int GROSOR = 3, PISO = 2;
	/** A cuántos bloques del piso va el piso de los pasillos (adentro se sube con escaleras). */
	private static final int ALTURA_PASILLO = 10;
	/** La puerta de entrada empieza 3 bloques más arriba que el piso. */
	private static final int ALTURA_PUERTA = 3;
	/** Cuántos bloques de tubo hay entre un cuadrado y el siguiente. */
	private static final int LARGO_TUBO = 9;
	/** Los tubos: por dentro se camina (radio 1,8) y la pared llega hasta 2,9. */
	private static final double TUBO_ADENTRO = 1.8, TUBO_AFUERA = 2.9;

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("struck").requires(s -> s.hasPermission(2))
				.then(Commands.argument("numero", IntegerArgumentType.integer(1))
						.executes(c -> {
							int numero = IntegerArgumentType.getInteger(c, "numero");
							CommandSourceStack fuente = c.getSource();
							if (numero != 1) {
								fuente.sendFailure(Component.literal("Todavía no existe la estructura " + numero + "."));
								return 0;
							}
							BlockPos pie = BlockPos.containing(fuente.getPosition());
							estructura1(fuente.getLevel(), pie.below(), net.minecraft.core.Direction.fromYRot(fuente.getRotation().y));
							fuente.sendSuccess(() -> Component.literal("Listo: estructura 1 armada en " + pie.getX() + " " + pie.getY()
									+ " " + pie.getZ() + "."), true);
							return 1;
						})));
	}

	/** Una caja (cuadrado) hueca: dónde está y de qué tamaño. */
	private record Caja(int x0, int y0, int z0, int x1, int y1, int z1) {
		/** Adentro del cuarto (sin contar las paredes, que son gruesas, ni el piso y el techo). */
		boolean adentro(int x, int y, int z) {
			return x >= x0 + GROSOR && x <= x1 - GROSOR && y >= y0 + PISO && y < y1 && z >= z0 + GROSOR && z <= z1 - GROSOR;
		}

		boolean contiene(int x, int y, int z) {
			return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
		}

		double cx() { return (x0 + x1) / 2.0 + 0.5; }
		double cz() { return (z0 + z1) / 2.0 + 0.5; }
	}

	/**
	 * @param piso      debajo de los pies del jugador (a esa altura van los pisos)
	 * @param adelante  hacia dónde se arma la fila (hacia donde mira el jugador)
	 */
	public static void estructura1(ServerLevel mundo, BlockPos piso, net.minecraft.core.Direction adelante) {
		RandomSource azar = RandomSource.create(piso.asLong());
		int py = piso.getY();
		// En cuadrado (visto desde arriba, el jugador abajo):
		//            [G5]
		//   [chico]  [G4] [G3]
		//            [G1] [G2]
		net.minecraft.core.Direction derecha = adelante.getClockWise();
		int paso = GRANDE + LARGO_TUBO;
		double base = 2 + GRANDE / 2.0;   // el primer cuadrado arranca 2 bloques delante del jugador
		int[][] celdas = {{0, 0}, {1, 0}, {1, 1}, {0, 1}, {0, 2}};   // {columna a la derecha, fila hacia adelante}
		List<Caja> grandes = new ArrayList<>();
		for (int[] c : celdas) {
			double dx = adelante.getStepX() * (base + c[1] * paso) + derecha.getStepX() * c[0] * paso;
			double dz = adelante.getStepZ() * (base + c[1] * paso) + derecha.getStepZ() * c[0] * paso;
			grandes.add(caja(piso, dx, dz, GRANDE, ALTO_GRANDE));
		}
		Caja cuarto = grandes.get(3);
		double corrido = GRANDE / 2.0 + LARGO_TUBO + CHICO / 2.0;
		double chicoX = cuarto.cx() - 0.5 - derecha.getStepX() * corrido, chicoZ = cuarto.cz() - 0.5 - derecha.getStepZ() * corrido;
		Caja chico = caja(piso, chicoX - piso.getX(), chicoZ - piso.getZ(), CHICO, ALTO_CHICO);
		List<Caja> todas = new ArrayList<>(grandes);
		todas.add(chico);

		for (Caja caja : todas) armarCaja(mundo, caja, azar);
		// Los pasillos entre los grandes van con el piso 10 bloques arriba (y escaleras para subir).
		double y = py + ALTURA_PASILLO + 2.5;
		net.minecraft.core.Direction[] hacia = {derecha, adelante, derecha.getOpposite(), adelante};
		for (int k = 0; k + 1 < grandes.size(); k++) {
			Caja a = grandes.get(k), b = grandes.get(k + 1);
			armarTubo(mundo, a.cx(), y, a.cz(), b.cx(), y, b.cz(), todas, azar);
			escalera(mundo, a, hacia[k], py);
			escalera(mundo, b, hacia[k].getOpposite(), py);
		}
		// El pasillo al chiquito (sale del cuarto grande, a la izquierda) va a ras del piso: el chiquito es bajito.
		armarTubo(mundo, cuarto.cx(), py + 2.5, cuarto.cz(), chico.cx(), py + 2.5, chico.cz(), todas, azar);
		// La entrada: una puerta de 3×4 (atravesando las 3 capas de pared) en el primer cuadrado, del lado del jugador,
		// 3 bloques más arriba que el piso.
		Caja entrada = grandes.get(0);
		int px = (int) Math.floor(entrada.cx()), pz = (int) Math.floor(entrada.cz());
		for (int capa = 0; capa < GROSOR; capa++) {
			int ancho = GRANDE / 2 - capa;
			for (int lado = -1; lado <= 1; lado++) {
				for (int alto = 1 + ALTURA_PUERTA; alto <= 4 + ALTURA_PUERTA; alto++) {
					int x = px - adelante.getStepX() * ancho + (adelante.getAxis() == net.minecraft.core.Direction.Axis.Z ? lado : 0);
					int z = pz - adelante.getStepZ() * ancho + (adelante.getAxis() == net.minecraft.core.Direction.Axis.X ? lado : 0);
					mundo.setBlock(new BlockPos(x, py + alto, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Una caja de "lado"×"lado" y "alto" de alto, con el centro corrido (dx, dz) desde el piso del jugador. */
	private static Caja caja(BlockPos piso, double dx, double dz, int lado, int alto) {
		int cx = piso.getX() + (int) Math.round(dx), cz = piso.getZ() + (int) Math.round(dz);
		int py = piso.getY();
		// El piso de 2: uno a la altura del jugador y otro más abajo.
		return new Caja(cx - lado / 2, py - (PISO - 1), cz - lado / 2, cx + lado / 2, py + alto - 1, cz + lado / 2);
	}

	/** Una escalera de mano pegada a la pared de la caja que da hacia "hacia", desde el piso hasta el pasillo. */
	private static void escalera(ServerLevel mundo, Caja caja, net.minecraft.core.Direction hacia, int py) {
		int x = (int) Math.floor(caja.cx()), z = (int) Math.floor(caja.cz());
		// Avanza desde el centro hasta el último bloque de adentro antes de la pared.
		while (caja.adentro(x + hacia.getStepX(), py + 1, z + hacia.getStepZ())) {
			x += hacia.getStepX();
			z += hacia.getStepZ();
		}
		BlockState escalon = Blocks.LADDER.defaultBlockState()
				.setValue(net.minecraft.world.level.block.LadderBlock.FACING, hacia.getOpposite());
		for (int y = py + 1; y <= py + ALTURA_PASILLO; y++) {
			mundo.setBlock(new BlockPos(x, y, z), escalon, Block.UPDATE_CLIENTS);
		}
	}

	/** Una caja hueca: piso, paredes y techo de pizarra, con los bordes de baldosas; adentro, aire. */
	private static void armarCaja(ServerLevel mundo, Caja c, RandomSource azar) {
		for (int x = c.x0; x <= c.x1; x++) {
			for (int y = c.y0; y <= c.y1; y++) {
				for (int z = c.z0; z <= c.z1; z++) {
					BlockState bloque;
					if (c.adentro(x, y, z)) {
						bloque = Blocks.AIR.defaultBlockState();
					} else {
						int bordes = (x == c.x0 || x == c.x1 ? 1 : 0) + (y == c.y0 || y == c.y1 ? 1 : 0) + (z == c.z0 || z == c.z1 ? 1 : 0);
						if (bordes >= 2) bloque = Blocks.DEEPSLATE_TILES.defaultBlockState();
						else if (y < c.y0 + PISO) bloque = piso(azar);
						else bloque = pared(azar);
					}
					mundo.setBlock(new BlockPos(x, y, z), bloque, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/**
	 * Un tubo redondo entre dos puntos. Por dentro queda aire (así también abre la puerta en las paredes de las cajas);
	 * la pared del tubo no se pone adentro de las cajas.
	 */
	private static void armarTubo(ServerLevel mundo, double ax, double ay, double az, double bx, double by, double bz,
								  List<Caja> cajas, RandomSource azar) {
		int x0 = Mth.floor(Math.min(ax, bx) - TUBO_AFUERA), x1 = Mth.floor(Math.max(ax, bx) + TUBO_AFUERA);
		int y0 = Mth.floor(Math.min(ay, by) - TUBO_AFUERA), y1 = Mth.floor(Math.max(ay, by) + TUBO_AFUERA);
		int z0 = Mth.floor(Math.min(az, bz) - TUBO_AFUERA), z1 = Mth.floor(Math.max(az, bz) + TUBO_AFUERA);
		double dx = bx - ax, dy = by - ay, dz = bz - az, largo2 = dx * dx + dy * dy + dz * dz;
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					double qx = x + 0.5 - ax, qy = y + 0.5 - ay, qz = z + 0.5 - az;
					double t = Mth.clamp((qx * dx + qy * dy + qz * dz) / largo2, 0, 1);
					double ex = qx - dx * t, ey = qy - dy * t, ez = qz - dz * t;
					double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
					if (d >= TUBO_AFUERA) continue;
					boolean enCaja = false;
					for (Caja c : cajas) enCaja |= c.contiene(x, y, z);
					BlockPos pos = new BlockPos(x, y, z);
					if (d < TUBO_ADENTRO) {
						// Adentro del tubo: aire (en la pared de una caja, eso hace la puerta; el hueco empieza un bloque
						// arriba del piso, así que nunca rompe el piso).
						mundo.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					} else if (!enCaja) {
						mundo.setBlock(pos, tubo(azar), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	private static BlockState pared(RandomSource azar) {
		int r = azar.nextInt(100);
		if (r < 35) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		if (r < 50) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
		if (r < 70) return Blocks.DEEPSLATE_TILES.defaultBlockState();
		if (r < 80) return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
		if (r < 90) return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
	}

	private static BlockState piso(RandomSource azar) {
		int r = azar.nextInt(100);
		if (r < 60) return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		if (r < 85) return Blocks.DEEPSLATE_TILES.defaultBlockState();
		return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
	}

	private static BlockState tubo(RandomSource azar) {
		int r = azar.nextInt(100);
		if (r < 45) return Blocks.DEEPSLATE_TILES.defaultBlockState();
		if (r < 65) return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
		if (r < 85) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	}
}
