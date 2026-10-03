package com.dedsafio4.boveda;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad del bloque principal de la Bóveda.
 * En el cliente lleva la cuenta de la animación (4 segundos para abrirse; al cerrarse hace lo mismo al
 * revés, y si se interrumpe a la mitad vuelve desde donde estaba).
 * En el servidor: los sonidos de cada tramo, abrir la pantalla cuando la puerta ya se separó de la
 * pared, y cerrarla sola cuando nadie la está usando.
 */
public class BovedaBlockEntity extends BlockEntity {
	/** Lo que dura la animación entera (4 segundos). */
	public static final float DURACION = 80f;
	/** A los cuántos ticks de empezar a abrirse aparece la pantalla (cuando la puerta empieza a girar). */
	private static final int DEMORA_PANTALLA = 34;

	// --- Cliente: la animación ---

	/** Cuánto estaba abierta (0..1) cuando cambió de estado, y en qué tick del mundo; -1 = sin animar. */
	private float desde;
	private long inicio = -1;

	// --- Servidor ---

	/** Jugadores que pidieron abrir → ticks que faltan para mostrarles la pantalla. */
	private final Map<UUID, Integer> pendientes = new HashMap<>();
	/** Ticks desde la última vez que se abrió o cerró. */
	private int ticks = 1000;

	public BovedaBlockEntity(BlockPos pos, BlockState estado) {
		super(ModBoveda.ENTIDAD, pos, estado);
	}

	private boolean abierta() {
		return getBlockState().getValue(BovedaBlock.ABIERTA);
	}

	@Override
	public void setBlockState(BlockState nuevo) {
		boolean antes = abierta();
		float actual = progreso(0);
		super.setBlockState(nuevo);
		if (level != null && level.isClientSide && antes != nuevo.getValue(BovedaBlock.ABIERTA)) {
			desde = actual;
			inicio = level.getGameTime();
		}
	}

	/** Cuánto avanzó la animación de abrir, de 0 (cerrada) a 1 (abierta del todo). */
	public float progreso(float parcial) {
		boolean abierta = abierta();
		if (inicio < 0 || level == null) return abierta ? 1 : 0;
		float avance = (level.getGameTime() - inicio + parcial) / DURACION;
		return abierta ? Math.min(1f, desde + avance) : Math.max(0f, desde - avance);
	}

	/** Click derecho de un miembro de una Hermandad: se abre la puerta y después la pantalla. */
	void pedirApertura(ServerPlayer jugador) {
		if (level == null) return;
		if (!abierta()) {
			BovedaBlock.setAbierta(level, worldPosition, getBlockState(), true);
			ticks = 0;
		}
		pendientes.put(jugador.getUUID(), Math.max(1, DEMORA_PANTALLA - ticks));
	}

	public static void tickServidor(Level level, BlockPos pos, BlockState estado, BovedaBlockEntity boveda) {
		boveda.ticks++;
		boveda.sonidos(level, pos.above());

		Iterator<Map.Entry<UUID, Integer>> it = boveda.pendientes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> pendiente = it.next();
			int quedan = pendiente.getValue() - 1;
			if (quedan > 0) {
				pendiente.setValue(quedan);
				continue;
			}
			it.remove();
			if (level.getPlayerByUUID(pendiente.getKey()) instanceof ServerPlayer jugador && jugador.isAlive()) {
				ModBoveda.abrirMenu(jugador, pos);
			}
		}

		// Si está abierta y nadie la usa, se cierra.
		if (estado.getValue(BovedaBlock.ABIERTA) && boveda.pendientes.isEmpty() && boveda.ticks > DEMORA_PANTALLA + 10
				&& level.getGameTime() % 10 == 0 && !enUso((ServerLevel) level, pos)) {
			BovedaBlock.setAbierta(level, pos, estado, false);
			boveda.ticks = 0;
		}
	}

	private static boolean enUso(ServerLevel level, BlockPos pos) {
		for (ServerPlayer jugador : level.players()) {
			if (jugador.containerMenu instanceof BovedaMenu menu && menu.pos().equals(pos)) return true;
		}
		return false;
	}

	/** La rueda, los cerrojos y el chirrido de las bisagras (al cerrar, al revés). */
	private void sonidos(Level level, BlockPos centro) {
		boolean abriendo = abierta();
		int rueda = abriendo ? 1 : 62, cerrojos = abriendo ? 14 : 48, bisagras = abriendo ? 34 : 1;
		if (ticks == rueda) sonar(level, centro, SoundEvents.GRINDSTONE_USE, 0.8f, 0.6f);
		if (ticks == cerrojos) sonar(level, centro, abriendo ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND, 0.7f, 0.8f);
		if (ticks == bisagras) sonar(level, centro, abriendo ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 1f, 0.5f);
		if (!abriendo && ticks == 76) sonar(level, centro, SoundEvents.ANVIL_LAND, 0.3f, 1.6f);
	}

	private static void sonar(Level level, BlockPos pos, SoundEvent sonido, float volumen, float tono) {
		level.playSound(null, pos, sonido, SoundSource.BLOCKS, volumen, tono);
	}
}
