package com.dedsafio4.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Solo para desarrollo: con la variable de entorno DEDSAFIO4_DEBUG_CIELO, al entrar a un mundo
 * activa el cielo rojo con su animación, mira hacia arriba, saca capturas de cada etapa,
 * vuelve al cielo normal (destello) y cierra el juego.
 */
public final class DebugCielo {
	private DebugCielo() {}

	public static final boolean ACTIVO = System.getenv("DEDSAFIO4_DEBUG_CIELO") != null;
	private static int ticks;
	/** Contador aparte, para mientras no hay mundo. */
	private static int ticksMenu;

	public static void registrar() {
		if (!ACTIVO) return;
		// Mientras no haya mundo, avisa qué pantalla está mostrando (para ver errores de carga).
		ClientTickEvents.END_CLIENT_TICK.register(cliente -> {
			if (cliente.player != null || cliente.getConnection() != null) return;
			if (cliente.getFps() == 0) return;
			if ((ticksMenu++) % 20 != 0) return;
			if (cliente.screen == null) return;
			Dedsafio4ClientDebug.info("sin mundo: " + cliente.screen.getClass().getSimpleName()
					+ " | " + cliente.screen.getTitle().getString());
			// El mundo con la dimensión nueva avisa que usa "ajustes experimentales": se confirma solo.
			for (var hijo : cliente.screen.children()) {
				if (hijo instanceof net.minecraft.client.gui.components.Button boton) {
					String texto = boton.getMessage().getString();
					if (texto.contains("know what") || texto.contains("Proceed") || texto.contains("Continuar")) {
						Dedsafio4ClientDebug.info("confirmando el aviso: " + cliente.screen.getTitle().getString());
						boton.onPress();
						return;
					}
				}
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(DebugCielo::tick);
	}

	private static final boolean MOMENTO = "momento".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de /momento revil: jugador en supervivencia a distintas alturas. */
	private static void tickMomento(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) comando(mc, "gamemode survival");
		if (t == 0) { comando(mc, "tp @s ~ 260 ~"); comando(mc, "effect give @s minecraft:slow_falling 60 0 true"); comando(mc, "momento revil 1.1"); }
		if (t == 70) captura(mc, "debug_m1_arriba_250.png");
		if (t == 80) { comando(mc, "effect give @s minecraft:instant_health 1 10 true"); comando(mc, "tp @s ~ 100 ~"); comando(mc, "momento revil 1.2"); }
		if (t == 125) captura(mc, "debug_m2_superficie.png");
		if (t == 130) { comando(mc, "effect give @s minecraft:instant_health 1 10 true"); comando(mc, "tp @s ~ -10 ~"); comando(mc, "fill ~-1 ~-1 ~-1 ~1 ~2 ~1 minecraft:glass hollow"); }
		if (t == 200) captura(mc, "debug_m3_bajo_cero.png");
		if (t == 210) mc.stop();
	}

	private static final boolean MARCAS = "marcas".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de marcas y bloqueo de Xaero: jugador sin permisos, marca creada y vista de lejos. */
	private static void tickMarcas(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "tp @s 0 120 0");
		}
		if (t == 3) {
			comando(mc, "marca crear \"Base Reviil\" rojo");
			comando(mc, "execute positioned 30 120 0 run marca crear Aldea celeste");
		}
		if (t == 5) comando(mc, "tp @s 12 126 12");
		if (t == 20) {
			mc.player.setYRot(135f); mc.player.setXRot(20f); mc.player.yRotO = 135f; mc.player.xRotO = 20f;
		}
		if (t == 40) captura(mc, "debug_x1_marcas.png");
		if (t == 45) comando(mc, "tp @s 130 140 130");
		if (t == 60) {
			mc.player.setYRot(135f); mc.player.setXRot(8f); mc.player.yRotO = 135f; mc.player.xRotO = 8f;
		}
		if (t == 80) captura(mc, "debug_x2_marcas_lejos.png");
		if (t == 90) {
			Dedsafio4ClientDebug.info("bloqueado=" + XaeroBloqueo.bloqueado());
			Dedsafio4ClientDebug.info("teclas xaero=" + java.util.Arrays.stream(mc.options.keyMappings).filter(XaeroBloqueo::esDeXaero).map(k -> k.getName()).toList());
		}
		if (t == 100) mc.stop();
	}

	private static final class Dedsafio4ClientDebug {
		static void info(String s) { com.dedsafio4.Dedsafio4.LOGGER.info("[debug] {}", s); }
	}

	private static final boolean NAVE = "nave".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de la nave: dispara al jugador y explota al morir. */
	private static void tickNave(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) {
			comando(mc, "kill @e[type=dedsafio4:nave]");
			comando(mc, "kill @e[type=dedsafio4:rayo]");
		}
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "tp @s 0 150 0");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -4 149 -4 4 149 4 minecraft:glass");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
			comando(mc, "effect give @s minecraft:instant_health 1 20 true");
			comando(mc, "time set day");
		}
		if (t == 10) comando(mc, "summon dedsafio4:nave 8 153 0");
		if (t >= 10 && t < 115) {
			mc.player.setYRot(-90f); mc.player.setXRot(-12f); mc.player.yRotO = -90f; mc.player.xRotO = -12f;   // hacia +X
		}
		if (t == 20) Dedsafio4ClientDebug.info("vida antes=" + mc.player.getHealth());
		if (t == 60) captura(mc, "debug_n1_nave.png");
		if (t == 63) captura(mc, "debug_n2_rayos.png");
		if (t == 80) Dedsafio4ClientDebug.info("vida tras 3 s de disparos=" + mc.player.getHealth());
		if (t == 85) { comando(mc, "kill @e[type=dedsafio4:nave]"); comando(mc, "kill @e[type=dedsafio4:rayo]"); }
		if (t == 88) { comando(mc, "fill -4 149 -4 4 149 4 minecraft:glass"); comando(mc, "tp @s 0 150 0"); }
		if (t == 90) comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		if (t == 95) comando(mc, "summon dedsafio4:nave ~2 ~ ~ {NoAI:1b}");
		if (t == 100) Dedsafio4ClientDebug.info("vida antes de explosion=" + mc.player.getHealth());
		if (t == 101) comando(mc, "kill @e[type=dedsafio4:nave]");
		if (t == 103) captura(mc, "debug_n3_explosion.png");
		if (t == 110) Dedsafio4ClientDebug.info("vida despues de explosion=" + mc.player.getHealth());
		// Vista del modelo: nave quieta a la altura de los ojos, de frente y de costado.
		if (t == 115) {
			comando(mc, "gamemode spectator");
			comando(mc, "tp @s 0 150 0");
		}
		if (t == 120) {
			comando(mc, "summon dedsafio4:nave ~6 ~ ~ {NoAI:1b,Rotation:[90f,0f]}");
			comando(mc, "summon dedsafio4:nave ~ ~ ~7 {NoAI:1b,Rotation:[0f,0f]}");
		}
		if (t >= 115 && t < 150) { mc.player.setYRot(-90f); mc.player.setXRot(5f); mc.player.yRotO = -90f; mc.player.xRotO = 5f; }
		if (t >= 150) { mc.player.setYRot(0f); mc.player.setXRot(5f); mc.player.yRotO = 0f; mc.player.xRotO = 5f; }
		if (t == 145) captura(mc, "debug_n4_frente.png");
		if (t == 165) captura(mc, "debug_n5_costado.png");
		if (t == 170) comando(mc, "kill @e[type=dedsafio4:nave]");
		if (t == 172) captura(mc, "debug_n6_explosion.png");
		if (t == 185) mc.stop();
	}

	private static final boolean POCIONES = "pociones".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de las pociones Rojizo y Negro Puro, y de la nave más chica disparando cada 1,5 s. */
	private static void tickPociones(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) { comando(mc, "kill @e[type=dedsafio4:nave]"); comando(mc, "kill @e[type=dedsafio4:rayo]"); }
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "weather clear");
			comando(mc, "effect clear @s");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -3 199 -3 12 199 6 minecraft:grass_block");
			comando(mc, "fill 4 200 3 5 202 4 minecraft:oak_leaves");
			comando(mc, "fill 7 200 -2 7 202 0 minecraft:glass");
			comando(mc, "fill 9 199 1 11 199 3 minecraft:water");
			comando(mc, "fill 9 200 -3 10 201 -2 minecraft:stone");
			comando(mc, "setblock 3 200 -1 minecraft:torch");
			comando(mc, "setblock 5 200 -2 minecraft:poppy");
			comando(mc, "tp @s 0 200 0");
		}
		if (t >= 5 && t < 235) { mc.player.setYRot(-90f); mc.player.setXRot(18f); mc.player.yRotO = -90f; mc.player.xRotO = 18f; }
		if (t == 10) comando(mc, "summon dedsafio4:nave 6 202 0 {NoAI:1b,Rotation:[90f,0f]}");
		if (t == 30) captura(mc, "debug_p1_normal.png");
		if (t == 32) comando(mc, "effect give @s dedsafio4:rojizo 60");
		if (t == 50) captura(mc, "debug_p2_rojizo.png");
		if (t == 52) comando(mc, "effect clear @s dedsafio4:rojizo");
		if (t == 55) comando(mc, "effect give @s dedsafio4:negro_puro 60");
		if (t == 75) captura(mc, "debug_p3_negro.png");
		if (t == 76) { comando(mc, "give @s minecraft:grass_block"); comando(mc, "give @s minecraft:oak_leaves"); comando(mc, "give @s minecraft:redstone_block"); }
		if (t == 80) captura(mc, "debug_p3b_hotbar.png");
		if (t == 82) comando(mc, "effect clear @s dedsafio4:negro_puro");
		if (t == 95) captura(mc, "debug_p4_vuelve.png");
		if (t == 97) comando(mc, "clear @s");
		// Cadencia: nave con IA contra el jugador en supervivencia.
		if (t == 100) {
			comando(mc, "kill @e[type=dedsafio4:nave]");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
			comando(mc, "effect give @s minecraft:instant_health 1 20 true");
			comando(mc, "summon dedsafio4:nave 8 204 0");
		}
		if (t == 110) Dedsafio4ClientDebug.info("vida antes=" + mc.player.getHealth());
		if (t == 170) { Dedsafio4ClientDebug.info("vida tras 3 s=" + mc.player.getHealth()); captura(mc, "debug_p5_nave.png"); }
		if (t == 230) Dedsafio4ClientDebug.info("vida tras 6 s=" + mc.player.getHealth());
		if (t == 235) comando(mc, "kill @e[type=dedsafio4:nave]");
		if (t == 245) mc.stop();
	}

	private static final boolean WARDENS = "wardens".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de /cambio wardens 2: el Warden que muere deja un Chillador de Sculk. */
	private static void tickWardens(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -4 199 -4 8 199 4 minecraft:stone");
			comando(mc, "fill -4 200 -4 8 203 4 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "cambio wardens 0");
		}
		if (t >= 5 && t < 200) { mc.player.setYRot(-90f); mc.player.setXRot(25f); mc.player.yRotO = -90f; mc.player.xRotO = 25f; }
		// Sin el cambio no deja nada.
		if (t == 10) comando(mc, "summon minecraft:warden 4 200 0");
		if (t == 20) comando(mc, "kill @e[type=minecraft:warden]");
		if (t == 30) comando(mc, "execute if block 4 200 0 minecraft:sculk_shrieker run say MAL: dejo chillador sin el cambio");
		if (t == 32) comando(mc, "execute unless block 4 200 0 minecraft:sculk_shrieker run say OK: sin el cambio no deja nada");
		// Con el cambio, sí.
		if (t == 40) comando(mc, "cambio wardens 2");
		if (t == 50) comando(mc, "summon minecraft:warden 4 200 0");
		if (t == 60) captura(mc, "debug_w1_warden.png");
		if (t == 65) comando(mc, "kill @e[type=minecraft:warden]");
		if (t == 90) {
			comando(mc, "execute if block 4 200 0 minecraft:sculk_shrieker[can_summon=true] run say OK: chillador con can_summon");
			comando(mc, "execute unless block 4 200 0 minecraft:sculk_shrieker[can_summon=true] run say MAL: no hay chillador");
			comando(mc, "execute if block 4 199 0 minecraft:sculk run say OK: sculk debajo");
		}
		if (t == 100) captura(mc, "debug_w2_chillador.png");
		if (t == 110) comando(mc, "cambio wardens 0");
		if (t == 120) mc.stop();
	}

	private static final boolean PERLAS = "perlas".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static float vidaAntes;

	/** Prueba de /cambio enderperlas 1 (perla ignora armadura) y /cambio creepers electricos 1. */
	private static void tickPerlas(Minecraft mc) {
		int t = ticks - 20;
		if (t == -5 || t == -3) revivir(mc);
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:stone");
			comando(mc, "fill -6 200 -6 6 204 6 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "cambio enderperlas 0");
			comando(mc, "cambio creepers electricos 0");
			comando(mc, "kill @e[type=minecraft:creeper]");
		}
		if (t == 3) {
			// Armadura de diamante con Protección IV en las cuatro piezas.
			for (String pieza : new String[]{"head minecraft:diamond_helmet", "chest minecraft:diamond_chestplate",
					"legs minecraft:diamond_leggings", "feet minecraft:diamond_boots"}) {
				String[] p = pieza.split(" ");
				comando(mc, "item replace entity @s armor." + p[0] + " with " + p[1]
						+ "[enchantments={levels:{\"minecraft:protection\":4}}]");
			}
			comando(mc, "gamemode survival");
			comando(mc, "attribute @s minecraft:generic.max_health base set 200");
			comando(mc, "effect give @s minecraft:instant_health 1 40 true");
			comando(mc, "effect give @s minecraft:saturation 1 10 true");
		}
		if (t >= 5 && t < 250) { mc.player.setXRot(90f); mc.player.xRotO = 90f; }   // mirando al piso
		// 1) Sin el cambio: la Protección IV frena parte del golpe.
		if (t == 20) { comando(mc, "give @s minecraft:ender_pearl 16"); }
        if (t == 30) vidaAntes = mc.player.getHealth();
		if (t == 31) tirarPerla(mc);
		if (t > 31 && t < 70 && mc.player.getHealth() != vidaAntes) { Dedsafio4ClientDebug.info("t=" + t + " vida=" + mc.player.getHealth() + " fuente=" + mc.player.getLastDamageSource()); vidaAntes = mc.player.getHealth(); }
		if (t == 70) Dedsafio4ClientDebug.info("perlas que quedan=" + mc.player.getInventory().countItem(net.minecraft.world.item.Items.ENDER_PEARL));
		if (t == 70) Dedsafio4ClientDebug.info("SIN cambio: danio=" + (vidaAntes - mc.player.getHealth()));
		// 2) Con el cambio: el golpe entero.
		if (t == 75) comando(mc, "cambio enderperlas 2");
		if (t == 85) { comando(mc, "effect give @s minecraft:instant_health 1 40 true"); }
		if (t == 90) vidaAntes = mc.player.getHealth();
		if (t == 91) tirarPerla(mc);
		if (t > 91 && t < 130 && mc.player.getHealth() != vidaAntes) { Dedsafio4ClientDebug.info("t=" + t + " vida=" + mc.player.getHealth() + " fuente=" + mc.player.getLastDamageSource()); vidaAntes = mc.player.getHealth(); }
		if (t == 130) Dedsafio4ClientDebug.info("CON cambio: danio=" + (vidaAntes - mc.player.getHealth()));
		// 3) Creepers eléctricos.
		if (t == 140) comando(mc, "summon minecraft:creeper 3 200 0 {NoAI:1b}");
		if (t == 145) {
			comando(mc, "execute if entity @e[type=minecraft:creeper,nbt={powered:1b}] run say MAL: creeper electrico sin el cambio");
			comando(mc, "execute unless entity @e[type=minecraft:creeper,nbt={powered:1b}] run say OK: sin el cambio el creeper es normal");
		}
		if (t == 150) comando(mc, "cambio creepers electricos 1");
		if (t == 160) comando(mc, "execute if entity @e[type=minecraft:creeper,nbt={powered:1b}] run say OK: el creeper que ya estaba quedo electrico");
		if (t == 165) comando(mc, "summon minecraft:creeper 3 200 3 {NoAI:1b}");
		if (t == 175) {
			comando(mc, "execute unless entity @e[type=minecraft:creeper,nbt={powered:0b}] run say OK: el creeper nuevo tambien es electrico");
			comando(mc, "gamemode spectator");
			comando(mc, "tp @s 0 202 -6");
		}
		if (t >= 176 && t < 250) { mc.player.setYRot(0f); mc.player.setXRot(10f); mc.player.yRotO = 0f; mc.player.xRotO = 10f; }
		if (t == 195) captura(mc, "debug_c1_creepers.png");
		if (t == 200) { comando(mc, "cambio creepers electricos 0"); comando(mc, "cambio enderperlas 0"); comando(mc, "kill @e[type=minecraft:creeper]"); }
		if (t == 210) mc.stop();
	}

	private static void tirarPerla(Minecraft mc) {
		mc.player.getInventory().selected = 0;
		mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
	}

	private static final boolean BOMBA = "bomba".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de la Bomba Warden: cómo se ve de frente, de costado y de atrás, y que explota al acercarse. */
	private static void tickBomba(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		mc.options.hideGui = true;
		if (t == -5) { comando(mc, "kill @e[type=dedsafio4:bomba_warden]"); comando(mc, "kill @e[type=!player,distance=..60]"); }
		if (t == 0) {
			comando(mc, "gamemode spectator");
			comando(mc, "time set day");
			comando(mc, "difficulty normal");
			comando(mc, "momento revil parar");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0 200.9 -2.4");
		}
		if (t == 5) comando(mc, "summon dedsafio4:bomba_warden 0 200 0 {NoAI:1b,Rotation:[180f,0f]}");
		if (t >= 6 && t < 90) { mc.player.setYRot(0f); mc.player.setXRot(0f); mc.player.yRotO = 0f; mc.player.xRotO = 0f; }
		if (t == 30) captura(mc, "debug_b1_frente.png");
		if (t == 35) comando(mc, "tp @e[type=dedsafio4:bomba_warden,limit=1] 0 200 0 90 0");
		if (t == 50) captura(mc, "debug_b2_costado.png");
		if (t == 55) comando(mc, "tp @e[type=dedsafio4:bomba_warden,limit=1] 0 200 0 0 0");
		if (t == 70) captura(mc, "debug_b3_atras.png");
		if (t == 75) comando(mc, "kill @e[type=dedsafio4:bomba_warden]");
		// Explosión al acercarse, con bloques al costado para ver si los rompe.
		if (t == 80) {
			comando(mc, "setblock 2 200 0 minecraft:oak_planks");
			comando(mc, "setblock -2 200 0 minecraft:glass");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "gamemode survival");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
			comando(mc, "effect give @s minecraft:instant_health 1 40 true");
			for (String pieza : new String[]{"head", "chest", "legs", "feet"}) {
				comando(mc, "item replace entity @s armor." + pieza + " with minecraft:air");
			}
		}
		if (t >= 81 && t < 250) { mc.player.setYRot(180f); mc.player.setXRot(0f); mc.player.yRotO = 180f; mc.player.xRotO = 0f; }
		if (t == 90) { comando(mc, "summon dedsafio4:bomba_warden 0 201.5 -12"); vidaAntes = mc.player.getHealth(); }
		if (t > 91 && t < 250 && mc.player.getHealth() != vidaAntes) {
			Dedsafio4ClientDebug.info("t=" + t + " vida=" + mc.player.getHealth() + " danio=" + (vidaAntes - mc.player.getHealth())
					+ " fuente=" + mc.player.getLastDamageSource());
			vidaAntes = mc.player.getHealth();
		}
		if (t == 130) captura(mc, "debug_b4_viene.png");
		if (t == 150) captura(mc, "debug_b5_cerca.png");
		if (t == 250) {
			comando(mc, "execute if block 2 200 0 minecraft:oak_planks run say OK: no rompio los bloques");
			comando(mc, "execute unless block 2 200 0 minecraft:oak_planks run say MAL: rompio los bloques");
			comando(mc, "execute if entity @e[type=dedsafio4:bomba_warden] run say MAL: la bomba sigue viva");
			comando(mc, "execute unless entity @e[type=dedsafio4:bomba_warden] run say OK: la bomba desaparecio al explotar");
		}
		if (t == 260) mc.stop();
	}

	private static final boolean DEEPDARK = "deepdark".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de que las Bombas Warden aparecen solas en la Oscuridad Profunda. */
	private static void tickDeepdark(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			// Los mobs no aparecen alrededor de un espectador: hay que estar en supervivencia.
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "kill @e[type=dedsafio4:bomba_warden]");
			comando(mc, "tp @s 64 -28 -64");
			comando(mc, "effect give @s minecraft:resistance 600 4 true");
			comando(mc, "effect give @s minecraft:regeneration 600 2 true");
			comando(mc, "effect give @s minecraft:night_vision 600 0 true");
		}
		if (t == 10) comando(mc, "fill 62 -30 -66 66 -25 -62 minecraft:glass hollow");
		if (t % 200 == 0 && t > 0 && t < 1300) {
			comando(mc, "execute if entity @e[type=dedsafio4:bomba_warden] run say HAY BOMBAS t=" + t);
		}
		if (t == 280) comando(mc, "tp @s @e[type=dedsafio4:bomba_warden,limit=1,sort=nearest]");
		if (t == 285) { comando(mc, "gamemode spectator"); comando(mc, "tp @s ~ ~ ~4"); mc.options.hideGui = true; }
		if (t >= 286 && t < 320) { mc.player.setYRot(180f); mc.player.setXRot(0f); mc.player.yRotO = 180f; mc.player.xRotO = 0f; }
		if (t == 300) captura(mc, "debug_b6_deepdark.png");
		if (t == 310) mc.stop();
		if (t == 1300) {
			comando(mc, "execute if entity @e[type=dedsafio4:bomba_warden] run say OK: aparecieron solas en la Oscuridad Profunda");
			comando(mc, "execute unless entity @e[type=dedsafio4:bomba_warden] run say MAL: no aparecio ninguna");
			captura(mc, "debug_b6_deepdark.png");
		}
		if (t == 1320) mc.stop();
	}

	private static final boolean CANDADO = "candado".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static final net.minecraft.core.BlockPos COFRE = new net.minecraft.core.BlockPos(1, 200, 0);

	/** Click derecho en el cofre, como si el jugador lo hiciera. */
	private static void clickCofre(Minecraft mc) {
		var hit = new net.minecraft.world.phys.BlockHitResult(
				net.minecraft.world.phys.Vec3.atCenterOf(COFRE), net.minecraft.core.Direction.WEST, COFRE, false);
		mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
	}

	private static void escribir(Minecraft mc, String texto) {
		if (mc.screen == null) return;
		for (char c : texto.toCharArray()) {
			mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_0 + (c - '0'), 0, 0);
		}
		mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);
	}

	/** Mira el candado del cofre en el servidor y lo cuenta por el registro. */
	private static void mirarCerradura(Minecraft mc, String etiqueta) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		server.execute(() -> {
			var cerradura = com.dedsafio4.candados.Candados.data(server.overworld()).cerradura(COFRE);
			Dedsafio4ClientDebug.info(etiqueta + ": " + (cerradura == null ? "sin candado"
					: "codigo=" + cerradura.codigo() + " autorizados=" + cerradura.autorizados().size()));
		});
	}

	/** Rompe el cofre igual que cuando un jugador lo pica, y cuenta si pudo. */
	private static void romperComoJugador(Minecraft mc, String etiqueta) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		var uuid = mc.player.getUUID();
		server.execute(() -> {
			var jugador = server.getPlayerList().getPlayer(uuid);
			if (jugador == null) return;
			boolean rompio = jugador.gameMode.destroyBlock(COFRE);
			Dedsafio4ClientDebug.info("romper " + etiqueta + ": pudo=" + rompio
					+ " quedo=" + server.overworld().getBlockState(COFRE).getBlock());
		});
	}

	/** Prueba del candado: ponerlo, abrir con el código, código incorrecto y romper el cofre. */
	private static void tickCandado(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof CandadoScreen)
				&& !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen)) {
			mc.setScreen(null);
		}
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -3 199 -3 3 199 3 minecraft:stone");
			comando(mc, "fill -3 200 -3 3 203 3 minecraft:air");
			comando(mc, "setblock 1 200 0 minecraft:chest");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "clear @s");
		}
		if (t >= 3 && t < 400) { mc.player.setYRot(-90f); mc.player.setXRot(10f); mc.player.yRotO = -90f; mc.player.xRotO = 10f; }
		if (t == 10) comando(mc, "item replace entity @s hotbar.0 with dedsafio4:candado 3");
		if (t == 20) { mc.player.getInventory().selected = 0; clickCofre(mc); }
		if (t == 30) { Dedsafio4ClientDebug.info("pantalla al poner=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_k1_poner.png"); }
		if (t == 33 && mc.screen != null) {
			for (char c : "12".toCharArray()) mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_0 + (c - '0'), 0, 0);
		}
		if (t == 34) captura(mc, "debug_k0_teclado.png");
		if (t == 35) escribir(mc, "1234");
		if (t == 45) { mirarCerradura(mc, "tras cerrar"); Dedsafio4ClientDebug.info("candados que quedan=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.CANDADO)); }
		// El dueño abre sin que le pidan nada.
		if (t == 55) clickCofre(mc);
		if (t == 65) { Dedsafio4ClientDebug.info("dueño abre: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_k2_dueno.png"); mc.setScreen(null); }
		// Ahora el candado queda a nombre de otro: al jugador le piden el código.
		if (t == 75) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> com.dedsafio4.candados.Candados.data(server.overworld())
					.poner(COFRE, "1234", java.util.UUID.randomUUID()));
		}
		if (t == 85) clickCofre(mc);
		if (t == 95) { Dedsafio4ClientDebug.info("ajeno: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_k3_pide.png"); }
		if (t == 100) escribir(mc, "9999");
		if (t == 115) { Dedsafio4ClientDebug.info("tras codigo incorrecto: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_k4_incorrecto.png"); }
		// Romper el cofre sin saber el código no se puede.
		if (t == 116) comando(mc, "setblock 2 201 0 minecraft:white_wool");   // testigo al lado de la TNT
		if (t == 118) { comando(mc, "summon minecraft:tnt 1 201 0 {fuse:10s}"); comando(mc, "tp @s 0 200 -3"); }
		if (t == 134) {
			comando(mc, "execute if block 1 200 0 minecraft:chest run say OK: la TNT no volo el cofre cerrado");
			// si la piedra de al lado voló, la explosión pasó de verdad
			comando(mc, "execute unless block 2 201 0 minecraft:white_wool run say OK: la TNT exploto igual (volo el testigo)");
			comando(mc, "execute if block 2 201 0 minecraft:white_wool run say MAL: la TNT no exploto, la prueba no vale");
		}
		if (t == 138) romperComoJugador(mc, "sin el codigo");
		if (t == 135) comando(mc, "execute if block 1 200 0 minecraft:chest run say OK: no se pudo romper sin el codigo");
		if (t == 140) comando(mc, "execute unless block 1 200 0 minecraft:chest run say MAL: lo rompio sin el codigo");
		// Con el código correcto se abre.
		if (t == 150) clickCofre(mc);
		if (t == 160) escribir(mc, "1234");
		if (t == 175) { Dedsafio4ClientDebug.info("con el codigo: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_k5_abierto.png"); mirarCerradura(mc, "tras acertar"); }
		if (t == 185) mc.setScreen(null);
		if (t == 195) romperComoJugador(mc, "con el codigo");
		if (t == 205) comando(mc, "execute unless block 1 200 0 minecraft:chest run say OK: con el codigo ya lo puede romper");
		if (t == 210) mirarCerradura(mc, "tras romper");
		if (t == 220) mc.stop();
	}

	private static final boolean MARTILLO = "martillo".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Martillo: repara la armadura de otro y lo de la mano secundaria, gastando XP. */
	private static void tickMartillo(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) comando(mc, "kill @e[type=minecraft:zombie]");
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "difficulty easy");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:stone");
			comando(mc, "fill -6 200 -6 6 204 6 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "clear @s");
			comando(mc, "xp set @s 30 levels");
		}
		if (t >= 3 && t < 300) { mc.player.setYRot(-90f); mc.player.setXRot(10f); mc.player.yRotO = -90f; mc.player.xRotO = 10f; }
		if (t == 5) {
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:martillo_netherite");
			comando(mc, "item replace entity @s weapon.offhand with minecraft:netherite_pickaxe[damage=1500]");
			// Un zombie con casco roto, quieto, para reparárselo.
			comando(mc, "summon minecraft:zombie 2 200 0 {NoAI:1b,PersistenceRequired:1b,ArmorItems:[{},{},{},{id:\"minecraft:diamond_helmet\",count:1,components:{\"minecraft:damage\":300}}]}");
		}
		if (t == 10) mc.player.getInventory().selected = 0;
		if (t == 15) Dedsafio4ClientDebug.info("antes: pico=" + danoOffhand(mc) + " casco=" + danoCasco(mc) + " nivel=" + mc.player.experienceLevel);
		// 1) Click derecho sobre el zombie: le repara el casco.
		if (t >= 20 && t < 60 && t % 4 == 0) clickEnZombie(mc);
		if (t == 62) Dedsafio4ClientDebug.info("tras clicks al zombie: casco=" + danoCasco(mc) + " pico=" + danoOffhand(mc) + " nivel=" + mc.player.experienceLevel);
		// 2) Click derecho al aire: repara el pico de la mano secundaria.
		if (t >= 70 && t < 110 && t % 4 == 0) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 112) Dedsafio4ClientDebug.info("tras clicks al aire: pico=" + danoOffhand(mc) + " casco=" + danoCasco(mc) + " nivel=" + mc.player.experienceLevel);
		// En supervivencia sí gasta experiencia.
		if (t == 115) { comando(mc, "gamemode survival"); comando(mc, "xp set @s 0 levels"); comando(mc, "xp set @s 10 levels"); }
		if (t == 118) Dedsafio4ClientDebug.info("supervivencia, antes: pico=" + danoOffhand(mc) + " nivel=" + mc.player.experienceLevel + " progreso=" + String.format("%.2f", mc.player.experienceProgress));
		if (t >= 120 && t < 140 && t % 4 == 0) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 142) Dedsafio4ClientDebug.info("supervivencia, despues: pico=" + danoOffhand(mc) + " nivel=" + mc.player.experienceLevel + " progreso=" + String.format("%.2f", mc.player.experienceProgress));
		if (t == 145) { comando(mc, "xp set @s 0 points"); comando(mc, "xp set @s 0 levels"); }
		if (t >= 150 && t < 165 && t % 4 == 0) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 167) Dedsafio4ClientDebug.info("sin experiencia: pico=" + danoOffhand(mc));
		if (t == 170) comando(mc, "kill @e[type=minecraft:zombie]");
		if (t == 175) mc.stop();
	}

	private static int danoOffhand(Minecraft mc) {
		return mc.player.getOffhandItem().getDamageValue();
	}

	private static int danoCasco(Minecraft mc) {
		for (var e : mc.level.entitiesForRendering()) {
			if (e instanceof net.minecraft.world.entity.monster.Zombie zombie) {
				return zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getDamageValue();
			}
		}
		return -1;
	}

	private static void clickEnZombie(Minecraft mc) {
		for (var e : mc.level.entitiesForRendering()) {
			if (e instanceof net.minecraft.world.entity.monster.Zombie zombie) {
				mc.gameMode.interact(mc.player, zombie, net.minecraft.world.InteractionHand.MAIN_HAND);
				return;
			}
		}
	}

	/** Daño de una pieza de armadura (2 = peto, 3 = casco). */
	private static int dano(Minecraft mc, int indice) {
		return mc.player.getInventory().armor.get(indice).getDamageValue();
	}

	private static final boolean CHIP = "chip".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Chip Phora: cómo se ve el ítem y su descripción. */
	private static void tickChip(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 30) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:chip_phora 12");
		}
		if (t == 30) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		if (t == 60) captura(mc, "debug_c1_chip.png");
		if (t == 70) mc.stop();
	}

	private static final boolean SOARER = "soarer".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Soarer: que el modelo y las animaciones de GeckoLib carguen bien. */
	private static void tickSoarer(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		mc.options.hideGui = true;
		if (t == -5) comando(mc, "kill @e[type=dedsafio4:soarer]");
		if (t == 0) {
			comando(mc, "gamemode spectator");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -10 199 -10 10 199 10 minecraft:stone");
			comando(mc, "fill -10 200 -10 10 210 10 minecraft:air");
			comando(mc, "tp @s 0 202 -9");
		}
		if (t == 5) comando(mc, "summon dedsafio4:soarer 0 200 0 {NoAI:1b,Rotation:[180f,0f]}");
		if (t >= 6 && t < 200) { mc.player.setYRot(0f); mc.player.setXRot(8f); mc.player.yRotO = 0f; mc.player.xRotO = 8f; }
		if (t == 40) captura(mc, "debug_s1_frente.png");
		if (t == 45) comando(mc, "tp @e[type=dedsafio4:soarer,limit=1] 0 200 0 90 0");
		if (t == 70) captura(mc, "debug_s2_costado.png");
		// Con IA: camina y se ve la animación de caminar.
		if (t == 75) { comando(mc, "kill @e[type=dedsafio4:soarer]"); comando(mc, "summon dedsafio4:soarer 6 200 4"); }
		if (t == 120) captura(mc, "debug_s3_caminando.png");
		if (t == 150) captura(mc, "debug_s4_caminando2.png");
		if (t == 160) comando(mc, "kill @e[type=dedsafio4:soarer]");
		if (t == 170) mc.stop();
	}

	private static final boolean LAGARTOS = "lagartos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de los cuatro lagartos: en fila quietos, y uno corriendo. */
	private static void tickLagartos(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		mc.options.hideGui = true;
		if (t == -5) {
			for (String id : new String[]{"mira", "tizon", "jade", "ambar"}) comando(mc, "kill @e[type=dedsafio4:" + id + "]");
		}
		if (t == 0) {
			comando(mc, "gamemode spectator");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -10 199 -10 10 199 10 minecraft:stone");
			comando(mc, "fill -10 200 -10 10 206 10 minecraft:air");
			comando(mc, "tp @s 0 201 -7");
		}
		// En fila, mirando a la cámara.
		if (t == 5) {
			String[] ids = {"mira", "tizon", "jade", "ambar"};
			for (int i = 0; i < ids.length; i++) {
				comando(mc, "summon dedsafio4:" + ids[i] + " " + (-3 + i * 2) + " 200 0 {NoAI:1b,Rotation:[180f,0f]}");
			}
		}
		if (t >= 6 && t < 200) { mc.player.setYRot(0f); mc.player.setXRot(6f); mc.player.yRotO = 0f; mc.player.xRotO = 6f; }
		if (t == 40) captura(mc, "debug_l1_fila.png");
		if (t == 45) {
			for (String id : new String[]{"mira", "tizon", "jade", "ambar"}) {
				comando(mc, "execute as @e[type=dedsafio4:" + id + "] run tp @s ~ ~ ~ 180 0");
			}
			comando(mc, "tp @s 0 200.8 -4");
		}
		if (t == 60) captura(mc, "debug_l2_cerca.png");
		// Uno corriendo detrás del jugador.
		if (t == 70) {
			for (String id : new String[]{"mira", "tizon", "jade", "ambar"}) comando(mc, "kill @e[type=dedsafio4:" + id + "]");
			comando(mc, "summon dedsafio4:jade 8 200 6");
			comando(mc, "tp @s 0 201 -6");
		}
		if (t >= 71 && t < 200) { mc.player.setYRot(-30f); mc.player.setXRot(6f); mc.player.yRotO = -30f; mc.player.xRotO = 6f; }
		if (t == 110) captura(mc, "debug_l3_caminando.png");
		if (t == 125) captura(mc, "debug_l4_caminando2.png");
		if (t == 135) {
			for (String id : new String[]{"mira", "tizon", "jade", "ambar"}) comando(mc, "kill @e[type=dedsafio4:" + id + "]");
		}
		if (t == 145) mc.stop();
	}

	private static final boolean WALKER = "walker".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Walker: que el modelo y las animaciones de GeckoLib carguen bien. */
	private static void tickWalker(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		mc.options.hideGui = true;
		if (t == -5) comando(mc, "kill @e[type=dedsafio4:walker]");
		if (t == 0) {
			comando(mc, "gamemode spectator");
			comando(mc, "time set day");
			comando(mc, "forceload add -32 -32 32 32");
			comando(mc, "fill -20 199 -20 20 199 20 minecraft:stone");
			comando(mc, "fill -20 200 -20 20 215 20 minecraft:air");
			comando(mc, "tp @s 0 205 -22");
		}
		if (t == 5) comando(mc, "summon dedsafio4:walker 0 200 0 {NoAI:1b,Rotation:[180f,0f]}");
		if (t >= 6 && t < 200) { mc.player.setYRot(0f); mc.player.setXRot(6f); mc.player.yRotO = 0f; mc.player.xRotO = 6f; }
		if (t == 40) captura(mc, "debug_w1_frente.png");
		if (t == 45) comando(mc, "tp @e[type=dedsafio4:walker,limit=1] 0 200 0 90 0");
		if (t == 70) captura(mc, "debug_w2_costado.png");
		// Con IA: camina.
		if (t == 75) { comando(mc, "kill @e[type=dedsafio4:walker]"); comando(mc, "summon dedsafio4:walker 10 200 8"); }
		if (t == 120) captura(mc, "debug_w3_caminando.png");
		if (t == 150) captura(mc, "debug_w4_caminando2.png");
		if (t == 160) comando(mc, "kill @e[type=dedsafio4:walker]");
		if (t == 170) mc.stop();
	}

	private static final boolean CACA = "caca".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del bloque CACA: que se caiga, que tenga media altura y que se rompa como la arena. */
	private static void tickCaca(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:stone");
			comando(mc, "fill -6 200 -6 6 206 6 minecraft:air");
			comando(mc, "tp @s 0 200 -3");
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(0f); mc.player.setXRot(10f); mc.player.yRotO = 0f; mc.player.xRotO = 10f; }
		// Uno apoyado y otro en el aire (este se tiene que caer).
		if (t == 10) {
			comando(mc, "setblock 0 200 0 dedsafio4:caca");
			comando(mc, "setblock 2 204 0 dedsafio4:caca");
			comando(mc, "setblock 2 200 0 minecraft:sand");
		}
		if (t == 12) comando(mc, "execute if block 2 204 0 dedsafio4:caca run say estaba arriba");
		if (t == 40) {
			comando(mc, "execute unless block 2 204 0 dedsafio4:caca run say OK: se cayo");
			comando(mc, "execute if block 2 200 0 dedsafio4:caca run say OK: llego al piso");
		}
		if (t == 45) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var mundo = server.overworld();
				var caca = mundo.getBlockState(new net.minecraft.core.BlockPos(0, 200, 0));
				var arena = net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
				Dedsafio4ClientDebug.info("dureza caca=" + caca.getDestroySpeed(mundo, new net.minecraft.core.BlockPos(0, 200, 0))
						+ " arena=" + arena.getDestroySpeed(mundo, new net.minecraft.core.BlockPos(0, 200, 0)));
			});
		}
		// Altura: el jugador se para encima y tiene que quedar a media altura.
		if (t == 55) comando(mc, "tp @s 0 202 0");
		if (t == 80) Dedsafio4ClientDebug.info("altura del jugador encima=" + (mc.player.getY() - 200));
		if (t == 85) { comando(mc, "tp @s 0 200.5 -2.2"); mc.options.hideGui = true; }
		if (t >= 86 && t < 120) { mc.player.setYRot(0f); mc.player.setXRot(25f); mc.player.yRotO = 0f; mc.player.xRotO = 25f; }
		if (t == 100) captura(mc, "debug_ca1_bloque.png");
		if (t == 110) mc.stop();
	}

	private static final boolean REPTI = "repti".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Reptisaurio con Lanza: ataca de lejos, envenena, y el antídoto y la máscara lo curan. */
	private static void tickRepti(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) comando(mc, "kill @e[type=dedsafio4:reptisaurio_lanza]");
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
			comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		}
		if (t >= 3 && t < 400) { mc.player.setYRot(-90f); mc.player.setXRot(5f); mc.player.yRotO = -90f; mc.player.xRotO = 5f; }
		if (t == 10) comando(mc, "summon dedsafio4:reptisaurio_lanza 6 200 0");
		if (t == 20) Dedsafio4ClientDebug.info("vida antes=" + mc.player.getHealth());
		if (t == 90) {
			Dedsafio4ClientDebug.info("tras el ataque: vida=" + mc.player.getHealth()
					+ " envenenado=" + mc.player.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO));
			captura(mc, "debug_r1_lanza.png");
		}
		// Con el veneno no se puede curar.
		if (t == 95) comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		if (t == 100) Dedsafio4ClientDebug.info("intento de curarme: vida=" + mc.player.getHealth());
		if (t == 140) Dedsafio4ClientDebug.info("40 ticks despues (el veneno pega): vida=" + mc.player.getHealth());
		// El antídoto lo saca.
		if (t == 150) { comando(mc, "kill @e[type=dedsafio4:reptisaurio_lanza]"); comando(mc, "item replace entity @s hotbar.0 with dedsafio4:antidoto_primitivo 2"); }
		if (t == 155) mc.player.getInventory().selected = 0;
		// Se toma en 32 ticks: hay que mantener el botón apretado, como haría un jugador.
		if (t == 157) mc.options.keyUse.setDown(true);
		if (t == 158) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 198) mc.options.keyUse.setDown(false);
		if (t == 205) Dedsafio4ClientDebug.info("tras el antidoto: envenenado=" + mc.player.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO));
		if (t == 208) comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		if (t == 212) Dedsafio4ClientDebug.info("ya me puedo curar: vida=" + mc.player.getHealth());
		// La máscara evita el veneno.
		if (t == 215) comando(mc, "item replace entity @s armor.head with dedsafio4:mascara_anti_esporas");
		if (t == 220) comando(mc, "summon dedsafio4:reptisaurio_lanza 6 200 0");
		if (t == 300) Dedsafio4ClientDebug.info("con la mascara: envenenado=" + mc.player.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO)
				+ " vida=" + mc.player.getHealth());
		// Drop al morir.
		if (t == 310) comando(mc, "kill @e[type=dedsafio4:reptisaurio_lanza]");
		if (t == 320) comando(mc, "execute if entity @e[type=minecraft:item,nbt={Item:{id:\"dedsafio4:sangre_reptisaurio\"}}] run say OK: solto Sangre de Reptisaurio");
		if (t == 330) mc.stop();
	}

	private static final boolean ARQUERO = "arquero".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Reptisaurio Arquero: dispara de lejos y sus flechas envenenan. */
	private static void tickArquero(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) comando(mc, "kill @e[type=dedsafio4:reptisaurio_arquero]");
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "time set day");
			comando(mc, "forceload add -32 -32 32 32");
			comando(mc, "fill -20 199 -20 20 199 20 minecraft:stone");
			comando(mc, "fill -20 200 -20 20 206 20 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
			comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		}
		if (t >= 3 && t < 400) { mc.player.setYRot(-90f); mc.player.setXRot(5f); mc.player.yRotO = -90f; mc.player.xRotO = 5f; }
		if (t == 10) comando(mc, "summon dedsafio4:reptisaurio_arquero 12 200 0");
		if (t == 20) Dedsafio4ClientDebug.info("vida antes=" + mc.player.getHealth());
		if (t == 70) captura(mc, "debug_a1_arquero.png");
		if (t == 120) Dedsafio4ClientDebug.info("tras los disparos: vida=" + mc.player.getHealth()
				+ " envenenado=" + mc.player.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO)
				+ " distancia=" + String.format("%.1f", distanciaArquero(mc)));
		// Con la máscara no debería envenenar.
		if (t == 130) {
			comando(mc, "effect clear @s");
			comando(mc, "item replace entity @s armor.head with dedsafio4:mascara_anti_esporas");
			comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		}
		if (t == 230) Dedsafio4ClientDebug.info("con la mascara: vida=" + mc.player.getHealth()
				+ " envenenado=" + mc.player.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO));
		if (t == 240) comando(mc, "kill @e[type=dedsafio4:reptisaurio_arquero]");
		if (t == 250) comando(mc, "execute if entity @e[type=minecraft:item,nbt={Item:{id:\"dedsafio4:sangre_reptisaurio\"}}] run say OK: solto Sangre de Reptisaurio");
		if (t == 260) mc.stop();
	}

	private static double distanciaArquero(Minecraft mc) {
		for (var e : mc.level.entitiesForRendering()) {
			if (e instanceof com.dedsafio4.lagartos.LagartoEntity lagarto && lagarto.conArco()) {
				return lagarto.distanceTo(mc.player);
			}
		}
		return -1;
	}

	private static final boolean GUERRERO = "guerrero".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Reptisaurio Guerrero: va de frente, aguanta y envenena al golpear. */
	private static void tickGuerrero(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5) comando(mc, "kill @e[type=dedsafio4:reptisaurio_guerrero]");
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -10 199 -10 10 199 10 minecraft:stone");
			comando(mc, "fill -10 200 -10 10 206 10 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
			comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		}
		if (t >= 3 && t < 400) { mc.player.setYRot(-90f); mc.player.setXRot(5f); mc.player.yRotO = -90f; mc.player.xRotO = 5f; }
		if (t == 10) comando(mc, "summon dedsafio4:reptisaurio_guerrero 8 200 0");
		if (t == 20) Dedsafio4ClientDebug.info("vida antes=" + mc.player.getHealth() + " vida del guerrero=" + vidaGuerrero(mc));
		if (t == 60) captura(mc, "debug_g1_guerrero.png");
		if (t == 100) Dedsafio4ClientDebug.info("tras la pelea: vida=" + mc.player.getHealth()
				+ " envenenado=" + mc.player.hasEffect(com.dedsafio4.reptisaurios.VenenoPrimitivo.EFECTO));
		if (t == 110) comando(mc, "kill @e[type=dedsafio4:reptisaurio_guerrero]");
		if (t == 120) comando(mc, "execute if entity @e[type=minecraft:item,nbt={Item:{id:\"dedsafio4:sangre_reptisaurio\"}}] run say OK: solto Sangre de Reptisaurio");
		if (t == 130) mc.stop();
	}

	private static String vidaGuerrero(Minecraft mc) {
		for (var e : mc.level.entitiesForRendering()) {
			if (e instanceof com.dedsafio4.lagartos.LagartoEntity lagarto && lagarto.esGuerrero()) {
				return lagarto.getHealth() + "/" + lagarto.getMaxHealth();
			}
		}
		return "no esta";
	}

	private static final boolean ROBOT = "robot".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Aldeano Robot: cómo se ve, que comercie sin mesa y que no queden aldeanos viejos. */
	private static void tickRobot(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 75) mc.setScreen(null);   // después del click dejamos abierta la del comercio
		mc.options.pauseOnLostFocus = false;
		if (t == -5) comando(mc, "kill @e[type=dedsafio4:aldeano_robot]");
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "difficulty normal");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0 200 -3");
			comando(mc, "clear @s");
		}
		if (t >= 3 && t < 300) { mc.player.setYRot(0f); mc.player.setXRot(3f); mc.player.yRotO = 0f; mc.player.xRotO = 3f; }
		if (t == 5) comando(mc, "summon dedsafio4:aldeano_robot 0 200 0 {NoAI:1b,Rotation:[180f,0f]}");
		// Aldeanos viejos: se tienen que borrar solos.
		if (t == 8) { comando(mc, "summon minecraft:villager 4 200 0"); comando(mc, "summon minecraft:zombie_villager -4 200 0"); }
		if (t == 20) {
			comando(mc, "execute if entity @e[type=minecraft:villager] run say MAL: quedo un aldeano viejo");
			comando(mc, "execute unless entity @e[type=minecraft:villager] run say OK: no quedan aldeanos viejos");
			comando(mc, "execute unless entity @e[type=minecraft:zombie_villager] run say OK: no quedan aldeanos zombis");
		}
		if (t == 40) { mc.options.hideGui = true; captura(mc, "debug_rb1_robot.png"); }
		if (t == 45) { mc.options.hideGui = false; comando(mc, "tp @e[type=dedsafio4:aldeano_robot,limit=1] 0 200 0 90 0"); }
		if (t == 60) captura(mc, "debug_rb2_costado.png");
		// Comercio: click derecho sin mesa de trabajo.
		if (t == 70) comando(mc, "tp @e[type=dedsafio4:aldeano_robot,limit=1] 0 200 -1.5 180 0");
		if (t == 80) {
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof com.dedsafio4.robots.AldeanoRobotEntity robot) {
					mc.gameMode.interact(mc.player, robot, net.minecraft.world.InteractionHand.MAIN_HAND);
					break;
				}
			}
		}
		if (t == 100) {
			Dedsafio4ClientDebug.info("pantalla al hacer click=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName()));
			captura(mc, "debug_rb3_comercio.png");
		}
		if (t == 110) { mc.setScreen(null); comando(mc, "kill @e[type=dedsafio4:aldeano_robot]"); }
		if (t == 120) mc.stop();
	}

	private static final boolean MAGMA = "magma".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de /cambio magma 1: tocar el magma de cualquier lado mata, aunque tengas de todo. */
	private static void tickMagma(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule keepInventory true");
			comando(mc, "momento revil parar");
			comando(mc, "cambio magma 0");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "effect give @s minecraft:fire_resistance 600 0 true");
			comando(mc, "effect give @s minecraft:resistance 600 4 true");
			for (String pieza : new String[]{"head minecraft:netherite_helmet", "chest minecraft:netherite_chestplate",
					"legs minecraft:netherite_leggings", "feet minecraft:netherite_boots"}) {
				String[] p = pieza.split(" ");
				comando(mc, "item replace entity @s armor." + p[0] + " with " + p[1]
						+ "[enchantments={levels:{\"minecraft:protection\":4}}]");
			}
		}
		// Sin el cambio: pararse encima del magma casi no hace nada (con resistencia y protección).
		if (t == 10) { comando(mc, "setblock 0 199 0 minecraft:magma_block"); }
		if (t == 40) Dedsafio4ClientDebug.info("sin el cambio, parado encima: vida=" + mc.player.getHealth());
		// Con el cambio: de arriba.
		if (t == 45) comando(mc, "cambio magma 1");
		if (t == 60) Dedsafio4ClientDebug.info("con el cambio, encima: vida=" + mc.player.getHealth()
				+ " muerto=" + mc.player.isDeadOrDying());
		if (t == 70) { revivir(mc); comando(mc, "setblock 0 199 0 minecraft:stone"); }
		if (t == 74) comando(mc, "tp @s 0 200 0");
		// De costado (esperando a que se pase la protección de revivir).
		if (t == 118) Dedsafio4ClientDebug.info("antes del costado: vivo=" + mc.player.isAlive() + " vida=" + mc.player.getHealth());
		if (t == 120) { comando(mc, "setblock 1 200 0 minecraft:magma_block"); comando(mc, "tp @s 0.71 200 0.5"); }
		if (t == 140) Dedsafio4ClientDebug.info("de costado: muerto=" + mc.player.isDeadOrDying() + " vida=" + mc.player.getHealth());
		if (t == 150) { revivir(mc); comando(mc, "setblock 1 200 0 minecraft:air"); }
		if (t == 154) comando(mc, "tp @s 0.5 200 0.5");
		// Desde abajo: techo de magma y el jugador salta para tocarlo.
		if (t == 198) Dedsafio4ClientDebug.info("antes de saltar: vivo=" + mc.player.isAlive() + " vida=" + mc.player.getHealth());
		if (t == 200) { comando(mc, "setblock 0 202 0 minecraft:magma_block"); comando(mc, "tp @s 0.5 200 0.5"); }
		if (t >= 205 && t < 225 && mc.player.onGround()) mc.player.jumpFromGround();
		if (t == 230) Dedsafio4ClientDebug.info("saltando contra el techo: muerto=" + mc.player.isDeadOrDying() + " vida=" + mc.player.getHealth());
		if (t == 235) { revivir(mc); comando(mc, "setblock 0 202 0 minecraft:air"); comando(mc, "cambio magma 0"); }
		if (t == 245) mc.stop();
	}


	/** Revive al jugador si quedó muerto (el comando /respawn no existe en Minecraft). */
	private static void revivir(Minecraft mc) {
		if (mc.player != null && !mc.player.isAlive()) mc.player.respawn();
	}

	private static final boolean CORAZONES = "corazones".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de la poción Corazones Ocultos: la barra de vida desaparece y vuelve. */
	private static void tickCorazones(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		mc.options.hideGui = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "cambio magma 0");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:stone");
			comando(mc, "fill -6 200 -6 6 204 6 minecraft:air");
			comando(mc, "tp @s 0 200 0");
			comando(mc, "effect clear @s");
			comando(mc, "attribute @s minecraft:generic.max_health base set 20");
			comando(mc, "item replace entity @s armor.chest with minecraft:iron_chestplate");
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(0f); mc.player.setXRot(0f); mc.player.yRotO = 0f; mc.player.xRotO = 0f; }
		if (t == 20) { comando(mc, "effect give @s minecraft:instant_health 1 20 true"); comando(mc, "effect give @s minecraft:instant_damage 1 0 true"); }
		if (t == 30) captura(mc, "debug_co1_normal.png");
		if (t == 35) comando(mc, "effect give @s dedsafio4:corazones_ocultos 5 0");
		if (t == 55) captura(mc, "debug_co2_sin_corazones.png");
		if (t == 60) Dedsafio4ClientDebug.info("con la pocion: vida=" + mc.player.getHealth()
				+ " efecto=" + mc.player.hasEffect(com.dedsafio4.pociones.ModPociones.EFECTO_CORAZONES_OCULTOS));
		if (t == 170) {
			Dedsafio4ClientDebug.info("cuando se pasa: efecto=" + mc.player.hasEffect(com.dedsafio4.pociones.ModPociones.EFECTO_CORAZONES_OCULTOS));
			captura(mc, "debug_co3_vuelven.png");
		}
		if (t == 180) mc.stop();
	}

	private static final boolean DIMENSION = "dimension".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Dónde está el árbol más alto que se encontró, y cuánto mide. */
	private static net.minecraft.core.BlockPos arbol;
	private static int arbolAlto;
	/** En qué tick apareció el árbol encontrado. */
	private static int tickDelArbol;

	/** Prueba de la dimensión: dos biomas, siempre de día, sin bichos y árboles altísimos. */
	private static void tickDimension(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode spectator");
			comando(mc, "weather clear");
			comando(mc, "execute in dedsafio4:dimension_nueva run tp @s 0 150 0");
		}
		if (t == 20) Dedsafio4ClientDebug.info("dimension=" + mc.level.dimension().location()
				+ " hora fija=" + mc.level.dimensionType().fixedTime() + " luz del sol=" + mc.level.getSkyDarken());
		// Busca el árbol más alto en un área grande y anota dónde está.
		if (t == 25) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var nivel = server.getLevel(net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
				if (nivel == null) return;
				java.util.Set<String> biomas = new java.util.HashSet<>();
				int mejor = 0;
				net.minecraft.core.BlockPos donde = null;
				for (int x = -160; x <= 160; x += 1) {
					for (int z = -160; z <= 160; z += 1) {
						if (x % 40 == 0 && z % 40 == 0) {
							nivel.getBiome(new net.minecraft.core.BlockPos(x, 80, z)).unwrapKey()
									.ifPresent(k -> biomas.add(k.location().toString()));
						}
						int alto = 0, primero = -1;
						for (int y = 50; y < 250; y++) {
							var pos = new net.minecraft.core.BlockPos(x, y, z);
							var estado = nivel.getBlockState(pos);
							if (estado.is(com.dedsafio4.bloques.ModBloques.ROBLE_CLARO_LOG)
									|| estado.is(com.dedsafio4.bloques.ModBloques.ABETO_CLARO_LOG)) {
								if (primero < 0) primero = y;
								alto++;
							}
						}
						if (alto > mejor) { mejor = alto; donde = new net.minecraft.core.BlockPos(x, primero, z); }
					}
				}
				arbol = donde;
				arbolAlto = mejor;
				Dedsafio4ClientDebug.info("biomas=" + biomas + " | arbol mas alto=" + mejor + " bloques en " + donde
						+ (donde == null ? "" : " bioma=" + nivel.getBiome(donde).unwrapKey().map(k -> k.location().getPath()).orElse("?")));
			});
		}
		// Se espera a que la búsqueda encuentre el árbol; recién ahí se acomoda la cámara.
		if (arbol != null && tickDelArbol == 0 && t > 30) tickDelArbol = t;
		if (tickDelArbol > 0 && t == tickDelArbol + 2) {
			comando(mc, "execute in dedsafio4:dimension_nueva run tp @s " + (arbol.getX() + 14) + " " + (arbol.getY() + 2) + " " + arbol.getZ());
			mc.options.hideGui = true;
		}
		if (tickDelArbol > 0 && t > tickDelArbol && t < tickDelArbol + 35) {
			mc.player.setYRot(90f); mc.player.setXRot(-45f); mc.player.yRotO = 90f; mc.player.xRotO = -45f;
		}
		if (tickDelArbol > 0 && t == tickDelArbol + 30) captura(mc, "debug_di1_arbol_abajo.png");
		if (tickDelArbol > 0 && t == tickDelArbol + 35) {
			comando(mc, "execute in dedsafio4:dimension_nueva run tp @s " + (arbol.getX() + 45) + " " + (arbol.getY() + arbolAlto / 2) + " " + arbol.getZ());
		}
		if (tickDelArbol > 0 && t > tickDelArbol + 35) {
			mc.player.setYRot(90f); mc.player.setXRot(0f); mc.player.yRotO = 90f; mc.player.xRotO = 0f;
		}
		if (tickDelArbol > 0 && t == tickDelArbol + 65) captura(mc, "debug_di2_arbol_entero.png");
		if (tickDelArbol > 0 && t == tickDelArbol + 75) mc.stop();
		if (t > 400) mc.stop();
	}

	private static final boolean PORTAL = "portal".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del viaje: el Agua Rara lleva y trae, y al llegar se arma solo el círculo del cielo. */
	private static void tickPortal(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty peaceful");
			comando(mc, "execute in minecraft:overworld run tp @s 0 200 0");
			comando(mc, "fill -3 199 -3 3 199 3 minecraft:stone");
			comando(mc, "fill -3 200 -3 3 204 3 minecraft:air");
		}
		if (t >= 3 && t < 400) { mc.player.setYRot(0f); mc.player.setXRot(10f); mc.player.yRotO = 0f; mc.player.xRotO = 10f; }
		if (t == 10) comando(mc, "setblock 2 200 0 dedsafio4:agua_portal");
		if (t == 20) { mc.options.hideGui = true; captura(mc, "debug_pt1_agua.png"); mc.options.hideGui = false; }
		if (t == 25) Dedsafio4ClientDebug.info("antes: dimension=" + mc.level.dimension().location());
		if (t == 30) comando(mc, "tp @s 2.5 200 0.5");
		if (t == 60) Dedsafio4ClientDebug.info("tras tocar el agua: dimension=" + mc.level.dimension().location()
				+ " pos=" + mc.player.blockPosition());
		// El círculo se tiene que haber armado solo, arriba de donde caí.
		if (t == 70) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			if (server != null) server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				if (jugador == null) return;
				var mundo = jugador.serverLevel();
				int cuantos = 0;
				for (int dx = -12; dx <= 12; dx++) {
					for (int dz = -12; dz <= 12; dz++) {
						var pos = new net.minecraft.core.BlockPos(jugador.blockPosition().getX() + dx, 200,
								jugador.blockPosition().getZ() + dz);
						if (mundo.getBlockState(pos).is(com.dedsafio4.bloques.ModBloques.AGUA_PORTAL)) cuantos++;
					}
				}
				Dedsafio4ClientDebug.info("bloques del circulo del cielo=" + cuantos);
			});
		}
		// Foto del círculo desde abajo.
		if (t == 90) { comando(mc, "gamemode spectator"); comando(mc, "tp @s ~ 185 ~"); }
		if (t >= 91 && t < 140) { mc.player.setXRot(-70f); mc.player.xRotO = -70f; }
		if (t == 110) { mc.options.hideGui = true; captura(mc, "debug_pt2_circulo.png"); mc.options.hideGui = false; }
		// Vuelta: tocar el círculo (ya pasaron los 5 segundos de espera).
		if (t == 150) { comando(mc, "gamemode survival"); comando(mc, "tp @s ~ 200 ~"); }
		if (t == 175) Dedsafio4ClientDebug.info("tras tocar el circulo: dimension=" + mc.level.dimension().location()
				+ " pos=" + mc.player.blockPosition());
		if (t == 185) mc.stop();
	}

	private static final boolean EBURIA = "eburia".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Huevo Eburia: crece en el árbol, se cosecha, y las tijeras y el hacha sacan cosas. */
	private static void tickEburia(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "difficulty peaceful");
			comando(mc, "execute in dedsafio4:dimension_nueva run tp @s 0 150 0");
		}
		if (t == 15) comando(mc, "execute in dedsafio4:dimension_nueva run forceload add -16 -16 16 16");
		// Un árbol de sabana armado a mano, para ver la fruta en el tronco.
		if (t == 30) {
			comando(mc, "execute in dedsafio4:dimension_nueva run fill -4 100 -4 4 100 4 minecraft:grass_block");
			comando(mc, "execute in dedsafio4:dimension_nueva run fill -4 101 -4 4 120 4 minecraft:air");
			comando(mc, "execute in dedsafio4:dimension_nueva run fill 0 101 0 1 118 1 dedsafio4:roble_claro_log");
			comando(mc, "execute in dedsafio4:dimension_nueva run tp @s 0 103 -4");
		}
		if (t == 40) {
			for (int i = 0; i < 3; i++) {
				comando(mc, "execute in dedsafio4:dimension_nueva run setblock 0 " + (104 + i * 3)
						+ " -1 dedsafio4:huevo_eburia[facing=north,edad=" + i + "]");
			}
		}
		if (t >= 41 && t < 120) { mc.player.setYRot(0f); mc.player.setXRot(-10f); mc.player.yRotO = 0f; mc.player.xRotO = -10f; }
		if (t == 48) Dedsafio4ClientDebug.info("fruta en el tronco=" + mc.level.getBlockState(
				new net.minecraft.core.BlockPos(0, 104, -1)).getBlock()
				+ " | jugador en " + mc.player.blockPosition());
		if (t == 50) comando(mc, "execute in dedsafio4:dimension_nueva run tp @s 0 106 -4");
		if (t == 52) { mc.player.setXRot(0f); mc.player.xRotO = 0f; }
		if (t == 53) { mc.options.hideGui = true; captura(mc, "debug_eb1_fruta.png"); mc.options.hideGui = false; }
		// Cosecha y usos.
		if (t == 55) {
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:shears");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:iron_axe");
			comando(mc, "item replace entity @s weapon.offhand with dedsafio4:huevo_eburia 3");
		}
        if (t == 60) mc.player.getInventory().selected = 0;
		if (t == 62) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 70) {
			Dedsafio4ClientDebug.info("con tijeras: pelo=" + cuantos(mc, com.dedsafio4.items.ModItems.PELO_EBURIA)
					+ " huevos=" + mc.player.getOffhandItem().getCount());
			mc.player.getInventory().selected = 1;
		}
		if (t == 75) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 85) Dedsafio4ClientDebug.info("con hacha: semillas=" + cuantos(mc, com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
				+ " huevos=" + mc.player.getOffhandItem().getCount());
		// Comerlo: tiene que dar Salud mejorada y +4 de vida máxima.
		if (t == 90) {
			comando(mc, "gamemode survival");
			comando(mc, "effect clear @s");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:huevo_eburia 2");
			comando(mc, "effect give @s minecraft:hunger 1 20 true");
		}
		if (t == 95) mc.player.getInventory().selected = 2;
		if (t == 100) Dedsafio4ClientDebug.info("antes de comer: vida maxima=" + mc.player.getMaxHealth());
		if (t == 102) { mc.options.keyUse.setDown(true); mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND); }
		if (t == 145) mc.options.keyUse.setDown(false);
		if (t == 150) Dedsafio4ClientDebug.info("despues de comer: vida maxima=" + mc.player.getMaxHealth()
				+ " efecto=" + mc.player.hasEffect(com.dedsafio4.eburia.Eburia.SALUD_MEJORADA));
		if (t == 160) mc.stop();
	}

	private static int cuantos(Minecraft mc, net.minecraft.world.item.Item item) {
		return mc.player.getInventory().countItem(item);
	}

	private static final boolean AMBAR = "ambar".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Ámbar: hace falta pico de diamante, suelta el bruto y se cocina. */
	private static void tickAmbar(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty peaceful");
			comando(mc, "execute in minecraft:overworld run tp @s 0 200 0");
			comando(mc, "fill -4 199 -4 4 199 4 minecraft:stone");
			comando(mc, "fill -4 200 -4 4 204 4 minecraft:air");
			comando(mc, "clear @s");
			comando(mc, "kill @e[type=minecraft:item]");
		}
		if (t >= 3 && t < 300) { mc.player.setYRot(0f); mc.player.setXRot(35f); mc.player.yRotO = 0f; mc.player.xRotO = 35f; }
		// Con pico de hierro no tiene que soltar nada.
		if (t == 10) {
			comando(mc, "setblock 0 200 2 dedsafio4:mineral_de_ambar");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:iron_pickaxe");
		}
		if (t == 15) mc.player.getInventory().selected = 0;
		if (t == 20) romperBloque(mc, new net.minecraft.core.BlockPos(0, 200, 2));
		if (t == 35) Dedsafio4ClientDebug.info("con pico de hierro: bruto tirado=" + tirados(mc, com.dedsafio4.items.ModItems.AMBAR_EN_BRUTO)
				+ " bloque=" + mc.level.getBlockState(new net.minecraft.core.BlockPos(0, 200, 2)).getBlock());
		// Con pico de diamante sí.
		if (t == 45) {
			comando(mc, "setblock 0 200 2 dedsafio4:mineral_de_ambar");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:diamond_pickaxe");
		}
		if (t == 55) romperBloque(mc, new net.minecraft.core.BlockPos(0, 200, 2));
		if (t == 75) Dedsafio4ClientDebug.info("con pico de diamante: bruto tirado=" + tirados(mc, com.dedsafio4.items.ModItems.AMBAR_EN_BRUTO)
				+ " bloque=" + mc.level.getBlockState(new net.minecraft.core.BlockPos(0, 200, 2)).getBlock());
		// Cocinarlo.
		if (t == 85) {
			comando(mc, "setblock 1 200 0 minecraft:furnace");
			comando(mc, "item replace block 1 200 0 container.0 with dedsafio4:ambar_en_bruto 3");
			comando(mc, "item replace block 1 200 0 container.1 with minecraft:coal 3");
		}
		if (t == 330) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var mundo = server.overworld();
				if (mundo.getBlockEntity(new net.minecraft.core.BlockPos(1, 200, 0))
						instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity horno) {
					Dedsafio4ClientDebug.info("en el horno: entra=" + horno.getItem(0)
							+ " | sale=" + horno.getItem(2));
				}
			});
		}
		// Foto final: unos minerales puestos en fila.
		if (t == 332) {
			comando(mc, "fill -2 200 2 2 200 2 dedsafio4:mineral_de_ambar");
			comando(mc, "tp @s 0 200.2 -1");
		}
		if (t >= 333 && t < 345) { mc.player.setYRot(0f); mc.player.setXRot(5f); mc.player.yRotO = 0f; mc.player.xRotO = 5f; }
		if (t == 338) { mc.options.hideGui = true; captura(mc, "debug_am1_mineral.png"); mc.options.hideGui = false; }
		if (t == 345) mc.stop();
	}

	/** Cuenta los ítems de ese tipo tirados cerca del jugador. */
	private static int tirados(Minecraft mc, net.minecraft.world.item.Item item) {
		int cuenta = 0;
		for (var e : mc.level.entitiesForRendering()) {
			if (e instanceof net.minecraft.world.entity.item.ItemEntity caido
					&& caido.getItem().is(item)) {
				cuenta += caido.getItem().getCount();
			}
		}
		return cuenta;
	}

	/** Rompe un bloque como si lo picara el jugador (usa el método del servidor). */
	private static void romperBloque(Minecraft mc, net.minecraft.core.BlockPos pos) {
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (server == null) return;
		server.execute(() -> {
			var jugador = server.getPlayerList().getPlayer(uuid);
			if (jugador != null) jugador.gameMode.destroyBlock(pos);
		});
	}

	private static final boolean MINERALES = "minerales".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Cuenta qué hay bajo tierra en la dimensión: tiene que haber sólo ámbar y carbón, y nada de piedra profunda. */
	private static void tickMinerales(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode spectator");
			comando(mc, "execute in dedsafio4:dimension_nueva run tp @s 0 100 0");
		}
		if (t == 60) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var nivel = server.getLevel(net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
				if (nivel == null) return;
				java.util.Map<String, Integer> cuenta = new java.util.TreeMap<>();
				for (int x = -64; x <= 64; x++) {
					for (int z = -64; z <= 64; z++) {
						for (int y = -60; y < 70; y += 1) {
							var estado = nivel.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
							String nombre = net.minecraft.core.registries.BuiltInRegistries.BLOCK
									.getKey(estado.getBlock()).toString();
							if (nombre.contains("ore") || nombre.contains("mineral") || nombre.contains("deepslate")) {
								cuenta.merge(nombre, 1, Integer::sum);
							}
						}
					}
				}
				Dedsafio4ClientDebug.info("bajo tierra: " + cuenta);
			});
		}
		if (t == 140) mc.stop();
	}

	private static final boolean BLAZE = "blaze".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de /cambio blaze 1: cuenta los Blaze que aparecen solos en el Nether, sin y con el cambio. */
	private static void tickBlaze(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "difficulty hard");
			comando(mc, "gamerule doMobSpawning true");
			comando(mc, "cambio blaze 0");
			comando(mc, "execute in minecraft:the_nether run tp @s 500 70 500");
		}
		if (t == 5) comando(mc, "execute in minecraft:the_nether run kill @e[type=minecraft:blaze]");
		if (t == 400) Dedsafio4ClientDebug.info("sin el cambio, 20 s: blazes=" + contarBlazes(mc));
		if (t == 405) { comando(mc, "cambio blaze 1"); comando(mc, "execute in minecraft:the_nether run kill @e[type=minecraft:blaze]"); }
		if (t == 805) Dedsafio4ClientDebug.info("con el cambio, 20 s: blazes=" + contarBlazes(mc));
		if (t == 810) { comando(mc, "cambio blaze 0"); comando(mc, "execute in minecraft:the_nether run kill @e[type=minecraft:blaze]"); }
		if (t == 820) mc.stop();
	}

	private static int contarBlazes(Minecraft mc) {
		int n = 0;
		for (var e : mc.level.entitiesForRendering()) if (e instanceof net.minecraft.world.entity.monster.Blaze) n++;
		return n;
	}

	private static final boolean PERLAS2 = "perlas2".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Tira una Perla del End del jugador hacia el piso, desde el servidor. */
	private static void tirarPerlaServidor(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (server == null) return;
		server.execute(() -> {
			var jugador = server.getPlayerList().getPlayer(uuid);
			if (jugador == null) return;
			var perla = new net.minecraft.world.entity.projectile.ThrownEnderpearl(jugador.level(), jugador);
			perla.setPos(jugador.getX(), jugador.getY() + 1.5, jugador.getZ());
			perla.shoot(0, -1, 0, 1.0f, 0f);
			jugador.level().addFreshEntity(perla);
		});
	}

	/** Prueba de enderperlas: sin cambio 5 de daño, con el nivel 2 hace 10. */
	private static void tickPerlas2(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule naturalRegeneration false");
			comando(mc, "momento revil parar");
			comando(mc, "cambio magma 0");
			comando(mc, "cambio enderperlas 0");
			comando(mc, "execute in minecraft:overworld run tp @s 0 200 0");
			comando(mc, "fill -3 199 -3 3 199 3 minecraft:stone");
			comando(mc, "fill -3 200 -3 3 204 3 minecraft:air");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "attribute @s minecraft:generic.max_health base set 100");
		}
		if (t == 10) comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		if (t == 30) { vidaAntes = mc.player.getHealth(); tirarPerlaServidor(mc); }
		if (t == 60) Dedsafio4ClientDebug.info("sin cambio: danio=" + (vidaAntes - mc.player.getHealth()));
		if (t == 65) comando(mc, "cambio enderperlas 2");
		if (t == 70) comando(mc, "effect give @s minecraft:instant_health 1 20 true");
		if (t == 90) { vidaAntes = mc.player.getHealth(); tirarPerlaServidor(mc); }
		if (t == 120) Dedsafio4ClientDebug.info("con enderperlas 2: danio=" + (vidaAntes - mc.player.getHealth()));
		if (t == 125) comando(mc, "cambio enderperlas 0");
		if (t == 130) mc.stop();
	}

	private static final boolean AYUDA = "ayuda".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de /help <mensaje>: lo manda el jugador (sin permisos) y se ve lo que le aparece en el chat. */
	private static void tickAyuda(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 5) mc.player.connection.sendCommand("help \"Necesito ayuda en la base\"");
		if (t == 15) mc.player.connection.sendCommand("help");
		if (t == 30) captura(mc, "debug_ayuda.png");
		if (t == 40) Dedsafio4ClientDebug.info("ayuda: listo, admin=" + mc.player.hasPermissions(2));
	}

	private static final boolean TOLVA = "tolva".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba: un cofre con candado no le da ni recibe cosas de las tolvas; uno sin candado sí (para comparar). */
	private static void tickTolva(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill 0 198 0 12 204 4 minecraft:air");
			for (int x : new int[]{2, 8}) {
				// tolva arriba (con oro) → cofre (con diamantes) → tolva abajo → cofre de abajo
				comando(mc, "execute in minecraft:overworld run setblock " + x + " 202 2 minecraft:hopper[facing=down]{Items:[{Slot:0b,id:\"minecraft:gold_ingot\",count:5}]}");
				comando(mc, "execute in minecraft:overworld run setblock " + x + " 201 2 minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:10}]}");
				comando(mc, "execute in minecraft:overworld run setblock " + x + " 200 2 minecraft:hopper[facing=down]");
				comando(mc, "execute in minecraft:overworld run setblock " + x + " 199 2 minecraft:chest");
			}
		}
		if (t == 2) server.execute(() -> {
			com.dedsafio4.candados.Candados.data(server.overworld()).poner(new net.minecraft.core.BlockPos(2, 201, 2), "1234", java.util.UUID.randomUUID());
		});
		if (t == 10 || t == 60) server.execute(() -> Dedsafio4ClientDebug.info("tolva t=" + ticks + ": candado=" + com.dedsafio4.candados.Candados.tieneCandado(
				server.overworld(), new net.minecraft.core.BlockPos(2, 201, 2)) + " bloque=" + server.overworld().getBlockState(new net.minecraft.core.BlockPos(2, 201, 2))));
		if (t == 200) server.execute(() -> {
			var w = server.overworld();
			for (int x : new int[]{2, 8}) {
				String nombre = x == 2 ? "CON candado" : "SIN candado";
				var cofre = (net.minecraft.world.Container) w.getBlockEntity(new net.minecraft.core.BlockPos(x, 201, 2));
				var abajo = (net.minecraft.world.Container) w.getBlockEntity(new net.minecraft.core.BlockPos(x, 199, 2));
				var arriba = (net.minecraft.world.Container) w.getBlockEntity(new net.minecraft.core.BlockPos(x, 202, 2));
				Dedsafio4ClientDebug.info("tolva " + nombre + ": diamantes en el cofre=" + cofre.countItem(net.minecraft.world.item.Items.DIAMOND)
						+ " oro en el cofre=" + cofre.countItem(net.minecraft.world.item.Items.GOLD_INGOT)
						+ " | cofre de abajo: diamantes=" + abajo.countItem(net.minecraft.world.item.Items.DIAMOND)
						+ " oro=" + abajo.countItem(net.minecraft.world.item.Items.GOLD_INGOT)
						+ " | oro que quedó en la tolva de arriba=" + arriba.countItem(net.minecraft.world.item.Items.GOLD_INGOT));
			}
		});
	}

	private static final boolean MARCO = "marco".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del marco grande: se pone con el ítem en una pared, se le pone una espada y al lado uno normal. */
	private static void tickMarco(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill -8 199 -1 10 210 12 minecraft:air");
			comando(mc, "execute in minecraft:overworld run fill -8 199 0 10 209 0 minecraft:stone_bricks");
			comando(mc, "execute in minecraft:overworld run fill -8 199 1 10 199 12 minecraft:stone");
			comando(mc, "execute in minecraft:overworld run kill @e[type=dedsafio4:marco_grande]");
			comando(mc, "execute in minecraft:overworld run kill @e[type=minecraft:item_frame]");
			comando(mc, "execute in minecraft:overworld run tp @s 1.5 202 10.5 180 0");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:marco_grande 2");
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(180f); mc.player.setXRot(10f); mc.player.yRotO = 180f; mc.player.xRotO = 10f; }
		if (t == 6) {
			comando(mc, "kill @e[type=dedsafio4:marco_grande]");
			comando(mc, "kill @e[type=minecraft:item_frame]");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "tp @s 1.5 200 4.5 180 0");
		}
		if (t == 25) comando(mc, "tp @s 1.5 202 10.5 180 10");
		if (t == 10) {
			mc.player.getInventory().selected = 0;
			var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(1.5, 201.5, 1.0),
					net.minecraft.core.Direction.SOUTH, new net.minecraft.core.BlockPos(1, 201, 0), false);
			mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		}
		if (t == 20) server.execute(() -> {
			var w = server.overworld();
			var marcos = w.getEntities(com.dedsafio4.marcos.ModMarcos.MARCO_GRANDE, e -> true);
			Dedsafio4ClientDebug.info("marco: grandes=" + marcos.size() + (marcos.isEmpty() ? "" : " caja=" + marcos.get(0).getBoundingBox()));
			for (var m : marcos) m.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			var normal = new net.minecraft.world.entity.decoration.ItemFrame(w, new net.minecraft.core.BlockPos(7, 201, 1), net.minecraft.core.Direction.SOUTH);
			normal.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
			w.addFreshEntity(normal);
			// Uno en el piso, con una manzana.
			var piso = new com.dedsafio4.marcos.MarcoGrandeEntity(w, new net.minecraft.core.BlockPos(-7, 200, 4), net.minecraft.core.Direction.UP);
			Dedsafio4ClientDebug.info("marco en el piso se sostiene=" + piso.survives());
			piso.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE));
			w.addFreshEntity(piso);
		});
		if (t == 40) { mc.options.hideGui = true; captura(mc, "debug_marco1.png"); mc.options.hideGui = false; }
		if (t == 45) comando(mc, "execute in minecraft:overworld run tp @s -1 204 7 200 35");
		if (t >= 46 && t < 200) { mc.player.setYRot(200f); mc.player.setXRot(35f); mc.player.yRotO = 200f; mc.player.xRotO = 35f; }
		if (t == 60) { mc.options.hideGui = true; captura(mc, "debug_marco2.png"); mc.options.hideGui = false; }
		if (t == 66) server.execute(() -> { var w = server.overworld(); for (var m : w.getEntities(com.dedsafio4.marcos.ModMarcos.MARCO_GRANDE, e -> true)) { m.dropItem(null); m.discard(); } Dedsafio4ClientDebug.info("marco: al romperlo quedan items=" + w.getEntities(net.minecraft.world.entity.EntityType.ITEM, e -> true).stream().map(e -> e.getItem().toString()).toList()); });
		if (t == 70) Dedsafio4ClientDebug.info("marco: listo, quedan en la mano=" + mc.player.getMainHandItem().getCount());
	}

	private static final boolean RULETA = "ruleta".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/**
	 * Prueba de las animaciones: /ruleta rojo (gira, cae en rojo y sigue la criatura), /ruleta rosa (sigue la nutria) y
	 * la de muerte (el jugador se mata). Fotos en cada parte.
	 */
	private static void tickRuleta(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 830) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) { comando(mc, "time set day"); comando(mc, "gamemode creative"); }
		if (t == 20) comando(mc, "ruleta rojo");
		if (t == 120) captura(mc, "debug_ruleta1_rojo.png");
		if (t == 280) captura(mc, "debug_ruleta2_rojo_final.png");
		if (t == 360) { captura(mc, "debug_ruleta3_criatura.png"); var r = mc.player.getEffect(com.dedsafio4.pociones.ModPociones.EFECTO_ROJIZO); Dedsafio4ClientDebug.info("ruleta: rojizo=" + (r == null ? "no" : r.getDuration() + " ticks, particulas=" + r.isVisible())); }
		if (t == 470) comando(mc, "ruleta rosa");
		if (t == 600) captura(mc, "debug_ruleta4_rosa.png");
		if (t == 775) captura(mc, "debug_ruleta5_nutria.png");
		if (t == 830) comando(mc, "kill @s");
		if (t == 855) captura(mc, "debug_ruleta6_muerte.png");
		if (t == 920) captura(mc, "debug_ruleta7_muerte.png");
		if (t == 1010) revivir(mc);
		if (t == 1020) Dedsafio4ClientDebug.info("ruleta: listo");
	}

	private static final boolean STRUCK = "struck".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de /struck 1: se arma en el cielo, foto desde arriba y desde adentro del cuadrado chiquito. */
	private static void tickStruck(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "effect give @s minecraft:night_vision 600 0 true");
			comando(mc, "execute in minecraft:overworld run tp @s 1200.5 181 800.5 180 0");
		}
		if (t >= 2 && t < 400) mc.player.getAbilities().flying = true;
		if (t == 40) comando(mc, "struck 1");
		if (t == 45) Dedsafio4ClientDebug.info("struck: jugador en " + mc.player.blockPosition() + " dim=" + mc.level.dimension().location()
				+ " techo=" + mc.level.getBlockState(mc.player.blockPosition().above(5)) + " piso=" + mc.level.getBlockState(mc.player.blockPosition().below()));
		if (t == 255) { mc.player.getAbilities().flying = true; comando(mc, "tp @s 1200.5 330 758.5 180 90"); }
		if (t >= 256 && t < 300) { mc.player.setYRot(180f); mc.player.setXRot(90f); mc.player.yRotO = 180f; mc.player.xRotO = 90f; mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF); }
		if (t == 300) { mc.options.hideGui = true; captura(mc, "debug_struck1.png"); mc.options.hideGui = false; }
		if (t == 305) comando(mc, "tp @s 1200.5 181 806.5 180 -8");
		if (t >= 306 && t < 350) { mc.player.setYRot(180f); mc.player.setXRot(-8f); mc.player.yRotO = 180f; mc.player.xRotO = -8f; }
		if (t == 340) { mc.options.hideGui = true; captura(mc, "debug_struck2.png"); mc.options.hideGui = false; }
		if (t == 345) comando(mc, "tp @s 1200.5 181 754.5 90 5");
		if (t >= 346 && t < 390) { mc.player.setYRot(90f); mc.player.setXRot(5f); mc.player.yRotO = 90f; mc.player.xRotO = 5f; }
		if (t == 380) { mc.options.hideGui = true; captura(mc, "debug_struck3.png"); mc.options.hideGui = false; }
		if (t == 382) Dedsafio4ClientDebug.info("struck: listo");
	}

	private static final boolean CASINO = "casino".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Casino: se pone con el huevo delante del jugador (queda mirándolo) y gira las ruedas. */
	private static void tickCasino(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill -6 199 -6 6 199 6 minecraft:smooth_stone");
			comando(mc, "execute in minecraft:overworld run fill -6 200 -6 6 206 6 minecraft:air");
			comando(mc, "execute in minecraft:overworld run kill @e[type=dedsafio4:casino]");
			comando(mc, "execute in minecraft:overworld run tp @s 1.7 200 4.5 160 10");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:casino_spawn_egg");
		}
		if (t >= 3 && t < 29) { mc.player.setYRot(160f); mc.player.setXRot(35f); mc.player.yRotO = 160f; mc.player.xRotO = 35f; }
		if (t >= 29 && t < 600) { mc.player.setYRot(180f); mc.player.setXRot(10f); mc.player.yRotO = 180f; mc.player.xRotO = 10f; }
		if (t == 30) {
			mc.player.getInventory().selected = 0;
			var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(0.5, 200, 0.5),
					net.minecraft.core.Direction.UP, new net.minecraft.core.BlockPos(0, 199, 0), false);
			mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		}
		if (t == 60) { mc.options.hideGui = true; captura(mc, "debug_casino1.png"); mc.options.hideGui = false; }
		if (t == 65) server.execute(() -> {
			var casinos = server.overworld().getEntities(com.dedsafio4.casino.ModCasino.CASINO, e -> true);
			Dedsafio4ClientDebug.info("casino: hay " + casinos.size() + (casinos.isEmpty() ? "" : " giro=" + casinos.get(0).getYRot()));
			if (System.getenv("DEDSAFIO4_CASINO_ANIM") != null) for (var c : casinos) c.animar(System.getenv("DEDSAFIO4_CASINO_ANIM"));
		});
		if (t == 66) {
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:dedita_casino 3");
			comando(mc, "gamemode survival");
			comando(mc, "casino 1");
			comando(mc, "clear @s minecraft:golden_carrot");
			comando(mc, "clear @s minecraft:golden_apple");
		}
		if (t == 70) {
			mc.player.getInventory().selected = 1;
			var casino = mc.level.getEntitiesOfClass(com.dedsafio4.casino.CasinoEntity.class, mc.player.getBoundingBox().inflate(8)).stream().findFirst();
			casino.ifPresent(c -> mc.gameMode.interact(mc.player, c, net.minecraft.world.InteractionHand.MAIN_HAND));
		}
		if (t == 80) {
			var casino = mc.level.getEntitiesOfClass(com.dedsafio4.casino.CasinoEntity.class, mc.player.getBoundingBox().inflate(8)).stream().findFirst();
			casino.ifPresent(c -> mc.gameMode.interact(mc.player, c, net.minecraft.world.InteractionHand.MAIN_HAND));
		}
		if (t == 90) Dedsafio4ClientDebug.info("casino: deditas que quedan=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.DEDITA_CASINO));
		if (t == 150) { mc.options.hideGui = true; captura(mc, "debug_casino2.png"); mc.options.hideGui = false; }
		if (t == 160) comando(mc, "tp @s 3.5 200 2.5 125 10");
		if (t >= 161 && t < 600) { mc.player.setYRot(125f); mc.player.setXRot(10f); mc.player.yRotO = 125f; mc.player.xRotO = 10f; }
		if (t == 180) { mc.options.hideGui = true; captura(mc, "debug_casino3.png"); mc.options.hideGui = false; }
		if (t == 250) comando(mc, "tp @s 0.5 200 2.5 180 20");
		if (t >= 251 && t < 600) { mc.player.setYRot(180f); mc.player.setXRot(20f); mc.player.yRotO = 180f; mc.player.xRotO = 20f; }
		if (t == 70 + 206) { mc.options.hideGui = true; captura(mc, "debug_casino4.png"); mc.options.hideGui = false; }
		if (t == 70 + 206) Dedsafio4ClientDebug.info("casino: items en el piso cerca=" + mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mc.player.getBoundingBox().inflate(6)).stream().map(e -> e.getItem().toString()).toList());
		if (t == 70 + 216) { StringBuilder s = new StringBuilder(); for (var it : mc.player.getInventory().items) if (!it.isEmpty()) s.append(it).append(it.getEnchantments().isEmpty() ? "" : " encantado").append("; "); Dedsafio4ClientDebug.info("casino: inventario " + s); }
		if (t == 70 + 195) Dedsafio4ClientDebug.info("casino: antes de parar, zanahorias=" + mc.player.getInventory().countItem(net.minecraft.world.item.Items.GOLDEN_CARROT));
		if (t == 70 + 215) Dedsafio4ClientDebug.info("casino: despues de parar, zanahorias=" + mc.player.getInventory().countItem(net.minecraft.world.item.Items.GOLDEN_CARROT)
				+ " manzanas=" + mc.player.getInventory().countItem(net.minecraft.world.item.Items.GOLDEN_APPLE));
		if (t == 70 + 220) Dedsafio4ClientDebug.info("casino: listo");
	}

	private static final boolean PREMIOS_CASINO = "premios".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de los premios del casino: sin /casino no da nada; con /casino 1, cada figura con 2 y con 3. */
	private static void tickPremios(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == 0) { comando(mc, "gamemode survival"); comando(mc, "casino 0"); comando(mc, "clear @s"); }
		if (t == 10) server.execute(() -> {
			var j = server.getPlayerList().getPlayers().get(0);
			com.dedsafio4.casino.CasinoPremios.dar(j, "hierro", 3);
			Dedsafio4ClientDebug.info("premios: con /casino 0 -> items=" + j.getInventory().items.stream().filter(s -> !s.isEmpty()).count());
		});
		if (t == 20) comando(mc, "casino 1");
		String[] figuras = {"hierro", "botella de experiencia", "pechera de hierro", "pico de hierro", "libro de encantamientos", "filete", "poción", "corazon"};
		for (int k = 0; k < figuras.length * 2; k++) {
			final String figura = figuras[k / 2];
			final int cantidad = 2 + k % 2;
			if (t == 30 + k * 4) server.execute(() -> {
				var j = server.getPlayerList().getPlayers().get(0);
				j.getInventory().clearContent();
				com.dedsafio4.casino.CasinoPremios.dar(j, figura, cantidad);
				StringBuilder s = new StringBuilder();
				for (var it : j.getInventory().items) if (!it.isEmpty()) s.append(it.getCount()).append("x").append(it.getItem()).append(it.getEnchantments().isEmpty() ? "" : it.getEnchantments().toString())
						.append(it.has(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS) ? it.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS).toString() : "")
						.append(it.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS) ? it.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).potion().map(h -> h.getRegisteredName()).orElse("") : "").append("  ");
				Dedsafio4ClientDebug.info("premios: " + cantidad + " x " + figura + " -> " + s);
			});
		}
		if (t == 110) Dedsafio4ClientDebug.info("premios: listo");
	}

	private static final boolean PARTES_NAVE = "partes".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de las partes de la nave: en la barra, en el inventario, en la mano, en el piso y en marcos. */
	private static void tickPartes(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill -6 199 -6 6 199 6 minecraft:smooth_stone");
			comando(mc, "execute in minecraft:overworld run fill -6 200 -6 6 206 6 minecraft:air");
			comando(mc, "execute in minecraft:overworld run fill -6 200 -2 6 204 -2 minecraft:oak_planks");
			comando(mc, "execute in minecraft:overworld run kill @e[type=minecraft:item_frame]");
			comando(mc, "execute in minecraft:overworld run kill @e[type=minecraft:item]");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 3.5 180 15");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:nave_cabina");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:nave_motor");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:nave_aleron_izquierdo");
			comando(mc, "item replace entity @s hotbar.3 with dedsafio4:nave_aleron_derecho");
			comando(mc, "item replace entity @s hotbar.4 with dedsafio4:nave_biplaza");
		}
		if (t >= 3 && t < 300) { mc.player.setYRot(180f); mc.player.setXRot(15f); mc.player.yRotO = 180f; mc.player.xRotO = 15f; }
		if (t == 10) {
			String[] partes = {"nave_cabina", "nave_motor", "nave_aleron_izquierdo", "nave_aleron_derecho"};
			for (int k = 0; k < 4; k++) {
				comando(mc, "execute in minecraft:overworld run summon minecraft:item_frame " + (-3 + k * 2) + " 202 -1 {Facing:3b,Item:{id:\"dedsafio4:" + partes[k] + "\",count:1}}");
				comando(mc, "execute in minecraft:overworld run summon minecraft:item " + (-3 + k * 2) + ".5 200 0.5 {Item:{id:\"dedsafio4:" + partes[k] + "\",count:1},PickupDelay:32767,Age:-32768}");
			}
		}
		if (t == 20) mc.player.getInventory().selected = 0;
		if (t == 40) captura(mc, "debug_partes1.png");
		if (t == 45) mc.player.getInventory().selected = 1;
		if (t == 55) captura(mc, "debug_partes2.png");
		if (t == 60) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		if (t == 70) captura(mc, "debug_partes3.png");
		if (t == 71 && mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
			var lineas = net.minecraft.world.item.ItemStack.EMPTY.isEmpty() ? mc.player.getInventory().getItem(4).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, net.minecraft.world.item.TooltipFlag.NORMAL) : null;
			for (var l : lineas) Dedsafio4ClientDebug.info("partes: tooltip " + l.getString());
		}
		if (t == 75) { mc.setScreen(null); Dedsafio4ClientDebug.info("partes: listo"); }
	}

	private static final boolean SIN_ALMA = "sinalma".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Sin Alma: click derecho con él y se tiene que abrir la G. */
	private static void tickSinAlma(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == 0) { if (mc.screen != null) mc.setScreen(null); comando(mc, "item replace entity @s hotbar.0 with dedsafio4:" + (System.getenv("DEDSAFIO4_ITEM") != null ? System.getenv("DEDSAFIO4_ITEM") : "sin_alma")); }
		if (t == 10) { mc.player.getInventory().selected = 0; mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND); }
		if (t == 20) { Dedsafio4ClientDebug.info("sinalma: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_sinalma.png"); }
		if (t == 22) Dedsafio4ClientDebug.info("sinalma: listo");
	}

	private static final boolean BANEOS = "baneos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Bloque de Baneados: baneados de mentira, abrir el bloque, elegir uno y desbanearlo con una cuchara. */
	private static void tickBaneos(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill -4 199 -4 4 199 4 minecraft:smooth_stone");
			comando(mc, "execute in minecraft:overworld run fill -4 200 -4 4 204 4 minecraft:air");
			comando(mc, "execute in minecraft:overworld run setblock 0 200 -1 dedsafio4:bloque_baneados");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 1.5 180 20");
			comando(mc, "item replace entity @s weapon.mainhand with dedsafio4:cuchara_hojas");
			server.execute(() -> {
				var bans = server.getPlayerList().getBans();
				for (var e : new java.util.ArrayList<>(bans.getEntries())) bans.remove(e);
				String[] nombres = {"Notch", "jeb_", "Dinnerbone", "Daniel_Z", "SrAmilca", "Werever", "DraCher", "chibidoki", "Brillager", "ANTONIc", "FairlyGa", "TiroLoco"};
				String[] ids = {"069a79f4-44e9-4726-a5be-fca90e38aaf5", "853c80ef-3c37-49fd-aa49-938b674adae6", "61699b2e-d327-4a01-9f1e-0ea8c3f06bc6"};
				for (int k = 0; k < nombres.length; k++) {
					java.util.UUID uuid = k < ids.length ? java.util.UUID.fromString(ids[k]) : java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + nombres[k]).getBytes());
					bans.add(new net.minecraft.server.players.UserBanListEntry(new com.mojang.authlib.GameProfile(uuid, nombres[k])));
				}
				Dedsafio4ClientDebug.info("baneos: baneados=" + bans.getEntries().size());
			});
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(180f); mc.player.setXRot(20f); mc.player.yRotO = 180f; mc.player.xRotO = 20f; }
		if (t == 20) {
			var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(0.5, 200.5, 0.0),
					net.minecraft.core.Direction.SOUTH, new net.minecraft.core.BlockPos(0, 200, -1), false);
			mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		}
		if (t == 40) { Dedsafio4ClientDebug.info("baneos: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())); captura(mc, "debug_baneos1.png"); }
		if (t == 45 && mc.screen instanceof BaneadosScreen p) {
			int x0 = (p.width - 250) / 2, y0 = (p.height - 208) / 2;
			p.mouseClicked(x0 + 40, y0 + 15 + 17 * 3 + 8, 0);   // la 4ª fila
		}
		if (t == 55) captura(mc, "debug_baneos2.png");
		if (t == 60 && mc.screen instanceof BaneadosScreen p) {
			int x0 = (p.width - 250) / 2, y0 = (p.height - 208) / 2;
			p.mouseClicked(x0 + 146 + 40, y0 + 108 + 10, 0);   // el botón
		}
		if (t == 80) {
			Dedsafio4ClientDebug.info("baneos: cucharas en la mano=" + mc.player.getMainHandItem().getCount());
			server.execute(() -> Dedsafio4ClientDebug.info("baneos: baneados despues=" + server.getPlayerList().getBans().getEntries().size()
					+ " sigue DraCher? " + java.util.Arrays.asList(server.getPlayerList().getBans().getUserList()).contains("DraCher")));
			captura(mc, "debug_baneos3.png");
		}
		if (t == 85) Dedsafio4ClientDebug.info("baneos: listo");
	}

	private static final boolean DENTADURA = "dentadura".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de la Dentadura Glebanoide: puesta mirando a distintos lados, y su ítem en la mano. */
	private static void tickDentadura(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill -6 199 -6 6 199 6 minecraft:smooth_stone");
			comando(mc, "execute in minecraft:overworld run fill -6 200 -6 6 205 6 minecraft:air");
			comando(mc, "execute in minecraft:overworld run setblock -3 200 -2 dedsafio4:bloque_dientes[facing=south]");
			comando(mc, "execute in minecraft:overworld run setblock 3 200 -2 dedsafio4:bloque_dientes[facing=east]");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 201 4.5 180 20");
			comando(mc, "item replace entity @s weapon.mainhand with dedsafio4:bloque_dientes");
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(180f); mc.player.setXRot(20f); mc.player.yRotO = 180f; mc.player.xRotO = 20f; }
		if (t == 40) captura(mc, "debug_dentadura.png");
		if (t == 42) Dedsafio4ClientDebug.info("dentadura: listo");
	}

	private static final boolean SACO = "saco".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Saco: se abre con click derecho y tiene que tener 36 lugares (4 filas). */
	private static void tickSaco(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode survival");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:saco");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:diamond 5");
		}
		if (t == 10) { mc.player.getInventory().selected = 0; mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND); }
		if (t == 25) {
			var menu = mc.player.containerMenu;
			Dedsafio4ClientDebug.info("saco: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())
					+ " lugares del saco=" + (menu.slots.size() - 36) + " titulo=" + (mc.screen == null ? "" : mc.screen.getTitle().getString()));
			captura(mc, "debug_saco.png");
		}
		if (t == 27) Dedsafio4ClientDebug.info("saco: listo");
	}

	private static final boolean CARNE_GLEBA = "carneglebanoide".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de la Carne Glebanoide: con Levitación, comerla la saca y da Caída Lenta; en el horno da Oro en Bruto. */
	private static void tickCarneGleba(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode survival");
			comando(mc, "effect clear @s");
			comando(mc, "effect give @s minecraft:levitation 30 0");
			comando(mc, "item replace entity @s weapon.mainhand with dedsafio4:carne_glebanoide 2");
		}
		if (t == 10) mc.options.keyUse.setDown(true);
		if (t == 60) {
			mc.options.keyUse.setDown(false);
			Dedsafio4ClientDebug.info("carneglebanoide: quedan=" + mc.player.getMainHandItem().getCount());
			server.execute(() -> {
				var j = server.getPlayerList().getPlayers().get(0);
				Dedsafio4ClientDebug.info("carneglebanoide: levitacion=" + j.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)
						+ " caida lenta=" + j.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING));
				var horno = server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
						new net.minecraft.world.item.crafting.SingleRecipeInput(new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.CARNE_GLEBANOIDE)), j.level());
				Dedsafio4ClientDebug.info("carneglebanoide: en el horno da " + horno.map(r -> r.value().getResultItem(j.registryAccess()).toString()).orElse("nada"));
			});
		}
		if (t == 70) Dedsafio4ClientDebug.info("carneglebanoide: listo");
	}

	private static final boolean ARCO_GLEBA = "arcoglebanoide".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del Arco Glebanoide: una flecha al primer zombi tiene que rebotar a los otros dos. */
	private static void tickArcoGleba(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run forceload add -16 -16 16 16");
			comando(mc, "execute in minecraft:overworld run fill -8 199 -12 8 199 8 minecraft:smooth_stone");
			comando(mc, "execute in minecraft:overworld run fill -8 200 -12 8 206 8 minecraft:air");
			comando(mc, "execute in minecraft:overworld run kill @e[type=minecraft:zombie]");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 6.5 180 0");
			for (int k = 0; k < 3; k++) comando(mc, "execute in minecraft:overworld run summon minecraft:zombie " + (k * 4 - 4) + ".5 200 -4.5 {NoAI:1b,PersistenceRequired:1b,Health:20f}");
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(180f); mc.player.setXRot(5f); mc.player.yRotO = 180f; mc.player.xRotO = 5f; }
		if (t == 20) server.execute(() -> {
			var j = server.getPlayerList().getPlayers().get(0);
			var w = server.overworld();
			var flecha = new net.minecraft.world.entity.projectile.Arrow(w, j, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),
					new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.ARCO_GLEBANOIDE));
			flecha.setPos(-3.5, 201.5, 4.0);
			flecha.shoot(0, 0, -1, 2.5f, 0f);
			flecha.setBaseDamage(2.0);
			w.addFreshEntity(flecha);
		});
		if (t == 28) captura(mc, "debug_arco1.png");
		if (t == 70) server.execute(() -> {
			var w = server.overworld();
			for (var z : w.getEntities(net.minecraft.world.entity.EntityType.ZOMBIE, e -> true)) {
				Dedsafio4ClientDebug.info("arcoglebanoide: zombi en x=" + Math.round(z.getX()) + " vida=" + z.getHealth());
			}
		});
		if (t == 75) Dedsafio4ClientDebug.info("arcoglebanoide: listo");
	}

	private static final boolean ORGANOS_CORTE = "organoscorte".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba de los colores de los Órganos: lugar nuevo, foto desde arriba y un corte del terreno para ver adentro. */
	private static void tickOrganosCorte(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode creative");
			comando(mc, "effect give @s minecraft:night_vision 600 0 true");
			comando(mc, "execute in dedsafio4:organos run tp @s " + (System.getenv("DEDSAFIO4_X") != null ? System.getenv("DEDSAFIO4_X") : "9000.5") + " 150 9030.5 180 35");
		}
		if (t >= 2 && t < 400) { mc.player.getAbilities().flying = true; }
		if (t == 120 && System.getenv("DEDSAFIO4_X") == null) comando(mc, "execute in dedsafio4:organos run fill 8988 30 9004 9013 160 9012 minecraft:air");
		if (t == 120 && System.getenv("DEDSAFIO4_X") != null) comando(mc, "execute in dedsafio4:organos run spreadplayers " + System.getenv("DEDSAFIO4_X") + " 9030.5 0 1 false @s");
		if (t == 140 && System.getenv("DEDSAFIO4_X") != null) comando(mc, "tp @s ~ ~6 ~");
		if (t >= 3 && t < 400) { mc.player.setYRot(180f); mc.player.setXRot(35f); mc.player.yRotO = 180f; mc.player.xRotO = 35f; }
		if (t == 100) captura(mc, "debug_organos1.png");
		if (t == 200) captura(mc, "debug_organos2.png");
		if (t == 195) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var w = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "organos")));
				if (w == null) return;
				var centro = server.getPlayerList().getPlayers().get(0).blockPosition();
				java.util.Map<String, Integer> cuenta = new java.util.TreeMap<>();
				int columnas = 0;
				for (int x = -64; x <= 64; x++) for (int z = -64; z <= 64; z++) {
					if (!w.hasChunkAt(centro.offset(x, 0, z))) continue;
					columnas++;
					for (int y = 40; y < 160; y++) {
						var b = w.getBlockState(new net.minecraft.core.BlockPos(centro.getX() + x, y, centro.getZ() + z)).getBlock();
						String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
						if (id.equals("pasto_rosa") || id.equals("bloque_carne") || id.equals("bloque_carne_rosa") || id.equals("bloque_dientes"))
							cuenta.merge(id, 1, Integer::sum);
					}
				}
				Dedsafio4ClientDebug.info("organoscorte: en " + columnas + " columnas: " + cuenta);
			});
		}
		if (t == 202) Dedsafio4ClientDebug.info("organoscorte: listo");
	}

	private static final boolean COFRE_NUEVO = "cofre".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Prueba del cofre nuevo: se pone, se abre con la tapa animada, guarda cosas y se rompe entero. */
	private static void tickCofre(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && (t < 38 || t > 95)) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "execute in minecraft:overworld run tp @s 0 200 -3");
			comando(mc, "fill -4 199 -4 4 199 4 minecraft:stone");
			comando(mc, "fill -4 200 -4 4 204 4 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
		}
		if (t >= 3 && t < 200) { mc.player.setYRot(0f); mc.player.setXRot(25f); mc.player.yRotO = 0f; mc.player.xRotO = 25f; }
		// Mirando al norte (hacia +Z el jugador está atrás... se pone con el frente hacia el jugador).
		if (t == 10) comando(mc, "setblock 0 200 0 dedsafio4:cofre_protegido[facing=north,parte=principal]");
		if (t == 11) comando(mc, "setblock 1 200 0 dedsafio4:cofre_protegido[facing=north,parte=secundaria]");
		if (t == 30) { mc.options.hideGui = true; captura(mc, "debug_co1_cerrado.png"); mc.options.hideGui = false; }
		// Abrirlo.
		if (t == 40) {
			var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(
					new net.minecraft.core.BlockPos(0, 200, 0)), net.minecraft.core.Direction.NORTH,
					new net.minecraft.core.BlockPos(0, 200, 0), false);
			mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		}
		if (t == 60) Dedsafio4ClientDebug.info("al abrir: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName()
				+ " espacios=" + (mc.player.containerMenu.slots.size() - 36)));
		if (t == 62) { mc.options.hideGui = true; captura(mc, "debug_co2_menu.png"); mc.options.hideGui = false; }
		if (t == 70) mc.setScreen(null);
		// Tapa abierta: se fuerza el estado abierto para la foto.
		if (t == 72) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> server.overworld().blockEvent(new net.minecraft.core.BlockPos(0, 200, 0),
					com.dedsafio4.cofres.ModCofres.COFRE, 1, 1));
		}
		if (t == 95) { mc.options.hideGui = true; captura(mc, "debug_co3_abierto.png"); mc.options.hideGui = false; }
		// Guardar algo y romperlo: tienen que salir el cofre y lo guardado, y desaparecer las dos mitades.
		if (t == 100) comando(mc, "item replace block 0 200 0 container.0 with minecraft:diamond 5");
		if (t == 105) { comando(mc, "gamemode survival"); comando(mc, "item replace entity @s hotbar.0 with minecraft:diamond_pickaxe"); }
		if (t == 108) romperBloque(mc, new net.minecraft.core.BlockPos(1, 200, 0));
		if (t == 125) {
			Dedsafio4ClientDebug.info("tras romper: principal=" + mc.level.getBlockState(new net.minecraft.core.BlockPos(0, 200, 0)).getBlock()
					+ " secundaria=" + mc.level.getBlockState(new net.minecraft.core.BlockPos(1, 200, 0)).getBlock()
					+ " diamantes tirados=" + tirados(mc, net.minecraft.world.item.Items.DIAMOND)
					+ " cofres tirados=" + tirados(mc, com.dedsafio4.cofres.ModCofres.COFRE_ITEM));
		}
		if (t == 135) mc.stop();
	}

	private static final boolean COFRE_CANDADO = "cofrecandado".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void clickEn(Minecraft mc, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction cara) {
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), cara, pos, false);
		mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
	}

	private static void mirar(Minecraft mc, float yaw, float pitch) {
		mc.player.setYRot(yaw); mc.player.setXRot(pitch); mc.player.yRotO = yaw; mc.player.xRotO = pitch;
	}

	/** Cofres con candado: el simple se ve como el Cofre Chico y el doble como el Cofre Oscuro. */
	private static void tickCofreCandado(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof CandadoScreen)) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var simple = new net.minecraft.core.BlockPos(-2, 200, 0);
		var izq = new net.minecraft.core.BlockPos(1, 200, 0);
		var der = new net.minecraft.core.BlockPos(2, 200, 0);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:stone");
			comando(mc, "fill -6 200 -6 6 204 6 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "clear @s");
			comando(mc, "tp @s 0 200 -2.5");
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var datos = com.dedsafio4.candados.Candados.data(server.overworld());
				datos.posiciones().forEach(datos::sacar);
				com.dedsafio4.candados.Candados.avisarCambio(server.overworld());
			});
		}
		if (t == 5) {
			comando(mc, "setblock -2 200 0 minecraft:chest[facing=north,type=single]");
			comando(mc, "setblock 1 200 0 minecraft:chest[facing=north,type=left]");
			comando(mc, "setblock 2 200 0 minecraft:chest[facing=north,type=right]");
			comando(mc, "setblock 5 200 0 minecraft:chest[facing=north,type=single]");
			comando(mc, "setblock -6 200 0 dedsafio4:cofre_protegido[facing=north,parte=principal]");
			comando(mc, "setblock -5 200 0 dedsafio4:cofre_protegido[facing=north,parte=secundaria]");
		}
		if (t >= 3 && t < 58) mirar(mc, 0f, 20f);
		if (t == 12) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 15) captura(mc, "debug_cc0_antes.png");
		if (t == 16) mc.options.hideGui = false;
		// Ponerle el candado a los dos con la pantalla, como un jugador.
		if (t >= 10 && t <= 60 && t % 4 == 0 && mc.level.getBlockEntity(simple) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c) {
			var server = mc.getSingleplayerServer();
			var jugadorS = server == null ? null : server.getPlayerList().getPlayer(mc.player.getUUID());
			Dedsafio4ClientDebug.info("t=" + t + " tapa simple=" + c.getOpenNess(1f) + " pantalla=" + (mc.screen == null ? "-" : mc.screen.getClass().getSimpleName())
					+ " menu servidor=" + (jugadorS == null ? "?" : jugadorS.containerMenu.getClass().getSimpleName()) + " menu cliente=" + mc.player.containerMenu.getClass().getSimpleName());
		}
		if (t == 18) { comando(mc, "item replace entity @s hotbar.0 with dedsafio4:candado 4"); mc.player.getInventory().selected = 0; }
		if (t == 22) clickEn(mc, simple, net.minecraft.core.Direction.NORTH);
		if (t == 30) escribir(mc, "1234");
		if (t == 38) clickEn(mc, der, net.minecraft.core.Direction.NORTH);
		if (t == 46) escribir(mc, "5678");
		if (t == 55) Dedsafio4ClientDebug.info("cliente sabe: " + com.dedsafio4.candados.Candados.cerradosCliente
				+ " simple=" + com.dedsafio4.candados.Candados.tieneCandado(mc.level, simple)
				+ " izq=" + com.dedsafio4.candados.Candados.tieneCandado(mc.level, izq)
				+ " der=" + com.dedsafio4.candados.Candados.tieneCandado(mc.level, der)
				+ " suelto=" + com.dedsafio4.candados.Candados.tieneCandado(mc.level, new net.minecraft.core.BlockPos(5, 200, 0)));
		if (t == 54) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 57) captura(mc, "debug_cc1_cerrados.png");
		if (t == 58) mc.options.hideGui = false;
		// De costado.
		if (t == 60) comando(mc, "tp @s -5 201 -3");
		if (t >= 61 && t < 70) mirar(mc, -60f, 25f);
		if (t == 65) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 68) captura(mc, "debug_cc2_costado.png");
		if (t == 69) mc.options.hideGui = false;
		// Abiertos.
		if (t == 70) {
			comando(mc, "tp @s 0 200 -2.5");
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				for (var p : new net.minecraft.core.BlockPos[]{simple, izq, der}) {
					server.overworld().blockEvent(p, net.minecraft.world.level.block.Blocks.CHEST, 1, 1);
				}
				server.overworld().blockEvent(new net.minecraft.core.BlockPos(-6, 200, 0), com.dedsafio4.cofres.ModCofres.COFRE, 1, 1);
			});
		}
		if (t >= 71 && t < 125) mirar(mc, 0f, 20f);
		if (t == 89) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 92) captura(mc, "debug_cc3_abiertos.png");
		if (t == 93) mc.options.hideGui = false;
		if (t == 94) {
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				for (var p : new net.minecraft.core.BlockPos[]{simple, izq, der}) {
					server.overworld().blockEvent(p, net.minecraft.world.level.block.Blocks.CHEST, 1, 0);
				}
			});
		}
		if (t == 95) comando(mc, "tp @s -5 200 -3");
		if (t == 96) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 99) captura(mc, "debug_cc5_protegido.png");
		if (t == 100) mc.options.hideGui = false;
		// Un cofre nuevo al lado del que tiene candado no se tiene que juntar.
		if (t == 100) { comando(mc, "tp @s -3 200 -2 0 30"); comando(mc, "item replace entity @s hotbar.1 with minecraft:chest"); mc.player.getInventory().selected = 1; }
		if (t == 110) clickEn(mc, new net.minecraft.core.BlockPos(-3, 199, 0), net.minecraft.core.Direction.UP);
		if (t == 118) Dedsafio4ClientDebug.info("al lado: nuevo=" + mc.level.getBlockState(new net.minecraft.core.BlockPos(-3, 200, 0))
				+ " con candado=" + mc.level.getBlockState(simple));
		// El dueño rompe la mitad que guardaba el candado: la otra mitad lo conserva.
		if (t == 122) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			if (server != null) server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				boolean rompio = jugador != null && jugador.gameMode.destroyBlock(izq);
				var datos = com.dedsafio4.candados.Candados.data(server.overworld());
				Dedsafio4ClientDebug.info("rompio mitad=" + rompio + " candado en izq=" + (datos.cerradura(izq) != null)
						+ " en der=" + (datos.cerradura(der) != null));
			});
		}
		if (t == 140) {
			Dedsafio4ClientDebug.info("cliente tras romper: " + com.dedsafio4.candados.Candados.cerradosCliente
					+ " der=" + com.dedsafio4.candados.Candados.tieneCandado(mc.level, der));
		}
		if (t == 150) mc.stop();
	}

	private static final boolean BLOQUES_COLOR = "bloques".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Los cuatro bloques animados y el sonido del cofre con candado. */
	private static void tickBloquesColor(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof CandadoScreen)) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var cofre = new net.minecraft.core.BlockPos(0, 200, 2);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -2.5 0 15");
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:stone");
			comando(mc, "fill -6 200 -6 6 204 6 minecraft:air");
			comando(mc, "clear @s");
			comando(mc, "tp @s 0.5 200 -2.5");
			var server = mc.getSingleplayerServer();
			if (server != null) server.execute(() -> {
				var datos = com.dedsafio4.candados.Candados.data(server.overworld());
				datos.posiciones().forEach(datos::sacar);
			});
			mc.getSoundManager().addListener((sonido, rango, distancia) -> {
				String id = sonido.getLocation().toString();
				if (id.contains("chest") || id.contains("cofre")) Dedsafio4ClientDebug.info("t=" + (ticks - 20) + " pantalla=" + Minecraft.getInstance().screen + " sonido: " + id + " en " + sonido.getX() + "," + sonido.getY() + "," + sonido.getZ());
			});
		}
		if (t == 5) {
			comando(mc, "setblock 2 200 1 dedsafio4:bloque_celeste");
			comando(mc, "setblock 1 200 1 dedsafio4:bloque_amarillo");
			comando(mc, "setblock -0 201 1 dedsafio4:bloque_verde");
			comando(mc, "setblock -1 200 1 dedsafio4:bloque_rojo");
			comando(mc, "setblock 2 201 1 dedsafio4:bloque_rojo");
			comando(mc, "setblock -1 201 1 dedsafio4:bloque_celeste");
			comando(mc, "setblock -2 200 1 minecraft:lime_wool");
			comando(mc, "setblock 3 200 1 minecraft:red_wool");
		}
		if (t >= 3) mirar(mc, 0f, 15f);
		if (t == 16) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 20) captura(mc, "debug_bc1.png");
		if (t == 21) comando(mc, "time set midnight");
		if (t == 55) captura(mc, "debug_bc2.png");
		if (t == 57) { mc.options.hideGui = false; comando(mc, "time set day"); }
		if (t == 60) Dedsafio4ClientDebug.info("nombres: " + new net.minecraft.world.item.ItemStack(com.dedsafio4.bloques.ModBloques.BLOQUE_CELESTE_ITEM).getHoverName().getString()
				+ ", " + new net.minecraft.world.item.ItemStack(com.dedsafio4.bloques.ModBloques.BLOQUE_ROJO_ITEM).getHoverName().getString());
		// Cofre con candado: tiene que sonar el sonido del usuario al abrir y cerrar.
		if (t == 65) comando(mc, "setblock 0 200 2 minecraft:chest[facing=north]");
		if (t == 45) { comando(mc, "item replace entity @s hotbar.0 with dedsafio4:candado"); mc.player.getInventory().selected = 0; }
		if (t == 50) clickEn(mc, cofre, net.minecraft.core.Direction.NORTH);
		if (t == 58) escribir(mc, "1234");
		if (t == 70) { Dedsafio4ClientDebug.info("--- abrir con candado ---"); clickEn(mc, cofre, net.minecraft.core.Direction.NORTH); }
		if (t == 90) { Dedsafio4ClientDebug.info("--- cerrar ---"); mc.player.closeContainer(); }
		// Sin candado: sonido normal.
		if (t == 110) { comando(mc, "setblock 3 200 3 minecraft:chest[facing=north]"); comando(mc, "tp @s 3.5 200 0.5"); }
		if (t == 115) { Dedsafio4ClientDebug.info("--- cofre sin candado ---"); clickEn(mc, new net.minecraft.core.BlockPos(3, 200, 3), net.minecraft.core.Direction.NORTH); }
		if (t == 130) mc.player.closeContainer();
		if (t == 68) mc.stop();
	}

	private static final boolean PIXELES = "pixeles".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** El Bloque de Píxeles: una cruz negra por cara y el resto transparente. */
	private static void tickPixeles(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:grass_block");
			comando(mc, "fill -6 200 -6 6 204 6 minecraft:air");
			comando(mc, "clear @s");
			comando(mc, "tp @s -1.5 201.2 -1.5");
		}
		if (t == 5) {
			comando(mc, "setblock 0 200 1 dedsafio4:bloque_pixeles");
			comando(mc, "setblock 1 200 1 dedsafio4:bloque_pixeles");
			comando(mc, "setblock 1 201 1 dedsafio4:bloque_pixeles");
			comando(mc, "setblock 2 200 2 minecraft:red_wool");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:bloque_pixeles");
			mc.player.getInventory().selected = 0;
		}
		if (t >= 3 && t < 40) mirar(mc, -35f, 25f);
		if (t == 20) mc.gui.getChat().clearMessages(false);
		if (t == 25) captura(mc, "debug_px1.png");
		if (t == 26) Dedsafio4ClientDebug.info("nombre: " + mc.player.getMainHandItem().getHoverName().getString());
		if (t == 30) comando(mc, "tp @s 0.5 200 -1.2");
		if (t >= 31 && t < 40) mirar(mc, 0f, 10f);
		if (t == 38) captura(mc, "debug_px2.png");
		if (t == 45) mc.stop();
	}

	private static final boolean MONITOR = "monitor".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void robotAhi(Minecraft mc, String como) {
		comando(mc, "tp @e[type=dedsafio4:aldeano_robot,limit=1] " + como);
	}

	/** El Aldeano Robot nuevo (Monitor Mob) y su bloque dormido. */
	private static void tickMonitor(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && (t < 95 || t > 110)) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == -4) comando(mc, "kill @e[type=dedsafio4:aldeano_robot]");
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0.5 200 -4.5");
			comando(mc, "clear @s");
		}
		if (t >= 3) mirar(mc, 0f, 5f);
		if (t == 5) {
			comando(mc, "summon dedsafio4:aldeano_robot 0.5 200 0.5 {NoAI:1b,Rotation:[180f,0f]}");
			comando(mc, "setblock 2 200 0 dedsafio4:robot_dormido[facing=north]");
			comando(mc, "setblock -1 200 0 minecraft:oak_planks");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:robot_dormido");
		}
		if (t == 25) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 30) captura(mc, "debug_mo1_frente.png");
		if (t == 32) robotAhi(mc, "0.5 200 0.5 0 0");
		if (t == 42) captura(mc, "debug_mo2_espalda.png");
		if (t == 44) robotAhi(mc, "0.5 200 0.5 90 0");
		if (t == 54) captura(mc, "debug_mo3_costado.png");
		// Al robot dormido de cerca.
		if (t == 56) { comando(mc, "tp @s 2.5 200.3 -1.6"); robotAhi(mc, "0.5 200 5.5 180 0"); }
		if (t >= 57 && t < 70) mirar(mc, 0f, 35f);
		if (t == 66) captura(mc, "debug_mo4_dormido.png");
		// Comercio: saluda mientras la pantalla está abierta.
		if (t == 70) { comando(mc, "tp @s 0.5 200 -2.5"); robotAhi(mc, "0.5 200 0.5 180 0"); }
		if (t == 85) {
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof com.dedsafio4.robots.AldeanoRobotEntity robot) {
					mc.gameMode.interact(mc.player, robot, net.minecraft.world.InteractionHand.MAIN_HAND);
					break;
				}
			}
		}
		if (t == 97) {
			Dedsafio4ClientDebug.info("pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName()));
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof com.dedsafio4.robots.AldeanoRobotEntity robot) Dedsafio4ClientDebug.info("comerciando=" + robot.estaComerciando()
						+ " caja=" + robot.getBoundingBox());
			}
		}
		if (t == 98) mc.setScreen(null);   // se cierra sólo en el cliente: para el servidor sigue comerciando
		if (t == 104) captura(mc, "debug_mo5_saluda.png");
		if (t == 112) { mc.player.closeContainer(); mc.options.hideGui = false; }
		if (t == 125) {
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof com.dedsafio4.robots.AldeanoRobotEntity robot) Dedsafio4ClientDebug.info("tras cerrar: comerciando=" + robot.estaComerciando());
			}
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 128) {
			captura(mc, "debug_mo6_item.png");
		}
		if (t == 135) mc.stop();
	}

	private static final boolean LLAVE = "llave".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La llave despierta al robot dormido; también se ve la textura nueva del Huevo Eburia. */
	private static void tickLlave(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var bloque = new net.minecraft.core.BlockPos(0, 200, 0);
		if (t == -5 || t == -3) revivir(mc);
		if (t == -4) comando(mc, "kill @e[type=dedsafio4:aldeano_robot]");
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0.5 200 -2.5");
			comando(mc, "clear @s");
			comando(mc, "kill @e[type=minecraft:item]");
		}
		if (t >= 3) mirar(mc, 0f, 10f);
		if (t == 5) {
			comando(mc, "setblock 0 200 0 dedsafio4:robot_dormido[facing=north]");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:llave");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:huevo_eburia 5");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:semilla_eburia 12");
			mc.player.getInventory().selected = 0;
		}
		if (t == 18) mc.gui.getChat().clearMessages(false);
		if (t == 20) captura(mc, "debug_ll1_antes.png");
		if (t == 25) clickEn(mc, bloque, net.minecraft.core.Direction.NORTH);
		if (t == 40) {
			int robots = 0;
			float giro = 0;
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof com.dedsafio4.robots.AldeanoRobotEntity r) { robots++; giro = r.getYRot(); }
			}
			Dedsafio4ClientDebug.info("tras la llave: bloque=" + mc.level.getBlockState(bloque).getBlock() + " robots=" + robots + " giro=" + giro
					+ " llave=" + mc.player.getMainHandItem().getHoverName().getString() + " x" + mc.player.getMainHandItem().getCount());
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 44) captura(mc, "debug_ll2_despierto.png");
		if (t == 55) mc.stop();
	}

	private static final boolean ORE = "ore".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La textura del Mineral de Ámbar, al lado de piedra y de otros minerales. */
	private static void tickOre(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0.5 200 -2.5");
			comando(mc, "clear @s");
			comando(mc, "kill @e[type=dedsafio4:aldeano_robot]");
		}
		if (t >= 3) mirar(mc, 0f, 20f);
		if (t == 5) {
			comando(mc, "fill -2 200 1 2 201 1 minecraft:stone");
			comando(mc, "setblock 0 200 1 dedsafio4:mineral_de_ambar");
			comando(mc, "setblock 1 201 1 dedsafio4:mineral_de_ambar");
			comando(mc, "setblock -1 201 1 dedsafio4:mineral_de_ambar");
			comando(mc, "setblock 2 200 1 minecraft:gold_ore");
			comando(mc, "setblock -2 200 1 minecraft:copper_ore");
			comando(mc, "setblock 0 201 1 dedsafio4:mineral_verde");
			comando(mc, "setblock -2 201 1 dedsafio4:mineral_verde");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:mineral_de_ambar");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:ambar_en_bruto 7");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:mineral_verde 3");
			mc.player.getInventory().selected = 2;
		}
		if (t == 18) mc.gui.getChat().clearMessages(false);
		if (t == 45) captura(mc, "debug_or1.png");
		if (t == 50) mc.stop();
	}

	private static final boolean LLAVE2 = "llave2".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Tooltip de la Llave de Cobre y que se gaste al activar al Aldeano Phora. */
	private static void tickLlave2(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 30) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var bloque = new net.minecraft.core.BlockPos(0, 200, 0);
		if (t == -5 || t == -3) revivir(mc);
		if (t == -4) comando(mc, "kill @e[type=dedsafio4:aldeano_robot]");
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "tp @s 0.5 200 -2.5");
			comando(mc, "clear @s");
		}
		if (t >= 3 && t < 30) mirar(mc, 0f, 10f);
		if (t == 5) {
			comando(mc, "setblock 0 200 0 dedsafio4:robot_dormido[facing=north]");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:llave");
			mc.player.getInventory().selected = 0;
		}
		if (t == 15) clickEn(mc, bloque, net.minecraft.core.Direction.NORTH);
		if (t == 25) {
			int robots = 0;
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof com.dedsafio4.robots.AldeanoRobotEntity r) { robots++; Dedsafio4ClientDebug.info("robot: " + r.getDisplayName().getString()); }
			}
			Dedsafio4ClientDebug.info("survival: bloque=" + mc.level.getBlockState(bloque).getBlock() + " robots=" + robots
					+ " llaves que quedan=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.LLAVE));
			var pila = new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.LLAVE);
			for (var linea : pila.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player,
					net.minecraft.world.item.TooltipFlag.Default.NORMAL)) {
				Dedsafio4ClientDebug.info("tooltip: " + linea.getString());
			}
		}
		// El tooltip tal cual lo dibuja el juego, sobre el inventario.
		if (t == 30) {
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) ->
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((p, g, mx, my, delta) ->
							g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.LLAVE), 30, 40)));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 45) captura(mc, "debug_l2_tooltip.png");
		if (t == 55) mc.stop();
	}

	private static final boolean SEMILLA = "semilla".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La Semilla de Eburia se planta en el tronco; el Huevo Eburia ya no. */
	private static void tickSemilla(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 88) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var tronco = new net.minecraft.core.BlockPos(0, 200, 0);
		var conSemilla = new net.minecraft.core.BlockPos(0, 200, -1);
		var conHuevo = new net.minecraft.core.BlockPos(-1, 200, 0);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:grass_block");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "tp @s 0.5 200 -2.5");
			comando(mc, "clear @s");
		}
		if (t >= 3 && t < 60) mirar(mc, 0f, 10f);
		if (t == 5) {
			comando(mc, "fill 0 200 0 0 203 0 dedsafio4:roble_claro_log");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:semilla_eburia 5");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:huevo_eburia 5");
			mc.player.getInventory().selected = 0;
		}
		// Semilla: click en la cara norte del tronco.
		if (t == 15) clickEn(mc, tronco, net.minecraft.core.Direction.NORTH);
		// Huevo: click en la cara oeste del tronco.
		if (t == 20) mc.player.getInventory().selected = 1;
		if (t == 23) clickEn(mc, tronco, net.minecraft.core.Direction.WEST);
		if (t == 35) {
			var inv = mc.player.getInventory();
			Dedsafio4ClientDebug.info("con la semilla: " + mc.level.getBlockState(conSemilla)
					+ " | con el huevo: " + mc.level.getBlockState(conHuevo).getBlock()
					+ " | semillas=" + inv.countItem(com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
					+ " huevos=" + inv.countItem(com.dedsafio4.items.ModItems.HUEVO_EBURIA));
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 40) captura(mc, "debug_se2_plantado.png");
		// Verde se rompe: devuelve la semilla.
		if (t == 45) romperBloque(mc, conSemilla);
		if (t == 60) {
			Dedsafio4ClientDebug.info("verde roto: semillas tiradas=" + tirados(mc, com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
					+ " huevos tirados=" + tirados(mc, com.dedsafio4.items.ModItems.HUEVO_EBURIA)
					+ " | en el inventario: semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
					+ " huevos=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.HUEVO_EBURIA));
			comando(mc, "kill @e[type=minecraft:item]");
		}
		// Maduro: da huevos.
		if (t == 65) comando(mc, "setblock 0 200 -1 dedsafio4:huevo_eburia[facing=north,edad=2]");
		if (t == 70) romperBloque(mc, conSemilla);
		if (t == 85) Dedsafio4ClientDebug.info("maduro roto: semillas tiradas=" + tirados(mc, com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
				+ " huevos tirados=" + tirados(mc, com.dedsafio4.items.ModItems.HUEVO_EBURIA)
				+ " | en el inventario: semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
				+ " huevos=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.HUEVO_EBURIA));
		// El tooltip de la semilla tal cual lo dibuja el juego.
		if (t == 88) {
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) ->
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((p, g, mx, my, delta) ->
							g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.SEMILLA_EBURIA), 20, 20)));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 100) captura(mc, "debug_se3_tooltip.png");
		if (t == 110) mc.stop();
	}

	private static final boolean PLUMO = "plumo".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static net.minecraft.world.entity.Entity buscar(Minecraft mc, Class<?> clase) {
		for (var e : mc.level.entitiesForRendering()) if (clase.isInstance(e)) return e;
		return null;
	}

	private static float vidaServidor(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		var j = server == null ? null : server.getPlayerList().getPlayer(mc.player.getUUID());
		return j == null ? -1 : j.getHealth();
	}

	/** Plumosaurio: fruta, choque, golpe y drop. Reptisaurio Salvaje: ataca solo y suelta sangre. */
	private static void tickPlumo(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 170) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == 176) mc.gui.getChat().clearMessages(false);
		if (t == -5 || t == -3) revivir(mc);
		if (t == -4) { comando(mc, "kill @e[type=dedsafio4:walker]"); comando(mc, "kill @e[type=dedsafio4:mira]"); }
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -12 199 -12 12 199 12 minecraft:grass_block");
			comando(mc, "fill -12 200 -12 12 212 12 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "tp @s 0.5 200 -6.5");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
		}
		if (t == 5) {
			comando(mc, "summon dedsafio4:walker 0.5 200 0.5 {NoAI:1b,Rotation:[180f,0f]}");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:fruta_solaria 3");
			mc.player.getInventory().selected = 0;
		}
		if (t >= 3 && t < 40) mirar(mc, 0f, -10f);
		// Darle una Fruta Solaria.
		if (t == 15) comando(mc, "tp @s 0.5 200 -3.3 0 -10");
		if (t == 20) {
			var w = buscar(mc, com.dedsafio4.bestias.WalkerEntity.class);
			if (w != null) mc.gameMode.interact(mc.player, w, net.minecraft.world.InteractionHand.MAIN_HAND);
		}
		if (t == 35) {
			int cacas = 0;
			for (var pos : net.minecraft.core.BlockPos.betweenClosed(-6, 199, -6, 6, 202, 8)) {
				if (mc.level.getBlockState(pos).is(com.dedsafio4.bloques.ModBloques.CACA)) { cacas++; Dedsafio4ClientDebug.info("excremento en " + pos); }
			}
			Dedsafio4ClientDebug.info("tras la fruta: frutas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.FRUTA_SOLARIA)
					+ " bloques de excremento=" + cacas + " nombre=" + buscar(mc, com.dedsafio4.bestias.WalkerEntity.class).getDisplayName().getString());
		}
		// Chocarlo: el jugador se mete contra él.
		if (t == 40) { Dedsafio4ClientDebug.info("vida antes del choque=" + vidaServidor(mc)); comando(mc, "tp @s 0.5 200 -1.2"); }
		if (t == 50) Dedsafio4ClientDebug.info("vida tras el choque=" + vidaServidor(mc) + " jugador en z=" + String.format("%.1f", mc.player.getZ()));
		// Golpearlo.
		if (t == 60) { comando(mc, "effect give @s minecraft:instant_health 1 5"); }
		if (t == 70) { comando(mc, "tp @s 0.5 200 -3 0 0"); }
		if (t == 75) {
			Dedsafio4ClientDebug.info("vida antes del golpe=" + vidaServidor(mc));
			var w = buscar(mc, com.dedsafio4.bestias.WalkerEntity.class);
			if (w != null) mc.gameMode.attack(mc.player, w);
		}
		if (t == 85) Dedsafio4ClientDebug.info("vida tras golpearlo=" + vidaServidor(mc));
		// Muere y suelta la Manzana de Ámbar.
		if (t == 90) { comando(mc, "effect give @s minecraft:instant_health 1 5"); comando(mc, "tp @s 0.5 200 -9"); comando(mc, "kill @e[type=dedsafio4:walker]"); }
		if (t == 110) {
			Dedsafio4ClientDebug.info("manzanas de ambar tiradas=" + tirados(mc, com.dedsafio4.items.ModItems.MANZANA_DE_AMBAR));
			comando(mc, "kill @e[type=minecraft:item]");
		}
		// Reptisaurio Salvaje: te ataca sin que hagas nada.
		if (t == 112) comando(mc, "summon dedsafio4:mira 0.5 200 -3");
		if (t == 118) Dedsafio4ClientDebug.info("vida antes del salvaje=" + vidaServidor(mc));
		if (t == 150) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				for (var e : server.overworld().getEntities(com.dedsafio4.nave.ModEntidades.MIRA, x -> true)) {
					Dedsafio4ClientDebug.info("salvaje: objetivo=" + (e.getTarget() == null ? "nadie" : e.getTarget().getName().getString())
							+ " nombre=" + e.getDisplayName().getString() + " categoria=" + e.getType().getCategory());
				}
			});
			Dedsafio4ClientDebug.info("vida tras el salvaje=" + vidaServidor(mc));
		}
		if (t == 152) comando(mc, "kill @e[type=dedsafio4:mira]");
		if (t == 170) Dedsafio4ClientDebug.info("sangre tirada=" + tirados(mc, com.dedsafio4.items.ModItems.SANGRE_REPTISAURIO));
		// Los huevos con su descripción.
		if (t == 172) {
			comando(mc, "gamemode creative");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:pelo_eburia 9");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:manzana_de_ambar 2");
			comando(mc, "item replace entity @s hotbar.3 with dedsafio4:fruta_solaria 2");
			comando(mc, "item replace entity @s hotbar.4 with dedsafio4:tela_primitiva 4");
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) ->
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((pp, g, mx, my, delta) -> {
						var cual = new net.minecraft.world.item.Item[]{com.dedsafio4.items.ModItems.WALKER_SPAWN_EGG,
								com.dedsafio4.items.ModItems.MIRA_SPAWN_EGG, com.dedsafio4.items.ModItems.PELO_EBURIA,
								com.dedsafio4.items.ModItems.TELA_PRIMITIVA}[Math.min(3, Math.max(0, (ticks - 20 - 180) / 6))];
						g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(cual), 4, 16);
					}));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 184) captura(mc, "debug_pl1_plumosaurio.png");
		if (t == 190) captura(mc, "debug_pl2_salvaje.png");
		if (t == 196) captura(mc, "debug_pl3_pelos.png");
		if (t == 202) captura(mc, "debug_pl4_tela.png");
		if (t == 210) mc.stop();
	}

	private static final boolean QUIU = "quiu".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Que aparezcan solos: Plumosaurios en la jungla del Centro de Quiu y Reptisaurios Salvajes en sus cuevas. */
	private static void tickQuiu(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t > 5) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "difficulty normal");
			comando(mc, "effect give @s minecraft:resistance 99999 255 true");
			comando(mc, "execute in dedsafio4:dimension_nueva run locate biome dedsafio4:jungla");
		}
		if (t == 10) comando(mc, "execute in dedsafio4:dimension_nueva run tp @s 5584 200 6416");
		if (t > 20 && t % 100 == 0) {
			var server = mc.getSingleplayerServer();
			final int tt = t;
			server.execute(() -> {
				var mundo = mc.player == null ? null : server.getLevel(mc.player.level().dimension());
				if (mundo == null) return;
				int plumos = 0, salvajes = 0, enCueva = 0;
				for (var e : mundo.getAllEntities()) {
					if (e.getType() == com.dedsafio4.nave.ModEntidades.WALKER) plumos++;
					if (e.getType() == com.dedsafio4.nave.ModEntidades.MIRA) {
						salvajes++;
						if (mundo.getBrightness(net.minecraft.world.level.LightLayer.SKY, e.blockPosition()) == 0) enCueva++;
					}
				}
				var pos = mc.player.blockPosition();
				Dedsafio4ClientDebug.info("t=" + tt + " dimension=" + mundo.dimension().location() + " bioma aca="
						+ mundo.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?")
						+ " plumosaurios=" + plumos + " reptisaurios salvajes=" + salvajes + " (en cuevas: " + enCueva + ")");
			});
		}
		if (t == 900) mc.stop();
	}

	private static final boolean CHAT_HERMANDAD = "chathermandad".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Escribe en el mini chat de la Hermandad como si fuera el jugador: click en el renglón, letras y Enter. */
	private static void escribirChat(Minecraft mc, String texto) {
		if (!(mc.screen instanceof HermandadScreen pantalla)) return;
		int x = (pantalla.width - 388) / 2, y = (pantalla.height - 232) / 2;
		pantalla.mouseClicked(x + 60, y + 214, 0);
		for (char c : texto.toCharArray()) pantalla.charTyped(c, 0);
		pantalla.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);
	}

	/** El chat de la Hermandad: se ve en la interfaz y en el chat normal, solo para los miembros. */
	private static void tickChatHermandad(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof HermandadScreen) && t > 0) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			var nombre = mc.player.getGameProfile().getName();
			server.execute(() -> {
				var data = server.overworld().getDataStorage().computeIfAbsent(
						com.dedsafio4.hermandad.HermandadesData.FACTORY, "dedsafio4_hermandades");
				data.todas().clear();
				var h = new com.dedsafio4.hermandad.HermandadesData.Hermandad("Los Quiu", uuid, java.util.List.of(
						new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(uuid, nombre),
						new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(java.util.UUID.randomUUID(), "Locochon")));
				data.agregar(h);
				data.setColor(h, 0x55FF55);
				data.agregarChat(h, new com.dedsafio4.hermandad.HermandadChatPayload.Linea("16:25", "Locochon", "-82 -17"));
				data.agregarChat(h, new com.dedsafio4.hermandad.HermandadChatPayload.Linea("16:38", "", "CapitanGatoYT se ha unido a la hermandad."));
			});
		}
		if (t == 5) net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((mensaje, arriba) ->
				Dedsafio4ClientDebug.info("chat normal: " + mensaje.getString() + " | color del nombre="
						+ mensaje.getSiblings().stream().findFirst().map(c -> String.valueOf(c.getStyle().getColor())).orElse("?")));
		if (t == 10) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.dedsafio4.hermandad.HermandadAccionPayload(com.dedsafio4.hermandad.HermandadAccionPayload.PEDIR_INFO, ""));
		if (t == 20) Dedsafio4ClientDebug.info("pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())
				+ " lineas del chat=" + HermandadesCliente.chat().size());
		if (t == 25) escribirChat(mc, "chicos necesitamos grava y bambu");
		if (t == 32) escribirChat(mc, "CHICOS TENEMOS GRANJA DE EXP!! esta en -482 -17 -3, vengan cuando puedan que hay mucha experiencia para todos");
		if (t == 40) {
			Dedsafio4ClientDebug.info("lineas del chat=" + HermandadesCliente.chat().size());
			for (var l : HermandadesCliente.chat()) Dedsafio4ClientDebug.info("  [" + l.hora() + "] " + (l.autor().isEmpty() ? "(aviso) " : l.autor() + ": ") + l.texto());

		}
		if (t == 45) captura(mc, "debug_ch1_interfaz.png");
		if (t == 50) mc.setScreen(null);
		if (t == 56) captura(mc, "debug_ch2_chat_normal.png");
		if (t == 65) mc.stop();
	}

	private static final boolean TOOLTIP = "tooltip".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Muestra el tooltip de un ítem (su id en DEDSAFIO4_ITEM, ej. dedsafio4:cuerda_resistente) y lo pone en la barra. */
	private static void tickTooltip(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 20) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		String id = System.getenv().getOrDefault("DEDSAFIO4_ITEM", "minecraft:stone");
		var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with " + id + " " + Math.min(5, item.getDefaultMaxStackSize()));
			comando(mc, "item replace entity @s hotbar.1 with " + System.getenv().getOrDefault("DEDSAFIO4_ITEM2", "minecraft:air"));
			comando(mc, "item replace entity @s armor.head with " + System.getenv().getOrDefault("DEDSAFIO4_CASCO", "minecraft:air"));
			if (System.getenv("DEDSAFIO4_SET") != null) {
				comando(mc, "item replace entity @s armor.chest with dedsafio4:pechera_netherita_planta");
				comando(mc, "item replace entity @s armor.legs with dedsafio4:pantalones_netherita_planta");
				comando(mc, "item replace entity @s armor.feet with dedsafio4:botas_netherita_planta");
			}
		}
		if (t == 15) {
			mc.gui.getChat().clearMessages(false);
			var pila = new net.minecraft.world.item.ItemStack(item);
			for (var linea : pila.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player,
					net.minecraft.world.item.TooltipFlag.Default.NORMAL)) Dedsafio4ClientDebug.info("tooltip: " + linea.getString());
		}
		if (t == 20) {
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) ->
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((pp, g, mx, my, delta) ->
							g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(item), 20, 20)));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 30) captura(mc, "debug_tt1.png");
		if (t == 36) mc.stop();
	}

	private static final boolean BOGGED = "bogged".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** esqueleto_bogged 1: de noche aparecen Bogged fuera de los pantanos; sin el cambio, no. */
	private static void tickBogged(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Consumer<String> contar = cuando -> server.execute(() -> {
			var mundo = server.overworld();
			var jugador = server.getPlayerList().getPlayer(uuid);
			int fuera = 0, pantano = 0;
			StringBuilder biomas = new StringBuilder();
			for (var b : mundo.getEntities(net.minecraft.world.entity.EntityType.BOGGED, e -> e.distanceTo(jugador) < 160)) {
				var bioma = mundo.getBiome(b.blockPosition());
				boolean esPantano = bioma.is(net.minecraft.world.level.biome.Biomes.SWAMP) || bioma.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP);
				if (esPantano) pantano++; else fuera++;
				if (biomas.length() < 120) biomas.append(bioma.unwrapKey().map(k -> k.location().getPath()).orElse("?")).append(' ');
			}
			int monstruos = mundo.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, jugador.getBoundingBox().inflate(128)).size();
			Dedsafio4ClientDebug.info("bogged " + cuando + ": monstruos cerca=" + monstruos + " fuera del pantano=" + fuera + " en pantano=" + pantano + " biomas: " + biomas
					+ " jugador en " + jugador.blockPosition().toShortString());
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "time set 18000");
			comando(mc, "cambio esqueleto_bogged 0");
			comando(mc, "execute in minecraft:overworld run tp @s 4000.5 200 4000.5");
		}
		if (t == 20) {
			server.execute(() -> {
				var mundo = server.overworld();
				var hallado = mundo.findClosestBiome3d(h -> h.is(net.minecraft.world.level.biome.Biomes.PLAINS),
						new net.minecraft.core.BlockPos(4000, 64, 4000), 6400, 32, 64);
				var p = hallado == null ? new net.minecraft.core.BlockPos(4000, 64, 4000) : hallado.getFirst();
				int y = mundo.getChunk(p.getX() >> 4, p.getZ() >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX() & 15, p.getZ() & 15);
				server.getPlayerList().getPlayer(uuid).teleportTo(p.getX() + 0.5, y + 1, p.getZ() + 0.5);
			});
		}
		if (t == 40) comando(mc, "kill @e[type=!minecraft:player]");
		if (t == 540) contar.accept("de noche sin el cambio");
		if (t == 542) {
			comando(mc, "cambio esqueleto_bogged 1");
			comando(mc, "kill @e[type=!minecraft:player]");
		}
		if (t == 1042) contar.accept("de noche con el cambio");
		if (t == 1046) {
			comando(mc, "cambio esqueleto_bogged 0");
			comando(mc, "gamerule doDaylightCycle true");
			comando(mc, "kill @e[type=minecraft:bogged]");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5");
		}
		if (t == 1060) mc.stop();
	}

	private static final boolean LINTERNA_MANO = "linternamano".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La Linterna en la mano derecha: primera persona, tercera de atrás y de frente. */
	private static void tickLinternaMano(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 4.5 180 0");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:linterna");
			comando(mc, "item replace entity @s weapon.offhand with dedsafio4:linterna");
		}
		if (t == 6) mc.player.getInventory().selected = 0;
		if (t == 8) mc.gui.getChat().clearMessages(false);
		if (t == 20) captura(mc, "debug_lm1_primera.png");
		if (t == 22) { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); mirar(mc, 180, 0); }
		if (t == 32) captura(mc, "debug_lm2_frente.png");
		if (t == 34) { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK); mirar(mc, 140, 0); }
		if (t == 44) captura(mc, "debug_lm3_atras.png");
		if (t == 46) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		if (t == 50) mc.stop();
	}

	private static final boolean AGARRE = "agarre".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));
	private static boolean agarreFotoVuelo;

	/** Qumara: al terminar el Atraer agarra al más cercano; minijuego ganado, salvado por tótem, y muerto por 5 fallos. */
	private static void tickAgarre(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 120");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 120 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 120 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 240 -60.5 0 30");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 30.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			q.accion(server.getPlayerList().getPlayer(uuid), 1);
		});
		if (t == 31) server.execute(() -> {
			var q = qumaraServidor(server);
			// Nace sola (sin jinete, accion no la deja): la hacemos nacer a mano.
			if (!q.nacida()) {
				try {
					var m = com.dedsafio4.qumara.QumaraEntity.class.getDeclaredMethod("nacer", net.minecraft.server.level.ServerPlayer.class);
					m.setAccessible(true);
					m.invoke(q, server.getPlayerList().getPlayer(uuid));
				} catch (Exception ex) { Dedsafio4ClientDebug.info("agarre: no nace " + ex); }
			}
		});
		if (t == 180) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> false;
			comando(mc, "gamemode survival");
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1] + "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 58.5 180 0");
		}
		if (t == 200) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.setHealth(j.getMaxHealth());
			qumaraServidor(server).atraerYa(server.overworld());
			Dedsafio4ClientDebug.info("agarre: atraer empezado, nacida=" + qumaraServidor(server).nacida());
		});
		// Registro del agarre.
		if (t >= 480 && t % 10 == 0) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			if (q == null || !q.agarrando()) return;
			var local = j.position().subtract(q.position());
			Dedsafio4ClientDebug.info(String.format("agarre t=%d g=%.2f jugador rel=(%.1f, %.1f, %.1f) montado=%b aciertos=%d fallos=%d vida=%.1f queda=%.1f",
					t, q.tiempoAgarre(0), local.x, local.y, local.z, j.getVehicle() == q, q.aciertos(), q.fallos(), j.getHealth(), q.tiempoParaPulsar(0)));
		});
		var qc = com.dedsafio4.client.qumara.AgarreMinijuego.activa();
		// Ronda 1: acierta siempre.
		if (t > 500 && t < 1080 && qc != null && com.dedsafio4.client.qumara.AgarreMinijuego.enLaZona(qc)) com.dedsafio4.client.qumara.AgarreMinijuego.pulsar(qc);
		if (t == 520) captura(mc, "debug_ag1_alcanza.png");
		if (t == 590) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_ag2_minijuego.png"); }
		if (t == 592) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
		if (t == 596) captura(mc, "debug_ag3_sostiene.png");
		if (t == 598) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		if (t > 600 && t < 1080 && !agarreFotoVuelo && mc.player.getVehicle() instanceof com.dedsafio4.qumara.QumaraEntity q && q.tSacar(0) > 1.0f) {
			agarreFotoVuelo = true;
			mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
		}
		if (agarreFotoVuelo && t < 1080 && mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT
				&& mc.player.getVehicle() instanceof com.dedsafio4.qumara.QumaraEntity q2 && q2.tSacar(0) > 1.25f) {
			captura(mc, "debug_ag4_vuela.png");
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		}
		if (t == 1080) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			var q = qumaraServidor(server);
			Dedsafio4ClientDebug.info(String.format("agarre ronda 1 final: agarrando=%b montado=%b en el piso=%b vida=%.1f rel=(%.1f, %.1f, %.1f)",
					q.agarrando(), j.isPassenger(), j.onGround(), j.getHealth(), j.getX() - q.getX(), j.getY() - q.getY(), j.getZ() - q.getZ()));
		});
		// Ronda 2: con tótem, no pulsa nunca.
		if (t == 1100) {
			comando(mc, "item replace entity @s weapon.offhand with minecraft:totem_of_undying");
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				j.setHealth(j.getMaxHealth());
				j.teleportTo(1455.5, 200, 50.5);
			});
		}
		if (t == 1110) server.execute(() -> qumaraServidor(server).agarrar(server.getPlayerList().getPlayer(uuid)));
		if (t == 1110 + 270) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info(String.format("agarre ronda 2 (tótem, sin pulsar): vivo=%b vida=%.1f mano izquierda=%s sacando=%.1f",
					j.isAlive(), j.getHealth(), j.getOffhandItem(), qumaraServidor(server).tSacar(0)));
		});
		// Ronda 3: sin tótem, 5 fallos.
		if (t == 1520) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.setHealth(j.getMaxHealth());
			j.teleportTo(1445.5, 200, 52.5);
			Dedsafio4ClientDebug.info("agarre antes de la ronda 3: agarrando=" + qumaraServidor(server).agarrando());
		});
		if (t == 1530) server.execute(() -> qumaraServidor(server).agarrar(server.getPlayerList().getPlayer(uuid)));
		if (t > 1600 && t < 1700 && t % 8 == 0 && qc != null && !com.dedsafio4.client.qumara.AgarreMinijuego.enLaZona(qc)) com.dedsafio4.client.qumara.AgarreMinijuego.pulsar(qc);
		if (t == 1640) captura(mc, "debug_ag5_fallos.png");
		if (t == 1720) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info("agarre ronda 3 (5 fallos): vivo=" + j.isAlive() + " vida=" + j.getHealth()
					+ " mensaje=" + j.getCombatTracker().getDeathMessage().getString());
		});
		if (t == 1740) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> pl.hasPermissions(2);
			comando(mc, "forceload remove all");
		}
		if (t == 1744) mc.stop();
	}

	private static final boolean ATRAER_SALTO = "atraersalto".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Atraer: qué tan rápido te trae caminando normal y saltando (espacio apretado). */
	private static void tickAtraerSalto(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 120");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 120 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 120 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 240 -60.5 0 30");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, -30.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			try {
				var m = com.dedsafio4.qumara.QumaraEntity.class.getDeclaredMethod("nacer", net.minecraft.server.level.ServerPlayer.class);
				m.setAccessible(true);
				m.invoke(q, server.getPlayerList().getPlayer(uuid));
			} catch (Exception ex) { Dedsafio4ClientDebug.info("salto: no nace " + ex); }
		});
		for (int ronda = 0; ronda < 2; ronda++) {
			int base = 200 + ronda * 200;
			boolean salta = ronda == 1;
			if (t == base) {
				com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> false;
				comando(mc, "gamemode survival");
				comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 60.5 180 0");
			}
			if (t == base + 10) server.execute(() -> qumaraServidor(server).atraerYa(server.overworld()));
			if (t > base + 10 && t < base + 110) mc.options.keyJump.setDown(salta);
			if (t == base + 110) mc.options.keyJump.setDown(false);
			if (t == base + 40) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_resiste" + ronda + ".png"); }
			if (t >= base + 20 && t <= base + 100 && (t - base) % 20 == 0) {
				int k = t - base - 10;
				server.execute(() -> {
					var j = server.getPlayerList().getPlayer(uuid);
					var q = qumaraServidor(server);
					Dedsafio4ClientDebug.info(String.format("salto %s: t=%.1fs distancia=%.1f y=%.1f aguante=%.2f", salta ? "ESPACIO" : "sin espacio", k / 20f,
							Math.hypot(j.getX() - q.getX(), j.getZ() - q.getZ()), j.getY(), com.dedsafio4.client.qumara.AtraerCliente.aguante()));
				});
			}
			if (t == base + 110) {
				comando(mc, "gamemode creative");
				server.execute(() -> {
					try {
						var f = com.dedsafio4.qumara.QumaraEntity.class.getDeclaredField("T_ATRAER");
						f.setAccessible(true);
						@SuppressWarnings("unchecked") var acc = (net.minecraft.network.syncher.EntityDataAccessor<Long>) f.get(null);
						qumaraServidor(server).getEntityData().set(acc, Long.MIN_VALUE / 2);   // cortar el Atraer (y que no agarre)
					} catch (Exception ex) { Dedsafio4ClientDebug.info("salto: " + ex); }
				});
			}
		}
		if (t == 620) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> pl.hasPermissions(2);
			comando(mc, "kill @e[type=dedsafio4:qumara]");
			comando(mc, "forceload remove all");
		}
		if (t == 624) mc.stop();
	}

	private static final boolean BOSS_IA = "bossia".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));
	private static String bossIaAntes = "";
	private static int bossIaFotos;

	/** /boss 1 ai: la IA maneja a Qumara contra el jugador de prueba (admin, en supervivencia). */
	private static void tickBossIa(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 120");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 120 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 120 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 -30.5 0 0");
		}
		if (t == 40) {
			comando(mc, "gamemode survival");
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1] + "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
		}
		if (t == 44) server.execute(() -> { var j = server.getPlayerList().getPlayer(uuid); j.setHealth(j.getMaxHealth()); });
		if (t == 45) comando(mc, "boss 1 ai");
		// Si muere, vuelve (con vida llena y la armadura) para seguir viendo a la IA.
		if (t > 60 && t < 1840 && !mc.player.isAlive() && t % 20 == 0) {
			Dedsafio4ClientDebug.info("bossia: murio en t=" + (t / 20f) + "s, vuelve");
			revivir(mc);
		}
		if (t > 60 && t < 1840 && t % 20 == 10 && mc.player.isAlive() && mc.player.getInventory().getArmor(2).isEmpty()) {
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 -30.5 0 0");
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1] + "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
		}
		if (t == 50) server.execute(() -> {
			var q = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.QumaraEntity.class, new net.minecraft.world.phys.AABB(1380, 180, -80, 1520, 260, 120)).stream().findFirst().orElse(null);
			if (q == null) { Dedsafio4ClientDebug.info("bossia: no aparecio"); return; }
			qumaraPrueba = q.getUUID();
			Dedsafio4ClientDebug.info(String.format("bossia: aparecio en %.1f,%.1f,%.1f ia=%b", q.getX(), q.getY(), q.getZ(), q.ia()));
		});
		// Cada segundo: qué está haciendo.
		if (t > 50 && t < 1850 && t % 10 == 0) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			if (q == null) return;
			String estado = (q.nacida() ? (q.naciendo() ? "naciendo" : "nacida") : "rosa") + (q.barriendo() ? " GIRO" : "")
					+ (q.recargaCirculos() > q.RECARGA_CIRCULOS - 20 ? " CIRCULOS" : "") + (q.atrayendo() ? " ATRAER" : "") + (q.agarrando() ? " AGARRE" : "")
					+ (q.debil() ? " enfriamiento" : "");
			Dedsafio4ClientDebug.info(String.format("bossia vida t=%.1fs: %.1f/%.1f", t / 20f, j.getHealth(), j.getMaxHealth()));
			if (!estado.equals(bossIaAntes)) {
				bossIaAntes = estado;
				Dedsafio4ClientDebug.info(String.format("bossia t=%.1fs: %s | jugador vida=%.1f distancia=%.1f muerto=%b", t / 20f, estado, j.getHealth(),
						Math.hypot(j.getX() - q.getX(), j.getZ() - q.getZ()), !j.isAlive()));
			}
		});
		// Si lo agarra, juega bien el minijuego.
		var qc = com.dedsafio4.client.qumara.AgarreMinijuego.activa();
		if (qc != null && com.dedsafio4.client.qumara.AgarreMinijuego.enLaZona(qc)) com.dedsafio4.client.qumara.AgarreMinijuego.pulsar(qc);
		// Fotos: cuando hay círculos y cuando gira el brazo.
		if (bossIaFotos < 2 && t % 5 == 0 && !mc.level.getEntitiesOfClass(com.dedsafio4.qumara.CirculoEntity.class, mc.player.getBoundingBox().inflate(40)).isEmpty()
				&& mc.level.getEntitiesOfClass(com.dedsafio4.qumara.CirculoEntity.class, mc.player.getBoundingBox().inflate(40)).get(0).tickCount == 30) {
			captura(mc, "debug_ia_circulos" + bossIaFotos + ".png");
			bossIaFotos++;
		}
		if (t == 1850) comando(mc, "boss 1 jugador");
		if (t == 1856) server.execute(() -> Dedsafio4ClientDebug.info("bossia: despues de /boss 1 jugador ia=" + qumaraServidor(server).ia()));
		if (t == 1860) {
			comando(mc, "gamemode creative");
			comando(mc, "clear @s");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "forceload remove all");
		}
		if (t == 1866) mc.stop();
	}

	private static final boolean NO_BOSS = "noboss".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** /admin no_boss: no se puede subir a Qumara ni al T-Rex; /admin boss: sí. */
	private static void tickNoBoss(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "admin boss");
			for (String e : new String[]{"qumara", "t_rex", "trex"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 0.5 0 0");
		}
		if (t == 20) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 14.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
		});
		java.util.function.Consumer<String> probar = cuando -> {
			var q = mc.level.getEntitiesOfClass(com.dedsafio4.qumara.QumaraEntity.class, mc.player.getBoundingBox().inflate(40)).stream().findFirst().orElse(null);
			if (q != null) mc.gameMode.interact(mc.player, q, net.minecraft.world.InteractionHand.MAIN_HAND);
		};
		java.util.function.Consumer<String> ver = cuando -> server.execute(() -> Dedsafio4ClientDebug.info("noboss " + cuando + ": montado=" + server.getPlayerList().getPlayer(uuid).getVehicle()));
		if (t == 40) comando(mc, "admin no_boss");
		if (t == 50) probar.accept("");
		if (t == 60) ver.accept("con no_boss");
		if (t == 62) captura(mc, "debug_noboss.png");
		if (t == 70) comando(mc, "admin boss");
		if (t == 80) probar.accept("");
		if (t == 90) ver.accept("con boss");
		if (t == 100) comando(mc, "admin no_boss");
		if (t == 110) ver.accept("despues de no_boss estando montado");
		if (t == 120) {
			comando(mc, "admin boss");
			comando(mc, "kill @e[type=dedsafio4:qumara]");
		}
		if (t == 126) mc.stop();
	}

	private static final boolean ANTIBIOTICO = "antibiotico".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Antibiótico y Antídoto Primitivo: creativo, soporte de pociones, pastilla, inmunidad, y los efectos abajo a la derecha. */
	private static void tickAntibiotico(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode survival");
			comando(mc, "effect clear @s");
			comando(mc, "clear @s");
			comando(mc, "time set 6000");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 0.5 0 0");
		}
		if (t == 10) {
			net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
			StringBuilder sb = new StringBuilder("antibiotico en el creativo:");
			for (var st : net.minecraft.world.item.CreativeModeTabs.searchTab().getDisplayItems()) {
				var c = st.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
				if (c != null && c.is(com.dedsafio4.pociones.ModPociones.ANTIBIOTICO)) sb.append(" [").append(st.getHoverName().getString()).append("]");
			}
			Dedsafio4ClientDebug.info(sb.toString());
		}
		// Soporte para pociones: agua / rara / otra cosa con el Antídoto.
		if (t == 15) server.execute(() -> {
			var pb = server.potionBrewing();
			var antidoto = new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.ANTIDOTO_PRIMITIVO);
			var agua = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
			var rara = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.AWKWARD);
			var fuerza = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, net.minecraft.world.item.alchemy.Potions.STRENGTH);
			Dedsafio4ClientDebug.info("soporte: antidoto es ingrediente=" + pb.isIngredient(antidoto)
					+ " | agua: mezcla=" + pb.hasMix(agua, antidoto) + " -> " + pb.mix(antidoto, agua.copy()).getHoverName().getString()
					+ " | rara: mezcla=" + pb.hasMix(rara, antidoto) + " -> " + pb.mix(antidoto, rara.copy()).getHoverName().getString()
					+ " | fuerza: mezcla=" + pb.hasMix(fuerza, antidoto));
		});
		// La pastilla: te da Antibiótico 3 minutos y ya no te agarra el Veneno Primitivo.
		if (t == 20) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			com.dedsafio4.reptisaurios.VenenoPrimitivo.aplicar(j);
			boolean antes = com.dedsafio4.reptisaurios.VenenoPrimitivo.envenenado(j);
			var pastilla = new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.ANTIDOTO_PRIMITIVO);
			pastilla.finishUsingItem(server.overworld(), j);
			var e = j.getEffect(com.dedsafio4.pociones.ModPociones.EFECTO_ANTIBIOTICO);
			com.dedsafio4.reptisaurios.VenenoPrimitivo.aplicar(j);
			Dedsafio4ClientDebug.info("pastilla: veneno primitivo antes=" + antes + " antibiotico=" + (e == null ? "no" : e.getDuration() / 20 + " s")
					+ " veneno primitivo despues (con otro intento)=" + com.dedsafio4.reptisaurios.VenenoPrimitivo.envenenado(j));
		});
		// Efectos: velocidad y antibiótico (buenos), debilidad (malo) y rojizo (no se ve).
		if (t == 30) {
			comando(mc, "effect give @s minecraft:speed 120 0");
			comando(mc, "effect give @s minecraft:weakness 120 0");
			comando(mc, "effect give @s dedsafio4:rojizo 120 0 true");
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 45) captura(mc, "debug_efectos_abajo.png");
		if (t == 50) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		if (t == 60) captura(mc, "debug_efectos_inventario.png");
		if (t == 62) {
			var pila = new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.ANTIDOTO_PRIMITIVO);
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) ->
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((pp, g, mx, my, delta) ->
							g.renderTooltip(cliente.font, pila, 10, 20)));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 72) captura(mc, "debug_antidoto_tooltip.png");
		if (t == 80) {
			mc.setScreen(null);
			comando(mc, "effect clear @s");
			comando(mc, "gamemode creative");
		}
		if (t == 84) mc.stop();
	}

	private static final boolean CORAZON = "corazon".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Corazón: cada uno que comés te da un corazón más para siempre (también después de morir). */
	private static void tickCorazon(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Consumer<String> log = cuando -> server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info(String.format("corazon %s: vida maxima=%.0f (extra de corazones=%.0f) vida=%.1f", cuando, j.getMaxHealth(),
					com.dedsafio4.items.CorazonItem.extra(j), j.getHealth()));
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "attribute @s minecraft:generic.max_health modifier remove dedsafio4:corazones_comidos");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 0.5 0 0");
		}
		if (t == 10) log.accept("al empezar");
		if (t == 15) comando(mc, "item replace entity @s hotbar.0 with dedsafio4:corazon 3");
		// Come uno de verdad (con la mano), con la barra de comida llena.
		if (t == 20) mc.options.keyUse.setDown(true);
		if (t == 70) { mc.options.keyUse.setDown(false); log.accept("despues de comer 1 (con la mano)"); }
		if (t == 75) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			for (int i = 0; i < 2; i++) new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.CORAZON).finishUsingItem(server.overworld(), j);
		});
		if (t == 80) log.accept("despues de comer 3");
		if (t == 82) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_corazon.png"); }
		if (t == 90) comando(mc, "kill @s");
		if (t == 100) revivir(mc);
		if (t == 120) log.accept("despues de morir");
		if (t == 124) comando(mc, "attribute @s minecraft:generic.max_health modifier remove dedsafio4:corazones_comidos");
		if (t == 128) { comando(mc, "gamemode creative"); mc.stop(); }
	}

	private static final boolean GAS = "gas".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));
	private static boolean gasFotoNube;

	/** Gas Morado: cómo se ve, que envenena (salvo con Antibiótico), y los soplos al agarrar + la nube al caer. */
	private static void tickGas(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Consumer<String> log = cuando -> server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info("gas " + cuando + ": veneno primitivo=" + com.dedsafio4.reptisaurios.VenenoPrimitivo.envenenado(j)
					+ " antibiotico=" + j.hasEffect(com.dedsafio4.pociones.ModPociones.EFECTO_ANTIBIOTICO)
					+ " gases=" + server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.GasMoradoEntity.class, j.getBoundingBox().inflate(60)).size());
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 120");
			comando(mc, "gamemode survival");
			comando(mc, "effect clear @s");
			comando(mc, "clear @s");
			comando(mc, "time set 6000");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo", "gas_morado"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 120 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 120 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1440.5 201 -14.5 0 25");
		}
		if (t == 30) server.execute(() -> {
			server.overworld().addFreshEntity(com.dedsafio4.qumara.GasMoradoEntity.nube(server.overworld(), new net.minecraft.world.phys.Vec3(1440.5, 200, -11.5), 400, 1.6f));
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 30.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			try {
				var m = com.dedsafio4.qumara.QumaraEntity.class.getDeclaredMethod("nacer", net.minecraft.server.level.ServerPlayer.class);
				m.setAccessible(true);
				m.invoke(q, server.getPlayerList().getPlayer(uuid));
			} catch (Exception ex) { Dedsafio4ClientDebug.info("gas: no nace " + ex); }
		});
		if (t == 50) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_gas_nube.png"); }
		if (t == 55) log.accept("afuera de la nube");
		if (t == 60) comando(mc, "execute in minecraft:overworld run tp @s 1440.5 200 -11.5 0 0");
		if (t == 70) log.accept("adentro de la nube");
		if (t == 75) server.execute(() -> new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.ANTIDOTO_PRIMITIVO).finishUsingItem(server.overworld(), server.getPlayerList().getPlayer(uuid)));
		if (t == 90) log.accept("adentro con antibiotico (despues del antidoto)");
		if (t == 100) {
			comando(mc, "effect clear @s");
			comando(mc, "kill @e[type=dedsafio4:gas_morado]");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 52.5 180 0");
		}
		// El agarre.
		if (t == 200) server.execute(() -> {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> false;
			var j = server.getPlayerList().getPlayer(uuid);
			j.setHealth(j.getMaxHealth());
			qumaraServidor(server).agarrar(j);
		});
		var qc = com.dedsafio4.client.qumara.AgarreMinijuego.activa();
		if (t > 330 && qc != null && com.dedsafio4.client.qumara.AgarreMinijuego.enLaZona(qc)) com.dedsafio4.client.qumara.AgarreMinijuego.pulsar(qc);
		if (t == 280) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
		if (t == 290) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_gas_soplo.png"); }
		if (t == 295) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		if (t == 305) captura(mc, "debug_gas_soplo_1ra.png");
		if (t == 320) log.accept("agarrado con los soplos");
		if (t > 330 && !gasFotoNube && mc.player.getVehicle() == null && t < 800) {
			var nubes = mc.level.getEntitiesOfClass(com.dedsafio4.qumara.GasMoradoEntity.class, mc.player.getBoundingBox().inflate(4), g -> g.modo() == com.dedsafio4.qumara.GasMoradoEntity.NUBE);
			if (!nubes.isEmpty() && nubes.get(0).tickCount == 20) {
				gasFotoNube = true;
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
			}
		}
		if (gasFotoNube && mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) {
			var nubes = mc.level.getEntitiesOfClass(com.dedsafio4.qumara.GasMoradoEntity.class, mc.player.getBoundingBox().inflate(4));
			if (!nubes.isEmpty() && nubes.get(0).tickCount >= 26) {
				captura(mc, "debug_gas_cayo.png");
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				log.accept("tirado en la nube");
			}
		}
		if (t == 800) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> pl.hasPermissions(2);
			comando(mc, "effect clear @s");
			comando(mc, "gamemode creative");
			for (String e : new String[]{"qumara", "gas_morado"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "forceload remove all");
		}
		if (t == 806) mc.stop();
	}

	private static final boolean COFRE_HUESOS = "cofrehuesos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Cofre de Huesos (cerrado, abierto, 54 espacios, se rompe y se suelta) y el botón 6 de Qumara (gas a todos). */
	private static void tickCofreHuesos(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 200) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		var pos = new net.minecraft.core.BlockPos(1450, 200, 4);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "time set 6000");
			comando(mc, "fill 1440 199 -6 1460 199 14 minecraft:stone_bricks");
			comando(mc, "fill 1440 200 -6 1460 206 14 minecraft:air");
			for (String e : new String[]{"qumara", "gas_morado"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 10) server.execute(() -> {
			var lvl = server.overworld();
			var est = com.dedsafio4.cofres.ModCofres.COFRE_HUESOS.defaultBlockState()
					.setValue(com.dedsafio4.cofres.CofreBlock.FACING, net.minecraft.core.Direction.SOUTH);
			lvl.setBlock(pos, est, 3);
			lvl.setBlock(pos.relative(com.dedsafio4.cofres.CofreBlock.haciaLaOtra(est)),
					est.setValue(com.dedsafio4.cofres.CofreBlock.PARTE, com.dedsafio4.cofres.CofreBlock.Parte.SECUNDARIA), 3);
		});
		if (t == 15) comando(mc, "execute in minecraft:overworld run tp @s 1452.0 201.2 7.6 160 28");
		if (t == 30) { mc.gui.getChat().clearMessages(false); mc.options.hideGui = true; captura(mc, "debug_cofre_huesos.png"); }
		if (t == 35) server.execute(() -> server.overworld().blockEvent(pos, com.dedsafio4.cofres.ModCofres.COFRE_HUESOS, 1, 1));
		if (t == 60) captura(mc, "debug_cofre_huesos_abierto.png");
		if (t == 62) server.execute(() -> server.overworld().blockEvent(pos, com.dedsafio4.cofres.ModCofres.COFRE_HUESOS, 1, 0));
		if (t == 70) server.execute(() -> {
			var be = server.overworld().getBlockEntity(pos);
			if (be instanceof com.dedsafio4.cofres.CofreBlockEntity c) {
				c.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
				Dedsafio4ClientDebug.info("cofre huesos: espacios=" + c.getContainerSize() + " nombre=" + c.getDisplayName().getString()
						+ " tipo=" + net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
			} else Dedsafio4ClientDebug.info("cofre huesos: sin inventario " + be);
		});
		if (t == 75) { mc.options.hideGui = false; comando(mc, "item replace entity @s hotbar.0 with dedsafio4:cofre_huesos"); mc.player.getInventory().selected = 0; }
		if (t == 85) captura(mc, "debug_cofre_huesos_mano.png");
		// Romperlo en supervivencia: se suelta el cofre y lo que tenía.
		if (t == 90) comando(mc, "gamemode survival");
		if (t == 95) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.getInventory().clearContent();
			j.gameMode.destroyBlock(pos.relative(net.minecraft.core.Direction.WEST));
		});
		if (t == 105) server.execute(() -> {
			var items = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4));
			StringBuilder sb = new StringBuilder("cofre huesos roto: bloques=" + server.overworld().getBlockState(pos).getBlock() + "," + server.overworld().getBlockState(pos.relative(net.minecraft.core.Direction.WEST)).getBlock() + " soltó:");
			for (var it : items) sb.append(" ").append(it.getItem().getCount()).append("x").append(it.getItem().getHoverName().getString());
			Dedsafio4ClientDebug.info(sb.toString());
			items.forEach(net.minecraft.world.entity.Entity::discard);
		});
		// Botón 6: gas a todos.
		if (t == 110) server.execute(() -> {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> false;
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 60.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			try {
				var m = com.dedsafio4.qumara.QumaraEntity.class.getDeclaredMethod("nacer", net.minecraft.server.level.ServerPlayer.class);
				m.setAccessible(true);
				m.invoke(q, server.getPlayerList().getPlayer(uuid));
			} catch (Exception ex) { Dedsafio4ClientDebug.info("gas6: " + ex); }
		});
		if (t == 280) server.execute(() -> {
			try {
				var m = com.dedsafio4.qumara.QumaraEntity.class.getDeclaredMethod("gasATodos", net.minecraft.server.level.ServerPlayer.class);
				m.setAccessible(true);
				m.invoke(qumaraServidor(server), (Object) null);
			} catch (Exception ex) { Dedsafio4ClientDebug.info("gas6: " + ex); }
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info("gas6: nubes cerca del jugador=" + server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.GasMoradoEntity.class, j.getBoundingBox().inflate(3)).size()
					+ " recarga=" + qumaraServidor(server).recargaGas() / 20 + " s");
		});
		if (t == 290) server.execute(() -> Dedsafio4ClientDebug.info("gas6: veneno primitivo=" + com.dedsafio4.reptisaurios.VenenoPrimitivo.envenenado(server.getPlayerList().getPlayer(uuid))));
		if (t == 300) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> pl.hasPermissions(2);
			comando(mc, "effect clear @s");
			comando(mc, "gamemode creative");
			for (String e : new String[]{"qumara", "gas_morado"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 304) mc.stop();
	}

	private static final boolean CRISTAL = "cristal".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Mineral Verde: con pico suelta Cristal Verde (y experiencia), con Toque de Seda el bloque, con Fortuna III más cristales. */
	private static void tickCristal(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 0.5 0 0");
			comando(mc, "fill 1445 199 5 1455 199 15 minecraft:stone");
			comando(mc, "fill 1445 200 5 1455 202 15 minecraft:air");
		}
		String[] picos = {"minecraft:iron_pickaxe", "minecraft:iron_pickaxe[minecraft:enchantments={levels:{'minecraft:silk_touch':1}}]",
				"minecraft:diamond_pickaxe[minecraft:enchantments={levels:{'minecraft:fortune':3}}]", "minecraft:stone_pickaxe"};
		for (int k = 0; k < picos.length; k++) {
			int base = 20 + k * 40, kk = k;
			if (t == base) comando(mc, "item replace entity @s weapon.mainhand with " + picos[k]);
			if (t == base + 5) server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				var lvl = server.overworld();
				int xp0 = lvl.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, new net.minecraft.world.phys.AABB(1440, 195, 0, 1460, 210, 20)).size();
				java.util.Map<String, Integer> total = new java.util.TreeMap<>();
				for (int i = 0; i < 10; i++) {
					var pos = new net.minecraft.core.BlockPos(1446 + i, 200, 8);
					lvl.setBlock(pos, com.dedsafio4.bloques.ModBloques.MINERAL_VERDE.defaultBlockState(), 3);
					j.gameMode.destroyBlock(pos);
				}
				for (var it : lvl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(1440, 195, 0, 1460, 210, 20))) {
					total.merge(it.getItem().getHoverName().getString(), it.getItem().getCount(), Integer::sum);
					it.discard();
				}
				int xp = lvl.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, new net.minecraft.world.phys.AABB(1440, 195, 0, 1460, 210, 20)).size() - xp0;
				Dedsafio4ClientDebug.info("cristal con " + picos[kk].replaceAll("\\[.*", "") + (kk == 1 ? " (toque de seda)" : kk == 2 ? " (fortuna III)" : "") + ", 10 bloques: " + total + " orbes de xp=" + xp);
			});
		}
		if (t == 180) {
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:cristal_verde 3");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:barra_cristal_verde 2");
			mc.player.getInventory().selected = 0;
		}
		if (t == 190) captura(mc, "debug_cristal.png");
		if (t == 195) { comando(mc, "clear @s"); comando(mc, "gamemode creative"); mc.stop(); }
	}

	private static final boolean BOLSA_ENDER = "bolsaender".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Bolsa de Ender: con click derecho abre el Cofre de Ender del jugador (lo mismo que un Cofre de Ender puesto). */
	private static void tickBolsaEnder(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s enderchest.0 with minecraft:diamond 7");
			comando(mc, "item replace entity @s enderchest.13 with minecraft:golden_apple 2");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:bolsa_ender");
			mc.player.getInventory().selected = 0;
		}
		if (t == 20) { mc.gui.getChat().clearMessages(false); mc.options.keyUse.setDown(true); }
		if (t == 22) mc.options.keyUse.setDown(false);
		if (t == 35) {
			var menu = mc.player.containerMenu;
			Dedsafio4ClientDebug.info("bolsa ender: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getTitle().getString())
					+ " espacios del cofre=" + (menu instanceof net.minecraft.world.inventory.ChestMenu c ? c.getContainer().getContainerSize() : -1)
					+ " espacio 0=" + menu.getSlot(0).getItem() + " espacio 13=" + menu.getSlot(13).getItem());
			captura(mc, "debug_bolsa_ender.png");
		}
		// Sacar los diamantes desde la bolsa y ver que salieron del Cofre de Ender.
		if (t == 40) mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
		if (t == 50) {
			mc.setScreen(null);
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info("bolsa ender: despues de sacar -> cofre de ender espacio 0=" + j.getEnderChestInventory().getItem(0)
						+ " diamantes en el inventario=" + j.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND));
			});
		}
		if (t == 60) { comando(mc, "clear @s"); comando(mc, "gamemode creative"); mc.stop(); }
	}

	private static final boolean CATALOGO = "catalogo".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Catálogo: se abre con la G, los ocultos se ven como "?", y al tocar un ítem con crafteo aparece el crafteo. */
	private static void tickCatalogo(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "catalogo mostrar todo");
		}
		if (t == 10) net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.g"));
		if (t == 20) {
			Dedsafio4ClientDebug.info("catalogo: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName()));
			captura(mc, "debug_catalogo.png");
		}
		if (t == 25) {
			comando(mc, "catalogo ocultar dedsafio4:ambar_con_insecto");
			comando(mc, "catalogo ocultar dedsafio4:pimpollo_qumara");
			comando(mc, "catalogo ocultar dedsafio4:bolsa_ender");
		}
		if (t == 40) {
			Dedsafio4ClientDebug.info("catalogo: ocultos en el cliente=" + com.dedsafio4.catalogo.Catalogo.OCULTOS_CLIENTE);
			captura(mc, "debug_catalogo_ocultos.png");
		}
		if (t == 45 && mc.screen instanceof CatalogoScreen c) {
			c.mostrarReceta(com.dedsafio4.items.ModItems.RACIMO_DILITIO);
			Dedsafio4ClientDebug.info("catalogo: crafteo del racimo=" + c.mostrandoReceta());
		}
		if (t == 55) captura(mc, "debug_catalogo_crafteo.png");
		if (t == 60 && mc.screen instanceof CatalogoScreen c) {
			c.mouseClicked(0, 0, 0);
			c.mostrarReceta(com.dedsafio4.items.ModItems.CRISTAL_VERDE);
			Dedsafio4ClientDebug.info("catalogo: crafteo del cristal verde (no tiene)=" + c.mostrandoReceta());
		}
		if (t == 65) net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey("key.keyboard.g"));
		if (t == 70) {
			Dedsafio4ClientDebug.info("catalogo: despues de la G otra vez, pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName()));
			comando(mc, "catalogo mostrar todo");
		}
		if (t == 72) {
			if (mc.screen == null) mc.setScreen(new CatalogoScreen());
		}
		if (t == 74 && mc.screen instanceof CatalogoScreen c) {
			c.buscarPrueba("casco");
			StringBuilder sb = new StringBuilder("catalogo: buscar 'casco' -> " + c.resultados().size() + ":");
			for (int k = 0; k < Math.min(8, c.resultados().size()); k++) sb.append(" ").append(new net.minecraft.world.item.ItemStack(c.resultados().get(k)).getHoverName().getString());
			Dedsafio4ClientDebug.info(sb.toString());
		}
		if (t == 84) captura(mc, "debug_catalogo_buscar.png");
		if (t == 86 && mc.screen instanceof CatalogoScreen c) {
			c.mostrarReceta(net.minecraft.world.item.Items.DIAMOND_HELMET);
			Dedsafio4ClientDebug.info("catalogo: crafteo del casco de diamante=" + c.mostrandoReceta());
		}
		if (t == 96) captura(mc, "debug_catalogo_buscar_crafteo.png");
		if (t == 100) mc.stop();
	}

	private static final boolean MISIONES = "misiones".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Misiones: la pestaña del pergamino; al craftear el Casco de Diamante la misión se completa (verde). */
	private static void tickMisiones(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				for (var it : new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.DIAMOND_HELMET, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,
						net.minecraft.world.item.Items.DIAMOND_LEGGINGS, net.minecraft.world.item.Items.DIAMOND_BOOTS})
					j.getStats().setValue(j, net.minecraft.stats.Stats.ITEM_CRAFTED.get(it), 0);
			});
		}
		if (t == 30) {
			var c = new CatalogoScreen();
			mc.setScreen(c);
			c.abrirMisiones();
		}
		if (t == 40) {
			Dedsafio4ClientDebug.info("misiones: progreso=" + java.util.Arrays.toString(com.dedsafio4.catalogo.Misiones.PROGRESO_CLIENTE));
			captura(mc, "debug_misiones.png");
		}
		// Craftear el casco (como si lo sacara de la mesa): cuenta en las estadísticas.
		if (t == 45) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.awardStat(net.minecraft.stats.Stats.ITEM_CRAFTED.get(net.minecraft.world.item.Items.DIAMOND_HELMET), 1);
		});
		if (t == 80) {
			Dedsafio4ClientDebug.info("misiones: progreso despues de craftear el casco=" + java.util.Arrays.toString(com.dedsafio4.catalogo.Misiones.PROGRESO_CLIENTE));
			captura(mc, "debug_misiones_completa.png");
		}
		if (t == 84) { mc.setScreen(new CatalogoScreen()); }
		if (t == 94) captura(mc, "debug_catalogo_marco.png");
		if (t == 98) mc.stop();
	}

	private static final boolean CATALOGO_EDITAR = "catalogoeditar".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Editar el Catálogo: mover un ítem, poner uno nuevo (de la lupa), sacar otro; /opop y /catalogo reiniciar. */
	private static void tickCatalogoEditar(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		java.util.function.Consumer<String> log = cuando -> Dedsafio4ClientDebug.info("editar " + cuando + ": editor=" + com.dedsafio4.catalogo.Catalogo.PUEDE_EDITAR_CLIENTE
				+ " (0,0,0)=" + com.dedsafio4.catalogo.Catalogo.itemEnCliente(0, 0, 0) + " (1,0,0)=" + com.dedsafio4.catalogo.Catalogo.itemEnCliente(1, 0, 0)
				+ " (0,6,0)=" + com.dedsafio4.catalogo.Catalogo.itemEnCliente(0, 6, 0) + " (0,6,1)=" + com.dedsafio4.catalogo.Catalogo.itemEnCliente(0, 6, 1));
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "catalogo reiniciar");
			comando(mc, "catalogo mostrar todo");
			comando(mc, "opop " + mc.player.getGameProfile().getName());
		}
		if (t == 15) { log.accept("al empezar"); mc.setScreen(new CatalogoScreen()); }
		if (t == 20 && mc.screen instanceof CatalogoScreen c) {
			c.elegirPrueba(1, 0, 0, "", -1);            // vacía la casilla de la Semilla Solaria
			c.elegirPrueba(0, 6, 0, "diamond", 0);      // pone el primer "diamond" que encuentra
			Dedsafio4ClientDebug.info("editar: buscar 'diamond' en el panel -> " + c.resultadosCasilla().size() + " resultados, el primero " + c.resultadosCasilla().get(0));
			c.elegirPrueba(0, 6, 1, "dilitio", 0);
			c.elegirPrueba(0, 0, 0, "", -1);            // vacía la Cuchara de Madera
		}
		if (t == 24 && mc.screen instanceof CatalogoScreen c) c.elegirPrueba(0, 1, 0, "ambar", -2);   // deja abierto el panel (para la foto)
		if (t == 35) { log.accept("despues de editar (con lo que mandó el servidor)"); captura(mc, "debug_catalogo_editar.png"); }
		if (t == 38) { mc.setScreen(null); mc.gui.getChat().clearMessages(false); comando(mc, "G guardar"); }
		if (t == 39) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				var d = server.overworld().getDataStorage().get(com.dedsafio4.catalogo.Catalogo.Datos.FACTORY, "dedsafio4_catalogo");
				String codigo = com.dedsafio4.catalogo.Catalogo.aCodigo(com.dedsafio4.catalogo.Catalogo.disenoPorDefecto());
				var vuelta = com.dedsafio4.catalogo.Catalogo.desdeCodigo(codigo);
				Dedsafio4ClientDebug.info("G guardar: codigo del diseno de siempre = " + codigo.length() + " letras; al cargarlo da " + vuelta.size()
						+ " casillas, igual=" + vuelta.equals(com.dedsafio4.catalogo.Catalogo.disenoPorDefecto()));
			});
		}
		if (t == 39) captura(mc, "debug_g_guardar.png");
		if (t == 40) { comando(mc, "catalogo reiniciar"); comando(mc, "deopop " + mc.player.getGameProfile().getName()); }
		if (t == 55) log.accept("despues de reiniciar");
		if (t == 60) mc.stop();
	}

	private static final boolean CAPSULA = "capsula".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Cápsula de Pastilla Vacía: el tooltip en el inventario y el crafteo en la G. */
	private static void tickCapsula(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			if (mc.screen != null) mc.setScreen(null);
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:pildora_vacia 3");
		}
		if (t == 10) {
			var pila = new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.PILDORA_VACIA);
			for (var l : pila.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL))
				Dedsafio4ClientDebug.info("capsula tooltip: " + l.getString());
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) -> {
				if (pantalla instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((pp, g, mx, my, delta) -> g.renderTooltip(cliente.font, pila, 20, 20));
			});
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 20) captura(mc, "debug_capsula_inventario.png");
		if (t == 25) {
			var c = new CatalogoScreen();
			mc.setScreen(c);
			c.mostrarReceta(com.dedsafio4.items.ModItems.PILDORA_VACIA);
			Dedsafio4ClientDebug.info("capsula: crafteo en la G=" + c.mostrandoReceta());
		}
		if (t == 35) captura(mc, "debug_capsula_g.png");
		if (t == 40) { mc.setScreen(null); mc.stop(); }
	}

	private static final boolean G_ESCALA = "gescala".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La G con Gui Scale 1, 2, 3 y 4 (tiene que ocupar la pantalla igual). */
	private static void tickGEscala(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		for (int e = 1; e <= 4; e++) {
			int base = 10 + (e - 1) * 20, ee = e;
			if (t == base) {
				mc.options.guiScale().set(ee);
				mc.resizeDisplay();
				var c = new CatalogoScreen();
				mc.setScreen(c);
				if (ee == 3) c.abrirMisiones();
				if (ee == 4) c.mostrarReceta(com.dedsafio4.items.ModItems.RACIMO_DILITIO);
			}
			if (t == base + 10) {
				Dedsafio4ClientDebug.info("g escala " + ee + ": pantalla " + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight());
				captura(mc, "debug_g_escala" + ee + ".png");
			}
		}
		if (t == 95) {
			mc.options.guiScale().set(0);
			mc.resizeDisplay();
			mc.setScreen(null);
			mc.stop();
		}
	}

	private static final double[] lanzamiento = new double[2];
	private static final net.minecraft.world.phys.AABB caja0 = new net.minecraft.world.phys.AABB(1330, 180, -90, 1570, 260, 150);
	private static final boolean ATRAER = "atraer".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Qumara botón 5: te atrae desde lejos, pegado a ella te saca 5 corazones (diamante Prot IV), y las flechas la lastiman. */
	private static void tickAtraer(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 80");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 80 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 80 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 240 -60.5 0 30");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 30.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			q.accion(server.getPlayerList().getPlayer(uuid), 1);
		});
		// Flecha contra Qumara ya nacida.
		if (t == 200) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			float antes = q.getHealth();
			var flecha = new net.minecraft.world.entity.projectile.Arrow(server.overworld(), j, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
			flecha.moveTo(1450.5, 215, 0.5, 0, 0);
			flecha.shoot(0, 0, 1, 3f, 0);
			server.overworld().addFreshEntity(flecha);
			Dedsafio4ClientDebug.info("atraer: flecha tirada, vida de qumara=" + antes);
		});
		if (t == 230) server.execute(() -> Dedsafio4ClientDebug.info("atraer: despues de la flecha, vida de qumara=" + qumaraServidor(server).getHealth()));
		if (t > 200 && t < 214) server.execute(() -> server.overworld().getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class, caja0, f -> f.getZ() < 60)
				.forEach(f -> Dedsafio4ClientDebug.info(String.format("atraer: flecha t=%d en %.2f,%.2f,%.2f v=%.2f encuentra=%s qumara caja=%s herida=%d ", t, f.getX(), f.getY(), f.getZ(),
						f.getDeltaMovement().length(), server.overworld().getEntities(f, f.getBoundingBox().expandTowards(f.getDeltaMovement()).inflate(1), e -> true),
						qumaraServidor(server).getBoundingBox(), qumaraServidor(server).invulnerableTime))));
		var caja = new net.minecraft.world.phys.AABB(1330, 180, -90, 1570, 260, 150);
		java.util.function.Supplier<java.util.List<com.dedsafio4.qumara.BichoCuboEntity>> cabezas = () ->
				server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> b.deLaCabeza() && b.isAlive());
		// Enfriamiento: 5 capullos, uno para cada bicho no.
		if (t == 240) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			q.invulnerableTime = 0;
			q.hurt(j.damageSources().playerAttack(j), 300);
			Dedsafio4ClientDebug.info("atraer: enfriamiento=" + q.debil() + " vida=" + q.getHealth());
		});
		if (t == 250) server.execute(() -> {
			var q = qumaraServidor(server);
			var nos = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> !b.deLaCabeza());
			var caps = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja);
			StringBuilder sb = new StringBuilder("atraer: bichos no=" + nos.size() + " capullos=" + caps.size() + " distancias capullo->bicho no mas cercano:");
			for (var c : caps) sb.append(String.format(" %.1f", nos.stream().mapToDouble(b -> Math.hypot(b.getX() - c.getX(), b.getZ() - c.getZ())).min().orElse(-1)));
			Dedsafio4ClientDebug.info(sb.toString());
		});
		if (t == 255) server.execute(() -> {
			var cap = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja).get(0);
			var j = server.getPlayerList().getPlayer(uuid);
			double dx = cap.getX() - qumaraServidor(server).getX(), dz = cap.getZ() - qumaraServidor(server).getZ(), d = Math.hypot(dx, dz);
			// Parado afuera del capullo, mirándolo de costado.
			j.teleportTo(server.overworld(), cap.getX() + dx / d * 26, cap.getY() + 6, cap.getZ() + dz / d * 26,
					(float) Math.toDegrees(Math.atan2(-dx, dz)) + 180, 5);
			j.getAbilities().flying = true;
			j.onUpdateAbilities();
		});
		if (t == 262) server.execute(() -> {
			var cap = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja).get(0);
			var j = server.getPlayerList().getPlayer(uuid);
			cap.hurt(j.damageSources().playerAttack(j), 1);
			Dedsafio4ClientDebug.info(String.format("atraer: golpe al capullo (y=%.0f), bichos de la cabeza=%d", cap.getY(), cabezas.get().size()));
		});
		for (int k = 264; k <= 310; k += 6) {
			int kk = k;
			if (t == k) server.execute(() -> {
				var cap = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja).get(0);
				cabezas.get().stream().findFirst().ifPresent(b -> Dedsafio4ClientDebug.info(String.format("atraer: bicho que sale t=%d: altura sobre el capullo=%.1f distancia=%.1f",
						kk - 262, b.getY() - cap.getY(), Math.hypot(b.getX() - cap.getX(), b.getZ() - cap.getZ()))));
			});
		}
		if (t == 270) captura(mc, "debug_at0_capullo.png");
		// Agarrarlo y tirarlo.
		if (t == 315) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			var b = cabezas.get().get(0);
			j.getAbilities().flying = false;
			j.onUpdateAbilities();
			j.teleportTo(server.overworld(), b.getX() + 1.5, b.getY(), b.getZ(), 90, 20);
		});
		if (t == 325) mc.level.getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, mc.player.getBoundingBox().inflate(6), b -> b.deLaCabeza())
				.stream().findFirst().ifPresent(b -> mc.gameMode.interact(mc.player, b, net.minecraft.world.InteractionHand.MAIN_HAND));
		if (t == 332) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			var b = cabezas.get().get(0);
			Dedsafio4ClientDebug.info("atraer: en la cabeza=" + (b.getVehicle() == j));
			lanzamiento[0] = j.getX(); lanzamiento[1] = j.getZ();
			b.lanzar(j);
		});
		if (t == 372) server.execute(() -> {
			var b = cabezas.get().stream().findFirst();
			Dedsafio4ClientDebug.info(b.map(x -> String.format("atraer: tirado cayo a %.1f bloques del jugador, sigue vivo (edad %.1f s)",
					Math.hypot(x.getX() - lanzamiento[0], x.getZ() - lanzamiento[1]), x.tickCount / 20f)).orElse("atraer: el tirado ya no esta"));
		});
		if (t == 400 || t == 500 || t == 640 || t == 670) server.execute(() -> {
			StringBuilder sb = new StringBuilder("atraer: t=" + t + " bichos de la cabeza (edad s, lanzado?):");
			for (var b : cabezas.get()) sb.append(String.format(" [%.1f %.0f,%.0f,%.0f]", b.tickCount / 20f, b.getX(), b.getY(), b.getZ()));
			Dedsafio4ClientDebug.info(sb.toString());
		});
		if (t == 680) server.execute(() -> Dedsafio4ClientDebug.info("atraer: a los 20,9 s bichos de la cabeza vivos=" + cabezas.get().size()));
		// Matar los bichos no para que se recupere.
		if (t == 685) server.execute(() -> server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> !b.deLaCabeza())
				.forEach(b -> b.hurt(server.overworld().damageSources().explosion(null, null), 100)));
		// Jugador común (no admin) en supervivencia con diamante Prot IV, a 80 bloques.
		if (t == 700) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> false;
			comando(mc, "gamemode survival");
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1] + "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 -49.5 0 0");
		}
		if (t == 710) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.setHealth(j.getMaxHealth());
			Dedsafio4ClientDebug.info("atraer: qumara enfriamiento=" + qumaraServidor(server).debil());
			qumaraServidor(server).accion(j, 5);
		});
		if (t > 710 && t <= 710 + 330 && (t - 710) % 10 == 0) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info(String.format("atraer t=%.1fs: distancia=%.1f vida=%.2f atrayendo=%b", (t - 710) / 20f,
					Math.hypot(j.getX() - q.getX(), j.getZ() - q.getZ()), j.getHealth(), q.atrayendo()));
		});
		if (t == 710 + 30) captura(mc, "debug_at1.png");
		if (t == 1060) {
			com.dedsafio4.qumara.QumaraEntity.esAdmin = pl -> pl.hasPermissions(2);
			comando(mc, "clear @s");
			comando(mc, "gamemode creative");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "forceload remove all");
		}
		if (t == 1066) mc.stop();
	}

	private static final boolean CIRCULOS = "circulos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Círculos Explosivos: cómo se ven montado en Qumara (nivel 3) y cuánto sacan en cada nivel con diamante y Protección IV. */
	private static void tickCirculos(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 80");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza", "circulo"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 80 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 80 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 240 60.5 180 30");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 60.5, 180, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			q.accion(server.getPlayerList().getPlayer(uuid), 1);
		});
		// Ya nacida: bajarle la vida a nivel 3 (dos pinchitos) y montarla.
		if (t == 200) server.execute(() -> {
			var q = qumaraServidor(server);
			q.setHealth(q.getMaxHealth() * 0.2f);
			Dedsafio4ClientDebug.info("circulos: nivel con 20% de vida=" + q.nivelCirculos() + " pinchitos=" + q.pinchitos());
			server.getPlayerList().getPlayer(uuid).startRiding(q, true);
		});
		if (t == 202) server.execute(() -> Dedsafio4ClientDebug.info("circulos: montado servidor=" + server.getPlayerList().getPlayer(uuid).getVehicle()));
		if (t == 208) Dedsafio4ClientDebug.info("circulos: montado cliente=" + mc.player.getVehicle() + " ojos y=" + mc.player.getEyeY());
		if (t >= 205 && t < 330) mc.player.setXRot(50);
		if (t == 210) server.execute(() -> com.dedsafio4.qumara.QumaraEntity.ponerCirculos(server.overworld(), 1444.5, 200, 30.5, 4, 0));
		if (t == 210 + 15) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_ci1.png"); }
		if (t == 210 + 38) captura(mc, "debug_ci2.png");
		if (t == 210 + 55) captura(mc, "debug_ci3.png");
		if (t == 210 + 62) captura(mc, "debug_ci4_explota.png");
		if (t == 210 + 95) captura(mc, "debug_ci5_quemado.png");
		// Daño por nivel: supervivencia, diamante con Protección IV, parado en el centro.
		if (t == 400) {
			server.execute(() -> server.getPlayerList().getPlayer(uuid).stopRiding());
			comando(mc, "kill @e[type=dedsafio4:qumara]");
			comando(mc, "gamemode survival");
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1] + "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
			comando(mc, "execute in minecraft:overworld run tp @s 1440.5 200 -20.5 0 0");
		}
		for (int nivel = 1; nivel <= 4; nivel++) {
			int base = 420 + (nivel - 1) * 120, n = nivel;
			if (t == base) server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				j.setHealth(j.getMaxHealth());
				com.dedsafio4.qumara.QumaraEntity.ponerCirculos(server.overworld(), j.getX(), j.getY(), j.getZ(), n, 0);
			});
			if (t == base + 100) server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info(String.format("circulo nivel %d: perdio %.2f de vida (%.2f corazones)", n, j.getMaxHealth() - j.getHealth(), (j.getMaxHealth() - j.getHealth()) / 2));
			});
		}
		if (t == 900) {
			comando(mc, "clear @s");
			comando(mc, "gamemode creative");
			comando(mc, "kill @e[type=dedsafio4:circulo]");
			comando(mc, "forceload remove all");
		}
		if (t == 904) mc.stop();
	}

	private static final boolean PINCHITOS = "pinchitos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Qumara: la vida se frena en cada pinchito (75/50/25%) y entra en Enfriamiento con 5 Bichos no de la cabeza; al matar los 5 se recupera. */
	private static void tickPinchitos(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		var caja = new net.minecraft.world.phys.AABB(1380, 180, -80, 1520, 260, 80);
		java.util.function.Supplier<java.util.List<com.dedsafio4.qumara.BichoCuboEntity>> nos = () ->
				server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> !b.deLaCabeza() && b.isAlive());
		java.util.function.Consumer<String> log = cuando -> {
			var q = qumaraServidor(server);
			Dedsafio4ClientDebug.info("pinchitos " + cuando + ": vida=" + q.getHealth() + " enfriamiento=" + q.debil() + " pinchitos=" + q.pinchitos()
					+ " bichos NO=" + nos.get().size() + " derrotada=" + q.derrotada());
		};
		java.util.function.IntConsumer pegar = cant -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			q.invulnerableTime = 0;
			q.hurt(j.damageSources().playerAttack(j), cant);
		};
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 80");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 80 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 80 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 240 60.5 180 30");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 0.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			q.accion(server.getPlayerList().getPlayer(uuid), 1);
		});
		for (int ronda = 0; ronda < 3; ronda++) {
			int base = 170 + ronda * 60;
			int r = ronda;
			if (t == base) server.execute(() -> { pegar.accept(900); log.accept("ronda " + (r + 1) + " golpe de 900"); });
			if (t == base + 5) server.execute(() -> { pegar.accept(50); log.accept("ronda " + (r + 1) + " golpe en enfriamiento"); });
			if (t == base + 10) server.execute(() -> {
				var l = nos.get();
				for (int i = 0; i < l.size() - 1; i++) l.get(i).hurt(server.overworld().damageSources().explosion(null, null), 100);
				log.accept("ronda " + (r + 1) + " matados 4");
			});
			if (t == base + 30) server.execute(() -> log.accept("ronda " + (r + 1) + " con 1 vivo"));
			if (t == base + 32) server.execute(() -> nos.get().forEach(b -> b.hurt(server.overworld().damageSources().explosion(null, null), 100)));
			if (t == base + 50) server.execute(() -> log.accept("ronda " + (r + 1) + " matados los 5"));
		}
		if (t == 168) mc.gui.getChat().clearMessages(false);
		if (t == 170 + 5) captura(mc, "debug_pi_enfriamiento.png");
		if (t == 222) server.execute(() -> { pegar.accept(130); log.accept("despues de la ronda 1, golpe de 130"); });
		if (t == 223) mc.gui.getChat().clearMessages(false);
		if (t == 226) captura(mc, "debug_pi_62.png");
		if (t == 360) server.execute(() -> { pegar.accept(900); log.accept("golpe final"); });
		if (t == 380) {
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "forceload remove all");
		}
		if (t == 384) mc.stop();
	}

	private static final boolean CAPULLO = "capullo".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Enfriamiento: bicho NO a 30, capullo a 20 del bicho (se cierra), golpes al capullo largan bichos, mecha de 30 s, y al morir el NO se recupera. */
	private static void tickCapullo(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		var caja = new net.minecraft.world.phys.AABB(1380, 180, -80, 1520, 260, 80);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1380 -80 1520 80");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "clear @s");
		}
		if (t == 20) {
			comando(mc, "fill 1380 199 -80 1520 199 80 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 3) comando(mc, "fill 1380 " + y + " -80 1520 " + (y + 2) + " 80 minecraft:air");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 0.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			q.accion(server.getPlayerList().getPlayer(uuid), 1);
		});
		if (t == 170) server.execute(() -> qumaraServidor(server).accion(server.getPlayerList().getPlayer(uuid), 3));
		if (t == 180) server.execute(() -> {
			var q = qumaraServidor(server);
			var no = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> !b.deLaCabeza()).get(0);
			var cap = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja).get(0);
			Dedsafio4ClientDebug.info(String.format("capullo: bicho NO a %.1f de Qumara; capullo a %.1f del bicho NO; bichos de la cabeza=%d",
					Math.hypot(no.getX() - q.getX(), no.getZ() - q.getZ()), Math.hypot(cap.getX() - no.getX(), cap.getZ() - no.getZ()),
					server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> b.deLaCabeza()).size()));
			var j = server.getPlayerList().getPlayer(uuid);
			j.teleportTo(cap.getX() + 26, cap.getY() + 9, cap.getZ());
			j.getAbilities().flying = true;
			j.onUpdateAbilities();
		});
		if (t == 183) {
			mc.level.getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, mc.player.getBoundingBox().inflate(40)).stream().findFirst().ifPresent(cap -> {
				var d = cap.position().add(0, 6, 0).subtract(mc.player.getEyePosition());
				mirar(mc, (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
			});
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 188) captura(mc, "debug_ca_cerrado.png");
		// Golpes al capullo (cerrado): tres golpes, separados.
		for (int k = 0; k < 3; k++) {
			if (t == 190 + k * 15) server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				var cap = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja).get(0);
				cap.hurt(j.damageSources().playerAttack(j), 5);
			});
		}
		if (t == 236) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			float antes = q.getHealth();
			boolean pego = q.hurt(j.damageSources().playerAttack(j), 50);
			Dedsafio4ClientDebug.info("qumara en enfriamiento: le pega=" + pego + " vida " + antes + " -> " + q.getHealth());
		});
		if (t == 860) server.execute(() -> {
			var q = qumaraServidor(server);
			var j = server.getPlayerList().getPlayer(uuid);
			float antes = q.getHealth();
			q.invulnerableTime = 0;
			boolean pego = q.hurt(j.damageSources().playerAttack(j), 50);
			Dedsafio4ClientDebug.info("qumara recuperada: le pega=" + pego + " vida " + antes + " -> " + q.getHealth());
		});
		if (t == 240) server.execute(() -> Dedsafio4ClientDebug.info("capullo golpeado 3 veces: bichos de la cabeza="
				+ server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> b.deLaCabeza()).size()));
		if (t == 242) captura(mc, "debug_ca_bichos.png");
		if (t == 190 + 600 + 10) server.execute(() -> Dedsafio4ClientDebug.info("a los 30 s: bichos de la cabeza que quedan="
				+ server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> b.deLaCabeza()).size()));
		// Matar al bicho NO: Qumara se recupera y el capullo se abre.
		if (t == 820) server.execute(() -> {
			var nos = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, caja, b -> !b.deLaCabeza());
			if (nos.isEmpty()) { Dedsafio4ClientDebug.info("el NO ya murio con las explosiones"); return; }
			var no = nos.get(0);
			Dedsafio4ClientDebug.info("antes de matar al NO: qumara debil=" + qumaraServidor(server).debil());
			no.hurt(server.overworld().damageSources().explosion(null, null), 50);
		});
		if (t == 850) server.execute(() -> {
			var cap = server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, caja).get(0);
			Dedsafio4ClientDebug.info("despues de matar al NO: qumara debil=" + qumaraServidor(server).debil() + " capullo abierto=" + cap.abierto());
		});
		if (t == 896) {
			comando(mc, "gamemode creative");
			mc.level.getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, mc.player.getBoundingBox().inflate(80)).stream().findFirst().ifPresent(cap ->
					comando(mc, String.format(java.util.Locale.ROOT, "tp @s %.1f %.1f %.1f", cap.getX() + 26, cap.getY() + 9, cap.getZ())));
		}
		if (t == 898) mc.level.getEntitiesOfClass(com.dedsafio4.qumara.CapulloEntity.class, mc.player.getBoundingBox().inflate(80)).stream().findFirst().ifPresent(cap -> {
			var d = cap.position().add(0, 6, 0).subtract(mc.player.getEyePosition());
			mirar(mc, (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
			mc.gui.getChat().clearMessages(false);
		});
		if (t == 902) captura(mc, "debug_ca_abierto.png");
		if (t == 906) {
			for (String e : new String[]{"qumara", "capullo", "bicho_cabeza", "bicho_no_cabeza"}) comando(mc, "kill @e[type=dedsafio4:" + e + "]");
			comando(mc, "forceload remove all");
		}
		if (t == 910) mc.stop();
	}

	private static final boolean BICHOS = "bichos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Enfriamiento: salen los bichos; el de la cabeza se agarra, se tira contra el otro y explota; y en la cabeza 2 s. */
	private static void tickBichos(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Supplier<java.util.List<com.dedsafio4.qumara.BichoCuboEntity>> bichos = () ->
				server.overworld().getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, new net.minecraft.world.phys.AABB(1400, 180, -60, 1500, 260, 60));
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1400 -60 1500 60");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			comando(mc, "kill @e[type=dedsafio4:qumara]");
			comando(mc, "kill @e[type=dedsafio4:bicho_cabeza]");
			comando(mc, "kill @e[type=dedsafio4:bicho_no_cabeza]");
			comando(mc, "clear @s");
		}
		if (t == 20) {
			comando(mc, "fill 1410 199 -40 1490 199 40 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 4) comando(mc, "fill 1410 " + y + " -40 1490 " + (y + 3) + " 40 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 200 30.5 180 10");
		}
		if (t == 30) server.execute(() -> {
			var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
			q.moveTo(1450.5, 200, 0.5, 0, 0);
			server.overworld().addFreshEntity(q);
			qumaraPrueba = q.getUUID();
			var j = server.getPlayerList().getPlayer(uuid);
			q.accion(j, 1);
		});
		if (t == 170) server.execute(() -> qumaraServidor(server).accion(server.getPlayerList().getPlayer(uuid), 3));
		if (t == 180) {
			server.execute(() -> {
				StringBuilder sb = new StringBuilder("bichos salieron:");
				var q = qumaraServidor(server);
				for (var b : bichos.get()) sb.append(String.format(" [%s a %.1f]", b.deLaCabeza() ? "cabeza" : "NO", Math.hypot(b.getX() - q.getX(), b.getZ() - q.getZ())));
				Dedsafio4ClientDebug.info(sb.toString());
				var no = bichos.get().stream().filter(b -> !b.deLaCabeza()).findFirst().get();
				boolean espada = no.hurt(server.getPlayerList().getPlayer(uuid).damageSources().playerAttack(server.getPlayerList().getPlayer(uuid)), 10);
				boolean flecha = no.hurt(server.overworld().damageSources().magic(), 10);
				Dedsafio4ClientDebug.info("bicho NO: vida=" + no.getHealth() + " espada pega=" + espada + " otra cosa pega=" + flecha);
			});
		}
		if (t == 184) { comando(mc, "tp @s 1450.5 222 36.5 180 30"); mc.gui.getChat().clearMessages(false); }
		if (t == 186) captura(mc, "debug_bi1_salen.png");
		// Agarrar uno de la cabeza (click derecho de verdad) y tirarlo contra el bicho NO.
		if (t == 190) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			var no = bichos.get().stream().filter(b -> !b.deLaCabeza()).findFirst().get();
			var si = bichos.get().stream().filter(b -> b.deLaCabeza()).findFirst().get();
			// Pararse entre el de la cabeza y el NO, mirando al de la cabeza.
			var haciaNo = no.position().subtract(si.position()).normalize();
			var pos = si.position().add(haciaNo.scale(3.5));
			j.teleportTo(pos.x, pos.y, pos.z);
			j.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, si.position().add(0, 2, 0));
		});
		if (t == 196) {
			var si = mc.level.getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, mc.player.getBoundingBox().inflate(8), b -> b.deLaCabeza()).stream().findFirst();
			si.ifPresent(b -> mc.gameMode.interact(mc.player, b, net.minecraft.world.InteractionHand.MAIN_HAND));
		}
		if (t == 202) Dedsafio4ClientDebug.info("bicho en la cabeza: " + mc.player.getPassengers());
		if (t == 204) { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); mc.gui.getChat().clearMessages(false); }
		if (t == 205) captura(mc, "debug_bi2_cabeza.png");
		if (t == 207) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		if (t == 206 || t == 208) {
			mc.level.getEntitiesOfClass(com.dedsafio4.qumara.BichoCuboEntity.class, mc.player.getBoundingBox().inflate(40), b -> !b.deLaCabeza())
					.stream().findFirst().ifPresent(no -> {
						var d = no.position().add(0, 2, 0).subtract(mc.player.getEyePosition());
						float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
						float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))) - 12;   // un poco para arriba: hace una curva
						mirar(mc, yaw, pitch);
					});
		}
		if (t == 210) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.dedsafio4.qumara.ModQumara.LanzarPayload());
		if (t >= 210 && t < 240) server.execute(() -> {
			var no = bichos.get().stream().filter(b -> !b.deLaCabeza()).findFirst().get();
			for (var b : bichos.get()) if (b.deLaCabeza() && !b.isPassenger() && b.getDeltaMovement().lengthSqr() > 0.01)
				Dedsafio4ClientDebug.info(String.format("bicho volando: %.1f,%.1f,%.1f dist al NO=%.1f", b.getX(), b.getY(), b.getZ(), b.distanceTo(no)));
		});
		if (t == 240) server.execute(() -> {
			var no = bichos.get().stream().filter(b -> !b.deLaCabeza()).findFirst();
			Dedsafio4ClientDebug.info("despues del tiro: bicho NO vida=" + no.map(b -> "" + b.getHealth()).orElse("muerto") + " bichos de la cabeza=" + bichos.get().stream().filter(b -> b.deLaCabeza()).count());
		});
		// Otro de la cabeza: dejarlo 2 s encima, con diamante y Protección IV.
		if (t == 250) {
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1] + "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
		}
		if (t == 256) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.setHealth(j.getMaxHealth());
			var si = bichos.get().stream().filter(b -> b.deLaCabeza()).findFirst().get();
			si.startRiding(j, true);
			Dedsafio4ClientDebug.info("en la cabeza 2 s: vida antes=" + j.getHealth());
		});
		if (t == 256 + 50) server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			Dedsafio4ClientDebug.info("en la cabeza 2 s: vida despues=" + j.getHealth() + " (perdio " + (j.getMaxHealth() - j.getHealth()) + ")");
		});
		if (t == 256 + 56) {
			comando(mc, "clear @s");
			comando(mc, "kill @e[type=dedsafio4:qumara]");
			comando(mc, "kill @e[type=dedsafio4:bicho_cabeza]");
			comando(mc, "kill @e[type=dedsafio4:bicho_no_cabeza]");
			comando(mc, "forceload remove all");
		}
		if (t == 256 + 60) mc.stop();
	}

	private static final boolean QUMARA = "qumara".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));
	private static java.util.UUID qumaraPrueba;

	private static com.dedsafio4.qumara.QumaraEntity qumaraServidor(net.minecraft.server.MinecraftServer server) {
		return qumaraPrueba == null ? null : (com.dedsafio4.qumara.QumaraEntity) server.overworld().getEntity(qumaraPrueba);
	}

	/** Qumara: rosa, nacer, giro (y a quién le pega), enfriamiento, y derrotada. */
	private static void tickQumara(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1400 -60 1500 60");
			comando(mc, "gamemode creative");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "time set 6000");
			comando(mc, "momento eclipse parar");
			comando(mc, "kill @e[type=dedsafio4:qumara]");
			comando(mc, "clear @s");
		}
		if (t == 20) {
			comando(mc, "fill 1410 199 -40 1490 199 40 minecraft:grass_block");
			for (int y = 200; y <= 250; y += 4) comando(mc, "fill 1410 " + y + " -40 1490 " + (y + 3) + " 40 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1450.5 215 58.5 180 8");
		}
		if (t == 30) {
			server.execute(() -> {
				var q = new com.dedsafio4.qumara.QumaraEntity(com.dedsafio4.qumara.ModQumara.QUMARA, server.overworld());
				q.moveTo(1450.5, 200, 0.5, 0, 0);
				server.overworld().addFreshEntity(q);
				qumaraPrueba = q.getUUID();
			});
		}
		if (t == 36) mc.gui.getChat().clearMessages(false);
		if (t > 20) { mc.player.getAbilities().flying = true; mc.player.setDeltaMovement(0, 0, 0); }
		if (t == 50) { mc.options.hideGui = true; comando(mc, "tp @s 1450.5 215 58.5 180 8"); }
		if (t == 51) captura(mc, "debug_qu1_rosa.png");
		// Nacer (con el botón, montado).
		if (t == 52) server.execute(() -> qumaraServidor(server).accion(server.getPlayerList().getPlayer(uuid), 1));
		if (t == 52 + 50) captura(mc, "debug_qu2_naciendo.png");
		if (t == 52 + 75) captura(mc, "debug_qu3_naciendo.png");
		if (t == 52 + 110) captura(mc, "debug_qu4_grito.png");
		if (t == 52 + 140) captura(mc, "debug_qu5_nacida.png");
		if (t == 52 + 142) comando(mc, "tp @s 1450.5 226 30.5 180 0");
		if (t == 52 + 148) captura(mc, "debug_qu5_cara.png");
		if (t == 52 + 150) comando(mc, "tp @s 1485.5 222 0.5 90 0");
		if (t == 52 + 156) { mc.options.hideGui = false; captura(mc, "debug_qu5_costado.png"); }
		// Giro: un zombi a 20 bloques de un costado y otro a 40 (fuera del alcance).
		if (t == 200) {
			comando(mc, "summon minecraft:zombie 1470.5 200 0.5 {active_effects:[{id:\"minecraft:fire_resistance\",duration:-1}],NoAI:1b,PersistenceRequired:1b,Fire:-1s,Invulnerable:0b,CustomName:'\"cerca\"'}");
			comando(mc, "summon minecraft:zombie 1450.5 200 -38.5 {active_effects:[{id:\"minecraft:fire_resistance\",duration:-1}],NoAI:1b,PersistenceRequired:1b,CustomName:'\"lejos\"'}");
			comando(mc, "tp @s 1450.5 250 40.5 180 45");
		}
		if (t == 208) { mc.options.hideGui = true; comando(mc, "tp @s 1450.5 268 38.5 180 62"); }
		if (t == 210) server.execute(() -> qumaraServidor(server).accion(server.getPlayerList().getPlayer(uuid), 2));
		if (t == 210 + 50) captura(mc, "debug_qu6_giro.png");
		if (t == 210 + 75) captura(mc, "debug_qu7_giro.png");
		if (t == 210 + 95) captura(mc, "debug_qu7b_giro.png");
		if (t == 210 + 150) {
			server.execute(() -> {
				StringBuilder sb = new StringBuilder("qumara giro:");
				for (var z : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
						new net.minecraft.world.phys.AABB(1400, 190, -60, 1500, 230, 60))) {
					sb.append(" [").append(z.getCustomName() == null ? "?" : z.getCustomName().getString()).append(" vida=").append(z.getHealth()).append("]");
				}
				Dedsafio4ClientDebug.info(sb.toString());
			});
		}
		// Enfriamiento.
		if (t == 380) {
			comando(mc, "tp @s 1450.5 215 58.5 180 8");
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				qumaraServidor(server).accion(j, 3);
				qumaraServidor(server).accion(j, 2);   // debilitada no puede girar
			});
		}
		if (t == 440) {
			captura(mc, "debug_qu8_debil.png");
			server.execute(() -> Dedsafio4ClientDebug.info("qumara debil: barriendo=" + qumaraServidor(server).barriendo()));
		}
		if (t == 442) server.execute(() -> qumaraServidor(server).accion(server.getPlayerList().getPlayer(uuid), 3));
		// Derrotada: se le saca toda la vida.
		if (t == 480) {
			comando(mc, "tp @s 1450.5 215 58.5 180 8");
			server.execute(() -> {
				var q = qumaraServidor(server);
				q.hurt(server.overworld().damageSources().magic(), 5000f);
				Dedsafio4ClientDebug.info("qumara golpe final: viva=" + q.isAlive() + " derrotada=" + q.derrotada() + " vida=" + q.getHealth());
			});
		}
		if (t == 480 + 80) captura(mc, "debug_qu9_derrotada.png");
		if (t == 480 + 120) {
			server.execute(() -> {
				var q = qumaraServidor(server);
				boolean pego = q.hurt(server.overworld().damageSources().magic(), 10f);
				Dedsafio4ClientDebug.info("qumara despues: sigue ahi=" + (q != null && !q.isRemoved()) + " le pega=" + pego);
			});
		}
		if (t == 480 + 124) {
			comando(mc, "kill @e[type=minecraft:zombie]");
			comando(mc, "effect clear @s");
			comando(mc, "forceload remove all");
		}
		if (t == 480 + 128) mc.stop();
	}

	private static final boolean ECLIPSE_ANIM = "eclipseanim".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La animación del Eclipse: la luna tapa el sol, se oscurece todo, se abre la grieta y se asoma Reviil. */
	private static void tickEclipseAnim(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "momento eclipse parar");
			comando(mc, "gamemode creative");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "time set 6000");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 4.5 0 -45");
			comando(mc, "cambio herobrine 0");
			comando(mc, "clear @s");
		}
		if (t == 20) { mirar(mc, 0, -45); mc.options.hideGui = true; }
		if (t == 24) captura(mc, "debug_ea0_sol.png");
		if (t == 26) comando(mc, "momento eclipse");
		int[] fotos = {40, 60, 80, 100, 116};
		for (int i = 0; i < fotos.length; i++) if (t == 26 + fotos[i]) captura(mc, "debug_ea" + (i + 1) + "_luna.png");
		if (t == 26 + 118) mirar(mc, 180, -80);
		if (t == 26 + 150) captura(mc, "debug_ea6_grieta.png");
		if (t == 26 + 200) captura(mc, "debug_ea7_reviil.png");
		if (t == 26 + 260) captura(mc, "debug_ea8_reviil.png");
		if (t == 26 + 262) {
			mc.options.hideGui = false;
			comando(mc, "momento eclipse parar");
			comando(mc, "gamerule doDaylightCycle true");
		}
		if (t == 26 + 268) mc.stop();
	}

	private static final boolean HAZ = "haz".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La luz sale de la Linterna: de día, de noche (apagada / prendida), en el Eclipse, y el haz visto desde afuera. */
	private static void tickHaz(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Consumer<Boolean> prender = si -> server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.getInventory().selected = 0;
			com.dedsafio4.items.LinternaItem.prender(j.getInventory().getItem(0), si);
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1085 -15 1115 15");
			comando(mc, "gamemode creative");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "time set 6000");
			comando(mc, "momento eclipse parar");
			comando(mc, "kill @e[type=dedsafio4:herobrine]");
			comando(mc, "clear @s");
		}
		if (t == 20) {
			comando(mc, "fill 1085 199 -15 1115 199 15 minecraft:grass_block");
			comando(mc, "fill 1085 200 -15 1115 206 15 minecraft:air");
			comando(mc, "fill 1094 200 -8 1106 203 -8 minecraft:stone_bricks");
			comando(mc, "fill 1097 200 -3 1097 202 -3 minecraft:oak_log");
			comando(mc, "fill 1103 200 -4 1103 201 -4 minecraft:oak_log");
			comando(mc, "setblock 1100 200 -6 minecraft:torch");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 4.5 180 12");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:linterna");
		}
		if (t == 24) mc.player.getInventory().selected = 0;
		if (t == 26) prender.accept(true);
		if (t == 28) mc.gui.getChat().clearMessages(false);
		if (t == 40) captura(mc, "debug_hz1_dia.png");
		if (t == 42) {
			comando(mc, "time set 18000");
			prender.accept(false);
		}
		if (t == 44) mc.gui.getChat().clearMessages(false);
		if (t == 60) captura(mc, "debug_hz2_noche_apagada.png");
		if (t == 62) prender.accept(true);
		if (t == 80) captura(mc, "debug_hz3_noche_prendida.png");
		if (t == 82) {
			comando(mc, "time set 6000");
			comando(mc, "momento eclipse");
		}
		if (t == 84) mc.gui.getChat().clearMessages(false);
		if (t == 88) comando(mc, "kill @e[type=dedsafio4:herobrine]");
		if (t == 102) captura(mc, "debug_hz4_eclipse.png");
		if (t == 104) mirar(mc, 200, 5);
		if (t == 112) captura(mc, "debug_hz5_eclipse_girando.png");
		if (t == 114) {
			mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
			mirar(mc, 150, 10);
		}
		if (t == 118) comando(mc, "kill @e[type=dedsafio4:herobrine]");
		if (t == 130) captura(mc, "debug_hz6_haz_afuera.png");
		if (t == 132) {
			mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			comando(mc, "momento eclipse parar");
			comando(mc, "gamerule doDaylightCycle true");
			comando(mc, "forceload remove all");
		}
		if (t == 136) mc.stop();
	}

	private static final boolean ECLIPSE = "eclipse".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Mira hacia un punto (desde el servidor y el cliente). */
	private static void mirarA(Minecraft mc, net.minecraft.server.MinecraftServer server, java.util.UUID uuid, net.minecraft.world.phys.Vec3 punto) {
		var ojos = mc.player.getEyePosition();
		var d = punto.subtract(ojos);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
		mirar(mc, yaw, pitch);
		server.execute(() -> {
			var j = server.getPlayerList().getPlayer(uuid);
			j.setYRot(yaw);
			j.setXRot(pitch);
			j.setYHeadRot(yaw);
		});
	}

	/** Eclipse: cielo negro con el sol eclipsado, solo alumbra la Linterna, y Herobrine (mirarlo a los ojos / alumbrarlo). */
	private static void tickEclipse(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1085 -15 1115 15");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "momento eclipse parar");
			comando(mc, "cambio herobrine 0");
			comando(mc, "clear @s");
		}
		if (t == 20) {
			comando(mc, "fill 1085 199 -15 1115 199 15 minecraft:grass_block");
			comando(mc, "fill 1085 200 -15 1115 206 15 minecraft:air");
			for (int i = -8; i <= 8; i += 4) comando(mc, "setblock " + (1100 + i) + " 200 -6 minecraft:torch");
			comando(mc, "setblock 1096 200 -3 minecraft:glowstone");
			comando(mc, "setblock 1104 200 -3 minecraft:glowstone");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 4.5 180 20");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:linterna");
		}
		if (t == 30) captura(mc, "debug_ec0_antes.png");
		if (t == 34) comando(mc, "momento eclipse");
		if (t == 40) mc.gui.getChat().clearMessages(false);
		if (t == 36) captura(mc, "debug_ec1_anuncio.png");
		if (t == 50) captura(mc, "debug_ec2_oscuro.png");
		if (t == 52) mirar(mc, -90, -55);
		if (t == 58) captura(mc, "debug_ec3_cielo_este.png");
		if (t == 60) mirar(mc, 90, -55);
		if (t == 66) captura(mc, "debug_ec3_cielo_oeste.png");
		// La Linterna: la única luz.
		if (t == 70) {
			mc.player.getInventory().selected = 0;
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				j.getInventory().selected = 0;
				com.dedsafio4.items.LinternaItem.prender(j.getInventory().getItem(0), true);
			});
			comando(mc, "kill @e[type=dedsafio4:herobrine]");
			mirarA(mc, server, uuid, new net.minecraft.world.phys.Vec3(1100.5, 199.5, -4.5));
		}
		if (t == 90) captura(mc, "debug_ec4_linterna.png");
		// Herobrine: mirarlo a los ojos (con la Linterna apagada) -> ataca.
		if (t == 100) {
			comando(mc, "kill @e[type=dedsafio4:herobrine]");
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				com.dedsafio4.items.LinternaItem.prender(j.getInventory().getItem(0), false);
				j.setHealth(j.getMaxHealth());
				com.dedsafio4.herobrine.ModHerobrine.aparecerEn(j, new net.minecraft.world.phys.Vec3(1100.5, 200, -6.5));
			});
		}
		if (t == 104) mirarA(mc, server, uuid, new net.minecraft.world.phys.Vec3(1100.5, 200 + 1.62, -6.5));
		if (t == 106) captura(mc, "debug_ec5_herobrine.png");
		if (t == 104 + 30) {
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info("eclipse: mirandolo a los ojos: vida " + j.getHealth() + " de " + j.getMaxHealth() + " herobrines="
						+ server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true).size());
			});
		}
		// Herobrine: alumbrarlo con la Linterna -> se pone rojo y desaparece, sin daño.
		if (t == 140) {
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				j.setHealth(j.getMaxHealth());
				com.dedsafio4.items.LinternaItem.prender(j.getInventory().getItem(0), true);
				com.dedsafio4.herobrine.ModHerobrine.aparecerEn(j, new net.minecraft.world.phys.Vec3(1100.5, 200, -6.5));
			});
		}
		if (t == 142) mirarA(mc, server, uuid, new net.minecraft.world.phys.Vec3(1100.5, 201.0, -6.5));
		if (t == 162) captura(mc, "debug_ec6_rojo.png");
		if (t == 200) {
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info("eclipse: alumbrandolo: vida " + j.getHealth() + " de " + j.getMaxHealth() + " herobrines="
						+ server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true).size());
			});
		}
		if (t == 204) comando(mc, "momento eclipse parar");
		if (t == 214) captura(mc, "debug_ec7_despues.png");
		if (t == 216) {
			comando(mc, "gamerule doDaylightCycle true");
			comando(mc, "forceload remove all");
		}
		if (t == 220) mc.stop();
	}

	private static final boolean STRAY = "stray".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** esqueleto_stray 1: de noche aparecen Stray fuera de la nieve; sin el cambio, no. */
	private static void tickStray(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Consumer<String> contar = cuando -> server.execute(() -> {
			var mundo = server.overworld();
			var jugador = server.getPlayerList().getPlayer(uuid);
			int fuera = 0, pantano = 0;
			StringBuilder biomas = new StringBuilder();
			for (var b : mundo.getEntities(net.minecraft.world.entity.EntityType.STRAY, e -> e.distanceTo(jugador) < 160)) {
				var bioma = mundo.getBiome(b.blockPosition());
				boolean esPantano = bioma.is(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS) || bioma.is(net.minecraft.world.level.biome.Biomes.ICE_SPIKES);
				if (esPantano) pantano++; else fuera++;
				if (biomas.length() < 120) biomas.append(bioma.unwrapKey().map(k -> k.location().getPath()).orElse("?")).append(' ');
			}
			int monstruos = mundo.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, jugador.getBoundingBox().inflate(128)).size();
			Dedsafio4ClientDebug.info("stray " + cuando + ": monstruos cerca=" + monstruos + " fuera del pantano=" + fuera + " en pantano=" + pantano + " biomas: " + biomas
					+ " jugador en " + jugador.blockPosition().toShortString());
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "difficulty normal");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "time set 18000");
			comando(mc, "cambio esqueleto_stray 0");
			comando(mc, "execute in minecraft:overworld run tp @s 4000.5 200 4000.5");
		}
		if (t == 20) {
			server.execute(() -> {
				var mundo = server.overworld();
				var hallado = mundo.findClosestBiome3d(h -> h.is(net.minecraft.world.level.biome.Biomes.PLAINS),
						new net.minecraft.core.BlockPos(4000, 64, 4000), 6400, 32, 64);
				var p = hallado == null ? new net.minecraft.core.BlockPos(4000, 64, 4000) : hallado.getFirst();
				int y = mundo.getChunk(p.getX() >> 4, p.getZ() >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX() & 15, p.getZ() & 15);
				server.getPlayerList().getPlayer(uuid).teleportTo(p.getX() + 0.5, y + 1, p.getZ() + 0.5);
			});
		}
		if (t == 40) comando(mc, "kill @e[type=!minecraft:player]");
		if (t == 41 && !strayEscuchando) {
			strayEscuchando = true;
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((e, mundo) -> {
				if (e.getType() != net.minecraft.world.entity.EntityType.STRAY) return;
				var bioma = mundo.getBiome(e.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("?");
				Dedsafio4ClientDebug.info("stray nacio en " + e.blockPosition().toShortString() + " bioma=" + bioma
						+ " cambio=" + com.dedsafio4.cambios.Cambios.nivel(mundo.getServer(), com.dedsafio4.cambios.Cambios.ESQUELETO_STRAY));
			});
		}
		if (t == 540) contar.accept("de noche sin el cambio");
		if (t == 542) {
			comando(mc, "cambio esqueleto_stray 1");
			comando(mc, "kill @e[type=!minecraft:player]");
		}
		if (t == 1042) contar.accept("de noche con el cambio");
		if (t == 1040) {
			server.execute(() -> {
				var mundo = server.overworld();
				var jugador = server.getPlayerList().getPlayer(uuid);
				var bp = jugador.blockPosition();
				var bioma = mundo.getBiome(bp);
				var mobs = bioma.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap();
				StringBuilder sb = new StringBuilder();
				for (var m : mobs) sb.append(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.type).getPath()).append(':').append(m.getWeight().asInt()).append(' ');
				int ok = 0, okBogged = 0;
				for (int dx = -40; dx <= 40; dx += 8) for (int dz = -40; dz <= 40; dz += 8) {
					int y = mundo.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bp.getX() + dx, bp.getZ() + dz);
					var p = new net.minecraft.core.BlockPos(bp.getX() + dx, y, bp.getZ() + dz);
					if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(net.minecraft.world.entity.EntityType.STRAY, mundo, net.minecraft.world.entity.MobSpawnType.NATURAL, p, mundo.getRandom())) ok++;
					if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(net.minecraft.world.entity.EntityType.BOGGED, mundo, net.minecraft.world.entity.MobSpawnType.NATURAL, p, mundo.getRandom())) okBogged++;
				}
				Dedsafio4ClientDebug.info("stray diag: bioma=" + bioma.unwrapKey().get().location().getPath() + " noche=" + mundo.isNight()
						+ " reglas ok stray=" + ok + "/121 bogged=" + okBogged + "/121 lista: " + sb);
			});
		}
		if (t == 1046) {
			comando(mc, "cambio esqueleto_stray 0");
			comando(mc, "gamerule doDaylightCycle true");
			comando(mc, "kill @e[type=minecraft:bogged]");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5");
		}
		if (t == 1060) mc.stop();
	}

	private static boolean strayEscuchando;

	private static final boolean ZOMBI_NETHER = "zombinether".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** zombi 3: los zombis aparecen en el Nether (sin importar la luz); con zombi 2, no. */
	private static void tickZombiNether(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		java.util.function.IntConsumer contar = nivel -> server.execute(() -> {
			var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
			var zombis = nether.getEntities(net.minecraft.world.entity.EntityType.ZOMBIE, e -> true);
			StringBuilder luces = new StringBuilder();
			for (int i = 0; i < Math.min(8, zombis.size()); i++) {
				var pos = zombis.get(i).blockPosition();
				luces.append(nether.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos)).append(' ');
			}
			Dedsafio4ClientDebug.info("zombi nether: con zombi " + nivel + " aparecieron " + zombis.size() + " zombis; luz de bloque donde estan: " + luces
					+ (zombis.isEmpty() ? "" : " espada=" + zombis.get(0).getMainHandItem()));
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode creative");
			comando(mc, "difficulty normal");
			comando(mc, "execute in minecraft:the_nether run tp @s 0.5 121 0.5");
		}
		if (t == 20) {
			comando(mc, "execute in minecraft:the_nether run fill -3 120 -3 3 120 3 minecraft:obsidian");
			comando(mc, "execute in minecraft:the_nether run fill -3 121 -3 3 124 3 minecraft:air");
			comando(mc, "execute in minecraft:the_nether run tp @s 0.5 121 0.5");
			comando(mc, "cambio zombi 2");
		}
		if (t == 30) comando(mc, "execute in minecraft:the_nether run kill @e[type=minecraft:zombie]");
		if (t == 430) contar.accept(2);
		if (t == 432) {
			comando(mc, "cambio zombi 3");
			comando(mc, "execute in minecraft:the_nether run kill @e[type=minecraft:zombie]");
		}
		if (t == 840) contar.accept(3);
		if (t == 844) {
			comando(mc, "cambio zombi 0");
			comando(mc, "execute in minecraft:the_nether run kill @e[type=minecraft:zombie]");
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5");
		}
		if (t == 860) mc.stop();
	}

	private static final boolean HEROBRINE_NOCHE = "herobrinenoche".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** herobrine 1: de día no aparece; de noche sí. */
	private static void tickHerobrineNoche(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		Runnable contar = () -> server.execute(() -> Dedsafio4ClientDebug.info("herobrine noche t=" + (ticks - 20) + " es de noche="
				+ server.overworld().isNight() + " herobrines=" + server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true).size()));
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1085 -15 1115 15");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			comando(mc, "gamerule doDaylightCycle false");
			comando(mc, "kill @e[type=dedsafio4:herobrine]");
		}
		if (t == 20) {
			comando(mc, "fill 1085 199 -15 1115 199 15 minecraft:grass_block");
			comando(mc, "fill 1085 200 -15 1115 206 15 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 0.5 180 3");
		}
		if (t == 30) comando(mc, "cambio herobrine 1");
		if (t == 250 || t == 450) contar.run();
		if (t == 452) comando(mc, "time set 18000");
		if (t == 500 || t == 600 || t == 700) contar.run();
		if (t == 704) {
			comando(mc, "cambio herobrine 0");
			comando(mc, "gamerule doDaylightCycle true");
			comando(mc, "kill @e[type=dedsafio4:herobrine]");
			comando(mc, "forceload remove all");
		}
		if (t == 708) mc.stop();
	}

	private static final boolean TREX = "trex".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));
	private static java.util.UUID trexPrueba;

	private static com.dedsafio4.trex.TRexEntity trexServidor(net.minecraft.server.MinecraftServer server) {
		return trexPrueba == null ? null : (com.dedsafio4.trex.TRexEntity) server.overworld().getEntity(trexPrueba);
	}

	/** T-Rex: quieto, rugiendo, montado (con los botones abajo), y caminando hasta pisar al jugador. */
	private static void tickTRex(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1200 -40 1300 80");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			comando(mc, "kill @e[type=dedsafio4:trex]");
		}
		if (t == 20) {
			comando(mc, "fill 1200 199 -40 1300 199 80 minecraft:grass_block");
			for (int z = -40; z <= 80; z += 41) comando(mc, "fill 1200 200 " + z + " 1300 206 " + Math.min(80, z + 40) + " minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1250.5 212 75.5 180 3");
		}
		if (t == 30) {
			server.execute(() -> {
				var trex = new com.dedsafio4.trex.TRexEntity(com.dedsafio4.trex.ModTRex.TREX, server.overworld());
				trex.moveTo(1250.5, 200, 0.5, 0, 0);
				server.overworld().addFreshEntity(trex);
				trexPrueba = trex.getUUID();
			});
		}
		if (t == 36) mc.gui.getChat().clearMessages(false);
		if (t == 50) captura(mc, "debug_tr1_quieto.png");
		if (t == 52) server.execute(() -> trexServidor(server).rugir());
		if (t == 80) captura(mc, "debug_tr2_rugido.png");
		// Montarlo.
		if (t == 125) server.execute(() -> server.getPlayerList().getPlayer(uuid).startRiding(trexServidor(server)));
		if (t == 128) mirar(mc, 0, 15);
		if (t == 134) mc.gui.getChat().clearMessages(false);
		if (t == 140) {
			Dedsafio4ClientDebug.info("trex montado: vehiculo=" + mc.player.getVehicle() + " y=" + String.format("%.1f", mc.player.getY()));
			captura(mc, "debug_tr3_montado.png");
			server.execute(() -> trexServidor(server).rugir());
		}
		if (t == 172) {
			Dedsafio4ClientDebug.info("trex montado rugiendo: y=" + String.format("%.1f", mc.player.getY()));
			captura(mc, "debug_tr5_montado_rugiendo.png");
		}
		if (t == 215) server.execute(() -> server.getPlayerList().getPlayer(uuid).stopRiding());
		if (t == 225) Dedsafio4ClientDebug.info("trex bajado: y=" + String.format("%.1f", mc.player.getY()) + " modo=" + mc.gameMode.getPlayerMode());
		// Pisotón: el jugador (en supervivencia) queda delante del pie izquierdo; el T-Rex camina hacia él.
		if (t == 230) {
			comando(mc, "gamemode survival");
			comando(mc, "tp @s 1242.5 200 28.5 180 -25");
		}
		if (t >= 240 && t < 470) {
			server.execute(() -> {
				var trex = trexServidor(server);
				if (trex == null) return;
				trex.setYRot(0);
				trex.yBodyRot = 0;
				trex.setDeltaMovement(0, trex.getDeltaMovement().y, 0.18);
			});
		}
		if (t == 300) captura(mc, "debug_tr4_viene.png");
		if (t >= 240 && t < 470 && t % 20 == 0) {
			server.execute(() -> {
				var trex = trexServidor(server);
				var jugador = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info(String.format("trex t=%d z=%.1f mezcla=%.2f jugador=%s", t, trex.getZ(), trex.mezcla, jugador.gameMode.getGameModeForPlayer()));
			});
		}
		if (t == 480) {
			comando(mc, "gamemode creative");
			comando(mc, "kill @e[type=dedsafio4:trex]");
			comando(mc, "forceload remove all");
		}
		if (t == 484) mc.stop();
	}

	private static final boolean MINIMAPA = "minimapa".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** /minimapa todos | admins: el cliente se entera, y los efectos de Xaero se ponen o se sacan. */
	private static void tickMinimapa(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		java.util.function.Consumer<String> log = cuando -> server.execute(() -> {
			var jugador = server.getPlayerList().getPlayer(uuid);
			var efecto = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_minimap"));
			Dedsafio4ClientDebug.info("minimapa " + cuando + ": cliente adminsLoUsan=" + XaeroBloqueo.adminsLoUsan
					+ " admin=" + jugador.hasPermissions(2) + " bloqueado=" + XaeroBloqueo.bloqueado() + " efecto no_minimap=" + (efecto.isPresent() && jugador.hasEffect(efecto.get())));
		});
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "deditas set @s 10270");
			var nombre = mc.player.getGameProfile().getName();
			server.execute(() -> server.getPlayerList().op(server.getPlayerList().getPlayer(uuid).getGameProfile()));
		}
		if (t == 10) log.accept("al entrar");
		if (t == 12) comando(mc, "minimapa admins");
		if (t == 60) log.accept("admins");
		if (t == 62) comando(mc, "minimapa todos");
		if (t == 110) log.accept("todos");
		if (t == 112) comando(mc, "minimapa");
		if (t == 118) mc.gui.getChat().clearMessages(false);
		if (t == 122) captura(mc, "debug_contador.png");
		if (t == 124) server.execute(() -> server.getPlayerList().deop(server.getPlayerList().getPlayer(uuid).getGameProfile()));
		if (t == 128) mc.stop();
	}

	private static final boolean HUEVO_HEROBRINE = "huevoherobrine".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** El huevo de Herobrine: aparece, va por el jugador más cercano y a los 5 segundos sin luz le saca 10 corazones. */
	private static void tickHuevoHerobrine(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 60) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1085 -15 1115 15");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 18000");
			comando(mc, "clear @s");
			comando(mc, "cambio herobrine 0");
		}
		if (t == 20) {
			comando(mc, "fill 1085 199 -15 1115 199 15 minecraft:grass_block");
			comando(mc, "fill 1085 200 -15 1115 206 15 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 0.5 180 20");
		}
		if (t == 30) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				jugador.setHealth(jugador.getMaxHealth());
				jugador.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.HEROBRINE_SPAWN_EGG));
				jugador.getInventory().selected = 0;
				var piso = new net.minecraft.core.BlockPos(1100, 199, -4);
				var golpe = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(piso).add(0, 0.5, 0),
						net.minecraft.core.Direction.UP, piso, false);
				var r = jugador.gameMode.useItemOn(jugador, server.overworld(), jugador.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, golpe);
				Dedsafio4ClientDebug.info("huevo herobrine: uso=" + r + " vida=" + jugador.getHealth());
			});
		}
		if (t == 40) {
			server.execute(() -> {
				var hs = server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true);
				Dedsafio4ClientDebug.info("huevo herobrine: herobrines=" + hs.size() + (hs.isEmpty() ? "" : " va por=" + hs.get(0).objetivo() + " (yo=" + uuid + ")"));
			});
		}
		if (t == 44) mc.gui.getChat().clearMessages(false);
		if (t == 50) captura(mc, "debug_he3_huevo.png");
		if (t == 60) mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true));
		if (t == 150) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info("huevo herobrine: despues de 5 s vida=" + jugador.getHealth() + " herobrines="
						+ server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true).size());
			});
		}
		if (t == 154) {
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static final boolean HEROBRINE = "herobrine".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Herobrine: sin alumbrarlo, a los 5 segundos saca 10 corazones; alumbrándolo con la Linterna se pone rojo y desaparece. */
	private static void tickHerobrine(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 1085 -15 1115 15");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 18000");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "cambio herobrine 0");
		}
		if (t == 20) {
			comando(mc, "fill 1085 199 -15 1115 199 15 minecraft:grass_block");
			comando(mc, "fill 1085 200 -15 1115 206 15 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1100.5 200 0.5 180 3");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:linterna");
		}
		if (t == 24) mc.player.getInventory().selected = 1;
		// 1) Sin alumbrarlo.
		if (t == 30) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				jugador.getInventory().selected = 1;
				jugador.setHealth(jugador.getMaxHealth());
				com.dedsafio4.herobrine.ModHerobrine.aparecerEn(jugador, new net.minecraft.world.phys.Vec3(1100.5, 200, -7.5));
				Dedsafio4ClientDebug.info("herobrine sin luz: vida antes=" + jugador.getHealth());
			});
		}
		if (t == 34) mc.gui.getChat().clearMessages(false);
		if (t == 45) captura(mc, "debug_he1_aparece.png");
		if (t > 30 && t < 140 && t % 20 == 0) {
			server.execute(() -> {
				for (var h : server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true)) {
					Dedsafio4ClientDebug.info("herobrine sin luz: edad=" + h.tickCount + " objetivo=" + h.objetivo() + " vivo=" + h.isAlive()
							+ " tickCount=" + h.tickCount + " noAI=" + h.isNoAi());
				}
			});
		}
		if (t == 140) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info("herobrine sin luz: vida despues=" + jugador.getHealth() + " herobrines="
						+ server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true).size());
			});
		}
		// 2) Alumbrándolo con la Linterna prendida.
		if (t == 150) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				jugador.setHealth(jugador.getMaxHealth());
				jugador.getInventory().selected = 0;
				com.dedsafio4.items.LinternaItem.prender(jugador.getInventory().getItem(0), true);
				com.dedsafio4.herobrine.ModHerobrine.aparecerEn(jugador, new net.minecraft.world.phys.Vec3(1100.5, 200, -7.5));
			});
			mc.player.getInventory().selected = 0;
		}
		if (t >= 150 && t <= 200 && t % 5 == 0) {
			for (var e : mc.level.getEntitiesOfClass(com.dedsafio4.herobrine.HerobrineEntity.class, mc.player.getBoundingBox().inflate(20))) {
				Dedsafio4ClientDebug.info("herobrine con luz t=" + t + " rojo=" + String.format("%.2f", e.rojo()));
			}
		}
		if (t == 172) captura(mc, "debug_he2_rojo.png");
		if (t == 260) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				Dedsafio4ClientDebug.info("herobrine con luz: vida=" + jugador.getHealth() + " de " + jugador.getMaxHealth() + " herobrines="
						+ server.overworld().getEntities(com.dedsafio4.herobrine.ModHerobrine.HEROBRINE, e -> true).size());
			});
		}
		if (t == 264) {
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static final boolean COBWEB = "cobweb".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** cobweb 1: al romper telarañas sale una araña venenosa 1 de cada 3 veces (y "/cambio Cobweb 1" con mayúscula anda). */
	private static void tickCobweb(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 985 -15 1015 15");
			comando(mc, "gamemode creative");
			comando(mc, "difficulty normal");
			comando(mc, "cambio cobweb 0");
		}
		if (t == 20) {
			comando(mc, "fill 985 199 -15 1015 199 15 minecraft:obsidian");
			comando(mc, "fill 985 200 -15 1015 206 15 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 1000.5 200 0.5");
		}
		for (int[] fase : new int[][]{{30, 0}, {50, 1}}) {
			if (t == fase[0] - 4) comando(mc, fase[1] == 1 ? "cambio Cobweb 1" : "cambio cobweb 0");
			if (t != fase[0]) continue;
			int conCambio = fase[1];
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				var mundo = server.overworld();
				mundo.getEntities(net.minecraft.world.entity.EntityType.CAVE_SPIDER, e -> true).forEach(e -> e.discard());
				int rotas = 0;
				for (int i = 0; i < 60; i++) {
					var pos = new net.minecraft.core.BlockPos(988 + i % 25, 201, -12 + (i / 25) * 3);
					mundo.setBlock(pos, net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), 3);
					if (jugador.gameMode.destroyBlock(pos)) rotas++;
				}
				int aranias = mundo.getEntities(net.minecraft.world.entity.EntityType.CAVE_SPIDER, e -> e.isAlive()).size();
				Dedsafio4ClientDebug.info("cobweb cambio=" + conCambio + ": telaranias rotas=" + rotas + " aranias venenosas=" + aranias
						+ " nivel=" + com.dedsafio4.cambios.Cambios.nivel(server, com.dedsafio4.cambios.Cambios.COBWEB));
			});
		}
		if (t == 60) {
			comando(mc, "cambio cobweb 0");
			comando(mc, "kill @e[type=minecraft:cave_spider]");
			comando(mc, "forceload remove all");
		}
		if (t == 64) mc.stop();
	}

	private static final boolean ZOMBI = "zombi".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** zombi 1: la mitad escala (uno que sí y uno que no, junto a una torre); zombi 2: todos con Espada de Hierro Filo V. */
	private static void tickZombi(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 885 -15 915 15");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 18000");
			comando(mc, "clear @s");
			comando(mc, "effect give @s minecraft:resistance 60 255 true");
			comando(mc, "cambio zombi 0");
			comando(mc, "kill @e[type=minecraft:zombie]");
		}
		if (t == 20) {
			comando(mc, "fill 885 199 -15 915 199 15 minecraft:obsidian");
			comando(mc, "fill 885 200 -15 915 210 15 minecraft:air");
			comando(mc, "fill 897 200 0 903 204 0 minecraft:stone");
			comando(mc, "execute in minecraft:overworld run tp @s 900.5 205 0.5 0 60");
		}
		if (t == 24) comando(mc, "cambio zombi 1");
		if (t == 30) {
			for (int i = 0; i < 20; i++) comando(mc, "summon minecraft:zombie " + (887 + i % 5) + " 200 " + (10 + i / 5) + " {NoAI:1b,PersistenceRequired:1b}");
			comando(mc, "summon minecraft:zombie 900.5 200 3.5 {PersistenceRequired:1b,Tags:[\"dedsafio4_zombi_decidido\",\"dedsafio4_zombi_escala\"],CustomName:'\"trepa\"'}");
			comando(mc, "summon minecraft:zombie 900.5 200 -3.5 {PersistenceRequired:1b,Tags:[\"dedsafio4_zombi_decidido\"],CustomName:'\"no trepa\"'}");
		}
		if (t == 34) mc.gui.getChat().clearMessages(false);
		if (t == 100) {
			server.execute(() -> {
				int total = 0, escalan = 0;
				for (var z : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
						new net.minecraft.world.phys.AABB(880, 190, -20, 920, 220, 20))) {
					if (z.hasCustomName()) continue;
					total++;
					if (z.getTags().contains("dedsafio4_zombi_escala")) escalan++;
				}
				Dedsafio4ClientDebug.info("zombi 1: escalan " + escalan + " de " + total);
			});
		}
		if (t >= 60 && t <= 140 && t % 10 == 0) {
			server.execute(() -> {
				StringBuilder sb = new StringBuilder("zombi t=" + (ticks - 20) + ":");
				for (var z : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
						new net.minecraft.world.phys.AABB(890, 190, -10, 910, 220, 10))) {
					if (z.hasCustomName()) sb.append(String.format(" [%s %.1f,%.1f,%.1f choca=%s nav=%s obj=%s tags=%s]", z.getCustomName().getString(), z.getX(), z.getY(), z.getZ(),
							z.horizontalCollision, z.getNavigation().getClass().getSimpleName(), z.getTarget() == null ? "-" : z.getTarget().getName().getString(), z.getTags()));
				}
				Dedsafio4ClientDebug.info(sb.toString());
			});
		}
		if (t == 72) comando(mc, "tp @s 900.5 205 0.5 0 50");
		if (t == 86) captura(mc, "debug_zo1_trepando.png");
		if (t == 150) comando(mc, "cambio zombi 2");
		if (t == 152) comando(mc, "summon minecraft:zombie 905.5 200 0.5 {PersistenceRequired:1b,NoAI:1b,CustomName:'\"nuevo\"'}");
		if (t == 160) {
			server.execute(() -> {
				int total = 0, conEspada = 0;
				String ejemplo = "";
				for (var z : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
						new net.minecraft.world.phys.AABB(880, 190, -20, 920, 220, 20))) {
					total++;
					var mano = z.getMainHandItem();
					var filo = mano.getEnchantments().getLevel(server.overworld().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
							.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS));
					if (mano.is(net.minecraft.world.item.Items.IRON_SWORD) && filo == 5) conEspada++;
					if (z.hasCustomName() && z.getCustomName().getString().equals("nuevo")) ejemplo = mano + " filo=" + filo;
				}
				Dedsafio4ClientDebug.info("zombi 2: con espada de hierro filo 5: " + conEspada + " de " + total + "; el nuevo: " + ejemplo);
			});
			comando(mc, "tp @s 903.5 200.5 0.5 90 10");
		}
		if (t == 170) captura(mc, "debug_zo2_espada.png");
		if (t == 176) {
			comando(mc, "cambio zombi 0");
			comando(mc, "kill @e[type=minecraft:zombie]");
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static final boolean MIEL = "miel".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void logMiel(Minecraft mc, String cuando) {
		var inv = mc.player.getInventory();
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		server.execute(() -> {
			var jugador = server.getPlayerList().getPlayer(uuid);
			var panal = server.overworld().getBlockState(new net.minecraft.core.BlockPos(800, 200, 0));
			int aire = 0;
			for (var pos : net.minecraft.core.BlockPos.betweenClosed(797, 197, -3, 803, 203, 3)) if (server.overworld().getBlockState(pos).isAir()) aire++;
			Dedsafio4ClientDebug.info("miel " + cuando + ": botellas=" + jugador.getInventory().countItem(net.minecraft.world.item.Items.GLASS_BOTTLE)
					+ " miel=" + jugador.getInventory().countItem(net.minecraft.world.item.Items.HONEY_BOTTLE)
					+ " panal=" + panal.getBlock() + (panal.hasProperty(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL)
							? " nivel=" + panal.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) : "")
					+ " vida=" + jugador.getHealth() + " aire alrededor=" + aire);
		});
	}

	/** miel 1: la botella no saca miel del panal, y romperlo explota como un Creeper. */
	private static void tickMiel(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		var panal = new net.minecraft.core.BlockPos(800, 200, 0);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 790 -10 810 10");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			comando(mc, "cambio miel 0");
		}
		if (t == 20) {
			comando(mc, "fill 790 197 -10 810 199 10 minecraft:obsidian");
			comando(mc, "fill 790 200 -10 810 206 10 minecraft:air");
			comando(mc, "execute in minecraft:overworld run tp @s 800.5 200 2.5 180 20");
		}
		// Botella en el panal lleno: sin el cambio (tiene que dar miel) y con el cambio (no).
		for (int[] paso : new int[][]{{30, 0}, {50, 1}}) {
			if (t == paso[0] - 4) comando(mc, "cambio miel " + paso[1]);
			if (t != paso[0]) continue;
			int conCambio = paso[1];
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				var mundo = server.overworld();
				mundo.setBlock(panal, net.minecraft.world.level.block.Blocks.BEEHIVE.defaultBlockState()
						.setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 5), 3);
				jugador.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 3));
				jugador.getInventory().selected = 0;
				var golpe = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(panal),
						net.minecraft.core.Direction.SOUTH, panal, false);
				var r = jugador.gameMode.useItemOn(jugador, mundo, jugador.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, golpe);
				Dedsafio4ClientDebug.info("miel botella cambio=" + conCambio + ": resultado=" + r
						+ " miel=" + jugador.getInventory().countItem(net.minecraft.world.item.Items.HONEY_BOTTLE)
						+ " nivel panal=" + mundo.getBlockState(panal).getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL));
				jugador.getInventory().clearContent();
			});
		}
		// Romper el panal con el cambio: explota.
		if (t == 60) {
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				var mundo = server.overworld();
				float antes = jugador.getHealth();
				int aireAntes = 0, aireDespues = 0;
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(796, 197, -4, 804, 204, 4)) if (mundo.getBlockState(pos).isAir()) aireAntes++;
				boolean roto = jugador.gameMode.destroyBlock(panal);
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(796, 197, -4, 804, 204, 4)) if (mundo.getBlockState(pos).isAir()) aireDespues++;
				Dedsafio4ClientDebug.info("miel panal roto=" + roto + ": vida " + antes + " -> " + jugador.getHealth()
						+ " (obsidiana no se rompe; aire " + aireAntes + " -> " + aireDespues + ")");
			});
		}
		if (t == 64) captura(mc, "debug_miel_explosion.png");
		if (t == 72) comando(mc, "cambio miel 0");
		if (t == 76) {
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static final boolean CRATER = "crater".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Cráter del meteorito en un lugar limpio: solo tiene que romper (nada de magma, piedra negra, tierra gruesa ni fuego). */
	private static void tickCrater(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 670 -30 730 30");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
		}
		if (t == 20) {
			comando(mc, "fill 675 199 -25 725 199 25 minecraft:grass_block");
			comando(mc, "fill 675 192 -25 725 198 25 minecraft:dirt");
			comando(mc, "fill 675 200 -25 725 208 25 minecraft:air");
			comando(mc, "fill 675 209 -25 725 216 25 minecraft:air");
			comando(mc, "setblock 705 200 3 minecraft:oak_log");
			comando(mc, "setblock 705 201 3 minecraft:oak_log");
			comando(mc, "tp @s 700.5 230 32.5 180 40");
		}
		if (t == 30) server.execute(() -> meteoritoPrueba = com.dedsafio4.meteoritos.ModMeteoritos.lanzar(server.overworld(), new net.minecraft.world.phys.Vec3(700.5, 200, 0.5), 1.0f));
		if (t == 34) mc.gui.getChat().clearMessages(false);
		if (t == 120) {
			server.execute(() -> {
				var mundo = server.overworld();
				int magma = 0, piedraNegra = 0, tierraGruesa = 0, fuego = 0, rotos = 0;
				var c = net.minecraft.core.BlockPos.containing(meteoritoPrueba.position());
				Dedsafio4ClientDebug.info("crater limpio: choco en " + c.toShortString() + " choco=" + meteoritoPrueba.choco());
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(c.offset(-24, -8, -24), c.offset(24, 8, 24))) {
					var b = mundo.getBlockState(pos);
					if (b.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) magma++;
					else if (b.is(net.minecraft.world.level.block.Blocks.BLACKSTONE)) piedraNegra++;
					else if (b.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)) tierraGruesa++;
					else if (b.is(net.minecraft.world.level.block.Blocks.FIRE)) fuego++;
					else if (b.isAir()) rotos++;
				}
				Dedsafio4ClientDebug.info("crater limpio: magma=" + magma + " piedra negra=" + piedraNegra + " tierra gruesa=" + tierraGruesa
						+ " fuego=" + fuego + " aire alrededor=" + rotos + " tronco=" + mundo.getBlockState(new net.minecraft.core.BlockPos(705, 201, 3)).getBlock());
			});
			captura(mc, "debug_me7_limpio.png");
		}
		if (t == 126) {
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static com.dedsafio4.meteoritos.MeteoritoEntity meteoritoPrueba;

	private static final boolean DACTYLO = "dactylo".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));
	private static boolean tintaCapturada;

	/** Dáctylo Bebé: el modelo de cerca, y uno que vuela cerca, escupe tinta y tapa la pantalla. */
	private static void tickDactylo(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "forceload add 480 -20 520 20");
			comando(mc, "kill @e[type=dedsafio4:dactylo_bebe]");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
		}
		if (t == 20) {
			comando(mc, "fill 485 199 -15 515 199 15 minecraft:grass_block");
			comando(mc, "fill 485 200 -15 515 215 15 minecraft:air");
		}
		if (t == 25) comando(mc, "execute in minecraft:overworld run tp @s 500.5 200 1.5 180 -12");
		if (t == 30) comando(mc, "summon dedsafio4:dactylo_bebe 500.5 201.2 -1.0 {NoAI:1b,Rotation:[30f,0f]}");
		if (t == 34) mc.gui.getChat().clearMessages(false);
		if (t == 45) captura(mc, "debug_da1_modelo.png");
		if (t == 48) {
			comando(mc, "kill @e[type=dedsafio4:dactylo_bebe]");
			comando(mc, "summon dedsafio4:dactylo_bebe 500.5 206 -12");
			comando(mc, "tp @s 500.5 200 1.5 180 -20");
		}
		if (t == 52) mc.gui.getChat().clearMessages(false);
		if (t > 50 && t % 20 == 0) {
			var d = mc.level.getEntitiesOfClass(com.dedsafio4.dactylos.DactyloBebeEntity.class, mc.player.getBoundingBox().inflate(40));
			var tintas = mc.level.getEntitiesOfClass(com.dedsafio4.dactylos.TintaDactyloEntity.class, mc.player.getBoundingBox().inflate(40)).size();
			Dedsafio4ClientDebug.info("dactylo t=" + t + (d.isEmpty() ? " ninguno" : String.format(" pos=%.1f,%.1f,%.1f dist=%.1f",
					d.get(0).getX(), d.get(0).getY(), d.get(0).getZ(), d.get(0).distanceTo(mc.player)))
					+ " tintas=" + tintas + " tinta en jugador=" + mc.player.hasEffect(com.dedsafio4.dactylos.ModDactylos.EFECTO_TINTA));
		}
		if (t == 90) captura(mc, "debug_da2_volando.png");
		if (t > 50 && !tintaCapturada && mc.player.hasEffect(com.dedsafio4.dactylos.ModDactylos.EFECTO_TINTA)) {
			var e = mc.player.getEffect(com.dedsafio4.dactylos.ModDactylos.EFECTO_TINTA);
			if (e.getDuration() < com.dedsafio4.dactylos.TintaDactyloEntity.DURACION - 12) {
				tintaCapturada = true;
				captura(mc, "debug_da3_tinta.png");
			}
		}
		if (t == 330) {
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static final boolean METEORITO = "meteorito".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Meteorito: se tira con un palo, cae, abre el cráter; y el daño con diamante + Protección IV (1 corazón). */
	private static void tickMeteorito(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 300.5 200 12.5 180 9");
			comando(mc, "fill 270 199 -25 330 199 30 minecraft:grass_block");
			comando(mc, "fill 270 198 -25 330 190 30 minecraft:dirt");
			comando(mc, "fill 270 200 -25 330 208 30 minecraft:air");
			comando(mc, "kill @e[type=!minecraft:player]");
			comando(mc, "gamemode creative");
			comando(mc, "time set 13000");
			comando(mc, "effect clear @s");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:stick");
			comando(mc, "forceload add 380 -20 420 20");
		}
		if (t == 20) {
			comando(mc, "fill 385 199 -15 415 199 15 minecraft:grass_block");
			comando(mc, "fill 385 200 -15 415 230 15 minecraft:air");
			comando(mc, "fill 398 190 -1 403 199 1 minecraft:bedrock");
		}
		if (t == 8) mc.gui.getChat().clearMessages(false);
		if (t == 10) {
			mc.gui.getChat().clearMessages(false);
			mirar(mc, 180, 9);
			mc.player.getInventory().selected = 0;
		}
		if (t == 12 || t == 13) mirar(mc, 180, 9);
		if (t == 14) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 15) {
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				var ojos = j.getEyePosition();
				var golpe = j.level().clip(new net.minecraft.world.level.ClipContext(ojos, ojos.add(j.getLookAngle().scale(160)),
						net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, j));
				Dedsafio4ClientDebug.info("meteorito palo: pitch servidor=" + j.getXRot() + " cliente=" + mc.player.getXRot()
						+ " permiso2=" + j.hasPermissions(2) + " mano=" + j.getMainHandItem() + " golpe=" + golpe.getType() + " " + golpe.getBlockPos()
						+ " enfriando=" + j.getCooldowns().isOnCooldown(net.minecraft.world.item.Items.STICK));
			});
		}
		if (t == 18) Dedsafio4ClientDebug.info("meteorito con el palo: " + mc.level.getEntitiesOfClass(
				com.dedsafio4.meteoritos.MeteoritoEntity.class, mc.player.getBoundingBox().inflate(80)).size());
		if (t == 50) captura(mc, "debug_me1_cayendo.png");
		if (t == 66) captura(mc, "debug_me2_cerca.png");
		if (t == 76) captura(mc, "debug_me3_choque.png");
		if (t == 100) captura(mc, "debug_me4_humo.png");
		if (t == 158) mc.gui.getChat().clearMessages(false);
		if (t == 160) comando(mc, "tp @s 300.5 226 30.5 180 50");
		if (t == 172) captura(mc, "debug_me5_crater.png");
		if (t == 173) {
			comando(mc, "tp @s 300.5 196 2.5 180 20");
			server.execute(() -> {
				var mundo = server.overworld();
				int magma = 0, piedraNegra = 0, tierraGruesa = 0, fuego = 0, aire = 0;
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(275, 185, -23, 325, 208, 27)) {
					var b = mundo.getBlockState(pos);
					if (b.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) magma++;
					else if (b.is(net.minecraft.world.level.block.Blocks.BLACKSTONE)) piedraNegra++;
					else if (b.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)) tierraGruesa++;
					else if (b.is(net.minecraft.world.level.block.Blocks.FIRE)) fuego++;
					else if (b.isAir() && pos.getY() <= 199) aire++;
				}
				Dedsafio4ClientDebug.info("meteorito crater: magma=" + magma + " piedra negra=" + piedraNegra + " tierra gruesa=" + tierraGruesa
						+ " fuego=" + fuego + " aire bajo el suelo=" + aire);
			});
		}
		if (t == 180) captura(mc, "debug_me6_adentro.png");
		// Daño: jugador con diamante completo y Protección IV, y un gólem sin armadura al lado.
		if (t == 184) {
			comando(mc, "tp @s 400.5 200 0.5 180 0");
			comando(mc, "gamemode survival");
			for (String[] a : new String[][]{{"head", "helmet"}, {"chest", "chestplate"}, {"legs", "leggings"}, {"feet", "boots"}}) {
				comando(mc, "item replace entity @s armor." + a[0] + " with minecraft:diamond_" + a[1]
						+ "[minecraft:enchantments={levels:{'minecraft:protection':4}}]");
			}
			comando(mc, "summon minecraft:iron_golem 401.8 200 0.5 {NoAI:1b}");
		}
		if (t == 196) {
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				jugador.setHealth(jugador.getMaxHealth());
				var golem = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, jugador.getBoundingBox().inflate(5)).get(0);
				Dedsafio4ClientDebug.info("meteorito antes: jugador=" + jugador.getHealth() + " armadura=" + jugador.getArmorValue()
						+ " golem=" + golem.getHealth() + " distancia golem=" + String.format("%.2f", golem.distanceTo(jugador)));
				com.dedsafio4.meteoritos.ModMeteoritos.lanzar(server.overworld(), jugador.position(), (float) Math.PI);
			});
		}
		if (t == 258) {
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				var golems = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, jugador.getBoundingBox().inflate(8));
				Dedsafio4ClientDebug.info("meteorito despues: jugador=" + jugador.getHealth() + " (perdio " + (jugador.getMaxHealth() - jugador.getHealth())
						+ ") golem=" + (golems.isEmpty() ? "-" : golems.get(0).getHealth()));
			});
		}
		if (t == 264) {
			comando(mc, "forceload remove all");
			mc.stop();
		}
	}

	private static final boolean SONIDO_CANDADO = "sonidocandado".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** El cofre con candado suena como un cofre normal al abrir y cerrar. */
	private static void tickSonidoCandado(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var cofre = new net.minecraft.core.BlockPos(0, 200, 2);
		if (t < 20 && mc.screen != null) mc.setScreen(null);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5 0 20");
			comando(mc, "gamemode survival");
			comando(mc, "fill -3 199 -3 3 199 4 minecraft:stone");
			comando(mc, "fill -3 200 -3 3 203 4 minecraft:air");
			comando(mc, "setblock 0 200 2 minecraft:chest[facing=north]");
			mc.getSoundManager().addListener((sonido, rango, distancia) -> {
				String id = sonido.getLocation().toString();
				if (id.contains("chest") || id.contains("cofre")) Dedsafio4ClientDebug.info("candado sonido: " + id);
			});
		}
		if (t == 6) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var mundo = server.overworld();
				com.dedsafio4.candados.Candados.data(mundo).poner(cofre, "1234", uuid);
				com.dedsafio4.candados.Candados.avisarCambio(mundo);
			});
		}
		if (t == 14) Dedsafio4ClientDebug.info("candado puesto: " + com.dedsafio4.candados.Candados.tieneCandado(mc.level, cofre));
		if (t == 20) clickEn(mc, cofre, net.minecraft.core.Direction.NORTH);
		if (t == 35) {
			Dedsafio4ClientDebug.info("candado pantalla: " + mc.screen);
			if (mc.player.containerMenu != mc.player.inventoryMenu) mc.player.closeContainer();
		}
		if (t == 55) mc.stop();
	}

	private static final boolean CREEPER = "creeper".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Creeper de Pasto: tres quietos para ver el modelo, y uno que viene caminando, se infla y explota. */
	private static void tickCreeper(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 5.0 180 15");
			comando(mc, "gamemode creative");
			comando(mc, "fill -20 199 -20 20 199 20 minecraft:grass_block");
			comando(mc, "fill -20 200 -20 20 206 20 minecraft:air");
			comando(mc, "kill @e[type=!minecraft:player]");
			comando(mc, "cambio creepers_pasto 0");
			comando(mc, "difficulty normal");
			comando(mc, "time set 6000");
			comando(mc, "summon dedsafio4:creeper_pasto -0.5 200 2.5 {NoAI:1b,Rotation:[20f,0f]}");
			comando(mc, "summon minecraft:creeper 1.5 200 2.0 {NoAI:1b,Rotation:[-20f,0f]}");
		}
		if (t == 8) mc.gui.getChat().clearMessages(false);
		if (t == 20) captura(mc, "debug_cr1_tamano.png");
		if (t == 22) comando(mc, "kill @e[type=!minecraft:player]");
		// Romper pasto sin el cambio (no tiene que salir ninguno) y con el cambio (1 de cada 3, más o menos).
		for (int[] fase : new int[][]{{30, 0, -10}, {50, 1, -12}}) {
			if (t == fase[0] - 4 && fase[1] == 1) comando(mc, "cambio creepers_pasto 1");
			if (t != fase[0]) continue;
			int z0 = fase[2], conCambio = fase[1];
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				var mundo = server.overworld();
				int rotos = 0;
				for (int z = z0; z >= z0 - (conCambio == 1 ? 4 : 0); z -= 2) {
					for (int x = -15; x <= 15; x++) {
						var pos = new net.minecraft.core.BlockPos(x, 199, z);
						if (x % 3 == 0) {   // también pasto (la planta)
							mundo.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState(), 3);
							if (jugador.gameMode.destroyBlock(pos.above())) rotos++;
						} else if (jugador.gameMode.destroyBlock(pos)) rotos++;
					}
				}
				int creepers = mundo.getEntities(com.dedsafio4.nave.ModEntidades.CREEPER_PASTO, e -> e.isAlive()).size();
				Dedsafio4ClientDebug.info("creepers pasto: cambio=" + conCambio + " pasto roto=" + rotos + " creepers=" + creepers);
			});
		}
		if (t == 60) comando(mc, "kill @e[type=dedsafio4:creeper_pasto]");
		// Explosiones sobre el pasto: una tirada de 33% por explosión.
		if (t == 64) {
			for (int k = 0; k < 15; k++) comando(mc, "summon minecraft:tnt " + (-14 + k * 2) + " 200 14 {fuse:0}");
		}
		if (t == 80) {
			server.execute(() -> Dedsafio4ClientDebug.info("creepers pasto: 15 explosiones -> creepers="
					+ server.overworld().getEntities(com.dedsafio4.nave.ModEntidades.CREEPER_PASTO, e -> e.isAlive()).size()));
		}
		if (t == 82) comando(mc, "tp @s 0.5 203 20 180 30");
		if (t == 92) captura(mc, "debug_cr2_explosiones.png");
		if (t == 94) comando(mc, "cambio creepers_pasto 0");
		if (t == 98) mc.stop();
	}

	private static boolean inflado;

	private static final boolean BAMBU = "bambu".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Bloque de Bambú Verde: cada cara con su parte de la textura. */
	private static void tickBambu(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 3.5 201 4.5 145 25");
			comando(mc, "fill -4 199 -4 6 199 6 minecraft:polished_andesite");
			comando(mc, "fill -4 200 -4 6 204 6 minecraft:air");
			comando(mc, "gamemode creative");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			comando(mc, "fill 0 200 0 1 201 0 dedsafio4:bloque_dilitio");
			comando(mc, "setblock -2 200 1 dedsafio4:bloque_dilitio");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:bloque_dilitio 5");
		}
		if (t == 8) mc.gui.getChat().clearMessages(false);
		if (t == 20) captura(mc, "debug_bambu.png");
		if (t == 24) mc.stop();
	}

	private static final boolean DILITIO = "dilitio".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Bloque de Dilitio: con pico de hierro no suelta nada; con pico de diamante suelta Dilitio. Y el tooltip. */
	private static void tickDilitio(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 28) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var bloque1 = new net.minecraft.core.BlockPos(2, 200, 0);
		var bloque2 = new net.minecraft.core.BlockPos(-2, 200, 0);
		var dilitio = com.dedsafio4.items.ModItems.DILITIO;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 2.5 180 20");
			comando(mc, "fill -4 199 -3 4 199 4 minecraft:polished_andesite");
			comando(mc, "fill -4 200 -3 4 203 4 minecraft:air");
			comando(mc, "kill @e[type=item]");
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "setblock 2 200 0 dedsafio4:bloque_dilitio");
			comando(mc, "setblock -2 200 0 dedsafio4:bloque_dilitio");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:iron_pickaxe");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:diamond_pickaxe");
			comando(mc, "item replace entity @s hotbar.4 with dedsafio4:dilitio 9");
			comando(mc, "item replace entity @s hotbar.5 with dedsafio4:bloque_dilitio");
		}
		for (int[] paso : new int[][]{{8, 0, 2}, {14, 1, -2}}) {
			if (t != paso[0]) continue;
			int slot = paso[1];
			var pos = new net.minecraft.core.BlockPos(paso[2], 200, 0);
			mc.player.getInventory().selected = slot;
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				jugador.getInventory().selected = slot;
				jugador.gameMode.destroyBlock(pos);
			});
		}
		if (t == 12) Dedsafio4ClientDebug.info("dilitio con pico de hierro: inventario=" + mc.player.getInventory().countItem(dilitio)
				+ " tirado=" + tirados(mc, dilitio) + " bloque=" + mc.level.getBlockState(bloque1).getBlock());
		if (t == 24) {
			Dedsafio4ClientDebug.info("dilitio con pico de diamante: inventario=" + mc.player.getInventory().countItem(dilitio)
					+ " tirado=" + tirados(mc, dilitio) + " bloque=" + mc.level.getBlockState(bloque2).getBlock());
			var receta = mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("dedsafio4:racimo_dilitio"));
			Dedsafio4ClientDebug.info("receta racimo: " + receta.map(r -> r.value().getResultItem(mc.level.registryAccess()).toString()).orElse("NO"));
			for (var item : new net.minecraft.world.item.Item[]{dilitio, com.dedsafio4.bloques.ModBloques.BLOQUE_DILITIO_ITEM, com.dedsafio4.items.ModItems.CANDADO}) {
				for (var linea : new net.minecraft.world.item.ItemStack(item).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY,
						mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL)) Dedsafio4ClientDebug.info("tooltip: " + linea.getString());
			}
		}
		if (t == 28) {
			net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) ->
					net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(pantalla).register((pp, g, mx, my, delta) -> {
						g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(dilitio), 10, 20);
						g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(com.dedsafio4.bloques.ModBloques.BLOQUE_DILITIO_ITEM), 10, 130);
						g.renderTooltip(cliente.font, new net.minecraft.world.item.ItemStack(com.dedsafio4.items.ModItems.CANDADO), 620, 20);
					}));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 38) captura(mc, "debug_dilitio.png");
		if (t == 44) mc.stop();
	}

	private static final boolean PUERTA = "puerta".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void logPuertas(Minecraft mc, String cuando) {
		var inv = mc.player.getInventory();
		StringBuilder sb = new StringBuilder("puertas " + cuando + ":");
		for (int x : new int[]{-2, 2}) {
			var centro = new net.minecraft.core.BlockPos(x, 201, 0);
			var estado = mc.level.getBlockState(centro);
			sb.append(" [").append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(estado.getBlock()).getPath());
			if (estado.getBlock() instanceof com.dedsafio4.puertas.PuertaBlock) {
				int partes = 0;
				for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
					if (mc.level.getBlockState(centro.offset(dx, dy, 0)).is(estado.getBlock())) partes++;
				sb.append(" partes=").append(partes).append(" frente=").append(estado.getValue(com.dedsafio4.puertas.PuertaBlock.FACING))
						.append(" abierta=").append(estado.getValue(com.dedsafio4.puertas.PuertaBlock.ABIERTA))
						.append(" choqueCentro=").append(!estado.getCollisionShape(mc.level, centro).isEmpty());
			}
			sb.append("]");
		}
		sb.append(" tarjetaPuerta=").append(inv.countItem(com.dedsafio4.items.ModItems.TARJETA_PUERTA_ROSA))
				.append(" tarjetaOtra=").append(inv.countItem(com.dedsafio4.items.ModItems.TARJETA_ROSA))
				.append(" tarjetaVerde=").append(inv.countItem(com.dedsafio4.items.ModItems.TARJETA_PUERTA_VERDE))
				.append(" amuleto=").append(inv.countItem(com.dedsafio4.items.ModItems.AMULETO_VERDE));
		Dedsafio4ClientDebug.info(sb.toString());
	}

	/** Puertas 3x3: se colocan, sin llave (o con la otra) no abren; con su llave se abren y la llave se gasta. */
	private static void tickPuerta(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var rosa = new net.minecraft.core.BlockPos(-2, 201, 0);
		var verde = new net.minecraft.core.BlockPos(2, 201, 0);
		var sur = net.minecraft.core.Direction.SOUTH;
		String cerca = "tp @s 0.5 200 3.2 180 5", lejos = "tp @s 0.5 200 8.5 180 5";
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run " + cerca);
			comando(mc, "fill -7 199 -4 7 199 10 minecraft:polished_andesite");
			comando(mc, "fill -7 200 -4 7 206 10 minecraft:air");
			comando(mc, "gamemode survival");
			comando(mc, "time set 6000");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:puerta_rosa");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:puerta_verde");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:tarjeta_puerta_rosa");
			comando(mc, "item replace entity @s hotbar.6 with dedsafio4:tarjeta_rosa");
			comando(mc, "item replace entity @s hotbar.3 with dedsafio4:tarjeta_puerta_verde");
			comando(mc, "item replace entity @s hotbar.7 with dedsafio4:amuleto_verde");
		}
		if (t == 10) {
			mc.player.getInventory().selected = 0;
			clickEn(mc, new net.minecraft.core.BlockPos(-2, 199, 0), net.minecraft.core.Direction.UP);
		}
		if (t == 14) {
			mc.player.getInventory().selected = 1;
			clickEn(mc, new net.minecraft.core.BlockPos(2, 199, 0), net.minecraft.core.Direction.UP);
		}
		if (t == 18) comando(mc, lejos);
		if (t == 22) logPuertas(mc, "colocadas");
		if (t == 26) captura(mc, "debug_pu1_cerradas.png");
		if (t == 27) comando(mc, cerca);
		if (t == 31) {
			mc.player.getInventory().selected = 6;   // la otra Tarjeta Rosa: no abre
			clickEn(mc, rosa, sur);
		}
		if (t == 33) {
			mc.player.getInventory().selected = 7;   // el Amuleto Verde ya no abre la Verde
			clickEn(mc, verde, sur);
		}
		if (t == 40) logPuertas(mc, "sin llave y con la otra");
		if (t == 42) {
			mc.player.getInventory().selected = 2;
			clickEn(mc, rosa, sur);
		}
		if (t == 43) comando(mc, lejos);
		if (t == 52) captura(mc, "debug_pu2_abriendo.png");
		if (t == 54) comando(mc, cerca);
		if (t == 57) {
			mc.player.getInventory().selected = 3;
			clickEn(mc, verde, sur);
		}
		if (t == 58) comando(mc, lejos);
		if (t == 67) captura(mc, "debug_pu3_abriendo.png");
		if (t == 95) {
			logPuertas(mc, "abiertas");
			captura(mc, "debug_pu4_abiertas.png");
		}
		if (t == 97) comando(mc, "tp @s 0.5 200 -5.5 0 5");
		if (t == 105) captura(mc, "debug_pu5_atras.png");
		if (t == 110) mc.stop();
	}

	private static final boolean PILA = "pila".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Las deditas se apilan de a 99. */
	private static void tickPila(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "give @s dedsafio4:dedita 150");
			comando(mc, "give @s dedsafio4:dedita_verde 99");
			comando(mc, "give @s dedsafio4:dedita_roja 100");
			comando(mc, "give @s dedsafio4:totem_frerico 3");
		}
		if (t == 20) {
			var inv = mc.player.getInventory();
			StringBuilder sb = new StringBuilder("pilas:");
			for (int i = 0; i < 9; i++) if (!inv.getItem(i).isEmpty()) sb.append(" ").append(inv.getItem(i).getCount());
			Dedsafio4ClientDebug.info(sb.toString());
			captura(mc, "debug_pila.png");
		}
		if (t == 26) mc.stop();
	}

	private static final boolean CAJERO = "cajero".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void quickMove(Minecraft mc, int slot) {
		if (mc.player.containerMenu == null) return;
		mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0,
				net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
	}

	private static void logCajero(Minecraft mc, String cuando) {
		var inv = mc.player.getInventory();
		Dedsafio4ClientDebug.info("cajero " + cuando + ": saldo=" + Dedsafio4Client.saldo()
				+ " dedita=" + inv.countItem(com.dedsafio4.items.ModItems.DEDITA)
				+ " verde=" + inv.countItem(com.dedsafio4.items.ModItems.DEDITA_VERDE)
				+ " roja=" + inv.countItem(com.dedsafio4.items.ModItems.DEDITA_ROJA)
				+ " casino=" + inv.countItem(com.dedsafio4.items.ModItems.DEDITA_CASINO)
				+ " negro=" + inv.countItem(com.dedsafio4.items.ModItems.DEDITA_MERCADO_NEGRO)
				+ " pantalla=" + (mc.screen == null ? "-" : mc.screen.getClass().getSimpleName()));
	}

	/** Cajero: ingresar deditas (las de Casino y Mercado Negro no entran) y después sacar. */
	private static void tickCajero(Minecraft mc) {
		int t = ticks - 20;
		mc.options.pauseOnLostFocus = false;
		var pos = new net.minecraft.core.BlockPos(0, 200, -2);
		if (t < 0 && mc.screen != null) mc.setScreen(null);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5 180 10");
			comando(mc, "fill -3 199 -3 3 199 3 minecraft:polished_andesite");
			comando(mc, "fill -3 200 -3 3 203 3 minecraft:air");
			comando(mc, "setblock 0 200 -2 dedsafio4:cajero[facing=south]");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "clear @s");
			comando(mc, "deditas set @s 0");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:dedita 64");
			comando(mc, "item replace entity @s hotbar.1 with dedsafio4:dedita_verde 3");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:dedita_roja 1");
			comando(mc, "item replace entity @s hotbar.3 with dedsafio4:dedita_casino 5");
			comando(mc, "item replace entity @s hotbar.4 with dedsafio4:dedita_mercado_negro 2");
			comando(mc, "item replace entity @s hotbar.5 with dedsafio4:dedita 5");
		}
		if (t == 8) mirar(mc, 180, 25);
		if (t == 18) captura(mc, "debug_ca0_bloque.png");
		if (t == 20) com.dedsafio4.banco.CajeroBlock.abrirPantalla.accept(pos);
		if (t == 28) captura(mc, "debug_ca1_menu.png");
		if (t == 30) apretar(mc, "Ingresar Dinero");
		if (t == 40) {
			logCajero(mc, "grilla abierta");
			for (int slot = 36; slot <= 40; slot++) quickMove(mc, slot);
		}
		if (t == 50) {
			logCajero(mc, "en la grilla");
			captura(mc, "debug_ca2_grilla.png");
		}
		if (t == 55) apretar(mc, "Ingresar");
		if (t == 70) {
			logCajero(mc, "ingresado");
			captura(mc, "debug_ca3_ingresado.png");
			quickMove(mc, 41);
		}
		if (t == 76) mc.player.closeContainer();
		if (t == 86) logCajero(mc, "cerrado con 5 en la grilla");
		if (t == 90) com.dedsafio4.banco.CajeroBlock.abrirPantalla.accept(pos);
		if (t == 95) apretar(mc, "Sacar Dinero");
		if (t == 100 && mc.screen != null) {
			var flechas = mc.screen.children().stream()
					.filter(h -> h instanceof com.dedsafio4.client.cajero.FlechaCajero)
					.map(h -> (com.dedsafio4.client.cajero.FlechaCajero) h).toList();
			flechas.get(0).onPress();
			for (int i = 0; i < 3; i++) flechas.get(2).onPress();
			for (int i = 0; i < 5; i++) flechas.get(4).onPress();
			flechas.get(0).onPress();   // no alcanza para otra Roja
		}
		if (t == 105) captura(mc, "debug_ca4_sacar.png");
		if (t == 108) apretar(mc, "Sacar");
		if (t == 120) {
			logCajero(mc, "sacado");
			captura(mc, "debug_ca5_sacado.png");
		}
		if (t == 126) mc.stop();
	}

	private static final boolean SALDO = "saldo".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** El contador con monedas: 100 deditas = 1 Verde, 100 Verdes = 1 Roja. */
	private static void tickSaldo(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		long[] montos = {70, 270, 10_270, 1_234_505};
		for (int i = 0; i < montos.length; i++) {
			if (t == 10 + i * 20) comando(mc, "deditas set @s " + montos[i]);
			if (t == 20 + i * 20) captura(mc, "debug_saldo" + i + ".png");
		}
		if (t == 10 + montos.length * 20) mc.stop();
	}

	private static final boolean EFECTOS = "efectos".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Con pociones puestas: no hay íconos en la pantalla ni en el inventario, y la Rojizo no tira partículas. */
	private static void tickEfectos(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 80) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0 200 0");
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "effect clear @s");
			comando(mc, "effect give @s minecraft:speed 600");
			comando(mc, "effect give @s minecraft:jump_boost 600");
		}
		if (t == 5) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var jugador = server.getPlayerList().getPlayer(uuid);
				new net.minecraft.world.item.alchemy.PotionContents(com.dedsafio4.pociones.ModPociones.ROJIZO)
						.forEachEffect(jugador::addEffect);
			});
		}
		if (t == 30) {
			Dedsafio4ClientDebug.info("efectos: " + mc.player.getActiveEffects().stream()
					.map(e -> e.getEffect().getRegisteredName() + (e.isVisible() ? "(con particulas)" : "(sin particulas)")).toList());
			captura(mc, "debug_ef1_hud.png");
		}
		if (t == 35) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
		if (t == 70) captura(mc, "debug_ef2_tercera.png");
		if (t == 75) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
		if (t == 80) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		if (t == 90) captura(mc, "debug_ef3_inventario.png");
		if (t == 96) mc.stop();
	}

	private static final boolean EXCREMENTO = "excremento".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** El Plumosaurio a veces deja el montón; con pico sale Excremento y a mano sale el bloque. */
	private static void tickExcremento(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 150) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var detras = new net.minecraft.core.BlockPos(0, 200, 3);
		if (t == -5 || t == -3) revivir(mc);
		if (t == -4) comando(mc, "kill @e[type=dedsafio4:walker]");
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -3.3");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -12 199 -12 12 199 12 minecraft:grass_block");
			comando(mc, "fill -12 200 -12 12 212 12 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "clear @s");
			comando(mc, "tp @s 0.5 200 -3.3 0 -10");
		}
		if (t == 5) {
			comando(mc, "summon dedsafio4:walker 0.5 200 0.5 {NoAI:1b,Rotation:[180f,0f]}");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:fruta_solaria 20");
			mc.player.getInventory().selected = 0;
		}
		// 10 frutas: el montón que queda se saca cada vez para contar cuántas veces lo dejó.
		if (t >= 20 && t < 80 && t % 6 == 0) {
			var w = buscar(mc, com.dedsafio4.bestias.WalkerEntity.class);
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(-8, 199, -8, 8, 203, 8)) {
					if (server.overworld().getBlockState(pos).is(com.dedsafio4.bloques.ModBloques.CACA)) {
						contadorMontones++;
						Dedsafio4ClientDebug.info("monton en " + pos.toShortString());
						server.overworld().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					}
				}
				for (var e : server.overworld().getEntities(com.dedsafio4.nave.ModEntidades.WALKER, x -> true))
					Dedsafio4ClientDebug.info("cuerpo=" + e.yBodyRot + " giro=" + e.getYRot());
			});
			if (w != null) mc.gameMode.interact(mc.player, w, net.minecraft.world.InteractionHand.MAIN_HAND);
		}
		if (t == 90) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(-8, 199, -8, 8, 203, 8))
					if (server.overworld().getBlockState(pos).is(com.dedsafio4.bloques.ModBloques.CACA)) contadorMontones++;
				int sueltos = 0;
				for (var e : server.overworld().getEntities(net.minecraft.world.entity.EntityType.ITEM, x -> x.getItem().is(com.dedsafio4.bloques.ModBloques.CACA_ITEM))) sueltos += e.getItem().getCount();
				Dedsafio4ClientDebug.info("10 frutas: montones=" + contadorMontones + " CACA tirada como item=" + sueltos + " frutas que quedan="
						+ mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.FRUTA_SOLARIA));
			});
		}
		// Romper el montón: con pico da Excremento, a mano da el bloque.
		if (t == 95) {
			comando(mc, "kill @e[type=dedsafio4:walker]");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "setblock 3 200 -3 dedsafio4:caca");
			comando(mc, "setblock -3 200 -3 dedsafio4:caca");
			comando(mc, "tp @s 0.5 200 -8");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:iron_pickaxe");
		}
		if (t == 100) romperBloque(mc, new net.minecraft.core.BlockPos(3, 200, -3));
		if (t == 103) comando(mc, "item replace entity @s hotbar.0 with minecraft:air");
		if (t == 106) romperBloque(mc, new net.minecraft.core.BlockPos(-3, 200, -3));
		if (t == 120) {
			int excrementos = 0, cacas = 0;
			for (var e : mc.level.entitiesForRendering()) {
				if (e instanceof net.minecraft.world.entity.item.ItemEntity ie) {
					if (ie.getItem().is(com.dedsafio4.items.ModItems.EXCREMENTO)) Dedsafio4ClientDebug.info("con pico: Excremento en x=" + ie.getBlockX());
					if (ie.getItem().is(com.dedsafio4.bloques.ModBloques.CACA_ITEM)) Dedsafio4ClientDebug.info("a mano: CACA en x=" + ie.getBlockX());
				}
			}
		}
		if (t == 125) mc.stop();
	}

	private static int contadorMontones;

	private static final boolean BOLSA = "bolsa".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La Bolsa de Tela: se abre, guarda cosas, no acepta otra bolsa y lo guardado sigue ahí al reabrirla. */
	private static void tickBolsa(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 10) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5");
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:bolsa_de_tela");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:diamond 20");
			comando(mc, "item replace entity @s hotbar.2 with dedsafio4:bolsa_de_tela");
			comando(mc, "item replace entity @s hotbar.3 with minecraft:oak_log 64");
			mc.player.getInventory().selected = 0;
		}
		if (t == 10) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 20) {
			var menu = mc.player.containerMenu;
			Dedsafio4ClientDebug.info("abierta: pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName())
					+ " espacios de la bolsa=" + (menu.slots.size() - 36));
			int id = menu.containerId;
			// shift-click de los diamantes (barra 1 = lugar 55) y de los troncos (barra 3 = lugar 57) a la bolsa
			mc.gameMode.handleInventoryMouseClick(id, 55, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
			mc.gameMode.handleInventoryMouseClick(id, 57, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
			// la otra bolsa (barra 2 = lugar 56) no tiene que entrar
			mc.gameMode.handleInventoryMouseClick(id, 56, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
			// la bolsa abierta (barra 0 = lugar 54) no se puede agarrar
			mc.gameMode.handleInventoryMouseClick(id, 54, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
		}
		if (t == 26) {
			var menu = mc.player.containerMenu;
			StringBuilder dentro = new StringBuilder();
			for (int i = 0; i < 27; i++) if (!menu.getSlot(i).getItem().isEmpty()) dentro.append(menu.getSlot(i).getItem()).append(" ");
			Dedsafio4ClientDebug.info("adentro: " + dentro + "| en el cursor: " + menu.getCarried()
					+ " | barra0=" + mc.player.getInventory().getItem(0) + " barra2=" + mc.player.getInventory().getItem(2));
		}
		if (t == 30) captura(mc, "debug_bo1_abierta.png");
		if (t == 34) mc.player.closeContainer();
		// Se vuelve a abrir: lo guardado sigue ahí.
		if (t == 40) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 48) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				var contenido = j.getInventory().getItem(0).get(net.minecraft.core.component.DataComponents.CONTAINER);
				Dedsafio4ClientDebug.info("guardado en la bolsa (servidor): " + (contenido == null ? "nada" : contenido.nonEmptyItemsCopy()));
			});
			StringBuilder dentro = new StringBuilder();
			for (int i = 0; i < 27; i++) if (!mc.player.containerMenu.getSlot(i).getItem().isEmpty()) dentro.append(mc.player.containerMenu.getSlot(i).getItem()).append(" ");
			Dedsafio4ClientDebug.info("al reabrir: " + dentro);
		}
		if (t == 52) mc.player.closeContainer();
		if (t == 56) {
			var pila = mc.player.getInventory().getItem(0);
			for (var linea : pila.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player,
					net.minecraft.world.item.TooltipFlag.Default.NORMAL)) Dedsafio4ClientDebug.info("tooltip: " + linea.getString());
		}
		if (t == 60) mc.stop();
	}

	private static final boolean LISTA_HERMANDADES = "listahermandades".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** La lista de todas las Hermandades (botón del catalejo). */
	private static void tickListaHermandades(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof HermandadScreen) && t > 0) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			var nombre = mc.player.getGameProfile().getName();
			server.execute(() -> {
				var data = server.overworld().getDataStorage().computeIfAbsent(
						com.dedsafio4.hermandad.HermandadesData.FACTORY, "dedsafio4_hermandades");
				data.todas().clear();
				Object[][] hermandades = {{"WEBONES CORP", 0xFFAA00}, {"Aldo Erome Buscar", 0x5555FF}, {"BonitasConRifles", 0xFF55FF},
						{"DOS PUNTOS UVE", 0xAA00AA}, {"EromeTeam", 0xFF55FF}, {"Eufonia", 0x55FF55}, {"Los Alcoholicos", 0xFFFF55},
						{"Los Quiu", 0x55FFFF}, {"Reptiles Unidos", 0xFF5555}};
				for (Object[] h : hermandades) {
					boolean mia = h[0].equals("WEBONES CORP");
					var maestro = mia ? uuid : java.util.UUID.randomUUID();
					var her = new com.dedsafio4.hermandad.HermandadesData.Hermandad((String) h[0], maestro, java.util.List.of(
							new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(maestro, mia ? nombre : "Jugador" + h[0].hashCode() % 100)));
					data.agregar(her);
					data.setColor(her, (Integer) h[1]);
				}
				com.dedsafio4.hermandad.Hermandades.sincronizarJugadores(server);
			});
		}
		if (t == 10) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.dedsafio4.hermandad.HermandadAccionPayload(com.dedsafio4.hermandad.HermandadAccionPayload.PEDIR_INFO, ""));
		if (t == 20 && mc.screen instanceof HermandadScreen pantalla) {
			int x = (pantalla.width - 388) / 2, y = (pantalla.height - 232) / 2;
			pantalla.mouseClicked(x + 388 - 8 - 11, y + 19, 0);   // el catalejo
			Dedsafio4ClientDebug.info("hermandades que conoce el cliente: " + HermandadesCliente.todas().keySet());
		}
		if (t == 28) captura(mc, "debug_lh1_lista.png");
		if (t == 30 && mc.screen instanceof HermandadScreen pantalla) pantalla.mouseScrolled(pantalla.width / 2.0, pantalla.height / 2.0, 0, -1);
		if (t == 36) captura(mc, "debug_lh2_bajando.png");
		if (t == 42) mc.stop();
	}

	private static final boolean FRUTA_ESPACIO = "frutaespacio".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Comer la Fruta del Espacio entre Dimensiones te lleva a otro lugar cercano. */
	private static void tickFrutaEspacio(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5");
			comando(mc, "gamemode survival");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -12 199 -12 12 199 12 minecraft:grass_block");
			comando(mc, "fill -12 200 -12 12 212 12 minecraft:air");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:fruta_espacio 3");
			mc.player.getInventory().selected = 0;
		}
		if (t == 10) Dedsafio4ClientDebug.info("antes: " + mc.player.blockPosition().toShortString());
		if (t >= 12 && t < 60) mc.options.keyUse.setDown(true);
		if (t == 60) {
			mc.options.keyUse.setDown(false);
			Dedsafio4ClientDebug.info("despues: " + mc.player.blockPosition().toShortString() + " frutas que quedan="
					+ mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.FRUTA_ESPACIO));
		}
		if (t == 65) mc.stop();
	}

	private static final boolean LLAVE_CANDADO = "llavecandado".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void tickLlaveCandado(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var mio = new net.minecraft.core.BlockPos(1, 200, 2);
		var ajeno = new net.minecraft.core.BlockPos(-1, 200, 2);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5 0 20");
			comando(mc, "gamemode survival");
			comando(mc, "fill -4 199 -4 4 199 4 minecraft:stone");
			comando(mc, "fill -4 200 -4 4 203 4 minecraft:air");
			comando(mc, "setblock 1 200 2 minecraft:chest[facing=north]");
			comando(mc, "setblock -1 200 2 minecraft:chest[facing=north]");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:llave_candado");
			mc.player.getInventory().selected = 0;
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				var datos = com.dedsafio4.candados.Candados.data(server.overworld());
				datos.posiciones().forEach(datos::sacar);
				datos.poner(mio, "1234", uuid);
				datos.poner(ajeno, "9999", java.util.UUID.randomUUID());
				com.dedsafio4.candados.Candados.avisarCambio(server.overworld());
			});
		}
		if (t == 10) clickEn(mc, ajeno, net.minecraft.core.Direction.NORTH);
		if (t == 16) clickEn(mc, mio, net.minecraft.core.Direction.NORTH);
		if (t == 26) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				var datos = com.dedsafio4.candados.Candados.data(server.overworld());
				Dedsafio4ClientDebug.info("mio con candado=" + (datos.cerradura(mio) != null) + " ajeno con candado=" + (datos.cerradura(ajeno) != null));
			});
			Dedsafio4ClientDebug.info("candados en el inventario=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.CANDADO)
					+ " llave=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.LLAVE_CANDADO)
					+ " pantalla=" + (mc.screen == null ? "ninguna" : mc.screen.getClass().getSimpleName()));
		}
		if (t == 30) mc.stop();
	}

	private static final boolean PILDORA = "pildora".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void tickPildora(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && t < 45) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "effect clear @s");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:pildora 3");
			mc.player.getInventory().selected = 0;
		}
		if (t >= 10 && t < 40) mc.options.keyUse.setDown(true);
		if (t == 40) {
			mc.options.keyUse.setDown(false);
			var efecto = mc.player.getEffect(com.dedsafio4.items.PildoraItem.PROTECCION_ZOMBIFICACION);
			Dedsafio4ClientDebug.info("pildoras que quedan=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.PILDORA)
					+ " efecto=" + (efecto == null ? "no" : efecto.getEffect().value().getDisplayName().getString() + " " + (efecto.getDuration() / 20 / 3600.0) + " horas"));
		}
		if (t == 45) {
			mc.gui.getChat().clearMessages(false);
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
		}
		if (t == 55) captura(mc, "debug_pi1_efecto.png");
		if (t == 60) mc.stop();
	}

	private static final boolean SEIS = "seis".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void tickSeis(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			String[] ids = {"cuchara_madera", "cuchara_hojas", "tenedor_hojas", "cuchara_dorada_hojas", "tarjeta_rosa", "amuleto_verde"};
			for (int i = 0; i < ids.length; i++) comando(mc, "item replace entity @s hotbar." + i + " with dedsafio4:" + ids[i]);
			mc.player.getInventory().selected = 2;
		}
		if (t == 15) {
			mc.gui.getChat().clearMessages(false);
			for (int i = 0; i < 6; i++) Dedsafio4ClientDebug.info("barra " + i + ": " + mc.player.getInventory().getItem(i).getHoverName().getString());
		}
		if (t == 20) captura(mc, "debug_seis.png");
		if (t == 25) mc.stop();
	}

	private static final boolean ARANDANO = "arandano".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void tickArandano(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "effect clear @s");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:arandano_nocturno 4");
			mc.player.getInventory().selected = 0;
		}
		if (t >= 10 && t < 50) mc.options.keyUse.setDown(true);
		if (t == 50) {
			mc.options.keyUse.setDown(false);
			var efecto = mc.player.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
			Dedsafio4ClientDebug.info("arandanos que quedan=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.ARANDANO_NOCTURNO)
					+ " vision nocturna=" + (efecto == null ? "no" : (efecto.getDuration() / 20) + " segundos"));
		}
		if (t == 55) mc.stop();
	}

	private static final boolean SOLARIA = "solaria".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void tickSolaria(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "gamemode survival");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:iron_axe");
			comando(mc, "item replace entity @s weapon.offhand with dedsafio4:fruta_solaria 3");
			mc.player.getInventory().selected = 0;
		}
		if (t == 10) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 16) Dedsafio4ClientDebug.info("solaria: semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_SOLARIA)
				+ " frutas en la otra mano=" + mc.player.getOffhandItem().getCount());
		if (t == 20) mc.stop();
	}

	private static final boolean SOLARIA2 = "solaria2".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static String solariaEn(Minecraft mc, int x, int y, int z) {
		var e = mc.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
		return e.getBlock() == com.dedsafio4.bloques.ModBloques.FRUTA_SOLARIA
				? "solaria edad " + e.getValue(com.dedsafio4.eburia.HuevoEburiaBlock.EDAD) + " mira " + e.getValue(com.dedsafio4.eburia.HuevoEburiaBlock.FACING)
				: e.getBlock().toString();
	}

	/** La Fruta Solaria crece en el costado del tronco: se planta, crece con harina de hueso y aparece en los árboles. */
	private static void tickSolaria2(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var tronco = new net.minecraft.core.BlockPos(0, 200, 2);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -1.5 0 10");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:grass_block");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "fill -3 200 2 3 202 2 dedsafio4:roble_claro_log");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:semilla_solaria 5");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:bone_meal 8");
			mc.player.getInventory().selected = 0;
		}
		// Plantar en la cara norte del tronco (queda en 0 200 1).
		if (t == 10) clickEn(mc, tronco, net.minecraft.core.Direction.NORTH);
		if (t == 16) Dedsafio4ClientDebug.info("plantada: " + solariaEn(mc, 0, 200, 1)
				+ " | semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_SOLARIA));
		if (t == 18) mc.player.getInventory().selected = 1;
		if (t == 21 || t == 25) clickEn(mc, new net.minecraft.core.BlockPos(0, 200, 1), net.minecraft.core.Direction.NORTH);
		if (t == 30) Dedsafio4ClientDebug.info("con harina de hueso: " + solariaEn(mc, 0, 200, 1));
		// Las cuatro etapas, una al lado de la otra, para la foto.
		if (t == 32) {
			comando(mc, "setblock -3 201 1 dedsafio4:fruta_solaria[edad=0,facing=north]");
			comando(mc, "setblock -1 201 1 dedsafio4:fruta_solaria[edad=1,facing=north]");
			comando(mc, "setblock 1 201 1 dedsafio4:fruta_solaria[edad=2,facing=north]");
			comando(mc, "setblock 3 201 1 dedsafio4:fruta_solaria[edad=3,facing=north]");
			comando(mc, "tp @s 0.5 200 -2.5 0 5");
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 36) mc.options.hideGui = true;
		if (t == 40) captura(mc, "debug_so1_etapas.png");
		if (t == 41) mc.options.hideGui = false;
		// Romperla madura: 2-3 frutas. Romper una verde: la semilla.
		if (t == 44) romperBloque(mc, new net.minecraft.core.BlockPos(0, 200, 1));
		if (t == 47) romperBloque(mc, new net.minecraft.core.BlockPos(-1, 201, 1));
		if (t == 60) Dedsafio4ClientDebug.info("rotas: frutas=" + (tirados(mc, com.dedsafio4.items.ModItems.FRUTA_SOLARIA)
				+ mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.FRUTA_SOLARIA))
				+ " semillas=" + (tirados(mc, com.dedsafio4.items.ModItems.SEMILLA_SOLARIA)
				+ mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_SOLARIA)));
		// En los árboles de la jungla del Centro de Quiu.
		if (t == 62) {
			// Una jungla sin generar: la más cercana a un lugar lejano.
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				var mundo = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
				var jungla = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "jungla"));
				var hallado = mundo.findClosestBiome3d(h -> h.is(jungla), new net.minecraft.core.BlockPos(-25000, 100, 31000), 6400, 32, 64);
				if (hallado != null) {
					var p = hallado.getFirst();
					Dedsafio4ClientDebug.info("jungla nueva en " + p.toShortString());
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
							"execute in dedsafio4:dimension_nueva run tp " + mc.player.getGameProfile().getName() + " " + p.getX() + " 220 " + p.getZ());
				}
			});
		}
		if (t == 64) comando(mc, "gamemode creative");
		if (t == 240) {
			var server = mc.getSingleplayerServer();
			var pos = mc.player.blockPosition();
			server.execute(() -> {
				var mundo = server.getLevel(mc.player.level().dimension());
				int solarias = 0, arandanos = 0, bajas = 0;
				String ejemplo = "";
				for (int x = pos.getX() - 64; x <= pos.getX() + 64; x++) {
					for (int z = pos.getZ() - 64; z <= pos.getZ() + 64; z++) {
						if (!mundo.hasChunk(x >> 4, z >> 4)) continue;
						for (int y = 60; y < 200; y++) {
							var b = new net.minecraft.core.BlockPos(x, y, z);
							var e = mundo.getBlockState(b);
							if (e.is(com.dedsafio4.bloques.ModBloques.FRUTA_SOLARIA)) {
								solarias++;
								// ¿cuántos bloques sobre el piso? (hasta encontrar algo sólido abajo)
								int sobre = 0;
								while (sobre < 20 && !mundo.getBlockState(b.below(sobre + 1)).isSolidRender(mundo, b.below(sobre + 1))) sobre++;
								if (sobre <= 5) bajas++;
								if (ejemplo.isEmpty()) ejemplo = x + " " + y + " " + z + " (" + sobre + " bloques sobre el piso)";
							}
							if (e.is(com.dedsafio4.bloques.ModBloques.ARANDANO_NOCTURNO)) arandanos++;
						}
					}
				}
				Dedsafio4ClientDebug.info("bioma=" + mundo.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?")
						+ " solarias=" + solarias + " (bajas: " + bajas + ") arandanos=" + arandanos + " ejemplo: " + ejemplo);
			});
		}
		if (t == 250) mc.stop();
	}

	private static final boolean UVINA = "uvina".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static String uvinaEn(Minecraft mc, int x, int y, int z) {
		var e = mc.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
		return e.getBlock() == com.dedsafio4.bloques.ModBloques.BAYA_UVINA
				? "uvina edad " + e.getValue(com.dedsafio4.eburia.HuevoEburiaBlock.EDAD) : e.getBlock().toString();
	}

	/** La Baya Uvina: se planta en el tronco, crece, el hacha saca la semilla y aparece en la sabana. */
	private static void tickUvina(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var tronco = new net.minecraft.core.BlockPos(0, 200, 2);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -1.5 0 10");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:grass_block");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "fill -3 200 2 3 202 2 dedsafio4:abeto_claro_log");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:semilla_uvina 5");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:bone_meal 8");
			comando(mc, "item replace entity @s hotbar.2 with minecraft:iron_axe");
			mc.player.getInventory().selected = 0;
		}
		if (t == 10) clickEn(mc, tronco, net.minecraft.core.Direction.NORTH);
		if (t == 16) Dedsafio4ClientDebug.info("plantada: " + uvinaEn(mc, 0, 200, 1));
		if (t == 18) mc.player.getInventory().selected = 1;
		if (t == 21 || t == 25) clickEn(mc, new net.minecraft.core.BlockPos(0, 200, 1), net.minecraft.core.Direction.NORTH);
		if (t == 30) Dedsafio4ClientDebug.info("con harina de hueso: " + uvinaEn(mc, 0, 200, 1));
		if (t == 32) {
			comando(mc, "setblock -3 201 1 dedsafio4:baya_uvina[edad=0,facing=north]");
			comando(mc, "setblock -1 201 1 dedsafio4:baya_uvina[edad=1,facing=north]");
			comando(mc, "setblock 1 201 1 dedsafio4:baya_uvina[edad=2,facing=north]");
			comando(mc, "setblock 3 201 1 dedsafio4:baya_uvina[edad=3,facing=north]");
			comando(mc, "tp @s 0.5 200 -2.5 0 5");
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 36) mc.options.hideGui = true;
		if (t == 40) captura(mc, "debug_uv1_etapas.png");
		if (t == 41) mc.options.hideGui = false;
		// Hacha con la baya en la otra mano: sale la semilla.
		if (t == 43) { comando(mc, "item replace entity @s weapon.offhand with dedsafio4:baya_uvina 2"); mc.player.getInventory().selected = 2; }
		if (t == 47) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 53) Dedsafio4ClientDebug.info("hacha: semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_UVINA)
				+ " bayas en la otra mano=" + mc.player.getOffhandItem().getCount());
		if (t == 56) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				var mundo = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
				var sabana = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "sabana"));
				var hallado = mundo.findClosestBiome3d(h -> h.is(sabana), new net.minecraft.core.BlockPos(41000, 100, -37000), 6400, 32, 64);
				if (hallado != null) {
					var p = hallado.getFirst();
					Dedsafio4ClientDebug.info("sabana nueva en " + p.toShortString());
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
							"execute in dedsafio4:dimension_nueva run tp " + mc.player.getGameProfile().getName() + " " + p.getX() + " 150 " + p.getZ());
				}
			});
		}
		if (t == 58) comando(mc, "gamemode creative");
		if (t == 230) {
			var server = mc.getSingleplayerServer();
			var pos = mc.player.blockPosition();
			server.execute(() -> {
				var mundo = server.getLevel(mc.player.level().dimension());
				int uvinas = 0, eburias = 0;
				for (int x = pos.getX() - 64; x <= pos.getX() + 64; x++) {
					for (int z = pos.getZ() - 64; z <= pos.getZ() + 64; z++) {
						if (!mundo.hasChunk(x >> 4, z >> 4)) continue;
						for (int y = 50; y < 160; y++) {
							var e = mundo.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
							if (e.is(com.dedsafio4.bloques.ModBloques.BAYA_UVINA)) uvinas++;
							if (e.is(com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA)) eburias++;
						}
					}
				}
				Dedsafio4ClientDebug.info("bioma=" + mundo.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?")
						+ " bayas uvina=" + uvinas + " huevos eburia=" + eburias);
			});
		}
		if (t == 240) mc.stop();
	}

	private static final boolean ARANDANO3 = "arandano3".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static String arandanoEn(Minecraft mc, int x, int y, int z) {
		var e = mc.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
		return e.getBlock() == com.dedsafio4.bloques.ModBloques.ARANDANO_NOCTURNO
				? "arandano edad " + e.getValue(com.dedsafio4.eburia.HuevoEburiaBlock.EDAD) : e.getBlock().toString();
	}

	/** La Baya Uvina: se planta en el tronco, crece, el hacha saca la semilla y aparece en la sabana. */
	private static void tickArandano3(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var tronco = new net.minecraft.core.BlockPos(0, 200, 2);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -1.5 0 10");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:grass_block");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "fill -3 200 2 3 202 2 dedsafio4:roble_claro_log");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:semilla_arandano 5");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:bone_meal 8");
			comando(mc, "item replace entity @s hotbar.2 with minecraft:iron_axe");
			mc.player.getInventory().selected = 0;
		}
		if (t == 7) mc.player.getInventory().selected = 0;
		if (t == 9) Dedsafio4ClientDebug.info("antes: tronco=" + mc.level.getBlockState(tronco) + " delante=" + mc.level.getBlockState(tronco.north())
				+ " en la mano=" + mc.player.getMainHandItem() + " bloque de la semilla=" + ((net.minecraft.world.item.BlockItem) com.dedsafio4.items.ModItems.SEMILLA_ARANDANO).getBlock());
		if (t == 10) {
			var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tronco), net.minecraft.core.Direction.NORTH, tronco, false);
			Dedsafio4ClientDebug.info("usar: " + mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
		}
		if (t == 16) Dedsafio4ClientDebug.info("plantada: " + arandanoEn(mc, 0, 200, 1));
		if (t == 18) mc.player.getInventory().selected = 1;
		if (t == 21 || t == 25) clickEn(mc, new net.minecraft.core.BlockPos(0, 200, 1), net.minecraft.core.Direction.NORTH);
		if (t == 30) Dedsafio4ClientDebug.info("con harina de hueso: " + arandanoEn(mc, 0, 200, 1));
		if (t == 32) {
			comando(mc, "setblock -3 201 1 dedsafio4:arandano_nocturno[edad=0,facing=north]");
			comando(mc, "setblock -1 201 1 dedsafio4:arandano_nocturno[edad=1,facing=north]");
			comando(mc, "setblock 1 201 1 dedsafio4:arandano_nocturno[edad=2,facing=north]");
			comando(mc, "setblock 3 201 1 dedsafio4:arandano_nocturno[edad=3,facing=north]");
			comando(mc, "tp @s 0.5 200 -2.5 0 5");
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 36) mc.options.hideGui = true;
		if (t == 40) captura(mc, "debug_ar3_etapas.png");
		if (t == 41) mc.options.hideGui = false;
		// Hacha con la baya en la otra mano: sale la semilla.
		if (t == 43) { comando(mc, "item replace entity @s weapon.offhand with dedsafio4:arandano_nocturno 2"); mc.player.getInventory().selected = 2; }
		if (t == 47) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 53) Dedsafio4ClientDebug.info("hacha: semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_ARANDANO)
				+ " bayas en la otra mano=" + mc.player.getOffhandItem().getCount());
		if (t == 56) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				var mundo = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
				var sabana = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "jungla"));
				var hallado = mundo.findClosestBiome3d(h -> h.is(sabana), new net.minecraft.core.BlockPos(-52000, 100, -47000), 6400, 32, 64);
				if (hallado != null) {
					var p = hallado.getFirst();
					Dedsafio4ClientDebug.info("sabana nueva en " + p.toShortString());
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
							"execute in dedsafio4:dimension_nueva run tp " + mc.player.getGameProfile().getName() + " " + p.getX() + " 150 " + p.getZ());
				}
			});
		}
		if (t == 58) comando(mc, "gamemode creative");
		if (t == 230) {
			var server = mc.getSingleplayerServer();
			var pos = mc.player.blockPosition();
			server.execute(() -> {
				var mundo = server.getLevel(mc.player.level().dimension());
				int uvinas = 0, eburias = 0;
				for (int x = pos.getX() - 64; x <= pos.getX() + 64; x++) {
					for (int z = pos.getZ() - 64; z <= pos.getZ() + 64; z++) {
						if (!mundo.hasChunk(x >> 4, z >> 4)) continue;
						for (int y = 50; y < 160; y++) {
							var e = mundo.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
							if (e.is(com.dedsafio4.bloques.ModBloques.ARANDANO_NOCTURNO)) uvinas++;
							if (e.is(com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA)) eburias++;
						}
					}
				}
				Dedsafio4ClientDebug.info("bioma=" + mundo.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?")
						+ " arandanos=" + uvinas + " huevos eburia=" + eburias);
			});
		}
		if (t == 240) mc.stop();
	}

	private static final boolean EBURIA3 = "eburia3".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static String eburiaEn(Minecraft mc, int x, int y, int z) {
		var e = mc.level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
		return e.getBlock() == com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA
				? "huevo edad " + e.getValue(com.dedsafio4.eburia.HuevoEburiaBlock.EDAD) : e.getBlock().toString();
	}

	/** La Baya Uvina: se planta en el tronco, crece, el hacha saca la semilla y aparece en la sabana. */
	private static void tickEburia3(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var tronco = new net.minecraft.core.BlockPos(0, 200, 2);
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -1.5 0 10");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:grass_block");
			comando(mc, "fill -8 200 -8 8 206 8 minecraft:air");
			comando(mc, "kill @e[type=minecraft:item]");
			comando(mc, "fill -3 200 2 3 202 2 dedsafio4:roble_claro_log");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:semilla_eburia 5");
			comando(mc, "item replace entity @s hotbar.1 with minecraft:bone_meal 8");
			comando(mc, "item replace entity @s hotbar.2 with minecraft:iron_axe");
			mc.player.getInventory().selected = 0;
		}
		if (t == 7) mc.player.getInventory().selected = 0;
		if (t == 9) Dedsafio4ClientDebug.info("antes: tronco=" + mc.level.getBlockState(tronco) + " delante=" + mc.level.getBlockState(tronco.north())
				+ " en la mano=" + mc.player.getMainHandItem() + " bloque de la semilla=" + ((net.minecraft.world.item.BlockItem) com.dedsafio4.items.ModItems.SEMILLA_EBURIA).getBlock());
		if (t == 10) {
			var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tronco), net.minecraft.core.Direction.NORTH, tronco, false);
			Dedsafio4ClientDebug.info("usar: " + mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
		}
		if (t == 16) Dedsafio4ClientDebug.info("plantada: " + eburiaEn(mc, 0, 200, 1));
		if (t == 18) mc.player.getInventory().selected = 1;
		if (t == 21 || t == 25) clickEn(mc, new net.minecraft.core.BlockPos(0, 200, 1), net.minecraft.core.Direction.NORTH);
		if (t == 30) Dedsafio4ClientDebug.info("con harina de hueso: " + eburiaEn(mc, 0, 200, 1));
		if (t == 32) {
			comando(mc, "setblock -3 201 1 dedsafio4:huevo_eburia[edad=0,facing=north]");
			comando(mc, "setblock -1 201 1 dedsafio4:huevo_eburia[edad=1,facing=north]");
			comando(mc, "setblock 1 201 1 dedsafio4:huevo_eburia[edad=2,facing=north]");
			comando(mc, "setblock 3 201 1 dedsafio4:huevo_eburia[edad=3,facing=north]");
			comando(mc, "tp @s 0.5 200 -2.5 0 5");
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 36) mc.options.hideGui = true;
		if (t == 40) captura(mc, "debug_eb3_etapas.png");
		if (t == 41) mc.options.hideGui = false;
		// Hacha con la baya en la otra mano: sale la semilla.
		if (t == 43) { comando(mc, "item replace entity @s weapon.offhand with dedsafio4:huevo_eburia 2"); mc.player.getInventory().selected = 2; }
		if (t == 47) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 53) Dedsafio4ClientDebug.info("hacha: semillas=" + mc.player.getInventory().countItem(com.dedsafio4.items.ModItems.SEMILLA_EBURIA)
				+ " bayas en la otra mano=" + mc.player.getOffhandItem().getCount());
		if (t == 56) {
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				var mundo = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
				var sabana = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "sabana"));
				var hallado = mundo.findClosestBiome3d(h -> h.is(sabana), new net.minecraft.core.BlockPos(63000, 100, 58000), 6400, 32, 64);
				if (hallado != null) {
					var p = hallado.getFirst();
					Dedsafio4ClientDebug.info("sabana nueva en " + p.toShortString());
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
							"execute in dedsafio4:dimension_nueva run tp " + mc.player.getGameProfile().getName() + " " + p.getX() + " 150 " + p.getZ());
				}
			});
		}
		if (t == 58) comando(mc, "gamemode creative");
		if (t == 230) {
			var server = mc.getSingleplayerServer();
			var pos = mc.player.blockPosition();
			server.execute(() -> {
				var mundo = server.getLevel(mc.player.level().dimension());
				int uvinas = 0, eburias = 0;
				for (int x = pos.getX() - 64; x <= pos.getX() + 64; x++) {
					for (int z = pos.getZ() - 64; z <= pos.getZ() + 64; z++) {
						if (!mundo.hasChunk(x >> 4, z >> 4)) continue;
						for (int y = 50; y < 160; y++) {
							var e = mundo.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
							if (e.is(com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA)) uvinas++;
							if (e.is(com.dedsafio4.bloques.ModBloques.HUEVO_EBURIA)) eburias++;
						}
					}
				}
				Dedsafio4ClientDebug.info("bioma=" + mundo.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?")
						+ " huevos=" + uvinas + " huevos eburia=" + eburias);
			});
		}
		if (t == 240) mc.stop();
	}

	private static final boolean ARBOLES = "arboles".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Va a una zona nueva de un bioma del Centro de Quiu (desde lejos) y vuelve la posición. */
	private static void irABioma(Minecraft mc, String bioma, int x, int z) {
		var server = mc.getSingleplayerServer();
		server.execute(() -> {
			var mundo = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "dimension_nueva")));
			var clave = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", bioma));
			var hallado = mundo.findClosestBiome3d(h -> h.is(clave), new net.minecraft.core.BlockPos(x, 100, z), 6400, 32, 64);
			if (hallado == null) return;
			var pos = hallado.getFirst();
			Dedsafio4ClientDebug.info(bioma + " nueva en " + pos.toShortString());
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
					"execute in dedsafio4:dimension_nueva run tp " + mc.player.getGameProfile().getName() + " " + pos.getX() + " 190 " + pos.getZ() + " 0 70");
		});
	}

	/** Cuenta los árboles (troncos que salen del piso) alrededor del jugador. */
	private static void contarArboles(Minecraft mc, String bioma) {
		var server = mc.getSingleplayerServer();
		var centro = mc.player.blockPosition();
		server.execute(() -> {
			var mundo = server.getLevel(mc.player.level().dimension());
			int arboles = 0, chunks = 0;
			for (int cx = (centro.getX() >> 4) - 4; cx <= (centro.getX() >> 4) + 4; cx++) {
				for (int cz = (centro.getZ() >> 4) - 4; cz <= (centro.getZ() >> 4) + 4; cz++) {
					if (!mundo.hasChunk(cx, cz)) continue;
					chunks++;
					for (int x = cx * 16; x < cx * 16 + 16; x++) {
						for (int z = cz * 16; z < cz * 16 + 16; z++) {
							for (int y = 50; y < 140; y++) {
								var b = new net.minecraft.core.BlockPos(x, y, z);
								var e = mundo.getBlockState(b);
								boolean tronco = e.is(com.dedsafio4.bloques.ModBloques.ROBLE_CLARO_LOG) || e.is(com.dedsafio4.bloques.ModBloques.ABETO_CLARO_LOG);
								// esquina noroeste del tronco 2x2, a 6 bloques del piso (ahí la base ya no se ensancha)
								if (tronco && mundo.getBlockState(b.east()).is(e.getBlock()) && mundo.getBlockState(b.south()).is(e.getBlock())
										&& mundo.getBlockState(b.east().south()).is(e.getBlock())
										&& !mundo.getBlockState(b.west()).is(e.getBlock()) && !mundo.getBlockState(b.north()).is(e.getBlock())
										&& mundo.getBlockState(b.below(6)).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) arboles++;
							}
						}
					}
				}
			}
			Dedsafio4ClientDebug.info(bioma + ": " + arboles + " arboles en " + chunks + " chunks = "
					+ String.format("%.2f", chunks == 0 ? 0 : arboles / (double) chunks) + " por chunk");
		});
	}

	private static void tickArboles(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) { comando(mc, "gamemode spectator"); comando(mc, "time set day"); }
		if (t == 5) irABioma(mc, "jungla", 91000, 83000);
		if (t >= 6 && t < 620) mirar(mc, 0f, 70f);
		if (t == 300) contarArboles(mc, "jungla");
		if (t == 302) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 305) captura(mc, "debug_arb1_jungla.png");
		if (t == 307) mc.options.hideGui = false;
		if (t == 310) irABioma(mc, "sabana", -97000, 88000);
		if (t == 600) contarArboles(mc, "sabana");
		if (t == 602) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 605) captura(mc, "debug_arb2_sabana.png");
		if (t == 607) mc.options.hideGui = false;
		if (t == 615) mc.stop();
	}

	private static final boolean FRUTAS = "frutas".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Las cuatro frutas del tronco, una por fila, en sus cuatro etapas (brote, verde, madura, podrida). */
	private static void tickFrutas(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 -3.5 0 0");
			comando(mc, "gamemode creative");
			comando(mc, "time set day");
			comando(mc, "forceload add -16 -16 16 16");
			comando(mc, "fill -8 199 -8 8 199 8 minecraft:grass_block");
			comando(mc, "fill -8 200 -8 8 208 8 minecraft:air");
			comando(mc, "fill -4 200 2 4 203 2 dedsafio4:roble_claro_log");
		}
		if (t == 5) {
			String[] frutas = {"huevo_eburia", "baya_uvina", "fruta_solaria", "arandano_nocturno"};
			for (int fila = 0; fila < 4; fila++) {
				for (int edad = 0; edad < 4; edad++) {
					comando(mc, "setblock " + (3 - edad * 2) + " " + (203 - fila) + " 1 dedsafio4:" + frutas[fila] + "[edad=" + edad + ",facing=north]");
				}
			}
		}
		if (t >= 3 && t < 30) mirar(mc, 0f, -12f);
		if (t == 20) { mc.options.hideGui = true; mc.gui.getChat().clearMessages(false); }
		if (t == 25) captura(mc, "debug_fr1_frente.png");
		if (t == 28) comando(mc, "tp @s -3.5 201 -2 -40 -5");
		if (t >= 29 && t < 40) mirar(mc, -40f, -5f);
		if (t == 36) captura(mc, "debug_fr2_costado.png");
		if (t == 40) { mc.options.hideGui = false; mc.stop(); }
	}

	private static final boolean ESTANDARTES = "estandartes".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Aprieta un botón de la pantalla por su texto. */
	private static void apretar(Minecraft mc, String texto) {
		if (mc.screen == null) return;
		for (var hijo : mc.screen.children()) {
			if (hijo instanceof net.minecraft.client.gui.components.AbstractButton b && b.getMessage().getString().equals(texto)) {
				Dedsafio4ClientDebug.info("boton " + texto + " activo=" + b.active);
				if (b.active) b.onPress();
				return;
			}
		}
		Dedsafio4ClientDebug.info("no hay boton " + texto);
	}

	/** Colección de Estandartes: guardar el de la mano, activarlo, la capa de los miembros y la lista. */
	private static void tickEstandartes(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof HermandadScreen) && t > 0) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5 180 0");
			comando(mc, "gamemode survival");
			comando(mc, "time set day");
			comando(mc, "fill -6 199 -6 6 199 6 minecraft:grass_block");
			comando(mc, "fill -6 200 -6 6 206 6 minecraft:air");
			comando(mc, "clear @s");
			comando(mc, "item replace entity @s hotbar.0 with minecraft:red_banner[banner_patterns=[{pattern:\"minecraft:creeper\",color:\"black\"},{pattern:\"minecraft:border\",color:\"yellow\"}]]");
			mc.player.getInventory().selected = 0;
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			var nombre = mc.player.getGameProfile().getName();
			server.execute(() -> {
				var data = server.overworld().getDataStorage().computeIfAbsent(
						com.dedsafio4.hermandad.HermandadesData.FACTORY, "dedsafio4_hermandades");
				data.todas().clear();
				var h = new com.dedsafio4.hermandad.HermandadesData.Hermandad("Los Quiu", uuid, java.util.List.of(
						new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(uuid, nombre)));
				data.agregar(h);
				data.setColor(h, 0x55FF55);
				data.agregar(new com.dedsafio4.hermandad.HermandadesData.Hermandad("Otra Hermandad", java.util.UUID.randomUUID(),
						java.util.List.of(new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(java.util.UUID.randomUUID(), "Alguien"))));
				com.dedsafio4.hermandad.Hermandades.sincronizarJugadores(server);
			});
		}
		if (t == 8) mc.player.getInventory().selected = 0;
		if (t == 10) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.dedsafio4.hermandad.HermandadAccionPayload(com.dedsafio4.hermandad.HermandadAccionPayload.PEDIR_INFO, ""));
		// Pestaña Estandartes (la cuarta).
		if (t == 18 && mc.screen instanceof HermandadScreen pantalla) {
			int x = (pantalla.width - 388) / 2, y = (pantalla.height - 232) / 2;
			pantalla.mouseClicked(x + 8 + 3 * 26 + 5, y + 12, 0);
		}
		if (t == 22) apretar(mc, "Guardar Mano");
		if (t == 30) {
			Dedsafio4ClientDebug.info("coleccion=" + HermandadesCliente.coleccion().size() + " en la mano=" + mc.player.getMainHandItem());
			if (mc.screen instanceof HermandadScreen pantalla) {
				int x0 = (pantalla.width - 388) / 2 + (388 - 24 * 9) / 2, y0 = (pantalla.height - 232) / 2 + 68;
				pantalla.mouseClicked(x0 + 10, y0 + 10, 0);   // el primer lugar
			}
		}
		if (t == 33) apretar(mc, "Activar");
		if (t == 42) {
			Dedsafio4ClientDebug.info("activo=" + HermandadesCliente.activo() + " mi capa=" + HermandadesCliente.estandarteDe(mc.player.getUUID()));
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 45) captura(mc, "debug_es1_coleccion.png");
		// La capa, vista desde atrás.
		if (t == 48) { mc.setScreen(null); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK); mc.options.hideGui = true; }
		if (t >= 48 && t < 62) mirar(mc, 180f, 20f);
		if (t == 60) captura(mc, "debug_es2_capa.png");
		if (t == 62) { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); }
		if (t == 68) captura(mc, "debug_es3_frente.png");
		if (t == 70) { mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); mc.options.hideGui = false; }
		// La lista de Hermandades con el estandarte.
		if (t == 72) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.dedsafio4.hermandad.HermandadAccionPayload(com.dedsafio4.hermandad.HermandadAccionPayload.PEDIR_INFO, ""));
		if (t == 80 && mc.screen instanceof HermandadScreen pantalla) {
			int x = (pantalla.width - 388) / 2, y = (pantalla.height - 232) / 2;
			pantalla.mouseClicked(x + 388 - 8 - 11, y + 19, 0);
		}
		if (t == 88) captura(mc, "debug_es4_lista.png");
		if (t == 92) mc.stop();
	}

	private static final boolean TABLON = "tablon".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void pestanaHermandad(Minecraft mc, int indice) {
		if (!(mc.screen instanceof HermandadScreen pantalla)) return;
		int x = (pantalla.width - 388) / 2, y = (pantalla.height - 232) / 2;
		pantalla.mouseClicked(x + 8 + indice * 26 + 5, y + 12, 0);
	}

	/** Escribe en el campo de anuncio del Tablón y aprieta Publicar. */
	private static void escribirAnuncio(Minecraft mc, String texto) {
		if (mc.screen == null) return;
		for (var hijo : mc.screen.children()) {
			if (hijo instanceof net.minecraft.client.gui.components.EditBox caja) {
				caja.setValue(texto);
				apretar(mc, "Publicar");
				return;
			}
		}
		Dedsafio4ClientDebug.info("no hay campo de anuncio");
	}

	private static void crearHermandad(Minecraft mc, boolean yoMaestro) {
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		var nombre = mc.player.getGameProfile().getName();
		server.execute(() -> {
			var data = server.overworld().getDataStorage().computeIfAbsent(
					com.dedsafio4.hermandad.HermandadesData.FACTORY, "dedsafio4_hermandades");
			data.todas().clear();
			var otro = java.util.UUID.randomUUID();
			var maestro = yoMaestro ? uuid : otro;
			var h = new com.dedsafio4.hermandad.HermandadesData.Hermandad("Los Quiu", maestro, java.util.List.of(
					new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(maestro, yoMaestro ? nombre : "ElMaestro"),
					new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(yoMaestro ? java.util.UUID.randomUUID() : uuid, yoMaestro ? "Locochon" : nombre),
					new com.dedsafio4.hermandad.ManuscritoDatos.Inscrito(java.util.UUID.randomUUID(), "AQUINOby02")));
			data.agregar(h);
			data.setColor(h, 0xFFAA00);
			if (!yoMaestro) data.setLider(h, uuid, true);
			com.dedsafio4.hermandad.Hermandades.sincronizarJugadores(server);
		});
	}

	/** Tablones (anuncios del Maestro y los Líderes) y subir de rango en Gestión. */
	private static void tickTablon(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null && !(mc.screen instanceof HermandadScreen) && t > 0) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) crearHermandad(mc, true);
		if (t == 8) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.dedsafio4.hermandad.HermandadAccionPayload(com.dedsafio4.hermandad.HermandadAccionPayload.PEDIR_INFO, ""));
		// Gestión: subir a Locochon a Líder.
		if (t == 16) pestanaHermandad(mc, 4);
		if (t == 20) apretar(mc, "^");
		if (t == 30) { mc.gui.getChat().clearMessages(false); captura(mc, "debug_ta1_gestion.png"); }
		// Tablones: publicar dos anuncios y borrar uno.
		if (t == 33) pestanaHermandad(mc, 1);
		if (t == 37) escribirAnuncio(mc, "CASA -623 72 -21");
		if (t == 43) escribirAnuncio(mc, "GRANJA DE EXP en -482 -17 -3, vengan todos que hay mucha experiencia. No rompan los hoppers por favor.");
		if (t == 49) escribirAnuncio(mc, "anuncio para borrar");
		if (t == 55) apretar(mc, "x");
		if (t == 62) {
			Dedsafio4ClientDebug.info("anuncios=" + HermandadesCliente.anuncios().size());
			for (var a : HermandadesCliente.anuncios()) Dedsafio4ClientDebug.info("  " + a.fecha() + " " + a.autor() + ": " + a.texto());
			for (var l : HermandadesCliente.chat()) Dedsafio4ClientDebug.info("  chat: " + l.texto());
			mc.gui.getChat().clearMessages(false);
		}
		if (t == 64) captura(mc, "debug_ta2_tablon.png");
		// Ahora el jugador es solo Líder (no Maestro): también puede publicar.
		if (t == 68) { mc.setScreen(null); crearHermandad(mc, false); }
		if (t == 76) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.dedsafio4.hermandad.HermandadAccionPayload(com.dedsafio4.hermandad.HermandadAccionPayload.PEDIR_INFO, ""));
		if (t == 84) pestanaHermandad(mc, 1);
		if (t == 88) escribirAnuncio(mc, "Soy Líder y puedo publicar");
		if (t == 96) Dedsafio4ClientDebug.info("como Líder: anuncios=" + HermandadesCliente.anuncios().size()
				+ (HermandadesCliente.anuncios().isEmpty() ? "" : " primero=" + HermandadesCliente.anuncios().get(0).texto()));
		if (t == 100) mc.stop();
	}

	private static final boolean LINTERNA = "linterna".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	private static void tickLinterna(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5 0 5");
			comando(mc, "gamemode survival");
			comando(mc, "time set midnight");
			comando(mc, "kill @e[type=minecraft:zombie]");
			comando(mc, "fill -8 199 -8 8 199 20 minecraft:stone");
			comando(mc, "fill -8 200 -8 8 208 20 minecraft:air");
			comando(mc, "fill -8 200 12 8 206 12 minecraft:stone");
			comando(mc, "fill -8 207 -8 8 207 20 minecraft:stone");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "item replace entity @s hotbar.0 with dedsafio4:linterna");
		}
		if (t >= 3 && t < 120) mirar(mc, 0f, 5f);
		if (t == 8) mc.player.getInventory().selected = 0;
		if (t == 14) { mc.options.hideGui = true; }
		if (t == 38) captura(mc, "debug_li0_apagada.png");
		if (t == 40) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 70) {
			mc.options.hideGui = false;
			mc.gui.getChat().clearMessages(false);
			var server = mc.getSingleplayerServer();
			server.execute(() -> {
				int luces = 0;
				String donde = "";
				for (var pos : net.minecraft.core.BlockPos.betweenClosed(-8, 199, -8, 8, 207, 20)) {
					if (server.overworld().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LIGHT)) { luces++; donde = pos.toShortString(); }
				}
				Dedsafio4ClientDebug.info("prendida=" + com.dedsafio4.items.LinternaItem.prendida(mc.player.getMainHandItem())
						+ " luces=" + luces + " en " + donde + " luz ahi=" + server.overworld().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new net.minecraft.core.BlockPos(0, 201, 10)));
			});
		}
		if (t == 71) captura(mc, "debug_li1_prendida.png");
		// Un zombi en el haz.
		if (t == 72) comando(mc, "summon minecraft:zombie 0.5 200 8.5 {NoAI:1b}");
		if (t == 75) Dedsafio4ClientDebug.info("zombi antes: vida=" + vidaZombi(mc));
		if (t == 115) Dedsafio4ClientDebug.info("zombi despues de 2 segundos en el haz: vida=" + vidaZombi(mc));
		if (t == 118) { mc.options.hideGui = true; comando(mc, "kill @e[type=!minecraft:player]"); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); }
		if (t >= 118 && t < 132) mirar(mc, 0f, 5f);
		if (t == 128) captura(mc, "debug_li2_tercera.png");
		if (t == 132) { mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); mc.options.hideGui = false; }
		// Batería: casi vacía → se apaga sola al gastar el último punto.
		if (t == 135) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> com.dedsafio4.items.LinternaItem.setBateria(server.getPlayerList().getPlayer(uuid).getMainHandItem(), 1));
		}
		if (t == 350) {
			var pila = mc.player.getMainHandItem();
			Dedsafio4ClientDebug.info("sin bateria: prendida=" + com.dedsafio4.items.LinternaItem.prendida(pila) + " bateria=" + com.dedsafio4.items.LinternaItem.bateria(pila) + " item=" + pila.getItem());
		}
		// Recargar con la Batería de Dilitio en la mano secundaria.
		if (t == 352) comando(mc, "item replace entity @s weapon.offhand with dedsafio4:bateria_dilitio 2");
		if (t == 356) mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
		if (t == 362) {
			var pila = mc.player.getMainHandItem();
			Dedsafio4ClientDebug.info("recargada: bateria=" + com.dedsafio4.items.LinternaItem.bateria(pila) + " baterias=" + mc.player.getOffhandItem().getCount());
			for (var linea : pila.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, mc.player,
					net.minecraft.world.item.TooltipFlag.Default.NORMAL)) Dedsafio4ClientDebug.info("tooltip: " + linea.getString());
		}
		if (t == 364) mc.gui.getChat().clearMessages(false);
		if (t == 368) captura(mc, "debug_li3_barra.png");
		if (t == 372) mc.stop();
	}

	private static String vidaZombi(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		for (var e : server.overworld().getEntities(net.minecraft.world.entity.EntityType.ZOMBIE, x -> true)) return String.valueOf(e.getHealth()) + " fuego=" + e.isOnFire();
		return "muerto";
	}

	private static final boolean TOTEM = "totem".equals(System.getenv("DEDSAFIO4_DEBUG_CIELO"));

	/** Totem Frerico: 2 corazones en la mano, y te salva de morir (se gasta), con la animación del tótem. */
	private static void tickTotem(Minecraft mc) {
		int t = ticks - 20;
		if (mc.screen != null) mc.setScreen(null);
		mc.options.pauseOnLostFocus = false;
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (t == -5 || t == -3) revivir(mc);
		if (t == 0) {
			comando(mc, "execute in minecraft:overworld run tp @s 0.5 200 0.5 0 0");
			comando(mc, "gamemode survival");
			comando(mc, "difficulty normal");
			comando(mc, "time set day");
			comando(mc, "clear @s");
			comando(mc, "effect clear @s");
			comando(mc, "item replace entity @s weapon.offhand with dedsafio4:totem_frerico");
		}
		if (t == 10) Dedsafio4ClientDebug.info("totem en la mano secundaria: vida maxima=" + mc.player.getMaxHealth());
		if (t == 12) mc.gui.getChat().clearMessages(false);
		if (t == 30) {
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				j.invulnerableTime = 0;
				boolean pego = j.hurt(j.damageSources().generic(), 1000f);
				Dedsafio4ClientDebug.info("totem golpe mortal: pego=" + pego + " modo=" + j.gameMode.getGameModeForPlayer() + " vivo=" + j.isAlive() + " vida=" + j.getHealth() + " totems=" + j.getInventory().countItem(com.dedsafio4.items.ModItems.TOTEM_FRERICO)
						+ " regeneracion=" + j.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION));
			});
		}
		if (t == 38) captura(mc, "debug_to2_salva.png");
		if (t == 70) {
			server.execute(() -> {
				var j = server.getPlayerList().getPlayer(uuid);
				j.invulnerableTime = 0;
				j.hurt(j.damageSources().generic(), 1000f);
				Dedsafio4ClientDebug.info("totem segundo golpe (ya sin totem): vivo=" + j.isAlive());
			});
		}
		if (t == 80) revivir(mc);
		if (t == 90) mc.stop();
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		ticks++;
		if (G_ESCALA) {
			tickGEscala(mc);
			return;
		}
		if (CAPSULA) {
			tickCapsula(mc);
			return;
		}
		if (CATALOGO_EDITAR) {
			tickCatalogoEditar(mc);
			return;
		}
		if (MISIONES) {
			tickMisiones(mc);
			return;
		}
		if (CATALOGO) {
			tickCatalogo(mc);
			return;
		}
		if (BOLSA_ENDER) {
			tickBolsaEnder(mc);
			return;
		}
		if (CRISTAL) {
			tickCristal(mc);
			return;
		}
		if (COFRE_HUESOS) {
			tickCofreHuesos(mc);
			return;
		}
		if (GAS) {
			tickGas(mc);
			return;
		}
		if (CORAZON) {
			tickCorazon(mc);
			return;
		}
		if (ANTIBIOTICO) {
			tickAntibiotico(mc);
			return;
		}
		if (NO_BOSS) {
			tickNoBoss(mc);
			return;
		}
		if (BOSS_IA) {
			tickBossIa(mc);
			return;
		}
		if (ATRAER_SALTO) {
			tickAtraerSalto(mc);
			return;
		}
		if (AGARRE) {
			tickAgarre(mc);
			return;
		}
		if (ATRAER) {
			tickAtraer(mc);
			return;
		}
		if (CIRCULOS) {
			tickCirculos(mc);
			return;
		}
		if (PINCHITOS) {
			tickPinchitos(mc);
			return;
		}
		if (CAPULLO) {
			tickCapullo(mc);
			return;
		}
		if (BICHOS) {
			tickBichos(mc);
			return;
		}
		if (QUMARA) {
			tickQumara(mc);
			return;
		}
		if (ECLIPSE_ANIM) {
			tickEclipseAnim(mc);
			return;
		}
		if (LINTERNA_MANO) {
			tickLinternaMano(mc);
			return;
		}
		if (HAZ) {
			tickHaz(mc);
			return;
		}
		if (ECLIPSE) {
			tickEclipse(mc);
			return;
		}
		if (STRAY) {
			tickStray(mc);
			return;
		}
		if (BOGGED) {
			tickBogged(mc);
			return;
		}
		if (ZOMBI_NETHER) {
			tickZombiNether(mc);
			return;
		}
		if (HEROBRINE_NOCHE) {
			tickHerobrineNoche(mc);
			return;
		}
		if (TREX) {
			tickTRex(mc);
			return;
		}
		if (MINIMAPA) {
			tickMinimapa(mc);
			return;
		}
		if (HUEVO_HEROBRINE) {
			tickHuevoHerobrine(mc);
			return;
		}
		if (HEROBRINE) {
			tickHerobrine(mc);
			return;
		}
		if (COBWEB) {
			tickCobweb(mc);
			return;
		}
		if (ZOMBI) {
			tickZombi(mc);
			return;
		}
		if (MIEL) {
			tickMiel(mc);
			return;
		}
		if (CRATER) {
			tickCrater(mc);
			return;
		}
		if (DACTYLO) {
			tickDactylo(mc);
			return;
		}
		if (METEORITO) {
			tickMeteorito(mc);
			return;
		}
		if (SONIDO_CANDADO) {
			tickSonidoCandado(mc);
			return;
		}
		if (CREEPER) {
			tickCreeper(mc);
			return;
		}
		if (BAMBU) {
			tickBambu(mc);
			return;
		}
		if (DILITIO) {
			tickDilitio(mc);
			return;
		}
		if (PUERTA) {
			tickPuerta(mc);
			return;
		}
		if (PILA) {
			tickPila(mc);
			return;
		}
		if (CAJERO) {
			tickCajero(mc);
			return;
		}
		if (SALDO) {
			tickSaldo(mc);
			return;
		}
		if (EFECTOS) {
			tickEfectos(mc);
			return;
		}
		if (TOTEM) {
			tickTotem(mc);
			return;
		}
		if (LINTERNA) {
			tickLinterna(mc);
			return;
		}
		if (TABLON) {
			tickTablon(mc);
			return;
		}
		if (ESTANDARTES) {
			tickEstandartes(mc);
			return;
		}
		if (FRUTAS) {
			tickFrutas(mc);
			return;
		}
		if (ARBOLES) {
			tickArboles(mc);
			return;
		}
		if (EBURIA3) {
			tickEburia3(mc);
			return;
		}
		if (ARANDANO3) {
			tickArandano3(mc);
			return;
		}
		if (UVINA) {
			tickUvina(mc);
			return;
		}
		if (SOLARIA2) {
			tickSolaria2(mc);
			return;
		}
		if (SOLARIA) {
			tickSolaria(mc);
			return;
		}
		if (ARANDANO) {
			tickArandano(mc);
			return;
		}
		if (SEIS) {
			tickSeis(mc);
			return;
		}
		if (PILDORA) {
			tickPildora(mc);
			return;
		}
		if (LLAVE_CANDADO) {
			tickLlaveCandado(mc);
			return;
		}
		if (FRUTA_ESPACIO) {
			tickFrutaEspacio(mc);
			return;
		}
		if (LISTA_HERMANDADES) {
			tickListaHermandades(mc);
			return;
		}
		if (BOLSA) {
			tickBolsa(mc);
			return;
		}
		if (EXCREMENTO) {
			tickExcremento(mc);
			return;
		}
		if (TOOLTIP) {
			tickTooltip(mc);
			return;
		}
		if (CHAT_HERMANDAD) {
			tickChatHermandad(mc);
			return;
		}
		if (QUIU) {
			tickQuiu(mc);
			return;
		}
		if (PLUMO) {
			tickPlumo(mc);
			return;
		}
		if (SEMILLA) {
			tickSemilla(mc);
			return;
		}
		if (LLAVE2) {
			tickLlave2(mc);
			return;
		}
		if (ORE) {
			tickOre(mc);
			return;
		}
		if (LLAVE) {
			tickLlave(mc);
			return;
		}
		if (MONITOR) {
			tickMonitor(mc);
			return;
		}
		if (PIXELES) {
			tickPixeles(mc);
			return;
		}
		if (BLOQUES_COLOR) {
			tickBloquesColor(mc);
			return;
		}
		if (COFRE_CANDADO) {
			tickCofreCandado(mc);
			return;
		}
		if (LAGARTOS) {
			tickLagartos(mc);
			return;
		}
		if (COFRE_NUEVO) {
			tickCofre(mc);
			return;
		}
		if (AYUDA) {
			tickAyuda(mc);
			return;
		}
		if (TOLVA) {
			tickTolva(mc);
			return;
		}
		if (MARCO) {
			tickMarco(mc);
			return;
		}
		if (RULETA) {
			tickRuleta(mc);
			return;
		}
		if (STRUCK) {
			tickStruck(mc);
			return;
		}
		if (CASINO) {
			tickCasino(mc);
			return;
		}
		if (PREMIOS_CASINO) {
			tickPremios(mc);
			return;
		}
		if (PARTES_NAVE) {
			tickPartes(mc);
			return;
		}
		if (SIN_ALMA) {
			tickSinAlma(mc);
			return;
		}
		if (BANEOS) {
			tickBaneos(mc);
			return;
		}
		if (DENTADURA) {
			tickDentadura(mc);
			return;
		}
		if (SACO) {
			tickSaco(mc);
			return;
		}
		if (CARNE_GLEBA) {
			tickCarneGleba(mc);
			return;
		}
		if (ARCO_GLEBA) {
			tickArcoGleba(mc);
			return;
		}
		if (ORGANOS_CORTE) {
			tickOrganosCorte(mc);
			return;
		}
		if (PERLAS2) {
			tickPerlas2(mc);
			return;
		}
		if (BLAZE) {
			tickBlaze(mc);
			return;
		}
		if (MINERALES) {
			tickMinerales(mc);
			return;
		}
		if (AMBAR) {
			tickAmbar(mc);
			return;
		}
		if (EBURIA) {
			tickEburia(mc);
			return;
		}
		if (PORTAL) {
			tickPortal(mc);
			return;
		}
		if (DIMENSION) {
			tickDimension(mc);
			return;
		}
		if (CORAZONES) {
			tickCorazones(mc);
			return;
		}
		if (MAGMA) {
			tickMagma(mc);
			return;
		}
		if (ROBOT) {
			tickRobot(mc);
			return;
		}
		if (GUERRERO) {
			tickGuerrero(mc);
			return;
		}
		if (ARQUERO) {
			tickArquero(mc);
			return;
		}
		if (REPTI) {
			tickRepti(mc);
			return;
		}
		if (CACA) {
			tickCaca(mc);
			return;
		}
		if (WALKER) {
			tickWalker(mc);
			return;
		}
		if (SOARER) {
			tickSoarer(mc);
			return;
		}
		if (CHIP) {
			tickChip(mc);
			return;
		}
		if (MARTILLO) {
			tickMartillo(mc);
			return;
		}
		if (CANDADO) {
			tickCandado(mc);
			return;
		}
		if (DEEPDARK) {
			tickDeepdark(mc);
			return;
		}
		if (BOMBA) {
			tickBomba(mc);
			return;
		}
		if (PERLAS) {
			tickPerlas(mc);
			return;
		}
		if (WARDENS) {
			tickWardens(mc);
			return;
		}
		if (POCIONES) {
			tickPociones(mc);
			return;
		}
		if (NAVE) {
			tickNave(mc);
			return;
		}
		if (MARCAS) {
			tickMarcas(mc);
			return;
		}
		if (MOMENTO) {
			tickMomento(mc);
			return;
		}
		if (ticks == 10 && mc.getSingleplayerServer() != null) {
			var server = mc.getSingleplayerServer();
			var uuid = mc.player.getUUID();
			server.execute(() -> {
				ServerPlayer jugador = server.getPlayerList().getPlayer(uuid);
				if (jugador == null) return;
				jugador.setGameMode(GameType.SPECTATOR);
				jugador.teleportTo(jugador.getX(), 200, jugador.getZ());
			});
		}
		mc.options.hideGui = false;
		mc.options.pauseOnLostFocus = false;
		if (mc.screen != null) mc.setScreen(null);
		int t = ticks - 20;
		mc.player.setYRot(180f);      // norte
		mc.player.setXRot(t < 70 ? -35f : -70f);
		mc.player.yRotO = 180f;
		mc.player.xRotO = t < 70 ? -35f : -70f;


		if (t == 0) CieloCliente.setActivo(true, true);
		if (t == 0) comando(mc, "tiempo 1:05");
		if (t == 30) captura(mc, "debug_t1_temporizador.png");
		if (t == 40) comando(mc, "tiempo 0:04");
		if (t == 60) captura(mc, "debug_t2_ultimo_minuto.png");
		if (t == 122) captura(mc, "debug_t3_terminado.png");
		if (t == 25) captura(mc, "debug_1a_esparce.png");
		if (t == 55) captura(mc, "debug_1b_esparce.png");
		if (t == 130) captura(mc, "debug_2_grieta.png");
		if (t == 190) captura(mc, "debug_3_reviil.png");
		if (t == 260) captura(mc, "debug_4_completo.png");
		if (t == 280) CieloCliente.setActivo(false, true);
		if (t == 283) captura(mc, "debug_5_destello.png");
		if (t == 330) captura(mc, "debug_6_normal.png");
		if (t == 350) mc.stop();
	}

	private static void comando(Minecraft mc, String comando) {
		var server = mc.getSingleplayerServer();
		var uuid = mc.player.getUUID();
		if (server == null) return;
		server.execute(() -> {
			ServerPlayer jugador = server.getPlayerList().getPlayer(uuid);
			if (jugador != null) {
				server.getCommands().performPrefixedCommand(jugador.createCommandSourceStack().withPermission(4), comando);
			}
		});
	}

	private static void captura(Minecraft mc, String nombre) {
		Screenshot.grab(mc.gameDirectory, nombre, mc.getMainRenderTarget(), mensaje -> {});
	}
}
