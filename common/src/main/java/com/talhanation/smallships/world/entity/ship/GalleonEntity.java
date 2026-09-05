package com.talhanation.smallships.world.entity.ship;

import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.mixin.controlling.BoatAccessor;
import com.talhanation.smallships.network.ModPackets;
import com.talhanation.smallships.world.entity.ModEntityTypes;
import com.talhanation.smallships.world.entity.ship.abilities.Cannonable;
import com.talhanation.smallships.world.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * First large, walkable ship. The vanilla entity box remains compact enough
 * for boat physics while these oriented local volumes describe the complete
 * hull and its moving deck.
 */
public class GalleonEntity extends BriggEntity {
    public static final String ID = "galleon";

    public static final double LENGTH = 34.0D;
    public static final double WIDTH = 8.0D;
    private static final double HALF_LENGTH = LENGTH / 2.0D;
    private static final double MAIN_DECK_HEIGHT = 3.05D;
    private static final double QUARTERDECK_HEIGHT = 4.25D;
    private static final double FORECASTLE_HEIGHT = 3.65D;
    private static final double HELM_LONGITUDINAL = -12.75D;
    private static final double HELM_VERTICAL = 5.70D;
    private static final double HELM_USE_REACH = 6.0D;
    private static final AABB HELM_INTERACTION_BOX = new AABB(
            HELM_LONGITUDINAL - 0.80D, HELM_VERTICAL - 1.55D, -1.55D,
            HELM_LONGITUDINAL + 0.80D, HELM_VERTICAL + 1.55D, 1.55D);
    private static final double DECK_EDGE_MARGIN = 0.28D;
    private static final double HULL_BOTTOM = -2.65D;
    private static final double DECK_CONTACT_HEIGHT = 0.24D;
    private static final double DECK_LANDING_DEPTH = 0.60D;
    private static final double DECK_JUMP_VELOCITY = 0.075D;
    private static final float MAX_RUDDER_ANGLE = 0.70F;

    private static final List<HullSegment> HULL_SEGMENTS = List.of(
            new HullSegment(-15.5D, 3.0D, 3.0D, 4.15D),
            new HullSegment(-12.0D, 4.0D, 5.8D, 4.15D),
            new HullSegment(-8.0D, 4.0D, 7.2D, 3.05D),
            new HullSegment(-4.0D, 4.0D, 8.0D, 2.90D),
            new HullSegment(0.0D, 4.0D, 8.0D, 2.90D),
            new HullSegment(4.0D, 4.0D, 8.0D, 2.90D),
            new HullSegment(8.0D, 4.0D, 7.2D, 2.95D),
            new HullSegment(12.0D, 4.0D, 5.8D, 3.55D),
            new HullSegment(15.5D, 3.0D, 3.0D, 3.55D)
    );

    private float previousRudderAngle;
    private float rudderAngle;
    private boolean acceptingHelmPassenger;
    private final Set<Integer> deckContacts = new HashSet<>();

    public GalleonEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    private GalleonEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.GALLEON, level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static GalleonEntity summon(Level level, double x, double y, double z) {
        return new GalleonEntity(level, x, y, z);
    }

    /**
     * The physical entity box stays compact for buoyancy, so the visible wheel
     * exposes its own oriented ray target.
     */
    public Optional<Vec3> clipHelmInteraction(Vec3 worldStart, Vec3 worldEnd) {
        Vec3 localStart = this.worldToLocal(worldStart);
        if (HELM_INTERACTION_BOX.contains(localStart)) {
            return Optional.of(worldStart);
        }

        Vec3 localEnd = this.worldToLocal(worldEnd);
        return HELM_INTERACTION_BOX.clip(localStart, localEnd)
                .map(localHit -> this.localToWorld(localHit.x, localHit.y, localHit.z));
    }

    private boolean isHelmInteractionPoint(Vec3 worldPosition) {
        return HELM_INTERACTION_BOX.contains(this.worldToLocal(worldPosition));
    }

    public boolean tryTakeHelm(Player player) {
        if (this.level().isClientSide
                || !this.isAlive()
                || this.isLocked()
                || player.isSpectator()
                || player.isSecondaryUseActive()
                || player.isPassenger()
                || !this.getPassengers().isEmpty()) {
            return false;
        }

        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewEnd = eyePosition.add(player.getViewVector(1.0F).scale(HELM_USE_REACH));
        if (eyePosition.distanceToSqr(this.localToWorld(HELM_LONGITUDINAL, HELM_VERTICAL, 0.0D))
                > HELM_USE_REACH * HELM_USE_REACH
                || this.clipHelmInteraction(eyePosition, viewEnd).isEmpty()) {
            return false;
        }

        this.acceptingHelmPassenger = true;
        try {
            return player.startRiding(this);
        } finally {
            this.acceptingHelmPassenger = false;
        }
    }

    @Override
    public boolean canAddPassenger(Entity entity) {
        return this.acceptingHelmPassenger && super.canAddPassenger(entity);
    }

    @Override
    public @NotNull InteractionResult interactAt(@NotNull Player player, @NotNull Vec3 hitPosition,
                                                  @NotNull InteractionHand hand) {
        Vec3 worldHit = hitPosition.add(this.position());
        if (!this.isHelmInteractionPoint(worldHit)
                || this.isLocked()
                || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide) {
            ModPackets.clientSendPacket(player, ModPackets.serverUseGalleonHelm.apply(this.getId()));
            return InteractionResult.SUCCESS;
        }
        return this.tryTakeHelm(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void tick() {
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();
        float oldYaw = this.getYRot();

        super.tick();
        if (this.isRemoved()) {
            return;
        }

        this.updateRudderAnimation();

        boolean horizontalTransformChanged = this.distanceToSqr(oldX, this.getY(), oldZ) > 1.0E-8D
                || Math.abs(Mth.wrapDegrees(this.getYRot() - oldYaw)) > 1.0E-3F;

        if (!this.level().isClientSide && horizontalTransformChanged && !this.isHullClear()) {
            this.stopAtHullCollision(oldX, oldZ, oldYaw);
        }

        this.updateMovingDeck(oldX, oldY, oldZ, oldYaw);
        if (!this.level().isClientSide) {
            this.pushEntitiesOutOfHull();
        }
    }

    private void stopAtHullCollision(double oldX, double oldZ, float oldYaw) {
        // Horizontal hull impacts must not cancel vanilla vertical buoyancy.
        this.setPos(oldX, this.getY(), oldZ);
        this.setYRot(oldYaw);
        this.setYHeadRot(oldYaw);
        this.setSpeed(0.0F);
        this.setRotSpeed(0.0F);
        ((BoatAccessor) this).setDeltaRotation(0.0F);
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        for (Entity passenger : this.getPassengers()) {
            this.positionRider(passenger);
        }
    }

    /** Used both by movement and by the placement item. */
    public boolean isHullClear() {
        for (HullSegment segment : HULL_SEGMENTS) {
            if (this.segmentIntersectsBlocks(segment)) {
                return false;
            }
        }
        return true;
    }

    private boolean segmentIntersectsBlocks(HullSegment segment) {
        AABB broadPhase = this.getSegmentBounds(segment);
        BlockPos min = BlockPos.containing(broadPhase.minX, broadPhase.minY, broadPhase.minZ);
        BlockPos max = BlockPos.containing(broadPhase.maxX, broadPhase.maxY, broadPhase.maxZ);

        for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
            VoxelShape collisionShape = this.level().getBlockState(blockPos)
                    .getCollisionShape(this.level(), blockPos, CollisionContext.of(this));
            if (collisionShape.isEmpty()) {
                continue;
            }
            for (AABB localBox : collisionShape.toAabbs()) {
                AABB worldBox = localBox.move(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                if (this.segmentIntersectsBox(segment, worldBox)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Separating-axis test between one oriented hull segment and a block box. */
    private boolean segmentIntersectsBox(HullSegment segment, AABB box) {
        double hullMinY = this.getY() + HULL_BOTTOM;
        double hullMaxY = this.getY() + segment.top;
        if (box.maxY <= hullMinY || box.minY >= hullMaxY) {
            return false;
        }

        Vec3 segmentCentre = this.localToWorld(segment.longitudinal, 0.0D, 0.0D);
        Vec3 longitudinalAxis = rotateLocal(new Vec3(1.0D, 0.0D, 0.0D), this.getYRot());
        Vec3 lateralAxis = rotateLocal(new Vec3(0.0D, 0.0D, 1.0D), this.getYRot());
        double blockCentreX = (box.minX + box.maxX) * 0.5D;
        double blockCentreZ = (box.minZ + box.maxZ) * 0.5D;
        double deltaX = blockCentreX - segmentCentre.x;
        double deltaZ = blockCentreZ - segmentCentre.z;
        double blockHalfX = (box.maxX - box.minX) * 0.5D;
        double blockHalfZ = (box.maxZ - box.minZ) * 0.5D;
        double segmentHalfLength = segment.length * 0.5D;
        double segmentHalfWidth = segment.width * 0.5D;

        double longitudinalDistance = Math.abs(deltaX * longitudinalAxis.x + deltaZ * longitudinalAxis.z);
        double blockOnLongitudinal = blockHalfX * Math.abs(longitudinalAxis.x)
                + blockHalfZ * Math.abs(longitudinalAxis.z);
        if (longitudinalDistance >= segmentHalfLength + blockOnLongitudinal) {
            return false;
        }

        double lateralDistance = Math.abs(deltaX * lateralAxis.x + deltaZ * lateralAxis.z);
        double blockOnLateral = blockHalfX * Math.abs(lateralAxis.x)
                + blockHalfZ * Math.abs(lateralAxis.z);
        if (lateralDistance >= segmentHalfWidth + blockOnLateral) {
            return false;
        }

        double segmentOnWorldX = segmentHalfLength * Math.abs(longitudinalAxis.x)
                + segmentHalfWidth * Math.abs(lateralAxis.x);
        if (Math.abs(deltaX) >= blockHalfX + segmentOnWorldX) {
            return false;
        }

        double segmentOnWorldZ = segmentHalfLength * Math.abs(longitudinalAxis.z)
                + segmentHalfWidth * Math.abs(lateralAxis.z);
        return Math.abs(deltaZ) < blockHalfZ + segmentOnWorldZ;
    }

    private void updateMovingDeck(double oldX, double oldY, double oldZ, float oldYaw) {
        AABB searchBounds = this.getHullBounds().inflate(1.25D, 2.5D, 1.25D);
        List<Entity> nearbyEntities = this.level().getEntities(this, searchBounds, entity ->
                entity.isAlive()
                        && !entity.isPassenger()
                        && !(entity instanceof Ship)
                        && (!(entity instanceof Player player) || !player.isSpectator()));

        Set<Integer> nextDeckContacts = new HashSet<>();

        for (Entity entity : nearbyEntities) {
            // The server owns every entity. On the client, only locally
            // controlled entities are moved to avoid fighting network updates.
            if (this.level().isClientSide
                    && (!(entity instanceof Player player) || !player.isLocalPlayer())) {
                continue;
            }
            if (entity.getVehicle() != null) {
                continue;
            }

            Vec3 movement = entity.getDeltaMovement();
            Vec3 oldLocal = worldToLocal(entity.position(), oldX, oldY, oldZ, oldYaw);
            double oldDeckHeight = getDeckHeight(oldLocal.x);
            boolean jumping = movement.y > DECK_JUMP_VELOCITY;
            if (jumping) {
                continue;
            }

            boolean insideOldDeck = isInsideDeck(oldLocal.x, oldLocal.z, DECK_EDGE_MARGIN);
            boolean retainedContact = this.deckContacts.contains(entity.getId());
            boolean nearOldDeck = oldLocal.y >= oldDeckHeight - 0.26D
                    && oldLocal.y <= oldDeckHeight + DECK_CONTACT_HEIGHT;

            if (insideOldDeck && (retainedContact || nearOldDeck)) {
                double feetOffset = Mth.clamp(oldLocal.y - oldDeckHeight, 0.0D, DECK_CONTACT_HEIGHT);
                Vec3 carriedPosition = localToWorld(
                        oldLocal.x,
                        getDeckHeight(oldLocal.x) + feetOffset,
                        oldLocal.z);
                this.supportOnDeck(entity, carriedPosition);
                nextDeckContacts.add(entity.getId());
                continue;
            }

            // A falling entity is caught once its feet cross the deck plane.
            // The velocity-dependent depth avoids tunnelling during a fast fall.
            Vec3 currentLocal = this.worldToLocal(entity.position());
            double currentDeckHeight = getDeckHeight(currentLocal.x);
            double landingDepth = Math.min(1.50D,
                    Math.max(DECK_LANDING_DEPTH, -movement.y + 0.20D));
            double penetration = currentDeckHeight - currentLocal.y;
            if (movement.y <= 0.0D
                    && penetration >= 0.0D
                    && penetration <= landingDepth
                    && isInsideDeck(currentLocal.x, currentLocal.z, DECK_EDGE_MARGIN)) {
                Vec3 deckPosition = this.localToWorld(currentLocal.x, currentDeckHeight, currentLocal.z);
                this.supportOnDeck(entity, deckPosition);
                nextDeckContacts.add(entity.getId());
            }
        }

        this.deckContacts.clear();
        this.deckContacts.addAll(nextDeckContacts);
    }

    private void supportOnDeck(Entity entity, Vec3 deckPosition) {
        entity.setPos(deckPosition.x, deckPosition.y, deckPosition.z);
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < 0.0D) {
            entity.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
    }

    private void pushEntitiesOutOfHull() {
        AABB searchBounds = this.getHullBounds().inflate(0.25D);
        List<Entity> nearbyEntities = this.level().getEntities(this, searchBounds, entity ->
                entity.isAlive()
                        && !entity.isPassenger()
                        && !(entity instanceof Ship)
                        && (!(entity instanceof Player player) || !player.isSpectator()));

        for (Entity entity : nearbyEntities) {
            Vec3 local = this.worldToLocal(entity.position());
            // Leave the landing band below the deck to updateMovingDeck. Hull
            // expulsion is only for entities genuinely trapped in the hold.
            if (local.y < HULL_BOTTOM || local.y >= getDeckHeight(local.x) - 0.70D) {
                continue;
            }

            double halfWidth = getDeckHalfWidth(local.x);
            if (Math.abs(local.x) >= HALF_LENGTH || Math.abs(local.z) >= halfWidth) {
                continue;
            }

            double lateralPenetration = halfWidth - Math.abs(local.z);
            double longitudinalPenetration = HALF_LENGTH - Math.abs(local.x);
            Vec3 localPush;
            if (lateralPenetration <= longitudinalPenetration) {
                localPush = new Vec3(0.0D, 0.0D, local.z < 0.0D ? -0.35D : 0.35D);
            } else {
                localPush = new Vec3(local.x < 0.0D ? -0.35D : 0.35D, 0.0D, 0.0D);
            }

            Vec3 worldPush = rotateLocal(localPush, this.getYRot());
            entity.push(worldPush.x, 0.0D, worldPush.z);
        }
    }

    public boolean isInsideDeck(double longitudinal, double lateral, double margin) {
        return Math.abs(longitudinal) <= HALF_LENGTH - margin
                && Math.abs(lateral) <= getDeckHalfWidth(longitudinal) - margin;
    }

    private static double getDeckHalfWidth(double longitudinal) {
        double distanceFromCentre = Math.abs(longitudinal);
        if (distanceFromCentre <= 11.5D) {
            return WIDTH / 2.0D;
        }
        double taper = Mth.clamp((distanceFromCentre - 11.5D) / (HALF_LENGTH - 11.5D), 0.0D, 1.0D);
        return Mth.lerp(taper, WIDTH / 2.0D, 1.0D);
    }

    /** Raised stern and bow decks create the high galleon silhouette above a closed hold. */
    private static double getDeckHeight(double longitudinal) {
        if (longitudinal <= -12.0D) {
            return QUARTERDECK_HEIGHT;
        }
        if (longitudinal < -9.0D) {
            double progress = (-longitudinal - 9.0D) / 3.0D;
            return Mth.lerp(progress, MAIN_DECK_HEIGHT, QUARTERDECK_HEIGHT);
        }
        if (longitudinal >= 13.0D) {
            return FORECASTLE_HEIGHT;
        }
        if (longitudinal > 10.5D) {
            double progress = (longitudinal - 10.5D) / 2.5D;
            return Mth.lerp(progress, MAIN_DECK_HEIGHT, FORECASTLE_HEIGHT);
        }
        return MAIN_DECK_HEIGHT;
    }

    public Vec3 localToWorld(double longitudinal, double vertical, double lateral) {
        return localToWorld(longitudinal, vertical, lateral, this.getX(), this.getY(), this.getZ(), this.getYRot());
    }

    public Vec3 worldToLocal(Vec3 worldPosition) {
        return worldToLocal(worldPosition, this.getX(), this.getY(), this.getZ(), this.getYRot());
    }

    private static Vec3 localToWorld(double longitudinal, double vertical, double lateral,
                                     double shipX, double shipY, double shipZ, float shipYaw) {
        Vec3 rotated = rotateLocal(new Vec3(longitudinal, vertical, lateral), shipYaw);
        return new Vec3(shipX + rotated.x, shipY + rotated.y, shipZ + rotated.z);
    }

    private static Vec3 worldToLocal(Vec3 worldPosition, double shipX, double shipY, double shipZ, float shipYaw) {
        Vec3 offset = worldPosition.subtract(shipX, shipY, shipZ);
        return offset.yRot(shipYaw * Mth.DEG_TO_RAD + ((float) Math.PI / 2.0F));
    }

    private static Vec3 rotateLocal(Vec3 localPosition, float shipYaw) {
        return localPosition.yRot(-shipYaw * Mth.DEG_TO_RAD - ((float) Math.PI / 2.0F));
    }

    private AABB getSegmentBounds(HullSegment segment) {
        double halfLongitudinal = segment.length / 2.0D;
        double halfLateral = segment.width / 2.0D;
        Vec3[] corners = new Vec3[]{
                this.localToWorld(segment.longitudinal - halfLongitudinal, 0.0D, -halfLateral),
                this.localToWorld(segment.longitudinal - halfLongitudinal, 0.0D, halfLateral),
                this.localToWorld(segment.longitudinal + halfLongitudinal, 0.0D, -halfLateral),
                this.localToWorld(segment.longitudinal + halfLongitudinal, 0.0D, halfLateral)
        };
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            minX = Math.min(minX, corner.x);
            maxX = Math.max(maxX, corner.x);
            minZ = Math.min(minZ, corner.z);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, this.getY() + HULL_BOTTOM, minZ, maxX, this.getY() + segment.top, maxZ);
    }

    public AABB getHullBounds() {
        Vec3[] corners = new Vec3[]{
                this.localToWorld(-HALF_LENGTH, 0.0D, -WIDTH / 2.0D),
                this.localToWorld(-HALF_LENGTH, 0.0D, WIDTH / 2.0D),
                this.localToWorld(HALF_LENGTH, 0.0D, -WIDTH / 2.0D),
                this.localToWorld(HALF_LENGTH, 0.0D, WIDTH / 2.0D)
        };
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            minX = Math.min(minX, corner.x);
            maxX = Math.max(maxX, corner.x);
            minZ = Math.min(minZ, corner.z);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, this.getY() + HULL_BOTTOM, minZ, maxX, this.getY() + QUARTERDECK_HEIGHT, maxZ);
    }

    @Override
    protected AABB getNavigationBounds() {
        return this.getHullBounds();
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return this.getHullBounds().inflate(4.0D, 18.0D, 4.0D);
    }

    @Override
    public CompoundTag createDefaultAttributes() {
        Attributes attributes = new Attributes();
        attributes.maxHealth = SmallShipsConfig.Common.shipAttributeBriggMaxHealth.get().floatValue() * 3.0F;
        attributes.maxSpeed = SmallShipsConfig.Common.shipAttributeBriggMaxSpeed.get().floatValue() * 0.62F;
        attributes.maxReverseSpeed = SmallShipsConfig.Common.shipAttributeBriggMaxReverseSpeed.get().floatValue();
        attributes.maxRotationSpeed = SmallShipsConfig.Common.shipAttributeBriggMaxRotationSpeed.get().floatValue() * 0.32F;
        attributes.acceleration = SmallShipsConfig.Common.shipAttributeBriggAcceleration.get().floatValue() * 0.45F;
        attributes.rotationAcceleration = SmallShipsConfig.Common.shipAttributeBriggRotationAcceleration.get().floatValue() * 0.30F;
        CompoundTag tag = new CompoundTag();
        attributes.addSaveData(tag);
        return tag;
    }

    @Override
    public int getMaxPassengers() {
        return 1;
    }

    @Override
    public void positionRider(@NotNull Entity entity) {
        if (!this.hasPassenger(entity)) {
            return;
        }
        Vec3 helmPosition = this.localToWorld(HELM_LONGITUDINAL,
                getDeckHeight(HELM_LONGITUDINAL) + entity.getMyRidingOffset(), 0.0D);
        entity.setPos(helmPosition.x, helmPosition.y, helmPosition.z);
        entity.setYRot(entity.getYRot() + ((BoatAccessor) this).getDeltaRotation());
        entity.setYHeadRot(entity.getYHeadRot() + ((BoatAccessor) this).getDeltaRotation());
        this.clampRotation(entity);
    }

    @Override
    public double getPassengersRidingOffset() {
        return getDeckHeight(HELM_LONGITUDINAL);
    }

    @Override
    public Cannonable.CannonPosition getCannonPosition(int index) {
        if (index < 0 || index >= this.getMaxCannonPerSide() * 2) {
            throw new IndexOutOfBoundsException("Invalid galleon cannon index: " + index);
        }
        int pairIndex = index / 2;
        boolean rightSided = index % 2 == 0;
        double longitudinal = -12.0D + pairIndex * (24.0D / 7.0D);
        return new Cannonable.CannonPosition(3.25D, 1.65D, longitudinal, rightSided);
    }

    @Override
    public byte getMaxCannonPerSide() {
        return 8;
    }

    @Override
    public float getDefaultCannonPower() {
        return 5.0F;
    }

    @Override
    public @NotNull Item getDropItem() {
        if (!SmallShipsConfig.Common.shipGeneralDoItemDrop.get()) {
            return ItemStack.EMPTY.getItem();
        }
        return ModItems.GALLEON_ITEMS.get(this.getVariant());
    }

    @Override
    public BiomeModifierType getBiomeModifierType() {
        return BiomeModifierType.NEUTRAL;
    }

    private void updateRudderAnimation() {
        this.previousRudderAngle = this.rudderAngle;
        float targetAngle = this.getDriver() == null
                ? 0.0F
                : Mth.clamp(-this.getRotSpeed() * 0.70F, -MAX_RUDDER_ANGLE, MAX_RUDDER_ANGLE);
        this.rudderAngle += (targetAngle - this.rudderAngle) * 0.22F;
        if (Math.abs(targetAngle - this.rudderAngle) < 1.0E-3F) {
            this.rudderAngle = targetAngle;
        }
    }

    public float getRudderAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.previousRudderAngle, this.rudderAngle);
    }

    private record HullSegment(double longitudinal, double length, double width, double top) {
    }
}
