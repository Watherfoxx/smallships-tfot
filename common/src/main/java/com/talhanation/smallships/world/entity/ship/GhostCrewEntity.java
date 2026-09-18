package com.talhanation.smallships.world.entity.ship;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Dedicated type lets clients render crew as player-shaped ghosts without changing vanilla pillagers. */
public class GhostCrewEntity extends Pillager {
    public static final String ID = "ghost_crew";

    public GhostCrewEntity(EntityType<? extends Pillager> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }
}
