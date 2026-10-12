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
 * Puesto de Intercambio: un bloque que no se puede romper (ni con explosiones ni con pistones). Con clic derecho abre
 * la pantalla de intercambios de los aldeanos, con estos tratos (sin límite de usos):
 *   1 Dedita            → 1 Dedita de Casino
 *   2 Dilitio           → 1 Dedita
 *   1 Dedita            → 1 Manuscrito de Hermandad (el de 3 participantes)
 */
public final class ModIntercambio {
	private ModIntercambio() {}

	public static final Block PUESTO = Registry.register(BuiltInRegistries.BLOCK, id("puesto_intercambio"),
			new PuestoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.EMERALD)
					.strength(-1f, 3600000f).noLootTable().pushReaction(PushReaction.BLOCK)
					.sound(SoundType.WOOD).lightLevel(estado -> 7)));
	public static final Item PUESTO_ITEM = Registry.register(BuiltInRegistries.ITEM, id("puesto_intercambio"),
			new BlockItem(PUESTO, new Item.Properties().rarity(Rarity.EPIC)));

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {}

	/** Los tratos (se arman cada vez que alguien abre el puesto). */
	private static MerchantOffers tratos() {
		MerchantOffers o = new MerchantOffers();
		o.add(trato(new ItemCost(ModItems.DEDITA, 1), new ItemStack(ModItems.DEDITA_CASINO)));
		o.add(trato(new ItemCost(ModItems.DILITIO, 2), new ItemStack(ModItems.DEDITA)));
		o.add(trato(new ItemCost(ModItems.DEDITA, 1), new ItemStack(ModItems.MANUSCRITO_HERMANDAD)));
		return o;
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

		@Override
		protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos pos, Player jugador, BlockHitResult golpe) {
			if (!mundo.isClientSide) new Puesto(jugador).openTradingScreen(jugador, Component.literal("Puesto de Intercambio"), 0);
			return InteractionResult.sidedSuccess(mundo.isClientSide);
		}
	}

	/** El "comerciante" del puesto: uno por cada vez que alguien lo abre. */
	static final class Puesto implements Merchant {
		private final MerchantOffers ofertas = tratos();
		private Player jugador;

		Puesto(Player jugador) {
			this.jugador = jugador;
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
