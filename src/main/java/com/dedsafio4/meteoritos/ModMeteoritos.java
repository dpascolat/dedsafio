package com.dedsafio4.meteoritos;

import com.dedsafio4.Dedsafio4;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Los meteoritos: por ahora los tira un admin (o alguien en creativo) con click derecho con un palo, hacia donde mira. */
public final class ModMeteoritos {
	private ModMeteoritos() {}

	public static final EntityType<MeteoritoEntity> METEORITO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("meteorito"), EntityType.Builder.<MeteoritoEntity>of(MeteoritoEntity::new, MobCategory.MISC)
					.sized(0.9f, 0.9f).clientTrackingRange(16).updateInterval(1).build("meteorito"));

	public static final ResourceKey<DamageType> DANIO_METEORITO = ResourceKey.create(Registries.DAMAGE_TYPE, id("meteorito"));

	/** Hasta dónde se puede apuntar con el palo. */
	private static final double ALCANCE = 160;
	/** De dónde viene: 27 bloques de costado y 36 de alto (la misma inclinación que en el diseño). */
	private static final double DESDE_COSTADO = 27, DESDE_ARRIBA = 36;

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Tira un meteorito que cae en ese punto, viniendo desde un costado al azar. */
	public static MeteoritoEntity lanzar(ServerLevel mundo, Vec3 destino) {
		return lanzar(mundo, destino, mundo.getRandom().nextFloat() * Mth.TWO_PI);
	}

	public static MeteoritoEntity lanzar(ServerLevel mundo, Vec3 destino, float angulo) {
		Vec3 inicio = destino.add(Math.cos(angulo) * DESDE_COSTADO, DESDE_ARRIBA, Math.sin(angulo) * DESDE_COSTADO);
		if (inicio.y > mundo.getMaxBuildHeight() + 20) inicio = new Vec3(inicio.x, mundo.getMaxBuildHeight() + 20, inicio.z);
		MeteoritoEntity meteorito = MeteoritoEntity.crear(mundo, inicio, destino);
		mundo.addFreshEntity(meteorito);
		return meteorito;
	}

	public static void registrar() {
		// Click derecho con un palo (solo admins o en creativo): cae un meteorito donde mira.
		UseItemCallback.EVENT.register((jugador, level, mano) -> {
			ItemStack pila = jugador.getItemInHand(mano);
			if (!pila.is(Items.STICK)) return InteractionResultHolder.pass(pila);
			if (!(level instanceof ServerLevel mundo)) return InteractionResultHolder.pass(pila);
			boolean puede = jugador.hasPermissions(2) || jugador.isCreative();
			if (!puede || jugador.getCooldowns().isOnCooldown(Items.STICK)) return InteractionResultHolder.pass(pila);
			Vec3 ojos = jugador.getEyePosition();
			BlockHitResult golpe = mundo.clip(new ClipContext(ojos, ojos.add(jugador.getLookAngle().scale(ALCANCE)),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jugador));
			if (golpe.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(pila);
			lanzar(mundo, golpe.getLocation());
			jugador.getCooldowns().addCooldown(Items.STICK, 20);
			return InteractionResultHolder.success(pila);
		});
	}
}
