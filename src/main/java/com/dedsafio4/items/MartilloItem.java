package com.dedsafio4.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Martillo de Netherite: repara durabilidad a cambio de experiencia.
 * - Click derecho sobre alguien: le repara de a poco las piezas de la armadura.
 * - Click derecho al aire: repara lo que tengas en la mano secundaria.
 * Manteniendo apretado el click sigue reparando solo.
 */
public class MartilloItem extends Item {
	/** Puntos de experiencia por golpe de martillo y durabilidad que devuelve cada punto. */
	private static final int PUNTOS = 4, DURABILIDAD_POR_PUNTO = 2;

	private static final int VIOLETA = 0xB06CFF, GRIS = 0xC6C6C6, AMARILLO = 0xFFD24A, VERDE = 0x55E05A;

	public MartilloItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(this.getDescriptionId(pila))
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(VIOLETA)));
	}

	/** Click derecho sobre alguien: le repara la armadura. */
	@Override
	public InteractionResult interactLivingEntity(ItemStack pila, Player jugador, LivingEntity objetivo, InteractionHand mano) {
		if (jugador.level().isClientSide) return InteractionResult.SUCCESS;
		ItemStack armadura = masRota(objetivo);
		if (armadura == null) {
			aviso(jugador, "No tiene nada roto para reparar");
			return InteractionResult.CONSUME;
		}
		if (!reparar(jugador, armadura)) return InteractionResult.CONSUME;
		return InteractionResult.CONSUME;
	}

	/** Click derecho al aire: repara lo de la mano secundaria. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
		ItemStack martillo = jugador.getItemInHand(mano);
		if (mano != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(martillo);
		if (level.isClientSide) return InteractionResultHolder.success(martillo);
		ItemStack secundaria = jugador.getOffhandItem();
		if (secundaria.isEmpty() || !secundaria.isDamaged()) {
			aviso(jugador, secundaria.isEmpty()
					? "Poné algo en la mano secundaria para repararlo"
					: "Eso no está roto");
			return InteractionResultHolder.consume(martillo);
		}
		reparar(jugador, secundaria);
		return InteractionResultHolder.consume(martillo);
	}

	/** Repara un poco gastando experiencia; avisa si no alcanza. */
	private boolean reparar(Player jugador, ItemStack objeto) {
		if (jugador.experienceLevel <= 0 && jugador.experienceProgress <= 0f && !jugador.getAbilities().instabuild) {
			aviso(jugador, "Te falta experiencia");
			return false;
		}
		int puntos = Math.min(PUNTOS, (objeto.getDamageValue() + DURABILIDAD_POR_PUNTO - 1) / DURABILIDAD_POR_PUNTO);
		objeto.setDamageValue(Math.max(0, objeto.getDamageValue() - puntos * DURABILIDAD_POR_PUNTO));
		if (!jugador.getAbilities().instabuild) jugador.giveExperiencePoints(-puntos);
		Level level = jugador.level();
		level.playSound(null, jugador.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.3f,
				1.4f + level.random.nextFloat() * 0.2f);
		if (level instanceof ServerLevel mundo) {
			mundo.sendParticles(ParticleTypes.ENCHANT, jugador.getX(), jugador.getY() + 1.2, jugador.getZ(),
					6, 0.4, 0.5, 0.4, 0.3);
		}
		return true;
	}

	/** La pieza de armadura puesta que esté más gastada. */
	private static ItemStack masRota(LivingEntity quien) {
		ItemStack peor = null;
		for (EquipmentSlot ranura : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack pieza = quien.getItemBySlot(ranura);
			if (pieza.isEmpty() || !pieza.isDamaged()) continue;
			if (peor == null || pieza.getDamageValue() > peor.getDamageValue()) peor = pieza;
		}
		return peor;
	}

	private static void aviso(Player jugador, String texto) {
		jugador.displayClientMessage(Component.literal(texto).withStyle(ChatFormatting.RED), true);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		Style gris = Style.EMPTY.withColor(TextColor.fromRgb(GRIS));
		Style amarillo = Style.EMPTY.withColor(TextColor.fromRgb(AMARILLO));
		Style verde = Style.EMPTY.withColor(TextColor.fromRgb(VERDE));
		texto.add(Component.empty());
		texto.add(Component.literal("Repara la durabilidad de").withStyle(gris));
		texto.add(Component.literal("los objetos a cambio de ").withStyle(gris)
				.append(Component.literal("XP").withStyle(amarillo))
				.append(Component.literal(".").withStyle(gris)));
		texto.add(Component.literal("Tiene dos formas de uso:").withStyle(gris));
		texto.add(Component.empty());
		texto.add(Component.literal("↖ ").withStyle(amarillo)
				.append(Component.literal("Haz ").withStyle(gris))
				.append(Component.literal("Click Derecho").withStyle(amarillo))
				.append(Component.literal(" sobre alguien,").withStyle(gris)));
		texto.add(Component.literal("y repararás poco a poco").withStyle(gris));
		texto.add(Component.literal("las piezas de su armadura.").withStyle(gris));
		texto.add(Component.empty());
		texto.add(Component.literal("↖ ").withStyle(amarillo)
				.append(Component.literal("Haz ").withStyle(gris))
				.append(Component.literal("Click Derecho").withStyle(amarillo))
				.append(Component.literal(" para").withStyle(gris)));
		texto.add(Component.literal("reparar lo que tengas").withStyle(gris));
		texto.add(Component.literal("en la Mano Secundaria.").withStyle(gris));
		texto.add(Component.empty());
		texto.add(Component.literal("En la mano principal:").withStyle(gris));
		texto.add(Component.literal(" Daño por golpe: 1").withStyle(verde));
		texto.add(Component.literal(" Velocidad de ataque: 1").withStyle(verde));
	}
}
