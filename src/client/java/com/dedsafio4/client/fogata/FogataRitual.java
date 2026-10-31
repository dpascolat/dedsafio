package com.dedsafio4.client.fogata;

import com.dedsafio4.revivir.FogataEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.resources.sounds.SoundInstance;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Los efectos del ritual de la Fogata (FogataEntity): todo lo que se ve alrededor y arriba, el cielo y los sonidos.
 * Animación de NavithLabs / AlieRequiem, usada con su permiso.
 */
public final class FogataRitual {
	private static final ResourceLocation a = a("blanco");
	private static final ResourceLocation b = a("brillo");
	private static final ResourceLocation c = a("haz");
	private static final ResourceLocation d = a("chispa");
	private static final ResourceLocation e = a("cinta");
	private static final ResourceLocation f = a("centro");
	private static final ResourceLocation g = a("aros");
	private static final ResourceLocation h = a("grieta");
	private static final float i = 120.0F;
	private static final float j = 53.8F;
	private static final float k = 60.0F;
	private static final float l = 56.9F;
	private static final float m = 1.0F;
	private static final ResourceLocation n = ResourceLocation.withDefaultNamespace("block/soul_campfire_fire");
	private static final SoundEvent o = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("dedsafio4", "fogata.revivir"));
	private static final SoundEvent p = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("dedsafio4", "fogata.revivido"));
	private static final float q = 30.59F;
	private static final int r = 15728880;
	private static final int s = OverlayTexture.NO_OVERLAY;
	private static final float t = (float) (Math.PI / 180.0);
	private static final float u = 33.15F;
	private static final float v = 1500.0F;
	private static final float w = 64.0F;
	private static final float x = 15.2F;
	private static final int y = 22;
	private static final ResourceLocation[] z = new ResourceLocation[12];
	private static final int A = 13630307;
	private static final int B = 5237745;
	private static final int C = 10944314;
	private static final int D = 4194289;
	private static final List<FogataEntity> E;
	private static final Map<String, Long> F;
	private static final Map<String, Long> G;
	private static final List<FogataRitual.a> H;
	private static final float I = 45.0F;
	private static final PoseStack J;
	private static final Vector3f K;
	private static final Vector3f L;
	private static final Map<String, CompletableFuture<PlayerSkin>> M;
	private static final Matrix4f N;
	private static final float O = 0.4F;
	private static final FogataRitual.b P;
	private static final FogataRitual.b Q;
	private static final float[][] R;
	private static final int[] S;
	private static final float[] T;
	private static final float U = 6.93F;
	private static final float V = 0.8F;
	private static final float W = 1.6F;
	private static final float X = 110.0F;
	private static final float Y = 60.0F;
	private static final float Z = 14.0F;
	private static final float aa = 7.2F;
	private static final float ab = 0.41887903F;
	private static final float[] ac;
	private static final float ad = 2.0F;
	private static final int[] ae;
	private static final float[] af;
	private static final float[] ag;
	private static final float[] ah;
	private static final float[] ai;
	private static final float aj = 2.4F;
	private static final float[][] ak;
	private static final float[][] al;

	private static ResourceLocation a(String var0) {
		return ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/fogata/revivir/" + var0 + ".png");
	}

	private FogataRitual() {
	}

	public static void a(FogataEntity var0) {
		if (!E.contains(var0)) {
			E.add(var0);
		}
	}

	private static Matrix4f a(Matrix4f var0) {
		float var1 = 0.999999F;
		return N.set(var0).m02(var0.m03() * var1).m12(var0.m13() * var1).m22(var0.m23() * var1).m32(var0.m33() * var1);
	}

	public static float a(float var0) {
		if (H.isEmpty()) {
			return 0.0F;
		} else {
			Minecraft var1 = Minecraft.getInstance();
			if (var1.level == null) {
				return 0.0F;
			} else {
				String var2 = var1.level.dimension().location().toString();
				float var3 = 0.0F;

				for (int var4 = 0; var4 < H.size(); var4++) {
					FogataRitual.a var5 = H.get(var4);
					if (var5.f().equals(var2)) {
						float var6 = ((float)(var1.level.getGameTime() - var5.e()) + var0) / 20.0F;
						var3 = Math.max(var3, 0.4F * d((var6 - 0.14F) / 1.6F) * (1.0F - d((var6 - 30.6F) / 2.4F)));
					}
				}

				return var3;
			}
		}
	}

	public static float b(float var0) {
		return 0.5F * a(var0) / 0.4F;
	}

	public static Vector4f a(Vector4f var0, float var1) {
		float var2 = a(var1);
		return var2 <= 0.0F ? var0 : new Vector4f(var0.x * (1.0F - var2), var0.y * (1.0F - var2), var0.z * (1.0F - var2), var0.w);
	}

	private static boolean b(String var0) {
		for (int var1 = 0; var1 < E.size(); var1++) {
			if (E.get(var1).getStringUUID().equals(var0)) {
				return true;
			}
		}

		return false;
	}

	public static void cielo(com.dedsafio4.revivir.Fogata.CieloPayload var0) {
		Minecraft var1 = Minecraft.getInstance();
		String var2 = var0.id();
		H.removeIf(var1x -> var1x.a().equals(var2));
		long var3 = var0.desde();
		if (var3 >= 0L && var1.level != null) {
			H.add(
				new FogataRitual.a(
					var2, var0.x(), var0.y(), var0.z(), var3, var1.level.dimension().location().toString()
				)
			);
		}
	}

	public static void a(Minecraft var0) {
		if (!H.isEmpty() && var0.level != null && var0.player != null) {
			String var1 = var0.level.dimension().location().toString();

			for (int var2 = H.size() - 1; var2 >= 0; var2--) {
				FogataRitual.a var3 = H.get(var2);
				float var4 = (float)(var0.level.getGameTime() - var3.e()) / 20.0F;
				if (!(var4 >= 33.15F) && var3.f().equals(var1)) {
					Long var5 = F.get(var3.a());
					if ((var5 == null || var5 != var3.e()) && var4 >= 0.0F && var4 < 1.0F) {
						F.put(var3.a(), var3.e());
						a(var0, o, var3);
					}

					Long var6 = G.get(var3.a());
					if ((var6 == null || var6 != var3.e()) && var4 >= 30.59F && var4 < 31.59F) {
						G.put(var3.a(), var3.e());
						a(var0, p, var3);
					}
				} else {
					H.remove(var2);
				}
			}
		}
	}

	private static void a(Minecraft var0, SoundEvent var1, FogataRitual.a var2) {
		double var3 = Math.sqrt(var0.player.distanceToSqr(var2.b(), var2.c() + 2.0, var2.d()));
		if (var3 <= 45.0) {
			var0.getSoundManager()
				.play(new SimpleSoundInstance(var1, SoundSource.BLOCKS, 1.0F, 1.0F, RandomSource.create(), var2.b(), var2.c() + 2.0, var2.d()));
		} else {
			float var5 = (float)Math.max(0.12, 0.3 - 1.8E-4 * (var3 - 45.0));
			var0.getSoundManager()
				.play(
					new SimpleSoundInstance(
						var1.getLocation(),
						SoundSource.BLOCKS,
						var5,
						1.0F,
						RandomSource.create(),
						false,
						0,
						SoundInstance.Attenuation.NONE,
						var2.b(),
						var2.c() + 2.0,
						var2.d(),
						false
					)
				);
		}
	}

	public static void a() {
		E.clear();
		F.clear();
		G.clear();
		H.clear();
		FogataTitere.a();
	}

	private static float c(FogataEntity var0, float var1) {
		Minecraft var2 = Minecraft.getInstance();
		return ((float)(var2.level.getGameTime() - var0.desde()) + var1) / 20.0F;
	}

	private static float d(float var0) {
		var0 = Mth.clamp(var0, 0.0F, 1.0F);
		return var0 * var0 * (3.0F - 2.0F * var0);
	}

	private static float a(float var0, float var1, float var2, float var3, float var4) {
		if (var0 <= var1 || var0 >= var4) {
			return 0.0F;
		} else if (var0 < var2) {
			return d((var0 - var1) / (var2 - var1));
		} else {
			return var0 > var3 ? 1.0F - d((var0 - var3) / (var4 - var3)) : 1.0F;
		}
	}

	private static float a(int var0, int var1) {
		int var2 = var0 * 73856093 ^ var1 * 19349663;
		var2 ^= var2 >>> 13;
		var2 *= 1540483477;
		var2 ^= var2 >>> 15;
		return (float)(var2 & 16777215) / 1.6777216E7F;
	}

	private static int a(int var0, float var1) {
		var1 = Mth.clamp(var1, 0.0F, 1.0F);
		int var2 = (int)((float)(var0 >> 16 & 0xFF) * var1);
		int var3 = (int)((float)(var0 >> 8 & 0xFF) * var1);
		int var4 = (int)((float)(var0 & 0xFF) * var1);
		return 0xFF000000 | var2 << 16 | var3 << 8 | var4;
	}

	private static int a(int var0, int var1, float var2) {
		var2 = Mth.clamp(var2, 0.0F, 1.0F);
		int var3 = Mth.lerpInt(var2, var0 >> 16 & 0xFF, var1 >> 16 & 0xFF);
		int var4 = Mth.lerpInt(var2, var0 >> 8 & 0xFF, var1 >> 8 & 0xFF);
		int var5 = Mth.lerpInt(var2, var0 & 0xFF, var1 & 0xFF);
		return var3 << 16 | var4 << 8 | var5;
	}

	private static void a(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, int var5, float var6, float var7) {
		var0.addVertex(var1, var2, var3, var4)
			.setColor(var5)
			.setUv(var6, var7)
			.setOverlay(s)
			.setLight(15728880)
			.setNormal(var1, 0.0F, 1.0F, 0.0F);
	}

	private static void a(
		VertexConsumer var0,
		PoseStack.Pose var1,
		float var2,
		float var3,
		float var4,
		float var5,
		float var6,
		float var7,
		float var8,
		float var9,
		float var10,
		float var11,
		float var12,
		float var13,
		int var14,
		int var15,
		int var16,
		int var17,
		float var18,
		float var19,
		float var20,
		float var21
	) {
		a(var0, var1, var2, var3, var4, var14, var18, var20);
		a(var0, var1, var5, var6, var7, var15, var19, var20);
		a(var0, var1, var8, var9, var10, var16, var19, var21);
		a(var0, var1, var11, var12, var13, var17, var18, var21);
		a(var0, var1, var11, var12, var13, var17, var18, var21);
		a(var0, var1, var8, var9, var10, var16, var19, var21);
		a(var0, var1, var5, var6, var7, var15, var19, var20);
		a(var0, var1, var2, var3, var4, var14, var18, var20);
	}

	private static void a(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, int var7) {
		float var8 = Mth.cos(var6) * var5;
		float var9 = Mth.sin(var6) * var5;
		float var10 = K.x * var8 + L.x * var9;
		float var11 = K.y * var8 + L.y * var9;
		float var12 = K.z * var8 + L.z * var9;
		float var13 = -K.x * var9 + L.x * var8;
		float var14 = -K.y * var9 + L.y * var8;
		float var15 = -K.z * var9 + L.z * var8;
		a(
			var0,
			var1,
			var2 - var10 - var13,
			var3 - var11 - var14,
			var4 - var12 - var15,
			var2 + var10 - var13,
			var3 + var11 - var14,
			var4 + var12 - var15,
			var2 + var10 + var13,
			var3 + var11 + var14,
			var4 + var12 + var15,
			var2 - var10 + var13,
			var3 - var11 + var14,
			var4 - var12 + var15,
			var7,
			var7,
			var7,
			var7,
			0.0F,
			1.0F,
			1.0F,
			0.0F
		);
	}

	private static void b(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, int var7) {
		float var8 = K.x * var5;
		float var9 = K.y * var5;
		float var10 = K.z * var5;
		float var11 = L.x * var6;
		float var12 = L.y * var6;
		float var13 = L.z * var6;
		a(
			var0,
			var1,
			var2 - var8 - var11,
			var3 - var9 - var12,
			var4 - var10 - var13,
			var2 + var8 - var11,
			var3 + var9 - var12,
			var4 + var10 - var13,
			var2 + var8 + var11,
			var3 + var9 + var12,
			var4 + var10 + var13,
			var2 - var8 + var11,
			var3 - var9 + var12,
			var4 - var10 + var13,
			var7,
			var7,
			var7,
			var7,
			0.0F,
			1.0F,
			1.0F,
			0.0F
		);
	}

	private static void a(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8) {
		float var9 = Mth.sqrt(K.x * K.x + K.z * K.z);
		float var10 = var9 > 0.001F ? K.x / var9 * var6 : var6;
		float var11 = var9 > 0.001F ? K.z / var9 * var6 : 0.0F;
		a(
			var0,
			var1,
			var2 - var10,
			var3,
			var5 - var11,
			var2 + var10,
			var3,
			var5 + var11,
			var2 + var10,
			var4,
			var5 + var11,
			var2 - var10,
			var4,
			var5 - var11,
			var7,
			var7,
			var8,
			var8,
			0.0F,
			1.0F,
			0.0F,
			1.0F
		);
	}

	private static void a(
		VertexConsumer var0,
		PoseStack.Pose var1,
		float var2,
		float var3,
		float var4,
		float var5,
		float var6,
		int var7,
		FogataRitual.b var8,
		FogataRitual.b var9,
		int var10,
		float var11,
		float var12
	) {
		float var13 = 0.0F;
		float var14 = 1.0F;
		float var15 = 0.0F;
		float var16 = 1.0F + var11 * Mth.sin((float)var10 * (var13 - var12));

		for (int var17 = 1; var17 <= var7; var17++) {
			float var18 = (float) (Math.PI * 2) * (float)var17 / (float)var7;
			float var19 = Mth.cos(var18);
			float var20 = Mth.sin(var18);
			float var21 = 1.0F + var11 * Mth.sin((float)var10 * (var18 - var12));
			int var22 = var8.a(var13);
			int var23 = var8.a(var18);
			int var24 = var9.a(var13);
			int var25 = var9.a(var18);
			a(
				var0,
				var1,
				var2 + var14 * var5 * var16,
				var3,
				var4 + var15 * var5 * var16,
				var2 + var19 * var5 * var21,
				var3,
				var4 + var20 * var5 * var21,
				var2 + var19 * var6 * var21,
				var3,
				var4 + var20 * var6 * var21,
				var2 + var14 * var6 * var16,
				var3,
				var4 + var15 * var6 * var16,
				var22,
				var23,
				var25,
				var24,
				0.5F,
				0.5F,
				0.5F,
				0.5F
			);
			var13 = var18;
			var14 = var19;
			var15 = var20;
			var16 = var21;
		}
	}

	private static void a(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, int var7, FogataRitual.b var8) {
		float var9 = 0.0F;
		float var10 = 1.0F;
		float var11 = 0.0F;

		for (int var12 = 1; var12 <= var7; var12++) {
			float var13 = (float) (Math.PI * 2) * (float)var12 / (float)var7;
			float var14 = Mth.cos(var13);
			float var15 = Mth.sin(var13);
			int var16 = var8.a(var9);
			int var17 = var8.a(var13);
			a(
				var0,
				var1,
				var2 + var10 * var5,
				var3,
				var4 + var11 * var5,
				var2 + var14 * var5,
				var3,
				var4 + var15 * var5,
				var2 + var14 * var5,
				var3 + var6,
				var4 + var15 * var5,
				var2 + var10 * var5,
				var3 + var6,
				var4 + var11 * var5,
				var16,
				var17,
				var17,
				var16,
				0.5F,
				0.5F,
				0.5F,
				0.5F
			);
			var9 = var13;
			var10 = var14;
			var11 = var15;
		}
	}

	public static void a(WorldRenderContext var0) {
		if (!E.isEmpty()) {
			Minecraft var1 = Minecraft.getInstance();
			if (var1.level == null) {
				E.clear();
			} else {
				float var2 = var0.tickCounter().getGameTimeDeltaPartialTick(false);
				Camera var3 = var0.camera();
				Vec3 var4 = var3.getPosition();
				K.set(var3.getLeftVector());
				L.set(var3.getUpVector());
				J.setIdentity();
				MultiBufferSource.BufferSource var5 = var1.renderBuffers().bufferSource();

				for (int var6 = E.size() - 1; var6 >= 0; var6--) {
					FogataEntity var7 = E.get(var6);
					float var8 = !var7.isRemoved() && var7.reviviendo() && var7.level() == var1.level ? c(var7, var2) : -1.0F;
					if (!(var8 < 0.0F) && !(var8 > 33.15F)) {
						float var9 = (float)(var7.getX() - var4.x);
						float var10 = (float)(var7.getY() - var4.y);
						float var11 = (float)(var7.getZ() - var4.z);
						PoseStack.Pose var12 = J.last();
						d(var5, var12, var9, var10, var11, var8);
						b(var5, var12, var9, var10, var11, var8);
						a(var5, var12, var7, var9, var10, var11, var8);
						f(var5, var12, var9, var10, var11, var8);
						g(var5, var12, var9, var10, var11, var8);
						i(var5, var12, var9, var10, var11, var8);
						h(var5, var12, var9, var10, var11, var8);
						e(var5, var12, var9, var10, var11, var8);
						if (var8 >= 16.6F && var8 < 30.42F) {
							FogataTitere.a(var5, var7, b(var7), var9, var10, var11, var8);
						}
					} else {
						E.remove(var6);
						FogataTitere.a(var7);
					}
				}

				var5.endBatch();
			}
		}
	}

	public static void b(WorldRenderContext var0) {
		if (!E.isEmpty() || !H.isEmpty()) {
			Minecraft var1 = Minecraft.getInstance();
			if (var1.level != null) {
				float var2 = var0.tickCounter().getGameTimeDeltaPartialTick(false);
				Vec3 var3 = var0.camera().getPosition();
				String var4 = var1.level.dimension().location().toString();
				FogataRitual.a var5 = null;
				double var6 = Double.MAX_VALUE;

				for (int var8 = 0; var8 < H.size(); var8++) {
					FogataRitual.a var9 = H.get(var8);
					float var10 = ((float)(var1.level.getGameTime() - var9.e()) + var2) / 20.0F;
					if (!(var10 < 0.14F) && !(var10 >= 33.15F) && var9.f().equals(var4)) {
						double var11 = var9.b() - var3.x;
						double var13 = var9.c() - var3.y;
						double var15 = var9.d() - var3.z;
						double var17 = var11 * var11 + var13 * var13 + var15 * var15;
						if (var17 < var6) {
							var6 = var17;
							var5 = var9;
						}
					}
				}

				FogataEntity var28 = null;
				double var29 = 4096.0;

				for (FogataEntity var12 : E) {
					if (!var12.isRemoved() && var12.reviviendo()) {
						float var33 = c(var12, var2);
						if (!(var33 < 0.14F) && !(var33 >= 33.15F)) {
							double var14 = var12.distanceToSqr(var3);
							if (var14 < var29) {
								var29 = var14;
								var28 = var12;
							}
						}
					}
				}

				if (var28 != null || var5 != null) {
					float var31 = var28 != null ? c(var28, var2) : 0.0F;
					float var32 = var28 != null ? (float)(var28.getX() - var3.x) : 0.0F;
					float var34 = var28 != null ? (float)(var28.getY() - var3.y) : 0.0F;
					float var35 = var28 != null ? (float)(var28.getZ() - var3.z) : 0.0F;
					float var36 = var5 != null ? ((float)(var1.level.getGameTime() - var5.e()) + var2) / 20.0F : var31;
					float var16 = var5 != null ? (float)(var5.b() - var3.x) : var32;
					float var37 = var5 != null ? (float)(var5.c() - var3.y) : var34;
					float var18 = var5 != null ? (float)(var5.d() - var3.z) : var35;
					float var19 = 1500.0F;
					float var20 = 90.0F;
					Matrix4fStack var21 = RenderSystem.getModelViewStack();
					var21.pushMatrix();
					var21.identity();
					var21.mul(var0.positionMatrix());
					RenderSystem.applyModelViewMatrix();
					J.setIdentity();
					MultiBufferSource.BufferSource var22 = var1.renderBuffers().bufferSource();
					float var23 = RenderSystem.getShaderFogStart();
					RenderSystem.setShaderFogStart(Float.MAX_VALUE);
					J.pushPose();
					float var24 = Math.max(var37 + var19, Mth.sqrt(var16 * var16 + (var37 + var19) * (var37 + var19) + var18 * var18));
					float var25 = var24 > var20 && var20 > 8.0F ? var20 / var24 : 1.0F;
					J.scale(var25, var25, var25);
					RenderSystem.backupProjectionMatrix();
					RenderSystem.setProjectionMatrix(a(RenderSystem.getProjectionMatrix()), RenderSystem.getVertexSorting());
					a(var22, J.last(), var16, var37 + var19, var18, var19, var36);
					var22.endBatch();
					RenderSystem.restoreProjectionMatrix();
					J.popPose();
					if (var28 != null) {
						J.pushPose();
						float var26 = a(var34, var19, var31);
						float var27 = Math.max(var26, Mth.sqrt(var32 * var32 + var26 * var26 + var35 * var35));
						var25 = var27 > var20 && var20 > 8.0F ? var20 / var27 : 1.0F;
						J.scale(var25, var25, var25);
						b(var22, J.last(), var32, var34, var35, var19, var31);
						J.popPose();
						a(var22, J.last(), var32, var34, var35, var31);
					}

					if (var5 != null && !b(var5.a())) {
						float var40 = Mth.sqrt(var16 * var16 + var37 * var37 + var18 * var18) + 60.0F;
						J.pushPose();
						var25 = var40 > var20 && var20 > 8.0F ? var20 / var40 : 1.0F;
						J.scale(var25, var25, var25);
						b(var22, J.last(), var16, var37, var18, var36);
						J.popPose();
					}

					var22.endBatch();
					RenderSystem.setShaderFogStart(var23);
					var21.popMatrix();
					RenderSystem.applyModelViewMatrix();
				}
			}
		}
	}

	private static float e(float var0) {
		float var1 = Mth.positiveModulo(var0, (float) (Math.PI * 2)) / (float) (Math.PI / 180.0);
		float var2 = 0.0F;

		for (int var3 = 0; var3 < T.length; var3++) {
			if (var1 < T[var3] || var3 == T.length - 1) {
				float var4 = var3 % 2 == 1 ? 1.0F : 0.0F;
				float var5 = var3 == 0 ? T[var3] - var1 : (var3 == T.length - 1 ? var1 - var2 : Math.min(var1 - var2, T[var3] - var1));
				return var5 < 2.0F ? 0.5F + (var4 - 0.5F) * Math.max(0.0F, var5) / 2.0F : var4;
			}

			var2 = T[var3];
		}

		return 0.0F;
	}

	private static void a(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6) {
		float var7;
		float var8;
		float var9;
		if (var6 < 25.58F) {
			float var10 = Mth.clamp((var6 - 0.14F) / 1.82F, 0.0F, 1.0F);
			float var11 = d((var6 - 24.3F) / 1.28F);
			var8 = (1.0F - (float)Math.pow((double)(1.0F - var10), 1.5)) * (1.0F - 0.16F * var11);
			var7 = 1.0F - 0.56F * var11;
			var9 = d((var6 - 0.14F) / 0.4F) * (0.9F + 0.1F * d((var6 - 0.8F) / 1.1F));
		} else {
			var7 = 0.44F * (1.0F - (var6 - 25.58F) / 7.57F);
			var8 = Math.max(0.2F, 0.84F - 0.125F * (var6 - 25.58F));
			var9 = 1.0F;
		}

		if (!(var8 <= 0.002F) && !(var7 <= 0.004F)) {
			float var18 = -0.13962634F * var6;
			VertexConsumer var19 = var0.getBuffer(RenderType.eyes(a));

			for (int var12 = 0; var12 < R.length; var12++) {
				float[] var13 = R[var12];
				float var14 = var5
					* Mth.sin(var13[0] * (float) (Math.PI / 180.0))
					/ Mth.cos(var13[0] * (float) (Math.PI / 180.0))
					* var8;
				float var15 = var5
					* Mth.sin(var13[1] * (float) (Math.PI / 180.0))
					/ Mth.cos(var13[1] * (float) (Math.PI / 180.0))
					* var8;
				FogataRitual.b var16 = var12 == 1 ? Q.a(1, 13369228, 8123391, var13[2] * var7, var18) : Q.a(S[var12], var13[2] * var7);
				a(var19, var1, var2, var3, var4, var14, var15, 128, P.a(0, 0.0F), var16, 5, var13[3], var18);
			}

			float var20 = var5 * 0.344F * (0.5F + 0.5F * var8);
			int var21 = a(13695743, 0.55F * var7 * var9);
			VertexConsumer var22 = var0.getBuffer(RenderType.eyes(f));
			float var23 = var3 + 0.05F;
			a(
				var22,
				var1,
				var2 - var20,
				var23,
				var4 - var20,
				var2 + var20,
				var23,
				var4 - var20,
				var2 + var20,
				var23,
				var4 + var20,
				var2 - var20,
				var23,
				var4 + var20,
				var21,
				var21,
				var21,
				var21,
				0.0F,
				1.0F,
				0.0F,
				1.0F
			);
		}
	}

	private static void b(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6) {
		if (!(var6 < 2.3F) && !(var6 >= 6.93F)) {
			float var7 = d((var6 - 2.3F) / 0.65F);
			float var8 = (6.7F + 1.6F * d((var6 - 2.5F) / 3.0F)) * (float) (Math.PI / 180.0);
			float var9 = var5 * 0.97F * Mth.sin(var8 / 2.0F) / Mth.cos(var8 / 2.0F);
			float var10 = a(var3, var5, var6);
			VertexConsumer var11 = var0.getBuffer(RenderType.eyes(a));
			a(var11, var1, var2, var10, var4, var9, (float) (Math.PI / 9) * var6, 0.36F * var7);
			a(var11, var1, var2, var10 - 0.02F, var4, var9 * 0.66F, -0.61086524F * var6, 0.55F * var7);
		}
	}

	private static float a(float var0, float var1, float var2) {
		float var3 = var1 * 0.97F;
		return var2 > 6.15F ? var0 + Mth.lerp((float)Math.pow((double)Math.min(1.0F, (var2 - 6.15F) / 0.78F), 0.9F), var3, 2.5F) : var0 + var3;
	}

	private static void a(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, float var7) {
		float var8 = Mth.cos(var6);
		float var9 = Mth.sin(var6);
		a(var0, var1, var2, var3, var4, 0.7F * var5, var5, var5, var8, var9, 0.33F * var7, var7);
	}

	private static void a(
		VertexConsumer var0,
		PoseStack.Pose var1,
		float var2,
		float var3,
		float var4,
		float var5,
		float var6,
		float var7,
		float var8,
		float var9,
		float var10,
		float var11
	) {
		for (int var12 = 0; var12 < 4; var12++) {
			float var13 = var12 != 0 && var12 != 3 ? -1.0F : 1.0F;
			float var14 = var12 < 2 ? 1.0F : -1.0F;
			int var15 = (var12 + 1) % 4;
			float var16 = var15 != 0 && var15 != 3 ? -1.0F : 1.0F;
			float var17 = var15 < 2 ? 1.0F : -1.0F;
			float var18 = var13 * var8 - var14 * var9;
			float var19 = var13 * var9 + var14 * var8;
			float var20 = var16 * var8 - var17 * var9;
			float var21 = var16 * var9 + var17 * var8;
			a(
				var0,
				var1,
				var2 + var18 * var6,
				var3,
				var4 + var19 * var6,
				var2 + var20 * var6,
				var3,
				var4 + var21 * var6,
				var2 + var20 * var5,
				var3,
				var4 + var21 * var5,
				var2 + var18 * var5,
				var3,
				var4 + var19 * var5,
				a(var18 * var6, var19 * var6, var7, var11),
				a(var20 * var6, var21 * var6, var7, var11),
				a(var20 * var5, var21 * var5, var7, var10),
				a(var18 * var5, var19 * var5, var7, var10),
				0.5F,
				0.5F,
				0.5F,
				0.5F
			);
		}
	}

	private static int a(float var0, float var1, float var2, float var3) {
		float var4 = Mth.clamp(0.5F + 0.5F * (var0 + var1) / (1.41F * var2), 0.0F, 1.0F);
		return a(a(11599690, 4184575, var4), var3);
	}

	public static void a(GuiGraphics var0, DeltaTracker var1) {
		if (!E.isEmpty()) {
			Minecraft var2 = Minecraft.getInstance();
			if (var2.player != null && var2.level != null) {
				float var3 = var1.getGameTimeDeltaPartialTick(false);

				for (FogataEntity var5 : E) {
					float var6 = c(var5, var3);
					if (!(var6 < 6.95F) && !(var6 >= 7.07F) && !(var2.player.distanceToSqr(var5) > 1600.0)) {
						int var7 = var0.guiWidth();
						int var8 = var0.guiHeight();
						int var9 = Math.max(1, Math.round((float)var8 * d((var6 - 6.95F) / 0.08F)));
						var0.fillGradient(0, 0, var7, var9, 1555365698, 783613762);
						return;
					}
				}
			}
		}
	}

	private static void a(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		float var6 = 8.53F;
		if (!(var5 < 6.93F) && !(var5 >= var6)) {
			float var7 = FogataEntity.radioOnda(var5);
			float var8 = a(var5, 6.93F, 7.0499997F, 7.81F, var6);
			if (!(var8 <= 0.004F)) {
				VertexConsumer var9 = var0.getBuffer(RenderType.eyes(a));
				byte var10 = 96;
				float var11 = 0.0F;
				float var12 = b(0.0F, var7, var5);
				int var13 = a(0.0F, var8);
				int var14 = a(0.0F, 0.45F * var8);

				for (int var15 = 1; var15 <= var10; var15++) {
					float var16 = (float) (Math.PI * 2) * (float)var15 / (float)var10;
					float var17 = b(var16, var7, var5);
					int var18 = a(var16, var8);
					int var19 = a(var16, 0.45F * var8);
					float var20 = Mth.cos(var11);
					float var21 = Mth.sin(var11);
					float var22 = Mth.cos(var16);
					float var23 = Mth.sin(var16);
					float var24 = var2 + var20 * var12;
					float var25 = var4 + var21 * var12;
					float var26 = var2 + var22 * var17;
					float var27 = var4 + var23 * var17;
					float var28 = 33.0F;
					a(
						var9,
						var1,
						var24,
						var3,
						var25,
						var26,
						var3,
						var27,
						var26,
						var3 + var28,
						var27,
						var24,
						var3 + var28,
						var25,
						var13,
						var18,
						var19,
						var14,
						0.5F,
						0.5F,
						0.5F,
						0.5F
					);
					a(
						var9,
						var1,
						var24,
						var3 + var28,
						var25,
						var26,
						var3 + var28,
						var27,
						var26,
						var3 + 110.0F,
						var27,
						var24,
						var3 + 110.0F,
						var25,
						var14,
						var19,
						-16777216,
						-16777216,
						0.5F,
						0.5F,
						0.5F,
						0.5F
					);
					var11 = var16;
					var12 = var17;
					var13 = var18;
					var14 = var19;
				}
			}
		}
	}

	private static float b(float var0, float var1, float var2) {
		return var1 * (1.0F + 0.06F * Mth.sin(5.0F * var0 + 3.8F * var2) + 0.04F * Mth.sin(11.0F * var0 - 5.2F * var2));
	}

	private static int a(float var0, float var1) {
		float var2 = 0.5F + 0.5F * Mth.sin(var0 * 37.0F + 2.1F * Mth.sin(var0 * 6.0F));
		float var3 = d((Mth.sin(var0 * 3.0F + 1.2F) + 0.2F) / 0.8F);
		return a(a(7274337, 3137791, var3), var1 * (0.35F + 0.65F * var2));
	}

	private static void b(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		float var6 = var5 < 7.74F ? a(var5, 7.0F, 7.1F, 7.6F, 7.74F) : (var5 >= 25.58F && var5 < 28.0F ? 1.0F - d((var5 - 26.0F) / 2.0F) : 0.0F);
		if (!(var6 <= 0.004F)) {
			a(var0.getBuffer(RenderType.eyes(a)), var1, var2, var3, var4, 13.0F, 0.0F, 60.0F, 48, a(13695743, 0.015F * var6), -16777216);
		}
	}

	private static void c(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 25.5F)) {
			float var6 = Mth.clamp((var5 - 25.5F) / 0.8F, 0.0F, 1.0F);
			float var7 = 7.2F * (1.0F - (1.0F - var6) * (1.0F - var6) * (1.0F - var6));
			int var8 = Math.round(255.0F * (1.0F - d((var5 - 32.6F) / 0.55F)));
			if (!(var7 <= 0.01F) && var8 > 2) {
				VertexConsumer var9 = var0.getBuffer(RenderType.entityTranslucentEmissive(h));
				int var10 = var8 << 24 | 16777215;
				byte var11 = 64;
				float var12 = var3 + 0.035F;
				float var13 = var7;
				float var14 = 0.0F;

				for (int var15 = 1; var15 <= var11; var15++) {
					float var16 = (float) (Math.PI * 2) * (float)var15 / (float)var11;
					float var17 = var7 * Mth.cos(var16);
					float var18 = var7 * Mth.sin(var16);
					a(var9, var1, var2, var12, var4, var10, 0.5F, 0.5F);
					a(var9, var1, var2 + var13, var12, var4 + var14, var10, 0.5F + var13 / 14.0F, 0.5F + var14 / 14.0F);
					a(var9, var1, var2 + var17, var12, var4 + var18, var10, 0.5F + var17 / 14.0F, 0.5F + var18 / 14.0F);
					a(var9, var1, var2, var12, var4, var10, 0.5F, 0.5F);
					var13 = var17;
					var14 = var18;
				}
			}
		}
	}

	private static void a(
		VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8, int var9, int var10
	) {
		if (!(var7 <= var6)) {
			float var11 = 1.0F;
			float var12 = 0.0F;
			float var13 = var3 + var6;
			float var14 = var3 + var7;

			for (int var15 = 1; var15 <= var8; var15++) {
				float var16 = (float) (Math.PI * 2) * (float)var15 / (float)var8;
				float var17 = Mth.cos(var16);
				float var18 = Mth.sin(var16);
				a(
					var0,
					var1,
					var2 + var11 * var5,
					var13,
					var4 + var12 * var5,
					var2 + var17 * var5,
					var13,
					var4 + var18 * var5,
					var2 + var17 * var5,
					var14,
					var4 + var18 * var5,
					var2 + var11 * var5,
					var14,
					var4 + var12 * var5,
					var9,
					var9,
					var10,
					var10,
					0.5F,
					0.5F,
					0.5F,
					0.5F
				);
				var11 = var17;
				var12 = var18;
			}
		}
	}

	private static float b(float var0, float var1) {
		return 0.5F + 0.5F * Mth.sin(8.0F * (var0 - 0.41887903F * var1));
	}

	private static void d(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 7.58F) && !(var5 >= 33.15F)) {
			c(var0, var1, var2, var3, var4, var5);
			if (!(var5 > 26.4F)) {
				VertexConsumer var6 = var0.getBuffer(RenderType.eyes(a));
				float var7 = var3 + 0.04F;
				float var8 = Mth.lerp(d((var5 - 7.58F) / 0.42F), 4.9F, 7.0F);
				float var9 = d((var5 - 7.58F) / 0.15F);
				a(var6, var1, var2, var3, var4, var8, 0.3F, 1.0F, var5, var9 * (1.0F - d((var5 - 24.2F) / 0.95F)));
				a(var6, var1, var2, var7, var4, var8, 0.15F, 96, Q.a(2, 9502704, 13172656, 0.7F * var9 * (1.0F - d((var5 - 24.6F) / 1.8F)), var5));
				if (!(var5 < 8.05F)) {
					float var10 = 1.0F - d((var5 - 24.4F) / 1.0F);
					float var11 = d((var5 - 8.05F) / 0.55F);
					float var12 = 3.25F * Mth.lerp(var11, 0.3F, 1.0F);
					a(
						var6,
						var1,
						var2,
						var7 + 0.01F,
						var4,
						var12 - 0.05F,
						var12 + 0.05F,
						72,
						P.a(2, 4194289, 10944314, 0.8F * var11 * var10, var5),
						Q.a(2, 4194289, 10944314, 0.8F * var11 * var10, var5),
						0,
						0.0F,
						0.0F
					);
					var11 = d((var5 - 8.6F) / 0.55F);
					float var13 = (5.4F + 0.6F * Mth.sin((float) (Math.PI * 2) * var5 / 4.1F)) * Mth.lerp(var11, 0.3F, 1.0F);
					a(
						var6,
						var1,
						var2,
						var7 + 0.01F,
						var4,
						Math.max(0.0F, var13 - 0.75F),
						var13 + 0.75F,
						72,
						P.a(2, 4194289, 10944314, 0.08F * var11 * var10, var5),
						Q.a(2, 4194289, 10944314, 0.35F * var11 * var10, var5),
						0,
						0.0F,
						0.0F
					);
					var11 = d((var5 - 9.35F) / 0.55F);
					if (var11 > 0.0F) {
						float var14 = Mth.lerp(var11, 0.6F, 1.0F);
						a(
							var6,
							var1,
							var2,
							var7 + 0.01F,
							var4,
							10.8F * var14,
							12.3F * var14,
							96,
							P.a(2, 4194289, 10944314, 0.08F * var11 * var10, var5),
							Q.a(2, 4194289, 10944314, 0.35F * var11 * var10, var5),
							0,
							0.0F,
							0.0F
						);
					}

					a(var6, var1, var2, var3, var4, 18.5F, 0.8F, 2.5F, var5, d((var5 - 11.13F) / 0.4F) * (1.0F - d((var5 - 24.2F) / 0.95F)));

					for (float var17 : ac) {
						float var18 = (var5 - var17) / 2.0F;
						if (!(var18 <= 0.0F) && !(var18 >= 1.0F)) {
							float var19 = 2.0F + 24.0F * (1.0F - (1.0F - var18) * (1.0F - var18));
							float var20 = a(var18, 0.0F, 0.06F, 0.5F, 1.0F);
							a(
								var6,
								var1,
								var2,
								var7 + 0.02F,
								var4,
								Math.max(0.0F, var19 - 1.5F),
								var19,
								128,
								P.a(2, 4194289, 10944314, 0.08F * var20, var5),
								Q.a(2, 4194289, 10944314, 0.35F * var20, var5),
								0,
								0.0F,
								0.0F
							);
						}
					}

					if (var5 >= 11.13F) {
						float var24 = Mth.clamp((var5 - 11.13F) / 0.27F, 0.0F, 1.0F);
						float var25 = 18.5F + 88.5F * (float)Math.pow((double)(1.0F - var24), 2.5);
						a(var6, var1, var2, var7, var4, var25, 0.15F, 128, Q.a(2, 9502704, 13172656, 0.7F * (1.0F - d((var5 - 24.6F) / 1.8F)), var5));
					}
				}
			}
		}
	}

	private static void a(VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
		if (!(var9 <= 0.004F)) {
			byte var10 = 96;
			float var11 = 0.0F;
			float var12 = 1.0F;
			float var13 = 0.0F;
			float var14 = b(0.0F, var8, var6, var7);
			int var15 = a(a(4194289, 10944314, b(0.0F, var8)), 0.8F * var9);

			for (int var16 = 1; var16 <= var10; var16++) {
				float var17 = (float) (Math.PI * 2) * (float)var16 / (float)var10;
				float var18 = Mth.cos(var17);
				float var19 = Mth.sin(var17);
				float var20 = b(var17, var8, var6, var7);
				int var21 = a(a(4194289, 10944314, b(var17, var8)), 0.8F * var9);
				a(
					var0,
					var1,
					var2 + var12 * var5,
					var3,
					var4 + var13 * var5,
					var2 + var18 * var5,
					var3,
					var4 + var19 * var5,
					var2 + var18 * var5,
					var3 + var20,
					var4 + var19 * var5,
					var2 + var12 * var5,
					var3 + var14,
					var4 + var13 * var5,
					var15,
					var21,
					-16777216,
					-16777216,
					0.5F,
					0.5F,
					0.5F,
					0.5F
				);
				var12 = var18;
				var13 = var19;
				var14 = var20;
				var15 = var21;
			}
		}
	}

	private static float b(float var0, float var1, float var2, float var3) {
		return var2 + (var3 - var2) * (0.5F + 0.5F * Mth.sin(3.0F * (var0 - 0.41887903F * var1)));
	}

	private static float b(float var0, float var1, float var2, float var3, float var4) {
		if (var0 <= var1) {
			return 0.0F;
		} else {
			float var5 = Math.min(1.0F, (var0 - var1) / (var2 - var1));
			float var6 = (var2 - var1) * (var5 * var5 * var5 - 0.5F * var5 * var5 * var5 * var5);
			if (var0 <= var2) {
				return var6;
			} else {
				var6 += Math.min(var0, var3) - var2;
				if (var0 <= var3) {
					return var6;
				} else {
					var5 = Math.min(1.0F, (var0 - var3) / (var4 - var3));
					return var6 + (var4 - var3) * (var5 - (var5 * var5 * var5 - 0.5F * var5 * var5 * var5 * var5));
				}
			}
		}
	}

	private static void e(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 6.93F) && !(var5 >= 33.15F)) {
			VertexConsumer var6 = var0.getBuffer(RenderType.entityTranslucentEmissive(g));
			float var7 = var5 - 6.93F;
			float var8 = 1.0F;
			float var9 = 1.0F;

			for (float[] var13 : ak) {
				var7 += var13[4] * b(var5, var13[0], var13[1], var13[2], var13[3]);
				var9 += var13[4] * a(var5, var13[0], var13[1], var13[2], var13[3]);
			}

			for (float[] var27 : al) {
				var8 += var27[4] * a(var5, var27[0], var27[1], var27[2], var27[3]);
			}

			float var22 = 0.14F + 0.06F * Mth.clamp((var9 - 1.0F) / 0.5F, 0.0F, 1.0F);
			float var24 = d((var5 - 32.15F) / 0.95F);
			float var26 = 1.0F - d((var5 - 32.95F) / 0.2F);
			float var28 = Mth.clamp((var5 - 6.93F) / 0.6F, 0.0F, 1.0F);
			float var14 = 1.0F - (1.0F - var28) * (1.0F - var28) * (1.0F - var28);
			int var15 = Math.round(255.0F * d((var5 - 6.93F) / 0.15F) * var26);

			for (int var16 = 0; var16 < 3 && var15 > 2; var16++) {
				float var17 = 1.0F + var22 * Mth.sin((float) (Math.PI * 2) * (var7 / 2.4F - (float)var16 / 3.0F));
				float var18 = Mth.lerp(var14, 0.4F, af[var16]) * 1.0F * var8 * var17 * (1.0F - 0.93F * var24);
				float var19 = (var14 * ag[var16] + 6.0F * Mth.sin(0.7F * var5 + (float)var16)) * (1.0F - var24);
				float var20 = Mth.lerp(var24, 2.2F + 0.25F * Mth.sin(1.1F * var5 + 2.0F * (float)var16), 0.9F);
				a(
					var6,
					var1,
					var2,
					var3 + var20,
					var4,
					((float)var16 * 120.0F + ai[var16] * var7) * (float) (Math.PI / 180.0),
					var19 * (float) (Math.PI / 180.0),
					var18,
					ah[var16] * var7 * (float) (Math.PI / 180.0),
					(float) (Math.PI * 2),
					ae[var16],
					var15
				);
			}

			if (var5 >= 15.2F) {
				int var29 = Math.round(255.0F * d((var5 - 15.2F) / 0.3F) * var26);
				float var30 = 1.0F - (float)Math.pow((double)(1.0F - Mth.clamp((var5 - 15.2F) / 0.5F, 0.0F, 1.0F)), 3.0);

				for (int var31 = 0; var31 < 2 && var29 > 2; var31++) {
					float var32 = 1.0F + var22 * Mth.sin((float) (Math.PI * 2) * (var7 / 1.6800001F - (float)var31 / 2.0F));
					a(
						var6,
						var1,
						var2,
						var3 + Mth.lerp(var24, 1.4F, 0.9F),
						var4,
						(var31 == 0 ? 60.0F : -45.0F) * var7 * (float) (Math.PI / 180.0),
						(var31 == 0 ? 90.0F : 70.0F) * (1.0F - var24) * (float) (Math.PI / 180.0),
						(var31 == 0 ? 1.5F : 1.9F) * 1.0F * var30 * var8 * var32 * (1.0F - 0.9F * var24),
						(var31 == 0 ? 300.0F : -260.0F) * var7 * (float) (Math.PI / 180.0),
						(float) (Math.PI * 2),
						var31,
						var29
					);
				}
			}
		}
	}

	private static void a(
		VertexConsumer var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int var10, int var11
	) {
		float var12 = Mth.cos(var5);
		float var13 = Mth.sin(var5);
		float var14 = Mth.cos(var6);
		float var15 = Mth.sin(var6);
		float var16 = -var13 * var14;
		float var17 = var15;
		float var18 = var12 * var14;
		float var19 = var7 * 53.8F / 56.9F;
		float var20 = var7 * 60.0F / 56.9F;
		int var21 = Math.max(8, (int)(160.0F * var9 / (float) (Math.PI * 2)));
		int var22 = var11 << 24 | 16777215;
		float var23 = 0.0F;
		float var24 = 0.0F;
		float var25 = 0.0F;
		float var26 = 0.0F;
		float var27 = 0.0F;
		float var28 = 0.0F;
		float var29 = 0.0F;
		float var30 = 0.0F;
		float var31 = 0.0F;
		float var32 = 0.0F;

		for (int var33 = 0; var33 <= var21; var33++) {
			float var34 = var8 + var9 * (float)var33 / (float)var21;
			float var35 = Mth.cos(var34);
			float var36 = Mth.sin(var34);
			float var37 = var12 * var35 + var16 * var36;
			float var38 = var17 * var36;
			float var39 = var13 * var35 + var18 * var36;
			float var40 = 60.0F;
			float var41 = ((float)var10 * 120.0F + var40 + 53.8F * var35) / 360.0F;
			float var42 = (var40 - 53.8F * var36) / 120.0F;
			float var43 = ((float)var10 * 120.0F + var40 + 60.0F * var35) / 360.0F;
			float var44 = (var40 - 60.0F * var36) / 120.0F;
			float var45 = var2 + var37 * var19;
			float var46 = var3 + var38 * var19;
			float var47 = var4 + var39 * var19;
			float var48 = var2 + var37 * var20;
			float var49 = var3 + var38 * var20;
			float var50 = var4 + var39 * var20;
			if (var33 > 0) {
				a(var0, var1, var23, var24, var25, var22, var29, var30);
				a(var0, var1, var45, var46, var47, var22, var41, var42);
				a(var0, var1, var48, var49, var50, var22, var43, var44);
				a(var0, var1, var26, var27, var28, var22, var31, var32);
			}

			var23 = var45;
			var24 = var46;
			var25 = var47;
			var26 = var48;
			var27 = var49;
			var28 = var50;
			var29 = var41;
			var30 = var42;
			var31 = var43;
			var32 = var44;
		}
	}

	private static void a(MultiBufferSource var0, PoseStack.Pose var1, FogataEntity var2, float var3, float var4, float var5, float var6) {
		if (!(var6 < 15.2F) && !(var6 >= 33.15F)) {
			TextureAtlasSprite var7 = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(n);
			VertexConsumer var8 = var0.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
			float var9 = d((var6 - 15.2F) / 1.6F);
			float var10 = var2.getYRot() * (float) (Math.PI / 180.0);
			float var11 = 1.875F * (0.35F + 0.65F * var9);
			float var12 = 3.75F * (0.25F + 0.75F * var9);
			float var13 = Mth.cos(var10) * var11;
			float var14 = Mth.sin(var10) * var11;
			float var15 = var7.getU0();
			float var16 = var7.getU1();
			float var17 = var7.getV0();
			float var18 = var7.getV1();
			a(
				var8,
				var1,
				var3 - var13,
				var4,
				var5 - var14,
				var3 + var13,
				var4,
				var5 + var14,
				var3 + var13,
				var4 + var12,
				var5 + var14,
				var3 - var13,
				var4 + var12,
				var5 - var14,
				-1,
				-1,
				-1,
				-1,
				var15,
				var16,
				var18,
				var17
			);
			a(
				var8,
				var1,
				var3 + var14,
				var4,
				var5 - var13,
				var3 - var14,
				var4,
				var5 + var13,
				var3 - var14,
				var4 + var12,
				var5 + var13,
				var3 + var14,
				var4 + var12,
				var5 - var13,
				-1,
				-1,
				-1,
				-1,
				var15,
				var16,
				var18,
				var17
			);
		}
	}

	private static void f(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 15.2F) && !(var5 >= 33.15F)) {
			if (Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas var6) {
				VertexConsumer var28 = var0.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_PARTICLES));
				float var8 = d((var5 - 15.2F) / 1.5F);

				for (int var9 = 0; var9 < 22; var9++) {
					float var10 = (var5 - 15.2F) / 6.0F + (float)var9 / 22.0F;
					int var11 = (int)Math.floor((double)var10);
					float var12 = var10 - (float)var11;
					if (!(var5 - 15.2F < var12 * 6.0F)) {
						float var13 = d(var12 / 0.08F) * (1.0F - d((var12 - 0.6F) / 0.4F)) * var8;
						if (!(var13 <= 0.02F)) {
							float var14 = (a(var9, var11 + 81) - 0.5F) * (1.0F + 1.6F * var12);
							float var15 = (a(var9, var11 + 82) - 0.5F) * (1.0F + 1.6F * var12);
							TextureAtlasSprite var16 = var6.getSprite(z[Math.min(11, (int)(var12 * 12.0F))]);
							int var17 = (int)(var13 * 200.0F) << 24 | a(9341828, 13223864, a(var9, var11 + 83));
							float var18 = 0.45F + 0.5F * var12;
							float var19 = var2 + var14;
							float var20 = var3 + 4.2F + 6.0F * var12;
							float var21 = var4 + var15;
							float var22 = K.x * var18;
							float var23 = K.y * var18;
							float var24 = K.z * var18;
							float var25 = L.x * var18;
							float var26 = L.y * var18;
							float var27 = L.z * var18;
							a(
								var28,
								var1,
								var19 - var22 - var25,
								var20 - var23 - var26,
								var21 - var24 - var27,
								var19 + var22 - var25,
								var20 + var23 - var26,
								var21 + var24 - var27,
								var19 + var22 + var25,
								var20 + var23 + var26,
								var21 + var24 + var27,
								var19 - var22 + var25,
								var20 - var23 + var26,
								var21 - var24 + var27,
								var17,
								var17,
								var17,
								var17,
								var16.getU0(),
								var16.getU1(),
								var16.getV1(),
								var16.getV0()
							);
						}
					}
				}
			}
		}
	}

	private static void g(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 6.7F) && !(var5 >= 33.15F)) {
			VertexConsumer var6 = var0.getBuffer(RenderType.eyes(d));

			for (int var7 = 0; var7 < 24; var7++) {
				float var8 = 0.6F + 0.4F * a(var7, 3);
				float var9 = (var5 - 6.7F) / var8 + a(var7, 5);
				int var10 = (int)Math.floor((double)var9);
				float var11 = Mth.sin((var9 - (float)var10) * (float) Math.PI);
				float var12 = a(var7 * 7 + var10, 1) * (float) (Math.PI * 2);
				float var13 = 1.5F + 4.0F * a(var7, var10 + 2);
				float var14 = 0.5F + 6.5F * a(var7, var10 + 3);
				float var15 = a(var7, var10 + 4);
				int var16 = var15 < 0.6F ? 15794175 : (var15 < 0.8F ? a(15794175, 5237745, 0.5F) : a(15794175, 13630307, 0.5F));
				a(
					var6,
					var1,
					var2 + Mth.cos(var12) * var13,
					var3 + var14,
					var4 + Mth.sin(var12) * var13,
					0.06F + 0.09F * a(var7, var10 + 6),
					0.0F,
					a(var16, 0.95F * var11)
				);
			}
		}
	}

	public static float a(FogataEntity var0, float var1) {
		return c(Math.min(c(var0, var1), 25.4F)) - 0.6F;
	}

	public static float b(FogataEntity var0, float var1) {
		float var2 = c(var0, var1);
		return var2 >= 16.8F && var2 < 25.45F ? var2 - 16.8F : -1.0F;
	}

	public static FogataEntity a(Vec3 var0) {
		Minecraft var1 = Minecraft.getInstance();
		if (!E.isEmpty() && var1.level != null) {
			for (FogataEntity var3 : E) {
				if (!var3.isRemoved() && var3.reviviendo() && !(var3.distanceToSqr(var0) > 1600.0)) {
					float var4 = c(var3, 0.0F);
					if (var4 >= 8.0F && var4 < 25.6F) {
						return var3;
					}
				}
			}

			return null;
		} else {
			return null;
		}
	}

	private static void h(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 15.2F) && !(var5 >= 33.15F)) {
			VertexConsumer var6 = var0.getBuffer(RenderType.eyes(a));
			int var7 = a(5237745, 0.9F * d((var5 - 15.2F) / 0.3F));
			a(var6, var1, var2, var3 + 0.8F, var3 + 4.8F, var4, 0.04F, var7, var7);
			if ((int)(var5 * 6.0F) % 3 == 0) {
				float var8 = Mth.sqrt(K.x * K.x + K.z * K.z);
				float var9 = var8 > 0.001F ? K.x / var8 * 0.2F : 0.2F;
				float var10 = var8 > 0.001F ? K.z / var8 * 0.2F : 0.0F;
				a(var6, var1, var2 + var9, var3 + 0.8F, var3 + 4.8F, var4 + var10, 0.03F, var7, var7);
			}
		}
	}

	static float c(float var0) {
		return 15.0F - 8.7F * (float)Math.exp((double)(-(var0 - 16.7F) / 3.8F));
	}

	private static void i(MultiBufferSource var0, PoseStack.Pose var1, float var2, float var3, float var4, float var5) {
		if (!(var5 < 16.8F) && !(var5 >= 25.45F)) {
			float var6;
			float var7;
			int var8;
			if (var5 < 19.4F) {
				var6 = 0.6F;
				var7 = 0.6F * d((var5 - 16.8F) / 0.2F);
				var8 = 13163676;
			} else {
				var6 = 1.0F + 0.35F * Mth.clamp((var5 - 20.5F) / 4.5F, 0.0F, 1.0F);
				var7 = 1.0F;
				var8 = 15265930;
			}

			var7 *= (1.0F + 0.12F * Mth.cos((float) (Math.PI * 2) * (var5 - 19.7F) / 1.15F)) * (1.0F - d((var5 - 25.35F) / 0.1F));
			if (!(var7 <= 0.004F)) {
				float var9 = var3 + c(var5) - 0.6F;
				VertexConsumer var10 = var0.getBuffer(RenderType.eyes(f));
				b(var10, var1, var2, var9, var4, 10.0F * var6, 9.0F * var6, a(var8, 0.55F * var7));
				a(var10, var1, var2, var9, var4, 3.6F * var6, 0.0F, a(var8, 0.6F * var7));
				a(var0.getBuffer(RenderType.eyes(b)), var1, var2, var9, var4, 0.9F * Math.max(var6, 0.8F), 0.0F, a(16645552, Math.min(1.0F, var7)));
				float var11 = a(var5, 21.8F, 22.3F, 24.6F, 25.35F);
				if (var11 > 0.004F) {
					VertexConsumer var12 = var0.getBuffer(RenderType.eyes(c));
					int var13 = a(11915374, 0.5F * var11);
					int var14 = a(14676128, 0.6F * var11);
					int var15 = -16777216;
					a(var12, var1, var2, var9, var9 + 14.0F, var4, 1.1F, var13, var15);
					a(var12, var1, var2, var3 + 4.8F, var9, var4, 1.1F, a(11915374, 0.25F * var11), var13);
					a(var12, var1, var2, var9 - 7.0F, var9, var4, 0.3F, var15, var14);
					a(var12, var1, var2, var9, var9 + 7.0F, var4, 0.3F, var14, var15);
				}
			}
		}
	}

	private static PlayerSkin b(FogataEntity var0) {
		return FogataSkins.skin(var0);
	}

	static {
		for (int var0 = 0; var0 < 12; var0++) {
			z[var0] = ResourceLocation.withDefaultNamespace("big_smoke_" + var0);
		}

		E = new ArrayList<>();
		F = new HashMap<>();
		G = new HashMap<>();
		H = new ArrayList<>();
		J = new PoseStack();
		K = new Vector3f();
		L = new Vector3f();
		M = new HashMap<>();
		N = new Matrix4f();
		P = new FogataRitual.b();
		Q = new FogataRitual.b();
		R = new float[][]{{20.2F, 21.6F, 0.15F, 0.02F}, {22.9F, 24.9F, 0.34F, 0.026F}, {25.5F, 28.2F, 0.09F, 0.06F}, {29.8F, 32.4F, 0.07F, 0.02F}};
		S = new int[]{10289137, 0, 11468764, 13041635};
		T = new float[]{52.0F, 86.0F, 138.0F, 189.0F, 241.0F, 298.0F, 350.0F, 359.0F, 360.0F};
		ac = new float[]{11.6F, 13.2F, 14.8F};
		ae = new int[]{1, 0, 1};
		af = new float[]{3.6F, 4.0F, 4.4F};
		ag = new float[]{65.0F, 35.0F, 80.0F};
		ah = new float[]{120.0F, -95.0F, 150.0F};
		ai = new float[]{45.0F, -55.0F, 38.0F};
		ak = new float[][]{
			{6.93F, 6.95F, 7.2F, 9.5F, 0.5F},
			{15.1F, 15.9F, 16.4F, 17.6F, 0.25F},
			{16.8F, 17.8F, 18.8F, 20.0F, -0.2F},
			{21.6F, 22.8F, 24.8F, 25.8F, 0.4F},
			{25.4F, 26.2F, 27.4F, 28.6F, -0.25F},
			{30.5F, 30.9F, 31.3F, 32.3F, 0.3F},
			{32.1F, 32.8F, 33.15F, 33.2F, 0.6F}
		};
		al = new float[][]{
			{16.8F, 17.8F, 25.0F, 25.6F, 0.2F}, {21.7F, 22.4F, 25.0F, 25.6F, 0.2F}, {25.6F, 26.2F, 29.8F, 30.4F, -0.15F}, {30.5F, 30.7F, 31.2F, 31.9F, 0.25F}
		};
	}

	private static record a(String a, double b, double c, double d, long e, String f) {
	}

	private static final class b {
		static final int a = 0;
		static final int b = 1;
		static final int c = 2;
		int d;
		int e;
		int f;
		float g;
		float h;

		FogataRitual.b a(int var1, float var2) {
			this.d = 0;
			this.e = var1;
			this.g = var2;
			return this;
		}

		FogataRitual.b a(int var1, int var2, int var3, float var4, float var5) {
			this.d = var1;
			this.e = var2;
			this.f = var3;
			this.g = var4;
			this.h = var5;
			return this;
		}

		int a(float var1) {
			switch (this.d) {
				case 1:
					return FogataRitual.a(FogataRitual.a(this.f, this.e, FogataRitual.e(var1 - this.h)), this.g);
				case 2:
					return FogataRitual.a(FogataRitual.a(this.e, this.f, FogataRitual.b(var1, this.h)), this.g);
				default:
					return FogataRitual.a(this.e, this.g);
			}
		}
	}
}
