package com.talhanation.smallships.world.entity.projectile;


import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.entity.ship.GhostCrewEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import com.talhanation.smallships.world.sound.ModSoundTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractCannonBall extends AbstractHurtingProjectile {
    public boolean inWater = false;
    public boolean wasShot = false;
    public int counter = 0;
    private boolean ghostRestrictedDamage;
    private final Set<UUID> ghostHitEntities = new HashSet<>();

    @Override
    public void setOwner(Entity owner) {
        super.setOwner(owner);
        // Remember the origin even if the captain dies before impact.
        if (owner instanceof GhostCrewEntity
                || owner instanceof Ship ship && ship.isAiControlled()
                || owner != null && owner.getTags().contains("smallships_pirate_captain")) {
            ghostRestrictedDamage = true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GhostRestrictedDamage", ghostRestrictedDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ghostRestrictedDamage = tag.getBoolean("GhostRestrictedDamage");
    }

    private boolean isGhostShot() {
        // Also recognize pre-existing projectiles whose owner is still available.
        if (!ghostRestrictedDamage) {
            Entity owner = getOwner();
            if (owner != null) setOwner(owner);
        }
        return ghostRestrictedDamage;
    }

    protected AbstractCannonBall(EntityType<? extends AbstractCannonBall> type, Level world) {
        super(type, world);
    }

    public AbstractCannonBall(EntityType<? extends AbstractCannonBall> type, LivingEntity owner, double d1, double d2, double d3, Level world) {
        super(type, owner, d1, d2, d3, world);
        this.moveTo(d1, d2, d3, this.getYRot(), this.getXRot());
    }

    public AbstractCannonBall(EntityType<? extends AbstractCannonBall> type, Entity owner, double d1, double d2, double d3, Level world) {
        super(type, world);
        this.setOwner(owner);
        this.moveTo(d1, d2, d3, this.getYRot(), this.getXRot());
    }

    @Override
    public void tick() {
        this.baseTick();

        Vec3 vector3d = this.getDeltaMovement();
        boolean ghostShot = isGhostShot();
        // A hull can overlap its passengers. Continue along the same segment after
        // hitting it, or fast cannonballs can skip the player hidden by that first hit.
        for (int impacts = 0; impacts < (ghostShot ? 16 : 1); impacts++) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() == HitResult.Type.MISS) break;
            if (ghostShot && hit instanceof EntityHitResult entityHit) {
                ghostHitEntities.add(entityHit.getEntity().getUUID());
            }
            this.onHit(hit);
            if (this.isRemoved() || !(hit instanceof EntityHitResult)) break;
        }

        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;
        this.updateRotation();
        float f = 0.99F;
        float f1 = 0.06F;
        float f2 = -0.05F;
        this.setDeltaMovement(vector3d.scale(f));
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -f1, 0.0D));
        }
        this.setPos(d0, d1, d2);

        if(isAlive()){
            this.setWasShot(true);
        }

        if(isInWater()){
            if (this.level().isClientSide() && !isUnderWater()) waterParticles();

            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -f2, 0.0D));
            this.setInWater(true);
        }

        if (wasShot){
            counter++;
        }

        if (counter < 4){
            if (this.level().isClientSide()) tailParticles();
        }

        if (isInWater() && counter > 200){
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // Filter before ray tracing: an immune mob must not hide a player behind it.
        if (isGhostShot() && (!(entity instanceof Player) && !(entity instanceof Boat)
                || ghostHitEntities.contains(entity.getUUID()))) return false;
        return super.canHitEntity(entity);
    }

    public void setWasShot(boolean bool){
        if (bool != wasShot){
            wasShot = true;
            if (this.level().isClientSide()) {
                this.shootParticles();
            }
        }
    }

    public void setInWater(boolean bool){
        if (bool != inWater){
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 3.3F, 0.8F + 0.4F * this.random.nextFloat());
            inWater = true;
            //this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (isGhostShot()) {
            // No explosion or block callback: neither collateral damage nor triggered TNT.
            if (!this.level().isClientSide()) this.discard();
            return;
        }
        super.onHitBlock(blockHitResult);
        if (!this.level().isClientSide()) {
            boolean doesSpreadFire = false;

            if(!isInWater()) this.level().explode(this.getOwner(), getX(), getY(), getZ(), SmallShipsConfig.Common.shipGeneralCannonDestruction.get().floatValue(), doesSpreadFire, Level.ExplosionInteraction.MOB);
            this.remove(RemovalReason.KILLED);
        }
    }


    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        this.hitParticles();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (isGhostShot() && !(hitResult.getEntity() instanceof Player)
                && !(hitResult.getEntity() instanceof Boat)) return;
        super.onHitEntity(hitResult);
        if (!this.level().isClientSide()) {
            Entity hitEntity = hitResult.getEntity();
            Entity ownerEntity = this.getOwner();

            if (isGhostShot() && hitEntity instanceof Player player) {
                float damage = SmallShipsConfig.Common.shipGeneralCannonDamage.get().floatValue();
                boolean applied = player.hurt(this.damageSources().thrown(this, ownerEntity), damage);
                if (applied) {
                    this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE,
                            getSoundSource(), 3.3F, 0.8F + 0.4F * random.nextFloat());
                } else {
                    com.talhanation.smallships.SmallShipsMod.LOGGER.info(
                            "Ghost cannon hit player {} but damage was rejected (damage={}, difficulty={}, invulnerable={})",
                            player.getScoreboardName(), damage, level().getDifficulty(), player.getAbilities().invulnerable);
                }
                return;
            }

            // Ne pas infliger de dégâts aux armor stands et item frames
            if (hitEntity instanceof ArmorStand || hitEntity instanceof ItemFrame) {
                return;
            }

            if (hitEntity instanceof Ship shipHitEntity) {
                shipHitEntity.hurt(this.damageSources().thrown(this, ownerEntity), random.nextInt(7) + 7);
                this.level().playSound(null, this.getX(), this.getY() + 4 , this.getZ(), ModSoundTypes.SHIP_HIT, this.getSoundSource(), 3.3F, 0.8F + 0.4F * this.random.nextFloat());
            }
            else if (ownerEntity instanceof LivingEntity livingOwnerEntity) {
                if(ownerEntity.getTeam() != null && ownerEntity.getTeam().isAlliedTo(hitEntity.getTeam()) && !ownerEntity.getTeam().isAllowFriendlyFire()) return;
                if (!isGhostShot()) this.doEnchantDamageEffects(livingOwnerEntity, hitEntity);
                this.level().playSound(null, this.getX(), this.getY() + 4 , this.getZ(), SoundEvents.GENERIC_EXPLODE, this.getSoundSource(), 3.3F, 0.8F + 0.4F * this.random.nextFloat());
            }

            hitEntity.hurt(this.damageSources().thrown(this, ownerEntity), SmallShipsConfig.Common.shipGeneralCannonDamage.get().floatValue());
        }
    }

    public void hitParticles(){
        for (int i = 0; i < 150; ++i) {
            double d0 = this.random.nextGaussian() * 0.03D;
            double d1 = this.random.nextGaussian() * 0.03D;
            double d2 = this.random.nextGaussian() * 0.03D;
            double d3 = 20.0D;
            this.level().addParticle(ParticleTypes.POOF, this.getX(1.0D) - d0 * d3, this.getRandomY() - d1 * d3, this.getRandomZ(2.0D) - d2 * d3, d0, d1, d2);
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(1.0D) - d0 * d3, this.getRandomY() - d1 * d3, this.getRandomZ(2.0D) - d2 * d3, d0, d1, d2);
        }
    }

    public void waterParticles(){
        for (int i = 0; i < 100; ++i) {
            double d0 = this.random.nextGaussian() * 0.03D;
            double d1 = this.random.nextGaussian() * 0.03D;
            double d2 = this.random.nextGaussian() * 0.03D;
            double d3 = 20.0D;
            this.level().addParticle(ParticleTypes.POOF, this.getX(1.0D) - d0 * d3, this.getRandomY() - d1 * d3  + i * 0.012, this.getRandomZ(2.0D) - d2 * d3, d0, d1, d2);
        }
    }


    public void shootParticles(){
        for (int i = 0; i < 50; ++i) {
            double d0 = this.random.nextGaussian() * 0.03D;
            double d1 = this.random.nextGaussian() * 0.03D;
            double d2 = this.random.nextGaussian() * 0.03D;
            double d3 = 20.0D;
            this.level().addParticle(ParticleTypes.POOF, this.getX(1.0D) - d0 * d3, this.getRandomY() - d1 * d3, this.getRandomZ(2.0D) - d2 * d3, d0, d1, d2);
        }

        for (int i = 0; i < 25; ++i) {
            double d00 = this.random.nextGaussian() * 0.03D;
            double d11 = this.random.nextGaussian() * 0.03D;
            double d22 = this.random.nextGaussian() * 0.03D;
            double d44 = 10.0D;
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(1.0D) - d00 * d44, this.getRandomY() - d11 * d44, this.getRandomZ(2.0D) - d22 * d44, d00, d11, d22);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(1.0D) - d00 * d44, this.getRandomY() - d11 * d44, this.getRandomZ(2.0D) - d22 * d44, 0, 0, 0);
        }
    }

    public void tailParticles(){
        for (int i = 0; i < 50; ++i) {
            this.level().addParticle(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ() , 0, 0, 0);
        }

        for (int i = 0; i < 25; ++i) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        return false;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        Entity entity = this.getOwner();
        int i = entity == null ? 0 : entity.getId();
        return new ClientboundAddEntityPacket(this.getId(), this.getUUID(), this.getX(), this.getY(), this.getZ(), this.getXRot(), this.getYRot(), this.getType(), i, new Vec3(this.xPower, this.yPower, this.zPower), 0.0);

    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected @NotNull ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }

}
