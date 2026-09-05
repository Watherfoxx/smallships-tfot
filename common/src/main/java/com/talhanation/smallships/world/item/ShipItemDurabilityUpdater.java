package com.talhanation.smallships.world.item;

import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.mixin.item.ItemAccessor;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ShipItemDurabilityUpdater {
    private static final int DEFAULT_COG_MAX_HEALTH = 150;
    private static final int DEFAULT_BRIGG_MAX_HEALTH = 300;
    private static final int DEFAULT_GALLEY_MAX_HEALTH = 250;
    private static final int DEFAULT_GALLEON_MAX_HEALTH = 1350;
    private static final int DEFAULT_DRAKKAR_MAX_HEALTH = 200;
    private static final int DEFAULT_ROWBOAT_MAX_HEALTH = 125;
    private static final int DEFAULT_VANILLA_BOAT_MAX_HEALTH = 100;
    private static boolean itemsReady;

    private ShipItemDurabilityUpdater() {}

    public static void markItemsReady() {
        itemsReady = true;
        updateFromConfig();
    }

    public static void updateFromConfig() {
        // Fabric fires its config-loading callback while ModItems is still
        // being initialized. Deferring the first update avoids a class-init
        // cycle while retaining live updates after the registries are ready.
        if (!itemsReady) {
            return;
        }
        setMaxDamage(ModItems.COG_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeCogMaxHealth, DEFAULT_COG_MAX_HEALTH));
        setMaxDamage(ModItems.BRIGG_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeBriggMaxHealth, DEFAULT_BRIGG_MAX_HEALTH));
        setMaxDamage(ModItems.GALLEY_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeGalleyMaxHealth, DEFAULT_GALLEY_MAX_HEALTH));
        setMaxDamage(ModItems.GALLEON_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeBriggMaxHealth, DEFAULT_GALLEON_MAX_HEALTH / 3) * 3);
        setMaxDamage(ModItems.DRAKKAR_ITEMS, getConfigHealthOrDefault(SmallShipsConfig.Common.shipAttributeDrakkarMaxHealth, DEFAULT_DRAKKAR_MAX_HEALTH));
        setMaxDamage(ModItems.ROWBOAT_ITEMS, DEFAULT_ROWBOAT_MAX_HEALTH);
        setVanillaBoatMaxDamage(DEFAULT_VANILLA_BOAT_MAX_HEALTH);
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

    private static void setVanillaBoatMaxDamage(int maxDamage) {
        int safeMaxDamage = Math.max(1, maxDamage);
        for (Item item : new Item[]{
                Items.OAK_BOAT,
                Items.SPRUCE_BOAT,
                Items.BIRCH_BOAT,
                Items.JUNGLE_BOAT,
                Items.ACACIA_BOAT,
                Items.DARK_OAK_BOAT,
                Items.MANGROVE_BOAT,
                Items.CHERRY_BOAT,
                Items.BAMBOO_RAFT
        }) {
            ((ItemAccessor) item).setMaxDamage(safeMaxDamage);
        }
    }
}
