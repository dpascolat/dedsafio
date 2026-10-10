package com.dedsafio4.client.mixin;

import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El tridente nuevo (el modelo de Patricio, tridente.bbmodel): reemplaza la forma del tridente de Minecraft, en la mano y
 * cuando se tira. La textura está en assets/minecraft/textures/entity/trident.png (64×64, el modelo la usa como 32×32).
 */
@Mixin(TridentModel.class)
public abstract class TridenteModeloMixin {
	@Inject(method = "createLayer", at = @At("HEAD"), cancellable = true)
	private static void dedsafio4$tridenteNuevo(CallbackInfoReturnable<LayerDefinition> cir) {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition root = malla.getRoot();
		PartDefinition pole = root.addOrReplaceChild("pole", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-0.5F, 2F, -0.5F, 1F, 25F, 1F)
				.texOffs(12, 9).addBox(-1.5F, 0F, -0.5F, 3F, 1F, 1F)
				.texOffs(12, 11).addBox(-2.5F, -3F, -0.5F, 1F, 4F, 1F)
				.texOffs(4, 15).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F)
				.texOffs(8, 15).addBox(1.5F, -3F, -0.5F, 1F, 4F, 1F)
				.texOffs(12, 16).addBox(-0.5F, 1F, -0.5F, 1F, 1F, 1F)
				.texOffs(4, 0).addBox(-1F, 7F, -1F, 2F, 1F, 2F)
				.texOffs(4, 3).addBox(-1F, 4F, -1F, 2F, 1F, 2F)
				.texOffs(4, 6).addBox(-1F, 10F, -1F, 2F, 1F, 2F)
				.texOffs(4, 9).addBox(-1F, 13F, -1F, 2F, 1F, 2F)
				.texOffs(12, 0).addBox(-1F, 16F, -1F, 2F, 1F, 2F)
				.texOffs(12, 3).addBox(-1F, 19F, -1F, 2F, 1F, 2F), PartPose.ZERO);
		pole.addOrReplaceChild("pieza_girada_1", CubeListBuilder.create().texOffs(16, 11).addBox(-1F, -2F, -1F, 2F, 2F, 1F), PartPose.offsetAndRotation(-0.7F, 2F, 0F, 0F, 0F, 0.829F));
		pole.addOrReplaceChild("pieza_girada_2", CubeListBuilder.create().texOffs(5, 13).addBox(-1F, -2F, -1F, 2F, 2F, 1F), PartPose.offsetAndRotation(0.7F, 24F, 0F, 0F, 0F, -0.7854F));
		cir.setReturnValue(LayerDefinition.create(malla, 32, 32));
	}
}
