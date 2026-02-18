package com.talhanation.smallships.world.entity.ship;

import com.talhanation.smallships.world.entity.ModEntityTypes;
import com.talhanation.smallships.world.entity.ship.abilities.Leashable;
import com.talhanation.smallships.world.entity.ship.abilities.Paddleable;
import com.talhanation.smallships.world.entity.ship.abilities.Repairable;
import com.talhanation.smallships.world.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RowBoatEntity extends Ship implements Repairable, Leashable, Paddleable {
    public static final String ID = "rowboat";

    public RowBoatEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    private RowBoatEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.ROWBOAT, level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static RowBoatEntity summon(Level level, double x, double y, double z) {
        return new RowBoatEntity(level, x, y, z);
    }

    @Override
    public int getMaxPassengers() {
        return 2;
    }

    @Override
    public @NotNull Item getDropItem() {
        return ModItems.ROWBOAT_ITEMS.get(this.getVariant());
    }

    @Override
    public BiomeModifierType getBiomeModifierType() {
        return BiomeModifierType.NONE;
    }

    @Override
    public CompoundTag createDefaultAttributes() {
        Attributes attributes = new Attributes();
        attributes.maxHealth = 125.0F;
        attributes.maxSpeed = 27.0F;
        attributes.maxReverseSpeed = 0.12F;
        attributes.maxRotationSpeed = 5.4F;
        attributes.acceleration = 0.02F;
        attributes.rotationAcceleration = 1.05F;

        CompoundTag tag = new CompoundTag();
        attributes.addSaveData(tag);
        return tag;
    }

    @Override
    protected float getSinglePassengerXOffset() {
        return -0.75F;
    }

    @Override
    protected float getSinglePassengerZOffset() {
        return 0.0F;
    }

    @Override
    public @Nullable Vec3 applyLeashOffset() {
        return new Vec3(0.0, this.getEyeHeight(), this.getBbWidth() * 0.1F);
    }
}
