package com.talhanation.smallships.world.entity.ship;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import com.talhanation.smallships.config.GhostShipsConfig;
import com.talhanation.smallships.world.entity.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

final class PirateCaptain {
    private static final String CAPTAIN_TAG = "smallships_pirate_captain";

    private PirateCaptain() {
    }

    static void spawn(ServerLevel level, Ship ship) {
        if (find(ship) != null) {
            return;
        }

        GhostShipsConfig.Profile profile = GhostShipsConfig.forShip(ship);
        if (!spawnMember(level, ship, true, profile.captainHealth)) return;
        int crew = Math.min(Math.max(0, ship.getMaxPassengers() - 1),
                profile.crewMin + level.getRandom().nextInt(profile.crewMax - profile.crewMin + 1));
        for (int i = 0; i < crew; i++) spawnMember(level, ship, false, profile.crewHealth);
    }

    private static boolean spawnMember(ServerLevel level, Ship ship, boolean pilot, double health) {
        Pillager captain = ModEntityTypes.GHOST_CREW.create(level);
        if (captain == null) {
            return false;
        }

        captain.moveTo(ship.getX(), ship.getY() + 1.0D, ship.getZ(), ship.getYRot(), 0.0F);
        captain.setCustomName(Component.translatable(pilot ? "entity.smallships.pirate_captain" : "entity.smallships.ghost_crew"));
        captain.setPersistenceRequired();
        captain.setNoAi(true);
        captain.setCanPickUpLoot(false);
        captain.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(health);
        captain.setHealth((float) health);
        captain.setSilent(true);
        captain.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        captain.addTag(pilot ? CAPTAIN_TAG : "smallships_ghost_crew");

        if (!level.addFreshEntity(captain) || !captain.startRiding(ship, true)) {
            captain.discard();
            return false;
        }
        return true;
    }

    static void tick(Ship ship, @Nullable Player target) {
        Pillager captain = find(ship);
        if (captain == null) {
            return;
        }

        float yaw = ship.getYRot();
        if (target != null) {
            double dx = target.getX() - captain.getX();
            double dz = target.getZ() - captain.getZ();
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        }

        captain.setYRot(yaw);
        captain.setYBodyRot(yaw);
        captain.setYHeadRot(yaw);
    }

    static Entity projectileOwner(Ship ship) {
        Pillager captain = find(ship);
        return captain != null ? captain : ship;
    }

    static void discard(Ship ship) {
        discard(ship, false);
    }

    static void disappearInSmoke(Ship ship) {
        discard(ship, true);
    }

    private static void discard(Ship ship, boolean smoke) {
        for (Entity passenger : java.util.List.copyOf(ship.getPassengers())) {
            if (passenger instanceof GhostCrewEntity || passenger.getTags().contains(CAPTAIN_TAG)) {
                if (smoke && !passenger.isRemoved() && ship.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SMOKE,
                            passenger.getX(), passenger.getY() + passenger.getBbHeight() * 0.5D,
                            passenger.getZ(), 16, 0.25D, 0.4D, 0.25D, 0.025D);
                }
                passenger.discard();
            }
        }
    }

    @Nullable
    private static Pillager find(Ship ship) {
        for (Entity passenger : ship.getPassengers()) {
            if (passenger instanceof Pillager pillager && passenger.getTags().contains(CAPTAIN_TAG)) {
                return pillager;
            }
        }
        return null;
    }
}
