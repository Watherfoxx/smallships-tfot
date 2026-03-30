package com.talhanation.smallships.world.item;

import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.entity.ship.abilities.Bannerable;
import com.talhanation.smallships.world.entity.ship.abilities.Sailable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ShipItem extends BoatItem {
    public static final String TAG_SAIL_COLOR = "SailColor";
    public static final String TAG_BANNER = "Banner";
    public static final String TAG_CANNON_COUNT = "CannonCount";
    public static final String TAG_CANNON_BALL_COUNT = "CannonBallCount";
    private final Boat.Type shipType;

    public ShipItem(Boat.Type type, Properties properties) {
        super(false, type, properties);
        this.shipType = type;
    }

    protected abstract @NotNull Boat getBoat(@NotNull Level level, @NotNull HitResult hitResult);
    protected abstract int getShipMaxHealth();

    @Override
    public int getMaxDamage(@NotNull ItemStack itemStack) {
        return Math.max(1, this.getShipMaxHealth());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand interactionHand) {
        ItemStack itemStack = player.getItemInHand(interactionHand);
        if (itemStack.isDamageableItem() && itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
            return InteractionResultHolder.fail(itemStack);
        }
        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemStack);
        }

        Vec3 viewVector = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(viewVector.scale(5.0)).inflate(1.0D), EntitySelector.NO_SPECTATORS.and(Entity::isPickable));
        if (!entities.isEmpty()) {
            Vec3 eyePosition = player.getEyePosition();
            for (Entity entity : entities) {
                AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (aabb.contains(eyePosition)) {
                    return InteractionResultHolder.pass(itemStack);
                }
            }
        }

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Boat boat = this.getBoat(level, hitResult);
            boat.setVariant(this.shipType);
            boat.moveTo(hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z, player.getYRot(), 0.0F);
            if (!level.noCollision(boat, boat.getBoundingBox())) {
                return InteractionResultHolder.fail(itemStack);
            }
            if (!level.isClientSide) {
                applyItemDataToBoat(boat, itemStack);
                level.addFreshEntity(boat);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, ((BlockHitResult) hitResult).getBlockPos());
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
        }

        return InteractionResultHolder.pass(itemStack);
    }

    private void applyItemDataToBoat(@NotNull Boat boat, @NotNull ItemStack itemStack) {
        if (!(boat instanceof Ship ship)) {
            return;
        }

        if (itemStack.isDamageableItem()) {
            int maxDamage = Math.max(1, itemStack.getMaxDamage());
            int itemDamage = Math.min(itemStack.getDamageValue(), maxDamage);
            int shipMaxHealth = Math.max(1, Math.round(ship.getAttributes().maxHealth));
            int maxDurabilityOffset = Math.max(0, maxDamage - shipMaxHealth);
            int shipDamage = Mth.clamp(itemDamage - maxDurabilityOffset, 0, shipMaxHealth);
            ship.setDamage(shipDamage);
        }

        CompoundTag tag = itemStack.getTag();
        if (tag == null) {
            return;
        }

        if (ship instanceof Sailable && tag.contains(TAG_SAIL_COLOR, Tag.TAG_STRING)) {
            ship.setData(Ship.SAIL_COLOR, tag.getString(TAG_SAIL_COLOR));
        }

        if (ship instanceof Bannerable && tag.contains(TAG_BANNER, Tag.TAG_COMPOUND)) {
            ship.setData(Ship.BANNER, ItemStack.of(tag.getCompound(TAG_BANNER)));
        }
        if (ship instanceof com.talhanation.smallships.world.entity.ship.abilities.Cannonable cannonShip
                && tag.contains(TAG_CANNON_COUNT)) {
            double cannonCount = 0;
            if (tag.contains(TAG_CANNON_COUNT, Tag.TAG_DOUBLE)) {
                cannonCount = tag.getDouble(TAG_CANNON_COUNT);
            } else if (tag.contains(TAG_CANNON_COUNT, Tag.TAG_INT)) {
                cannonCount = tag.getInt(TAG_CANNON_COUNT);
            } else if (tag.contains(TAG_CANNON_COUNT, Tag.TAG_BYTE)) {
                cannonCount = tag.getByte(TAG_CANNON_COUNT);
            }
            int maxCannons = cannonShip.getMaxCannonPerSide() * 2;
            cannonShip.setCannonCount((byte) Mth.clamp(Math.round(cannonCount), 0, maxCannons));
            cannonShip.updateCannonCount();
        }
        if (ship instanceof com.talhanation.smallships.world.entity.ship.abilities.Cannonable cannonShip
                && tag.contains(TAG_CANNON_BALL_COUNT)) {
            int cannonBallCount = 0;
            if (tag.contains(TAG_CANNON_BALL_COUNT, Tag.TAG_DOUBLE)) {
                cannonBallCount = Mth.floor(tag.getDouble(TAG_CANNON_BALL_COUNT));
            } else if (tag.contains(TAG_CANNON_BALL_COUNT, Tag.TAG_INT)) {
                cannonBallCount = tag.getInt(TAG_CANNON_BALL_COUNT);
            } else if (tag.contains(TAG_CANNON_BALL_COUNT, Tag.TAG_BYTE)) {
                cannonBallCount = tag.getByte(TAG_CANNON_BALL_COUNT);
            }
            cannonShip.setCannonBallCount(Math.max(cannonBallCount, 0));
        }
    }
}
