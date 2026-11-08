package com.dedsafio4.bossplanta;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * La Planta Mordedora (ver PlantaBossEntity) y sus comandos:
 * <ul>
 * <li>/campo_boss &lt;radio&gt;: arma un campo redondo centrado donde estás parado y pone la planta en el medio.</li>
 * <li>/campo_boss quitar: saca las plantas que haya cerca (hasta 128 bloques).</li>
 * <li>/campo_boss radio &lt;radio&gt;: agranda o achica el campo de la planta más cercana (sin moverla).</li>
 * <li>/boss 2 gente N: la vida de la planta más cercana pasa a ser N × 500 (se mantiene el porcentaje).</li>
 * </ul>
 */
public final class ModPlantaBoss {
	private ModPlantaBoss() {}

	public static final EntityType<PlantaBossEntity> PLANTA_BOSS = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("planta_boss"),
			EntityType.Builder.<PlantaBossEntity>of(PlantaBossEntity::new, MobCategory.MISC).sized(2.4f, 5f)
					.clientTrackingRange(16).updateInterval(1).build("planta_boss"));

	/** Mirar la luz. */
	public static final ResourceKey<DamageType> DANIO_LUZ = ResourceKey.create(Registries.DAMAGE_TYPE, id("luz_planta"));
	/** Las mordidas (no miran la armadura ni la gastan). */
	public static final ResourceKey<DamageType> DANIO_MORDIDA = ResourceKey.create(Registries.DAMAGE_TYPE, id("mordida_planta"));

	private static final double BUSCAR = 128;

	public static void registrar() {
		FabricDefaultAttributeRegistry.register(PLANTA_BOSS, PlantaBossEntity.crearAtributos());
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		DanioJefe.registrarComandos(dispatcher);
		dispatcher.register(Commands.literal("campo_boss").requires(s -> s.hasPermission(2))
				.then(Commands.literal("quitar").executes(c -> quitar(c.getSource())))
				.then(Commands.literal("radio").then(Commands.argument("radio", IntegerArgumentType.integer(8, 100))
						.executes(c -> cambiarRadio(c.getSource(), IntegerArgumentType.getInteger(c, "radio")))))
				.then(Commands.argument("radio", IntegerArgumentType.integer(8, 100))
						.executes(c -> crear(c.getSource(), IntegerArgumentType.getInteger(c, "radio")))));
		dispatcher.register(Commands.literal("boss").requires(s -> s.hasPermission(2))
				.then(Commands.literal("2").then(Commands.literal("gente").then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100))
						.executes(c -> gente(c.getSource(), IntegerArgumentType.getInteger(c, "cantidad")))))));
	}

	private static int crear(CommandSourceStack fuente, int radio) {
		ServerLevel mundo = fuente.getLevel();
		Vec3 pos = fuente.getPosition();
		PlantaBossEntity planta = PLANTA_BOSS.create(mundo);
		if (planta == null) return 0;
		double x = Mth.floor(pos.x) + 0.5, z = Mth.floor(pos.z) + 0.5;
		planta.moveTo(x, Mth.floor(pos.y), z, fuente.getRotation().y + 180, 0);
		planta.yBodyRot = planta.yHeadRot = planta.getYRot();
		planta.campo(radio);
		mundo.addFreshEntity(planta);
		fuente.sendSuccess(() -> Component.literal("Campo de " + radio + " bloques de radio armado: la Planta Mordedora está en el medio. "
				+ "Ataca a los que entren al campo.").withColor(0xF0418F), true);
		return 1;
	}

	/** /campo_boss radio <radio>: agranda (o achica) el campo de la planta más cercana, sin moverla ni curarla. */
	private static int cambiarRadio(CommandSourceStack fuente, int radio) {
		PlantaBossEntity mejor = null;
		double d = Double.MAX_VALUE;
		for (PlantaBossEntity p : cercanas(fuente)) {
			double dd = p.distanceToSqr(fuente.getPosition());
			if (dd < d) {
				d = dd;
				mejor = p;
			}
		}
		if (mejor == null) {
			fuente.sendFailure(Component.literal("No hay ninguna Planta Mordedora cerca."));
			return 0;
		}
		mejor.cambiarRadio(radio);
		fuente.sendSuccess(() -> Component.literal("El campo de la Planta Mordedora ahora tiene " + radio + " bloques de radio.").withColor(0xF0418F), true);
		return 1;
	}

	private static int quitar(CommandSourceStack fuente) {
		int n = 0;
		for (PlantaBossEntity p : cercanas(fuente)) {
			p.discard();
			n++;
		}
		int cuantas = n;
		fuente.sendSuccess(() -> Component.literal(cuantas == 0 ? "No hay ninguna Planta Mordedora cerca."
				: "Saqué " + cuantas + (cuantas == 1 ? " Planta Mordedora." : " Plantas Mordedoras.")).withColor(0xF0418F), true);
		return n;
	}

	private static int gente(CommandSourceStack fuente, int cantidad) {
		PlantaBossEntity mejor = null;
		double d = Double.MAX_VALUE;
		for (PlantaBossEntity p : cercanas(fuente)) {
			double dd = p.distanceToSqr(fuente.getPosition());
			if (dd < d) {
				d = dd;
				mejor = p;
			}
		}
		if (mejor == null) {
			fuente.sendFailure(Component.literal("No hay ninguna Planta Mordedora cerca."));
			return 0;
		}
		AttributeInstance vida = mejor.getAttribute(Attributes.MAX_HEALTH);
		if (vida == null) return 0;
		float porcentaje = mejor.getHealth() / mejor.getMaxHealth();
		vida.setBaseValue(PlantaBossEntity.VIDA_BASE * cantidad);
		mejor.setHealth(mejor.getMaxHealth() * porcentaje);
		fuente.sendSuccess(() -> Component.literal("La Planta Mordedora ahora tiene " + (int) (PlantaBossEntity.VIDA_BASE * cantidad)
				+ " de vida (" + cantidad + (cantidad == 1 ? " persona" : " personas") + ").").withColor(0xF0418F), true);
		return cantidad;
	}

	private static java.util.List<PlantaBossEntity> cercanas(CommandSourceStack fuente) {
		Vec3 pos = fuente.getPosition();
		return fuente.getLevel().getEntitiesOfClass(PlantaBossEntity.class, new AABB(pos, pos).inflate(BUSCAR), e -> e.isAlive());
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}
}
