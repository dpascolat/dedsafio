package com.dedsafio4.catalogo;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.items.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las Deditas de la Misión que reparten los OP a mano (sin una Misión Principal):
 *   /deditas_de_mision            abre el premio (el mismo editor de 3×9 que el de las misiones) para elegir qué da;
 *   /deditas_de_mision entregar   le da una Dedita de la Misión a cada jugador que esté a 300 bloques o menos;
 *   /deditas_de_mision obtener    te da una a ti.
 * (También anda como /dedita_de_mision.) La dedita se canjea en el Pedestal de la Montaña como las otras y da el premio
 * que estaba puesto cuando se repartió (si después se cambia el premio, las que ya se dieron siguen dando el de antes).
 */
public final class DeditasMision {
	private DeditasMision() {}

	private static final double DISTANCIA = 300;

	// --- Guardado: el premio de ahora y el de cada tanda que se repartió ---

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		List<ItemStack> premio = new ArrayList<>();
		/** Cada tanda repartida: su número y el premio que tenía. */
		final Map<Integer, List<ItemStack>> tandas = new HashMap<>();
		int ultima = 0;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.premio = leerPremio(tag.getList("premio", Tag.TAG_COMPOUND), registros);
			CompoundTag t = tag.getCompound("tandas");
			for (String k : t.getAllKeys()) d.tandas.put(Integer.parseInt(k), leerPremio(t.getList(k, Tag.TAG_COMPOUND), registros));
			d.ultima = tag.getInt("ultima");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.put("premio", guardarPremio(premio, registros));
			CompoundTag t = new CompoundTag();
			tandas.forEach((k, p) -> t.put(String.valueOf(k), guardarPremio(p, registros)));
			tag.put("tandas", t);
			tag.putInt("ultima", ultima);
			return tag;
		}

		/** El número de la tanda con el premio de ahora (si cambió desde la última, una nueva). */
		int tandaActual() {
			List<ItemStack> antes = tandas.get(ultima);
			if (antes == null || !iguales(antes, premio)) {
				ultima++;
				tandas.put(ultima, copiar(premio));
				setDirty();
			}
			return ultima;
		}
	}

	private static ListTag guardarPremio(List<ItemStack> premio, HolderLookup.Provider registros) {
		ListTag l = new ListTag();
		for (int i = 0; i < premio.size(); i++) {
			if (premio.get(i).isEmpty()) continue;
			CompoundTag casilla = new CompoundTag();
			casilla.putByte("Slot", (byte) i);
			l.add(premio.get(i).save(registros, casilla));
		}
		return l;
	}

	private static List<ItemStack> leerPremio(ListTag l, HolderLookup.Provider registros) {
		List<ItemStack> premio = new ArrayList<>();
		for (Tag o : l) {
			var pila = ItemStack.parse(registros, o);
			if (pila.isEmpty()) continue;
			int casilla = ((CompoundTag) o).getByte("Slot") & 0xFF;
			if (casilla >= Misiones.MAX_PREMIO) continue;
			while (premio.size() <= casilla) premio.add(ItemStack.EMPTY);
			premio.set(casilla, pila.get());
		}
		return premio;
	}

	private static List<ItemStack> copiar(List<ItemStack> l) {
		List<ItemStack> c = new ArrayList<>();
		for (ItemStack p : l) c.add(p.copy());
		return c;
	}

	private static boolean iguales(List<ItemStack> a, List<ItemStack> b) {
		int n = Math.max(a.size(), b.size());
		for (int i = 0; i < n; i++) {
			ItemStack x = i < a.size() ? a.get(i) : ItemStack.EMPTY, y = i < b.size() ? b.get(i) : ItemStack.EMPTY;
			if (!ItemStack.matches(x, y)) return false;
		}
		return true;
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_deditas_mision");
	}

	/** El premio de una Dedita de la Misión repartida con /deditas_de_mision, o null si no es una de esas. */
	public static List<ItemStack> premioDe(MinecraftServer server, ItemStack dedita) {
		CustomData d = dedita.get(DataComponents.CUSTOM_DATA);
		if (!dedita.is(ModItems.DEDITA_MISION) || d == null || !d.copyTag().contains("tanda")) return null;
		return datos(server).tandas.getOrDefault(d.copyTag().getInt("tanda"), List.of());
	}

	private static ItemStack dedita(MinecraftServer server) {
		ItemStack pila = new ItemStack(ModItems.DEDITA_MISION);
		CompoundTag tag = new CompoundTag();
		tag.putInt("tanda", datos(server).tandaActual());
		pila.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return pila;
	}

	private static void dar(ServerPlayer p, ItemStack pila) {
		if (!p.getInventory().add(pila)) p.drop(pila, false);
		p.displayClientMessage(Component.literal("¡Recibiste una ").withColor(0x7CFC6A).append(Component.literal("Dedita de la Misión").withColor(0x55D9F0))
				.append(Component.literal("! Canjéala en el Pedestal de la Montaña.").withColor(0x7CFC6A)), false);
		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	// --- Red: abrir el editor del premio y guardarlo ---

	/** Al OP: abre el editor con el premio de ahora. */
	public record AbrirPayload(List<ItemStack> premio) implements CustomPacketPayload {
		public static final Type<AbrirPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "deditas_mision_abrir"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AbrirPayload> CODEC =
				ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(AbrirPayload::new, AbrirPayload::premio);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Del OP: el premio nuevo (al tocar "Listo"). */
	public record GuardarPayload(List<ItemStack> premio) implements CustomPacketPayload {
		public static final Type<GuardarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "deditas_mision_guardar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, GuardarPayload> CODEC =
				ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(GuardarPayload::new, GuardarPayload::premio);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(AbrirPayload.TYPE, AbrirPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(GuardarPayload.TYPE, GuardarPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GuardarPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			ServerPlayer p = context.player();
			if (!p.hasPermissions(2)) return;
			Datos d = datos(context.server());
			List<ItemStack> premio = new ArrayList<>(payload.premio());
			while (premio.size() > Misiones.MAX_PREMIO) premio.remove(premio.size() - 1);
			d.premio = premio;
			d.setDirty();
			long cosas = premio.stream().filter(s -> !s.isEmpty()).count();
			p.displayClientMessage(Component.literal(cosas == 0 ? "Listo: la Dedita de la Misión no da premio."
					: "Listo: el premio de la Dedita de la Misión tiene " + cosas + (cosas == 1 ? " cosa." : " cosas.")).withStyle(ChatFormatting.GREEN), false);
		}));
	}

	// --- Comandos (sólo OP) ---

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		var principal = dispatcher.register(arbol("deditas_de_mision"));
		dispatcher.register(Commands.literal("dedita_de_mision").requires(s -> s.hasPermission(2)).redirect(principal)
				.executes(c -> abrir(c.getSource())));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> arbol(String nombre) {
		return Commands.literal(nombre).requires(s -> s.hasPermission(2))
				.executes(c -> abrir(c.getSource()))
				.then(Commands.literal("entregar").executes(c -> entregar(c.getSource())))
				.then(Commands.literal("obtener").executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					dar(p, dedita(c.getSource().getServer()));
					return 1;
				}));
	}

	private static int abrir(CommandSourceStack fuente) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = fuente.getPlayerOrException();
		ServerPlayNetworking.send(p, new AbrirPayload(copiar(datos(fuente.getServer()).premio)));
		return 1;
	}

	/** Una Dedita de la Misión a cada jugador a 300 bloques o menos (en la misma dimensión; el que lo manda, no). */
	private static int entregar(CommandSourceStack fuente) {
		MinecraftServer server = fuente.getServer();
		ServerPlayer yo = fuente.getPlayer();
		List<String> nombres = new ArrayList<>();
		for (ServerPlayer p : fuente.getLevel().players()) {
			if (p == yo || p.isSpectator() || p.position().distanceTo(fuente.getPosition()) > DISTANCIA) continue;
			dar(p, dedita(server));
			nombres.add(p.getGameProfile().getName());
		}
		if (nombres.isEmpty()) {
			fuente.sendFailure(Component.literal("No hay nadie a 300 bloques o menos."));
			return 0;
		}
		fuente.sendSuccess(() -> Component.literal("Le diste una Dedita de la Misión a " + nombres.size()
				+ (nombres.size() == 1 ? " jugador: " : " jugadores: ") + String.join(", ", nombres) + ".").withStyle(ChatFormatting.GREEN), true);
		return nombres.size();
	}
}
