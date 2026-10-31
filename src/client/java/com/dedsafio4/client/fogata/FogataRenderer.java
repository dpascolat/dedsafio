package com.dedsafio4.client.fogata;

import com.dedsafio4.revivir.FogataEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dibuja la Fogata grande (el modelo está en modelos/fogata.json).
 * Animación de NavithLabs / AlieRequiem, usada con su permiso.
 */
public final class FogataRenderer extends EntityRenderer<FogataEntity> {
	private static final Logger a = LoggerFactory.getLogger("dedsafio4");
	private static final ResourceLocation b = ResourceLocation.fromNamespaceAndPath("dedsafio4", "modelos/fogata.json");
	private static final ResourceLocation c = ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/entity/fogata.png");
	private static final int d = 4;
	private final BlockPos.MutableBlockPos e = new BlockPos.MutableBlockPos();
	private static boolean f;
	private static boolean g;
	private static float[] h;
	private static int i;
	private static float j;
	private static float k;
	private static float l;
	private static float m;

	public FogataRenderer(EntityRendererProvider.Context var1) {
		super(var1);
	}

	private static boolean a() {
		if (f) {
			return true;
		} else if (g) {
			return false;
		} else {
			try {
				Optional var0 = Minecraft.getInstance().getResourceManager().getResource(b);
				if (var0.isEmpty()) {
					throw new IllegalStateException("no está " + b);
				} else {
					JsonObject var1;
					try (BufferedReader var2 = ((Resource)var0.get()).openAsReader()) {
						var1 = JsonParser.parseReader(var2).getAsJsonObject();
					}

					JsonArray var11 = var1.getAsJsonArray("quads");
					i = var11.size();
					h = new float[i * 23];
					int var3 = 0;

					for (JsonElement var5 : var11) {
						for (JsonElement var7 : var5.getAsJsonArray()) {
							h[var3++] = var7.getAsFloat();
						}
					}

					JsonArray var12 = var1.getAsJsonArray("caja");
					float[] var13 = new float[6];

					for (int var14 = 0; var14 < 6; var14++) {
						var13[var14] = var12.get(var14).getAsFloat();
					}

					j = (var13[0] + var13[3]) / 2.0F;
					k = (var13[1] + var13[4]) / 2.0F;
					l = (var13[2] + var13[5]) / 2.0F;
					m = 1.0F / Math.max(var13[3] - var13[0], Math.max(var13[4] - var13[1], var13[5] - var13[2]));
					f = true;
					return true;
				}
			} catch (Exception var10) {
				a.error("no se pudo cargar el modelo de la fogata", var10);
				g = true;
				return false;
			}
		}
	}

	@Override
	public void render(FogataEntity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
		if (a()) {
			var4.pushPose();
			var4.mulPose(Axis.YN.rotationDegrees(var1.getYRot() + 180.0F));
			var4.scale(0.0625F, 0.0625F, 0.0625F);
			a(var4, var5, var6, OverlayTexture.NO_OVERLAY);
			var4.popPose();
			super.render(var1, var2, var3, var4, var5, var6);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(FogataEntity var1) {
		return c;
	}

	@Override
	protected int getBlockLightLevel(FogataEntity var1, BlockPos var2) {
		return this.a(var1, LightLayer.BLOCK);
	}

	@Override
	protected int getSkyLightLevel(FogataEntity var1, BlockPos var2) {
		return this.a(var1, LightLayer.SKY);
	}

	private int a(FogataEntity var1, LightLayer var2) {
		int var3 = 0;

		for (int var4 = 0; var4 < 4; var4++) {
			this.e.set(var1.getBlockX(), var1.getBlockY() + var4, var1.getBlockZ());
			var3 = Math.max(var3, var1.level().getBrightness(var2, this.e));
		}

		return var3;
	}

	public static void a(ItemStack var0, ItemDisplayContext var1, PoseStack var2, MultiBufferSource var3, int var4, int var5) {
		if (a()) {
			var2.pushPose();
			var2.translate(0.5, 0.5, 0.5);
			var2.scale(m, m, m);
			var2.translate(-j, -k, -l);
			a(var2, var3, var4, var5);
			var2.popPose();
		}
	}

	private static void a(PoseStack var0, MultiBufferSource var1, int var2, int var3) {
		VertexConsumer var4 = var1.getBuffer(RenderType.entityCutout(c));
		PoseStack.Pose var5 = var0.last();
		int var6 = 0;

		for (int var7 = 0; var6 < i; var7 += 23) {
			for (int var8 = 0; var8 < 4; var8++) {
				int var9 = var7 + var8 * 5;
				var4.addVertex(var5, h[var9], h[var9 + 1], h[var9 + 2])
					.setColor(-1)
					.setUv(h[var9 + 3], h[var9 + 4])
					.setOverlay(var3)
					.setLight(var2)
					.setNormal(var5, h[var7 + 20], h[var7 + 21], h[var7 + 22]);
			}

			var6++;
		}
	}
}
