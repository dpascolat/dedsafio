package com.dedsafio4.client.disfraz;

import com.dedsafio4.Dedsafio4;
import com.dedsafio4.client.mixin.WalkAnimationAccessor;
import com.dedsafio4.disfraz.Disfraces;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Del lado del cliente, /cambiarmob: guarda quién está transformado en qué y arma un mob "de mentira" por jugador
 * (no está en el mundo ni piensa) que copia lo que hace el jugador (dónde mira, si camina, si pega, lo que tiene en
 * la mano) para dibujarlo en su lugar (PlayerRendererMobMixin).
 */
public final class DisfracesCliente {
	private DisfracesCliente() {}

	private record Falso(EntityType<?> tipo, Level mundo, Entity entidad) {}

	private static final Map<UUID, Falso> FALSOS = new HashMap<>();
	/** Los mobs que no se pudieron armar o dibujar (de algún mod raro): esos jugadores se ven normales. */
	private static final Set<EntityType<?>> FALLARON = new HashSet<>();

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(Disfraces.Payload.TYPE, (payload, context) -> context.client().execute(() -> {
			Disfraces.CLIENTE.clear();
			payload.mobs().forEach((jugador, id) -> {
				ResourceLocation r = ResourceLocation.tryParse(id);
				if (r != null) Disfraces.CLIENTE.put(jugador, r);
			});
			FALSOS.keySet().removeIf(j -> !Disfraces.CLIENTE.containsKey(j));
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null) for (Player p : mc.level.players()) p.refreshDimensions();
		}));
	}

	/** Si no se pudo dibujar ese mob, no se vuelve a intentar (el jugador se ve normal). */
	public static void fallo(EntityType<?> tipo, Exception e) {
		if (FALLARON.add(tipo)) Dedsafio4.LOGGER.warn("No se pudo dibujar al jugador como " + EntityType.getKey(tipo), e);
	}

	/** El mob de mentira de este jugador, ya acomodado como él (o null si no está transformado). */
	public static Entity falso(AbstractClientPlayer jugador) {
		EntityType<?> tipo = Disfraces.de(jugador);
		if (tipo == null || FALLARON.contains(tipo)) return null;
		Falso f = FALSOS.get(jugador.getUUID());
		if (f == null || f.tipo() != tipo || f.mundo() != jugador.level()) {
			Entity nueva;
			try {
				nueva = tipo.create(jugador.level());
			} catch (Exception e) {
				fallo(tipo, e);
				return null;
			}
			if (nueva == null) {
				FALLARON.add(tipo);
				return null;
			}
			f = new Falso(tipo, jugador.level(), nueva);
			FALSOS.put(jugador.getUUID(), f);
		}
		copiar(jugador, f.entidad());
		return f.entidad();
	}

	private static void copiar(AbstractClientPlayer p, Entity e) {
		e.setPos(p.getX(), p.getY(), p.getZ());
		e.xo = p.xo;
		e.yo = p.yo;
		e.zo = p.zo;
		e.xOld = p.xOld;
		e.yOld = p.yOld;
		e.zOld = p.zOld;
		e.setYRot(p.getYRot());
		e.yRotO = p.yRotO;
		e.setXRot(p.getXRot());
		e.xRotO = p.xRotO;
		e.tickCount = p.tickCount;
		e.setOnGround(p.onGround());
		e.setShiftKeyDown(p.isShiftKeyDown());
		e.setSprinting(p.isSprinting());
		e.setInvisible(p.isInvisible());
		e.setDeltaMovement(p.getDeltaMovement());
		if (!(e instanceof LivingEntity m)) return;
		m.yBodyRot = p.yBodyRot;
		m.yBodyRotO = p.yBodyRotO;
		m.yHeadRot = p.yHeadRot;
		m.yHeadRotO = p.yHeadRotO;
		m.hurtTime = p.hurtTime;
		m.hurtDuration = p.hurtDuration;
		m.deathTime = p.deathTime;
		m.swinging = p.swinging;
		m.swingTime = p.swingTime;
		m.swingingArm = p.swingingArm;
		m.attackAnim = p.attackAnim;
		m.oAttackAnim = p.oAttackAnim;
		m.setHealth(Math.max(0.01f, p.getHealth()));
		// Las piernas: lo mismo que el jugador.
		WalkAnimationAccessor desde = (WalkAnimationAccessor) p.walkAnimation, hacia = (WalkAnimationAccessor) m.walkAnimation;
		hacia.dedsafio4$setSpeedOld(desde.dedsafio4$getSpeedOld());
		hacia.dedsafio4$setSpeed(desde.dedsafio4$getSpeed());
		hacia.dedsafio4$setPosition(desde.dedsafio4$getPosition());
		// Lo que tiene en las manos y la armadura (los zombis, esqueletos, etc. lo muestran).
		for (EquipmentSlot lugar : EquipmentSlot.values()) {
			if (lugar.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
			m.setItemSlot(lugar, p.getItemBySlot(lugar));
		}
	}
}
