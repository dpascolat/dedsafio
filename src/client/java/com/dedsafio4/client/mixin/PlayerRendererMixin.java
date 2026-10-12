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
 *   [alma] Nombre          (del color de su Hermandad, si tiene; el ícono del Alma o del Sin Alma a la izquierda)
 *   [rango] <Hermandad>    (solo si tiene; donde antes iba el nombre; el rango es el dibujito de la lista de la
 *                          Hermandad: corona el Maestro, estrella los Líderes, persona los Miembros)
 * Con /nombre esconder no se ve nada de eso.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	/** Celeste lavanda del número de vida. */
	private static final TextColor COLOR_VIDA = TextColor.fromRgb(0xAAB4FF);
	/** La fuente de los íconos del mod (los dibujitos de los rangos). */
	private static final net.minecraft.resources.ResourceLocation ICONOS =
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dedsafio4", "iconos");

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

	@Inject(method = RENDER_NAME_TAG, at = @At("HEAD"), cancellable = true)
	private void dedsafio4$lineasExtra(AbstractClientPlayer jugador, Component nombre, PoseStack pose,
									   MultiBufferSource buffers, int luz, float partialTick, CallbackInfo ci) {
		// Todo el cartel lo dibuja el mod (el nombre de Minecraft no).
		ci.cancel();
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

		HermandadesJugadoresPayload.Entrada hermandad = HermandadesCliente.de(jugador.getUUID());
		// Con Hermandad: la Hermandad va donde iba el nombre y el nombre (y la vida) suben una línea.
		float yNombre = hermandad != null ? -10f : 0f;
		dibujarLinea(pose, buffers, luz, jugador, vida, yNombre - 10f);
		float ancho = dibujarLinea(pose, buffers, luz, jugador, nombre, yNombre, 10f);
		dibujarAlma(pose, buffers, luz, jugador, -ancho / 2f + 4.5f, yNombre + 4f);
		if (hermandad != null) {
			// El dibujito del rango (como en la lista de la Hermandad): corona, estrella o persona.
			String icono;
			int colorRango;
			switch (hermandad.rango()) {
				case "Maestro" -> { icono = ""; colorRango = 0xFFC21F; }
				case "Líder" -> { icono = ""; colorRango = 0xC77DF0; }
				default -> { icono = ""; colorRango = 0x4DA6FF; }
			}
			Component linea = Component.empty()
					.append(Component.literal(icono).withStyle(s -> s.withFont(ICONOS).withColor(TextColor.fromRgb(colorRango))))
					.append(Component.literal(" <" + hermandad.hermandad() + ">")
							.withStyle(s -> s.withColor(TextColor.fromRgb(hermandad.color()))));
			dibujarLinea(pose, buffers, luz, jugador, linea, 0f);
		}
		pose.popPose();
	}

	/** El ícono del Alma (o del Sin Alma, si se la sacaron), con el centro en (x, y). */
	private static void dibujarAlma(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador, float x, float y) {
		boolean tiene = !com.dedsafio4.client.AlmaCliente.SIN_ALMA.contains(jugador.getUUID());
		net.minecraft.world.item.ItemStack icono = new net.minecraft.world.item.ItemStack(
				tiene ? com.dedsafio4.items.ModItems.ALMA : com.dedsafio4.items.ModItems.SIN_ALMA);
		pose.pushPose();
		pose.translate(x, y, 0);
		pose.scale(9f, -9f, 0.01f);   // el cartel tiene la Y para abajo; el ítem la quiere para arriba
		Minecraft.getInstance().getItemRenderer().renderStatic(icono, net.minecraft.world.item.ItemDisplayContext.GUI,
				net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
				pose, buffers, jugador.level(), 0);
		pose.popPose();
	}

	private static void dibujarLinea(PoseStack pose, MultiBufferSource buffers, int luz,
									 AbstractClientPlayer jugador, Component texto, float y) {
		dibujarLinea(pose, buffers, luz, jugador, texto, y, 0f);
	}

	/**
	 * Una línea centrada; {@code lugarIzquierda} deja ese espacio libre a la izquierda (para un ícono) y la corre a la
	 * derecha para que el conjunto quede centrado. Devuelve el ancho total (espacio + texto).
	 */
	private static float dibujarLinea(PoseStack pose, MultiBufferSource buffers, int luz,
									  AbstractClientPlayer jugador, Component texto, float y, float lugarIzquierda) {
		Matrix4f matriz = pose.last().pose();
		Font font = Minecraft.getInstance().font;
		float total = font.width(texto) + lugarIzquierda;
		float x = -total / 2f + lugarIzquierda;
		int fondo = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
		boolean visibleAtraves = !jugador.isDiscrete();

		font.drawInBatch(texto, x, y, 0x20FFFFFF, false, matriz, buffers,
				visibleAtraves ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, fondo, luz);
		if (visibleAtraves) {
			font.drawInBatch(texto, x, y, 0xFFFFFFFF, false, matriz, buffers, Font.DisplayMode.NORMAL, 0, luz);
		}
		return total;
	}
}
