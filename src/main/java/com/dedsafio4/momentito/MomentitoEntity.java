package com.dedsafio4.momentito;

import com.dedsafio4.Dedsafio4;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * /momentito 1 [radio]  la escena del hacker (30 segundos) en el lugar donde se escribe el comando, mirando
 * hacia donde mira el que lo escribe (el campo de fuerza mide radio × 2 bloques; si no se dice, 750 = 1500 × 1500): llega la nave negra del hacker, tira una TNT hackeada, el héroe busca el
 * Cristal del Desierto y lo levanta, aparece el campo de fuerza violeta, la bomba explota contra el campo,
 * el hacker se enoja y se va volando al cielo. Todo es de mentira: no rompe nada ni lastima a nadie.
 * La escena la dibuja el cliente (MomentitoRenderer) y, a los que están cerca, les maneja la cámara con
 * tomas de cine (MomentitoCamara); acá solo vive 30 segundos y hace los sonidos.
 */
public class MomentitoEntity extends Entity {
	public static final int DURACION = 30 * 20;

	public static final EntityType<MomentitoEntity> TIPO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, "momentito"),
			EntityType.Builder.<MomentitoEntity>of(MomentitoEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
					.clientTrackingRange(16).updateInterval(20).build("momentito"));

	/** Tick del mundo en que empezó (así todos los que la ven van sincronizados). */
	private static final EntityDataAccessor<Integer> INICIO = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.INT);
	/** Radio del campo de fuerza, en bloques. */
	private static final EntityDataAccessor<Float> RADIO = SynchedEntityData.defineId(MomentitoEntity.class, EntityDataSerializers.FLOAT);
	public static final float RADIO_POR_DEFECTO = 750;

	public MomentitoEntity(EntityType<? extends MomentitoEntity> tipo, Level level) {
		super(tipo, level);
		noPhysics = true;
	}

	public static void registrar() {}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("momentito").requires(s -> s.hasPermission(2))
				.then(Commands.argument("escena", IntegerArgumentType.integer(1))
						.executes(ctx -> empezar(ctx, RADIO_POR_DEFECTO))
						.then(Commands.argument("radio", IntegerArgumentType.integer(3, 750))
								.executes(ctx -> empezar(ctx, IntegerArgumentType.getInteger(ctx, "radio"))))));
	}

	private static int empezar(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, float radio) {
		int escena = IntegerArgumentType.getInteger(ctx, "escena");
		if (escena != 1) {
			ctx.getSource().sendFailure(Component.literal("Todavía no existe el momentito " + escena + "."));
			return 0;
		}
		ServerLevel mundo = ctx.getSource().getLevel();
		Vec3 pos = ctx.getSource().getPosition();
		MomentitoEntity m = TIPO.create(mundo);
		if (m == null) return 0;
		m.moveTo(pos.x, pos.y, pos.z, ctx.getSource().getRotation().y, 0);
		m.entityData.set(INICIO, (int) mundo.getGameTime());
		m.entityData.set(RADIO, radio);
		mundo.addFreshEntity(m);
		int lado = Math.round(radio * 2);
		ctx.getSource().sendSuccess(() -> Component.literal("Momentito 1: la escena del hacker (campo de "
				+ lado + " × " + lado + " bloques)."), true);
		return 1;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(INICIO, 0);
		datos.define(RADIO, RADIO_POR_DEFECTO);
	}

	public float radio() {
		return entityData.get(RADIO);
	}

	/** La escena es enorme: se dibuja desde cualquier distancia. */
	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return true;
	}

	/** Segundos desde que empezó la escena. */
	public float tiempo(float parcial) {
		return (level().getGameTime() - entityData.get(INICIO) + parcial) / 20f;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) return;
		int t = (int) (level().getGameTime() - entityData.get(INICIO));
		if (t >= DURACION) {
			discard();
			return;
		}
		switch (t) {
			case 0 -> sonar(SoundEvents.BEACON_AMBIENT, 2f, 0.5f);
			case 60 -> sonar(SoundEvents.BEACON_AMBIENT, 2f, 0.6f);
			case 132 -> sonar(SoundEvents.STONE_BUTTON_CLICK_ON, 1.5f, 1.2f);
			case 148 -> sonar(SoundEvents.TNT_PRIMED, 2f, 1f);
			case 150 -> sonar(SoundEvents.WITCH_CELEBRATE, 1.5f, 0.7f);
			case 212 -> sonar(SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 1f);
			case 224 -> sonar(SoundEvents.BEACON_ACTIVATE, 2f, 1.2f);
			case 240 -> {
				sonar(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.8f);
				sonar(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
			}
			case 290, 310 -> sonar(SoundEvents.VILLAGER_NO, 2f, 0.6f);
			case 340 -> sonar(SoundEvents.FIREWORK_ROCKET_LAUNCH, 4f, 0.5f);
			case 490 -> sonar(SoundEvents.BEACON_DEACTIVATE, 2f, 1f);
			default -> {
			}
		}
	}

	private void sonar(SoundEvent sonido, float volumen, float tono) {
		level().playSound(null, getX(), getY() + 3, getZ(), sonido, SoundSource.AMBIENT, volumen, tono);
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag datos) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag datos) {
	}
}
