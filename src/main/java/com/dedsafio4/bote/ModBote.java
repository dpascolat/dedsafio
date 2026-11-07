package com.dedsafio4.bote;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * El Bote (ver BoteEntity): la entidad, el ítem para ponerlo en el agua (no tiene crafteo: se saca del creativo o
 * con /give) y /bote_velocidad &lt;velocidad&gt;, que cambia la velocidad de todos los botes (1 = como un bote común,
 * 2 = el doble, 0,5 = la mitad). Queda guardada en el mundo.
 */
public final class ModBote {
	private ModBote() {}

	/** La caja del bote: chica y en el medio (el dibujo es mucho más largo). Se sube haciendo clic en el medio. */
	public static final EntityType<BoteEntity> BOTE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("bote"),
			EntityType.Builder.<BoteEntity>of(BoteEntity::new, MobCategory.MISC).sized(3.0f, 0.5625f)
					.clientTrackingRange(10).build("bote"));

	public static final Item BOTE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("bote"),
			new BoteItem(new Item.Properties().stacksTo(1)));

	public static final double VELOCIDAD_MAXIMA = 10;

	public static void registrar() {
	}

	public static double velocidad(MinecraftServer servidor) {
		return datos(servidor).velocidad;
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bote_velocidad").requires(s -> s.hasPermission(2))
				.executes(c -> {
					double v = velocidad(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal("Velocidad de los botes: " + texto(v)
							+ " (1 = normal). Para cambiarla: /bote_velocidad <velocidad>").withStyle(ChatFormatting.AQUA), false);
					return 1;
				})
				.then(Commands.argument("velocidad", DoubleArgumentType.doubleArg(0, VELOCIDAD_MAXIMA)).executes(c -> {
					double v = DoubleArgumentType.getDouble(c, "velocidad");
					Datos d = datos(c.getSource().getServer());
					d.velocidad = v;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Ahora todos los botes andan a velocidad " + texto(v)
							+ " (1 = normal).").withStyle(ChatFormatting.AQUA), true);
					return 1;
				})));
	}

	private static String texto(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	private static Datos datos(MinecraftServer servidor) {
		return servidor.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_bote");
	}

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		double velocidad = 1;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			if (tag.contains("velocidad")) d.velocidad = tag.getDouble("velocidad");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putDouble("velocidad", velocidad);
			return tag;
		}
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}
}
