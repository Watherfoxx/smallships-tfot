package com.talhanation.smallships.mixin.vanilla;

import com.talhanation.smallships.duck.VanillaBoatAccess;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Boat.class)
public class VanillaBoatMixin implements VanillaBoatAccess {
    @Unique
    private static final String SMALLSHIPS_OWNER_TAG = "SmallShipsOwner";
    @Unique
    private static final String SMALLSHIPS_DAMAGE_TAG = "SmallShipsDamage";
    @Unique
    private static final int VANILLA_BOAT_MAX_DAMAGE = 100;
    @Unique
    private static final EntityDataAccessor<Boolean> SMALLSHIPS_SUNKEN = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final Component NOT_OWNER_MESSAGE = Component.translatable("message.smallships.ship.not_owner").withStyle(ChatFormatting.RED);
    @Unique
    private static final Component OWNER_PICKUP_HINT_MESSAGE = Component.translatable("message.smallships.ship.owner_pickup_hint").withStyle(ChatFormatting.RED);

    @Unique
    @Nullable
    private UUID smallships$ownerUuid;
    @Unique
    private float smallships$damage;

    @Unique
    private Boat smallships$self() {
        return (Boat) (Object) this;
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void smallships$defineSynchedData(CallbackInfo ci) {
        this.smallships$self().getEntityData().define(SMALLSHIPS_SUNKEN, false);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void smallships$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!this.smallships$isSimpleVanillaBoat()) {
            return;
        }
        this.smallships$ownerUuid = tag.hasUUID(SMALLSHIPS_OWNER_TAG) ? tag.getUUID(SMALLSHIPS_OWNER_TAG) : null;
        this.smallships$damage = Mth.clamp(tag.contains(SMALLSHIPS_DAMAGE_TAG, Tag.TAG_FLOAT) ? tag.getFloat(SMALLSHIPS_DAMAGE_TAG) : 0.0F, 0.0F, VANILLA_BOAT_MAX_DAMAGE);
        this.smallships$setSunken(this.smallships$hasNoDurability());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void smallships$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (!this.smallships$isSimpleVanillaBoat()) {
            return;
        }
        if (this.smallships$ownerUuid != null) {
            tag.putUUID(SMALLSHIPS_OWNER_TAG, this.smallships$ownerUuid);
        }
        if (this.smallships$damage > 0.0F) {
            tag.putFloat(SMALLSHIPS_DAMAGE_TAG, this.smallships$damage);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void smallships$hurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.smallships$isSimpleVanillaBoat() || this.smallships$self().isInvulnerableTo(damageSource)) {
            return;
        }

        if (!this.smallships$self().level().isClientSide && !this.smallships$self().isRemoved()) {
            if (damageSource.getEntity() instanceof Player player && player.isCrouching()) {
                InteractionResult pickupResult = this.smallships$tryPickup(player);
                if (pickupResult != InteractionResult.PASS) {
                    cir.setReturnValue(true);
                    return;
                }
            }

            this.smallships$damage = Mth.clamp(this.smallships$damage + amount, 0.0F, VANILLA_BOAT_MAX_DAMAGE);
            if (this.smallships$hasNoDurability()) {
                this.smallships$setSunken(true);
                this.smallships$self().ejectPassengers();
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canAddPassenger(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void smallships$preventMountingSunkenBoat(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        if (this.smallships$isSimpleVanillaBoat() && this.smallships$isSunken()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void smallships$ejectPassengersFromSunkenBoat(CallbackInfo ci) {
        if (this.smallships$isSimpleVanillaBoat() && this.smallships$isSunken() && !this.smallships$self().level().isClientSide) {
            this.smallships$self().ejectPassengers();
        }
    }

    @Inject(method = "floatBoat", at = @At("TAIL"))
    private void smallships$sinkBrokenBoat(CallbackInfo ci) {
        if (this.smallships$isSimpleVanillaBoat() && this.smallships$isSunken()) {
            this.smallships$self().setDeltaMovement(0.0D, -0.2D, 0.0D);
        }
    }

    @Unique
    private InteractionResult smallships$tryPickup(Player player) {
        if (this.smallships$self().getPassengers().stream().anyMatch(passenger -> passenger instanceof Player)) {
            return InteractionResult.PASS;
        }
        if (!this.smallships$canPickup(player)) {
            if (!this.smallships$self().level().isClientSide) {
                player.sendSystemMessage(NOT_OWNER_MESSAGE);
            }
            return InteractionResult.FAIL;
        }
        if (this.smallships$self().level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack boatStack = new ItemStack(this.smallships$self().getDropItem());
        if (boatStack.isDamageableItem()) {
            boatStack.setDamageValue(Mth.clamp(Math.round(this.smallships$damage), 0, boatStack.getMaxDamage()));
        }
        this.smallships$self().spawnAtLocation(boatStack);
        this.smallships$self().discard();
        return InteractionResult.SUCCESS;
    }

    @Unique
    private boolean smallships$canPickup(Player player) {
        return this.smallships$isOwner(player) || this.smallships$hasNoDurability();
    }

    @Unique
    private boolean smallships$isOwner(Player player) {
        return this.smallships$ownerUuid == null || player.getUUID().equals(this.smallships$ownerUuid);
    }

    @Unique
    private boolean smallships$hasNoDurability() {
        return this.smallships$damage >= VANILLA_BOAT_MAX_DAMAGE;
    }

    @Unique
    private boolean smallships$isSunken() {
        return this.smallships$self().getEntityData().get(SMALLSHIPS_SUNKEN);
    }

    @Unique
    private void smallships$setSunken(boolean sunken) {
        this.smallships$self().getEntityData().set(SMALLSHIPS_SUNKEN, sunken);
    }

    @Unique
    private boolean smallships$isSimpleVanillaBoat() {
        return this.smallships$self().getClass().equals(Boat.class);
    }

    @Override
    public void smallships$setOwner(Player player) {
        this.smallships$ownerUuid = player.getUUID();
    }

    @Override
    public void smallships$applyItemDamage(ItemStack itemStack) {
        if (itemStack.isDamageableItem()) {
            this.smallships$damage = Mth.clamp(itemStack.getDamageValue(), 0, itemStack.getMaxDamage());
            this.smallships$setSunken(this.smallships$hasNoDurability());
        }
    }

    @Override
    public boolean smallships$handleAttackInteraction(Player player) {
        if (player.isCrouching()) {
            return this.smallships$tryPickup(player) != InteractionResult.PASS;
        }
        if (!this.smallships$self().level().isClientSide && this.smallships$isOwner(player) && !this.smallships$hasNoDurability()) {
            player.sendSystemMessage(OWNER_PICKUP_HINT_MESSAGE);
        }

        return false;
    }
}
