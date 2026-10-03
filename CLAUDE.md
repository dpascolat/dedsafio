# Dedsafío 4 — notas para Claude Code

Mod de Minecraft **1.21.1** de Patricio (habla español, no es programador: explicarle todo simple y en español).
El código, los comentarios y los nombres están en español; seguir ese estilo.

## Qué hay en el repo

| Carpeta / archivo | Qué es |
|---|---|
| `src/main/java` | Código común (servidor y cliente) del mod. Paquete `com.dedsafio4`. |
| `src/client/java` | Código sólo del cliente (renderers, pantallas, mixins de cliente). |
| `src/main/resources` | Texturas, modelos, idiomas (`assets/dedsafio4/lang/es_es.json` y `en_us.json`), datapack (`data/dedsafio4`), `fabric.mod.json`, mixins. |
| `neoforge/` | La versión **NeoForge**. NO tiene una copia del código: al compilar copia `src/` y cambia `net.fabricmc.` por `com.dedsafio4.neocompat.` (ver más abajo). |
| `dedsafio4.jar` | El mod compilado para **Fabric** (lo actualiza GitHub Actions solo). |
| `dedsafio4-neoforge.jar` | El mod compilado para **NeoForge** (lo actualiza GitHub Actions solo). |
| `dedsafio.json` / `dedsafio-neoforge.json` | Listas de mods (con url y sha1) que usa el launcher de Patricio ("SAO Estudios") para bajar el modpack. |

## Versiones

- Fabric: loader 0.19.3, Fabric API 0.116.15+1.21.1, Loom (mappings oficiales de Mojang).
- NeoForge: 21.1.252, ModDevGradle 2.0.148.
- GeckoLib 4.9.2 (`geckolib-fabric-1.21.1` / `geckolib-neoforge-1.21.1`) para los mobs animados.
- Xaero's Minimap 26.5.0 (el mod lo bloquea con códigos de chat en algunos momentos).
- Java 21.

## Cómo se compila

- **Fabric:** `./gradlew build` → `build/libs/dedsafio4-0.1.0.jar`.
- **NeoForge:** `./gradlew -p neoforge build` → `neoforge/build/libs/dedsafio4-neoforge-0.1.0.jar`.
- En GitHub Actions (`.github/workflows/`):
  - `compilar.yml`: con cada push a `src/**` compila Fabric y commitea `dedsafio4.jar` + sha1 en `dedsafio.json`.
  - `compilar-neoforge.yml`: con cada push a `src/**` o `neoforge/**` compila NeoForge y commitea `dedsafio4-neoforge.jar` + sha1 en `dedsafio-neoforge.json`.
  - `probar-neoforge.yml` (a mano): arranca servidor NeoForge, conecta un cliente con pantalla virtual, saca capturas y sube los logs a la rama `diagnostico-neoforge`.
  - `mods-neoforge.yml` (a mano): baja los mods de NeoForge y los deja en la rama `mods-neoforge`.
- Después de un push, esperar el commit "Actualizar dedsafio4.jar (compilado automáticamente)" y hacer `git pull` antes de seguir (si no, hay conflicto con el jar).
- Patricio trabaja directo en `main`.

## Versión NeoForge (capa compatible)

`neoforge/src/main/java/com/dedsafio4/neocompat/` imita la parte de Fabric API que usa el mod (mismos nombres de
clases y métodos, en `com.dedsafio4.neocompat.fabric.api...`) pero por dentro usa eventos de NeoForge:

- `Puente.java`: arranca el mod en el `RegisterEvent` de atributos (antes no se pueden armar los atributos de los
  bichos) y pasa cada evento de NeoForge a los oyentes estilo Fabric. `PuenteCliente.java`: lo mismo en el cliente.
- Red: los payloads se anotan en `PayloadTypeRegistry` y se registran en `RegisterPayloadHandlersEvent`.
- `NeoComun` / `NeoCompat`: reemplazos de cosas de Minecraft que en NeoForge son privadas
  (`SpawnPlacements.register`, `MenuScreens.register`); el `build.gradle` de neoforge cambia esas llamadas al copiar.
- Mixins propios (`neocompat/mixin`): mensajes del juego, después de romper un bloque, cielo/nubes/clima por
  dimensión, golpe de la enderperla.
- `AjustesNeoForge.java`: "respirar agua" usa `LivingBreatheEvent` (NeoForge cambió esa parte de Minecraft).
- En NeoForge los mixins del mod no son obligatorios (`defaultRequire` 0): si uno no encaja, `DiagnosticoMixins`
  lo avisa en el log como "INYECCIÓN FALLIDA" en vez de cerrar el juego.
- Si se usa algo nuevo de Fabric API en `src/`, hay que agregarlo a la capa (si no, la versión NeoForge no compila).

## Cosas grandes del mod (para ubicarse)

- Dimensión de los Órganos (`dimension/`, datapack): piso de carne, árboles de gelatina, agua rosa que lastima,
  pasto rojo oscuro de bloque y medio.
- Plataforma de Despegue y Nave Biplaza (`despegue/`): cuenta regresiva, subida animada, viaje por la dimensión
  Espacio (planetas 3D gigantes y salto a la velocidad de la luz en `client/despegue/EspacioCielo.java`) y aterrizaje.
- Qumara (jefe, `qumara/`), `/boss 1 gente N`, Atraer, Levitación.
- Almas (`almas/`, `/alma obtener|sacar|ver`), Fruto de Quiu, tótems (Frerico, Concha), Soarer (vuela y agarra
  jugadores), Nave Veloz, Creeper Amarillo, bloques de carne, Dentadura Glebanoide, ítems Glebanoides, kit `/function dedsafio4:kit`.

## Al terminar un pedido

1. Push a `main`.
2. Esperar los jars compilados por GitHub Actions.
3. Pasarle a Patricio el jar que corresponda y explicarle en español simple qué cambió y cómo probarlo.
