package com.dedsafio4.puertas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * La Puerta de Hueso como ítem: lleva su grosor y su alto (los da /puertita <grueso> <alto>; sin nada, la de siempre).
 * Al usarla sobre un bloque se pone parada encima, con el frente (la calavera) hacia el que la pone, ocupando ese bloque
 * y el de su derecha.
 */
public class PuertaHuesoItem extends Item {
	public PuertaHuesoItem(Properties propiedades) {
		super(propiedades);
	}

	public static float grueso(ItemStack pila) {
		CompoundTag tag = pila.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.contains("grueso") ? tag.getFloat("grueso") : PuertaHuesoEntity.GRUESO_NORMAL;
	}

	public static float alto(ItemStack pila) {
		CompoundTag tag = pila.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.contains("alto") ? tag.getFloat("alto") : PuertaHuesoEntity.ALTO_NORMAL;
	}

	public static ItemStack con(float grueso, float alto) {
		ItemStack pila = new ItemStack(PuertasHueso.ITEM);
		CompoundTag tag = new CompoundTag();
		tag.putFloat("grueso", grueso);
		tag.putFloat("alto", alto);
		pila.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return pila;
	}

	@Override
	public InteractionResult useOn(UseOnContext contexto) {
		Level level = contexto.getLevel();
		if (!(level instanceof ServerLevel mundo)) return InteractionResult.SUCCESS;
		BlockPos pos = contexto.getClickedPos();
		if (!level.getBlockState(pos).canBeReplaced()) pos = pos.relative(contexto.getClickedFace());
		Direction mira = contexto.getHorizontalDirection();
		Direction frente = mira.getOpposite(), derecha = mira.getClockWise();
		ItemStack pila = contexto.getItemInHand();
		PuertaHuesoEntity puerta = PuertasHueso.PUERTA.create(mundo);
		if (puerta == null) return InteractionResult.FAIL;
		Vec3 centro = Vec3.atBottomCenterOf(pos).add(Vec3.atLowerCornerOf(derecha.getNormal()).scale(0.5));
		puerta.moveTo(centro.x, centro.y, centro.z, frente.toYRot(), 0);
		puerta.tamaño(grueso(pila), alto(pila));
		if (!level.noCollision(puerta, puerta.getBoundingBox().deflate(0.01))) return InteractionResult.FAIL;
		mundo.addFreshEntity(puerta);
		mundo.playSound(null, centro.x, centro.y, centro.z, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
		if (contexto.getPlayer() == null || !contexto.getPlayer().isCreative()) pila.shrink(1);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Grosor: " + numero(grueso(pila)) + " · Alto: " + numero(alto(pila)) + " bloques").withColor(0xE4DBBB));
		texto.add(Component.literal("Aguanta 2 embestidas del Triceratops;").withColor(0xC6CFD6));
		texto.add(Component.literal("a la tercera se cae.").withColor(0xC6CFD6));
	}

	static String numero(float f) {
		return f == Math.round(f) ? String.valueOf(Math.round(f)) : String.valueOf(Math.round(f * 100) / 100f);
	}
}
