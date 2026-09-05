package com.talhanation.smallships.world.entity.ship;

import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.world.entity.ship.abilities.Cannonable;
import com.talhanation.smallships.world.entity.ship.abilities.Sailable;
import com.talhanation.smallships.world.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PirateShipSpawner {
    private static final int MIN_SPAWN_DISTANCE = 10;
    private static final int MAX_SPAWN_DISTANCE = 20;
    private static final int SPAWN_ATTEMPTS = 24;
    private static final double NEARBY_PIRATE_RADIUS = 192.0D;

    private PirateShipSpawner() {
    }

    public static void tick(ServerLevel level) {
        if (!SmallShipsConfig.Common.pirateShipsEnabled.get()) {
            return;
        }

        int interval = SmallShipsConfig.Common.pirateShipsSpawnInterval.get();
        if (level.getGameTime() % interval != 0L) {
            return;
        }

        List<ServerPlayer> players = new ArrayList<>(level.players());
        Collections.shuffle(players);
        for (ServerPlayer player : players) {
            if (!PirateShipAi.isPlayerAtSea(player)) {
                continue;
            }
            if (level.getRandom().nextDouble() > SmallShipsConfig.Common.pirateShipsSpawnChance.get()) {
                continue;
            }
            if (countNearbyPirateShips(level, player) >= SmallShipsConfig.Common.pirateShipsMaxNearby.get()) {
                continue;
            }
            if (trySpawn(level, player)) {
                return;
            }
        }
    }

    private static int countNearbyPirateShips(ServerLevel level, ServerPlayer player) {
        AABB searchArea = player.getBoundingBox().inflate(NEARBY_PIRATE_RADIUS);
        return level.getEntitiesOfClass(Ship.class, searchArea, Ship::isAiControlled).size();
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = MIN_SPAWN_DISTANCE + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos chunkCheck = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(chunkCheck)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos waterPos = new BlockPos(x, surfaceY - 1, z);
            if (!hasOpenWater(level, waterPos)) {
                continue;
            }

            Ship ship = createPirateShip(level, x + 0.5D, surfaceY, z + 0.5D, random);
            PirateShipAi.beginEmergence(ship, surfaceY);
            AABB emergencePath = ship.getBoundingBox()
                    .expandTowards(0.0D, PirateShipAi.TRANSITION_DEPTH, 0.0D)
                    .inflate(0.5D);
            if (!level.noCollision(ship, emergencePath)) {
                ship.discard();
                continue;
            }

            if (!level.addFreshEntity(ship)) {
                ship.discard();
                continue;
            }
            PirateCaptain.spawn(level, ship);
            return true;
        }
        return false;
    }

    private static boolean hasOpenWater(ServerLevel level, BlockPos center) {
        int[] offsets = {-2, 0, 2};
        for (int xOffset : offsets) {
            for (int zOffset : offsets) {
                BlockPos pos = center.offset(xOffset, 0, zOffset);
                for (int depth = 0; depth <= (int) Math.ceil(PirateShipAi.TRANSITION_DEPTH); depth++) {
                    if (!level.getFluidState(pos.below(depth)).is(FluidTags.WATER)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Ship createPirateShip(ServerLevel level, double x, double y, double z, RandomSource random) {
        int roll = random.nextInt(100);
        Ship ship;
        if (roll < 55) {
            ship = CogEntity.summon(level, x, y, z);
        } else if (roll < 85) {
            ship = GalleyEntity.summon(level, x, y, z);
        } else {
            ship = BriggEntity.summon(level, x, y, z);
        }

        Boat.Type[] variants = Boat.Type.values();
        ship.setVariant(variants[random.nextInt(variants.length)]);
        ship.setYRot(random.nextFloat() * 360.0F);
        ship.setYHeadRot(ship.getYRot());
        ship.setCustomName(Component.translatable("entity.smallships.pirate_ship"));
        ship.setAiControlled(true);

        if (ship instanceof Sailable sailable) {
            sailable.setSailState((byte) 0);
            ship.setData(Ship.SAIL_COLOR, "ghost");
        }
        if (ship instanceof Cannonable cannonable) {
            byte cannonCount = (byte) Math.min(cannonable.getMaxCannonPerSide() * 2, 4);
            cannonable.setCannonCount(cannonCount);
            cannonable.updateCannonCount();
            cannonable.setCannonBallCount(48 + random.nextInt(33));
        }
        if (ship instanceof ContainerShip containerShip) {
            fillCargo(containerShip, random);
        }

        return ship;
    }

    private static void fillCargo(ContainerShip ship, RandomSource random) {
        int rolls = 6 + random.nextInt(5);
        for (int slot = 0; slot < rolls && slot < ship.getContainerSize(); slot++) {
            int lootRoll = random.nextInt(100);
            ItemStack stack;
            if (lootRoll < 18) {
                stack = new ItemStack(Items.OAK_PLANKS, 8 + random.nextInt(17));
            } else if (lootRoll < 34) {
                stack = new ItemStack(Items.IRON_NUGGET, 8 + random.nextInt(17));
            } else if (lootRoll < 48) {
                stack = new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(5));
            } else if (lootRoll < 62) {
                stack = new ItemStack(ModItems.CANNON_BALL, 2 + random.nextInt(5));
            } else if (lootRoll < 76) {
                stack = new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(5));
            } else if (lootRoll < 87) {
                stack = new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(4));
            } else if (lootRoll < 95) {
                stack = new ItemStack(Items.EMERALD, 1 + random.nextInt(3));
            } else {
                stack = new ItemStack(Items.DIAMOND, 1);
            }
            ship.setItem(slot, stack);
        }
    }
}
