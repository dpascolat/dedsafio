package com.dedsafio4.demonios;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.HuevoConDescripcion;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/**
 * Los fantasmas encadenados (ver EspectroEntity): los Reclusos Espectrales (chicos, unos 2 bloques) y los Colosos
 * Prisioneros (grandes, unos 5 bloques), en rosa, morado y rojo; el efecto Sin Salto del Morado; y el Farol (el de 4
 * patas, que al pegar bloquea la cámara con un puzzle). Cada uno con su huevo. El daño de cada uno se cambia con
 * /dañomob &lt;mob&gt; &lt;daño&gt; (ver AjustesMob), como el de los demás mobs.
 * <p>
 * /boss tamano &lt;tamaño&gt; cambia el tamaño del jefe o mob del mod más cercano (Planta Mordedora, estos y cualquier
 * otro): 1 = normal, 2 = el doble, 0,5 = la mitad (de 0,1 a 16). Usa el atributo de tamaño de Minecraft, así que
 * también se agranda la caja para pegarle.
 */
public final class ModDemonios {
	private ModDemonios() {}

	/** El modelo mide 3 bloques: los Reclusos se dibujan al 65% y los Colosos al 160% (ver DemonioRenderer). */
	public static final float ESCALA_RECLUSO = 0.65f, ESCALA_COLOSO = 1.6f;

	public static final EntityType<EspectroEntity> RECLUSO_ROSA = espectro("recluso_rosa", EspectroEntity.Color.ROSA, false);
	public static final EntityType<EspectroEntity> RECLUSO_MORADO = espectro("recluso_morado", EspectroEntity.Color.MORADO, false);
	public static final EntityType<EspectroEntity> RECLUSO_ROJO = espectro("recluso_rojo", EspectroEntity.Color.ROJO, false);
	public static final EntityType<EspectroEntity> COLOSO_ROSA = espectro("coloso_rosa", EspectroEntity.Color.ROSA, true);
	public static final EntityType<EspectroEntity> COLOSO_MORADO = espectro("coloso_morado", EspectroEntity.Color.MORADO, true);
	public static final EntityType<EspectroEntity> COLOSO_ROJO = espectro("coloso_rojo", EspectroEntity.Color.ROJO, true);
	/** El Farol (ver FarolEntity): el modelo mide 3 bloques de alto y unos 2,6 de ancho con las patas. */
	public static final EntityType<FarolEntity> FAROL = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("farol"),
			EntityType.Builder.<FarolEntity>of(FarolEntity::new, MobCategory.MONSTER).sized(1.8f, 2.9f)
					.clientTrackingRange(10).build(id("farol").toString()));

	public static final Item HUEVO_RECLUSO_ROSA = huevo("recluso_rosa", RECLUSO_ROSA, 0xE9A3D6, 0x1E1B2E, ModDemonios::rosa);
	public static final Item HUEVO_RECLUSO_MORADO = huevo("recluso_morado", RECLUSO_MORADO, 0x9A5BC8, 0x1E1B2E, t -> morado(t, false));
	public static final Item HUEVO_RECLUSO_ROJO = huevo("recluso_rojo", RECLUSO_ROJO, 0xC23A32, 0x1E1B2E, ModDemonios::rojo);
	public static final Item HUEVO_COLOSO_ROSA = huevo("coloso_rosa", COLOSO_ROSA, 0xE9A3D6, 0x0E0C18, t -> {
		rosa(t);
		noEmpujar(t);
	});
	public static final Item HUEVO_COLOSO_MORADO = huevo("coloso_morado", COLOSO_MORADO, 0x9A5BC8, 0x0E0C18, t -> {
		morado(t, true);
		noEmpujar(t);
	});
	public static final Item HUEVO_COLOSO_ROJO = huevo("coloso_rojo", COLOSO_ROJO, 0xC23A32, 0x0E0C18, t -> {
		rojo(t);
		noEmpujar(t);
	});
	public static final Item HUEVO_FAROL = Registry.register(BuiltInRegistries.ITEM, id("farol_spawn_egg"),
			new HuevoConDescripcion(FAROL, 0x2B3138, 0xC8C6F0, new Item.Properties(), ModDemonios::farol));

	public static final List<Item> HUEVOS = List.of(HUEVO_RECLUSO_ROSA, HUEVO_RECLUSO_MORADO, HUEVO_RECLUSO_ROJO,
			HUEVO_COLOSO_ROSA, HUEVO_COLOSO_MORADO, HUEVO_COLOSO_ROJO, HUEVO_FAROL);

	/** Sin Salto (lo da el Morado al golpear): la fuerza de salto queda en 0. */
	public static final Holder<MobEffect> SIN_SALTO = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("sin_salto"), new SinSalto());

	private static final class SinSalto extends MobEffect {
		SinSalto() {
			super(MobEffectCategory.HARMFUL, 0x7A3FB0);
			addAttributeModifier(Attributes.JUMP_STRENGTH, id("sin_salto"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
	}

	private static final double BUSCAR = 64;

	public static boolean esColoso(EntityType<?> tipo) {
		return tipo == COLOSO_ROSA || tipo == COLOSO_MORADO || tipo == COLOSO_ROJO;
	}

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(RECLUSO_ROSA, EspectroEntity.atributosRecluso(false));
		FabricDefaultAttributeRegistry.register(RECLUSO_MORADO, EspectroEntity.atributosRecluso(true));
		FabricDefaultAttributeRegistry.register(RECLUSO_ROJO, EspectroEntity.atributosRecluso(false));
		FabricDefaultAttributeRegistry.register(COLOSO_ROSA, EspectroEntity.atributosColoso(false));
		FabricDefaultAttributeRegistry.register(COLOSO_MORADO, EspectroEntity.atributosColoso(true));
		FabricDefaultAttributeRegistry.register(COLOSO_ROJO, EspectroEntity.atributosColoso(false));
		FabricDefaultAttributeRegistry.register(FAROL, FarolEntity.crearAtributos());
		PuzzleCamara.registrar();
	}

	// --- Descripciones de los huevos ---

	private static final int GRIS = HuevoConDescripcion.GRIS, VIOLETA = HuevoConDescripcion.VIOLETA, AVISO = 0xF2D24B;

	private static void rosa(List<Component> t) {
		t.add(parte("Criatura que hace daño cuerpo a", GRIS));
		t.add(parte("cuerpo. Si está cerca, te aplicará", GRIS));
		t.add(parte("Hambre LVL. 100", VIOLETA).append(parte(" durante algunos", GRIS)));
		t.add(parte("segundos.", GRIS));
	}

	private static void morado(List<Component> t, boolean coloso) {
		t.add(parte("Criatura que hace daño cuerpo a", GRIS));
		t.add(parte("cuerpo. Si te golpea, te aplicará", GRIS));
		t.add(parte("un efecto que ", GRIS).append(parte("no te permitirá", VIOLETA)));
		t.add(parte("saltar", VIOLETA).append(parte(coloso ? ". Muy lento y con mucha" : ". Puede volar.", GRIS)));
		if (coloso) t.add(parte("vida.", GRIS));
	}

	private static void rojo(List<Component> t) {
		t.add(parte("Criatura que hace daño cuerpo", GRIS));
		t.add(parte("a cuerpo. Te aplica ", GRIS).append(parte("Lentitud", VIOLETA)).append(parte(".", GRIS)));
	}

	private static void farol(List<Component> t) {
		t.add(parte("Criatura que hace daño cuerpo a", GRIS));
		t.add(parte("cuerpo. Si te golpea, te ", GRIS).append(parte("bloquea la", VIOLETA)));
		t.add(parte("cámara", VIOLETA).append(parte(" hasta que gires la palanca", GRIS)));
		t.add(parte("4 veces.", GRIS));
	}

	private static void noEmpujar(List<Component> t) {
		t.add(Component.empty());
		t.add(parte("⚠ Atención: No puedes empujarlo.", AVISO));
	}

	private static MutableComponent parte(String texto, int color) {
		return HuevoConDescripcion.parte(texto, color);
	}

	// --- /boss tamano ---

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String palabra : new String[]{"tamaño", "tamano"}) {
			dispatcher.register(Commands.literal("boss").requires(s -> s.hasPermission(2))
					.then(Commands.literal(palabra).then(Commands.argument("tamano", FloatArgumentType.floatArg(0.1f, 16f))
							.executes(c -> tamano(c.getSource(), FloatArgumentType.getFloat(c, "tamano"))))));
		}
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

	private static EntityType<EspectroEntity> espectro(String nombre, EspectroEntity.Color color, boolean grande) {
		float e = grande ? ESCALA_COLOSO : ESCALA_RECLUSO;
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id(nombre),
				EntityType.Builder.<EspectroEntity>of((t, l) -> new EspectroEntity(t, l, color, grande), MobCategory.MONSTER)
						.sized(1.5f * e, 3f * e).clientTrackingRange(10).build(id(nombre).toString()));
	}

	private static Item huevo(String nombre, EntityType<EspectroEntity> tipo, int fondo, int manchas, Consumer<List<Component>> descripcion) {
		return Registry.register(BuiltInRegistries.ITEM, id(nombre + "_spawn_egg"),
				new HuevoConDescripcion(tipo, fondo, manchas, new Item.Properties(), descripcion));
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}
}
