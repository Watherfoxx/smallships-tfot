package com.talhanation.smallships.client.renderer.entity;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.client.model.RowBoatModel;
import com.talhanation.smallships.world.entity.ship.RowBoatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

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
}
