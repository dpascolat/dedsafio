package com.dedsafio4.neocompat.fabric.api.entity.event.v1;

import com.dedsafio4.neocompat.fabric.api.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ServerLivingEntityEvents {
	private ServerLivingEntityEvents() {}

	public interface AllowDeath {
		boolean allowDeath(LivingEntity entidad, DamageSource fuente, float danio);
	}

	public interface AfterDeath {
		void afterDeath(LivingEntity entidad, DamageSource fuente);
	}

	public static final Event<AllowDeath> ALLOW_DEATH = new Event<>();
	public static final Event<AfterDeath> AFTER_DEATH = new Event<>();
}
