package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Creeper de Papel: una solicitud de empleo con cara de creeper. Aparece de día en el Overworld. Te persigue y, en
 * cuanto te tiene cerca, "explota" al instante: no rompe nada ni lastima, pero a los que estaban cerca les aparece en
 * pantalla la solicitud de empleo, que hay que firmar y esperar 2 segundos para poder seguir (FormularioPapelScreen).
 * CreeperNuclearMixin cambia su explosión por esto.
 */
public class CreeperPapelEntity extends Creeper {
	/** Hasta dónde llega el "papeleo". */
	private static final double ALCANCE = 4.5;

	/** Servidor → jugador: que le aparezca la solicitud de empleo para firmar. */
	public record FormularioPayload() implements CustomPacketPayload {
		public static final Type<FormularioPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "creeper_papel_formulario"));
		public static final StreamCodec<ByteBuf, FormularioPayload> CODEC = StreamCodec.unit(new FormularioPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public CreeperPapelEntity(EntityType<? extends Creeper> tipo, Level level) {
		super(tipo, level);
		// Mecha de 1 tick: explota apenas empieza a inflarse.
		CompoundTag datos = new CompoundTag();
		datos.putShort("Fuse", (short) 1);
		super.readAdditionalSaveData(datos);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(FormularioPayload.TYPE, FormularioPayload.CODEC);
	}

	/** Aparece de día, a cielo abierto, sobre suelo firme (fuera de pacífico). */
	public static boolean puedeAparecer(EntityType<? extends Mob> tipo, ServerLevelAccessor mundo, MobSpawnType razon,
										BlockPos pos, RandomSource azar) {
		if (mundo.getDifficulty() == Difficulty.PEACEFUL) return false;
		if (razon == MobSpawnType.SPAWNER) return Mob.checkMobSpawnRules(tipo, mundo, razon, pos, azar);
		return mundo.getLevel().isDay() && mundo.canSeeSky(pos) && Mob.checkMobSpawnRules(tipo, mundo, razon, pos, azar);
	}

	/** Con sol también le sirve el lugar (a los monstruos normales la luz no los deja aparecer). */
	@Override
	public float getWalkTargetValue(BlockPos pos, net.minecraft.world.level.LevelReader mundo) {
		return 0;
	}

	/** En vez de explotar: un "puf" de papeles y la solicitud de empleo para los que estaban cerca. */
	public void papeleo(ServerLevel mundo) {
		mundo.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 25, 0.5, 0.6, 0.5, 0.05);
		mundo.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.5), getZ(), 1, 0, 0, 0, 0);
		mundo.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 2f, 0.8f);
		mundo.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.6f);
		for (ServerPlayer jugador : mundo.players()) {
			if (jugador.isSpectator() || jugador.isCreative() || jugador.distanceTo(this) > ALCANCE) continue;
			ServerPlayNetworking.send(jugador, new FormularioPayload());
		}
		this.dead = true;
		this.discard();
	}
}
