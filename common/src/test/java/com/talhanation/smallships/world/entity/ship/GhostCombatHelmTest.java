package com.talhanation.smallships.world.entity.ship;

import java.util.EnumSet;
import java.util.Random;

public final class GhostCombatHelmTest {
    public static void main(String[] args) {
        Random random = new Random(7);
        var helm = new GhostCombatHelm();
        helm.tick(input(0, 0, 0, 5, 0, false), random::nextInt);
        check(helm.maneuver() == GhostCombatHelm.Maneuver.DISENGAGE, "escape close targets");
        helm = new GhostCombatHelm();
        for (int i = 0; i < 80; i++) helm.tick(input(0, 0, 0, 30, 0, true), random::nextInt);
        check(helm.maneuver() == GhostCombatHelm.Maneuver.RECOVER, "stuck recovery");
        helm = new GhostCombatHelm();
        helm.tick(input(0, 0, 0, 30, 0, true), random::nextInt);
        helm.tick(input(0.1, 0, 0, 30, 20, true), random::nextInt);
        check(helm.maneuver() == GhostCombatHelm.Maneuver.WEAVE, "react to a heavy hit");
        helm = new GhostCombatHelm();
        var modes = EnumSet.noneOf(GhostCombatHelm.Maneuver.class);
        for (int i = 0; i < 4000; i++) {
            // Moving ship, persistent bad angle: must try different tactics.
            var order = helm.tick(input(i * 0.1, 0, 0, 30, 0, false), random::nextInt);
            modes.add(helm.maneuver());
            check(Float.isFinite(order.yaw()) && Math.abs(order.yaw()) <= 180, "bounded heading");
            check(order.speed() >= 0 && order.speed() <= 0.25, "bounded speed");
        }
        check(modes.size() >= 5, "varied failed-angle maneuvers");
        var far = new GhostCombatHelm().tick(input(0, 0, 0, 100, 0, false), random::nextInt);
        check(!far.fire(), "no firing beyond range");
        var coincident = new GhostCombatHelm().tick(input(0, 0, 0, 0, 0, false), random::nextInt);
        check(Float.isFinite(coincident.yaw()), "overlapping positions");
        System.out.println("Ghost combat helm regression tests passed");
    }

    private static GhostCombatHelm.Input input(double x, double z, float yaw, double distance,
                                               double damage, boolean angle) {
        return new GhostCombatHelm.Input(x, z, yaw, x + distance, z, 0.15, 0, damage, 250, angle);
    }
    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
