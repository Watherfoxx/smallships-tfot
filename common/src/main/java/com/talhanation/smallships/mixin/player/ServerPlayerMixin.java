package com.talhanation.smallships.mixin.player;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void readAdditionalSaveDataRemoveShipRootVehicle(CompoundTag tag, CallbackInfo ci) {
        removeShipRootVehicle(tag);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addAdditionalSaveDataRemoveShipRootVehicle(CompoundTag tag, CallbackInfo ci) {
        if (((ServerPlayer) (Object) this).getVehicle() instanceof Ship) {
            tag.remove("RootVehicle");
        }
    }

    private static void removeShipRootVehicle(CompoundTag tag) {
        if (!tag.contains("RootVehicle", Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag rootVehicle = tag.getCompound("RootVehicle");
        if (!rootVehicle.contains("Entity", Tag.TAG_COMPOUND)) {
            return;
        }

        String entityId = rootVehicle.getCompound("Entity").getString("id");
        if (entityId.startsWith(SmallShipsMod.MOD_ID + ":")) {
            tag.remove("RootVehicle");
        }
    }
}
