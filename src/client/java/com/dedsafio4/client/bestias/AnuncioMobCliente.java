package com.dedsafio4.client.bestias;

import com.dedsafio4.bestias.AnuncioMob;
import com.dedsafio4.items.HuevoConDescripcion;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * El cartel de /mob: baja desde arriba, se queda unos segundos y se va. A la izquierda el mob girando (o su huevo si
 * no se puede dibujar), al lado "Nuevo Mob - Nombre" en amarillo y abajo la descripción del huevo en blanco.
 */
public final class AnuncioMobCliente {
	private AnuncioMobCliente() {}

	private static final long DURA = 9000, ENTRA = 500;
	private static EntityType<?> tipo;
	private static Entity mob;
	private static List<String> descripcion = List.of();
	private static long desde;

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(AnuncioMob.Payload.TYPE, (payload, context) -> context.client().execute(() -> mostrar(payload.mob())));
		HudRenderCallback.EVENT.register((g, contador) -> dibujar(g));
	}

	private static void mostrar(String id) {
		Minecraft mc = Minecraft.getInstance();
		ResourceLocation rl = ResourceLocation.tryParse(id);
		if (rl == null || mc.level == null) return;
		tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
		if (tipo == null) return;
		try {
			mob = tipo.create(mc.level);
		} catch (Exception e) {
			mob = null;
		}
		// La descripción del huevo: la primera parte (hasta el renglón vacío), en un solo párrafo.
		List<Component> lineas = new ArrayList<>();
		SpawnEggItem huevo = SpawnEggItem.byId(tipo);
		if (huevo instanceof HuevoConDescripcion h) h.describir(lineas);
		StringBuilder texto = new StringBuilder();
		for (Component c : lineas) {
			String s = c.getString().trim();
			if (s.isEmpty()) {
				if (texto.length() > 0) break;
				continue;
			}
			if (texto.length() > 0) texto.append(' ');
			texto.append(s);
		}
		descripcion = texto.length() == 0 ? List.of() : List.of(texto.toString());
		desde = Util.getMillis();
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f));
	}

	private static void dibujar(GuiGraphics g) {
		if (tipo == null) return;
		long t = Util.getMillis() - desde;
		if (t > DURA) {
			tipo = null;
			mob = null;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		float entra = Mth.clamp(t / (float) ENTRA, 0, 1), sale = Mth.clamp((DURA - t) / (float) ENTRA, 0, 1);
		float mostrar = Math.min(entra, sale);
		int ancho = Math.min(330, g.guiWidth() - 20);
		Component titulo = Component.literal("Nuevo Mob - ").append(tipo.getDescription())
				.withStyle(Style.EMPTY.withBold(true).withColor(0xFFF23A));
		List<FormattedCharSequence> renglones = new ArrayList<>();
		for (String d : descripcion) renglones.addAll(font.split(Component.literal(d), ancho - 52));
		int alto = Math.max(44, 22 + renglones.size() * 10 + 6);
		int x = (g.guiWidth() - ancho) / 2, y = Math.round(6 - (1 - mostrar) * (alto + 10));

		g.fillGradient(x, y, x + ancho, y + alto, 0xE0123A6E, 0xE00A2448);
		g.fill(x, y, x + ancho, y + 1, 0xFF5CA8FF);
		g.fill(x, y + alto - 1, x + ancho, y + alto, 0xFF0A1830);
		g.drawString(font, titulo, x + 48, y + 7, 0xFFFFFFFF, true);
		int ry = y + 21;
		for (FormattedCharSequence r : renglones) {
			g.drawString(font, r, x + 48, ry, 0xFFF0F4FF, true);
			ry += 10;
		}

		// El mob, girando despacio (o el huevo).
		int cx = x + 24, cy = y + alto / 2;
		if (mob instanceof LivingEntity vivo) {
			float tam = Math.max(vivo.getBbWidth(), vivo.getBbHeight());
			float escala = 30f / Math.max(0.5f, tam);
			vivo.yBodyRot = vivo.yBodyRotO = 180 + t / 25f;
			vivo.yHeadRot = vivo.yHeadRotO = vivo.yBodyRot;
			vivo.setYRot(vivo.yBodyRot);
			g.enableScissor(x + 2, y + 1, x + 46, y + alto - 1);
			try {
				InventoryScreen.renderEntityInInventory(g, cx, cy + vivo.getBbHeight() * escala / 2f, escala,
						new Vector3f(), new Quaternionf().rotateZ((float) Math.PI), null, vivo);
			} catch (Exception e) {
				mob = null;
			}
			g.disableScissor();
		} else {
			SpawnEggItem huevo = SpawnEggItem.byId(tipo);
			if (huevo != null) {
				g.pose().pushPose();
				g.pose().translate(cx - 16, cy - 16, 0);
				g.pose().scale(2, 2, 1);
				g.renderItem(new ItemStack(huevo), 0, 0);
				g.pose().popPose();
			}
		}
	}
}
