package com.dedsafio4.neocompat.loader.api;

import java.nio.file.Path;
import java.util.Optional;

public interface ModContainer {
	Optional<Path> findPath(String ruta);
}
