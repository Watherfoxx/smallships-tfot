package com.talhanation.smallships.client.renderer.entity;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.client.model.RowBoatModel;
import com.talhanation.smallships.world.entity.ship.RowBoatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.jetbrains.annotations.NotNull;

public class RowBoatRenderer extends ShipRenderer<RowBoatEntity> {
    public RowBoatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected RowBoatModel createBoatModel(EntityRendererProvider.Context context, Boat.Type type) {
        return new RowBoatModel(context.bakeLayer(RowBoatModel.LAYER_LOCATION));
    }

    @Override
    protected ResourceLocation getTextureLocation(Boat.Type type) {
        return new ResourceLocation(SmallShipsMod.MOD_ID, "textures/entity/ship/" + ShipRenderer.getNameFromType(type) + ".png");
    }

    @Override
    public void render(@NotNull RowBoatEntity rowBoatEntity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource multiBufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.translate(0.0D, 1.3D, 0.0D);
        super.render(rowBoatEntity, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
    }
}
