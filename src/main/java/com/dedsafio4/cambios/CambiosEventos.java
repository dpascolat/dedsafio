package com.dedsafio4.cambios;

import com.dedsafio4.mixin.CreeperAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;

/** Cambios que se resuelven con eventos de Fabric (los que necesitan mixin están en el paquete mixin). */
public final class CambiosEventos {
	private CambiosEventos() {}

	public static void registrar() {
		// vacas 1: no se puede ordeñar ninguna vaca (incluye champiñacas).
		UseEntityCallback.EVENT.register((jugador, level, mano, entidad, hit) -> {
			if (!(jugador instanceof ServerPlayer servidor) || !(entidad instanceof Cow)) return InteractionResult.PASS;
			if (!jugador.getItemInHand(mano).is(Items.BUCKET)) return InteractionResult.PASS;
			if (Cambios.nivel(servidor.server, Cambios.VACAS) < 1) return InteractionResult.PASS;
			// El cliente ya "predijo" el balde de leche; le reenviamos el inventario real.
			servidor.inventoryMenu.sendAllDataToRemote();
			return InteractionResult.FAIL;
		});

		// wardens 2: cada Warden que muere deja un Chillador de Sculk que puede invocar más Wardens.
		ServerLivingEntityEvents.AFTER_DEATH.register((entidad, fuente) -> {
			if (!(entidad instanceof Warden) || !(entidad.level() instanceof ServerLevel level)) return;
			if (Cambios.nivel(level.getServer(), Cambios.WARDENS) < 2) return;
			dejarChillador(level, entidad.blockPosition());
		});

		// magma 1: tocar un Bloque de Magma de cualquier lado mata al instante.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (Cambios.nivel(server, Cambios.MAGMA) < 1) return;
			for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
				if (jugador.isCreative() || jugador.isSpectator() || !jugador.isAlive()) continue;
				if (!tocaMagma(jugador)) continue;
				jugador.invulnerableTime = 0;
				jugador.hurt(Cambios.danioMagma(jugador.level()), DANIO_MAGMA);
			}
		});

		// miel 1: una botella en un panal (o colmena) ya no saca miel.
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((jugador, level, mano, golpe) -> {
			if (!(jugador instanceof ServerPlayer servidor)) return InteractionResult.PASS;
			if (!jugador.getItemInHand(mano).is(Items.GLASS_BOTTLE)) return InteractionResult.PASS;
			if (!esPanal(level.getBlockState(golpe.getBlockPos()))) return InteractionResult.PASS;
			if (Cambios.nivel(servidor.server, Cambios.MIEL) < 1) return InteractionResult.PASS;
			servidor.inventoryMenu.sendAllDataToRemote();
			return InteractionResult.FAIL;
		});

		// cobweb 1: al romper una telaraña, 33% de que salga una araña venenosa (araña de cueva).
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, jugador, pos, estado, entidad) -> {
			if (!(level instanceof ServerLevel mundo) || !estado.is(Blocks.COBWEB)) return;
			if (Cambios.nivel(mundo.getServer(), Cambios.COBWEB) < 1 || mundo.getRandom().nextFloat() >= 0.33f) return;
			net.minecraft.world.entity.EntityType.CAVE_SPIDER.spawn(mundo, pos, net.minecraft.world.entity.MobSpawnType.EVENT);
		});

		// miel 1: romper un panal (o colmena) hace la explosión de un Creeper.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, jugador, pos, estado, entidad) -> {
			if (!(level instanceof ServerLevel mundo) || !esPanal(estado)) return;
			if (Cambios.nivel(mundo.getServer(), Cambios.MIEL) < 1) return;
			mundo.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0f, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
		});

		// creepers_pasto 1: al romper pasto (la planta o el bloque) puede salir un Creeper de Pasto.
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, jugador, pos, estado, entidad) -> {
			if (level instanceof ServerLevel mundo && com.dedsafio4.bestias.CreeperPastoEntity.esPasto(estado)) {
				com.dedsafio4.bestias.CreeperPastoEntity.quizasAparecer(mundo, pos);
			}
		});

		// creepers electricos 1: todo Creeper que aparece (o que se carga con el trozo de mundo) es eléctrico.
		// zombi 1 y 2: cada zombi que aparece decide si escala y (en nivel 2) recibe su espada.
		ServerEntityEvents.ENTITY_LOAD.register((entidad, level) -> {
			if (Zombis.esZombi(entidad)) Zombis.preparar((net.minecraft.world.entity.monster.Zombie) entidad, level);
		});

		ServerEntityEvents.ENTITY_LOAD.register((entidad, level) -> {
			if (!(entidad instanceof Creeper creeper)) return;
			if (Cambios.nivel(level.getServer(), Cambios.CREEPERS_ELECTRICOS) >= 1) electrificar(creeper);
		});
	}

	/** Panal de abejas o colmena. */
	private static boolean esPanal(BlockState estado) {
		return estado.is(Blocks.BEE_NEST) || estado.is(Blocks.BEEHIVE);
	}

	/** Un trillón de daño: no hay forma de sobrevivirlo. */
	private static final float DANIO_MAGMA = 1.0e12f;

	/** ¿El jugador está tocando un Bloque de Magma? Cuenta de arriba, de abajo y de los costados. */
	private static boolean tocaMagma(ServerPlayer jugador) {
		AABB caja = jugador.getBoundingBox().inflate(0.05);
		for (BlockPos pos : BlockPos.betweenClosed(
				BlockPos.containing(caja.minX, caja.minY, caja.minZ),
				BlockPos.containing(caja.maxX, caja.maxY, caja.maxZ))) {
			if (jugador.level().getBlockState(pos).is(Blocks.MAGMA_BLOCK)) return true;
		}
		return false;
	}

	/** Al activar el cambio, los Creepers que ya estaban en el mundo también se vuelven eléctricos. */
	public static void alActivar(MinecraftServer server, String cambio, int nivel) {
		if (Cambios.HEROBRINE.equals(cambio) && nivel >= 1) com.dedsafio4.herobrine.ModHerobrine.alActivar(server);
		// Los zombis que ya estaban en el mundo también.
		if (Cambios.ZOMBI.equals(cambio) && nivel >= 1) {
			for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
				for (Entity entidad : level.getAllEntities()) {
					if (Zombis.esZombi(entidad)) Zombis.preparar((net.minecraft.world.entity.monster.Zombie) entidad, level);
				}
			}
		}
		if (!Cambios.CREEPERS_ELECTRICOS.equals(cambio) || nivel < 1) return;
		for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
			for (Entity entidad : level.getAllEntities()) {
				if (entidad instanceof Creeper creeper) electrificar(creeper);
			}
		}
	}

	private static void electrificar(Creeper creeper) {
		if (!creeper.isPowered()) creeper.getEntityData().set(CreeperAccessor.dedsafio4$powered(), true);
	}

	/** Pone un Chillador de Sculk donde cayó el Warden, sobre una capa de sculk. */
	private static void dejarChillador(ServerLevel level, BlockPos donde) {
		BlockPos pos = null;
		// Busca desde los pies hacia arriba un lugar libre que tenga suelo debajo.
		for (int alto = 0; alto <= 2 && pos == null; alto++) {
			BlockPos candidato = donde.above(alto);
			if (!level.getBlockState(candidato).canBeReplaced()) continue;
			if (!level.getBlockState(candidato.below()).isFaceSturdy(level, candidato.below(), Direction.UP)) continue;
			pos = candidato;
		}
		if (pos == null) return;
		BlockState chillador = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true);
		level.setBlockAndUpdate(pos, chillador);
		if (level.getBlockState(pos.below()).is(Blocks.SCULK_SHRIEKER)) return;
		level.setBlockAndUpdate(pos.below(), Blocks.SCULK.defaultBlockState());
		level.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 1.5f, 1f);
	}
}
