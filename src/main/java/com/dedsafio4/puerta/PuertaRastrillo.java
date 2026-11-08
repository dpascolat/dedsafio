package com.dedsafio4.puerta;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La Puerta Rastrillo: una reja de castillo de 19 de ancho × 35 de alto, de piedra negra pulida (barras verticales
 * cada 3 bloques y horizontales en las filas 0, 4, 8... 32 y 34). Se abre subiendo 32 bloques en 3 segundos (y se
 * cierra bajando): cerrada y abierta son bloques de verdad; mientras se mueve, los bloques se sacan y se dibuja la
 * reja subiendo suave (ver PuertaEntity), y al llegar se vuelven a poner arriba (o abajo).
 * <ul>
 * <li>/puerta colocar &lt;nombre&gt;: la arma cerrada desde donde estás parado, hacia tu derecha y para arriba.</li>
 * <li>/puerta abrir [nombre] y /puerta cerrar [nombre]: sin nombre, la más cercana.</li>
 * <li>/puerta quitar &lt;nombre&gt; y /puerta lista.</li>
 * </ul>
 * Al subir no pisa bloques que haya arriba: donde hay algo, esa parte de la reja no se pone.
 */
public final class PuertaRastrillo {
	private PuertaRastrillo() {}

	public static final int ANCHO = 19, ALTO = 35, SUBE = 32, DURA = 60;
	public static final BlockState BLOQUE = Blocks.POLISHED_BLACKSTONE.defaultBlockState();

	public static final EntityType<PuertaEntity> PUERTA = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("puerta_rastrillo"),
			EntityType.Builder.<PuertaEntity>of(PuertaEntity::new, MobCategory.MISC).sized(1f, 1f).noSummon().fireImmune()
					.clientTrackingRange(16).updateInterval(20).build(id("puerta_rastrillo").toString()));

	public static void registrar() {
	}

	/** Si en esa celda (i de 0 a 18, j de 0 a 34) hay reja. */
	public static boolean hayReja(int i, int j) {
		return i % 3 == 0 || (j % 4 == 0 && j <= 32) || j == ALTO - 1;
	}

	/** Las celdas con reja: {i, j}. */
	public static List<int[]> celdas() {
		List<int[]> c = new ArrayList<>();
		for (int i = 0; i < ANCHO; i++) for (int j = 0; j < ALTO; j++) if (hayReja(i, j)) c.add(new int[]{i, j});
		return c;
	}

	// --- Lo guardado ---

	static final class Puerta {
		String nombre;
		ResourceKey<Level> mundo;
		BlockPos origen;
		Direction derecha;
		boolean abierta, moviendo;

		BlockPos celda(int i, int j, boolean arriba) {
			return origen.relative(derecha, i).above(j + (arriba ? SUBE : 0));
		}
	}

	public static final class Datos extends SavedData {
		public static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::leer, null);
		final Map<String, Puerta> puertas = new LinkedHashMap<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (String k : tag.getAllKeys()) {
				CompoundTag t = tag.getCompound(k);
				Puerta p = new Puerta();
				p.nombre = k;
				p.mundo = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("mundo")));
				p.origen = new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z"));
				p.derecha = Direction.from3DDataValue(t.getInt("derecha"));
				p.abierta = t.getBoolean("abierta");
				p.moviendo = t.getBoolean("moviendo");
				d.puertas.put(k, p);
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			puertas.forEach((k, p) -> {
				CompoundTag t = new CompoundTag();
				t.putString("mundo", p.mundo.location().toString());
				t.putInt("x", p.origen.getX());
				t.putInt("y", p.origen.getY());
				t.putInt("z", p.origen.getZ());
				t.putInt("derecha", p.derecha.get3DDataValue());
				t.putBoolean("abierta", p.abierta);
				t.putBoolean("moviendo", p.moviendo);
				tag.put(k, t);
			});
			return tag;
		}
	}

	static Datos datos(MinecraftServer servidor) {
		return servidor.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_puertas");
	}

	// --- Mover ---

	/** Empieza a abrir (subir = true) o cerrar la puerta: saca los bloques y pone la reja que se mueve. */
	private static String mover(ServerLevel mundo, Puerta p, boolean subir) {
		if (p.moviendo) return "La puerta " + p.nombre + " ya se está moviendo.";
		if (p.abierta == subir) return "La puerta " + p.nombre + (subir ? " ya está abierta." : " ya está cerrada.");
		for (int[] c : celdas()) {
			BlockPos pos = p.celda(c[0], c[1], !subir);
			if (mundo.getBlockState(pos).is(BLOQUE.getBlock())) mundo.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		PuertaEntity e = PUERTA.create(mundo);
		if (e == null) return "No se pudo mover la puerta.";
		e.preparar(p.nombre, p.origen, p.derecha, subir, mundo.getGameTime());
		mundo.addFreshEntity(e);
		p.moviendo = true;
		datos(mundo.getServer()).setDirty();
		Vec3 medio = Vec3.atCenterOf(p.celda(ANCHO / 2, ALTO / 2, !subir));
		mundo.playSound(null, medio.x, medio.y, medio.z, subir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 4f, 0.5f);
		return null;
	}

	/** Lo llama la reja que se mueve al llegar: pone los bloques donde quedó. */
	static void llego(ServerLevel mundo, String nombre, boolean subio) {
		Datos d = datos(mundo.getServer());
		Puerta p = d.puertas.get(nombre);
		if (p == null) return;
		for (int[] c : celdas()) {
			BlockPos pos = p.celda(c[0], c[1], subio);
			if (mundo.getBlockState(pos).isAir() || mundo.getBlockState(pos).canBeReplaced()) mundo.setBlock(pos, BLOQUE, Block.UPDATE_ALL);
		}
		p.abierta = subio;
		p.moviendo = false;
		d.setDirty();
		Vec3 medio = Vec3.atCenterOf(p.celda(ANCHO / 2, subio ? ALTO - 1 : 0, subio));
		mundo.playSound(null, medio.x, medio.y, medio.z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.5f, 0.5f);
	}

	// --- Comandos ---

	private static final SuggestionProvider<CommandSourceStack> NOMBRES = (c, b) ->
			SharedSuggestionProvider.suggest(datos(c.getSource().getServer()).puertas.keySet(), b);

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("puerta").requires(s -> s.hasPermission(2))
				.then(Commands.literal("colocar").then(Commands.argument("nombre", StringArgumentType.word()).executes(c -> colocar(c.getSource(), StringArgumentType.getString(c, "nombre")))))
				.then(Commands.literal("abrir").executes(c -> abrirCerrar(c.getSource(), null, true)).then(nombre().executes(c -> abrirCerrar(c.getSource(), StringArgumentType.getString(c, "nombre"), true))))
				.then(Commands.literal("cerrar").executes(c -> abrirCerrar(c.getSource(), null, false)).then(nombre().executes(c -> abrirCerrar(c.getSource(), StringArgumentType.getString(c, "nombre"), false))))
				.then(Commands.literal("quitar").then(nombre().executes(c -> quitar(c.getSource(), StringArgumentType.getString(c, "nombre")))))
				.then(Commands.literal("lista").executes(c -> lista(c.getSource()))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> nombre() {
		return Commands.argument("nombre", StringArgumentType.word()).suggests(NOMBRES);
	}

	private static void decir(CommandSourceStack fuente, String texto, boolean todos) {
		fuente.sendSuccess(() -> Component.literal(texto).withStyle(ChatFormatting.GRAY), todos);
	}

	private static int colocar(CommandSourceStack fuente, String nombre) {
		Datos d = datos(fuente.getServer());
		if (d.puertas.containsKey(nombre)) {
			fuente.sendFailure(Component.literal("Ya hay una puerta que se llama " + nombre + "."));
			return 0;
		}
		ServerLevel mundo = fuente.getLevel();
		Puerta p = new Puerta();
		p.nombre = nombre;
		p.mundo = mundo.dimension();
		p.origen = BlockPos.containing(fuente.getPosition());
		p.derecha = Direction.fromYRot(fuente.getRotation().y).getClockWise();
		for (int[] c : celdas()) mundo.setBlock(p.celda(c[0], c[1], false), BLOQUE, Block.UPDATE_ALL);
		d.puertas.put(nombre, p);
		d.setDirty();
		decir(fuente, "Puerta " + nombre + " colocada (19 × 35, cerrada). Ábrela con /puerta abrir " + nombre + ".", true);
		return 1;
	}

	private static int abrirCerrar(CommandSourceStack fuente, String nombre, boolean abrir) {
		Puerta p = nombre != null ? datos(fuente.getServer()).puertas.get(nombre) : masCercana(fuente);
		if (p == null) {
			fuente.sendFailure(Component.literal(nombre != null ? "No hay ninguna puerta que se llame " + nombre + "." : "No hay ninguna puerta cerca (hasta 96 bloques)."));
			return 0;
		}
		ServerLevel mundo = fuente.getServer().getLevel(p.mundo);
		if (mundo == null) return 0;
		String error = mover(mundo, p, abrir);
		if (error != null) {
			fuente.sendFailure(Component.literal(error));
			return 0;
		}
		decir(fuente, (abrir ? "Abriendo" : "Cerrando") + " la puerta " + p.nombre + "...", true);
		return 1;
	}

	private static Puerta masCercana(CommandSourceStack fuente) {
		Puerta mejor = null;
		double d = 96 * 96;
		for (Puerta p : datos(fuente.getServer()).puertas.values()) {
			if (p.mundo != fuente.getLevel().dimension()) continue;
			double dd = Vec3.atCenterOf(p.celda(ANCHO / 2, 0, false)).distanceToSqr(fuente.getPosition());
			if (dd < d) {
				d = dd;
				mejor = p;
			}
		}
		return mejor;
	}

	private static int quitar(CommandSourceStack fuente, String nombre) {
		Datos d = datos(fuente.getServer());
		Puerta p = d.puertas.remove(nombre);
		if (p == null) {
			fuente.sendFailure(Component.literal("No hay ninguna puerta que se llame " + nombre + "."));
			return 0;
		}
		ServerLevel mundo = fuente.getServer().getLevel(p.mundo);
		if (mundo != null) {
			for (int[] c : celdas()) {
				for (boolean arriba : new boolean[]{false, true}) {
					BlockPos pos = p.celda(c[0], c[1], arriba);
					if (mundo.getBlockState(pos).is(BLOQUE.getBlock())) mundo.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			for (PuertaEntity e : mundo.getEntities(PUERTA, e -> nombre.equals(e.nombre()))) e.discard();
		}
		d.setDirty();
		decir(fuente, "Puerta " + nombre + " quitada.", true);
		return 1;
	}

	private static int lista(CommandSourceStack fuente) {
		Map<String, Puerta> puertas = datos(fuente.getServer()).puertas;
		if (puertas.isEmpty()) {
			decir(fuente, "No hay puertas. Pon una con /puerta colocar <nombre>.", false);
			return 0;
		}
		for (Puerta p : puertas.values()) {
			decir(fuente, "• " + p.nombre + ": " + (p.moviendo ? "moviéndose" : p.abierta ? "abierta" : "cerrada") + " en "
					+ p.origen.getX() + " " + p.origen.getY() + " " + p.origen.getZ() + " (" + p.mundo.location().getPath() + ")", false);
		}
		return puertas.size();
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}
}
