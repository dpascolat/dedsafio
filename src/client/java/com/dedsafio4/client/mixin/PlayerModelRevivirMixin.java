package com.dedsafio4.client.mixin;

import com.dedsafio4.client.revivir.RevivirCliente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Las animaciones del jugador en /revivir cinematica (brazos y piernas; la pose agachado/acostado la pone Revivir):
 *   100–124  sale agachado, los brazos van subiendo
 *   124–184  sube con los brazos arriba
 *   184–244  vuela hacia adelante con los brazos abiertos, balanceándose
 *   244–258  cae sacudiendo brazos y piernas
 *   258–276  acostado boca arriba con los brazos abiertos
 *   276–308  se sienta, se apoya y se para; los brazos bajan
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelRevivirMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dedsafio4$revivir(LivingEntity entidad, float limbSwing, float limbSwingAmount, float edad,
								   float giroCabeza, float inclinacionCabeza, CallbackInfo ci) {
		if (!(entidad instanceof Player jugador)) return;
		float t = RevivirCliente.tiempo(jugador.getUUID(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
		if (t < 100 || t >= 308) return;
		PlayerModel<?> m = (PlayerModel<?>) (Object) this;
		// Por defecto, quieto (sin la animación de caminar).
		float bdX = 0, bdZ = 0, biX = 0, biZ = 0, pdX = 0, piX = 0, pdZ = 0, piZ = 0, cuerpoX = 0;
		if (t < 124) {
			// Sale agachado: los brazos suben de a poco hacia adelante.
			float k = (t - 100) / 24f;
			bdX = biX = -0.4f - 1.4f * k;
			bdZ = 0.15f;
			biZ = -0.15f;
		} else if (t < 184) {
			// Sube: los brazos bien arriba (un poco abiertos) y las piernas juntas, un poco atrás.
			float k = Math.min(1, (t - 124) / 10f);
			bdX = biX = Mth.lerp(k, -1.8f, -3.0f);
			bdZ = 0.25f + 0.05f * Mth.sin(t * 0.2f);
			biZ = -0.25f - 0.05f * Mth.sin(t * 0.2f);
			pdX = piX = 0.15f;
		} else if (t < 244) {
			// Vuela hacia adelante: brazos abiertos como alas, se balancea; las piernas se mueven un poco.
			float k = Math.min(1, (t - 184) / 10f), vaiven = Mth.sin(t * 0.18f);
			bdX = biX = Mth.lerp(k, -3.0f, -0.3f);
			bdZ = Mth.lerp(k, 0.25f, 1.25f) + 0.15f * vaiven;
			biZ = -Mth.lerp(k, 0.25f, 1.25f) + 0.15f * vaiven;
			pdX = 0.25f * Mth.sin(t * 0.25f);
			piX = -pdX;
			cuerpoX = 0.15f * k;
		} else if (t < 258) {
			// Cae: brazos y piernas sacudiéndose.
			bdX = -2.6f + 0.7f * Mth.sin(t * 1.3f);
			biX = -2.6f + 0.7f * Mth.sin(t * 1.3f + 1.6f);
			bdZ = 0.5f;
			biZ = -0.5f;
			pdX = 0.6f * Mth.sin(t * 1.1f);
			piX = -pdX;
		} else if (t < 276) {
			// Acostado boca arriba: brazos y piernas abiertos.
			bdZ = 1.1f;
			biZ = -1.1f;
			pdZ = 0.25f;
			piZ = -0.25f;
		} else {
			// Se sienta, se apoya con las manos y se para: los brazos bajan.
			float k = (t - 276) / 32f;
			bdX = biX = Mth.lerp(k, -1.2f, 0f);
			bdZ = Mth.lerp(k, 0.3f, 0f);
			biZ = -bdZ;
		}
		m.rightArm.xRot = bdX;
		m.rightArm.yRot = 0;
		m.rightArm.zRot = bdZ;
		m.leftArm.xRot = biX;
		m.leftArm.yRot = 0;
		m.leftArm.zRot = biZ;
		if ((t >= 124 && t < 276) || t >= 292) {
			// (Agachado, las piernas las deja Minecraft.)
			m.rightLeg.xRot = pdX;
			m.leftLeg.xRot = piX;
			m.rightLeg.zRot = pdZ;
			m.leftLeg.zRot = piZ;
			m.body.xRot = cuerpoX;
		}
		m.leftSleeve.copyFrom(m.leftArm);
		m.rightSleeve.copyFrom(m.rightArm);
		m.leftPants.copyFrom(m.leftLeg);
		m.rightPants.copyFrom(m.rightLeg);
		m.jacket.copyFrom(m.body);
	}
}
