package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

/**
 * La Armadura Cedrónida: la mejora de la del Limbo (con la Plantilla de Mejora Cedrónida, en la mesa de herrería: la
 * receta se pone con /mesa_de_plantilla). Cada pieza da 2 corazones extra y saca un 12,5% del daño de las Criaturas del
 * Limbo (como la del Limbo: ver ArmaduraLimbo.reducir). Con el set completo da, mientras lo tengas puesto, Fuerza II,
 * Regeneración I, Velocidad I y Resistencia I.
 *   Casco       6 de armadura, 2 de resistencia, 907 de durabilidad
 *   Pechera    11 de armadura, 2 de resistencia, 1092 de durabilidad
 *   Pantalones  9 de armadura, 2 de resistencia, 1055 de durabilidad
 *   Botas       6 de armadura, 2 de resistencia, 981 de durabilidad y +1 de movimiento en el agua
 * La textura puesta (textures/models/armor/cedronida_layer_1 y _2): por ahora solo están dibujadas las botas.
 */
public final class ArmaduraCedronida {
	private ArmaduraCedronida() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cedronida");
	public static final int ROSA = 0xF04BD2;
	private static final int BLANCO = 0xE8E8E8, VERDE = 0x55FF55, AZUL = 0x5FA8FF;

	/** Los efectos del set completo (efecto y nivel, empezando en 0). */
	private static final List<Map.Entry<Holder<MobEffect>, Integer>> EFECTOS = List.of(
			Map.entry(MobEffects.DAMAGE_BOOST, 1), Map.entry(MobEffects.REGENERATION, 0),
			Map.entry(MobEffects.MOVEMENT_SPEED, 0), Map.entry(MobEffects.DAMAGE_RESISTANCE, 0));

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 6, ArmorItem.Type.CHESTPLATE, 11, ArmorItem.Type.LEGGINGS, 9,
					ArmorItem.Type.BOOTS, 6, ArmorItem.Type.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
					() -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(ID)), 2f, 0f));

	/** Una pieza: nombre rosa, 2 corazones extra (las botas, además, más rápido en el agua) y la descripción del set. */
	public static class Pieza extends ArmorItem {
		private final ResourceLocation corazones, agua;
		private final EquipmentSlotGroup lugar;

		public Pieza(Type tipo, int durabilidad, Properties propiedades) {
			super(MATERIAL, tipo, propiedades.durability(durabilidad));
			this.lugar = EquipmentSlotGroup.bySlot(tipo.getSlot());
			this.corazones = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cedronida_corazones_" + tipo.getName());
			this.agua = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "cedronida_agua_" + tipo.getName());
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(ROSA);
		}

		@Override
		public ItemAttributeModifiers getDefaultAttributeModifiers() {
			ItemAttributeModifiers m = super.getDefaultAttributeModifiers().withModifierAdded(Attributes.MAX_HEALTH,
					new AttributeModifier(corazones, 4.0, AttributeModifier.Operation.ADD_VALUE), lugar);
			if (getType() == Type.BOOTS) m = m.withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY,
					new AttributeModifier(agua, 1.0, AttributeModifier.Operation.ADD_VALUE), lugar);
			return m;
		}

		@Override
		public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
			texto.add(Component.empty());
			texto.add(Component.literal("El set completo otorga,").withColor(BLANCO));
			texto.add(Component.literal("de forma infinita:").withColor(BLANCO));
			texto.add(Component.empty());
			for (String efecto : new String[]{"Fuerza II", "Regeneración I", "Velocidad I", "Resistencia I"}) {
				texto.add(Component.literal("✦ " + efecto).withColor(VERDE));
			}
			texto.add(Component.empty());
			texto.add(Component.literal("♥ Otorga 2 Corazones Extra.").withColor(VERDE));
			texto.add(Component.literal("⛨ Otorga 12,5 Protección").withColor(AZUL));
			texto.add(Component.literal("contra Criaturas del Limbo.").withColor(AZUL));
		}
	}

	/** Cuántas piezas Cedrónidas tiene puestas. */
	public static int piezas(LivingEntity quien) {
		int n = 0;
		for (EquipmentSlot lugar : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (quien.getItemBySlot(lugar).getItem() instanceof Pieza) n++;
		}
		return n;
	}

	/**
	 * Cada segundo: con el set completo, los 4 efectos sin fin (ocultos, como los de un faro); sin el set, se sacan los
	 * que puso la armadura (los sin fin), no los de una poción.
	 */
	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				boolean set = piezas(jugador) == 4;
				for (var e : EFECTOS) {
					MobEffectInstance actual = jugador.getEffect(e.getKey());
					boolean nuestro = actual != null && actual.isInfiniteDuration() && actual.getAmplifier() == e.getValue();
					if (set && (actual == null || actual.getAmplifier() < e.getValue())) {
						jugador.addEffect(new MobEffectInstance(e.getKey(), MobEffectInstance.INFINITE_DURATION, e.getValue(), true, false, true));
					} else if (!set && nuestro) {
						jugador.removeEffect(e.getKey());
					}
				}
			}
		});
	}
}
