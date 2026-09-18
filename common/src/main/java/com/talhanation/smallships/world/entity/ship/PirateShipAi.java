package com.talhanation.smallships.world.entity.ship;

import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.math.Kalkuel;
import com.talhanation.smallships.world.entity.ship.abilities.Cannonable;
import com.talhanation.smallships.world.entity.ship.abilities.Sailable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

final class PirateShipAi {
    private static final double MAX_CANNON_DISTANCE = 56.0D;
    private static final double CANNON_BALL_SPEED = 2.6D;
    private static final double CANNON_BALL_DRAG = 0.99D;
    private static final double CANNON_BALL_GRAVITY = 0.06D;
    private static final double MAX_TARGET_SPEED = 1.0D;
    private static final double MAX_FLIGHT_TICKS = 40.0D;
    private static final double AIM_LATENCY_COMPENSATION_TICKS = 1.5D;
    static final double TRANSITION_DEPTH = 4.5D;
    private static final int STATE_ACTIVE = 0;
    private static final int STATE_EMERGING = 1;
    private static final int STATE_DIVING = 2;
    private static final int EMERGENCE_TICKS = 20 * 4;
    private static final int DIVE_TICKS = 20 * 3;

    private PirateShipAi() {
    }

    static void tick(Ship ship) {
        if (!(ship.level() instanceof ServerLevel level)) {
            return;
        }

        if (ship.isSunken()) {
            ship.setGhostPitch(0.0F);
            // Also handles passengers restored after loading an already sunken ship.
            PirateCaptain.disappearInSmoke(ship);
            dropCargo(ship, level);
            stop(ship);
            return;
        }

        if (ship.aiState == STATE_EMERGING) {
            tickEmergence(ship, level);
            PirateCaptain.tick(ship, null);
            return;
        }
        if (ship.aiState == STATE_DIVING) {
            tickDive(ship, level);
            PirateCaptain.tick(ship, null);
            return;
        }

        double detectionRange = com.talhanation.smallships.config.GhostShipsConfig.general().detectionRange;
        Player target = findNearestPlayer(level, ship, detectionRange, true);
        if (target == null) {
            if (++ship.aiCombatIdleTicks >= com.talhanation.smallships.config.GhostShipsConfig.forShip(ship).combatIdleSeconds * 20) {
                beginDive(ship);
                tickDive(ship, level);
                PirateCaptain.tick(ship, null);
                return;
            }
        } else {
            ship.aiCombatIdleTicks = 0;
        }

        PirateCaptain.tick(ship, target);
        Navigation navigation = target == null ? patrol(ship, level) : attack(ship, target);
        move(ship, level, navigation);
    }

    static void beginEmergence(Ship ship, double surfaceY) {
        ship.aiState = STATE_EMERGING;
        ship.aiStateTicks = 0;
        ship.aiCombatIdleTicks = 0;
        ship.aiSurfaceY = surfaceY;
        ship.setGhostPitch(0.0F);
        ship.setPos(ship.getX(), surfaceY - TRANSITION_DEPTH, ship.getZ());
        stop(ship);
        if (ship instanceof Sailable sailable) sailable.setSailState((byte) 3);
    }

    private static void beginDive(Ship ship) {
        ship.aiState = STATE_DIVING;
        ship.aiStateTicks = 0;
        ship.aiSurfaceY = Math.max(ship.aiSurfaceY, ship.getY());
        stop(ship);
    }

    private static void tickEmergence(Ship ship, ServerLevel level) {
        if (ship.aiStateTicks == 0) {
            level.playSound(null, ship.getX(), ship.aiSurfaceY, ship.getZ(),
                    SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 2.5F, 0.65F);
        }

        double progress = Math.min(++ship.aiStateTicks / (double) EMERGENCE_TICKS, 1.0D);
        // Boat buoyancy balances gravity with about 65% of the hitbox submerged.
        // The water surface is not the resting height of the entity's bottom.
        double restingDraft = ship.getBbHeight() * 0.65D;
        // Rise bow-first, drop onto the water, then settle with a small rebound.
        double height;
        double pitch;
        if (progress < 0.55D) {
            double rise = smoothStep(progress / 0.55D);
            height = -TRANSITION_DEPTH + (TRANSITION_DEPTH + 0.55D) * rise;
            pitch = 28.0D * Math.sin(rise * Math.PI / 2.0D);
        } else if (progress < 0.8D) {
            double fall = smoothStep((progress - 0.55D) / 0.25D);
            height = 0.55D - 0.7D * fall;
            pitch = 28.0D - 32.0D * fall;
        } else {
            double settle = (progress - 0.8D) / 0.2D;
            height = -restingDraft * smoothStep(settle)
                    - 0.15D * (1.0D - settle) * Math.cos(settle * Math.PI * 2.0D);
            pitch = -4.0D * (1.0D - settle) * Math.cos(settle * Math.PI * 2.0D);
        }
        ship.setGhostPitch((float) pitch);
        ship.setPos(ship.getX(), ship.aiSurfaceY + height, ship.getZ());
        if (ship.aiStateTicks == 64) {
            level.playSound(null, ship.getX(), ship.aiSurfaceY, ship.getZ(),
                    SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 3.0F, 0.65F);
            level.sendParticles(ParticleTypes.SPLASH, ship.getX(), ship.aiSurfaceY, ship.getZ(),
                    70, ship.getBbWidth() * 0.5D, 0.15D, ship.getBbWidth() * 0.5D, 0.25D);
        }
        // Emerge under sail, keeping forward momentum when navigation takes over.
        float maxSpeed = Math.max(0.0F, ship.getAttributes().maxSpeed / (60.0F * 1.15F) * 0.7F);
        float speed = Math.min(maxSpeed, (float) (0.03D + 0.08D * smoothStep(progress)));
        if (!hasWaterAhead(level, ship, ship.getYRot(), 4.0D)) speed = 0.0F;
        ship.setSpeed(speed);
        ship.setRotSpeed(0.0F);
        if (ship instanceof Sailable sailable) sailable.setSailState((byte) 3);
        // Only vertical velocity is reset: height remains controlled by the animation.
        ship.setDeltaMovement(Kalkuel.calculateMotionX(speed, ship.getYRot()), 0.0D,
                Kalkuel.calculateMotionZ(speed, ship.getYRot()));
        spawnTransitionParticles(level, ship, true);

        if (progress >= 1.0D) {
            ship.aiState = STATE_ACTIVE;
            ship.setGhostPitch(0.0F);
            ship.aiStateTicks = 0;
            ship.setPos(ship.getX(), ship.aiSurfaceY - restingDraft, ship.getZ());
        }
    }

    private static void tickDive(Ship ship, ServerLevel level) {
        if (ship.aiStateTicks == 0) {
            level.playSound(null, ship.getX(), ship.aiSurfaceY, ship.getZ(),
                    SoundEvents.CONDUIT_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.7F);
        }

        double progress = Math.min(++ship.aiStateTicks / (double) DIVE_TICKS, 1.0D);
        double easedProgress = smoothStep(progress);
        ship.setPos(ship.getX(), ship.aiSurfaceY - TRANSITION_DEPTH * easedProgress, ship.getZ());
        stop(ship);
        spawnTransitionParticles(level, ship, false);

        if (progress >= 1.0D) {
            ship.discard();
        }
    }

    private static void spawnTransitionParticles(ServerLevel level, Ship ship, boolean emerging) {
        if (ship.aiStateTicks % 3 != 0) {
            return;
        }

        double spread = Math.max(1.0D, ship.getBbWidth() * 0.35D);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                ship.getX(), ship.getY() + 0.6D, ship.getZ(), 8,
                spread, 0.8D, spread, emerging ? 0.08D : 0.02D);
        level.sendParticles(ParticleTypes.SOUL,
                ship.getX(), ship.aiSurfaceY + 0.15D, ship.getZ(), 3,
                spread, 0.2D, spread, 0.015D);
        level.sendParticles(ParticleTypes.SPLASH,
                ship.getX(), ship.aiSurfaceY + 0.05D, ship.getZ(), 6,
                spread, 0.05D, spread, emerging ? 0.12D : 0.05D);
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    static boolean isPlayerAtSea(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && (player.isInWaterOrBubble() || player.getVehicle() instanceof Boat);
    }

    private static Player findNearestPlayer(ServerLevel level, Ship ship, double range, boolean atSeaOnly) {
        double maxDistanceSqr = range * range;
        return level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator() && !player.isCreative())
                .filter(player -> !atSeaOnly || isPlayerAtSea(player))
                .filter(player -> player.distanceToSqr(ship) <= maxDistanceSqr)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(ship)))
                .orElse(null);
    }

    private static Navigation patrol(Ship ship, ServerLevel level) {
        if (--ship.aiPatrolTicks <= 0) {
            ship.aiPatrolTicks = 100 + level.getRandom().nextInt(201);
            ship.aiPatrolYaw = Mth.wrapDegrees(ship.getYRot() - 45.0F + level.getRandom().nextFloat() * 90.0F);
        }
        return new Navigation(ship.aiPatrolYaw, 0.11F, false, null);
    }

    private static Navigation attack(Ship ship, Player target) {
        if (!target.getUUID().equals(ship.aiCombatTargetId)) {
            ship.aiCombatTargetId = target.getUUID();
            ship.aiCombatHelm = new GhostCombatHelm();
        }
        Entity vehicle = target.getVehicle() != null ? target.getVehicle() : target;
        Vec3 velocity = vehicle.getDeltaMovement();
        Vec3 aim = calculateIntercept(ship, target);
        double aimLength = Math.hypot(aim.x, aim.z);
        boolean angle = aimLength > 0.001 && Math.abs(
                Kalkuel.calculateMotionX(1, ship.getYRot()) * aim.x / aimLength
                        + Kalkuel.calculateMotionZ(1, ship.getYRot()) * aim.z / aimLength) <= 0.35;
        var order = ship.aiCombatHelm.tick(new GhostCombatHelm.Input(
                ship.getX(), ship.getZ(), ship.getYRot(), vehicle.getX(), vehicle.getZ(),
                Mth.clamp(velocity.x, -1, 1), Mth.clamp(velocity.z, -1, 1),
                ship.getDamage(), ship.getAttributes().maxHealth, angle), ship.level().getRandom()::nextInt);
        return new Navigation(order.yaw(), order.speed(), order.fire(), target);
    }

    private static void move(Ship ship, ServerLevel level, Navigation navigation) {
        float desiredYaw = Mth.wrapDegrees(navigation.yaw());
        float desiredSpeed = navigation.speed();

        if (ship.aiAvoidTicks > 0 && isClearCourse(level, ship, ship.aiAvoidYaw)) {
            ship.aiAvoidTicks--;
            desiredYaw = ship.aiAvoidYaw;
            desiredSpeed = Math.min(desiredSpeed, 0.12F);
        } else if (!isClearCourse(level, ship, desiredYaw)
                || !isClearCourse(level, ship, ship.getYRot())) {
            float bestYaw = ship.getYRot();
            double bestScore = -Double.MAX_VALUE;
            for (int offset : new int[]{0, 30, -30, 60, -60, 90, -90, 135, -135, 180}) {
                float candidate = Mth.wrapDegrees(desiredYaw + offset);
                if (!isClearCourse(level, ship, candidate)) continue;
                double score = -Math.abs(offset) - 0.35 * Math.abs(Mth.wrapDegrees(candidate - ship.getYRot()));
                if (score > bestScore) { bestScore = score; bestYaw = candidate; }
            }
            desiredYaw = bestYaw;
            desiredSpeed = bestScore == -Double.MAX_VALUE ? 0 : Math.min(desiredSpeed, 0.10F);
            ship.aiAvoidYaw = desiredYaw;
            ship.aiAvoidTicks = bestScore == -Double.MAX_VALUE ? 0 : 40;
        }

        float maxShipSpeed = ship.getAttributes().maxSpeed / (60.0F * 1.15F);
        desiredSpeed = Math.min(desiredSpeed, Math.max(0.0F, maxShipSpeed * 0.7F));
        float yaw = Mth.approachDegrees(ship.getYRot(), desiredYaw, 1.6F);
        float speed = ship.isInWater() ? Mth.approach(ship.getSpeed(), desiredSpeed, 0.008F) : 0.0F;

        ship.setYRot(yaw);
        ship.setYHeadRot(yaw);
        ship.setRotSpeed(0.0F);
        ship.setSpeed(speed);
        ship.setDeltaMovement(
                Kalkuel.calculateMotionX(speed, yaw),
                ship.getDeltaMovement().y,
                Kalkuel.calculateMotionZ(speed, yaw)
        );

        if (ship instanceof Sailable sailable) {
            sailable.setSailState((byte) (navigation.target() == null ? 2 : 3));
        }

        if (navigation.fireCannons() && navigation.target() != null) {
            fireCannons(ship, navigation.target());
        }
    }

    private static void fireCannons(Ship ship, Player target) {
        if (!(ship instanceof Cannonable cannonable)) {
            return;
        }

        Vec3 aim = calculateIntercept(ship, target);
        Vec3 horizontalAim = new Vec3(aim.x, 0.0D, aim.z);
        double distance = horizontalAim.length();
        if (distance < 0.001D) {
            return;
        }

        Vec3 forward = new Vec3(
                Kalkuel.calculateMotionX(1.0F, ship.getYRot()),
                0.0D,
                Kalkuel.calculateMotionZ(1.0F, ship.getYRot())
        );
        Vec3 normalizedAim = horizontalAim.normalize();
        if (Math.abs(forward.dot(normalizedAim)) > 0.35D) {
            return;
        }

        cannonable.triggerAiCannons(
                aim,
                aim.y,
                PirateCaptain.projectileOwner(ship),
                CANNON_BALL_SPEED,
                com.talhanation.smallships.config.GhostShipsConfig.general().cannonInaccuracy
        );
    }

    private static Vec3 calculateIntercept(Ship ship, Player target) {
        Vec3 origin = new Vec3(ship.getX(), ship.getY() + 1.0D, ship.getZ());
        Entity motionSource = target.getVehicle() != null ? target.getVehicle() : target;
        Vec3 targetPosition = motionSource instanceof Ship targetShip
                ? new Vec3(targetShip.getX(), targetShip.getY() + targetShip.getBbHeight() * 0.55D, targetShip.getZ())
                : target.getEyePosition();
        TargetMotion targetMotion = getTargetMotion(motionSource);
        Vec3 targetVelocity = targetMotion.velocity();
        if (targetVelocity.horizontalDistance() > MAX_TARGET_SPEED) {
            targetVelocity = targetVelocity.normalize().scale(MAX_TARGET_SPEED);
            targetMotion = new TargetMotion(targetVelocity, targetMotion.yaw(), targetMotion.turnRate());
        }

        Vec3 relativePosition = targetPosition.subtract(origin);
        Vec3 bestVelocity = relativePosition.normalize().scale(CANNON_BALL_SPEED);
        double bestError = Double.MAX_VALUE;

        for (double flightTicks = 4.0D; flightTicks <= MAX_FLIGHT_TICKS; flightTicks += 0.25D) {
            double dragDistanceFactor = (1.0D - Math.pow(CANNON_BALL_DRAG, flightTicks)) / (1.0D - CANNON_BALL_DRAG);
            double predictionTicks = flightTicks + AIM_LATENCY_COMPENSATION_TICKS;
            Vec3 predictedTarget = relativePosition.add(predictTargetDisplacement(targetMotion, predictionTicks));

            double requiredX = predictedTarget.x / dragDistanceFactor;
            double requiredZ = predictedTarget.z / dragDistanceFactor;
            double gravityDrop = (CANNON_BALL_GRAVITY / (1.0D - CANNON_BALL_DRAG))
                    * (flightTicks - dragDistanceFactor);
            double requiredY = (predictedTarget.y + gravityDrop) / dragDistanceFactor;
            Vec3 requiredVelocity = new Vec3(requiredX, requiredY, requiredZ);
            double speedError = Math.abs(requiredVelocity.length() - CANNON_BALL_SPEED);

            if (speedError < bestError) {
                bestError = speedError;
                bestVelocity = requiredVelocity;
            }
        }

        return bestVelocity.normalize();
    }

    private static TargetMotion getTargetMotion(Entity motionSource) {
        if (motionSource instanceof Ship targetShip) {
            float speed = targetShip.getSpeed();
            float yaw = targetShip.getYRot();
            Vec3 velocity = new Vec3(
                    Kalkuel.calculateMotionX(speed, yaw),
                    0.0D,
                    Kalkuel.calculateMotionZ(speed, yaw)
            );
            return new TargetMotion(velocity, yaw, targetShip.getRotSpeed());
        }

        return new TargetMotion(
                motionSource.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D),
                motionSource.getYRot(),
                0.0F
        );
    }

    private static Vec3 predictTargetDisplacement(TargetMotion motion, double ticks) {
        double turnRateRadians = Math.toRadians(motion.turnRate());
        double horizontalSpeed = motion.velocity().horizontalDistance();
        if (horizontalSpeed < 0.001D || Math.abs(turnRateRadians) < 1.0E-4D) {
            return motion.velocity().scale(ticks);
        }

        double signedSpeed = motion.velocity().dot(new Vec3(
                Kalkuel.calculateMotionX(1.0F, motion.yaw()),
                0.0D,
                Kalkuel.calculateMotionZ(1.0F, motion.yaw())
        )) < 0.0D ? -horizontalSpeed : horizontalSpeed;
        double startYaw = Math.toRadians(motion.yaw());
        double endYaw = startYaw + turnRateRadians * ticks;
        double scale = signedSpeed / turnRateRadians;
        return new Vec3(
                scale * (Math.cos(endYaw) - Math.cos(startYaw)),
                0.0D,
                scale * (Math.sin(endYaw) - Math.sin(startYaw))
        );
    }

    private static boolean isClearCourse(ServerLevel level, Ship ship, float yaw) {
        if (!hasWaterAhead(level, ship, yaw, 4) || !hasWaterAhead(level, ship, yaw, 8)
                || !hasWaterAhead(level, ship, yaw, 12)) return false;
        double dx = Kalkuel.calculateMotionX(4, yaw);
        double dz = Kalkuel.calculateMotionZ(4, yaw);
        return level.noCollision(ship, ship.getBoundingBox().move(dx, 0, dz));
    }

    private static boolean hasWaterAhead(ServerLevel level, Ship ship, float yaw, double distance) {
        double x = ship.getX() + Kalkuel.calculateMotionX((float) distance, yaw);
        double z = ship.getZ() + Kalkuel.calculateMotionZ((float) distance, yaw);
        BlockPos surface = BlockPos.containing(x, ship.getY() - 0.5D, z);
        if (!level.hasChunkAt(surface)) {
            return false;
        }

        for (int depth = 0; depth <= 2; depth++) {
            if (level.getFluidState(surface.below(depth)).is(net.minecraft.tags.FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static void dropCargo(Ship ship, ServerLevel level) {
        if (ship.aiLootDropped) {
            return;
        }
        ship.aiLootDropped = true;

        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            PirateLoot.drop(ship, level);
        } else {
            com.talhanation.smallships.SmallShipsMod.LOGGER.info("Ghost rewards disabled by doEntityDrops=false at {}", ship.blockPosition());
        }
        if (ship instanceof ContainerShip containerShip) {
            containerShip.clearContent();
        }
    }

    private static void stop(Ship ship) {
        ship.setSpeed(0.0F);
        ship.setRotSpeed(0.0F);
        ship.setDeltaMovement(0.0D, ship.getDeltaMovement().y, 0.0D);
        if (ship instanceof Sailable sailable) {
            sailable.setSailState((byte) 0);
        }
    }

    private record Navigation(float yaw, float speed, boolean fireCannons, Player target) {
    }

    private record TargetMotion(Vec3 velocity, float yaw, float turnRate) {
    }
}
