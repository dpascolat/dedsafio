package com.dedsafio4.items;

import com.dedsafio4.nave.ModEntidades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Altavoz: con clic derecho lanza una onda de sonido hacia adelante. Es lo único que lastima al Creeper Visceral
 * (dos ondas lo matan); a todo lo demás no le hace nada.
 */
public class AltavozItem extends Item {
	public static final ResourceKey<DamageType> DANIO = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("dedsafio4", "altavoz"));
	private static final double ALCANCE = 12;
	/** Qué tan abierta es la onda (coseno del ángulo: unos 35 grados para cada lado). */
	private static final double CONO = 0.82;
	private static final float GOLPE = 10;
	private static final int ESPERA = 40;

	public AltavozItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level mundo, Player jugador, InteractionHand mano) {
		ItemStack pila = jugador.getItemInHand(mano);
		jugador.getCooldowns().addCooldown(this, ESPERA);
		if (mundo instanceof ServerLevel servidor) {
			Vec3 ojos = jugador.getEyePosition(), mira = jugador.getLookAngle();
			servidor.playSound(null, jugador.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1f, 1.3f);
			for (int i = 2; i <= ALCANCE; i += 2) {
				Vec3 punto = ojos.add(mira.scale(i));
				servidor.sendParticles(ParticleTypes.SONIC_BOOM, punto.x, punto.y, punto.z, 1, 0, 0, 0, 0);
			}
			DamageSource fuente = new DamageSource(servidor.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO), jugador);
			for (var creeper : servidor.getEntities(ModEntidades.CREEPER_VISCERAL, jugador.getBoundingBox().inflate(ALCANCE), e -> true)) {
				Vec3 hacia = creeper.getBoundingBox().getCenter().subtract(ojos);
				if (hacia.length() <= ALCANCE && hacia.normalize().dot(mira) >= CONO) creeper.hurt(fuente, GOLPE);
			}
		}
		return InteractionResultHolder.sidedSuccess(pila, mundo.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
		texto.add(Component.literal("Lanza una onda de sonido.").withStyle(ChatFormatting.GRAY));
		texto.add(Component.literal("Es lo único que mata al Creeper Visceral.").withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
