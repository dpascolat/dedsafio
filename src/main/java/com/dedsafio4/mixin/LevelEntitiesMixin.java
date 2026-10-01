package com.dedsafio4.mixin;

import com.dedsafio4.qumara.QumaraEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Minecraft busca las entidades solo en los pedazos del mundo cerca de sus pies, así que a Qumara (40
 * bloques de alto) las flechas y los golpes en la parte de arriba la atravesaban. Acá se la agrega a la
 * búsqueda si su caja toca la zona.
 */
@Mixin(Level.class)
public abstract class LevelEntitiesMixin {
	@Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
			at = @At("RETURN"))
	private void dedsafio4$entidadesGrandes(Entity excepto, AABB zona, Predicate<? super Entity> filtro, CallbackInfoReturnable<List<Entity>> cir) {
		QumaraEntity.agregarGrandes((Level) (Object) this, excepto, zona, filtro, cir.getReturnValue());
	}
}
