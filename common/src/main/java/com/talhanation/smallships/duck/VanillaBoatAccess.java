package com.talhanation.smallships.duck;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface VanillaBoatAccess {
    void smallships$setOwner(Player player);
    void smallships$applyItemDamage(ItemStack itemStack);
    boolean smallships$handleAttackInteraction(Player player);
}
