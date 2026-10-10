package com.dedsafio4.client.mixin;

import com.dedsafio4.client.HermandadesCliente;
import com.dedsafio4.hermandad.HermandadesJugadoresPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arriba de la cabeza de cada jugador:
 *   vida ❤
 *   Nombre           (del color de su Hermandad, si tiene)
 *   <Hermandad>      (solo si tiene)
 * Con /nombre esconder no se ve nada de eso.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	/** Celeste lavanda del número de vida. */
	private static final TextColor COLOR_VIDA = TextColor.fromRgb(0xAAB4FF);

	private static final String RENDER_NAME_TAG = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V";

	/** /nombre esconder: arriba de la cabeza no se dibuja nada (ni la vida, ni el nombre, ni la Hermandad). */
	@Inject(method = RENDER_NAME_TAG, at = @At("HEAD"), cancellable = true)
	private void dedsafio4$nombreEscondido(AbstractClientPlayer jugador, Component nombre, PoseStack pose,
										   MultiBufferSource buffers, int luz, float partialTick, CallbackInfo ci) {
		if (com.dedsafio4.NombreOculto.ocultoEnCliente(jugador.getUUID())) ci.cancel();
	}

	/** El nombre toma el color de la Hermandad (solo arriba de la cabeza, no en el chat). */
	@ModifyVariable(method = RENDER_NAME_TAG, at = @At("HEAD"), argsOnly = true)
	private Component dedsafio4$nombreColorHermandad(Component nombre, AbstractClientPlayer jugador, Component original,
													 PoseStack pose, MultiBufferSource buffers, int luz, float partialTick) {
		HermandadesJugadoresPayload.Entrada hermandad = HermandadesCliente.de(jugador.getUUID());
		if (hermandad == null) return nombre;
		return nombre.copy().withStyle(s -> s.withColor(TextColor.fromRgb(hermandad.color())));
	}

	@Inject(method = RENDER_NAME_TAG, at = @At("HEAD"))
	private void dedsafio4$lineasExtra(AbstractClientPlayer jugador, Component nombre, PoseStack pose,
									   MultiBufferSource buffers, int luz, float partialTick, CallbackInfo ci) {
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		if (dispatcher.distanceToSqr(jugador) > 4096.0) return;
		Vec3 anclaje = jugador.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, jugador.getViewYRot(partialTick));
		if (anclaje == null) return;

		Component vida = Component.empty()
				.append(Component.literal(Mth.ceil(jugador.getHealth()) + " ").withStyle(s -> s.withColor(COLOR_VIDA)))
				.append(Component.literal("❤").withStyle(ChatFormatting.RED));

		pose.pushPose();
		pose.translate(anclaje.x, anclaje.y + 0.5, anclaje.z);
		pose.mulPose(dispatcher.cameraOrientation());
		pose.scale(0.025F, -0.025F, 0.025F);

		dibujarLinea(pose, buffers, luz, jugador, vida, -10f);   // una línea por encima del nombre
		HermandadesJugadoresPayload.Entrada hermandad = HermandadesCliente.de(jugador.getUUID());
		if (hermandad != null) {
			Component linea = Component.literal("<" + hermandad.hermandad() + ">")
					.withStyle(s -> s.withColor(TextColor.fromRgb(hermandad.color())));
			dibujarLinea(pose, buffers, luz, jugador, linea, 10f);  // una línea por debajo del nombre
		}
		pose.popPose();
	}

	private static void dibujarLinea(PoseStack pose, MultiBufferSource buffers, int luz,
									 AbstractClientPlayer jugador, Component texto, float y) {
		Matrix4f matriz = pose.last().pose();
		Font font = Minecraft.getInstance().font;
		float x = -font.width(texto) / 2f;
		int fondo = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
		boolean visibleAtraves = !jugador.isDiscrete();

		font.drawInBatch(texto, x, y, 0x20FFFFFF, false, matriz, buffers,
				visibleAtraves ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, fondo, luz);
		if (visibleAtraves) {
			font.drawInBatch(texto, x, y, 0xFFFFFFFF, false, matriz, buffers, Font.DisplayMode.NORMAL, 0, luz);
		}
	}
}
