package com.talhanation.smallships.client.model;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.world.entity.ship.GalleonEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Purpose-built 34-block galleon model. All dimensions are authored directly
 * around the ship origin: the closed hold sits below y=0, while the main deck,
 * forecastle and quarterdeck follow the collision heights in GalleonEntity.
 */
public class GalleonModel extends ShipModel<GalleonEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(SmallShipsMod.MOD_ID, GalleonEntity.ID + "_model"), "main");

    private final ModelPart root;
    private final ModelPart rudder;
    private final ModelPart wheel;

    public GalleonModel(ModelPart modelPart) {
        this.root = modelPart;
        ModelPart galleon = modelPart.getChild("galleon");
        this.rudder = galleon.getChild("rudder");
        this.wheel = galleon.getChild("wheel");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition galleon = root.addOrReplaceChild("galleon", CubeListBuilder.create(), PartPose.ZERO);

        addClosedHull(galleon);
        addDecksAndCastles(galleon);
        addRailings(galleon);
        addMastsAndYards(galleon);
        addCaptainHelm(galleon);
        addRudder(galleon);
        addBowSprit(galleon);
        addCannonPortFrames(galleon);

        // One model UV pixel now matches one plank-texture pixel. Large faces
        // repeat the 16x16 block texture instead of sampling the small-ship atlas.
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static void addClosedHull(PartDefinition galleon) {
        PartDefinition hull = galleon.addOrReplaceChild("closed_hold", CubeListBuilder.create(), PartPose.ZERO);

        // Deep keel and bilge: this volume is deliberately solid and has no
        // opening, so the visual hold matches the non-accessible collision hull.
        hull.addOrReplaceChild("keel", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, 18.0F, -190.0F, 16.0F, 16.0F, 380.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        hull.addOrReplaceChild("bilge", CubeListBuilder.create().texOffs(0, 18)
                        .addBox(-12.0F, 5.0F, -208.0F, 24.0F, 24.0F, 36.0F, new CubeDeformation(0.0F))
                        .addBox(-25.0F, 1.0F, -172.0F, 50.0F, 30.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-35.0F, -5.0F, -123.0F, 70.0F, 38.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-41.0F, -8.0F, -74.0F, 82.0F, 42.0F, 197.0F, new CubeDeformation(0.0F))
                        .addBox(-30.0F, -3.0F, 123.0F, 60.0F, 35.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-15.0F, 2.0F, 172.0F, 30.0F, 28.0F, 37.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        // High outer shell. Overlapping stepped sections produce the rounded,
        // heavy silhouette of a galleon without inheriting another ship mesh.
        hull.addOrReplaceChild("outer_shell", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-15.0F, -10.0F, -209.0F, 30.0F, 30.0F, 37.0F, new CubeDeformation(0.0F))
                        .addBox(-31.0F, -20.0F, -172.0F, 62.0F, 42.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-43.0F, -29.0F, -123.0F, 86.0F, 54.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-49.0F, -37.0F, -74.0F, 98.0F, 62.0F, 197.0F, new CubeDeformation(0.0F))
                        .addBox(-36.0F, -43.0F, 123.0F, 72.0F, 64.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-19.0F, -48.0F, 172.0F, 38.0F, 58.0F, 37.0F, new CubeDeformation(0.0F))
                        .addBox(-29.0F, -45.0F, -172.0F, 58.0F, 25.0F, 37.0F, new CubeDeformation(0.0F))
                        .addBox(-15.0F, -45.0F, -209.0F, 30.0F, 35.0F, 37.0F, new CubeDeformation(0.0F))
                        .addBox(-35.0F, -52.0F, 123.0F, 70.0F, 9.0F, 49.0F, new CubeDeformation(0.0F))
                        .addBox(-19.0F, -52.0F, 172.0F, 38.0F, 4.0F, 37.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        hull.addOrReplaceChild("waterline_bands", CubeListBuilder.create().texOffs(0, 56)
                        .addBox(-50.5F, -8.0F, -122.0F, 2.0F, 5.0F, 244.0F, new CubeDeformation(0.0F))
                        .addBox(48.5F, -8.0F, -122.0F, 2.0F, 5.0F, 244.0F, new CubeDeformation(0.0F))
                        .addBox(-44.0F, -1.0F, -170.0F, 2.0F, 4.0F, 48.0F, new CubeDeformation(0.0F))
                        .addBox(42.0F, -1.0F, -170.0F, 2.0F, 4.0F, 48.0F, new CubeDeformation(0.0F))
                        .addBox(-34.0F, -1.0F, 122.0F, 2.0F, 4.0F, 50.0F, new CubeDeformation(0.0F))
                        .addBox(32.0F, -1.0F, 122.0F, 2.0F, 4.0F, 50.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addDecksAndCastles(PartDefinition galleon) {
        PartDefinition decks = galleon.addOrReplaceChild("decks", CubeListBuilder.create(), PartPose.ZERO);

        decks.addOrReplaceChild("main_deck", CubeListBuilder.create().texOffs(0, 36)
                        .addBox(-48.0F, -39.0F, -135.0F, 96.0F, 3.0F, 264.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        decks.addOrReplaceChild("quarterdeck", CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-35.0F, -53.0F, 129.0F, 70.0F, 3.0F, 80.0F, new CubeDeformation(0.0F))
                        .addBox(-39.0F, -43.0F, 120.0F, 78.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
                        .addBox(-37.0F, -48.0F, 125.0F, 74.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        decks.addOrReplaceChild("forecastle", CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-29.0F, -46.0F, -209.0F, 58.0F, 3.0F, 74.0F, new CubeDeformation(0.0F))
                        .addBox(-37.0F, -42.0F, -139.0F, 74.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        // Closed stern castle and captain's cabin establish the tall aft profile.
        decks.addOrReplaceChild("stern_castle", CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-34.0F, -80.0F, 165.0F, 68.0F, 27.0F, 38.0F, new CubeDeformation(0.0F))
                        .addBox(-30.0F, -88.0F, 174.0F, 60.0F, 8.0F, 29.0F, new CubeDeformation(0.0F))
                        .addBox(-20.0F, -94.0F, 187.0F, 40.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        decks.addOrReplaceChild("stern_gallery", CubeListBuilder.create().texOffs(72, 26)
                        .addBox(-37.0F, -78.0F, 202.0F, 74.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                        .addBox(-34.0F, -60.0F, 205.0F, 68.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-32.0F, -75.0F, 205.0F, 4.0F, 19.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-18.0F, -75.0F, 205.0F, 4.0F, 19.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-2.0F, -75.0F, 205.0F, 4.0F, 19.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(14.0F, -75.0F, 205.0F, 4.0F, 19.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(28.0F, -75.0F, 205.0F, 4.0F, 19.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        decks.addOrReplaceChild("deck_furniture", CubeListBuilder.create().texOffs(64, 0)
                        .addBox(-14.0F, -49.0F, -32.0F, 28.0F, 10.0F, 26.0F, new CubeDeformation(0.0F))
                        .addBox(-11.0F, -51.0F, 42.0F, 22.0F, 12.0F, 22.0F, new CubeDeformation(0.0F))
                        .addBox(-8.0F, -45.0F, -105.0F, 16.0F, 6.0F, 18.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addRailings(PartDefinition galleon) {
        PartDefinition rails = galleon.addOrReplaceChild("railings", CubeListBuilder.create(), PartPose.ZERO);

        rails.addOrReplaceChild("main_rails", CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-50.0F, -51.0F, -134.0F, 3.0F, 14.0F, 259.0F, new CubeDeformation(0.0F))
                        .addBox(47.0F, -51.0F, -134.0F, 3.0F, 14.0F, 259.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        rails.addOrReplaceChild("castle_rails", CubeListBuilder.create().texOffs(28, 0)
                        .addBox(-37.0F, -67.0F, 130.0F, 3.0F, 14.0F, 77.0F, new CubeDeformation(0.0F))
                        .addBox(34.0F, -67.0F, 130.0F, 3.0F, 14.0F, 77.0F, new CubeDeformation(0.0F))
                        .addBox(-31.0F, -59.0F, -207.0F, 3.0F, 13.0F, 70.0F, new CubeDeformation(0.0F))
                        .addBox(28.0F, -59.0F, -207.0F, 3.0F, 13.0F, 70.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        rails.addOrReplaceChild("cross_rails", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-47.0F, -51.0F, 122.0F, 94.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .addBox(-34.0F, -67.0F, 204.0F, 68.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .addBox(-28.0F, -59.0F, -207.0F, 56.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addMastsAndYards(PartDefinition galleon) {
        PartDefinition rig = galleon.addOrReplaceChild("wooden_rig", CubeListBuilder.create(), PartPose.ZERO);

        rig.addOrReplaceChild("masts", CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-3.5F, -180.0F, -93.5F, 7.0F, 141.0F, 7.0F, new CubeDeformation(0.0F))
                        .addBox(-4.5F, -218.0F, -4.5F, 9.0F, 179.0F, 9.0F, new CubeDeformation(0.0F))
                        .addBox(-3.5F, -169.0F, 101.5F, 7.0F, 117.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        rig.addOrReplaceChild("yards", CubeListBuilder.create().texOffs(60, 0)
                        .addBox(-47.0F, -151.0F, -92.0F, 94.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-39.0F, -113.0F, -92.0F, 78.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-62.0F, -184.0F, -2.0F, 124.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-54.0F, -140.0F, -2.0F, 108.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-42.0F, -100.0F, -2.0F, 84.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .addBox(-36.0F, -137.0F, 103.0F, 72.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        rig.addOrReplaceChild("tops", CubeListBuilder.create().texOffs(84, 0)
                        .addBox(-11.0F, -134.0F, -100.0F, 22.0F, 5.0F, 22.0F, new CubeDeformation(0.0F))
                        .addBox(-14.0F, -160.0F, -14.0F, 28.0F, 6.0F, 28.0F, new CubeDeformation(0.0F))
                        .addBox(-10.0F, -119.0F, 95.0F, 20.0F, 5.0F, 20.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addCaptainHelm(PartDefinition galleon) {
        PartDefinition wheel = galleon.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(96, 0)
                        .addBox(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -70.0F, 157.0F));

        wheel.addOrReplaceChild("spoke_0", CubeListBuilder.create().texOffs(100, 14)
                        .addBox(-1.5F, -18.0F, -2.0F, 3.0F, 36.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        wheel.addOrReplaceChild("spoke_1", CubeListBuilder.create().texOffs(100, 14)
                        .addBox(-1.5F, -18.0F, -2.0F, 3.0F, 36.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, 1.0472F));
        wheel.addOrReplaceChild("spoke_2", CubeListBuilder.create().texOffs(100, 14)
                        .addBox(-1.5F, -18.0F, -2.0F, 3.0F, 36.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, 2.0944F));

        galleon.addOrReplaceChild("wheel_stand", CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-3.0F, -70.0F, 154.0F, 6.0F, 19.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addRudder(PartDefinition galleon) {
        PartDefinition rudder = galleon.addOrReplaceChild("rudder", CubeListBuilder.create(),
                PartPose.offset(0.0F, -6.0F, 205.0F));

        rudder.addOrReplaceChild("stock", CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-3.0F, -48.0F, -3.0F, 6.0F, 66.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        rudder.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-16.0F, -17.0F, -2.5F, 32.0F, 49.0F, 5.0F, new CubeDeformation(0.0F))
                        .addBox(-12.0F, 32.0F, -2.5F, 24.0F, 12.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addBowSprit(PartDefinition galleon) {
        PartDefinition bowsprit = galleon.addOrReplaceChild("bowsprit", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -49.0F, -202.0F, -0.2182F, 0.0F, 0.0F));
        bowsprit.addOrReplaceChild("spar", CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-3.5F, -3.5F, -78.0F, 7.0F, 7.0F, 82.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        bowsprit.addOrReplaceChild("crosspiece", CubeListBuilder.create().texOffs(60, 0)
                        .addBox(-28.0F, -2.0F, -55.0F, 56.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
    }

    private static void addCannonPortFrames(PartDefinition galleon) {
        PartDefinition ports = galleon.addOrReplaceChild("cannon_port_frames", CubeListBuilder.create(), PartPose.ZERO);
        float[] positions = {-143.0F, -103.0F, -63.0F, -23.0F, 17.0F, 57.0F, 97.0F, 137.0F};
        for (int i = 0; i < positions.length; i++) {
            float z = positions[i];
            float side = Math.abs(z) > 120.0F ? 35.0F : 48.5F;
            ports.addOrReplaceChild("port_" + i, CubeListBuilder.create().texOffs(112, 0)
                            .addBox(-2.0F, -31.0F, z - 8.0F, 4.0F, 15.0F, 16.0F, new CubeDeformation(0.0F)),
                    PartPose.offset(side, 0.0F, 0.0F));
            ports.addOrReplaceChild("starboard_" + i, CubeListBuilder.create().texOffs(112, 0)
                            .addBox(-2.0F, -31.0F, z - 8.0F, 4.0F, 15.0F, 16.0F, new CubeDeformation(0.0F)),
                    PartPose.offset(-side, 0.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(GalleonEntity galleon, float partialTicks, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float rudderAngle = galleon.getRudderAngle(partialTicks);
        this.rudder.yRot = rudderAngle;
        this.wheel.zRot = -rudderAngle * 1.8F;
    }

    @Override
    public @NotNull ModelPart root() {
        return this.root;
    }
}
