package com.talhanation.smallships.mixin.controlling;

import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class GhostCaptainDeathMixin {
    @Shadow public boolean dead;
    @Unique private Ship smallships$captainedShip;

    @Inject(method = "die", at = @At("HEAD"))
    private void smallships$captureShip(DamageSource source, CallbackInfo ci) {
        LivingEntity captain = (LivingEntity) (Object) this;
        smallships$captainedShip = !captain.level().isClientSide
                && captain.getTags().contains("smallships_pirate_captain")
                && captain.getVehicle() instanceof Ship ship && ship.isAiControlled()
                ? ship : null;
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void smallships$sinkShip(DamageSource source, CallbackInfo ci) {
        // Capture before death can dismount the captain; only sink after confirmed death.
        if (dead && smallships$captainedShip != null && !smallships$captainedShip.isRemoved()) {
            smallships$captainedShip.setSunken(true);
        }
        smallships$captainedShip = null;
    }
}
