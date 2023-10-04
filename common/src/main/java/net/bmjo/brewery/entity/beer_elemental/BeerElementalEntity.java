package net.bmjo.brewery.entity.beer_elemental;

import net.bmjo.brewery.sound.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

/*
 * IMPORTANT: Moved to extending Monster rather than Blaze due to being unable to avoid the creation of smoke particles
 * while calling super.aiStep() which is a necessity. Not ideal, but the alternative would be very messy mixins/coremods.
 */
public class BeerElementalEntity extends Monster {

    private float allowedHeightOffset = 0.5f;
    private int nextHeightOffsetChangeTick;

    public BeerElementalEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    @Override
    public void aiStep() {
        if (!onGround && getDeltaMovement().y < 0.0D)
            setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));

        if (level.isClientSide) {
            if (random.nextInt(24) == 0 && !isSilent())
                level.playLocalSound(getX(), getY(), getZ(), SoundEvents.BLAZE_BURN, getSoundSource(), 1.0F + random.nextFloat(), 0.3F + random.nextFloat() * 0.7F, false);

            for(int i = 0; i < 2; i++)
                level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, getRandomX(0.8D), getY() + random.nextDouble() * 2.0D, getRandomZ(0.8D), 0.0D, 0.0D, 0.0D);
        }

        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        nextHeightOffsetChangeTick--;
        if (nextHeightOffsetChangeTick <= 0) {
            nextHeightOffsetChangeTick = 100;
            allowedHeightOffset = (float)random.triangle(0.5D, 6.891D);
        }

        LivingEntity target = getTarget();
        if (target != null) {
            if(target.getEyeY() > getEyeY() + (double)this.allowedHeightOffset) {
                if(canAttack(target)) {
                    Vec3 velocity = getDeltaMovement();
                    velocity = velocity.add(0.0D, (0.3D - velocity.y) * 0.3D, 0.0D);
                    setDeltaMovement(velocity);
                    hasImpulse = true;
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundRegistry.BEER_ELEMENTAL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundRegistry.BEER_ELEMENTAL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.BEER_ELEMENTAL_DEATH.get();
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    public boolean causeFallDamage(float f, float g, DamageSource damageSource) {
        return false; // Do not take fall damage
    }


}
