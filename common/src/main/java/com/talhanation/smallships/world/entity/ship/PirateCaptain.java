package com.talhanation.smallships.world.entity.ship;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class PirateCaptain {
    private static final String CAPTAIN_TAG = "smallships_pirate_captain";

    private PirateCaptain() {
    }

    static void spawn(ServerLevel level, Ship ship) {
        if (find(ship) != null) {
            return;
        }

        Pillager captain = EntityType.PILLAGER.create(level);
        if (captain == null) {
            return;
        }

        captain.moveTo(ship.getX(), ship.getY() + 1.0D, ship.getZ(), ship.getYRot(), 0.0F);
        captain.setCustomName(Component.translatable("entity.smallships.pirate_captain"));
        captain.setPersistenceRequired();
        captain.setNoAi(true);
        captain.setCanPickUpLoot(false);
        captain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        captain.addTag(CAPTAIN_TAG);

        if (!level.addFreshEntity(captain) || !captain.startRiding(ship, true)) {
            captain.discard();
        }
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
        Pillager captain = find(ship);
        if (captain != null) {
            captain.discard();
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
