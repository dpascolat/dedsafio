package com.dedsafio4.mixin;

import com.dedsafio4.cambios.Cambios;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Cambio "zombi 1": los zombis que escalan suben las paredes como las arañas. */
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$zombiTrepa(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		if (com.dedsafio4.cambios.Zombis.trepando((LivingEntity) (Object) this)) cir.setReturnValue(true);
	}

	/**
	 * Cambio "respirar agua 1": meter la cabeza en una puerta, trampilla, antorcha, cartel, etc.
	 * rodeado de agua ya no sirve para respirar; cuenta como tener los ojos bajo el agua.
	 */
	@WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean dedsafio4$sinBolsasDeAire(LivingEntity entidad, TagKey<Fluid> fluido, Operation<Boolean> original) {
		if (original.call(entidad, fluido)) return true;
		if (fluido != FluidTags.WATER || !(entidad instanceof Player)) return false;
		if (!(entidad.level() instanceof ServerLevel level)) return false;
		if (Cambios.nivel(level.getServer(), Cambios.RESPIRAR_AGUA) < 1) return false;
		return cabezaEnBolsaDeAire(level, BlockPos.containing(entidad.getX(), entidad.getEyeY(), entidad.getZ()));
	}

	/**
	 * La cabeza está en un bloque sin agua que no es aire (puerta, trampilla, antorcha...) y hay agua
	 * en al menos 2 de los lados o arriba. Pedir 2 evita que te ahogues en tierra firme por pararte
	 * en una puerta que tiene agua de un solo lado.
	 */
	private static boolean cabezaEnBolsaDeAire(Level level, BlockPos cabeza) {
		BlockState estado = level.getBlockState(cabeza);
		if (estado.isAir() || !estado.getFluidState().isEmpty()) return false;

		int ladosConAgua = 0;
		for (Direction dir : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			if (level.getFluidState(cabeza.relative(dir)).is(FluidTags.WATER)) ladosConAgua++;
		}
		return ladosConAgua >= 2;
	}

	/** Con Veneno Primitivo no te podés curar de ninguna forma. */
	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void dedsafio4$sinCuracion(float cuanto, CallbackInfo ci) {
		LivingEntity entidad = (LivingEntity) (Object) this;
		if (com.dedsafio4.reptisaurios.VenenoPrimitivo.envenenado(entidad)) ci.cancel();
	}
}
