package com.dedsafio4.mixin;

import com.dedsafio4.items.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Las flechas del Arco Glebanoide rebotan: cuando le pegan a algo, sale una flecha nueva desde ahí hacia el enemigo
 * más cercano (hasta 3 rebotes, a 10 bloques como mucho). Las flechas del rebote no se pueden juntar.
 */
@Mixin(AbstractArrow.class)
public abstract class ArcoReboteMixin {
	private static final int REBOTES = 3;
	private static final double ALCANCE = 10;
	private static final String MARCA = "dedsafio4_rebotes_";

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void dedsafio4$rebotar(EntityHitResult golpe, CallbackInfo ci) {
		AbstractArrow flecha = (AbstractArrow) (Object) this;
		if (!(flecha.level() instanceof ServerLevel mundo)) return;
		int quedan = rebotesQueQuedan(flecha);
		if (quedan <= 0) return;
		Entity golpeado = golpe.getEntity();
		Entity dueno = flecha.getOwner();
		LivingEntity siguiente = null;
		double mejor = ALCANCE * ALCANCE;
		for (LivingEntity posible : mundo.getEntitiesOfClass(LivingEntity.class, golpeado.getBoundingBox().inflate(ALCANCE),
				e -> e.isAlive() && e instanceof Enemy && e != golpeado && e != dueno)) {
			double d = posible.distanceToSqr(golpeado);
			if (d < mejor) {
				mejor = d;
				siguiente = posible;
			}
		}
		if (siguiente == null) return;
		Vec3 desde = golpeado.position().add(0, golpeado.getBbHeight() * 0.6, 0);
		Arrow nueva = new Arrow(mundo, desde.x, desde.y, desde.z, new ItemStack(Items.ARROW), null);
		nueva.setOwner(dueno);
		nueva.setBaseDamage(flecha.getBaseDamage());
		nueva.pickup = AbstractArrow.Pickup.DISALLOWED;
		nueva.addTag(MARCA + (quedan - 1));
		Vec3 hacia = siguiente.getEyePosition().subtract(desde);
		// Que no se choque con el que acaba de golpear: arranca un poquito corrida hacia el próximo.
		Vec3 inicio = desde.add(hacia.normalize().scale(golpeado.getBbWidth() * 0.6 + 0.3));
		nueva.setPos(inicio.x, inicio.y, inicio.z);
		nueva.shoot(hacia.x, hacia.y + hacia.horizontalDistance() * 0.05, hacia.z, 2.0f, 0.5f);
		mundo.addFreshEntity(nueva);
	}

	/** Las del Arco Glebanoide arrancan con 3; las de un rebote tienen la cuenta en una marca. */
	private static int rebotesQueQuedan(AbstractArrow flecha) {
		for (String marca : flecha.getTags()) {
			if (marca.startsWith(MARCA)) {
				try {
					return Integer.parseInt(marca.substring(MARCA.length()));
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		ItemStack arma = flecha.getWeaponItem();
		return arma != null && arma.is(ModItems.ARCO_GLEBANOIDE) ? REBOTES : 0;
	}
}
