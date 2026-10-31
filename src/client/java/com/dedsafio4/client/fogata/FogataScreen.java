package com.dedsafio4.client.fogata;

import com.dedsafio4.revivir.FogataEntity;
import java.util.List;
import java.util.UUID;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.PlayerFaceRenderer;

/**
 * El menú de la Fogata: la lista de baneados, lo que paga y el botón Revivir.
 * Animación de NavithLabs / AlieRequiem, usada con su permiso.
 */
public final class FogataScreen extends Screen {
	private static final ResourceLocation a = a("fondo");
	private static final ResourceLocation b = a("fila");
	private static final ResourceLocation c = a("fila_verde");
	private static final ResourceLocation d = a("barrita");
	private static final ResourceLocation e = a("boton");
	private static final ResourceLocation f = a("boton_claro");
	private static final Component g = Component.literal("Revivir");
	private static final int h = 5;
	private static final int i = 610;
	private static final int j = 225;
	private static final int k = 725;
	private static final int l = 630;
	private static final int m = 695;
	private static final int n = 310;
	private static final int o = 265;
	private static final int p = 50;
	private static final int q = 295;
	private static final int r = 10;
	private static final int s = 5;
	private static final int t = 5;
	private static final int u = 40;
	private static final float v = 63.0F;
	private static final float w = 14.0F;
	private static final float x = 3.0F;
	private static final float y = 194.0F;
	private static final int z = 960;
	private static final int A = 30;
	private static final int B = 45;
	private static final int C = 310;
	private static final int D = 91;
	private static final int E = 1040;
	private static final int F = 565;
	private static final int G = 160;
	private static final int H = 80;
	private static final float I = 1057.0F;
	private static final float J = 585.0F;
	private static final float K = 3.5F;
	private static final int L = 1080;
	private static final int M = 450;
	private static final int N = -1;
	private final FogataEntity O;
	private int P;
	private UUID Q;
	private boolean R;
	private double S;

	private static ResourceLocation a(String var0) {
		return ResourceLocation.fromNamespaceAndPath("dedsafio4", "textures/gui/fogata/" + var0 + ".png");
	}

	public FogataScreen(FogataEntity var1) {
		super(Component.literal("Fogata"));
		this.O = var1;
	}

	public boolean isPauseScreen() {
		return false;
	}

	public void renderBackground(GuiGraphics var1, int var2, int var3, float var4) {
	}

	public void tick() {
		if (this.O.isRemoved()) {
			this.onClose();
		}
	}

	private static List<FogataSkins.Muerto> a() {
		return FogataSkins.LISTA;
	}

	private static int b() {
		return Math.max(0, a().size() - 10);
	}

	private float c() {
		Window var1 = this.minecraft.getWindow();
		return (float)var1.getHeight() / 1080.0F / (float)var1.getGuiScale();
	}

	private double a(double var1) {
		return (var1 - (double)this.width / 2.0) / (double)this.c() + 960.0;
	}

	private double b(double var1) {
		return (var1 - (double)this.height / 2.0) / (double)this.c() + 540.0;
	}

	private boolean d() {
		if (this.Q == null) {
			return false;
		} else {
			for (FogataSkins.Muerto var2 : a()) {
				if (var2.id.equals(this.Q)) {
					return true;
				}
			}

			this.Q = null;
			return false;
		}
	}

	public void render(GuiGraphics var1, int var2, int var3, float var4) {
		this.P = Mth.clamp(this.P, 0, b());
		double var5 = this.a((double)var2);
		double var7 = this.b((double)var3);
		float var9 = this.c();
		PoseStack var10 = var1.pose();
		var10.pushPose();
		var10.translate((float)this.width / 2.0F, (float)this.height / 2.0F, 0.0F);
		var10.scale(var9, var9, 1.0F);
		var10.translate(-960.0F, -540.0F, 0.0F);
		a(var1, a, 610, 225, 725, 630);
		List var11 = a();
		ItemStack var12 = this.e();
		boolean var13 = false;
		int var14 = this.R ? -1 : this.a(var5, var7);

		for (int var15 = 0; var15 < 10 && this.P + var15 < var11.size(); var15++) {
			int var16 = this.P + var15;
			int var17 = 310 + var15 * 50;
			FogataSkins.Muerto var18 = (FogataSkins.Muerto)var11.get(var16);
			a(var1, !var13 && !var18.id.equals(this.Q) && var16 != var14 ? b : c, 695, var17, 265, 50);
			PlayerFaceRenderer.draw(var1, var18.skin(), 700, var17 + 5, 40);
			int var19 = this.font.width(var18.texto);
			float var20 = (float)var19 * 3.0F > 194.0F ? 194.0F / (float)var19 : 3.0F;
			var10.pushPose();
			var10.translate(758.0F, (float)var17 + 14.0F + (3.0F - var20) * 3.5F, 0.0F);
			var10.scale(var20, var20, 1.0F);
			var1.drawString(this.font, var18.texto, 0, 0, -1, true);
			var10.popPose();
		}

		if (var11.size() > 10) {
			a(var1, d, 960, this.f(), 30, 45);
		}

		if (!var12.isEmpty()) {
			var10.pushPose();
			var10.translate(1080.0F, 450.0F, 0.0F);
			var10.scale(5.0F, 5.0F, 1.0F);
			var1.renderItem(var12, 0, 0);
			var10.popPose();
		}

		if (this.a(var13)) {
			a(var1, this.c(var5, var7) ? f : e, 1040, 565, 160, 80);
			var10.pushPose();
			var10.translate(1057.0F, 585.0F, 0.0F);
			var10.scale(3.5F, 3.5F, 1.0F);
			var1.drawString(this.font, g, 0, 0, -1, true);
			var10.popPose();
		}

		var10.popPose();
	}

	private ItemStack e() {
		return this.minecraft.player == null ? ItemStack.EMPTY : com.dedsafio4.baneos.Baneos.cubierto(this.minecraft.player);
	}

	private boolean a(boolean var1) {
		return var1 ? !a().isEmpty() : this.d();
	}

	private static void a(GuiGraphics var0, ResourceLocation var1, int var2, int var3, int var4, int var5) {
		int var6 = var4 / 5;
		int var7 = var5 / 5;
		var0.blit(var1, var2, var3, var4, var5, 0.0F, 0.0F, var6, var7, var6, var7);
	}

	private int f() {
		int var1 = b();
		return var1 <= 0 ? 310 : 310 + 5 * (91 * this.P / var1);
	}

	private int a(double var1, double var3) {
		if (!(var1 < 695.0) && !(var1 >= 990.0) && !(var3 < 310.0)) {
			int var5 = (int)((var3 - 310.0) / 50.0);
			return var5 < 10 && this.P + var5 < a().size() ? this.P + var5 : -1;
		} else {
			return -1;
		}
	}

	private boolean b(double var1, double var3) {
		int var5 = this.f();
		return a().size() > 10 && var1 >= 960.0 && var1 < 990.0 && var3 >= (double)var5 && var3 < (double)(var5 + 45);
	}

	private boolean c(double var1, double var3) {
		return var1 >= 1040.0 && var1 < 1200.0 && var3 >= 565.0 && var3 < 645.0;
	}

	private static void g() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	public boolean mouseClicked(double var1, double var3, int var5) {
		if (var5 != 0) {
			return super.mouseClicked(var1, var3, var5);
		} else {
			double var6 = this.a(var1);
			double var8 = this.b(var3);
			if (this.a(false) && this.c(var6, var8)) {
				g();
				if (this.Q != null) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.dedsafio4.revivir.Fogata.RevivirPayload(this.O.getId(), this.Q.toString()));
				this.onClose();
				return true;
			} else if (this.b(var6, var8)) {
				this.S = var8 - (double)this.f();
				this.R = true;
				return true;
			} else {
				int var10 = this.a(var6, var8);
				if (var10 >= 0) {
					this.Q = a().get(var10).id;
					g();
					return true;
				} else {
					return super.mouseClicked(var1, var3, var5);
				}
			}
		}
	}

	public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
		if (this.R) {
			this.c(this.b(var3));
			return true;
		} else {
			return super.mouseDragged(var1, var3, var5, var6, var8);
		}
	}

	public boolean mouseReleased(double var1, double var3, int var5) {
		this.R = false;
		return super.mouseReleased(var1, var3, var5);
	}

	private void c(double var1) {
		int var3 = b();
		double var4 = (var1 - this.S - 310.0) / 455.0;
		this.P = Mth.clamp((int)Math.round(var4 * (double)var3), 0, var3);
	}

	public boolean mouseScrolled(double var1, double var3, double var5, double var7) {
		if (var7 != 0.0 && b() > 0) {
			this.P = Mth.clamp(this.P - (int)Math.signum(var7), 0, b());
			return true;
		} else {
			return false;
		}
	}
}
