package com.dedsafio4.meteoritos;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

/** Los meteoritos: caen donde se les pida con lanzar() (por ejemplo, desde las cinemáticas). */
public final class ModMeteoritos {
	private ModMeteoritos() {}

	public static final EntityType<MeteoritoEntity> METEORITO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("meteorito"), EntityType.Builder.<MeteoritoEntity>of(MeteoritoEntity::new, MobCategory.MISC)
					.sized(0.9f, 0.9f).clientTrackingRange(16).updateInterval(1).build("meteorito"));

	public static final ResourceKey<DamageType> DANIO_METEORITO = ResourceKey.create(Registries.DAMAGE_TYPE, id("meteorito"));

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

	/** Antes un palo en manos de un admin tiraba un meteorito; ya no (los palos ahora tienen otros usos). */
	public static void registrar() {
	}
}
