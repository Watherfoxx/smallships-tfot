package com.talhanation.smallships.mixin.render;

import com.talhanation.smallships.world.entity.ship.GalleonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "pick", at = @At("TAIL"))
    private void smallships$pickGalleonHelm(float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity viewer = minecraft.getCameraEntity();
        if (viewer == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        double reach = minecraft.gameMode.getPickRange();
        Vec3 start = viewer.getEyePosition(partialTicks);
        Vec3 end = start.add(viewer.getViewVector(partialTicks).scale(reach));
        double closestDistance = reach * reach;

        HitResult vanillaHit = minecraft.hitResult;
        if (vanillaHit != null && vanillaHit.getType() != HitResult.Type.MISS) {
            closestDistance = Math.min(closestDistance, start.distanceToSqr(vanillaHit.getLocation()));
        }

        AABB searchBounds = new AABB(
                Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z),
                Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z))
                .inflate(18.0D);

        GalleonEntity closestGalleon = null;
        Vec3 closestHit = null;
        for (GalleonEntity galleon : minecraft.level.getEntitiesOfClass(
                GalleonEntity.class, searchBounds, entity -> entity.isAlive() && !entity.isLocked())) {
            Optional<Vec3> hit = galleon.clipHelmInteraction(start, end);
            if (hit.isEmpty()) {
                continue;
            }

            double hitDistance = start.distanceToSqr(hit.get());
            if (hitDistance <= closestDistance) {
                closestDistance = hitDistance;
                closestGalleon = galleon;
                closestHit = hit.get();
            }
        }

        if (closestGalleon != null) {
            EntityHitResult helmHit = new EntityHitResult(closestGalleon, closestHit);
            minecraft.hitResult = helmHit;
            minecraft.crosshairPickEntity = closestGalleon;
        }
    }
}
