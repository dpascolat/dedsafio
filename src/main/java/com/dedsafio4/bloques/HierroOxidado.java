package com.dedsafio4.bloques;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bloque de Hierro Oxidado: no se rompe con nada (como la bedrock, tampoco con explosiones normales ni pistones).
 * Solo lo destruye la explosión de un Creeper Nuclear: todos los que haya a menos de 50 bloques se rompen en
 * cadena, primero los más cercanos y después los más lejos, como una onda.
 */
public final class HierroOxidado {
	private HierroOxidado() {}

	/** Hasta dónde llega la cadena desde la explosión. */
	public static final double RADIO = 50;
	/** Cuántos bloques avanza la cadena por tick. */
	private static final double VELOCIDAD = 2;

	public static final Block BLOQUE = Registry.register(BuiltInRegistries.BLOCK, id("hierro_oxidado"),
			new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(-1f, 3_600_000f)
					.noLootTable().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)
					.isValidSpawn((estado, mundo, pos, tipo) -> false)));
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, id("hierro_oxidado"),
			new BlockItem(BLOQUE, new Item.Properties()) {
				@Override
				public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
					texto.add(Component.literal("Nada puede romperlo...").withColor(0xC98A5A));
					texto.add(Component.literal("salvo una explosión nuclear.").withColor(0xC98A5A));
				}
			});

	/** Un bloque que se va a romper en la cadena, y en qué tick. */
	private record Pendiente(ServerLevel mundo, BlockPos pos, long tick) {}

	private static final List<Pendiente> PENDIENTES = new ArrayList<>();

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	public static void registrar() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDIENTES.isEmpty()) return;
			long ahora = server.getTickCount();
			int sonidos = 0;
			for (var it = PENDIENTES.iterator(); it.hasNext(); ) {
				Pendiente p = it.next();
				if (p.tick() > ahora) continue;
				it.remove();
				if (!p.mundo().getBlockState(p.pos()).is(BLOQUE)) continue;
				p.mundo().destroyBlock(p.pos(), false);
				p.mundo().sendParticles(ParticleTypes.LARGE_SMOKE, p.pos().getX() + 0.5, p.pos().getY() + 0.5, p.pos().getZ() + 0.5,
						3, 0.3, 0.3, 0.3, 0.02);
				// Unos pocos sonidos por tick, para que la cadena se escuche sin saturar.
				if (sonidos++ < 3) p.mundo().playSound(null, p.pos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f,
						0.6f + p.mundo().random.nextFloat() * 0.3f);
			}
		});
	}

	/** El Creeper Nuclear explotó en "centro": arranca la cadena con todos los bloques a menos de RADIO. */
	public static void explosion(ServerLevel mundo, Vec3 centro) {
		List<BlockPos> encontrados = new ArrayList<>();
		int r = (int) Math.ceil(RADIO);
		BlockPos medio = BlockPos.containing(centro);
		int cx0 = SectionPos.blockToSectionCoord(medio.getX() - r), cx1 = SectionPos.blockToSectionCoord(medio.getX() + r);
		int cz0 = SectionPos.blockToSectionCoord(medio.getZ() - r), cz1 = SectionPos.blockToSectionCoord(medio.getZ() + r);
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				LevelChunk pedazo = mundo.getChunkSource().getChunkNow(cx, cz);
				if (pedazo == null) continue;
				LevelChunkSection[] secciones = pedazo.getSections();
				for (int i = 0; i < secciones.length; i++) {
					LevelChunkSection seccion = secciones[i];
					// Se salta las secciones que seguro no tienen el bloque (sin mirar bloque por bloque).
					if (seccion.hasOnlyAir() || !seccion.maybeHas(estado -> estado.is(BLOQUE))) continue;
					int y0 = SectionPos.sectionToBlockCoord(pedazo.getSectionYFromSectionIndex(i));
					if (y0 + 15 < medio.getY() - r || y0 > medio.getY() + r) continue;
					ChunkPos cp = pedazo.getPos();
					for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
						if (!seccion.getBlockState(x, y, z).is(BLOQUE)) continue;
						BlockPos pos = new BlockPos(cp.getMinBlockX() + x, y0 + y, cp.getMinBlockZ() + z);
						if (Vec3.atCenterOf(pos).distanceTo(centro) <= RADIO) encontrados.add(pos);
					}
				}
			}
		}
		if (encontrados.isEmpty()) return;
		encontrados.sort(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(centro)));
		long ahora = mundo.getServer().getTickCount();
		for (BlockPos pos : encontrados) {
			long demora = 1 + (long) (Vec3.atCenterOf(pos).distanceTo(centro) / VELOCIDAD);
			PENDIENTES.add(new Pendiente(mundo, pos, ahora + demora));
		}
	}
}
