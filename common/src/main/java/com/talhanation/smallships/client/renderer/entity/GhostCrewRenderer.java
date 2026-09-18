package com.talhanation.smallships.client.renderer.entity;

import com.talhanation.smallships.world.entity.ship.GhostCrewEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class GhostCrewRenderer extends MobRenderer<GhostCrewEntity, PlayerModel<GhostCrewEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("smallships", "textures/entity/ghost_crew.png");

    public GhostCrewRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<GhostCrewEntity>(context.bakeLayer(ModelLayers.PLAYER), false) {
            @Override
            public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                       float red, float green, float blue, float alpha) {
                super.renderToBuffer(pose, buffer, light, overlay, red, green, blue, alpha * 0.65F);
            }
        }, 0.0F);
        // Preserve the supplied skin's colors and overlay layers.
    }

    @Override
    public ResourceLocation getTextureLocation(GhostCrewEntity entity) { return SKIN; }

    @Override
    public boolean shouldShowName(GhostCrewEntity entity) { return false; }

    @Override
    public int getBlockLightLevel(GhostCrewEntity entity, BlockPos pos) { return 15; }

    @Override
    protected RenderType getRenderType(GhostCrewEntity entity, boolean visible, boolean translucent, boolean glowing) {
        if (visible || translucent) return RenderType.entityTranslucent(SKIN);
        return glowing ? RenderType.outline(SKIN) : null;
    }
}
