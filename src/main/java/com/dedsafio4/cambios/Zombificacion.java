package com.dedsafio4.cambios;

import com.dedsafio4.items.PildoraItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * zombificacion 1: todos necesitan Zombificación (el efecto Protección contra Zombificación) para vivir. Al activar el
 * cambio, cada jugador (los conectados y los que entren después, una sola vez) recibe 20 minutos; la Píldora da 2
 * horas; cuando salta un tótem da 5 minutos (y también al reaparecer, para no morir en seguida otra vez). Al que se le
 * acaba, lo mata pase lo que pase (el golpe no lo frena la armadura ni nada), salvo que tenga un tótem.
 */
public final class Zombificacion {
	private Zombificacion() {}

	private static final int MINUTO = 60 * 20;
	public static final int AL_EMPEZAR = 20 * MINUTO, TRAS_TOTEM = 5 * MINUTO;
	private static final float GOLPE = 1000f;

	public static final ResourceKey<DamageType> DANIO = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "zombificacion"));

	/** Los que ya recibieron sus 20 minutos (se vacía al sacar el cambio). */
	static final class Datos extends SavedData {
		static final Factory<Datos> FACTORY = new Factory<>(Datos::new, Datos::leer, null);
		final Set<UUID> recibieron = new HashSet<>();

		private static Datos leer(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			for (Tag t : tag.getList("recibieron", Tag.TAG_INT_ARRAY)) d.recibieron.add(NbtUtils.loadUUID(t));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (UUID u : recibieron) l.add(NbtUtils.createUUID(u));
			tag.put("recibieron", l);
			return tag;
		}
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "dedsafio4_zombificacion");
	}

	/** A los que les saltó un tótem en este tick: al final del tick reciben sus 5 minutos (después de que el tótem saque los efectos). */
	private static final Set<UUID> TRAS_TOTEM_PENDIENTES = new HashSet<>();

	public static boolean activo(MinecraftServer server) {
		return Cambios.nivel(server, Cambios.ZOMBIFICACION) >= 1;
	}

	/** Lo llama AvisoTotem cada vez que a alguien le salta un tótem (cualquiera). */
	public static void saltoTotem(ServerPlayer jugador) {
		if (activo(jugador.server)) TRAS_TOTEM_PENDIENTES.add(jugador.getUUID());
	}

	/** Le da (o le estira hasta) esa cantidad de Zombificación. */
	public static void dar(ServerPlayer jugador, int ticks) {
		MobEffectInstance antes = jugador.getEffect(PildoraItem.PROTECCION_ZOMBIFICACION);
		if (antes != null && antes.getDuration() >= ticks) return;
		jugador.addEffect(new MobEffectInstance(PildoraItem.PROTECCION_ZOMBIFICACION, ticks, 0, false, true, true));
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!activo(server)) {
				TRAS_TOTEM_PENDIENTES.clear();
				if (server.getTickCount() % 100 == 0) {
					Datos d = datos(server);
					if (!d.recibieron.isEmpty()) {
						d.recibieron.clear();
						d.setDirty();
					}
				}
				return;
			}
			for (UUID u : TRAS_TOTEM_PENDIENTES) {
				ServerPlayer p = server.getPlayerList().getPlayer(u);
				if (p != null && p.isAlive()) dar(p, TRAS_TOTEM);
			}
			TRAS_TOTEM_PENDIENTES.clear();
			if (server.getTickCount() % 20 != 0) return;
			Datos d = datos(server);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
				// Los 20 minutos del principio (también a los que entran después, una vez).
				if (d.recibieron.add(p.getUUID())) {
					d.setDirty();
					dar(p, AL_EMPEZAR);
					p.sendSystemMessage(Component.literal("Recibiste 20 minutos de Zombificación. Si se te acaba, mueres: consume una Píldora.")
							.withColor(0xB86BD8));
					continue;
				}
				if (p.hasEffect(PildoraItem.PROTECCION_ZOMBIFICACION)) continue;
				// Sin Zombificación: muere (sólo un tótem lo salva).
				p.invulnerableTime = 0;
				p.hurt(new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO)), GOLPE);
			}
		});
		// Al reaparecer, 5 minutos (si no, se volvería a morir en seguida).
		ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> {
			if (activo(nuevo.server)) dar(nuevo, TRAS_TOTEM);
		});
	}
}
