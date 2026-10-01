package com.dedsafio4.mixin;

import com.dedsafio4.candados.Candados;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** Un cofre con candado tampoco se puede volar con TNT ni con un Creeper. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow @Final private Level level;
	@Shadow @Final private ObjectArrayList<BlockPos> toBlow;

	@Inject(method = "explode", at = @At("RETURN"))
	private void dedsafio4$respetarCandados(CallbackInfo ci) {
		if (!(this.level instanceof ServerLevel mundo)) return;
		this.toBlow.removeIf(pos -> Candados.sePuedeCerrar(mundo.getBlockState(pos))
				&& Candados.cerradura(mundo, pos) != null);
	}

	/**
	 * creepers_pasto 1: si la explosión rompe pasto, puede salir un Creeper de Pasto (una sola tirada por
	 * explosión, para que las explosiones de los mismos Creepers de Pasto no los multipliquen sin fin).
	 */
	@Inject(method = "explode", at = @At("RETURN"))
	private void dedsafio4$creeperDePasto(CallbackInfo ci) {
		if (!(this.level instanceof ServerLevel mundo)) return;
		java.util.List<BlockPos> pasto = this.toBlow.stream()
				.filter(pos -> com.dedsafio4.bestias.CreeperPastoEntity.esPasto(mundo.getBlockState(pos))).toList();
		if (pasto.isEmpty()) return;
		com.dedsafio4.bestias.CreeperPastoEntity.quizasAparecer(mundo, pasto.get(mundo.getRandom().nextInt(pasto.size())));
	}
}
