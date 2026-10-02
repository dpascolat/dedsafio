package com.dedsafio4.neocompat.loader.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Lo poco que usa el mod de FabricLoader: buscar un archivo adentro del jar del mod. */
public final class FabricLoader {
	private static final FabricLoader INSTANCIA = new FabricLoader();

	public static FabricLoader getInstance() {
		return INSTANCIA;
	}

	public Optional<ModContainer> getModContainer(String id) {
		var archivo = net.neoforged.fml.ModList.get().getModFileById(id);
		if (archivo == null) return Optional.empty();
		return Optional.of(ruta -> {
			Path p = archivo.getFile().findResource(ruta);
			return p != null && Files.exists(p) ? Optional.of(p) : Optional.empty();
		});
	}

	public boolean isModLoaded(String id) {
		return net.neoforged.fml.ModList.get().isLoaded(id);
	}
}
