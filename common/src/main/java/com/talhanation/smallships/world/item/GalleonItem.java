package com.talhanation.smallships.world.item;

import com.talhanation.smallships.world.entity.ship.GalleonEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class GalleonItem extends ShipItem {
    public GalleonItem(Boat.Type type, Properties properties) {
        super(type, properties);
    }

    @Override
    protected @NotNull Boat getBoat(@NotNull Level level, @NotNull HitResult hitResult) {
        return GalleonEntity.summon(level, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z);
    }

    @Override
    protected boolean canPlaceBoat(@NotNull Level level, @NotNull Boat boat) {
        return super.canPlaceBoat(level, boat)
                && (!(boat instanceof GalleonEntity galleon) || galleon.isHullClear());
    }
}
