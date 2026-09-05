package com.talhanation.smallships.client.model.sail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** Purpose-built three-masted sail plan for the large galleon. */
public class GalleonSailModel extends SailModel {
    private final ModelPart rig;
    private final ModelPart[] sailStates = new ModelPart[5];

    public GalleonSailModel() {
        ModelPart root = createBodyLayer().bakeRoot();
        this.rig = root.getChild("galleon_sails");
        for (int state = 0; state < this.sailStates.length; state++) {
            this.sailStates[state] = this.rig.getChild("state_" + state);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rig = root.addOrReplaceChild("galleon_sails", CubeListBuilder.create(), PartPose.ZERO);

        // State zero intentionally contains no cloth: yards and masts are part
        // of GalleonModel and remain visible when all sails are furled.
        rig.addOrReplaceChild("state_0", CubeListBuilder.create(), PartPose.ZERO);
        addSailState(rig, 1, 0.22F);
        addSailState(rig, 2, 0.48F);
        addSailState(rig, 3, 0.74F);
        addSailState(rig, 4, 1.00F);

        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addSailState(PartDefinition rig, int stateNumber, float deployment) {
        PartDefinition state = rig.addOrReplaceChild("state_" + stateNumber, CubeListBuilder.create(), PartPose.ZERO);

        addSquareSail(state, "fore_top", -91.5F, -146.0F, 88.0F, 31.0F, deployment);
        addSquareSail(state, "fore_course", -91.5F, -108.0F, 72.0F, 51.0F, deployment);

        addSquareSail(state, "main_top", -1.5F, -179.0F, 116.0F, 35.0F, deployment);
        addSquareSail(state, "main_middle", -1.5F, -135.0F, 102.0F, 31.0F, deployment);
        addSquareSail(state, "main_course", -1.5F, -95.0F, 80.0F, 48.0F, deployment);

        PartDefinition mizzen = state.addOrReplaceChild("mizzen_lateen", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-32.0F, 0.0F, -1.0F, 64.0F, 58.0F * deployment, 2.0F, new CubeDeformation(0.0F))
                        .addBox(-24.0F, 58.0F * deployment, -1.0F, 48.0F, 9.0F * deployment, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -132.0F, 104.0F, 0.0F, 0.0F, -0.1745F));
        mizzen.addOrReplaceChild("lateen_foot", CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-16.0F, 0.0F, -1.0F, 32.0F, 8.0F * deployment, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, 59.0F * deployment, 0.0F));

        // Three stepped centre-line panels suggest triangular jibs between the
        // foremast and bowsprit while keeping the model entirely cube-authored.
        state.addOrReplaceChild("jibs", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.0F, -129.0F, -139.0F, 2.0F, 25.0F * deployment, 31.0F, new CubeDeformation(0.0F))
                        .addBox(-1.0F, -101.0F, -166.0F, 2.0F, 22.0F * deployment, 27.0F, new CubeDeformation(0.0F))
                        .addBox(-1.0F, -77.0F, -193.0F, 2.0F, 18.0F * deployment, 27.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(-0.1745F, 0.0F, 0.0F));
    }

    private static void addSquareSail(PartDefinition parent, String name, float z, float topY,
                                      float width, float fullHeight, float deployment) {
        float deployedHeight = Math.max(2.0F, fullHeight * deployment);
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-width / 2.0F, 0.0F, -1.0F, width, deployedHeight, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, topY, z));
    }

    @Override
    public void setupAnim(Ship ship, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        int activeState = Mth.clamp(ship.getData(Ship.SAIL_STATE).intValue(), 0, 4);
        for (int state = 0; state < this.sailStates.length; state++) {
            this.sailStates[state].visible = state == activeState;
        }
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.rig.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
