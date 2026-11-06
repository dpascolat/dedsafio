package com.dedsafio4.bestias;

import com.dedsafio4.nave.ModEntidades;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;

/**
 * Almita: el alma de un jugador (Steve como fantasma ángel, cian y transparente, con aureola y alas). No ataca: solo
 * flota rápido por ahí.
 * /almita <nombre> → le pone ese nombre arriba a la Almita más cercana (a 8 bloques); si no hay ninguna, aparece una
 * nueva donde estás, con ese nombre.
 */
public class AlmitaEntity extends PathfinderMob {
	public AlmitaEntity(EntityType<? extends AlmitaEntity> tipo, Level mundo) {
		super(tipo, mundo);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder crearAtributos() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.4)
				.add(Attributes.FLYING_SPEED, 0.9);
	}

	@Override
	protected PathNavigation createNavigation(Level mundo) {
		FlyingPathNavigation navegacion = new FlyingPathNavigation(this, mundo);
		navegacion.setCanFloat(true);
		navegacion.setCanPassDoors(true);
		return navegacion;
	}

	@Override
	protected void registerGoals() {
		// Flota rápido de un lado a otro; mira a los jugadores; nunca ataca.
		goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.4));
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	// --- Las Almitas del Limbo (AlmitasLimbo): la de un jugador, con su nombre, que no desaparece ---

	/** De quién es (null si es una Almita común, de /almita). */
	private java.util.UUID dueno;
	/** El lugar del Limbo de donde salió: si se aleja mucho, vuelve. */
	private BlockPos casa;

	public java.util.UUID dueno() {
		return dueno;
	}

	/** Pasa a ser el alma de ese jugador: con su nombre arriba, sin desaparecer nunca. */
	public void serDe(ServerPlayer jugador, BlockPos lugar) {
		dueno = jugador.getUUID();
		casa = lugar.immutable();
		setCustomName(Component.literal(jugador.getGameProfile().getName()).withStyle(ChatFormatting.AQUA));
		setCustomNameVisible(true);
		setPersistenceRequired();
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (dueno != null) tag.putUUID("dueno", dueno);
		if (casa != null) tag.putLong("casa", casa.asLong());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("dueno")) dueno = tag.getUUID("dueno");
		if (tag.contains("casa")) casa = BlockPos.of(tag.getLong("casa"));
	}

	/** La de un jugador no se puede lastimar (sólo /kill). */
	@Override
	public boolean isInvulnerableTo(DamageSource fuente) {
		if (dueno != null && !fuente.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
		return super.isInvulnerableTo(fuente);
	}

	@Override
	public void tick() {
		super.tick();
		// Si se alejó mucho de su lugar, vuelve volando.
		if (!level().isClientSide && casa != null && tickCount % 20 == 0 && distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(casa)) > 12 * 12) {
			getNavigation().moveTo(casa.getX() + 0.5, casa.getY() + 1, casa.getZ() + 0.5, 1.2);
		}
	}

	/** Clic derecho con el Recolector de Almas lleno, en tu propia Almita: recuperas tu alma para revivir. */
	@Override
	protected net.minecraft.world.InteractionResult mobInteract(Player jugador, net.minecraft.world.InteractionHand mano) {
		net.minecraft.world.item.ItemStack pila = jugador.getItemInHand(mano);
		if (dueno == null || !pila.is(com.dedsafio4.items.ModItems.RECOLECTOR_ALMAS)) return super.mobInteract(jugador, mano);
		if (!(jugador instanceof ServerPlayer sp)) return net.minecraft.world.InteractionResult.SUCCESS;
		if (!dueno.equals(sp.getUUID())) {
			sp.displayClientMessage(Component.literal("Esa no es tu alma.").withStyle(ChatFormatting.RED), true);
		} else if (com.dedsafio4.almas.AlmitasLimbo.yaRecupero(sp)) {
			sp.displayClientMessage(Component.literal("Ya recuperaste tu alma una vez: no se puede de nuevo.").withStyle(ChatFormatting.RED), true);
		} else if (com.dedsafio4.almas.RecolectorAlmasItem.carga(pila) < com.dedsafio4.almas.RecolectorAlmasItem.LLENO) {
			sp.displayClientMessage(Component.literal("El Recolector está gastado: tiene " + com.dedsafio4.almas.RecolectorAlmasItem.carga(pila)
					+ " de 100 almas. Mata Fantasmas Negros o Amarillos.").withStyle(ChatFormatting.RED), true);
		} else if (com.dedsafio4.almas.Almas.tieneAlma(sp)) {
			sp.displayClientMessage(Component.literal("Ya tienes tu alma.").withStyle(ChatFormatting.YELLOW), true);
		} else {
			com.dedsafio4.almas.Almas.ponerAlma(sp, true);
			com.dedsafio4.almas.RecolectorAlmasItem.ponerCarga(pila, 0);
			com.dedsafio4.almas.AlmitasLimbo.recupero(sp);
			if (level() instanceof net.minecraft.server.level.ServerLevel mundo) {
				mundo.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 30, 0.4, 0.6, 0.4, 0.05);
			}
			level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
			sp.sendSystemMessage(Component.literal("Recuperaste tu alma: ya puedes revivir.").withStyle(ChatFormatting.AQUA));
			discard();   // ya no la necesita: sólo se puede recuperar una vez
		}
		return net.minecraft.world.InteractionResult.CONSUME;
	}

	@Override
	public void remove(RemovalReason razon) {
		// Si se fue para siempre (no sólo porque se descargó su zona), vuelve a aparecer después.
		if (!level().isClientSide && dueno != null && razon.shouldDestroy() && getServer() != null) {
			com.dedsafio4.almas.AlmitasLimbo.olvidar(getServer(), dueno);
		}
		super.remove(razon);
	}

	@Override
	public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean enElPiso, BlockState estado, BlockPos pos) {}

	public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("almita").requires(s -> s.hasPermission(2))
				.then(Commands.argument("nombre", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(c.getSource().getOnlinePlayerNames(), b))
						.executes(c -> {
							ServerPlayer p = c.getSource().getPlayerOrException();
							String nombre = StringArgumentType.getString(c, "nombre");
							// La Almita más cercana (hasta 128 bloques); sólo si no hay ninguna aparece una nueva.
							AlmitaEntity almita = p.serverLevel().getEntitiesOfClass(AlmitaEntity.class, p.getBoundingBox().inflate(128)).stream()
									.min(Comparator.comparingDouble(a -> a.distanceToSqr(p))).orElse(null);
							boolean nueva = almita == null;
							if (nueva) almita = ModEntidades.ALMITA.spawn(p.serverLevel(), p.blockPosition().above(), MobSpawnType.COMMAND);
							if (almita == null) return 0;
							almita.setCustomName(Component.literal(nombre).withStyle(ChatFormatting.AQUA));
							almita.setCustomNameVisible(true);
							almita.setPersistenceRequired();
							c.getSource().sendSuccess(() -> Component.literal((nueva ? "Apareció una Almita con el nombre " : "La Almita ahora se llama ")
									+ nombre + ".").withStyle(ChatFormatting.AQUA), true);
							return 1;
						})));
	}
}
