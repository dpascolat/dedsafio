package com.dedsafio4.cambios;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Los cambios de dificultad del Nether, el agua, la comida y las cosas que no se pueden tocar. */
public final class CambiosDificultad {
	private CambiosDificultad() {}

	private static final int MULTIPLICADOR = 10;
	/** Lo que hace tocar un botón, una puerta o una placa: 4 mil millones de millones de millones de millones. */
	private static final float DANIO_INTERACTUAR = 4.0e27f;
	/** Mientras dura el Fuego Infinito, cuántos ticks de fuego le quedan siempre. */
	private static final int FUEGO = 300;
	/** Con cuánto aire menos por tick pierde además del que ya perdió (5 veces más rápido en total). */
	private static final int AIRE_EXTRA = 4;
	/** Cada 30 segundos, cada pila de carne tiene 10% de pudrirse (unos 5 minutos en promedio). */
	private static final int CADA_PUDRIR = 30 * 20;
	private static final float PROBABILIDAD_PUDRIR = 0.1f;

	private static final Set<Item> CARNES = Set.of(Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
			Items.MUTTON, Items.COOKED_MUTTON, Items.CHICKEN, Items.COOKED_CHICKEN, Items.RABBIT, Items.COOKED_RABBIT,
			Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH);

	/** El aire que tenía cada jugador en el tick anterior. */
	private static final Map<UUID, Integer> AIRE = new HashMap<>();
	/** Los que están parados en una Placa de Presión. */
	private static final Set<UUID> EN_PLACA = new java.util.HashSet<>();

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			boolean sinArmadura = Cambios.nivel(server, Cambios.FUEGO_ARMADURA) >= 1, fuegoNether = Cambios.nivel(server, Cambios.FUEGO_NETHER) >= 1;
			boolean placas = Cambios.nivel(server, Cambios.INTERACTUAR) >= 1, respiracion = Cambios.nivel(server, Cambios.RESPIRACION) >= 1;
			boolean pudrir = Cambios.nivel(server, Cambios.CARNE) >= 1 && server.getTickCount() % CADA_PUDRIR == 0;
			if (!respiracion) AIRE.clear();
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (jugador.isCreative() || jugador.isSpectator() || !jugador.isAlive()) continue;
				if (jugador.level().dimension() == Level.NETHER
						&& ((sinArmadura && !armaduraCompleta(jugador)) || (fuegoNether && jugador.getRemainingFireTicks() > 0))) {
					jugador.setRemainingFireTicks(FUEGO);
				}
				// Solo al pisarla: si un tótem lo salva, no lo vuelve a matar por seguir parado arriba.
				boolean enPlaca = placas && jugador.level().getBlockState(jugador.blockPosition()).getBlock() instanceof BasePressurePlateBlock;
				if (!enPlaca) EN_PLACA.remove(jugador.getUUID());
				else if (EN_PLACA.add(jugador.getUUID())) interactuo(jugador);
				if (respiracion) perderAire(jugador);
				if (pudrir) pudrir(jugador, PROBABILIDAD_PUDRIR);
			}
		});

		UseBlockCallback.EVENT.register((jugador, level, mano, golpe) -> {
			if (!(jugador instanceof ServerPlayer servidor) || mano != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (jugador.isCreative() || jugador.isSpectator() || Cambios.nivel(servidor.server, Cambios.INTERACTUAR) < 1) return InteractionResult.PASS;
			if (jugador.isSecondaryUseActive() && !jugador.getMainHandItem().isEmpty()) return InteractionResult.PASS;
			Block bloque = level.getBlockState(golpe.getBlockPos()).getBlock();
			if (bloque instanceof DoorBlock || bloque instanceof ButtonBlock) interactuo(servidor);
			return InteractionResult.PASS;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (entidad instanceof Spider && entidad.level() instanceof ServerLevel mundo) telarana(mundo, entidad.blockPosition());
		});
	}

	private static boolean armaduraCompleta(Player jugador) {
		for (EquipmentSlot lugar : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (!(jugador.getItemBySlot(lugar).getItem() instanceof ArmorItem)) return false;
		}
		return true;
	}

	private static void interactuo(ServerPlayer jugador) {
		jugador.invulnerableTime = 0;
		jugador.hurt(Cambios.danioInteractuar(jugador.level()), DANIO_INTERACTUAR);
	}

	/** Si en este tick perdió aire, le saca más. Minecraft sigue desde 0 con el ahogo, así que no se baja de ahí. */
	private static void perderAire(ServerPlayer jugador) {
		int aire = jugador.getAirSupply();
		Integer antes = AIRE.put(jugador.getUUID(), aire);
		if (antes == null || aire >= antes || aire <= 0) return;
		aire = Math.max(0, aire - AIRE_EXTRA);
		jugador.setAirSupply(aire);
		AIRE.put(jugador.getUUID(), aire);
	}

	/** Cada pila de carne del inventario, con esa probabilidad, se vuelve Carne Podrida. */
	public static void pudrir(ServerPlayer jugador, float probabilidad) {
		var inventario = jugador.getInventory();
		for (int i = 0; i < inventario.getContainerSize(); i++) {
			ItemStack pila = inventario.getItem(i);
			if (CARNES.contains(pila.getItem()) && jugador.getRandom().nextFloat() < probabilidad) {
				inventario.setItem(i, new ItemStack(Items.ROTTEN_FLESH, pila.getCount()));
			}
		}
	}

	/** Fuego y lava x10, ahogarse x10 (a los jugadores). */
	public static float danioJugador(LivingEntity quien, DamageSource fuente, float danio) {
		if (!(quien instanceof ServerPlayer jugador)) return danio;
		if (fuente.is(DamageTypeTags.IS_FIRE) && Cambios.nivel(jugador.server, Cambios.FUEGO_DANIO) >= 1) return danio * MULTIPLICADOR;
		if (fuente.is(DamageTypes.DROWN) && Cambios.nivel(jugador.server, Cambios.RESPIRACION) >= 1) return danio * MULTIPLICADOR;
		return danio;
	}

	/** Una araña le pegó a alguien: le deja una telaraña a los pies. */
	public static void aranaPego(Spider arana, LivingEntity objetivo) {
		if (arana.level() instanceof ServerLevel mundo) telarana(mundo, objetivo.blockPosition());
	}

	private static void telarana(ServerLevel mundo, BlockPos pos) {
		MinecraftServer server = mundo.getServer();
		if (Cambios.nivel(server, Cambios.ARANAS_TELAS) >= 1 && mundo.getBlockState(pos).isAir()) mundo.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
	}
}
