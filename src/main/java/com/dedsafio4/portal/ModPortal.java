package com.dedsafio4.portal;

import com.dedsafio4.Dedsafio4;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** El Portal del Espacio (ver PortalEspacioEntity) y su ítem para ponerlo. */
public final class ModPortal {
	private ModPortal() {}

	public static final EntityType<PortalEspacioEntity> PORTAL_ESPACIO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			id("portal_espacio"), EntityType.Builder.<PortalEspacioEntity>of(PortalEspacioEntity::new, MobCategory.MISC)
					.sized(2.8f, 3f).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).fireImmune().build("portal_espacio"));

	/** Click derecho en el piso: pone el portal parado ahí, mirando hacia uno (derecho, como los bloques). */
	public static final Item PORTAL_ESPACIO_ITEM = Registry.register(BuiltInRegistries.ITEM, id("portal_espacio"),
			new Item(new Item.Properties()) {
				@Override
				public InteractionResult useOn(UseOnContext contexto) {
					Level mundo = contexto.getLevel();
					Player jugador = contexto.getPlayer();
					BlockPos pos = contexto.getClickedPos().relative(contexto.getClickedFace());
					float giro = jugador == null ? 0 : Math.round(Mth.wrapDegrees(jugador.getYRot() + 180) / 90f) * 90f;
					if (!mundo.isClientSide) {
						PortalEspacioEntity portal = new PortalEspacioEntity(mundo, Vec3.atBottomCenterOf(pos), giro);
						mundo.addFreshEntity(portal);
						mundo.gameEvent(jugador, GameEvent.ENTITY_PLACE, portal.position());
						mundo.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 1.4f);
					}
					contexto.getItemInHand().consume(1, jugador);
					return InteractionResult.sidedSuccess(mundo.isClientSide);
				}
			});

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(Dedsafio4.MOD_ID, nombre);
	}

	/** Solo para que se carguen los registros al arrancar. */
	public static void registrar() {}
}
