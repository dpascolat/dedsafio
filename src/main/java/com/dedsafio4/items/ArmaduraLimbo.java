package com.dedsafio4.items;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.dimension.Limbo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 * La armadura del Limbo (sin encantamientos; cada pieza da 1 corazón extra mientras la tienes puesta):
 *   Casco       5 de armadura, 2 de resistencia, 907 de durabilidad, reducción base ~20%
 *   Pechera    10 de armadura, 2 de resistencia, 1092 de durabilidad, reducción base ~40%
 *   Pantalones  8 de armadura, 2 de resistencia, 1055 de durabilidad, reducción base ~32%
 *   Botas       5 de armadura, 2 de resistencia, 981 de durabilidad, reducción base ~20%
 * Cada pieza puesta (también las Cedrónidas, la mejora) saca un 12,5% del daño que hacen las Criaturas del Limbo
 * (tag dedsafio4:criaturas_limbo).
 * Con el set completo (las 4 piezas) se recibe la mitad del daño dentro del Limbo y de sus criaturas.
 * Las texturas puestas en el cuerpo (textures/models/armor/limbo_layer_1 y _2) son las de hierro pintadas con los
 * colores de los íconos.
 */
public final class ArmaduraLimbo {
	private ArmaduraLimbo() {}

	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo");
	public static final TagKey<EntityType<?>> CRIATURAS = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "criaturas_limbo"));
	/** Lo que saca cada pieza del daño de las criaturas del Limbo, y lo que se saca con el set completo. */
	private static final float POR_PIEZA = 0.125f, CON_SET = 0.5f;
	private static final int LILA = 0xB9A5FF, AMARILLO = 0xFFD84A, BLANCO = 0xFFFFFF, VERDE = 0x7CFC6A, CELESTE = 0x5CE1E6;

	public static final Holder<ArmorMaterial> MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ID,
			new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 5, ArmorItem.Type.CHESTPLATE, 10, ArmorItem.Type.LEGGINGS, 8,
					ArmorItem.Type.BOOTS, 5, ArmorItem.Type.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE,
					() -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(ID)), 2f, 0f));

	/** Una pieza: nombre lila, 1 corazón extra y la descripción del set. */
	public static class Pieza extends ArmorItem {
		private final ResourceLocation corazon;
		private final EquipmentSlotGroup lugar;
		private final int reduccion;

		public Pieza(Type tipo, int durabilidad, int reduccion, Properties propiedades) {
			super(MATERIAL, tipo, propiedades.durability(durabilidad));
			this.reduccion = reduccion;
			this.lugar = EquipmentSlotGroup.bySlot(tipo.getSlot());
			this.corazon = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "limbo_corazon_" + tipo.getName());
		}

		@Override
		public Component getName(ItemStack pila) {
			return Component.translatable(getDescriptionId()).withColor(LILA);
		}

		/** La armadura de siempre más 1 corazón de vida máxima. */
		@Override
		public ItemAttributeModifiers getDefaultAttributeModifiers() {
			return super.getDefaultAttributeModifiers().withModifierAdded(Attributes.MAX_HEALTH,
					new AttributeModifier(corazon, 2.0, AttributeModifier.Operation.ADD_VALUE), lugar);
		}

		@Override
		public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
			texto.add(Component.empty());
			texto.add(Component.literal("Con el set completo recibes").withColor(BLANCO));
			texto.add(Component.literal("50%").withColor(AMARILLO).append(Component.literal(" menos de Daño dentro").withColor(BLANCO)));
			texto.add(Component.literal("del Limbo y de sus criaturas.").withColor(BLANCO));
			texto.add(Component.empty());
			texto.add(Component.literal("♥ Otorga 1 Corazón Extra.").withColor(VERDE));
			texto.add(Component.literal("⛨ Otorga 12,5 Protección").withColor(CELESTE));
			texto.add(Component.literal("contra Criaturas del Limbo.").withColor(CELESTE));
			texto.add(Component.literal("⛨ Reducción Base: ~" + reduccion + "%").withColor(CELESTE));
		}
	}

	/** Cuántas piezas del Limbo tiene puestas. */
	public static int piezas(LivingEntity quien) {
		int n = 0;
		for (EquipmentSlot lugar : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (quien.getItemBySlot(lugar).getItem() instanceof Pieza) n++;
		}
		return n;
	}

	/** El daño que recibe con las piezas del Limbo puestas (lo llama LivingEntityAjustesMixin al principio de hurt). */
	public static float reducir(LivingEntity quien, DamageSource fuente, float danio) {
		if (quien.level().isClientSide || danio <= 0) return danio;
		int n = piezas(quien), cedronidas = ArmaduraCedronida.piezas(quien);
		if (n + cedronidas == 0) return danio;
		boolean deCriatura = esCriatura(fuente.getEntity()) || esCriatura(fuente.getDirectEntity());
		boolean enLimbo = quien.level().dimension() == Limbo.DIMENSION;
		if (n == 4 && (enLimbo || deCriatura)) return danio * (1 - CON_SET);
		if (deCriatura) return danio * (1 - POR_PIEZA * (n + cedronidas));
		return danio;
	}

	private static boolean esCriatura(Entity e) {
		return e != null && e.getType().is(CRIATURAS);
	}
}
