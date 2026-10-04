package com.dedsafio4.casino;

import com.dedsafio4.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Los premios del Casino. /casino 1 prende la tabla de premios 1 (hasta que se prenda otra) y le muestra en pantalla
 * al que la prendió qué da cada figura (TablaPremiosPayload); /casino 0 la apaga (las ruedas giran igual, pero no
 * dan nada). Se guarda en el mundo.
 * Con 2 figuras iguales se da el premio chico de esa figura; con 3, el grande. Cada premio tiene varias opciones y
 * sale UNA sola, al azar (por ejemplo: 10 zanahorias de oro O 1 manzana de oro).
 */
public final class CasinoPremios {
	private CasinoPremios() {}

	// --- El modo (qué tabla de premios está prendida) ---

	public static final class Modo extends SavedData {
		public static final SavedData.Factory<Modo> FACTORY = new SavedData.Factory<>(Modo::new, Modo::leer, null);
		int modo = 0;

		private static Modo leer(CompoundTag tag, HolderLookup.Provider registros) {
			Modo m = new Modo();
			m.modo = tag.getInt("modo");
			return m;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putInt("modo", modo);
			return tag;
		}
	}

	public static Modo modo(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Modo.FACTORY, "dedsafio4_casino");
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("casino").requires(s -> s.hasPermission(2))
				.then(Commands.argument("tabla", IntegerArgumentType.integer(0))
						.executes(c -> {
							int tabla = IntegerArgumentType.getInteger(c, "tabla");
							if (tabla > 1) {
								c.getSource().sendFailure(Component.literal("Todavía no existe la tabla de premios " + tabla + "."));
								return 0;
							}
							Modo m = modo(c.getSource().getServer());
							m.modo = tabla;
							m.setDirty();
							c.getSource().sendSuccess(() -> Component.literal(tabla == 0
									? "Casino: sin premios (las ruedas giran pero no dan nada)."
									: "Casino: premios de la tabla " + tabla + " prendidos.").withStyle(ChatFormatting.GOLD), true);
							// Al que prendió la tabla le aparece en pantalla qué da cada figura.
							if (tabla == 1 && c.getSource().getEntity() instanceof ServerPlayer jugador) {
								net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(jugador, TablaPremiosPayload.tabla1());
							}
							return 1;
						})));
	}

	// --- Los premios ---

	/** Le da el premio directo al jugador (sin casino que lo tire; para las pruebas). */
	public static void dar(ServerPlayer jugador, String figura, int cantidad) {
		dar(jugador, figura, cantidad, null);
	}

	/**
	 * El premio (con la tabla que esté prendida): el casino lo tira por adelante, hacia el jugador.
	 * @param figura   la figura que salió repetida
	 * @param cantidad cuántas veces salió (2 o 3)
	 * @param casino   el casino que lo tira (si es null, va directo al inventario)
	 */
	public static void dar(ServerPlayer jugador, String figura, int cantidad, net.minecraft.world.entity.Entity casino) {
		if (modo(jugador.server).modo != 1 || cantidad < 2) return;
		RandomSource azar = jugador.getRandom();
		HolderLookup.RegistryLookup<Enchantment> encantamientos =
				jugador.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		List<ItemStack> opciones = cantidad >= 3 ? tres(figura, azar, encantamientos) : dos(figura, azar, encantamientos);
		if (opciones.isEmpty()) return;
		ItemStack item = opciones.get(azar.nextInt(opciones.size())).copy();
		if (casino != null) tirar(casino, jugador, item);
		else if (!jugador.getInventory().add(item)) jugador.drop(item, false);
		jugador.sendSystemMessage(Component.literal("¡Casino! " + cantidad + " × " + nombre(figura) + ": ganaste un premio.")
				.withStyle(cantidad >= 3 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
		jugador.level().playSound(null, casino != null ? casino.blockPosition() : jugador.blockPosition(),
				cantidad >= 3 ? net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP : net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
				net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
	}

	/** El casino escupe el ítem por la bandeja de adelante, con un saltito hacia el jugador. */
	private static void tirar(net.minecraft.world.entity.Entity casino, ServerPlayer jugador, ItemStack item) {
		float giro = casino.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
		double frenteX = -net.minecraft.util.Mth.sin(giro), frenteZ = net.minecraft.util.Mth.cos(giro);
		double x = casino.getX() + frenteX * 0.9, y = casino.getY() + 0.55, z = casino.getZ() + frenteZ * 0.9;
		net.minecraft.world.entity.item.ItemEntity tirado = new net.minecraft.world.entity.item.ItemEntity(casino.level(), x, y, z, item);
		double dx = jugador.getX() - x, dz = jugador.getZ() - z, largo = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
		double fuerza = Math.min(0.3, 0.08 + largo * 0.05);
		tirado.setDeltaMovement(dx / largo * fuerza, 0.25, dz / largo * fuerza);
		tirado.setPickUpDelay(10);
		casino.level().addFreshEntity(tirado);
	}

	/** Premio chico (2 figuras iguales): las opciones (sale una). Si se cambian, cambiar también TablaPremiosPayload. */
	private static List<ItemStack> dos(String figura, RandomSource azar, HolderLookup.RegistryLookup<Enchantment> enc) {
		return switch (figura) {
			case "hierro" -> List.of(new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.DIAMOND));
			case "botella de experiencia" -> List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 16));
			case "pechera de hierro" -> List.of(armaduraDeDiamante(azar, enc, 1, 1));
			case "pico de hierro" -> List.of(new ItemStack(Items.ARROW, 16), new ItemStack(Items.SHIELD));
			case "libro de encantamientos" -> List.of(libro(azar, enc, 1, 2));
			case "filete" -> List.of(new ItemStack(Items.COOKED_BEEF, 16), new ItemStack(Items.APPLE, 16),
					new ItemStack(Items.COOKED_PORKCHOP, 16));
			case "poción" -> pociones(Items.POTION);
			default -> List.of();   // 2 corazones: nada
		};
	}

	/** Premio grande (3 figuras iguales): las opciones (sale una). */
	private static List<ItemStack> tres(String figura, RandomSource azar, HolderLookup.RegistryLookup<Enchantment> enc) {
		return switch (figura) {
			case "hierro" -> List.of(new ItemStack(Items.DIAMOND, 5), new ItemStack(Items.NETHERITE_SCRAP));
			case "botella de experiencia" -> List.of(new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
			case "pechera de hierro" -> List.of(armaduraDeDiamante(azar, enc, 3, 4));
			case "pico de hierro" -> List.of(encantado(new ItemStack(Items.CROSSBOW), azar, enc, 1, 2),
					encantado(new ItemStack(Items.BOW), azar, enc, 1, 2),
					encantado(new ItemStack(Items.DIAMOND_PICKAXE), azar, enc, 1, 2));
			case "libro de encantamientos" -> List.of(libro(azar, enc, 3, 4));
			case "filete" -> List.of(new ItemStack(Items.GOLDEN_CARROT, 10), new ItemStack(Items.GOLDEN_APPLE));
			case "poción" -> pociones(Items.SPLASH_POTION);
			case "corazon" -> List.of(new ItemStack(ModItems.CORAZON));
			default -> List.of();
		};
	}

	private static String nombre(String figura) {
		return switch (figura) {
			case "hierro" -> "Hierro";
			case "botella de experiencia" -> "Botella de experiencia";
			case "pechera de hierro" -> "Pechera de hierro";
			case "pico de hierro" -> "Pico de hierro";
			case "libro de encantamientos" -> "Libro encantado";
			case "filete" -> "Filete";
			case "poción" -> "Poción";
			case "corazon" -> "Corazón";
			default -> figura;
		};
	}

	/** Curación instantánea, resistencia al fuego o fuerza (nivel 1), para tomar o tirables. */
	private static List<ItemStack> pociones(Item tipo) {
		return List.of(PotionContents.createItemStack(tipo, Potions.HEALING),
				PotionContents.createItemStack(tipo, Potions.FIRE_RESISTANCE),
				PotionContents.createItemStack(tipo, Potions.STRENGTH));
	}

	/**
	 * Una pieza de armadura de diamante al azar con un encantamiento de armadura (no irrompibilidad ni reparación, que
	 * son de todo) de nivel entre "desde" y "hasta" (solo los que llegan a "desde").
	 */
	private static ItemStack armaduraDeDiamante(RandomSource azar, HolderLookup.RegistryLookup<Enchantment> enc, int desde, int hasta) {
		Item[] piezas = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
		ItemStack pieza = new ItemStack(piezas[azar.nextInt(piezas.length)]);
		List<Holder.Reference<Enchantment>> opciones = new ArrayList<>();
		enc.listElements().forEach(e -> {
			if (e.value().canEnchant(pieza) && !e.is(EnchantmentTags.CURSE) && !e.is(Enchantments.UNBREAKING)
					&& !e.is(Enchantments.MENDING) && e.value().getMaxLevel() >= desde) opciones.add(e);
		});
		if (!opciones.isEmpty()) {
			Holder.Reference<Enchantment> e = opciones.get(azar.nextInt(opciones.size()));
			pieza.enchant(e, nivel(azar, e, desde, hasta));
		}
		return pieza;
	}

	/** Un ítem con un encantamiento al azar de los que le sirven (sin maldiciones), de nivel "desde" a "hasta". */
	private static ItemStack encantado(ItemStack item, RandomSource azar, HolderLookup.RegistryLookup<Enchantment> enc, int desde, int hasta) {
		List<Holder.Reference<Enchantment>> opciones = new ArrayList<>();
		enc.listElements().forEach(e -> {
			if (e.value().canEnchant(item) && !e.is(EnchantmentTags.CURSE)) opciones.add(e);
		});
		if (!opciones.isEmpty()) {
			Holder.Reference<Enchantment> e = opciones.get(azar.nextInt(opciones.size()));
			item.enchant(e, nivel(azar, e, desde, hasta));
		}
		return item;
	}

	/** Un libro encantado con cualquier encantamiento menos reparación (y sin maldiciones). */
	private static ItemStack libro(RandomSource azar, HolderLookup.RegistryLookup<Enchantment> enc, int desde, int hasta) {
		List<Holder.Reference<Enchantment>> opciones = new ArrayList<>();
		enc.listElements().forEach(e -> {
			if (!e.is(Enchantments.MENDING) && !e.is(EnchantmentTags.CURSE) && e.value().getMaxLevel() >= desde) opciones.add(e);
		});
		if (opciones.isEmpty()) return new ItemStack(Items.BOOK);
		Holder.Reference<Enchantment> e = opciones.get(azar.nextInt(opciones.size()));
		return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e, nivel(azar, e, desde, hasta)));
	}

	/** Un nivel entre "desde" y "hasta", sin pasarse del máximo del encantamiento. */
	private static int nivel(RandomSource azar, Holder<Enchantment> e, int desde, int hasta) {
		int max = Math.min(hasta, e.value().getMaxLevel());
		int min = Math.min(desde, max);
		return min + azar.nextInt(max - min + 1);
	}
}
