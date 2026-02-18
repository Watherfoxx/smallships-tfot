package com.talhanation.smallships.client.model;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.world.entity.ship.RowBoatEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class RowBoatModel extends ShipModel<RowBoatEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(SmallShipsMod.MOD_ID, RowBoatEntity.ID + "_model"), "main");

    private final ModelPart root;
    private final ModelPart rightOar;
    private final ModelPart leftOar;
    private final ModelPart cargo0;
    private final ModelPart cargo1;

    public RowBoatModel(ModelPart modelPart) {
        this.root = modelPart;
        ModelPart rowboat = modelPart.getChild("rowboat");
        this.rightOar = rowboat.getChild("ruder_r");
        this.leftOar = rowboat.getChild("ruder_l");
        this.cargo0 = rowboat.getChild("cargo_0");
        this.cargo1 = rowboat.getChild("cargo_1");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition rowboat = root.addOrReplaceChild("rowboat", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 16.0F, 14.0F, 0.0F, 1.5708F, 0.0F));

        rowboat.addOrReplaceChild("hull", CubeListBuilder.create()
                        .texOffs(0, 8).addBox(0.0F, 0.0F, -9.0F, 19.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(0.0F, 0.0F, -3.0F, 19.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(0.0F, 0.0F, 3.0F, 19.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(19.0F, 0.0F, -9.0F, 21.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(19.0F, 0.0F, -3.0F, 21.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(19.0F, 0.0F, 3.0F, 21.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(-12.0F, 0.0F, -9.0F, 12.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(-12.0F, 0.0F, -3.0F, 12.0F, 3.0F, 6.0F)
                        .texOffs(0, 8).addBox(-12.0F, 0.0F, 3.0F, 12.0F, 3.0F, 6.0F)
                        .texOffs(0, 43).addBox(0.0F, -6.0F, -11.0F, 14.0F, 6.0F, 2.0F)
                        .texOffs(0, 35).addBox(14.0F, -6.0F, -11.0F, 14.0F, 6.0F, 2.0F)
                        .texOffs(0, 43).addBox(14.0F, -6.0F, 9.0F, 14.0F, 6.0F, 2.0F)
                        .texOffs(0, 43).addBox(0.0F, -6.0F, 9.0F, 14.0F, 6.0F, 2.0F)
                        .texOffs(2, 42).addBox(-12.0F, -7.0F, -9.0F, 12.0F, 7.0F, 2.0F)
                        .texOffs(2, 42).addBox(-12.0F, -7.0F, 7.0F, 12.0F, 7.0F, 2.0F)
                        .texOffs(2, 42).addBox(28.0F, -7.0F, -9.0F, 12.0F, 7.0F, 2.0F)
                        .texOffs(2, 42).addBox(28.0F, -7.0F, 7.0F, 12.0F, 7.0F, 2.0F)
                        .texOffs(3, 15).addBox(-14.0F, -8.0F, -7.0F, 2.0F, 8.0F, 14.0F)
                        .texOffs(3, 15).addBox(40.0F, -8.0F, -7.0F, 2.0F, 8.0F, 14.0F),
                PartPose.ZERO);

        rowboat.addOrReplaceChild("cargo_0", CubeListBuilder.create().texOffs(96, 38).addBox(0.0F, 9.0F, 0.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(-11.2F, -16.8F, 2.0F, 0.0F, 1.5708F, 0.0F));
        rowboat.addOrReplaceChild("cargo_1", CubeListBuilder.create().texOffs(96, 38).addBox(0.0F, 0.0F, 0.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(31.2F, -8.0F, -6.0F));

        rowboat.addOrReplaceChild("ruder_r", CubeListBuilder.create().texOffs(62, 20)
                        .addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F)
                        .addBox(-0.2F, -3.0F, 12.0F, 1.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(10.0F, -9.0F, -9.0F, -0.6545F, 2.4813F, 0.1963F));

        rowboat.addOrReplaceChild("ruder_l", CubeListBuilder.create().texOffs(62, 0)
                        .addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F)
                        .addBox(-0.8F, -3.0F, 12.0F, 1.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(10.0F, -9.0F, 9.0F, -0.6545F, 0.6603F, 0.1963F));

        return LayerDefinition.create(meshDefinition, 128, 64);
    }

    @Override
    public void setupAnim(@NotNull RowBoatEntity rowBoatEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.cargo0.visible = false;
        this.cargo1.visible = false;

        this.animatePaddle(rowBoatEntity, 0, this.leftOar, 1.0F);
        this.animatePaddle(rowBoatEntity, 1, this.rightOar, -1.0F);
    }

    private void animatePaddle(RowBoatEntity rowBoatEntity, int side, ModelPart paddle, float sideDirection) {
        float rowingTime = rowBoatEntity.getRowingTime(side, 1.0F);
        paddle.xRot = Mth.clampedLerp(-(float)Math.PI / 3.0F, -(float)Math.PI / 12.0F, (Mth.sin(-rowingTime) + 1.0F) * 0.5F);
        paddle.yRot = sideDirection * Mth.clampedLerp(-(float)Math.PI / 4.0F, (float)Math.PI / 4.0F, (Mth.sin(-rowingTime + 1.0F) + 1.0F) * 0.5F);
    }

    @Override
    public @NotNull ModelPart root() {
        return this.root;
    }
}
