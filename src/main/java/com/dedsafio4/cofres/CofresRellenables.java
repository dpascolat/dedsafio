package com.dedsafio4.cofres;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cofres que se rellenan solos, con loot individual: "/cofre_rellenar <tiempo>" mirando un cofre (o barril, o
 * cualquier cosa con inventario) guarda lo que tiene adentro como su loot. Desde ahí cada jugador que lo abre ve SU
 * propio cofre (lo que saca uno no le falta a los demás), y cada <tiempo> se le vuelve a llenar con el mismo loot.
 * El cofre de verdad queda vacío (así romperlo o un embudo no da nada). Funciona con los cofres dobles y con los del mod;
 * al abrir el suyo, la tapa del cofre de verdad se abre con su sonido (y se cierra cuando ya nadie lo mira). "/cofre_rellenar quitar" lo vuelve normal y le
 * devuelve el loot. Arriba del cofre, a los que todavía no lo abrieron desde que se rellenó, les aparece una flecha
 * blanca (la dibuja el cliente: FlechaCofreCliente).
 * El tiempo se escribe como 30s, 5m, 2h, 1d o combinado (1h30m); sólo un número son segundos.
 */
public final class CofresRellenables {
	private CofresRellenables() {}

	/** Las flechas que tiene que ver el jugador en su dimensión: de a dos posiciones por cofre (las dos mitades, o la misma). */
	public record FlechasPayload(List<Long> cofres) implements CustomPacketPayload {
		public static final Type<FlechasPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "flechas_cofres"));
		public static final StreamCodec<RegistryFriendlyByteBuf, FlechasPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), FlechasPayload::cofres, FlechasPayload::new);

		@Override
		public Type<FlechasPayload> type() {
			return TYPE;
		}
	}

	/** Un cofre que se rellena: dónde está (sus dos mitades si es doble), cada cuánto y lo que tiene cada jugador. */
	static final class Cofre {
		ResourceKey<Level> dimension;
		final List<BlockPos> lugares = new ArrayList<>();
		long intervalo, inicio;
		NonNullList<ItemStack> loot;
		/** Por jugador: en qué rellenado lo abrió por última vez y lo que le queda adentro. */
		final Map<UUID, Long> ciclos = new HashMap<>();
		final Map<UUID, NonNullList<ItemStack>> contenidos = new HashMap<>();

		long ciclo(long ahora) {
			return Math.max(0, (ahora - inicio) / intervalo);
		}

		boolean tieneFlecha(UUID jugador, long ahora) {
			Long visto = ciclos.get(jugador);
			return visto == null || visto < ciclo(ahora);
		}

		CompoundTag guardar(HolderLookup.Provider registros) {
			CompoundTag tag = new CompoundTag();
			tag.putString("dimension", dimension.location().toString());
			ListTag l = new ListTag();
			for (BlockPos p : lugares) l.add(LongTag.valueOf(p.asLong()));
			tag.put("lugares", l);
			tag.putLong("intervalo", intervalo);
			tag.putLong("inicio", inicio);
			CompoundTag lootTag = ContainerHelper.saveAllItems(new CompoundTag(), loot, true, registros);
			lootTag.putInt("tamanio", loot.size());
			tag.put("loot", lootTag);
			ListTag jugadores = new ListTag();
			for (Map.Entry<UUID, Long> e : ciclos.entrySet()) {
				CompoundTag j = new CompoundTag();
				j.putUUID("uuid", e.getKey());
				j.putLong("ciclo", e.getValue());
				NonNullList<ItemStack> c = contenidos.get(e.getKey());
				if (c != null) j.put("items", ContainerHelper.saveAllItems(new CompoundTag(), c, true, registros));
				jugadores.add(j);
			}
			tag.put("jugadores", jugadores);
			return tag;
		}

		static Cofre cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Cofre c = new Cofre();
			c.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension")));
			for (Tag t : tag.getList("lugares", Tag.TAG_LONG)) c.lugares.add(BlockPos.of(((LongTag) t).getAsLong()));
			c.intervalo = Math.max(1, tag.getLong("intervalo"));
			c.inicio = tag.getLong("inicio");
			CompoundTag loot = tag.getCompound("loot");
			c.loot = NonNullList.withSize(Math.max(1, tamanio(loot)), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(loot, c.loot, registros);
			for (Tag t : tag.getList("jugadores", Tag.TAG_COMPOUND)) {
				CompoundTag j = (CompoundTag) t;
				UUID uuid = j.getUUID("uuid");
				c.ciclos.put(uuid, j.getLong("ciclo"));
				if (j.contains("items")) {
					NonNullList<ItemStack> items = NonNullList.withSize(c.loot.size(), ItemStack.EMPTY);
					ContainerHelper.loadAllItems(j.getCompound("items"), items, registros);
					c.contenidos.put(uuid, items);
				}
			}
			return c;
		}

		/** El tamaño del inventario se guarda aparte (ContainerHelper sólo guarda los lugares con algo). */
		private static int tamanio(CompoundTag loot) {
			return loot.contains("tamanio") ? loot.getInt("tamanio") : 27;
		}
	}

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		final List<Cofre> cofres = new ArrayList<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("cofres", Tag.TAG_COMPOUND)) d.cofres.add(Cofre.cargar((CompoundTag) t, registros));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (Cofre c : cofres) l.add(c.guardar(registros));
			tag.put("cofres", l);
			return tag;
		}

		Cofre en(ResourceKey<Level> dimension, BlockPos pos) {
			for (Cofre c : cofres) if (c.dimension.equals(dimension) && c.lugares.contains(pos)) return c;
			return null;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_cofres_rellenar");
	}

	/** El reloj de los cofres y los ítems que desaparecen: el tiempo del mundo, en ticks. */
	public static long ahora(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	/** Lo último que se le mandó a cada jugador, para no mandarle lo mismo cada segundo. */
	private static final Map<UUID, List<Long>> ENVIADO = new HashMap<>();

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(FlechasPayload.TYPE, FlechasPayload.CODEC);
		UseBlockCallback.EVENT.register((jugador, mundo, mano, golpe) -> {
			if (!(jugador instanceof ServerPlayer sp) || !(mundo instanceof ServerLevel nivel)) return InteractionResult.PASS;
			Cofre c = datos(nivel.getServer()).en(nivel.dimension(), golpe.getBlockPos());
			if (c == null || (jugador.isSecondaryUseActive() && !jugador.getMainHandItem().isEmpty())) return InteractionResult.PASS;
			abrir(sp, nivel, c);
			return InteractionResult.SUCCESS;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			Datos d = datos(server);
			long ahora = ahora(server);
			// Si rompieron el cofre, se olvida.
			if (d.cofres.removeIf(c -> {
				ServerLevel nivel = server.getLevel(c.dimension);
				if (nivel == null) return true;
				BlockPos p = c.lugares.get(0);
				return nivel.isLoaded(p) && !(nivel.getBlockEntity(p) instanceof Container);
			})) d.setDirty();
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				List<Long> flechas = new ArrayList<>();
				for (Cofre c : d.cofres) {
					if (c.dimension.equals(jugador.level().dimension()) && c.tieneFlecha(jugador.getUUID(), ahora)) {
						flechas.add(c.lugares.get(0).asLong());
						flechas.add(c.lugares.get(c.lugares.size() - 1).asLong());
					}
				}
				if (!flechas.equals(ENVIADO.get(jugador.getUUID()))) {
					ENVIADO.put(jugador.getUUID(), flechas);
					ServerPlayNetworking.send(jugador, new FlechasPayload(flechas));
				}
			}
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				ENVIADO.remove(handler.player.getUUID()));
	}

	/** Cuántos jugadores tienen abierto cada cofre (para la tapa y los sonidos). */
	private static final Map<Cofre, Integer> MIRANDO = new java.util.IdentityHashMap<>();

	/**
	 * Abre o cierra la tapa del cofre de verdad, con su sonido. Sin usar el contador de Minecraft: ése sólo cuenta a los
	 * que tienen abierto el inventario del cofre de verdad, y acá cada uno ve el suyo (la tapa se cerraría sola).
	 */
	private static void tapa(ServerLevel nivel, Cofre c, boolean abierta) {
		BlockPos primero = c.lugares.get(0);
		net.minecraft.world.level.block.Block bloque = nivel.getBlockState(primero).getBlock();
		for (BlockPos p : c.lugares) {
			BlockState estado = nivel.getBlockState(p);
			if (estado.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
				nivel.setBlock(p, estado.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN, abierta), 3);
			} else {
				nivel.blockEvent(p, estado.getBlock(), 1, abierta ? 1 : 0);
			}
		}
		net.minecraft.sounds.SoundEvent sonido;
		if (bloque instanceof CofreBlock cofre) sonido = abierta ? cofre.sonidoAbrir() : cofre.sonidoCerrar();
		else if (bloque instanceof net.minecraft.world.level.block.BarrelBlock) sonido = abierta ? net.minecraft.sounds.SoundEvents.BARREL_OPEN : net.minecraft.sounds.SoundEvents.BARREL_CLOSE;
		else sonido = abierta ? net.minecraft.sounds.SoundEvents.CHEST_OPEN : net.minecraft.sounds.SoundEvents.CHEST_CLOSE;
		double x = 0, y = 0, z = 0;
		for (BlockPos p : c.lugares) { x += p.getX() + 0.5; y += p.getY() + 0.5; z += p.getZ() + 0.5; }
		int n = c.lugares.size();
		nivel.playSound(null, x / n, y / n, z / n, sonido, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, nivel.random.nextFloat() * 0.1f + 0.9f);
	}

	/** Le abre a este jugador su propio cofre (rellenado si ya pasó el tiempo). */
	private static void abrir(ServerPlayer jugador, ServerLevel nivel, Cofre c) {
		Datos d = datos(nivel.getServer());
		long ciclo = c.ciclo(ahora(nivel.getServer()));
		UUID uuid = jugador.getUUID();
		Long visto = c.ciclos.get(uuid);
		NonNullList<ItemStack> items = c.contenidos.get(uuid);
		if (visto == null || visto < ciclo || items == null) {
			items = copia(c.loot);
			c.contenidos.put(uuid, items);
		}
		c.ciclos.put(uuid, ciclo);
		d.setDirty();
		ENVIADO.remove(uuid);   // que la flecha se vaya enseguida
		int filas = Math.max(1, Math.min(6, (c.loot.size() + 8) / 9));
		NonNullList<ItemStack> propios = items;
		boolean[] listo = {false};
		SimpleContainer caja = new SimpleContainer(filas * 9) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (!listo[0]) return;   // mientras se llena no se copia (borraría lo que todavía no se puso)
				for (int i = 0; i < propios.size(); i++) propios.set(i, getItem(i).copy());
				d.setDirty();
			}

			@Override
			public void startOpen(net.minecraft.world.entity.player.Player quien) {
				int antes = MIRANDO.getOrDefault(c, 0);
				MIRANDO.put(c, antes + 1);
				if (antes == 0) tapa(nivel, c, true);
			}

			@Override
			public void stopOpen(net.minecraft.world.entity.player.Player quien) {
				int quedan = MIRANDO.getOrDefault(c, 1) - 1;
				if (quedan > 0) MIRANDO.put(c, quedan);
				else {
					MIRANDO.remove(c);
					tapa(nivel, c, false);
				}
			}
		};
		for (int i = 0; i < propios.size() && i < caja.getContainerSize(); i++) caja.setItem(i, propios.get(i).copy());
		listo[0] = true;
		BlockEntity be = nivel.getBlockEntity(c.lugares.get(0));
		Component titulo = be instanceof Nameable n ? n.getDisplayName() : Component.translatable("container.chest");
		MenuType<?> tipo = switch (filas) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 3 -> MenuType.GENERIC_9x3;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			default -> MenuType.GENERIC_9x6;
		};
		jugador.openMenu(new SimpleMenuProvider((id, inventario, p) -> new ChestMenu(tipo, id, inventario, caja, filas), titulo));
	}

	/** Una copia del loot, sin el reloj de los ítems que desaparecen (cada uno empieza a contar al agarrarlo). */
	private static NonNullList<ItemStack> copia(NonNullList<ItemStack> loot) {
		NonNullList<ItemStack> c = NonNullList.withSize(loot.size(), ItemStack.EMPTY);
		for (int i = 0; i < loot.size(); i++) c.set(i, ItemsQueDesaparecen.sinReloj(loot.get(i).copy()));
		return c;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cofre_rellenar").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar").executes(CofresRellenables::quitar))
				.then(Commands.argument("tiempo", StringArgumentType.word()).executes(c -> poner(c, StringArgumentType.getString(c, "tiempo")))));
	}

	/** El bloque que el jugador tiene adelante (hasta 6 bloques). */
	private static BlockPos mirando(ServerPlayer jugador) {
		Vec3 ojos = jugador.getEyePosition();
		Vec3 fin = ojos.add(jugador.getViewVector(1f).scale(6));
		BlockHitResult golpe = jugador.level().clip(new ClipContext(ojos, fin, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, jugador));
		return golpe.getType() == HitResult.Type.BLOCK ? golpe.getBlockPos() : null;
	}

	/** El inventario de ese bloque (el doble entero si es un cofre doble) y sus lugares. */
	private static Container inventario(ServerLevel nivel, BlockPos pos, List<BlockPos> lugares) {
		BlockState estado = nivel.getBlockState(pos);
		if (estado.getBlock() instanceof CofreBlock) {
			BlockPos principal = CofreBlock.principal(estado, pos);
			lugares.add(principal);
			BlockPos otra = principal.relative(CofreBlock.haciaLaOtra(nivel.getBlockState(principal)));
			if (nivel.getBlockState(otra).getBlock() == estado.getBlock()) lugares.add(otra);
			return nivel.getBlockEntity(principal) instanceof Container c ? c : null;
		}
		lugares.add(pos);
		if (estado.getBlock() instanceof ChestBlock cofre) {
			Container doble = ChestBlock.getContainer(cofre, estado, nivel, pos, true);
			if (doble != null && doble.getContainerSize() > 27 && estado.hasProperty(ChestBlock.TYPE)) {
				lugares.add(pos.relative(ChestBlock.getConnectedDirection(estado)));
			}
			if (doble != null) return doble;
		}
		return nivel.getBlockEntity(pos) instanceof Container c ? c : null;
	}

	private static int poner(CommandContext<CommandSourceStack> ctx, String texto) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer jugador = ctx.getSource().getPlayerOrException();
		long ticks = ItemsQueDesaparecen.leerTiempo(texto);
		if (ticks <= 0) {
			ctx.getSource().sendFailure(Component.literal("Tiempo no válido. Ejemplos: 30s, 5m, 2h, 1d o 1h30m."));
			return 0;
		}
		BlockPos pos = mirando(jugador);
		ServerLevel nivel = jugador.serverLevel();
		List<BlockPos> lugares = new ArrayList<>();
		Container caja = pos == null ? null : inventario(nivel, pos, lugares);
		if (caja == null) {
			ctx.getSource().sendFailure(Component.literal("Mira un cofre (o un barril) para elegirlo"
					+ (pos == null ? "." : " (estás mirando " + nivel.getBlockState(pos).getBlock().getName().getString() + " en " + pos.toShortString() + ").")));
			return 0;
		}
		Datos d = datos(ctx.getSource().getServer());
		Cofre viejo = d.en(nivel.dimension(), pos);
		Cofre c = viejo != null ? viejo : new Cofre();
		if (viejo == null) {
			c.dimension = nivel.dimension();
			c.lugares.addAll(lugares);
			// El loot es lo que tiene ahora; el cofre de verdad queda vacío.
			c.loot = NonNullList.withSize(caja.getContainerSize(), ItemStack.EMPTY);
			for (int i = 0; i < caja.getContainerSize(); i++) c.loot.set(i, ItemsQueDesaparecen.sinReloj(caja.getItem(i).copy()));
			caja.clearContent();
			caja.setChanged();
			d.cofres.add(c);
		}
		c.intervalo = ticks;
		c.inicio = ahora(ctx.getSource().getServer());
		c.ciclos.clear();
		c.contenidos.clear();
		d.setDirty();
		ENVIADO.clear();
		ctx.getSource().sendSuccess(() -> Component.literal("Cofre rellenable: cada jugador tiene el suyo y se rellena cada "
				+ ItemsQueDesaparecen.escribirTiempo(ticks) + ". (/cofre_rellenar quitar lo vuelve normal)").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int quitar(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer jugador = ctx.getSource().getPlayerOrException();
		BlockPos pos = mirando(jugador);
		Datos d = datos(ctx.getSource().getServer());
		Cofre c = pos == null ? null : d.en(jugador.level().dimension(), pos);
		if (c == null) {
			ctx.getSource().sendFailure(Component.literal("Ese cofre no se rellena."));
			return 0;
		}
		// Le devuelve el loot al cofre de verdad.
		Container caja = inventario(jugador.serverLevel(), c.lugares.get(0), new ArrayList<>());
		if (caja != null) {
			for (int i = 0; i < c.loot.size() && i < caja.getContainerSize(); i++) caja.setItem(i, c.loot.get(i).copy());
		}
		d.cofres.remove(c);
		d.setDirty();
		ENVIADO.clear();
		ctx.getSource().sendSuccess(() -> Component.literal("El cofre volvió a ser normal (con su loot adentro).").withStyle(ChatFormatting.GRAY), true);
		return 1;
	}
}
