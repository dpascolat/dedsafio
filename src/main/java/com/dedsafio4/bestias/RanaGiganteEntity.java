package com.dedsafio4.bestias;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Rana Gigante (de Dedsafío 3): agarra con la lengua al jugador que se le acerca (a 5 bloques) y lo deja quieto
 * delante de ella. Para soltarse hay que apretar espacio 100 veces (sale una barra en pantalla) antes de 10 segundos;
 * si no, se lo traga. Tragado, solo sale si otro jugador se acerca a la rana: entonces sale disparado 30 bloques
 * para arriba. Después de soltar a alguien espera 5 segundos antes de volver a agarrar. /rana salir (admins) suelta.
 */
public class RanaGiganteEntity extends Monster implements GeoEntity {
	public static final int ESPACIOS_PARA_SALIR = 100;

	private static final double ALCANCE = 5.0;
	private static final int ESPERA = 100, TIEMPO_PARA_SALIR = 200;
	private static final double ALTURA_DISPARO = 30.0;

	private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

	private UUID agarrado;
	private int espacios, espera, tiempo;
	private boolean tragado;

	/** Servidor → jugador: si una Rana Gigante lo tiene agarrado y si ya se lo tragó (para la barra). */
	public record AgarrePayload(boolean agarrado, boolean tragado) implements CustomPacketPayload {
		public static final Type<AgarrePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "rana_gigante_agarre"));
		public static final StreamCodec<ByteBuf, AgarrePayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, AgarrePayload::agarrado, ByteBufCodecs.BOOL, AgarrePayload::tragado, AgarrePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Jugador → servidor: apretó espacio una vez mientras lo tienen agarrado. */
	public record EspacioPayload() implements CustomPacketPayload {
		public static final Type<EspacioPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "rana_gigante_espacio"));
		public static final StreamCodec<ByteBuf, EspacioPayload> CODEC = StreamCodec.unit(new EspacioPayload());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public RanaGiganteEntity(EntityType<? extends RanaGiganteEntity> tipo, Level level) {
		super(tipo, level);
	}

	public static void registrar() {
		PayloadTypeRegistry.playS2C().register(AgarrePayload.TYPE, AgarrePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EspacioPayload.TYPE, EspacioPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(EspacioPayload.TYPE, (payload, contexto) -> contexto.server().execute(() -> {
			ServerPlayer jugador = contexto.player();
			for (RanaGiganteEntity rana : jugador.serverLevel().getEntitiesOfClass(RanaGiganteEntity.class,
					jugador.getBoundingBox().inflate(16))) {
				rana.alApretarEspacio(jugador);
			}
		}));
	}

	/** /rana salir: un admin se suelta de la Rana Gigante que lo tenga agarrado. */
	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("rana").requires(s -> s.hasPermission(2))
				.then(Commands.literal("salir").executes(c -> {
					ServerPlayer jugador = c.getSource().getPlayerOrException();
					for (RanaGiganteEntity rana : jugador.serverLevel().getEntitiesOfClass(RanaGiganteEntity.class,
							jugador.getBoundingBox().inflate(64))) {
						if (rana.soltarSiLoTiene(jugador.getUUID())) {
							c.getSource().sendSuccess(() -> Component.literal("Te soltaste de la Rana Gigante."), true);
							return 1;
						}
					}
					c.getSource().sendFailure(Component.literal("Ninguna Rana Gigante te tiene agarrado ahora."));
					return 0;
				})));
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) return;
		if (espera > 0) espera--;
		if (agarrado != null) sostener();
		else if (espera <= 0) agarrarCercano();
	}

	private void agarrarCercano() {
		for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(ALCANCE))) {
			if (!(p instanceof ServerPlayer jugador) || p.isSpectator() || p.isCreative()) continue;
			agarrado = jugador.getUUID();
			espacios = 0;
			tiempo = 0;
			tragado = false;
			ServerPlayNetworking.send(jugador, new AgarrePayload(true, false));
			return;
		}
	}

	private void sostener() {
		ServerPlayer jugador = buscarAgarrado();
		if (jugador == null || !jugador.isAlive()) {
			soltar(null);
			return;
		}
		// Delante de la boca (agarrado con la lengua) o adentro (tragado).
		Vec3 donde = tragado ? this.position().add(0, 0.9, 0) : this.position().add(this.getLookAngle().scale(1.2)).add(0, 0.5, 0);
		if (this.level() instanceof ServerLevel mundo) {
			jugador.teleportTo(mundo, donde.x, donde.y, donde.z, jugador.getYRot(), jugador.getXRot());
		}
		jugador.setDeltaMovement(Vec3.ZERO);
		jugador.fallDistance = 0;
		if (tragado) {
			quizasEscupir(jugador);
			return;
		}
		tiempo++;
		if (tiempo >= TIEMPO_PARA_SALIR && espacios < ESPACIOS_PARA_SALIR) {
			tragado = true;
			ServerPlayNetworking.send(jugador, new AgarrePayload(true, true));
			return;
		}
		if (espacios >= ESPACIOS_PARA_SALIR) soltar(jugador);
	}

	/** Tragado: sale disparado para arriba cuando otro jugador se acerca a la rana. */
	private void quizasEscupir(ServerPlayer tragadoJugador) {
		if (!(this.level() instanceof ServerLevel mundo)) return;
		for (Player otro : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(ALCANCE))) {
			if (otro == tragadoJugador || otro.isSpectator() || otro.isCreative()) continue;
			Vec3 pos = tragadoJugador.position();
			tragadoJugador.teleportTo(mundo, pos.x, pos.y + ALTURA_DISPARO, pos.z, tragadoJugador.getYRot(), tragadoJugador.getXRot());
			soltar(tragadoJugador);
			return;
		}
	}

	/** Suelta al jugador si es el que tiene agarrado (para /rana salir). */
	public boolean soltarSiLoTiene(UUID jugador) {
		if (!jugador.equals(agarrado)) return false;
		soltar(null);
		return true;
	}

	public void alApretarEspacio(ServerPlayer jugador) {
		if (!tragado && jugador.getUUID().equals(agarrado)) espacios++;
	}

	private ServerPlayer buscarAgarrado() {
		if (agarrado == null || !(this.level() instanceof ServerLevel mundo)) return null;
		return mundo.getServer().getPlayerList().getPlayer(agarrado);
	}

	private void soltar(ServerPlayer conocido) {
		ServerPlayer jugador = conocido != null ? conocido : buscarAgarrado();
		if (jugador != null) {
			ServerPlayNetworking.send(jugador, new AgarrePayload(false, false));
			Vec3 afuera = jugador.position().subtract(this.position());
			afuera = afuera.lengthSqr() > 0 ? afuera.normalize() : new Vec3(0, 0, 1);
			jugador.setDeltaMovement(afuera.x * 1.5, 0.3, afuera.z * 1.5);
			jugador.hurtMarked = true;
		}
		agarrado = null;
		espacios = 0;
		tiempo = 0;
		tragado = false;
		espera = ESPERA;
	}

	@Override
	public void remove(RemovalReason razon) {
		if (agarrado != null) soltar(null);
		super.remove(razon);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
		controladores.add(new AnimationController<>(this, "movimiento", 4, estado -> estado.isMoving()
				? estado.setAndContinue(RawAnimation.begin().thenLoop("walk"))
				: estado.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animaciones;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.FROG_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.FROG_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.FROG_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5f;   // voz grave: es enorme
	}
}
