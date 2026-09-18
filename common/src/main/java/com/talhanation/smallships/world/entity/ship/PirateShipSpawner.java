package com.talhanation.smallships.world.entity.ship;


import com.talhanation.smallships.world.entity.ship.abilities.Cannonable;
import com.talhanation.smallships.world.entity.ship.abilities.Sailable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PirateShipSpawner {
    private static final int SPAWN_ATTEMPTS = 24;
    private static final double NEARBY_PIRATE_RADIUS = 192.0D;

    private PirateShipSpawner() {
    }

    public static void tick(ServerLevel level) {
        if (!com.talhanation.smallships.config.GhostShipsConfig.general().enabled) {
            return;
        }

        int interval = com.talhanation.smallships.config.GhostShipsConfig.general().spawnIntervalTicks;
        if (level.getGameTime() % interval != 0L) {
            return;
        }

        List<ServerPlayer> players = new ArrayList<>(level.players());
        Collections.shuffle(players);
        for (ServerPlayer player : players) {
            if (!PirateShipAi.isPlayerAtSea(player)
                    || !(player.getVehicle() instanceof Ship playerShip)
                    || playerShip.isAiControlled()
                    || playerShip.isSunken()
                    || !(playerShip instanceof Cannonable armedShip)
                    || armedShip.getCannonCount() < 1
                    || armedShip.getCannonBallCount() < com.talhanation.smallships.config.GhostShipsConfig.general().requiredCannonBalls) {
                continue;
            }
            if (level.getRandom().nextDouble() >= com.talhanation.smallships.config.GhostShipsConfig.general().spawnChance) {
                continue;
            }
            if (countNearbyPirateShips(level, player) >= com.talhanation.smallships.config.GhostShipsConfig.general().maxNearby) {
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
        var config = com.talhanation.smallships.config.GhostShipsConfig.general();
        var origin = player.getVehicle() != null ? player.getVehicle() : player;
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = config.spawnDistanceMin + random.nextInt(config.spawnDistanceMax - config.spawnDistanceMin + 1);
            int x = (int) Math.floor(origin.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(origin.getZ() + Math.sin(angle) * distance);
            BlockPos chunkCheck = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(chunkCheck)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos waterPos = new BlockPos(x, surfaceY - 1, z);
            if (!hasOpenWater(level, waterPos)) {
                continue;
            }

            // Check all nearby boats, not just the player chosen for this spawn attempt.
            AABB clearance = new AABB(x + 0.5D, surfaceY, z + 0.5D,
                    x + 0.5D, surfaceY, z + 0.5D).inflate(config.spawnDistanceMin, 16, config.spawnDistanceMin);
            if (!level.getEntitiesOfClass(Boat.class, clearance, boat ->
                    Math.hypot(boat.getX() - (x + 0.5D), boat.getZ() - (z + 0.5D))
                            < config.spawnDistanceMin).isEmpty()) continue;

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
            byte cannonCount = (byte) (cannonable.getMaxCannonPerSide() * 2);
            cannonable.setCannonCount(cannonCount);
            cannonable.updateCannonCount();
            cannonable.setCannonBallCount(48 + random.nextInt(33));
        }
        Attributes attributes = ship.getAttributes();
        attributes.maxHealth = (float) com.talhanation.smallships.config.GhostShipsConfig.forShip(ship).health;
        ship.setData(Ship.ATTRIBUTES, attributes.getSaveData());

        return ship;
    }

}
