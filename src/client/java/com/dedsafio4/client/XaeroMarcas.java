package com.dedsafio4.client;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.marcas.MarcasPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Las marcas de /marca como waypoints de Xaero's Minimap (como hace Xaero con los de Waystones: waypoints "de
 * otro mod", que se ven en el mundo y en el minimapa, en la dimensión de cada marca). Va todo por reflexión, así el
 * mod anda igual si alguien no tiene Xaero (en ese caso las marcas las dibuja MarcasCliente, como antes).
 */
public final class XaeroMarcas {
	private XaeroMarcas() {}

	private static final ResourceLocation ORIGEN = ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "marcas");

	/** null: todavía no se probó si está Xaero. */
	private static Boolean hayXaero;
	private static List<MarcasPayload.Marca> marcas = List.of();
	/** Hay que volver a cargarlas en Xaero (llegaron marcas nuevas, o Xaero cambió de mundo). */
	private static boolean pendiente;
	/** La carpeta de Xaero donde se cargaron por última vez (si cambia, se cargan de nuevo). */
	private static Object raizCargada;

	/** Si está Xaero's Minimap: entonces las marcas van como waypoints suyos. */
	public static boolean activo() {
		if (hayXaero == null) {
			try {
				Class.forName("xaero.hud.minimap.BuiltInHudModules");
				Class.forName("xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints");
				hayXaero = true;
			} catch (Throwable e) {
				hayXaero = false;
			}
		}
		return hayXaero;
	}

	public static void poner(List<MarcasPayload.Marca> nuevas) {
		marcas = List.copyOf(nuevas);
		pendiente = true;
	}

	/** Cada segundo: si hay que cargarlas (o Xaero cambió de mundo), las carga. */
	public static void tick(Minecraft mc) {
		if (!activo() || mc.level == null || mc.level.getGameTime() % 20 != 0) return;
		try {
			Object raiz = raiz();
			if (raiz == null) return;   // Xaero todavía no arrancó en este mundo
			if (!pendiente && raiz == raizCargada) return;
			cargar(raiz);
			raizCargada = raiz;
			pendiente = false;
		} catch (Throwable e) {
			Dedsafio4.LOGGER.warn("No se pudieron poner las marcas en Xaero's Minimap", e);
			hayXaero = false;   // otra versión de Xaero: se dibujan como antes
		}
	}

	private static Object sesion() throws Exception {
		Object modulo = Class.forName("xaero.hud.minimap.BuiltInHudModules").getField("MINIMAP").get(null);
		return metodo(modulo, "getCurrentSession").invoke(modulo);
	}

	private static Object raiz() throws Exception {
		Object sesion = sesion();
		if (sesion == null) return null;
		Object mundos = metodo(sesion, "getWorldManager").invoke(sesion);
		return mundos == null ? null : metodo(mundos, "getAutoRootContainer").invoke(mundos);
	}

	private static void cargar(Object raiz) throws Exception {
		// Primero se borran las de antes (de todas las dimensiones).
		for (Object sub : (Iterable<?>) metodo(raiz, "getSubContainers").invoke(raiz)) {
			Object gestor = metodo(sub, "getThirdPartyWaypointManager").invoke(sub);
			metodo(gestor, "clearOrigin", ResourceLocation.class).invoke(gestor, ORIGEN);
		}
		if (marcas.isEmpty()) return;
		Object sesion = metodo(raiz, "getSession").invoke(raiz);
		Object dimensiones = metodo(sesion, "getDimensionHelper").invoke(sesion);
		Object camino = metodo(raiz, "getPath").invoke(raiz);
		Class<?> claseCamino = Class.forName("xaero.hud.path.XaeroPath");
		Class<?> claseColor = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
		Class<?> claseUso = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");
		Class<?> claseWaypoint = Class.forName("xaero.common.minimap.waypoints.Waypoint");
		Object normal = claseUso.getField("NORMAL").get(null);
		var crear = claseWaypoint.getConstructor(int.class, int.class, int.class, String.class, String.class, claseColor, claseUso);
		for (MarcasPayload.Marca m : marcas) {
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(m.dimension()));
			String carpeta = (String) metodo(dimensiones, "getDimensionDirectoryName", ResourceKey.class).invoke(dimensiones, dimension);
			Object caminoDim = claseCamino.getMethod("resolve", String.class).invoke(camino, carpeta);
			Object contenedor = metodo(raiz, "addSubContainer", claseCamino).invoke(raiz, caminoDim);
			Object gestor = metodo(contenedor, "getThirdPartyWaypointManager").invoke(contenedor);
			Object lista = metodo(gestor, "get", ResourceLocation.class).invoke(gestor, ORIGEN);
			String inicial = m.nombre().isEmpty() ? "X" : m.nombre().substring(0, 1).toUpperCase();
			Object waypoint = crear.newInstance(m.x(), m.y(), m.z(), m.nombre(), inicial, colorMasParecido(claseColor, m.color()), normal);
			metodo(lista, "add", String.class, claseWaypoint).invoke(lista, m.nombre(), waypoint);
		}
	}

	/** Para las pruebas: cuántas marcas hay cargadas en Xaero (-1 si no está o no arrancó). */
	public static int cargadas() {
		try {
			Object raiz = activo() ? raiz() : null;
			if (raiz == null) return -1;
			int n = 0;
			for (Object sub : (Iterable<?>) metodo(raiz, "getSubContainers").invoke(raiz)) {
				Object gestor = metodo(sub, "getThirdPartyWaypointManager").invoke(sub);
				Object lista = metodo(gestor, "get", ResourceLocation.class).invoke(gestor, ORIGEN);
				n += (int) metodo(lista, "getCount").invoke(lista);
			}
			return n;
		} catch (Throwable e) {
			return -1;
		}
	}

	/** El color de waypoint de Xaero que más se parece al de la marca. */
	private static Object colorMasParecido(Class<?> claseColor, int rgb) throws Exception {
		Object mejor = null;
		long mejorDist = Long.MAX_VALUE;
		Method hex = claseColor.getMethod("getHex");
		for (Object c : claseColor.getEnumConstants()) {
			int h = (int) hex.invoke(c);
			long dr = (h >> 16 & 255) - (rgb >> 16 & 255), dg = (h >> 8 & 255) - (rgb >> 8 & 255), db = (h & 255) - (rgb & 255);
			long d = dr * dr + dg * dg + db * db;
			if (d < mejorDist) {
				mejorDist = d;
				mejor = c;
			}
		}
		return mejor;
	}

	/** Un método público (buscado también en las clases de arriba) por nombre y parámetros. */
	private static Method metodo(Object objeto, String nombre, Class<?>... parametros) throws NoSuchMethodException {
		return objeto.getClass().getMethod(nombre, parametros);
	}
}
