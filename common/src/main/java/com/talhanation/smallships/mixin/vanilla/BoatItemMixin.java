package com.talhanation.smallships.mixin.vanilla;

import com.talhanation.smallships.duck.VanillaBoatAccess;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BoatItem.class)
public class BoatItemMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean smallships$addFreshEntityWithOwner(Level level, Entity entity, Level methodLevel, Player player, InteractionHand interactionHand) {
        if (entity.getClass().equals(Boat.class) && entity instanceof VanillaBoatAccess vanillaBoat && !(entity instanceof Ship)) {
            ItemStack itemStack = player.getItemInHand(interactionHand);
            vanillaBoat.smallships$setOwner(player);
            vanillaBoat.smallships$applyItemDamage(itemStack);
        }

        return level.addFreshEntity(entity);
    }
}
