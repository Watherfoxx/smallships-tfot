package com.talhanation.smallships.world.entity.ship;

import java.util.function.IntUnaryOperator;

/** Short-lived tactical memory. No world access: decisions can be regression-tested. */
public final class GhostCombatHelm {
    public enum Maneuver { BROADSIDE, ORBIT, CROSS_BOW, CUT_STERN, WEAVE, DISENGAGE, RECOVER }
    public record Input(double x, double z, float yaw, double targetX, double targetZ,
                        double targetVx, double targetVz, double damage, double maxHealth,
                        boolean firingAngle) {}
    public record Order(float yaw, float speed, boolean fire) {}
    private Maneuver maneuver = Maneuver.BROADSIDE;
    private int remaining, noAngleTicks, movementTicks, reactionCooldown;
    private int side = 1;
    private double checkpointX, checkpointZ, lastDamage;
    private float escapeYaw;
    private boolean initialized;

    public Maneuver maneuver() { return maneuver; }

    public Order tick(Input input, IntUnaryOperator random) {
        double dx = input.targetX - input.x, dz = input.targetZ - input.z;
        double distance = Math.hypot(dx, dz);
        float bearing = bearing(dx, dz);
        if (!initialized) {
            initialized = true;
            side = Math.abs(wrap(bearing + 90 - input.yaw)) <= Math.abs(wrap(bearing - 90 - input.yaw)) ? 1 : -1;
            checkpointX = input.x;
            checkpointZ = input.z;
            lastDamage = input.damage;
            remaining = 100 + random.applyAsInt(81);
        }
        remaining--;
        reactionCooldown = Math.max(0, reactionCooldown - 1);
        noAngleTicks = input.firingAngle || distance > 56 ? 0 : noAngleTicks + 1;
        boolean stuck = false;
        if (++movementTicks >= 80) {
            stuck = Math.hypot(input.x - checkpointX, input.z - checkpointZ) < 1.0;
            checkpointX = input.x;
            checkpointZ = input.z;
            movementTicks = 0;
        }
        boolean heavyHit = input.damage - lastDamage >= Math.max(5, input.maxHealth * 0.04);
        lastDamage = input.damage;
        if (stuck && maneuver != Maneuver.RECOVER) {
            side = -side;
            maneuver = Maneuver.RECOVER;
            escapeYaw = input.yaw + side * (100 + random.applyAsInt(61));
            remaining = 100;
            noAngleTicks = 0;
        } else if (distance < 12 && maneuver != Maneuver.DISENGAGE && maneuver != Maneuver.RECOVER) {
            maneuver = Maneuver.DISENGAGE;
            remaining = 80;
            noAngleTicks = 0;
        } else if (heavyHit && reactionCooldown == 0 && maneuver != Maneuver.RECOVER) {
            maneuver = input.damage > input.maxHealth * 0.65 ? Maneuver.DISENGAGE : Maneuver.WEAVE;
            side = -side;
            remaining = 80 + random.applyAsInt(61);
            reactionCooldown = 160;
            noAngleTicks = 0;
        } else if (remaining <= 0 || noAngleTicks >= 120) {
            // An unsuccessful attack must choose something different, not repeat the same turn.
            if (noAngleTicks >= 120 || random.applyAsInt(3) == 0) side = -side;
            int next = random.applyAsInt(5);
            if (next == maneuver.ordinal()) next = (next + 1) % 5;
            maneuver = Maneuver.values()[next];
            remaining = 100 + random.applyAsInt(101);
            noAngleTicks = 0;
        }

        float yaw;
        float speed;
        if (maneuver == Maneuver.RECOVER) {
            yaw = escapeYaw;
            speed = 0.12F;
        } else if (maneuver == Maneuver.DISENGAGE && distance < 36) {
            yaw = bearing + 180 + side * 25;
            speed = 0.24F;
        } else if (distance > 44) {
            // Lead the target while closing, instead of following its wake forever.
            double lead = Math.min(35, distance * 0.7);
            yaw = bearing(dx + input.targetVx * lead, dz + input.targetVz * lead);
            speed = 0.25F;
        } else {
            double targetSpeed = Math.hypot(input.targetVx, input.targetVz);
            double forwardX = targetSpeed > 0.025 ? input.targetVx / targetSpeed : dx / Math.max(distance, 0.01);
            double forwardZ = targetSpeed > 0.025 ? input.targetVz / targetSpeed : dz / Math.max(distance, 0.01);
            switch (maneuver) {
                case CROSS_BOW, CUT_STERN -> {
                    double along = maneuver == Maneuver.CROSS_BOW ? 16 : -14;
                    double goalX = dx + forwardX * along - forwardZ * side * 20;
                    double goalZ = dz + forwardZ * along + forwardX * side * 20;
                    yaw = Math.hypot(goalX, goalZ) > 7 ? bearing(goalX, goalZ) : bearing + side * 90;
                    speed = 0.21F;
                }
                case WEAVE -> {
                    yaw = bearing + side * (90 + 30 * (float) Math.sin(remaining / 22.0));
                    speed = 0.17F;
                }
                default -> {
                    double preferredRange = maneuver == Maneuver.ORBIT ? 23 : 32;
                    float correction = (float) Math.max(-32, Math.min(32, (preferredRange - distance) * 2));
                    yaw = bearing + side * (90 + correction);
                    speed = maneuver == Maneuver.ORBIT ? 0.19F : input.firingAngle ? 0.09F : 0.14F;
                }
            }
        }
        // Slow through tight turns rather than making an endless wide circle.
        float turn = Math.abs(wrap(yaw - input.yaw));
        if (turn > 65) speed *= 0.55F;
        return new Order(wrap(yaw), speed, distance <= 56);
    }

    private static float bearing(double x, double z) { return (float) Math.toDegrees(Math.atan2(-x, z)); }
    private static float wrap(float angle) { return (angle % 360 + 540) % 360 - 180; }
}
