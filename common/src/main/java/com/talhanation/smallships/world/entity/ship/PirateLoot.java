package com.talhanation.smallships.world.entity.ship;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.config.GhostShipsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import com.talhanation.smallships.config.GhostLootTable;

final class PirateLoot {
    static void drop(Ship ship, ServerLevel level) {
        int[] stacks = {0};
        var random = level.getRandom();
        GhostLootTable.roll(GhostShipsConfig.forShip(ship).lootPools, random::nextInt, random::nextDouble, reward -> {
            ResourceLocation id = ResourceLocation.tryParse(reward.item);
            var item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) {
                SmallShipsMod.LOGGER.warn("Unknown ghost reward item: {}", reward.item);
                return;
            }
            int count = reward.min + random.nextInt(reward.max - reward.min + 1);
            while (count > 0) {
                int size = Math.min(count, item.getMaxStackSize());
                if (spawnReward(ship, level, new ItemStack(item, size))) stacks[0]++;
                count -= size;
            }
        });
        SmallShipsMod.LOGGER.info("Ghost ship {} sank at {}: spawned {} reward stacks",
                BuiltInRegistries.ENTITY_TYPE.getKey(ship.getType()), ship.blockPosition(), stacks[0]);
    }

    static boolean spawnReward(Ship ship, ServerLevel level, ItemStack stack) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ship.getBlockX(), ship.getBlockZ());
        double y = Math.max(ship.getY() + 1.0D, surface + 0.5D);
        ItemEntity entity = new ItemEntity(level, ship.getX(), y, ship.getZ(), stack.copy());
        entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5D) * 0.15D, 0.15D,
                (level.getRandom().nextDouble() - 0.5D) * 0.15D);
        entity.setDefaultPickUpDelay();
        return level.addFreshEntity(entity);
    }
}
