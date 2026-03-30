package com.talhanation.smallships.world.item;

import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.mixin.item.ItemAccessor;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;

public final class ShipItemDurabilityUpdater {
    private static final int DEFAULT_COG_MAX_HEALTH = 150;
    private static final int DEFAULT_BRIGG_MAX_HEALTH = 300;
    private static final int DEFAULT_GALLEY_MAX_HEALTH = 250;
    private static final int DEFAULT_DRAKKAR_MAX_HEALTH = 200;
    private static final int DEFAULT_ROWBOAT_MAX_HEALTH = 125;

    private ShipItemDurabilityUpdater() {}

    public static void updateFromConfig() {
        setMaxDamage(ModItems.COG_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeCogMaxHealth, DEFAULT_COG_MAX_HEALTH));
        setMaxDamage(ModItems.BRIGG_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeBriggMaxHealth, DEFAULT_BRIGG_MAX_HEALTH));
        setMaxDamage(ModItems.GALLEY_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeGalleyMaxHealth, DEFAULT_GALLEY_MAX_HEALTH));
        setMaxDamage(ModItems.DRAKKAR_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeDrakkarMaxHealth, DEFAULT_DRAKKAR_MAX_HEALTH));
        setMaxDamage(ModItems.ROWBOAT_ITEMS, DEFAULT_ROWBOAT_MAX_HEALTH);
    }

    private static void setMaxDamage(java.util.Map<Boat.Type, Item> shipItems, int maxDamage) {
        int safeMaxDamage = Math.max(1, maxDamage);
        for (Item item : shipItems.values()) {
            if (item instanceof ShipItem) {
                ((ItemAccessor) item).setMaxDamage(safeMaxDamage);
            }
        }
    }

    private static int getConfigHealthOrDefault(net.minecraftforge.common.ForgeConfigSpec.DoubleValue configValue, int fallback) {
        if (configValue == null) {
            return fallback;
        }
        Double configHealth = configValue.get();
        if (configHealth == null || configHealth.isNaN() || configHealth.isInfinite()) {
            return fallback;
        }
        return Math.max(1, configHealth.intValue());
    }
}
