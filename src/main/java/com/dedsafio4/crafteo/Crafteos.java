package com.dedsafio4.crafteo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.catalogo.Catalogo;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * /crafteo: abre una mesa para cambiar el crafteo de un ítem. Se pone el ítem en el resultado (aparece su crafteo de
 * ahora), se arma el nuevo en la grilla de 3×3 y "GUARDAR". Los ítems no se gastan: las casillas solo copian lo que
 * se pone. Desde ahí, ese ítem se craftea solo con el crafteo nuevo (los de antes se borran), y la G y las
 * descripciones muestran el nuevo. /crafteo quitar <ítem> vuelve al de Minecraft. Queda guardado en el mundo.
 */
public final class Crafteos {
	private Crafteos() {}

	public static final MenuType<CrafteoMenu> MENU = Registry.register(BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "crafteo"), new MenuType<>(CrafteoMenu::new, FeatureFlags.VANILLA_SET));

	/** Un crafteo cambiado: las 9 casillas (id del ítem o "" si está vacía) y cuántos da. */
	record Receta(List<String> casillas, int cantidad) {}

	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		/** id del ítem resultado → su crafteo nuevo. */
		final Map<String, Receta> recetas = new LinkedHashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			CompoundTag todas = tag.getCompound("recetas");
			for (String item : todas.getAllKeys()) {
				CompoundTag r = todas.getCompound(item);
				List<String> casillas = new ArrayList<>();
				for (Tag t : r.getList("casillas", Tag.TAG_STRING)) casillas.add(t.getAsString());
				while (casillas.size() < 9) casillas.add("");
				d.recetas.put(item, new Receta(casillas, Math.max(1, r.getInt("cantidad"))));
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			CompoundTag todas = new CompoundTag();
			recetas.forEach((item, r) -> {
				CompoundTag t = new CompoundTag();
				ListTag casillas = new ListTag();
				for (String c : r.casillas()) casillas.add(StringTag.valueOf(c));
				t.put("casillas", casillas);
				t.putInt("cantidad", r.cantidad());
				todas.put(item, t);
			});
			tag.put("recetas", todas);
			return tag;
		}
	}

	static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_crafteos");
	}

	// --- Red ---

	/** Del editor al servidor: "GUARDAR". */
	public record GuardarPayload() implements CustomPacketPayload {
		public static final Type<GuardarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "crafteo_guardar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GuardarPayload> CODEC = StreamCodec.unit(new GuardarPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Del servidor a los jugadores: cambiaron los crafteos (para que las descripciones los vuelvan a buscar). */
	public record CambiaronPayload() implements CustomPacketPayload {
		public static final Type<CambiaronPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "crafteos_cambiaron"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CambiaronPayload> CODEC = StreamCodec.unit(new CambiaronPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playC2S().register(GuardarPayload.TYPE, GuardarPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CambiaronPayload.TYPE, CambiaronPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GuardarPayload.TYPE, (payload, context) ->
				context.server().execute(() -> guardar(context.player())));
		// Después de /reload (o al arrancar) Minecraft arma de nuevo las recetas: se vuelven a poner las cambiadas.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			RecipeManager recetas = server.getRecipeManager();
			if (APLICADAS.containsKey(recetas) || datos(server).recetas.isEmpty()) return;
			aplicar(server);
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("crafteo").requires(Catalogo::puedeEditar)
				.executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					p.openMenu(new SimpleMenuProvider((id, inv, j) -> new CrafteoMenu(id, inv), Component.literal("Cambiar crafteo")));
					return 1;
				})
				.then(Commands.literal("quitar").then(Commands.argument("item", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(datos(c.getSource().getServer()).recetas.keySet(), b))
						.executes(c -> {
							String item = StringArgumentType.getString(c, "item").trim();
							MinecraftServer server = c.getSource().getServer();
							if (datos(server).recetas.remove(item) == null) {
								c.getSource().sendFailure(Component.literal("Ese ítem no tiene un crafteo cambiado."));
								return 0;
							}
							datos(server).setDirty();
							aplicar(server);
							c.getSource().sendSuccess(() -> Component.literal("El crafteo de " + item + " volvió a ser el de siempre.")
									.withStyle(ChatFormatting.GOLD), true);
							return 1;
						}))));
	}

	private static void guardar(ServerPlayer p) {
		if (!(p.containerMenu instanceof CrafteoMenu menu)) return;
		if (!Catalogo.esEditor(p)) return;
		ItemStack resultado = menu.resultado();
		if (resultado.isEmpty()) {
			p.sendSystemMessage(Component.literal("Pon en el resultado el ítem que quieres craftear.").withStyle(ChatFormatting.RED));
			return;
		}
		List<String> casillas = new ArrayList<>();
		boolean alguno = false;
		for (int i = 0; i < 9; i++) {
			ItemStack s = menu.casilla(i);
			casillas.add(s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
			alguno |= !s.isEmpty();
		}
		if (!alguno) {
			p.sendSystemMessage(Component.literal("Arma el crafteo en la grilla de 3×3.").withStyle(ChatFormatting.RED));
			return;
		}
		String item = BuiltInRegistries.ITEM.getKey(resultado.getItem()).toString();
		datos(p.server).recetas.put(item, new Receta(casillas, resultado.getCount()));
		datos(p.server).setDirty();
		aplicar(p.server);
		p.sendSystemMessage(Component.literal("Crafteo de ").withStyle(ChatFormatting.GREEN)
				.append(resultado.getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" cambiado: el de antes ya no sirve.").withStyle(ChatFormatting.GREEN)));
	}

	// --- Las recetas ---

	/** Las recetas de Minecraft (sin cambiar) de cada RecipeManager al que ya se le pusieron las cambiadas. */
	private static final Map<RecipeManager, List<RecipeHolder<?>>> APLICADAS = Collections.synchronizedMap(new WeakHashMap<>());

	/** Saca los crafteos viejos de los ítems cambiados, pone los nuevos y se los manda a todos. */
	static void aplicar(MinecraftServer server) {
		RecipeManager manager = server.getRecipeManager();
		List<RecipeHolder<?>> base = APLICADAS.computeIfAbsent(manager, m -> List.copyOf(m.getRecipes()));
		Map<String, Receta> cambiadas = datos(server).recetas;
		var registros = server.registryAccess();
		List<RecipeHolder<?>> nuevas = new ArrayList<>();
		for (RecipeHolder<?> r : base) {
			if (r.value().getType() == RecipeType.CRAFTING) {
				ItemStack da = r.value().getResultItem(registros);
				if (!da.isEmpty() && cambiadas.containsKey(BuiltInRegistries.ITEM.getKey(da.getItem()).toString())) continue;
			}
			nuevas.add(r);
		}
		cambiadas.forEach((item, receta) -> {
			RecipeHolder<CraftingRecipe> r = armar(item, receta);
			if (r != null) nuevas.add(r);
		});
		manager.replaceRecipes(nuevas);
		ClientboundUpdateRecipesPacket paquete = new ClientboundUpdateRecipesPacket(manager.getOrderedRecipes());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.connection.send(paquete);
			ServerPlayNetworking.send(p, new CambiaronPayload());
		}
	}

	/** El crafteo con forma (como está en la grilla, sin las filas y columnas vacías de los costados). */
	private static RecipeHolder<CraftingRecipe> armar(String itemId, Receta receta) {
		Item resultado = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
		if (resultado == Items.AIR) return null;
		int minX = 3, maxX = -1, minY = 3, maxY = -1;
		for (int i = 0; i < 9; i++) {
			if (receta.casillas().get(i).isEmpty()) continue;
			minX = Math.min(minX, i % 3);
			maxX = Math.max(maxX, i % 3);
			minY = Math.min(minY, i / 3);
			maxY = Math.max(maxY, i / 3);
		}
		if (maxX < 0) return null;
		Map<String, Character> letras = new HashMap<>();
		Map<Character, Ingredient> clave = new HashMap<>();
		List<String> filas = new ArrayList<>();
		for (int y = minY; y <= maxY; y++) {
			StringBuilder fila = new StringBuilder();
			for (int x = minX; x <= maxX; x++) {
				String id = receta.casillas().get(y * 3 + x);
				Item ing = id.isEmpty() ? Items.AIR : BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
				if (ing == Items.AIR) {
					fila.append(' ');
					continue;
				}
				char letra = letras.computeIfAbsent(id, k -> (char) ('A' + letras.size()));
				clave.put(letra, Ingredient.of(ing));
				fila.append(letra);
			}
			filas.add(fila.toString());
		}
		ShapedRecipe forma = new ShapedRecipe("", CraftingBookCategory.MISC, ShapedRecipePattern.of(clave, filas),
				new ItemStack(resultado, receta.cantidad()));
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "crafteo/" + itemId.replace(':', '_'));
		return new RecipeHolder<>(id, forma);
	}

	/** Los ítems cambiados (para avisar en el editor). */
	public static Set<String> cambiados(MinecraftServer server) {
		return datos(server).recetas.keySet();
	}
}
