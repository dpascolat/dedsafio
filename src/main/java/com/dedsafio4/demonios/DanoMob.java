package com.dedsafio4.demonios;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /danomob &lt;mob&gt; &lt;daño&gt;: cuánto pega cada uno de los fantasmas y el Farol (en medios corazones, como el
 * daño de Minecraft: 2 = 1 corazón). /danomob todos &lt;daño&gt; los cambia a todos juntos y /danomob &lt;mob&gt; dice cuánto
 * pega ahora. Queda guardado en el mundo. (Sin ñ: Minecraft no deja usarla en los comandos.)
 */
public final class DanoMob {
	private DanoMob() {}

	/** Nombre en el comando → tipo de mob (el daño normal es el de sus atributos). */
	public static Map<String, EntityType<? extends Mob>> mobs() {
		Map<String, EntityType<? extends Mob>> m = new LinkedHashMap<>();
		m.put("recluso_rosa", ModDemonios.RECLUSO_ROSA);
		m.put("recluso_morado", ModDemonios.RECLUSO_MORADO);
		m.put("recluso_rojo", ModDemonios.RECLUSO_ROJO);
		m.put("coloso_rosa", ModDemonios.COLOSO_ROSA);
		m.put("coloso_morado", ModDemonios.COLOSO_MORADO);
		m.put("coloso_rojo", ModDemonios.COLOSO_ROJO);
		m.put("farol", ModDemonios.FAROL);
		return m;
	}

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		final Map<String, Float> danio = new LinkedHashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (String k : tag.getAllKeys()) d.danio.put(k, tag.getFloat(k));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			danio.forEach(tag::putFloat);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer servidor) {
		return servidor.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_danomob");
	}

	/** Le pone al mob el daño elegido con /danomob (si no se eligió, deja el normal). Se llama antes de cada golpe. */
	public static void aplicar(Mob mob) {
		MinecraftServer servidor = mob.getServer();
		if (servidor == null) return;
		Float d = datos(servidor).danio.get(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath());
		AttributeInstance a = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (a == null) return;
		if (d != null) a.setBaseValue(d);
	}

	private static String corazones(float v) {
		return texto(v / 2) + (v == 2 ? " corazón" : " corazones");
	}

	private static String texto(float v) {
		return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
	}

	/** El daño actual de ese mob: el elegido, o el normal de sus atributos. */
	private static float actual(MinecraftServer servidor, String nombre, EntityType<? extends Mob> tipo) {
		Float d = datos(servidor).danio.get(nombre);
		if (d != null) return d;
		var atributos = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(
				(EntityType<? extends net.minecraft.world.entity.LivingEntity>) tipo);
		return (float) atributos.getBaseValue(Attributes.ATTACK_DAMAGE);
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("danomob").requires(s -> s.hasPermission(2));
		mobs().forEach((nombre, tipo) -> raiz.then(Commands.literal(nombre)
				.executes(c -> {
					float v = actual(c.getSource().getServer(), nombre, tipo);
					c.getSource().sendSuccess(() -> Component.translatable(tipo.getDescriptionId()).append(" pega " + texto(v)
							+ " (" + corazones(v) + ").").withStyle(ChatFormatting.LIGHT_PURPLE), false);
					return 1;
				})
				.then(Commands.argument("dano", FloatArgumentType.floatArg(0, 1000)).executes(c -> {
					float v = FloatArgumentType.getFloat(c, "dano");
					Datos d = datos(c.getSource().getServer());
					d.danio.put(nombre, v);
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.translatable(tipo.getDescriptionId()).append(" ahora pega " + texto(v)
							+ " (" + corazones(v) + ").").withStyle(ChatFormatting.LIGHT_PURPLE), true);
					return 1;
				}))));
		raiz.then(Commands.literal("todos").then(Commands.argument("dano", FloatArgumentType.floatArg(0, 1000)).executes(c -> {
			float v = FloatArgumentType.getFloat(c, "dano");
			Datos d = datos(c.getSource().getServer());
			for (String nombre : mobs().keySet()) d.danio.put(nombre, v);
			d.setDirty();
			c.getSource().sendSuccess(() -> Component.literal("Todos los fantasmas y el Farol ahora pegan " + texto(v)
					+ " (" + corazones(v) + ").").withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return 1;
		})));
		dispatcher.register(raiz);
	}
}
