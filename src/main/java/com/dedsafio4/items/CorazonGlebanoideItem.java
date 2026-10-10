package com.dedsafio4.items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Corazón Glebanoide: con click derecho golpea a todas las criaturas Glebanoides (las de Gleba, tag
 * dedsafio4:glebanoides) que estén a RADIO bloques, enfrente o debajo tuyo (nunca a las que estén por encima de tu
 * cabeza). Cada uso gasta 1 de durabilidad (tiene 100) y EXPERIENCIA puntos de experiencia; hay que esperar 1 segundo
 * entre usos.
 */
public class CorazonGlebanoideItem extends Item {
	public static final TagKey<EntityType<?>> GLEBANOIDES = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "glebanoides"));
	private static final double RADIO = 8;
	private static final float DANIO = 8f;
	private static final int EXPERIENCIA = 5, ESPERA = 20;

	public CorazonGlebanoideItem(Properties propiedades) {
		super(propiedades.durability(100).attributes(ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 0, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build()));
	}

	@Override
	public Component getName(ItemStack pila) {
		return Component.translatable(getDescriptionId()).withColor(0xE07AE6);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level mundo, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		if (mundo.isClientSide) return InteractionResultHolder.success(pila);
		if (!jugador.isCreative() && jugador.totalExperience < EXPERIENCIA) {
			jugador.displayClientMessage(Component.literal("No tienes suficiente experiencia.").withColor(0xFF7A7A), true);
			return InteractionResultHolder.fail(pila);
		}
		Vec3 mira = jugador.getLookAngle().multiply(1, 0, 1).normalize();
		int golpeados = 0;
		for (LivingEntity e : mundo.getEntitiesOfClass(LivingEntity.class, jugador.getBoundingBox().inflate(RADIO),
				e -> e.isAlive() && e.getType().is(GLEBANOIDES) && e.distanceToSqr(jugador) <= RADIO * RADIO)) {
			// Nunca a las que están por encima de tu cabeza; sí a las de enfrente o debajo tuyo.
			if (e.getY() > jugador.getEyeY()) continue;
			Vec3 hacia = e.position().subtract(jugador.position()).multiply(1, 0, 1);
			boolean enfrente = hacia.lengthSqr() < 0.01 || hacia.normalize().dot(mira) > 0;
			boolean debajo = e.getY() + e.getBbHeight() <= jugador.getY() + 0.1;
			if (!enfrente && !debajo) continue;
			e.hurt(jugador.damageSources().playerAttack(jugador), DANIO);
			((ServerLevel) mundo).sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(),
					6, 0.3, 0.3, 0.3, 0.1);
			golpeados++;
		}
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 1.4f);
		((ServerLevel) mundo).sendParticles(ParticleTypes.SWEEP_ATTACK, jugador.getX() + mira.x * 1.5, jugador.getY() + 1, jugador.getZ() + mira.z * 1.5,
				1, 0, 0, 0, 0);
		if (!jugador.isCreative()) {
			jugador.giveExperiencePoints(-EXPERIENCIA);
			pila.hurtAndBreak(1, jugador, LivingEntity.getSlotForHand(mano));
		}
		jugador.getCooldowns().addCooldown(this, ESPERA);
		if (golpeados == 0) jugador.displayClientMessage(Component.literal("No hay criaturas Glebanoides cerca.").withColor(0xC6CFD6), true);
		return InteractionResultHolder.consume(pila);
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		int blanco = 0xE8E8E8, rosa = 0xFF7A7A, naranja = 0xFFA23C, violeta = 0xD98CFF;
		texto.add(Component.empty());
		texto.add(Component.literal("Golpea en radio a las Criaturas").withColor(blanco));
		texto.add(Component.literal("Glebanoides únicamente. Pueden").withColor(blanco));
		texto.add(Component.literal("estar enfrente o debajo tuyo.").withColor(blanco));
		texto.add(Component.empty());
		texto.add(Component.literal("⚠ Atención: ").withColor(0xF0D86A).append(Component.literal("SÓLO sirve con").withColor(rosa)));
		texto.add(Component.literal("criaturas de Gleba. NO puedes golpear").withColor(rosa));
		texto.add(Component.literal("entidades que estén por encima").withColor(rosa));
		texto.add(Component.literal("de tu cabeza.").withColor(rosa));
		texto.add(Component.empty());
		texto.add(Component.literal("Se utiliza con ").withColor(blanco).append(Component.literal("Click Derecho").withColor(naranja))
				.append(Component.literal(".").withColor(blanco)));
		texto.add(Component.literal("Gasta ").withColor(blanco).append(Component.literal("Durabilidad").withColor(naranja))
				.append(Component.literal(" y ").withColor(blanco)).append(Component.literal("Experiencia").withColor(violeta)));
		texto.add(Component.literal("al usarlo.").withColor(blanco));
	}
}
