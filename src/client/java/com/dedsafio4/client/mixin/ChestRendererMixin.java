package com.dedsafio4.client.mixin;

import com.dedsafio4.candados.Candados;
import com.dedsafio4.client.cofres.MallaCofre;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Los cofres con candado se dibujan con los diseños del usuario: el Cofre Chico si es simple
 * y el Cofre Oscuro si es doble (ese lo dibuja una sola de las dos mitades). El Cofre Amarillo
 * se dibuja siempre con su diseño (simple o doble), tenga candado o no.
 */
@Mixin(ChestRenderer.class)
public class ChestRendererMixin {
	@Unique
	private static final MallaCofre CHICO = new MallaCofre("cofre_chico.json", Set.of(1), 2);
	@Unique
	private static final MallaCofre OSCURO = new MallaCofre("cofre_oscuro.json", Set.of(1), 2);
	@Unique
	private static final MallaCofre AMARILLO = new MallaCofre("cofre_amarillo.json", Set.of(1), -1);
	@Unique
	private static final MallaCofre AMARILLO_DOBLE = new MallaCofre("cofre_amarillo_doble.json", Set.of(1), -1);

	@Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At("HEAD"), cancellable = true)
	private void dedsafio4$conCandado(BlockEntity entidad, float parcial, PoseStack pose, MultiBufferSource buffers,
									  int luz, int overlay, CallbackInfo ci) {
		Level level = entidad.getLevel();
		if (level == null) return;   // el ítem del inventario
		BlockPos pos = entidad.getBlockPos();
		BlockState estado = entidad.getBlockState();
		boolean amarillo = estado.getBlock() instanceof com.dedsafio4.cofres.CofreAmarilloBlock;
		if (!(estado.getBlock() instanceof ChestBlock) || (!amarillo && !Candados.tieneCandado(level, pos))) return;
		ci.cancel();

		boolean simple = estado.getValue(ChestBlock.TYPE) == ChestType.SINGLE;
		// El doble lo dibuja sólo una de las dos mitades (la del candado, o la izquierda en el amarillo), centrado entre las dos.
		if (!simple && (amarillo ? estado.getValue(ChestBlock.TYPE) != ChestType.LEFT : !Candados.principal(level, pos).equals(pos))) return;

		Direction frente = estado.getValue(ChestBlock.FACING);
		float abierto = entidad instanceof LidBlockEntity tapa ? tapa.getOpenNess(parcial) : 0f;
		pose.pushPose();
		if (simple) {
			pose.translate(0.5, 0, 0.5);
		} else {
			Direction otra = ChestBlock.getConnectedDirection(estado);
			pose.translate(0.5 + otra.getStepX() * 0.5, 0, 0.5 + otra.getStepZ() * 0.5);
		}
		pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));   // el frente del modelo es +Z
		if (amarillo) {
			// Las patas van debajo del cofre: se sube todo 4 píxeles para que se apoyen en el piso.
			pose.translate(0, 4 / 16f, 0);
			(simple ? AMARILLO : AMARILLO_DOBLE).dibujar(pose, buffers, luz, overlay, abierto);
		} else {
			(simple ? CHICO : OSCURO).dibujar(pose, buffers, luz, overlay, abierto);
		}
		pose.popPose();
	}
}
