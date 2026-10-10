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
							AlmitaEntity almita = p.serverLevel().getEntitiesOfClass(AlmitaEntity.class, p.getBoundingBox().inflate(8)).stream()
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
