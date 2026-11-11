package com.dedsafio4.herreria;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * /mesa_de_plantilla: recetas propias para la mesa de herrería. El comando abre una pantalla (un cofre) con 4 lugares:
 * plantilla, base, material y resultado; al cerrarla con los 4 puestos, la receta queda guardada en el mundo y anda en
 * cualquier mesa de herrería (se suma a las recetas de Minecraft y se les manda a todos los jugadores). Las cosas que
 * se pusieron vuelven al inventario. /mesa_de_plantilla lista las muestra y /mesa_de_plantilla borrar <número> saca una.
 */
public final class MesaDePlantilla {
	private MesaDePlantilla() {}

	/** Los lugares de la pantalla (cofre de 3 filas): plantilla, base, material y resultado, en la fila del medio. */
	private static final int PLANTILLA = 10, BASE = 12, MATERIAL = 14, RESULTADO = 16;
	private static final int[] LUGARES = {PLANTILLA, BASE, MATERIAL, RESULTADO};
	private static final String[] NOMBRES = {"Plantilla", "Base", "Material", "Resultado"};
	private static final String CARPETA = "mesa_plantilla/";

	/** Una receta: plantilla + base + material → resultado. */
	private record Receta(ItemStack plantilla, ItemStack base, ItemStack material, ItemStack resultado) {}

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final List<Receta> recetas = new ArrayList<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("recetas", Tag.TAG_COMPOUND)) {
				CompoundTag r = (CompoundTag) t;
				d.recetas.add(new Receta(pila(registros, r, "plantilla"), pila(registros, r, "base"), pila(registros, r, "material"),
						pila(registros, r, "resultado")));
			}
			d.recetas.removeIf(r -> r.plantilla().isEmpty() || r.base().isEmpty() || r.material().isEmpty() || r.resultado().isEmpty());
			return d;
		}

		private static ItemStack pila(HolderLookup.Provider registros, CompoundTag r, String clave) {
			return r.contains(clave) ? ItemStack.parse(registros, r.getCompound(clave)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag lista = new ListTag();
			for (Receta r : recetas) {
				CompoundTag c = new CompoundTag();
				c.put("plantilla", r.plantilla().save(registros));
				c.put("base", r.base().save(registros));
				c.put("material", r.material().save(registros));
				c.put("resultado", r.resultado().save(registros));
				lista.add(c);
			}
			tag.put("recetas", lista);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_mesa_plantilla");
	}

	public static void registrar() {
		// Al arrancar el servidor y después de un /reload Minecraft arma las recetas de nuevo: si faltan las nuestras, se ponen.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			Datos d = datos(server);
			if (!d.recetas.isEmpty() && server.getRecipeManager().byKey(id(0)).isEmpty()) cargar(server);
		});
	}

	private static ResourceLocation id(int i) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, CARPETA + i);
	}

	/** Pone nuestras recetas (y saca las viejas) en las de Minecraft, y se las manda a todos los jugadores. */
	private static void cargar(MinecraftServer server) {
		RecipeManager manager = server.getRecipeManager();
		List<RecipeHolder<?>> todas = new ArrayList<>();
		for (RecipeHolder<?> r : manager.getRecipes()) {
			if (!(r.id().getNamespace().equals(Dedsafio4.MOD_ID) && r.id().getPath().startsWith(CARPETA))) todas.add(r);
		}
		List<Receta> recetas = datos(server).recetas;
		for (int i = 0; i < recetas.size(); i++) {
			Receta r = recetas.get(i);
			todas.add(new RecipeHolder<>(id(i), new SmithingTransformRecipe(Ingredient.of(r.plantilla().getItem()),
					Ingredient.of(r.base().getItem()), Ingredient.of(r.material().getItem()), r.resultado().copy())));
		}
		manager.replaceRecipes(todas);
		server.getPlayerList().broadcastAll(new ClientboundUpdateRecipesPacket(manager.getOrderedRecipes()));
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mesa_de_plantilla").requires(s -> s.hasPermission(2))
				.executes(c -> {
					ServerPlayer jugador = c.getSource().getPlayerOrException();
					jugador.openMenu(new SimpleMenuProvider((id, inv, p) -> new Pantalla(id, inv),
							Component.literal("Mesa de Plantilla")));
					return 1;
				})
				.then(Commands.literal("lista").executes(c -> {
					List<Receta> recetas = datos(c.getSource().getServer()).recetas;
					if (recetas.isEmpty()) {
						c.getSource().sendSuccess(() -> Component.literal("No hay recetas de la Mesa de Plantilla."), false);
						return 0;
					}
					for (int i = 0; i < recetas.size(); i++) {
						Receta r = recetas.get(i);
						int n = i + 1;
						c.getSource().sendSuccess(() -> Component.literal(n + ". ").withStyle(ChatFormatting.GOLD)
								.append(nombre(r.plantilla())).append(" + ").append(nombre(r.base())).append(" + ")
								.append(nombre(r.material())).append(" → ").append(nombre(r.resultado())), false);
					}
					return recetas.size();
				}))
				.then(Commands.literal("borrar").then(Commands.argument("número", IntegerArgumentType.integer(1)).executes(c -> {
					MinecraftServer server = c.getSource().getServer();
					Datos d = datos(server);
					int n = IntegerArgumentType.getInteger(c, "número");
					if (n > d.recetas.size()) {
						c.getSource().sendFailure(Component.literal("No hay receta número " + n + " (mira /mesa_de_plantilla lista)."));
						return 0;
					}
					d.recetas.remove(n - 1);
					d.setDirty();
					cargar(server);
					c.getSource().sendSuccess(() -> Component.literal("Receta " + n + " borrada.").withStyle(ChatFormatting.GREEN), true);
					return 1;
				}))));
	}

	private static Component nombre(ItemStack pila) {
		Component n = pila.getHoverName();
		return pila.getCount() > 1 ? Component.empty().append(n).append(" x" + pila.getCount()) : n;
	}

	/** Al cerrar la pantalla con los 4 lugares puestos, guarda la receta. Las cosas vuelven al inventario siempre. */
	private static void guardar(ServerPlayer jugador, Container caja) {
		ItemStack[] p = new ItemStack[4];
		for (int i = 0; i < 4; i++) p[i] = caja.getItem(LUGARES[i]).copy();
		boolean alguno = false, todos = true;
		for (ItemStack s : p) {
			alguno |= !s.isEmpty();
			todos &= !s.isEmpty();
		}
		if (todos) {
			Datos d = datos(jugador.server);
			d.recetas.add(new Receta(p[0].copyWithCount(1), p[1].copyWithCount(1), p[2].copyWithCount(1), p[3].copy()));
			d.setDirty();
			cargar(jugador.server);
			jugador.sendSystemMessage(Component.literal("Receta guardada: ").withStyle(ChatFormatting.GREEN)
					.append(nombre(p[0])).append(" + ").append(nombre(p[1])).append(" + ").append(nombre(p[2]))
					.append(" → ").append(nombre(p[3])));
		} else if (alguno) {
			jugador.sendSystemMessage(Component.literal("No se guardó: faltan cosas (plantilla, base, material y resultado).")
					.withStyle(ChatFormatting.RED));
		}
		for (int i = 0; i < 4; i++) {
			ItemStack s = caja.removeItemNoUpdate(LUGARES[i]);
			if (!s.isEmpty()) jugador.getInventory().placeItemBackInInventory(s);
		}
	}

	/** La pantalla: un cofre de 3 filas con vidrios fijos y los 4 lugares libres (con su nombre arriba). */
	private static final class Pantalla extends ChestMenu {
		private final Container caja;

		Pantalla(int id, Inventory inventario) {
			this(id, inventario, new SimpleContainer(27));
		}

		private Pantalla(int id, Inventory inventario, Container caja) {
			super(MenuType.GENERIC_9x3, id, inventario, caja, 3);
			this.caja = caja;
			for (int i = 0; i < 27; i++) {
				if (libre(i)) continue;
				int k = i < 9 ? java.util.Arrays.binarySearch(LUGARES, i + 9) : -1;
				ItemStack vidrio = new ItemStack(k >= 0 ? Items.LIME_STAINED_GLASS_PANE : Items.GRAY_STAINED_GLASS_PANE);
				vidrio.set(DataComponents.CUSTOM_NAME, Component.literal(k >= 0 ? NOMBRES[k] + " (abajo)" : " "));
				caja.setItem(i, vidrio);
			}
		}

		private static boolean libre(int lugar) {
			for (int l : LUGARES) if (l == lugar) return true;
			return false;
		}

		@Override
		public void clicked(int lugar, int boton, ClickType tipo, Player jugador) {
			if (lugar >= 0 && lugar < 27 && !libre(lugar)) return;   // los vidrios no se tocan
			super.clicked(lugar, boton, tipo, jugador);
		}

		@Override
		public boolean canTakeItemForPickAll(ItemStack pila, Slot lugar) {
			return lugar.container != caja && super.canTakeItemForPickAll(pila, lugar);
		}

		@Override
		public ItemStack quickMoveStack(Player jugador, int lugar) {
			if (lugar < 27 && !libre(lugar)) return ItemStack.EMPTY;
			return super.quickMoveStack(jugador, lugar);
		}

		@Override
		public void removed(Player jugador) {
			if (jugador instanceof ServerPlayer j) guardar(j, caja);
			for (int i = 0; i < 27; i++) if (!libre(i)) caja.setItem(i, ItemStack.EMPTY);
			super.removed(jugador);
		}
	}
}
