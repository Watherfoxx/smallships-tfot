package com.talhanation.smallships.world.entity.ship.abilities;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.world.entity.projectile.Cannon;
import com.talhanation.smallships.world.entity.ship.ContainerShip;
import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.item.ModItems;
import com.talhanation.smallships.world.item.ShipItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface Cannonable extends Ability {
    float getDefaultCannonPower();
    CannonPosition getCannonPosition(int index);
    byte getMaxCannonPerSide();

    default void tickCannonShip() {
        for(Cannon cannon : this.getCannons()) {
            cannon.tick();
            if(self().isCannonKeyPressed() && canShoot()){
                this.triggerCannon(cannon);
            }
        }
    }
    default void triggerCannon(Cannon cannon){
        if(cannon.canShootDirection()) cannon.trigger();
    }

    //Important for reflection
    default void triggerCannons(Vec3 shootVec, double yShootVec, LivingEntity driverEntity, double speed, double accuracy){
        if(canShoot()){
            for(Cannon cannon : this.getCannons())
                this.triggerCannonAdvanced(cannon,shootVec, yShootVec, driverEntity, speed, accuracy);
        }
    }
    default void triggerCannonAdvanced(Cannon cannon, Vec3 shootVec, double yShootVec, LivingEntity driverEntity, double speed, double accuracy){
        if(cannon.canShootDirection()) cannon.trigger(shootVec, yShootVec, driverEntity, speed, accuracy);
    }

    default void defineCannonShipSynchedData() {
        self().getEntityData().define(Ship.CANNON_POWER, this.getDefaultCannonPower());
        self().getEntityData().define(Ship.CANNON_COUNT, (byte) 0);
        self().getEntityData().define(Ship.CANNON_BALL_COUNT, 0);
    }

    @SuppressWarnings("unused")
    default void readCannonShipSaveData(CompoundTag tag) {
        if (tag.contains("CannonCount")) {
            this.setCannonCount((byte) Math.round(tag.getDouble("CannonCount")));
            this.updateCannonCount();
        }
        if (tag.contains(ShipItem.TAG_CANNON_BALL_COUNT)) {
            int cannonBallCount = 0;
            if (tag.contains(ShipItem.TAG_CANNON_BALL_COUNT, Tag.TAG_DOUBLE)) {
                cannonBallCount = (int) Math.floor(tag.getDouble(ShipItem.TAG_CANNON_BALL_COUNT));
            } else if (tag.contains(ShipItem.TAG_CANNON_BALL_COUNT, Tag.TAG_INT)) {
                cannonBallCount = tag.getInt(ShipItem.TAG_CANNON_BALL_COUNT);
            } else if (tag.contains(ShipItem.TAG_CANNON_BALL_COUNT, Tag.TAG_BYTE)) {
                cannonBallCount = tag.getByte(ShipItem.TAG_CANNON_BALL_COUNT);
            }
            this.setCannonBallCount(Math.max(cannonBallCount, 0));
        }
    }

    @SuppressWarnings("unused")
    default void addCannonShipSaveData(CompoundTag tag) {
        tag.putDouble("CannonCount", this.getCannonCount());
        tag.putInt(ShipItem.TAG_CANNON_BALL_COUNT, this.getCannonBallCount());
    }

    default float getCannonModifier() {
        return this.getCannonCount() * SmallShipsConfig.Common.shipGeneralCannonModifier.get().floatValue();
    }

    default void updateCannonCount(){
        byte cannons = this.getCannonCount();

        this.getCannons().clear();
        for (int i = 0; i < cannons; i++) {
            CannonPosition cannonPosition = this.getCannonPosition(i);

            if(cannonPosition!= null){
                Cannon cannon = new Cannon(self(), cannonPosition);
                this.getCannons().add(cannon);
            }
        }

        this.setCannonCount(cannons);
    }
    default boolean interactCannon(Player player, InteractionHand interactionHand) {
        ItemStack item = player.getItemInHand(interactionHand);
        byte cannonCount = this.getCannonCount();
        if (item.getItem() == ModItems.CANNON && self() instanceof ContainerShip) {
            if (cannonCount >= getMaxCannonPerSide() * 2) {
                return false;
            }
            else {
                this.setCannonCount((byte) (cannonCount + 1));

                self().level().playSound(player, self().getX(), self().getY() + 4 , self().getZ(), SoundEvents.ARMOR_EQUIP_CHAIN, self().getSoundSource(), 15.0F, 1.5F);
                if (!player.isCreative()) item.shrink(1);

                this.updateCannonCount();
            }
            return true;
        } else if (item.getItem() instanceof AxeItem && cannonCount > 0) {
            this.setCannonCount((byte) (cannonCount - 1));

            self().spawnAtLocation(ModItems.CANNON);
            self().level().playSound(player, self().getX(), self().getY() + 4 , self().getZ(), SoundEvents.ARMOR_EQUIP_CHAIN, self().getSoundSource(), 15.0F, 1.0F);
            return true;
        }
        return false;
    }

    default boolean canShoot() {
        return this.getCannonBallCount() > 0;
    }

    default void consumeCannonBall() {
        int cannonBallCount = this.getCannonBallCount();
        if (cannonBallCount > 0) {
            this.setCannonBallCount(cannonBallCount - 1);
        }
    }

    default ResourceLocation getTextureLocation() {
        return new ResourceLocation(SmallShipsMod.MOD_ID,"textures/entity/cannon/ship_cannon.png");
    }

    default void setCannonCount(byte x) {
        self().getEntityData().set(Ship.CANNON_COUNT, x);
    }
    default byte getCannonCount() {
        return self().getEntityData().get(Ship.CANNON_COUNT);
    }

    default List<Cannon> getCannons() {
        return self().CANNONS;
    }

    default void setCannonBallCount(int x) {
        self().getEntityData().set(Ship.CANNON_BALL_COUNT, Math.max(x, 0));
    }

    default int getCannonBallCount() {
        return self().getEntityData().get(Ship.CANNON_BALL_COUNT);
    }

    default void cannonShipDestroyed(Level level, Ship ship){
        for(int i = 0; i < getCannonCount(); i++){
            ship.spawnAtLocation(ModItems.CANNON,4);
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    class CannonPosition {
        public final double x;
        public final double y;
        public final double z;
        public final boolean isRightSided;

        public CannonPosition(double x, double y, double z, boolean isRightSided) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isRightSided = isRightSided;
        }
    }
}
