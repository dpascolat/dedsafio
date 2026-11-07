package com.dedsafio4.bote;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Pone el Bote donde mira el jugador (en el agua o en el piso), mirando para el mismo lado que él. */
public class BoteItem extends Item {
	public BoteItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		HitResult golpe = getPlayerPOVHitResult(level, jugador, ClipContext.Fluid.ANY);
		if (golpe.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(pila);
		BlockHitResult bloque = (BlockHitResult) golpe;
		BoteEntity bote = ModBote.BOTE.create(level);
		if (bote == null) return InteractionResultHolder.fail(pila);
		var lugar = bloque.getLocation();
		bote.moveTo(lugar.x, lugar.y, lugar.z, jugador.getYRot(), 0);
		if (!level.noCollision(bote, bote.getBoundingBox())) return InteractionResultHolder.fail(pila);
		if (!level.isClientSide) {
			level.addFreshEntity(bote);
			level.gameEvent(jugador, GameEvent.ENTITY_PLACE, lugar);
			pila.consume(1, jugador);
		}
		jugador.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(pila, level.isClientSide());
	}
}
