package com.dedsafio4.demonios;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Los mobs nuevos sin función todavía (nombres provisorios): Demonio Rosa, Demonio con Ojos y Demonio Rojo (el mismo
 * cuerpo con distinto color) y la Criatura Oscura (la de 4 patas). Cada uno con su huevo.
 * <p>
 * /boss tamano &lt;tamaño&gt; cambia el tamaño del jefe o mob del mod más cercano (Planta Mordedora, Qumara, estos y
 * cualquier otro): 1 = normal, 2 = el doble, 0,5 = la mitad (de 0,1 a 16). Usa el atributo de tamaño de Minecraft,
 * así que también se agranda la caja para pegarle.
 */
public final class ModDemonios {
	private ModDemonios() {}

	/** Los Demonios miden unos 3 bloques; la Criatura Oscura, 2,5. */
	public static final EntityType<MallaMobEntity> DEMONIO_ROSA = entidad("demonio_rosa", true, 1.6f, 3f);
	public static final EntityType<MallaMobEntity> DEMONIO_OJOS = entidad("demonio_ojos", true, 1.6f, 3f);
	public static final EntityType<MallaMobEntity> DEMONIO_ROJO = entidad("demonio_rojo", true, 1.6f, 3f);
	public static final EntityType<MallaMobEntity> CRIATURA_OSCURA = entidad("criatura_oscura", false, 2f, 2.5f);

	public static final Item HUEVO_DEMONIO_ROSA = huevo("demonio_rosa", DEMONIO_ROSA, 0xE9A3D6, 0x1E1B2E);
	public static final Item HUEVO_DEMONIO_OJOS = huevo("demonio_ojos", DEMONIO_OJOS, 0xB58AD8, 0x1E1B2E);
	public static final Item HUEVO_DEMONIO_ROJO = huevo("demonio_rojo", DEMONIO_ROJO, 0xC23A32, 0x1E1B2E);
	public static final Item HUEVO_CRIATURA_OSCURA = huevo("criatura_oscura", CRIATURA_OSCURA, 0x2B3138, 0x6E7A86);

	private static final double BUSCAR = 64;

	public static void registrar() {
		for (EntityType<MallaMobEntity> t : java.util.List.of(DEMONIO_ROSA, DEMONIO_OJOS, DEMONIO_ROJO, CRIATURA_OSCURA)) {
			FabricDefaultAttributeRegistry.register(t, MallaMobEntity.crearAtributos());
		}
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		// (Sin ñ: Minecraft no deja escribir la ñ en los nombres de los comandos.)
		dispatcher.register(Commands.literal("boss").requires(s -> s.hasPermission(2))
				.then(Commands.literal("tamano").then(Commands.argument("tamano", FloatArgumentType.floatArg(0.1f, 16f))
						.executes(c -> tamano(c.getSource(), FloatArgumentType.getFloat(c, "tamano"))))));
	}

	/** Cambia el tamaño del mob del mod más cercano (sin contar jugadores). */
	private static int tamano(CommandSourceStack fuente, float tamano) {
		Vec3 pos = fuente.getPosition();
		LivingEntity mejor = null;
		double d = BUSCAR * BUSCAR;
		for (LivingEntity e : fuente.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(BUSCAR),
				e -> e.isAlive() && Dedsafio4.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace()))) {
			double dd = e.distanceToSqr(pos);
			if (dd < d) {
				d = dd;
				mejor = e;
			}
		}
		if (mejor == null) {
			fuente.sendFailure(Component.literal("No hay ningún jefe ni mob del mod cerca (hasta 64 bloques)."));
			return 0;
		}
		AttributeInstance escala = mejor.getAttribute(Attributes.SCALE);
		if (escala == null) {
			fuente.sendFailure(Component.literal("A ese no se le puede cambiar el tamaño."));
			return 0;
		}
		escala.setBaseValue(tamano);
		mejor.refreshDimensions();
		Component nombre = mejor.getName();
		fuente.sendSuccess(() -> Component.literal("Tamaño de ").append(nombre).append(": " + tamano + " (1 = normal).").withColor(0xF0418F), true);
		return 1;
	}

	private static EntityType<MallaMobEntity> entidad(String nombre, boolean flota, float ancho, float alto) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.<MallaMobEntity>of((t, l) -> new MallaMobEntity(t, l, flota), MobCategory.MISC).sized(ancho, alto)
						.clientTrackingRange(10).build(id.toString()));
	}

	private static Item huevo(String nombre, EntityType<MallaMobEntity> tipo, int fondo, int manchas) {
		return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre + "_spawn_egg"),
				new SpawnEggItem(tipo, fondo, manchas, new Item.Properties()));
	}
}
