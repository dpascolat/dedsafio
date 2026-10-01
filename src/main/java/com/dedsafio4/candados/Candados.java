package com.dedsafio4.candados;

import com.dedsafio4.items.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Candados: poniéndole un candado a un cofre queda cerrado con un código.
 * El que lo cerró lo abre siempre; a los demás les pide el código (una sola vez) y,
 * mientras no lo sepan, tampoco lo pueden romper.
 */
public final class Candados {
	private Candados() {}

	public static final int LARGO_MAXIMO_CODIGO = 16;

	public static CandadosData data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(CandadosData.FACTORY, "dedsafio4_candados");
	}

	/** Solo se pueden cerrar cofres, cofres trampa, barriles y shulkers. */
	public static boolean sePuedeCerrar(BlockState estado) {
		return estado.getBlock() instanceof ChestBlock || estado.getBlock() instanceof BarrelBlock
				|| estado.getBlock() instanceof ShulkerBoxBlock;
	}

	/** En un cofre doble las dos mitades comparten el candado: se guarda en una sola. */
	public static BlockPos principal(Level level, BlockPos pos) {
		BlockState estado = level.getBlockState(pos);
		if (estado.getBlock() instanceof ChestBlock && estado.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			BlockPos vecino = pos.relative(ChestBlock.getConnectedDirection(estado));
			return pos.compareTo(vecino) <= 0 ? pos.immutable() : vecino.immutable();
		}
		return pos.immutable();
	}

	/** Los cofres con candado que conoce el cliente (los manda el servidor); sirve para dibujarlos. */
	public static volatile Set<BlockPos> cerradosCliente = Set.of();

	/** Si el cofre de esa posición tiene candado, del lado del servidor o del cliente. */
	public static boolean tieneCandado(Level level, BlockPos pos) {
		if (!sePuedeCerrar(level.getBlockState(pos))) return false;
		BlockPos clave = principal(level, pos);
		if (level instanceof ServerLevel mundo) return data(mundo).cerradura(clave) != null;
		return level.isClientSide && cerradosCliente.contains(clave);
	}

	public static void enviarLista(ServerPlayer jugador) {
		ServerPlayNetworking.send(jugador, new CandadosListaPayload(data(jugador.serverLevel()).posiciones()));
	}

	/** Cambió algún candado: todos los que están en esa dimensión reciben la lista nueva. */
	public static void avisarCambio(ServerLevel mundo) {
		for (ServerPlayer jugador : mundo.players()) enviarLista(jugador);
	}

	public static CandadosData.Cerradura cerradura(ServerLevel level, BlockPos pos) {
		return data(level).cerradura(principal(level, pos));
	}

	public static boolean puedeAbrir(ServerPlayer jugador, CandadosData.Cerradura cerradura) {
		UUID uuid = jugador.getUUID();
		return cerradura.duenio().equals(uuid) || cerradura.autorizados().contains(uuid);
	}

	/** Bloques que hay que volver a mandarle a un jugador en el próximo tick. */
	private static final List<Runnable> PENDIENTES = new ArrayList<>();

	private static void reenviarBloque(ServerPlayer jugador, ServerLevel mundo, BlockPos pos) {
		jugador.connection.send(new ClientboundBlockUpdatePacket(pos, mundo.getBlockState(pos)));
		BlockEntity conDatos = mundo.getBlockEntity(pos);
		if (conDatos == null) return;
		Packet<ClientGamePacketListener> paquete = conDatos.getUpdatePacket();
		if (paquete != null) jugador.connection.send(paquete);
	}

	public static void registrar() {
		// Las tolvas (y todo lo que usa la API de transporte de Fabric) ven un cofre con candado como vacío y lleno:
		// no le sacan ni le meten nada. La parte de Minecraft la corta HopperBlockEntityMixin.
		net.minecraft.world.level.block.Block[] cerrables = net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
				.filter(b -> b instanceof ChestBlock || b instanceof BarrelBlock || b instanceof ShulkerBoxBlock)
				.toArray(net.minecraft.world.level.block.Block[]::new);
		net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlocks((mundo, pos, estado, be, lado) ->
				mundo instanceof ServerLevel && tieneCandado(mundo, pos)
						? net.fabricmc.fabric.api.transfer.v1.storage.Storage.<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>empty() : null,
				cerrables);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enviarLista(handler.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((jugador, antes, despues) -> enviarLista(jugador));
		ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> enviarLista(nuevo));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 == 0) {
				for (ServerLevel mundo : server.getAllLevels()) limpiar(mundo);
			}
			if (PENDIENTES.isEmpty()) return;
			List<Runnable> copia = new ArrayList<>(PENDIENTES);
			PENDIENTES.clear();
			copia.forEach(Runnable::run);
		});

		// Click derecho sobre un cofre: poner el candado, o pedir el código si ya está cerrado.
		UseBlockCallback.EVENT.register((jugador, level, mano, hit) -> {
			if (!(jugador instanceof ServerPlayer servidor) || !(level instanceof ServerLevel mundo)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			if (!sePuedeCerrar(mundo.getBlockState(pos))) return InteractionResult.PASS;
			BlockPos clave = principal(mundo, pos);
			CandadosData.Cerradura cerradura = data(mundo).cerradura(clave);

			// Con la Llave de Candado, el dueño le saca el candado (y lo recupera).
			if (jugador.getItemInHand(mano).is(ModItems.LLAVE_CANDADO)) {
				desproteger(servidor, mundo, clave, cerradura);
				return InteractionResult.SUCCESS;
			}
			if (cerradura == null) {
				if (!jugador.getItemInHand(mano).is(ModItems.CANDADO)) return InteractionResult.PASS;
				ServerPlayNetworking.send(servidor, new CandadoPantallaPayload(clave, true, ""));
				return InteractionResult.SUCCESS;
			}
			if (puedeAbrir(servidor, cerradura)) return InteractionResult.PASS;
			ServerPlayNetworking.send(servidor, new CandadoPantallaPayload(clave, false, ""));
			mundo.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 1f);
			return InteractionResult.SUCCESS;
		});

		// Sin el código tampoco se puede romper el cofre.
		PlayerBlockBreakEvents.BEFORE.register((level, jugador, pos, estado, entidad) -> {
			if (!(level instanceof ServerLevel mundo) || !(jugador instanceof ServerPlayer servidor)) return true;
			if (!sePuedeCerrar(estado)) return true;
			CandadosData.Cerradura cerradura = data(mundo).cerradura(principal(mundo, pos));
			if (cerradura == null || puedeAbrir(servidor, cerradura)) return true;
			servidor.displayClientMessage(Component.literal("Este cofre está cerrado con un candado.")
					.withStyle(ChatFormatting.RED), true);
			mundo.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 1f);
			// El cliente ya "rompió" el bloque de su lado. Se lo devolvemos en el tick siguiente,
			// cuando ya aplicó su predicción, para que no le quede un agujero donde está el cofre.
			BlockPos donde = pos.immutable();
			PENDIENTES.add(() -> reenviarBloque(servidor, mundo, donde));
			return false;
		});

		// Si lo rompe alguien que sí puede, el candado se va con el cofre.
		PlayerBlockBreakEvents.AFTER.register((level, jugador, pos, estado, entidad) -> {
			if (!(level instanceof ServerLevel mundo) || !sePuedeCerrar(estado)) return;
			// Si era un cofre doble, la mitad que queda se queda con el candado.
			BlockPos donde = pos.immutable();
			CandadosData datos = data(mundo);
			CandadosData.Cerradura cerradura = datos.cerradura(donde);
			BlockPos otra = null;
			if (estado.getBlock() instanceof ChestBlock && estado.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
				otra = donde.relative(ChestBlock.getConnectedDirection(estado));
			}
			boolean habia = cerradura != null || (otra != null && datos.cerradura(otra) != null);
			datos.sacar(donde);
			if (cerradura != null && otra != null && sePuedeCerrar(mundo.getBlockState(otra))) datos.mover(otra, cerradura);
			if (habia) avisarCambio(mundo);
		});
	}

	/**
	 * Saca los candados que quedaron sin cofre (si el cofre se fue sin que un jugador lo rompa, por
	 * ejemplo con /setblock o un Wither); si no, un cofre nuevo en ese lugar aparecería cerrado.
	 */
	private static void limpiar(ServerLevel mundo) {
		CandadosData datos = data(mundo);
		boolean cambio = false;
		for (BlockPos pos : datos.posiciones()) {
			if (!mundo.isLoaded(pos)) continue;
			CandadosData.Cerradura cerradura = datos.cerradura(pos);
			if (!sePuedeCerrar(mundo.getBlockState(pos))) {
				datos.sacar(pos);
				cambio = true;
				continue;
			}
			BlockPos clave = principal(mundo, pos);
			if (!clave.equals(pos) && datos.cerradura(clave) == null) {
				datos.sacar(pos);
				datos.mover(clave, cerradura);
				cambio = true;
			}
		}
		if (cambio) avisarCambio(mundo);
	}

	/** La Llave de Candado: solo el dueño puede sacar el candado; se le devuelve. */
	private static void desproteger(ServerPlayer jugador, ServerLevel mundo, BlockPos clave, CandadosData.Cerradura cerradura) {
		if (cerradura == null) {
			jugador.displayClientMessage(Component.literal("Este cofre no tiene candado.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!cerradura.duenio().equals(jugador.getUUID())) {
			jugador.displayClientMessage(Component.literal("Este cofre no es de tu propiedad.").withStyle(ChatFormatting.RED), true);
			mundo.playSound(null, clave, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 1f);
			return;
		}
		data(mundo).sacar(clave);
		avisarCambio(mundo);
		ItemStack candado = new ItemStack(ModItems.CANDADO);
		if (!jugador.getInventory().add(candado)) jugador.drop(candado, false);
		mundo.playSound(null, clave, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 1.2f);
		jugador.displayClientMessage(Component.literal("Cofre desprotegido.").withStyle(ChatFormatting.GREEN), true);
	}

	/** Llega el código escrito por el jugador. */
	public static void codigoEscrito(ServerPlayer jugador, CandadoCodigoPayload payload) {
		ServerLevel mundo = jugador.serverLevel();
		BlockPos pos = payload.pos();
		if (!jugador.blockPosition().closerThan(pos, 8)) return;   // tiene que estar al lado del cofre
		BlockState estado = mundo.getBlockState(pos);
		if (!sePuedeCerrar(estado)) return;
		String codigo = payload.codigo().trim();
		CandadosData datos = data(mundo);
		CandadosData.Cerradura cerradura = datos.cerradura(pos);

		if (payload.cerrar()) {
			if (cerradura != null || codigo.isEmpty()) return;
			ItemStack candado = jugador.getMainHandItem().is(ModItems.CANDADO)
					? jugador.getMainHandItem() : jugador.getOffhandItem();
			if (!candado.is(ModItems.CANDADO)) return;
			candado.shrink(1);
			datos.poner(pos, codigo, jugador.getUUID());
			avisarCambio(mundo);
			mundo.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 1.4f);
			jugador.displayClientMessage(Component.literal("Cofre cerrado con el código ")
					.withStyle(ChatFormatting.GREEN)
					.append(Component.literal(codigo).withStyle(ChatFormatting.YELLOW)), false);
			return;
		}

		if (cerradura == null) return;
		if (!cerradura.codigo().equals(codigo)) {
			ServerPlayNetworking.send(jugador, new CandadoPantallaPayload(pos, false, "Código incorrecto"));
			mundo.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 0.8f);
			return;
		}
		datos.autorizar(pos, jugador.getUUID());
		mundo.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 1.4f);
		abrir(jugador, mundo, pos);
	}

	/** Abre el cofre como si le hubiera hecho click derecho. */
	private static void abrir(ServerPlayer jugador, ServerLevel mundo, BlockPos pos) {
		BlockState estado = mundo.getBlockState(pos);
		MenuProvider proveedor = estado.getMenuProvider(mundo, pos);
		if (proveedor != null) jugador.openMenu(proveedor);
	}

	/** Para el texto del ítem y de la pantalla. */
	public static Component nombreDe(Player jugador) {
		return jugador.getDisplayName();
	}
}
