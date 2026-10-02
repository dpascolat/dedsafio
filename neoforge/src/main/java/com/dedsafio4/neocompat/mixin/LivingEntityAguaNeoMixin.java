package com.dedsafio4.neocompat.mixin;

import com.dedsafio4.cambios.Cambios;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Versión NeoForge del cambio "respirar agua 1" (en Fabric: LivingEntityMixin#sinBolsasDeAire). NeoForge mira el
 * agua de los ojos con FluidType en vez de con la etiqueta de agua, así que se envuelve eso.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAguaNeoMixin {
	@WrapOperation(method = "baseTick", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluidType(Lnet/neoforged/neoforge/fluids/FluidType;)Z"))
	private boolean dedsafio4$sinBolsasDeAireNeo(LivingEntity entidad, FluidType tipo, Operation<Boolean> original) {
		if (original.call(entidad, tipo)) return true;
		return tipo == NeoForgeMod.WATER_TYPE.value() && dedsafio4$enBolsaDeAire(entidad);
	}

	@WrapOperation(method = "baseTick", require = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getEyeInFluidType()Lnet/neoforged/neoforge/fluids/FluidType;"))
	private FluidType dedsafio4$sinBolsasDeAireTipo(LivingEntity entidad, Operation<FluidType> original) {
		FluidType tipo = original.call(entidad);
		if (tipo.isAir() && dedsafio4$enBolsaDeAire(entidad)) return NeoForgeMod.WATER_TYPE.value();
		return tipo;
	}

	@Unique
	private static boolean dedsafio4$enBolsaDeAire(LivingEntity entidad) {
		if (!(entidad instanceof Player) || !(entidad.level() instanceof ServerLevel level)) return false;
		if (Cambios.nivel(level.getServer(), Cambios.RESPIRAR_AGUA) < 1) return false;
		BlockPos cabeza = BlockPos.containing(entidad.getX(), entidad.getEyeY(), entidad.getZ());
		BlockState estado = level.getBlockState(cabeza);
		if (estado.isAir() || !estado.getFluidState().isEmpty()) return false;
		int ladosConAgua = 0;
		for (Direction dir : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			if (level.getFluidState(cabeza.relative(dir)).is(FluidTags.WATER)) ladosConAgua++;
		}
		return ladosConAgua >= 2;
	}
}
