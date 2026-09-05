package com.talhanation.smallships.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.talhanation.smallships.client.model.GalleonModel;
import com.talhanation.smallships.world.entity.projectile.Cannon;
import com.talhanation.smallships.world.entity.ship.GalleonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.jetbrains.annotations.NotNull;

public class GalleonRenderer extends ShipRenderer<GalleonEntity> {
    public GalleonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 4.0F;
    }

    @Override
    protected GalleonModel createBoatModel(EntityRendererProvider.Context context, Boat.Type type) {
        return new GalleonModel(context.bakeLayer(GalleonModel.LAYER_LOCATION));
    }

    @Override
    protected ResourceLocation getTextureLocation(Boat.Type type) {
        return new ResourceLocation("minecraft",
                "textures/block/" + ShipRenderer.getNameFromType(type) + "_planks.png");
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GalleonEntity galleon) {
        // The 16x16 plank texture repeats cleanly over the purpose-built large
        // faces. Ghost tint/translucency is still applied by ShipRenderer.
        return this.getTextureLocation(galleon.getVariant());
    }

    @Override
    protected float getCannonHeightOffset() {
        return 0.0F;
    }

    @Override
    protected float getCannonRenderPositionScale() {
        return 1.0F / 1.3F;
    }

    @Override
    protected double getRenderedCannonLateralOffset(Cannon cannon) {
        // The common renderer mirrors X; this model is authored without that
        // legacy mirror, so port and starboard must be swapped back visually.
        return cannon.isRightSided() ? cannon.getOffsetX() : -cannon.getOffsetX();
    }

    @Override
    public Axis getWaveAngleRotation() {
        // After ShipRenderer's internal quarter-turn, X is the longitudinal
        // axis of this model. Rolling around it keeps the captain on the deck.
        return Axis.XN;
    }

    @Override
    public void render(@NotNull GalleonEntity galleon, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        // The purpose-built model is authored longitudinally on Z, whereas
        // legacy ship meshes include their own quarter-turn in the baked root.
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entityYaw));
        super.render(galleon, entityYaw, partialTicks, poseStack, buffers, packedLight);
    }
}
