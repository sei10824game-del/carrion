package com.carrionmod;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

/** Web strand (0), acid spit (1) or bone spine (2). */
public class CarrionShot extends ThrowableProjectile {
    public static final int WEB = 0, SPIT = 1, SPINE = 2;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(CarrionShot.class, EntityDataSerializers.INT);
    private int life;

    public CarrionShot(EntityType<? extends CarrionShot> t, Level l) { super(t, l); }

    public CarrionShot(Level l, CarrionEntity owner, int kind) {
        super(CarrionMod.SHOT.get(), owner, l);
        setKind(kind);
    }

    @Override protected void defineSynchedData() { entityData.define(KIND, 0); }
    public int kind() { return entityData.get(KIND); }
    public void setKind(int k) { entityData.set(KIND, k); }
    @Override protected float getGravity() { return kind() == WEB ? 0.012f : kind() == SPIT ? 0.035f : 0.004f; }

    @Override protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof CarrionEntity) && !CarrionEntity.isThrall(e);
    }

    @Override public void tick() {
        super.tick();
        if (++life > 80) discard();
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            switch (kind()) {
                case WEB -> sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB)), getX(), getY(), getZ(), 2, .1, .1, .1, 0.01);
                case SPIT -> sl.sendParticles(new DustParticleOptions(new Vector3f(.55f, .03f, .06f), 1.2f), getX(), getY(), getZ(), 3, .08, .08, .08, 0);
                default -> sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    @Override protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide || !(getOwner() instanceof CarrionEntity c)) return;
        Entity hit = r.getEntity();
        if (!(hit instanceof LivingEntity v)) return;
        double d = c.dmg(1);
        switch (kind()) {
            case WEB -> { v.hurt(damageSources().mobProjectile(this, c), (float) (d * .25)); c.snare(v); }
            case SPIT -> {
                v.hurt(damageSources().mobProjectile(this, c), (float) (d * .55));
                v.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
            }
            default -> v.hurt(damageSources().mobProjectile(this, c), (float) (d * .4));
        }
        splash();
    }

    @Override protected void onHit(HitResult r) {
        super.onHit(r);
        if (!level().isClientSide) { if (r.getType() == HitResult.Type.BLOCK) splash(); discard(); }
    }

    private void splash() {
        if (!(level() instanceof ServerLevel sl)) return;
        if (kind() == WEB) sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB)), getX(), getY(), getZ(), 14, .3, .3, .3, 0.05);
        else sl.sendParticles(new DustParticleOptions(new Vector3f(.55f, .03f, .06f), 1.6f), getX(), getY(), getZ(), 18, .3, .3, .3, 0.05);
        sl.playSound(null, blockPosition(), kind() == SPIT ? SoundEvents.SLIME_SQUISH : SoundEvents.HONEY_BLOCK_HIT, SoundSource.HOSTILE, 1, .7f);
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("Kind", kind()); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); setKind(t.getInt("Kind")); }
}
