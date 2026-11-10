package com.dedsafio4.bloques;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bloque Rojo Mortal (bloque_rojo_mortal; "bloque_rojo" es otro, el de colores): un bloque rojo transparente que se puede atravesar. El que está adentro recibe un golpe que lo mata al
 * instante (solo lo salva un tótem, que salta) y, si sigue adentro, otro cada 20 ticks (1 segundo).
 * No le hace nada a los que están en creativo o espectador.
 */
public class BloqueRojo extends Block {
	/** Cada cuántos ticks vuelve a golpear al que sigue adentro. */
	public static final int CADA = 20;
	/** Un golpe mágico enorme: no lo frena la armadura y mata a cualquiera, pero el tótem sí salta. */
	private static final float GOLPE = 1000f;

	/** El último golpe a cada uno (tiempo del juego), para no pegarle dos veces si toca varios bloques a la vez. */
	private static final Map<Entity, Long> ULTIMO = new WeakHashMap<>();

	public static final Block BLOQUE = Registry.register(BuiltInRegistries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bloque_rojo_mortal"),
			new BloqueRojo(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().noOcclusion()
					.strength(1f).sound(SoundType.GLASS).isSuffocating((e, m, p) -> false).isViewBlocking((e, m, p) -> false)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "bloque_rojo_mortal"), new BlockItem(BLOQUE, new Item.Properties()));

	public BloqueRojo(Properties propiedades) {
		super(propiedades);
	}

	/**
	 * Los jugadores quietos no "tocan" los bloques en el servidor (eso pasa solo cuando se mueven), así que cada tick
	 * se mira si alguno está metido en un Bloque Rojo Mortal. A los mobs los avisa entityInside.
	 */
	public static void registrar() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (net.minecraft.server.level.ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (!jugador.isAlive() || jugador.isCreative() || jugador.isSpectator()) continue;
				var caja = jugador.getBoundingBox().deflate(1.0E-5);
				for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(caja.minX, caja.minY, caja.minZ),
						BlockPos.containing(caja.maxX, caja.maxY, caja.maxZ))) {
					if (jugador.level().getBlockState(pos).is(BLOQUE)) {
						golpear(jugador, jugador.level());
						break;
					}
				}
			}
		});
	}

	@Override
	protected void entityInside(BlockState estado, Level mundo, BlockPos pos, Entity entidad) {
		if (!(mundo instanceof ServerLevel) || !(entidad instanceof LivingEntity vivo) || !vivo.isAlive()) return;
		if (vivo instanceof Player) return;   // a los jugadores los mira registrar(), cada tick
		golpear(vivo, mundo);
	}

	/** Golpe que mata (salta el tótem), como mucho uno cada 20 ticks por cada uno. */
	private static void golpear(LivingEntity vivo, Level mundo) {
		long ahora = mundo.getGameTime();
		Long antes = ULTIMO.get(vivo);
		if (antes != null && ahora - antes < CADA) return;
		vivo.invulnerableTime = 0;
		// Solo cuenta si el golpe entró (por ejemplo, recién reaparecido el jugador es invulnerable 3 segundos).
		if (vivo.hurt(vivo.damageSources().magic(), GOLPE)) ULTIMO.put(vivo, ahora);
	}
}
