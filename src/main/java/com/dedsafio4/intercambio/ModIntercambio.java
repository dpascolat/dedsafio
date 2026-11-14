package com.dedsafio4.intercambio;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Puesto de Intercambio: un bloque invisible como la Barrera (sólido, no se ve; en creativo, con el Puesto en la mano,
 * se ve su marca) que no se puede romper (ni con explosiones ni con pistones). Con clic derecho abre
 * la pantalla de intercambios de los aldeanos, con estos tratos (sin límite de usos):
 *   1 Dedita            → 1 Dedita de Casino
 *   2 Dilitio           → 1 Dedita
 *   1 Dedita            → 1 Manuscrito de Hermandad (el de 3 participantes)
 * Los OP los cambian con /puesto (valen para todos los puestos y quedan guardados en el mundo):
 *   /puesto lista                 los tratos, numerados
 *   /puesto agregar               agrega uno: lo que se paga es lo de la mano principal (con su cantidad) y lo que
 *                                 se recibe, lo de la mano secundaria (con su cantidad, encantamientos y nombre)
 *   /puesto quitar <número>       saca ese trato
 *   /puesto original              vuelve a los tres de siempre
 */
public final class ModIntercambio {
	private ModIntercambio() {}

	public static final Block PUESTO = Registry.register(BuiltInRegistries.BLOCK, id("puesto_intercambio"),
			new PuestoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
					.strength(-1f, 3600000f).noLootTable().pushReaction(PushReaction.BLOCK)
					.noOcclusion().isViewBlocking((estado, mundo, pos) -> false).isSuffocating((estado, mundo, pos) -> false)
					.sound(SoundType.STONE)));
	public static final Item PUESTO_ITEM = Registry.register(BuiltInRegistries.ITEM, id("puesto_intercambio"),
			new BlockItem(PUESTO, new Item.Properties().rarity(Rarity.EPIC)));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {}

	/** Los tratos cambiados con /puesto (si nunca se cambiaron, "cambiados" es false y se usan los de siempre). */
	static final class Datos extends net.minecraft.world.level.saveddata.SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean cambiados;
		/** Cada trato: lo que se paga y lo que se recibe. */
		final java.util.List<ItemStack[]> tratos = new java.util.ArrayList<>();

		private static Datos leer(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.cambiados = tag.getBoolean("cambiados");
			for (net.minecraft.nbt.Tag t : tag.getList("tratos", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
				net.minecraft.nbt.CompoundTag c = (net.minecraft.nbt.CompoundTag) t;
				ItemStack paga = ItemStack.parseOptional(registros, c.getCompound("paga"));
				ItemStack recibe = ItemStack.parseOptional(registros, c.getCompound("recibe"));
				if (!paga.isEmpty() && !recibe.isEmpty()) d.tratos.add(new ItemStack[]{paga, recibe});
			}
			return d;
		}

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registros) {
			tag.putBoolean("cambiados", cambiados);
			net.minecraft.nbt.ListTag lista = new net.minecraft.nbt.ListTag();
			for (ItemStack[] t : tratos) {
				net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
				c.put("paga", t[0].save(registros));
				c.put("recibe", t[1].save(registros));
				lista.add(c);
			}
			tag.put("tratos", lista);
			return tag;
		}
	}

	private static Datos datos(net.minecraft.server.MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_puesto_intercambio");
	}

	/** Los tratos de siempre (lo que se paga, lo que se recibe). */
	private static java.util.List<ItemStack[]> originales() {
		return java.util.List.of(
				new ItemStack[]{new ItemStack(ModItems.DEDITA, 1), new ItemStack(ModItems.DEDITA_CASINO)},
				new ItemStack[]{new ItemStack(ModItems.DILITIO, 2), new ItemStack(ModItems.DEDITA)},
				new ItemStack[]{new ItemStack(ModItems.DEDITA, 1), new ItemStack(ModItems.MANUSCRITO_HERMANDAD)});
	}

	private static java.util.List<ItemStack[]> actuales(net.minecraft.server.MinecraftServer server) {
		Datos d = datos(server);
		return d.cambiados ? d.tratos : originales();
	}

	/** Si todavía son los de siempre, los copia para poder cambiarlos. */
	private static Datos paraCambiar(net.minecraft.server.MinecraftServer server) {
		Datos d = datos(server);
		if (!d.cambiados) {
			d.tratos.clear();
			for (ItemStack[] t : originales()) d.tratos.add(new ItemStack[]{t[0].copy(), t[1].copy()});
			d.cambiados = true;
		}
		d.setDirty();
		return d;
	}

	/** Los tratos (se arman cada vez que alguien abre el puesto). Lo que se paga se compara solo por el ítem y la cantidad. */
	private static MerchantOffers tratos(net.minecraft.server.MinecraftServer server) {
		MerchantOffers o = new MerchantOffers();
		for (ItemStack[] t : actuales(server)) o.add(trato(new ItemCost(t[0].getItem(), t[0].getCount()), t[1].copy()));
		return o;
	}

	private static Component nombre(ItemStack pila) {
		return Component.literal(pila.getCount() + " ").append(pila.getHoverName());
	}

	private static Component flecha() {
		return Component.literal("  →  ").withStyle(net.minecraft.ChatFormatting.GRAY);
	}

	public static void registrarComandos(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		var c = net.minecraft.commands.Commands.literal("puesto").requires(s -> s.hasPermission(2));
		c.then(net.minecraft.commands.Commands.literal("lista").executes(x -> {
			var server = x.getSource().getServer();
			var lista = actuales(server);
			x.getSource().sendSuccess(() -> Component.literal("Tratos del Puesto de Intercambio" + (datos(server).cambiados ? ":" : " (los de siempre):"))
					.withStyle(net.minecraft.ChatFormatting.GOLD), false);
			for (int i = 0; i < lista.size(); i++) {
				ItemStack[] t = lista.get(i);
				int n = i + 1;
				x.getSource().sendSuccess(() -> Component.literal(n + ". ").withStyle(net.minecraft.ChatFormatting.YELLOW)
						.append(nombre(t[0])).append(flecha()).append(nombre(t[1])), false);
			}
			return lista.size();
		}));
		c.then(net.minecraft.commands.Commands.literal("agregar").executes(x -> {
			Player jugador = x.getSource().getPlayerOrException();
			ItemStack paga = jugador.getMainHandItem(), recibe = jugador.getOffhandItem();
			if (paga.isEmpty() || recibe.isEmpty()) {
				x.getSource().sendFailure(Component.literal("Pon en la mano principal lo que se paga y en la mano secundaria lo que se recibe (con sus cantidades)."));
				return 0;
			}
			Datos d = paraCambiar(x.getSource().getServer());
			d.tratos.add(new ItemStack[]{paga.copy(), recibe.copy()});
			int n = d.tratos.size();
			x.getSource().sendSuccess(() -> Component.literal("Listo, trato " + n + ": ").withStyle(net.minecraft.ChatFormatting.GREEN)
					.append(nombre(paga)).append(flecha()).append(nombre(recibe)), true);
			return n;
		}));
		c.then(net.minecraft.commands.Commands.literal("quitar").then(net.minecraft.commands.Commands.argument("numero",
				com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(x -> {
			int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(x, "numero");
			if (n > actuales(x.getSource().getServer()).size()) {
				x.getSource().sendFailure(Component.literal("No hay trato " + n + ". Mira los números con /puesto lista."));
				return 0;
			}
			Datos d = paraCambiar(x.getSource().getServer());
			ItemStack[] t = d.tratos.remove(n - 1);
			x.getSource().sendSuccess(() -> Component.literal("Listo, se sacó el trato " + n + ": ").withStyle(net.minecraft.ChatFormatting.GREEN)
					.append(nombre(t[0])).append(flecha()).append(nombre(t[1])), true);
			return 1;
		})));
		c.then(net.minecraft.commands.Commands.literal("original").executes(x -> {
			Datos d = datos(x.getSource().getServer());
			d.cambiados = false;
			d.tratos.clear();
			d.setDirty();
			x.getSource().sendSuccess(() -> Component.literal("Listo: el Puesto de Intercambio vuelve a los tratos de siempre.")
					.withStyle(net.minecraft.ChatFormatting.GREEN), true);
			return 1;
		}));
		dispatcher.register(c);
	}

	private static MerchantOffer trato(ItemCost cuesta, ItemStack da) {
		return new MerchantOffer(cuesta, da, Integer.MAX_VALUE, 0, 0f);
	}

	static final class PuestoBlock extends Block {
		static final MapCodec<PuestoBlock> CODEC = simpleCodec(PuestoBlock::new);

		PuestoBlock(Properties propiedades) {
			super(propiedades);
		}

		@Override
		protected MapCodec<? extends Block> codec() {
			return CODEC;
		}

		/** Invisible como el bloque Barrera (pero sólido y con su borde al apuntarlo). */
		@Override
		protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState estado) {
			return net.minecraft.world.level.block.RenderShape.INVISIBLE;
		}

		@Override
		protected boolean propagatesSkylightDown(BlockState estado, net.minecraft.world.level.BlockGetter mundo, BlockPos pos) {
			return true;
		}

		@Override
		protected float getShadeBrightness(BlockState estado, net.minecraft.world.level.BlockGetter mundo, BlockPos pos) {
			return 1f;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos pos, Player jugador, BlockHitResult golpe) {
			if (!mundo.isClientSide) new Puesto(jugador, tratos(mundo.getServer())).openTradingScreen(jugador, Component.literal("Puesto de Intercambio"), 0);
			return InteractionResult.sidedSuccess(mundo.isClientSide);
		}
	}

	/** El "comerciante" del puesto: uno por cada vez que alguien lo abre. */
	static final class Puesto implements Merchant {
		private final MerchantOffers ofertas;
		private Player jugador;

		Puesto(Player jugador, MerchantOffers ofertas) {
			this.jugador = jugador;
			this.ofertas = ofertas;
		}

		@Override public void setTradingPlayer(Player p) { jugador = p; }
		@Override public Player getTradingPlayer() { return jugador; }
		@Override public MerchantOffers getOffers() { return ofertas; }
		@Override public void overrideOffers(MerchantOffers o) {}
		@Override public void notifyTrade(MerchantOffer o) { o.resetUses(); }
		@Override public void notifyTradeUpdated(ItemStack pila) {}
		@Override public int getVillagerXp() { return 0; }
		@Override public void overrideXp(int xp) {}
		@Override public boolean showProgressBar() { return false; }
		@Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
		@Override public boolean isClientSide() { return false; }
	}
}
