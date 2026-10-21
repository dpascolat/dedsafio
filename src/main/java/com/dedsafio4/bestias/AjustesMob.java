package com.dedsafio4.bestias;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Cambiar a mano los números de un mob (se guarda en el mundo y vale para todos los de ese tipo, los que ya están y
 * los que aparezcan después):
 *   /dañomob <mob> <daño>          el golpe (en los creepers, el daño de la explosión, igual de cerca o de lejos;
 *                                  en los que tiran flechas, como el esqueleto, también cada flecha)
 *   /vidamob <mob> <vida>          la vida máxima (y los que ya están quedan con la vida llena)
 *   /velocidad <mob> <velocidad>   la velocidad (0,25 es la de un zombi; 0,3 la de un esqueleto). En los que vuelan
 *                                  (fantasmas, naves, phantoms, abejas...) cambia lo rápido que van por el aire: se
 *                                  multiplica su vuelo por velocidad nueva / velocidad de siempre.
 * Con "normal" en lugar del número vuelve a ser como era.
 */
public final class AjustesMob {
	private AjustesMob() {}

	private static final String DANO = "dano", VIDA = "vida", VELOCIDAD = "velocidad";
	/** Etiqueta que se le pone al mob con la vida de /vidamob que ya recibió ("d4vida:100.0"). */
	private static final String MARCA_VIDA = "d4vida:";

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		/** "dano" / "vida" / "velocidad" → id del mob → valor. */
		final Map<String, Map<String, Double>> valores = new HashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (String que : tag.getAllKeys()) {
				CompoundTag t = tag.getCompound(que);
				Map<String, Double> m = new HashMap<>();
				for (String id : t.getAllKeys()) m.put(id, t.getDouble(id));
				d.valores.put(que, m);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			valores.forEach((que, m) -> {
				CompoundTag t = new CompoundTag();
				m.forEach(t::putDouble);
				tag.put(que, t);
			});
			return tag;
		}

		Double valor(String que, EntityType<?> tipo) {
			Map<String, Double> m = valores.get(que);
			return m == null ? null : m.get(EntityType.getKey(tipo).toString());
		}
	}

	/** El daño que se le puso a ese mob con /dañomob (o null si no se cambió). */
	public static Double danoDe(MinecraftServer server, EntityType<?> tipo) {
		return datos(server).valor(DANO, tipo);
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_ajustes_mob");
	}

	/** Lo que se cambió para este mob, puesto en sus atributos. */
	private static void aplicar(LivingEntity mob, Datos d, boolean llenarVida) {
		Double dano = d.valor(DANO, mob.getType()), vida = d.valor(VIDA, mob.getType()), velocidad = d.valor(VELOCIDAD, mob.getType());
		if (dano != null && !(mob instanceof Creeper)) poner(mob, Attributes.ATTACK_DAMAGE, dano);
		if (vida != null && poner(mob, Attributes.MAX_HEALTH, vida)) {
			// La primera vez que a este mob le toca esta vida (recién aparecido, o recién cambiada con /vidamob), arranca
			// con la vida llena. Queda anotado en el mob, así al volver a cargarlo no se le cura lo que le sacaron.
			String marca = MARCA_VIDA + vida;
			boolean nueva = !mob.getTags().contains(marca);
			if (nueva) {
				mob.getTags().removeIf(t -> t.startsWith(MARCA_VIDA));
				mob.addTag(marca);
			}
			if (llenarVida || nueva || mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
		}
		// Los que vuelan no: a ésos se les agranda el movimiento en el aire (ver factorVuelo).
		if (velocidad != null && !volador(mob)) poner(mob, Attributes.MOVEMENT_SPEED, velocidad);
	}

	/** ¿Vuela? (sin gravedad, o un mob volador de Minecraft, o que se mueve con navegación de vuelo). */
	public static boolean volador(LivingEntity mob) {
		return mob.isNoGravity() || mob instanceof net.minecraft.world.entity.FlyingMob
				|| mob instanceof net.minecraft.world.entity.animal.FlyingAnimal
				|| (mob instanceof net.minecraft.world.entity.Mob m
						&& m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation);
	}

	/** Cuánto agrandar el movimiento en el aire de este mob (1 si no tiene /velocidad o no vuela). Lo usa EntityVueloMixin. */
	public static double factorVuelo(LivingEntity mob) {
		if (mob instanceof net.minecraft.world.entity.player.Player || mob.onGround() || !(mob.level() instanceof ServerLevel mundo)) return 1;
		Double velocidad = datos(mundo.getServer()).valor(VELOCIDAD, mob.getType());
		if (velocidad == null || !volador(mob)) return 1;
		double normal = velocidadNormal(mob);
		return normal <= 0 ? 1 : velocidad / normal;
	}

	@SuppressWarnings("unchecked")
	private static double velocidadNormal(LivingEntity mob) {
		var porDefecto = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(
				(EntityType<? extends LivingEntity>) mob.getType());
		if (porDefecto.hasAttribute(Attributes.FLYING_SPEED)) return porDefecto.getBaseValue(Attributes.FLYING_SPEED);
		if (porDefecto.hasAttribute(Attributes.MOVEMENT_SPEED)) return porDefecto.getBaseValue(Attributes.MOVEMENT_SPEED);
		return 0.25;
	}

	private static boolean poner(LivingEntity mob, Holder<Attribute> atributo, double valor) {
		AttributeInstance a = mob.getAttribute(atributo);
		if (a == null) return false;
		a.setBaseValue(valor);
		return true;
	}

	/**
	 * Con /dañomob, la explosión de un creeper y las flechas de un mob (esqueletos, Stray, Bogged...) hacen ese daño
	 * justo, sin importar la dificultad ni lo rápido que vaya la flecha (lo llama LivingEntityAjustesMixin; la armadura
	 * lo baja después, como siempre). Devuelve el daño a usar, o el mismo si no corresponde.
	 */
	public static float danoExplosion(LivingEntity victima, DamageSource fuente, float danio) {
		if (!(victima.level() instanceof ServerLevel mundo)) return danio;
		Entity causa = fuente.getEntity() != null ? fuente.getEntity() : fuente.getDirectEntity();
		boolean explosion = fuente.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && causa instanceof Creeper;
		boolean flecha = fuente.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow
				&& causa instanceof net.minecraft.world.entity.Mob;
		if (!explosion && !flecha) return danio;
		Double fijo = datos(mundo.getServer()).valor(DANO, causa.getType());
		return fijo == null ? danio : fijo.floatValue();
	}

	public static void registrar() {
		ServerEntityEvents.ENTITY_LOAD.register((entidad, mundo) -> {
			if (entidad instanceof LivingEntity vivo) aplicar(vivo, datos(mundo.getServer()), false);
			if (entidad instanceof net.minecraft.world.entity.monster.Spider arana) aranaComoZombi(arana);
		});
	}

	/**
	 * Las arañas (y las de cueva) buscan pelea como un zombi: de día y de noche, a la misma distancia (35 bloques) y sin
	 * soltarte cuando hay luz. Minecraft las deja tranquilas con luz y solo ven a 16 bloques.
	 */
	private static void aranaComoZombi(net.minecraft.world.entity.monster.Spider arana) {
		// Primero la distancia: las tareas de buscar objetivo la toman al crearse.
		var distancia = arana.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
		if (distancia != null && distancia.getBaseValue() < 35) distancia.setBaseValue(35);
		var mob = (com.dedsafio4.mixin.MobAccessor) arana;
		mob.dedsafio4$objetivos().removeAllGoals(g -> g.getClass().getSimpleName().equals("SpiderTargetGoal"));
		mob.dedsafio4$goals().removeAllGoals(g -> g.getClass().getSimpleName().equals("SpiderAttackGoal"));
		mob.dedsafio4$goals().addGoal(4, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(arana, 1.0, true));
		mob.dedsafio4$objetivos().addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
				arana, net.minecraft.world.entity.player.Player.class, true));
		mob.dedsafio4$objetivos().addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
				arana, net.minecraft.world.entity.animal.IronGolem.class, true));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		for (String nombre : new String[]{"dañomob", "danomob"}) registrarUno(dispatcher, nombre, DANO);
		registrarUno(dispatcher, "vidamob", VIDA);
		registrarUno(dispatcher, "velocidad", VELOCIDAD);
	}

	private static void registrarUno(CommandDispatcher<CommandSourceStack> dispatcher, String comando, String que) {
		dispatcher.register(Commands.literal(comando).requires(s -> s.hasPermission(2))
				.then(Commands.argument("mob", ResourceLocationArgument.id())
						.suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b))
						.then(Commands.literal("normal").executes(c -> cambiar(c, que, null)))
						.then(Commands.argument("valor", DoubleArgumentType.doubleArg(0, 100000))
								.executes(c -> cambiar(c, que, DoubleArgumentType.getDouble(c, "valor"))))));
	}

	/** Busca el mob: "zombie" o "minecraft:zombie"; si no es de Minecraft, prueba con los del mod ("walker"). */
	private static EntityType<?> tipo(ResourceLocation id) {
		var tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
		if (tipo.isEmpty() && id.getNamespace().equals("minecraft")) {
			tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("dedsafio4", id.getPath()));
		}
		return tipo.orElse(null);
	}

	private static int cambiar(CommandContext<CommandSourceStack> c, String que, Double valor) {
		EntityType<?> tipo = tipo(ResourceLocationArgument.getId(c, "mob"));
		if (tipo == null) {
			c.getSource().sendFailure(Component.literal("No existe ese mob."));
			return 0;
		}
		MinecraftServer server = c.getSource().getServer();
		Datos d = datos(server);
		String id = EntityType.getKey(tipo).toString();
		Map<String, Double> m = d.valores.computeIfAbsent(que, k -> new HashMap<>());
		if (valor == null) m.remove(id);
		else m.put(id, valor);
		d.setDirty();
		// Al instante, a todos los que ya están (si volvió a lo normal, con el valor de siempre).
		int cuantos = 0;
		for (ServerLevel mundo : server.getAllLevels()) {
			for (Entity e : mundo.getAllEntities()) {
				if (e.getType() != tipo || !(e instanceof LivingEntity vivo)) continue;
				if (valor == null) volverANormal(vivo, que);
				aplicar(vivo, d, que.equals(VIDA));
				cuantos++;
			}
		}
		String nombreMob = tipo.getDescription().getString();
		String texto = valor == null
				? (que.equals(DANO) ? "El daño" : que.equals(VIDA) ? "La vida" : "La velocidad") + " de " + nombreMob + " vuelve a ser la normal."
				: (que.equals(DANO) ? "Daño" : que.equals(VIDA) ? "Vida" : "Velocidad") + " de " + nombreMob + ": " + valor
						+ (que.equals(DANO) && (Creeper.class.isAssignableFrom(tipo.getBaseClass()) || tipo == com.dedsafio4.nave.ModEntidades.BOMBA_WARDEN) ? " (la explosión)"
						: que.equals(DANO) && tipo.is(net.minecraft.tags.EntityTypeTags.SKELETONS)
								? " (el golpe y cada flecha)" : "") + ".";
		int total = cuantos;
		c.getSource().sendSuccess(() -> Component.literal(texto + " (" + total + " ya en el mundo)").withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	/** Vuelve un atributo al valor de siempre de ese mob. */
	@SuppressWarnings("unchecked")
	private static void volverANormal(LivingEntity mob, String que) {
		var porDefecto = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(
				(EntityType<? extends LivingEntity>) mob.getType());
		java.util.List<Holder<Attribute>> atributos = switch (que) {
			case DANO -> java.util.List.of(Attributes.ATTACK_DAMAGE);
			case VIDA -> java.util.List.of(Attributes.MAX_HEALTH);
			default -> java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED);
		};
		for (Holder<Attribute> a : atributos) {
			AttributeInstance instancia = mob.getAttribute(a);
			if (instancia != null && porDefecto.hasAttribute(a)) instancia.setBaseValue(porDefecto.getBaseValue(a));
		}
		if (que.equals(VIDA)) {
			mob.getTags().removeIf(t -> t.startsWith(MARCA_VIDA));
			if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
		}
	}
}
