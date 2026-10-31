package com.dedsafio4.client.fogata;

import com.dedsafio4.revivir.FogataEntity;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.math.Axis;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.resources.PlayerSkin.Model;

/**
 * La copia del jugador que hace el ritual de la Fogata: sale del fuego, da vueltas y cae.
 * Animación de NavithLabs / AlieRequiem, usada con su permiso.
 */
final class FogataTitere {
	private static final int d = 15728880;
	private static final int e = OverlayTexture.NO_OVERLAY;
	static final float a = 16.6F;
	static final float b = 27.87F;
	static final float c = 30.42F;
	private static final float f = 3.0F;
	private static final float g = 6.0F;
	private static final float h = 0.9375F;
	private static final float i = 0.703125F;
	private static final int j = 0;
	private static final int k = 1;
	private static final int l = 2;
	private static final int m = 3;
	private static final int n = 4;
	private static final int o = 5;
	private static final int p = 6;
	private static final int q = 7;
	private static final int r = 8;
	private static final int s = 9;
	private static final int t = 10;
	private static final int u = 11;
	private static final int v = 12;
	private static final int w = 13;
	private static final int x = 14;
	private static final int y = 15;
	private static final int z = 16;
	private static final int A = 17;
	private static final int B = 18;
	private static final int C = 19;
	private static final int D = 20;
	private static final int E = 21;
	private static final int F = 22;
	private static final int G = 18;
	private static final float[] H = new float[]{
		4.5F, 6.0F, 7.0F, 8.0F, 8.0F, 10.0F, 8.0F, 8.0F, 10.0F, 7.0F, 7.0F, 9.0F, 7.0F, 7.0F, 9.0F, 6.0F, 8.0F, 8.0F, 6.0F, 6.0F, 4.0F, 3.5F
	};
	private static final float[] I = new float[]{
		0.9F, 0.7F, 0.75F, 0.5F, 0.5F, 0.45F, 0.5F, 0.5F, 0.45F, 0.6F, 0.6F, 0.5F, 0.6F, 0.6F, 0.5F, 0.8F, 0.5F, 0.5F, 0.8F, 0.7F, 0.9F, 0.95F
	};
	private static final float[] J = a(
		0.2F, 0.65F, 0.55F, -0.95F, 0.3F, -2.0F, -0.9F, -0.3F, -2.05F, -1.7F, 0.12F, 2.15F, -1.55F, -0.12F, 2.05F, 0.0F, 0.0F, 0.0F
	);
	private static final float[] K = a(
		0.18F, -0.12F, -0.1F, -0.25F, 1.05F, -0.5F, -0.1F, -0.95F, -0.62F, -0.3F, 0.18F, 0.55F, 0.05F, -0.22F, 0.38F, 0.0F, 0.0F, 0.0F
	);
	private static final float[] L = a(
		0.05F, 0.25F, 0.3F, -0.4F, 0.5F, -1.0F, -0.35F, -0.55F, -0.95F, -0.5F, 0.12F, 0.85F, -0.3F, -0.12F, 0.7F, 0.0F, 0.0F, 0.0F
	);
	private static final float[] M = a(
		-0.15F, 0.45F, 0.35F, -0.95F, 0.45F, -0.45F, -0.85F, -0.5F, -0.5F, -0.6F, 0.1F, 0.35F, -0.5F, -0.1F, 0.45F, 0.0F, 0.0F, 0.0F
	);
	private static final float[] N = a(1.5F, -0.4F, -0.55F, -1.6F, 0.0F, -1.8F, -1.6F, 0.0F, -1.8F, 0.15F, 0.35F, 1.0F, 0.1F, -0.32F, 1.15F, 0.0F, 1.2F, -1.2F);
	private static final float[] O = a(1.5708F, 0.0F, -0.1F, -1.4F, 0.2F, -1.4F, -1.4F, -0.2F, -1.4F, 0.0F, 0.15F, 0.2F, 0.05F, -0.1F, 0.35F, 1.0F, 1.2F, -1.2F);
	private static final float[] P = a(1.5708F, 0.0F, -0.3F, 0.6F, 1.6F, -2.2F, 0.6F, -1.6F, -2.2F, 0.0F, 0.1F, 0.25F, 0.0F, -0.1F, 0.3F, 0.0F, 0.8F, -0.8F);
	private static final float[] Q = a(
		1.13F, -0.3F, -0.45F, -1.15F, 0.15F, -0.1F, -1.15F, -0.15F, -0.1F, 0.05F, 0.06F, 0.15F, 0.05F, -0.06F, 0.15F, 0.0F, 0.0F, 0.0F
	);
	private static final float[] R = a(
		0.12F, 0.2F, 0.5F, -0.8F, 0.0F, 0.0F, -0.1F, -0.12F, -0.35F, -1.35F, 0.08F, 1.35F, 0.12F, -0.06F, 1.35F, 0.0F, -0.4F, 0.0F
	);
	private static final float[] S = a(
		0.0F, -0.02F, -0.05F, 0.05F, 0.08F, -0.2F, -0.02F, -0.08F, -0.15F, -0.03F, 0.03F, 0.06F, 0.02F, -0.03F, 0.04F, 0.0F, 0.0F, 0.0F
	);
	private static final float[][] T = new float[][]{J, K, L, M, N, O, P, Q, R, S};
	private static final float[] U = new float[]{0.05F, 0.16F, 0.06F, 0.1F, 0.14F, 0.015F, 0.02F, 0.03F, 0.03F, 0.035F};
	private static final float[] V = new float[]{1.2F, 0.7F, 1.2F, 1.2F, 6.0F, 1.2F, 1.2F, 1.2F, 1.2F, 1.2F};
	private static final float[] W = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.13F, 0.14F, 0.23F, 0.47F, 0.703125F};
	private static final Map<FogataEntity, FogataTitere.a> X = new HashMap<>();
	private static final float[] Y = new float[T.length];
	private static final float[] Z = new float[22];
	private static final float[] aa = new float[3];
	private static final float[] ab = new float[3];
	private static final float[] ac = new float[3];
	private static final PoseStack ad = new PoseStack();
	private static FogataTitere.b ae;
	private static FogataTitere.b af;
	private static final float[] ag = new float[3];
	private static final Set<Direction> ah = EnumSet.allOf(Direction.class);
	private static final Set<Direction> ai = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
	private static final Set<Direction> aj = EnumSet.of(
		Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
	);
	private static final Set<Direction> ak = EnumSet.of(Direction.UP);

	private static float[] a(float... var0) {
		return var0;
	}

	private static void a(float var0, float[] var1) {
		float var2 = 1.0F - b((var0 - 16.9F) / 1.7F);
		var1[0] = var2;
		var1[1] = (1.0F - var2) * (1.0F - b((var0 - 25.3F) / 0.4F));
		var1[2] = a(var0, 25.3F, 25.6F, 25.6F, 25.9F);
		var1[3] = a(var0, 25.5F, 25.8F, 26.0F, 26.4F);
		var1[4] = b((var0 - 25.9F) / 1.0F) * (1.0F - b((var0 - 27.87F) / 0.12F));
		var1[5] = b((var0 - 27.87F) / 0.12F) * (1.0F - b((var0 - 28.2F) / 0.25F));
		var1[6] = a(var0, 28.05F, 28.3F, 28.35F, 28.6F);
		var1[7] = a(var0, 28.35F, 28.7F, 28.85F, 29.15F);
		var1[8] = a(var0, 28.9F, 29.35F, 29.6F, 30.0F);
		var1[9] = b((var0 - 29.6F) / 0.55F);
	}

	private static float a(float var0) {
		float var1 = Mth.clamp((var0 - 24.6F) / 0.8F, 0.0F, 1.0F);
		return (float) (Math.PI / 3) * (Math.min(var0, 24.6F) - 16.6F + 0.8F * (var1 - var1 * var1 * var1 + 0.5F * var1 * var1 * var1 * var1));
	}

	private static void a(float var0, float var1, float var2, float var3, float var4, float[] var5, float[] var6) {
		float var7 = Math.min(var0, 25.4F);
		float var8 = 3.0F * b((var7 - 16.6F) / 1.5F);
		float var9 = a(var0);
		float var10 = var8 * Mth.sin(var9);
		float var11 = var8 * Mth.cos(var9);
		float var12 = 0.22F * Mth.sin((float) (Math.PI * 2) * var0 / 2.8F) * b((var0 - 16.6F) / 1.5F) * (1.0F - b((var0 - 26.6F) / 0.5F));
		float var13 = FogataRitual.c(var7) - 1.8F + 0.703125F + var12 - 0.35F * b((var0 - 25.4F) / 0.7F);
		if (var0 >= 25.5F) {
			float var14 = b((var0 - 25.5F) / 0.6F);
			var10 = Mth.lerp(var14, var10, var3 * 6.5F);
			var11 = Mth.lerp(var14, var11, var4 * 6.5F);
		}

		if (var0 > 27.1F) {
			float var17 = Math.min(1.0F, (var0 - 27.1F) / 0.77000046F);
			float var15 = b(var17);
			var10 = Mth.lerp(var15, var10, var1);
			var11 = Mth.lerp(var15, var11, var2);
			var13 = Mth.lerp(var17 * var17, var13, W[5]);
		}

		if (var0 >= 27.87F) {
			a(var0, var5);
			float var18 = 0.0F;
			float var19 = 0.0F;

			for (int var16 = 5; var16 < T.length; var16++) {
				var18 += var5[var16];
				var19 += var5[var16] * W[var16];
			}

			float var20 = var0 - 27.87F;
			var13 = (var18 > 1.0E-4F ? var19 / var18 : W[5]) + 0.05F * (float)Math.exp((double)(-10.0F * var20)) * Mth.sin(22.0F * var20);
		}

		var6[0] = var10;
		var6[1] = var13;
		var6[2] = var11;
	}

	static void a(FogataEntity var0) {
		X.remove(var0);
	}

	static void a() {
		X.clear();
	}

	static void a(MultiBufferSource var0, FogataEntity var1, PlayerSkin var2, float var3, float var4, float var5, float var6) {
		if (!(var6 < 16.6F) && !(var6 >= 30.42F)) {
			float var7 = var1.lugarX();
			float var8 = var1.lugarZ();
			float var9 = Mth.sqrt(var7 * var7 + var8 * var8);
			float var10 = var9 > 0.001F ? var7 / var9 : 0.0F;
			float var11 = var9 > 0.001F ? var8 / var9 : 1.0F;
			FogataTitere.a var12 = X.computeIfAbsent(var1, var0x -> new FogataTitere.a());
			float var13 = 0.04F;
			a(var6 - var13, var7, var8, var10, var11, Y, aa);
			a(var6 + var13, var7, var8, var10, var11, Y, ac);
			a(var6, var7, var8, var10, var11, Y, ab);
			float var14 = (ac[0] - aa[0]) / (2.0F * var13);
			float var15 = (ac[1] - aa[1]) / (2.0F * var13);
			float var16 = (ac[2] - aa[2]) / (2.0F * var13);
			float var17 = (ac[0] - 2.0F * ab[0] + aa[0]) / (var13 * var13);
			float var18 = (ac[2] - 2.0F * ab[2] + aa[2]) / (var13 * var13);
			a(var12, var6, var7, var8, var10, var11, ab, var14, var15, var16, var17, var18);
			a(var12, var6);
			FogataTitere.b var19 = a(var2.model() == PlayerSkin.Model.SLIM);
			var19.a(var12.a);
			float var20 = b((var6 - 16.6F) / 0.3F) * (1.0F - b((var6 - 30.27F) / 0.15F));
			ad.setIdentity();
			ad.pushPose();
			ad.translate(var3 + ab[0], var4 + ab[1], var5 + ab[2]);
			ad.mulPose(Axis.YP.rotation(var12.a[21]));
			ad.mulPose(Axis.XP.rotation(var12.a[0]));
			ad.mulPose(Axis.ZP.rotation(var12.a[20]));
			ad.scale(1.0F, -1.0F, -1.0F);
			ad.scale(0.9375F, 0.9375F, 0.9375F);
			var19.a.render(ad, var0.getBuffer(RenderType.entityTranslucent(var2.texture())), 15728880, e, Math.round(255.0F * var20) << 24 | 16777215);
			ad.popPose();
		} else {
			X.remove(var1);
		}
	}

	private static void a(
		FogataTitere.a var0, float var1, float var2, float var3, float var4, float var5, float[] var6, float var7, float var8, float var9, float var10, float var11
	) {
		a(var1, Y);
		float var12 = 0.0F;
		float var13 = 0.0F;
		float var14 = 0.0F;

		for (int var15 = 0; var15 < T.length; var15++) {
			var12 += Y[var15];
		}

		if (var12 < 1.0E-4F) {
			var12 = 1.0F;
		}

		for (int var34 = 0; var34 < 22; var34++) {
			Z[var34] = 0.0F;
		}

		for (int var35 = 0; var35 < T.length; var35++) {
			float var16 = Y[var35] / var12;
			if (!(var16 <= 0.0F)) {
				for (int var17 = 0; var17 < 18; var17++) {
					Z[var17] = Z[var17] + var16 * T[var35][var17];
				}

				var13 += var16 * U[var35];
				var14 += var16 * V[var35];
			}
		}

		var0.d = var0.d + var14 * Math.max(0.0F, Math.min(0.1F, var1 - (var0.c < 0.0F ? var1 : var0.c)));

		for (int var36 = 1; var36 < 20; var36++) {
			float var38 = var0.d;
			Z[var36] = Z[var36]
				+ var13
					* (
						0.6F * Mth.sin(var38 * (0.9F + 0.37F * (float)var36) + 1.7F * (float)var36)
							+ 0.4F * Mth.sin(var38 * (1.6F + 0.23F * (float)var36) + 0.6F * (float)var36 * (float)var36)
					);
		}

		Z[0] = Z[0] + 0.3F * var13 * Mth.sin(var0.d * 0.8F + 2.0F);
		Z[1] = Z[1]
			+ 0.035F * Mth.sin((float) (Math.PI * 2) * var1 / 3.2F)
			+ Y[8] / var12 * 0.07F * Mth.sin((float) (Math.PI * 2) * var1 / 1.3F);
		float var37 = 0.02F * Mth.sin((float) (Math.PI * 2) * var1 / 3.2F);
		Z[4] = Z[4] + var37;
		Z[7] = Z[7] - var37;
		float var39 = a(var1);
		float var40 = Y[1] / var12;
		float var18 = var1 < 25.2F
			? (float)Math.atan2((double)(-Mth.sin(var39)), (double)(-Mth.cos(var39)))
				+ 0.12F * Mth.sin(0.5F * var1) * var40
			: (float)Math.atan2((double)(-var4), (double)(-var5));
		Z[21] = var18;
		Z[20] = 0.06F * Mth.sin(0.7F * var1) * var40;
		float var19 = -0.045F * var10 - 0.0012F * var7 * Math.abs(var7);
		float var20 = -0.0012F * var8 * Math.abs(var8);
		float var21 = -0.045F * var11 - 0.0012F * var9 * Math.abs(var9);
		float[] var22 = a(var0, var19, var20, var21);
		float var23 = Mth.clamp(var22[0], -0.7F, 0.7F);
		float var24 = Mth.clamp(var22[1], -0.7F, 0.7F);
		float var25 = Mth.clamp(var22[2], -0.7F, 0.7F);
		Z[3] = Z[3] - var25;
		Z[6] = Z[6] - var25;
		Z[4] = Z[4] + -var23 + 0.6F * var24;
		Z[7] = Z[7] + (-var23 - 0.6F * var24);
		Z[9] = Z[9] - 0.6F * var25;
		Z[12] = Z[12] - 0.6F * var25;
		Z[10] = Z[10] - 0.4F * var23;
		Z[13] = Z[13] - 0.4F * var23;
		Z[11] = Z[11] + 0.5F * Math.max(0.0F, -var25);
		Z[14] = Z[14] + 0.5F * Math.max(0.0F, -var25);
		Z[1] = Z[1] + 0.3F * var25;
		float var26 = FogataRitual.c(Math.min(var1, 25.4F));
		float var27 = 0.0F;
		float var28 = var26 - 0.6F;
		float var29 = 0.0F;
		float var30 = 0.85F * var40 + 0.5F * (Y[2] + Y[3]) / var12;
		if (var1 >= 25.9F && var1 < 27.87F) {
			var27 = var2;
			var28 = 0.0F;
			var29 = var3;
			var30 = 0.6F * Y[4] / var12;
		} else if (var1 >= 27.87F) {
			if (var1 < 29.2F) {
				var27 = var2 - var4 * 3.0F;
				var28 = 0.5F;
				var29 = var3 - var5 * 3.0F;
				var30 = 0.5F * (Y[6] + Y[7]) / var12;
			} else {
				var27 = 0.0F;
				var28 = 2.5F;
				var29 = 0.0F;
				var30 = 0.7F * Y[9] / var12;
				Z[15] = Z[15] + 0.5F * b((var1 - 30.05F) / 0.12F) * Mth.sin((var1 - 30.05F) * 6.0F);
			}
		}

		if (var30 > 0.004F) {
			float[] var31 = a(var0, var27 - var6[0], var28 - (var6[1] + 0.95F), var29 - var6[2]);
			float var32 = Mth.clamp(-((float)Math.atan2((double)var31[0], (double)var31[2])), -1.25F, 1.25F);
			float var33 = Mth.clamp(
				-((float)Math.atan2((double)var31[1], (double)Mth.sqrt(var31[0] * var31[0] + var31[2] * var31[2]))), -0.9F, 0.8F
			);
			Z[15] = Mth.lerp(var30, Z[15], var32);
			Z[2] = Mth.lerp(var30, Z[2], var33);
		}
	}

	private static float[] a(FogataTitere.a var0, float var1, float var2, float var3) {
		float var4 = Mth.cos(var0.a[21]);
		float var5 = Mth.sin(var0.a[21]);
		float var6 = var1 * var4 - var3 * var5;
		float var7 = var1 * var5 + var3 * var4;
		float var8 = Mth.cos(var0.a[0]);
		float var9 = Mth.sin(var0.a[0]);
		float var10 = var2 * var8 + var7 * var9;
		float var11 = -var2 * var9 + var7 * var8;
		float var12 = Mth.cos(var0.a[20]);
		float var13 = Mth.sin(var0.a[20]);
		ag[0] = var6 * var12 + var10 * var13;
		ag[1] = -var6 * var13 + var10 * var12;
		ag[2] = var11;
		return ag;
	}

	private static void a(FogataTitere.a var0, float var1) {
		float var2 = var0.c < 0.0F ? -1.0F : var1 - var0.c;
		boolean var3 = var0.c >= 0.0F && var0.c < 27.87F && var1 >= 27.87F;
		var0.c = var1;
		if (!(var2 < 0.0F) && !(var2 > 0.25F)) {
			if (var3) {
				var0.b[3] = var0.b[3] - 5.0F;
				var0.b[6] = var0.b[6] - 5.0F;
				var0.b[5] = var0.b[5] - 4.0F;
				var0.b[8] = var0.b[8] - 4.0F;
				var0.b[9] = var0.b[9] + 3.0F;
				var0.b[12] = var0.b[12] + 3.0F;
				var0.b[11] = var0.b[11] + 4.0F;
				var0.b[14] = var0.b[14] + 4.0F;
				var0.b[1] = var0.b[1] - 3.0F;
				var0.b[2] = var0.b[2] + 4.0F;
				var0.b[0]++;
			}

			float var12 = var1 >= 28.1F ? 1.35F : 1.0F;
			int var5 = Math.max(1, (int)Math.ceil((double)(var2 * 120.0F)));
			float var6 = var2 / (float)var5;

			for (int var7 = 0; var7 < var5; var7++) {
				for (int var8 = 0; var8 < 22; var8++) {
					float var9 = H[var8] * var12;
					float var10 = var1 >= 28.1F ? Math.max(I[var8], 0.75F) : I[var8];
					float var11 = Z[var8] - var0.a[var8];
					if (var8 == 21) {
						var11 = Mth.wrapDegrees(var11 * (180.0F / (float)Math.PI)) * (float) (Math.PI / 180.0);
					}

					var0.b[var8] = var0.b[var8] + (var9 * var9 * var11 - 2.0F * var10 * var9 * var0.b[var8]) * var6;
					var0.a[var8] = var0.a[var8] + var0.b[var8] * var6;
				}
			}
		} else {
			for (int var4 = 0; var4 < 22; var4++) {
				var0.a[var4] = Z[var4];
				var0.b[var4] = 0.0F;
			}
		}
	}

	private static FogataTitere.b a(boolean var0) {
		if (var0) {
			if (af == null) {
				af = new FogataTitere.b(true);
			}

			return af;
		} else {
			if (ae == null) {
				ae = new FogataTitere.b(false);
			}

			return ae;
		}
	}

	private static float b(float var0) {
		var0 = Mth.clamp(var0, 0.0F, 1.0F);
		return var0 * var0 * (3.0F - 2.0F * var0);
	}

	private static float a(float var0, float var1, float var2, float var3, float var4) {
		if (var0 <= var1 || var0 >= var4) {
			return 0.0F;
		} else if (var0 < var2) {
			return b((var0 - var1) / (var2 - var1));
		} else {
			return var0 > var3 ? 1.0F - b((var0 - var3) / (var4 - var3)) : 1.0F;
		}
	}

	private static ModelPart.Cube a(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, Set<Direction> var9) {
		return new ModelPart.Cube(var0, var1, var2, var3, var4, var5, var6, var7, var8, var8, var8, false, 64.0F, 64.0F, var9);
	}

	private static ModelPart a(List<ModelPart.Cube> var0, Map<String, ModelPart> var1) {
		return new ModelPart(var0, var1);
	}

	private static final class a {
		final float[] a = new float[22];
		final float[] b = new float[22];
		float c = -1.0F;
		float d;
	}

	private static final class b {
		final ModelPart a;
		final ModelPart b;
		final ModelPart c;
		final ModelPart d;
		final ModelPart e;
		final ModelPart f;
		final ModelPart g;
		final ModelPart h;
		final ModelPart i;
		final ModelPart j;
		final ModelPart k;
		final ModelPart l;
		final ModelPart m;
		final ModelPart n;
		final ModelPart o;
		final ModelPart p;

		b(boolean var1) {
			float var2 = var1 ? 3.0F : 4.0F;
			float var3 = var1 ? -3.5F : -4.0F;
			ModelPart[] var4 = a(40, 16, 40, 32, var1 ? -2.0F : -3.0F, -2.0F, var2, true);
			ModelPart[] var5 = a(32, 48, 48, 48, -1.0F, -2.0F, var2, true);
			ModelPart[] var6 = a(0, 16, 0, 32, -2.0F, 0.0F, 4.0F, false);
			ModelPart[] var7 = a(16, 48, 0, 48, -2.0F, 0.0F, 4.0F, false);
			this.e = var4[0];
			this.f = var4[1];
			this.g = var4[2];
			this.h = var5[0];
			this.i = var5[1];
			this.j = var5[2];
			this.k = var6[0];
			this.l = var6[1];
			this.m = var6[2];
			this.n = var7[0];
			this.o = var7[1];
			this.p = var7[2];
			this.e.setPos(-5.0F, var3, 0.0F);
			this.h.setPos(5.0F, var3, 0.0F);
			this.k.setPos(-1.9F, 0.0F, 0.0F);
			this.n.setPos(1.9F, 0.0F, 0.0F);
			this.d = FogataTitere.a(
				List.of(
					FogataTitere.a(0, 0, -4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F, FogataTitere.ah),
					FogataTitere.a(32, 0, -4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.5F, FogataTitere.ah)
				),
				Map.of()
			);
			this.d.setPos(0.0F, -6.0F, 0.0F);
			this.c = FogataTitere.a(
				List.of(
					FogataTitere.a(16, 16, -4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F, 0.0F, FogataTitere.aj),
					FogataTitere.a(16, 32, -4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F, 0.25F, FogataTitere.aj)
				),
				Map.of("cabeza", this.d, "hombro_d", this.e, "hombro_i", this.h)
			);
			this.b = FogataTitere.a(
				List.of(
					FogataTitere.a(16, 21, -4.0F, -1.0F, -2.0F, 8.0F, 2.0F, 4.0F, -0.01F, FogataTitere.ai),
					FogataTitere.a(16, 37, -4.0F, -1.0F, -2.0F, 8.0F, 2.0F, 4.0F, 0.24F, FogataTitere.ai)
				),
				Map.of("pecho", this.c)
			);
			this.b.setPos(0.0F, -6.0F, 0.0F);
			ModelPart var8 = FogataTitere.a(
				List.of(
					FogataTitere.a(16, 22, -4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F, 0.0F, FogataTitere.ai),
					FogataTitere.a(16, 16, -4.0F, 0.0F, -2.0F, 8.0F, 0.0F, 4.0F, 0.0F, FogataTitere.ak),
					FogataTitere.a(16, 38, -4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F, 0.25F, FogataTitere.ai),
					FogataTitere.a(16, 32, -4.0F, 0.0F, -2.0F, 8.0F, 0.0F, 4.0F, 0.25F, FogataTitere.ak)
				),
				Map.of("cintura", this.b)
			);
			this.a = FogataTitere.a(List.of(), Map.of("panza", var8, "muslo_d", this.k, "muslo_i", this.n));
		}

		private static ModelPart[] a(int var0, int var1, int var2, int var3, float var4, float var5, float var6, boolean var7) {
			ModelPart var8 = FogataTitere.a(
				List.of(
					FogataTitere.a(var0, var1 + 6, var4, 0.0F, -2.0F, var6, 6.0F, 4.0F, 0.0F, FogataTitere.ai),
					FogataTitere.a(var0, var1, var4, 6.0F, -2.0F, var6, 0.0F, 4.0F, 0.0F, FogataTitere.ak),
					FogataTitere.a(var2, var3 + 6, var4, 0.0F, -2.0F, var6, 6.0F, 4.0F, 0.25F, FogataTitere.ai),
					FogataTitere.a(var2, var3, var4, 6.0F, -2.0F, var6, 0.0F, 4.0F, 0.25F, FogataTitere.ak)
				),
				Map.of()
			);
			ModelPart var9 = FogataTitere.a(
				List.of(
					FogataTitere.a(var0, var1 + 5, var4, -1.0F, -2.0F, var6, 2.0F, 4.0F, -0.01F, FogataTitere.ai),
					FogataTitere.a(var2, var3 + 5, var4, -1.0F, -2.0F, var6, 2.0F, 4.0F, 0.24F, FogataTitere.ai)
				),
				Map.of("abajo", var8)
			);
			var9.setPos(0.0F, var5 + 6.0F, 0.0F);
			Set var10 = var7 ? FogataTitere.aj : FogataTitere.ai;
			ModelPart var11 = FogataTitere.a(
				List.of(
					FogataTitere.a(var0, var1, var4, var5, -2.0F, var6, 6.0F, 4.0F, 0.0F, var10),
					FogataTitere.a(var2, var3, var4, var5, -2.0F, var6, 6.0F, 4.0F, 0.25F, var10)
				),
				Map.of("junta", var9)
			);
			return new ModelPart[]{var11, var9, var8};
		}

		void a(float[] var1) {
			this.b.setRotation(0.5F * var1[1], 0.0F, 0.5F * var1[19]);
			this.c.setRotation(0.5F * var1[1], 0.0F, 0.5F * var1[19]);
			this.d.setRotation(var1[2], var1[15], var1[18]);
			this.e.setRotation(var1[3], var1[16], var1[4]);
			this.h.setRotation(var1[6], var1[17], var1[7]);
			this.f.xRot = this.g.xRot = 0.5F * var1[5];
			this.i.xRot = this.j.xRot = 0.5F * var1[8];
			this.k.setRotation(var1[9], 0.0F, var1[10]);
			this.n.setRotation(var1[12], 0.0F, var1[13]);
			this.l.xRot = this.m.xRot = 0.5F * var1[11];
			this.o.xRot = this.p.xRot = 0.5F * var1[14];
		}
	}
}
