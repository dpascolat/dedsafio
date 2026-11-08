package com.dedsafio4.cambios;

import com.dedsafio4.despegue.NaveViajeEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * gas_toxico 1: en el Overworld, estar por encima de la capa 150 lastima hasta morir (gas_toxico 2: de la capa 130). No lastima adentro de una Nave
 * Biplaza ni en la Montaña, un rectángulo que se marca con /montana <desde> <hasta> (/montana ver, /montana quitar).
 * También se puede escribir /montaña.
 */
public final class GasToxico {
	private GasToxico() {}

	private static final int CAPA = 150, CAPA_NIVEL_2 = 130;
	/** Dos corazones por segundo: con menos, la vida que recupera un jugador bien comido casi lo compensa. */
	private static final float DANIO = 4;
	private static final int CADA = 20;

	public static final ResourceKey<DamageType> DANIO_GAS = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "gas_toxico"));

	private static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		boolean hay;
		int minX, minZ, maxX, maxZ;

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			d.hay = tag.getBoolean("hay");
			d.minX = tag.getInt("minX");
			d.minZ = tag.getInt("minZ");
			d.maxX = tag.getInt("maxX");
			d.maxZ = tag.getInt("maxZ");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("hay", hay);
			tag.putInt("minX", minX);
			tag.putInt("minZ", minZ);
			tag.putInt("maxX", maxX);
			tag.putInt("maxZ", maxZ);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_montana");
	}

	/** ¿Está ese punto del Overworld adentro de la Montaña? */
	public static boolean enMontana(ServerLevel mundo, double x, double z) {
		if (mundo.dimension() != Level.OVERWORLD) return false;
		Datos d = datos(mundo.getServer());
		return d.hay && x >= d.minX && x < d.maxX + 1 && z >= d.minZ && z < d.maxZ + 1;
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CADA != 0 || Cambios.nivel(server, Cambios.GAS_TOXICO) < 1) return;
			ServerLevel mundo = server.overworld();
			int capa = Cambios.nivel(server, Cambios.GAS_TOXICO) >= 2 ? CAPA_NIVEL_2 : CAPA;
			for (ServerPlayer jugador : mundo.players()) {
				if (jugador.isCreative() || jugador.isSpectator() || !jugador.isAlive() || jugador.getY() <= capa) continue;
				if (jugador.getVehicle() instanceof NaveViajeEntity || enMontana(mundo, jugador.getX(), jugador.getZ())) continue;
				jugador.hurt(new DamageSource(mundo.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO_GAS)), DANIO);
				mundo.sendParticles(ParticleTypes.SNEEZE, jugador.getX(), jugador.getY() + 1, jugador.getZ(), 8, 0.5, 0.6, 0.5, 0.01);
				jugador.displayClientMessage(Component.literal("¡Gas tóxico! Baja de la capa " + capa + ".").withStyle(ChatFormatting.GREEN), true);
			}
		});
	}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		var montana = dispatcher.register(Commands.literal("montana").requires(s -> s.hasPermission(2))
				.then(Commands.literal("ver").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal(d.hay
							? "La Montaña va de " + d.minX + " " + d.minZ + " a " + d.maxX + " " + d.maxZ + "."
							: "La Montaña no está marcada.").withStyle(ChatFormatting.GREEN), false);
					return d.hay ? 1 : 0;
				}))
				.then(Commands.literal("quitar").executes(c -> {
					Datos d = datos(c.getSource().getServer());
					d.hay = false;
					d.setDirty();
					c.getSource().sendSuccess(() -> Component.literal("Listo: la Montaña ya no está marcada.").withStyle(ChatFormatting.GREEN), true);
					return 1;
				}))
				.then(Commands.argument("desde", BlockPosArgument.blockPos())
						.then(Commands.argument("hasta", BlockPosArgument.blockPos()).executes(c -> {
							BlockPos a = BlockPosArgument.getBlockPos(c, "desde"), b = BlockPosArgument.getBlockPos(c, "hasta");
							Datos d = datos(c.getSource().getServer());
							d.hay = true;
							d.minX = Math.min(a.getX(), b.getX());
							d.maxX = Math.max(a.getX(), b.getX());
							d.minZ = Math.min(a.getZ(), b.getZ());
							d.maxZ = Math.max(a.getZ(), b.getZ());
							d.setDirty();
							c.getSource().sendSuccess(() -> Component.literal("Listo: la Montaña va de " + d.minX + " " + d.minZ
									+ " a " + d.maxX + " " + d.maxZ + ". Ahí no llega el gas.").withStyle(ChatFormatting.GREEN), true);
							return 1;
						}))));
		// /montaña (con ñ) hace lo mismo que /montana.
		dispatcher.register(Commands.literal("montaña").requires(s -> s.hasPermission(2)).redirect(montana));
	}
}
