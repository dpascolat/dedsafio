package com.dedsafio4.client.cinematica;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Los videos de las cinemáticas son grandes (unos 40 MB), así que no van adentro del mod: cada cliente los baja
 * una sola vez del repositorio de GitHub y los guarda en .minecraft/dedsafio4/cinematicas. Se empiezan a bajar
 * apenas abre el juego, para que cuando alguien haga /cinematica ya estén.
 */
public final class CinematicaArchivos {
	private CinematicaArchivos() {}

	private record Info(String url, long tamano, String sha1) {}

	/** Por número de cinemática (la 0 no existe). Si se cambia el video, cambiar el tamaño y el sha1. */
	private static final Info[] INFO = {null,
			new Info("https://raw.githubusercontent.com/dpascolat/dedsafio/main/cinematicas/cinematica1.dat",
					39407658L, "4f65e67af4f7ea963716a76df006c2cc866a43b2")};

	public enum Estado {FALTA, BAJANDO, LISTO, ERROR}

	private static final Estado[] ESTADO = new Estado[INFO.length];
	private static final float[] PROGRESO = new float[INFO.length];
	private static boolean empezado;

	static {
		java.util.Arrays.fill(ESTADO, Estado.FALTA);
	}

	public static void registrar() {
		// En el primer tick (ya está la carpeta del juego) se revisan y se bajan las que falten.
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (empezado) return;
			empezado = true;
			for (int i = 1; i < INFO.length; i++) asegurar(i);
		});
	}

	public static boolean existe(int numero) {
		return numero > 0 && numero < INFO.length;
	}

	public static Path archivo(int numero) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("dedsafio4").resolve("cinematicas")
				.resolve("cinematica" + numero + ".dat");
	}

	public static synchronized Estado estado(int numero) {
		return ESTADO[numero];
	}

	/** De 0 a 1, mientras se baja. */
	public static float progreso(int numero) {
		return PROGRESO[numero];
	}

	/** Si no está (o se cortó la descarga antes), la empieza a bajar en otro hilo. */
	public static synchronized void asegurar(int numero) {
		if (!existe(numero) || ESTADO[numero] == Estado.LISTO || ESTADO[numero] == Estado.BAJANDO) return;
		ESTADO[numero] = Estado.BAJANDO;
		PROGRESO[numero] = 0;
		Thread hilo = new Thread(() -> {
			Estado fin;
			try {
				fin = preparar(numero) ? Estado.LISTO : Estado.ERROR;
			} catch (Exception e) {
				Dedsafio4.LOGGER.error("No se pudo bajar la cinemática " + numero, e);
				fin = Estado.ERROR;
			}
			synchronized (CinematicaArchivos.class) {
				ESTADO[numero] = fin;
			}
		}, "Dedsafio4 cinematica " + numero);
		hilo.setDaemon(true);
		hilo.start();
	}

	private static boolean preparar(int numero) throws Exception {
		Info info = INFO[numero];
		Path destino = archivo(numero);
		if (Files.exists(destino) && Files.size(destino) == info.tamano() && info.sha1().equals(sha1(destino))) return true;
		Files.createDirectories(destino.getParent());
		Path parcial = destino.resolveSibling(destino.getFileName() + ".part");
		HttpClient cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
		HttpResponse<InputStream> respuesta = cliente.send(HttpRequest.newBuilder(URI.create(info.url())).GET().build(),
				HttpResponse.BodyHandlers.ofInputStream());
		if (respuesta.statusCode() != 200) {
			Dedsafio4.LOGGER.error("No se pudo bajar la cinemática {}: código {}", numero, respuesta.statusCode());
			return false;
		}
		try (InputStream entrada = respuesta.body(); OutputStream salida = Files.newOutputStream(parcial)) {
			byte[] buf = new byte[1 << 16];
			long total = 0;
			int n;
			while ((n = entrada.read(buf)) > 0) {
				salida.write(buf, 0, n);
				total += n;
				PROGRESO[numero] = Math.min(1f, total / (float) info.tamano());
			}
		}
		if (!info.sha1().equals(sha1(parcial))) {
			Dedsafio4.LOGGER.error("La cinemática {} se bajó mal (no coincide el sha1)", numero);
			Files.deleteIfExists(parcial);
			return false;
		}
		Files.move(parcial, destino, StandardCopyOption.REPLACE_EXISTING);
		return true;
	}

	private static String sha1(Path archivo) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		try (InputStream entrada = Files.newInputStream(archivo)) {
			byte[] buf = new byte[1 << 16];
			int n;
			while ((n = entrada.read(buf)) > 0) md.update(buf, 0, n);
		}
		return HexFormat.of().formatHex(md.digest());
	}
}
