package com.talhanation.smallships.world.item;

import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.mixin.item.ItemAccessor;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;

public final class ShipItemDurabilityUpdater {
    private ShipItemDurabilityUpdater() {}

    public static void updateFromConfig() {
        setMaxDamage(ModItems.COG_ITEMS, SmallShipsConfig.Common.shipAttributeCogMaxHealth.get().intValue());
        setMaxDamage(ModItems.BRIGG_ITEMS, SmallShipsConfig.Common.shipAttributeBriggMaxHealth.get().intValue());
        setMaxDamage(ModItems.GALLEY_ITEMS, SmallShipsConfig.Common.shipAttributeGalleyMaxHealth.get().intValue());
        setMaxDamage(ModItems.DRAKKAR_ITEMS, SmallShipsConfig.Common.shipAttributeDrakkarMaxHealth.get().intValue());
        setMaxDamage(ModItems.ROWBOAT_ITEMS, 125);
    }

    private static void setMaxDamage(java.util.Map<Boat.Type, Item> shipItems, int maxDamage) {
        int safeMaxDamage = Math.max(1, maxDamage);
        for (Item item : shipItems.values()) {
            if (item instanceof ShipItem) {
                ((ItemAccessor) item).setMaxDamage(safeMaxDamage);
            }
        }
    }
}
