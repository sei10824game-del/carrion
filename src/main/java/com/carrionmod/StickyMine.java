package com.carrionmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Proximity mine: sticks to Carrion when it passes close, then detonates after 5 seconds unless shed or blocked by a carapace. */
public class StickyMine extends Entity {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(StickyMine.class, EntityDataSerializers.INT);
    private CarrionEntity host;
    private int hostId = -1;

    public StickyMine(EntityType<? extends StickyMine> t, Level l) { super(t, l); }

    @Override protected void defineSynchedData() { entityData.define(FUSE, -1); }
    public int fuse() { return entityData.get(FUSE); }
    public boolean armedOnly() { return fuse() < 0; }
    @Override public boolean isPickable() { return true; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (host == null) {
            if (tickCount % 4 == 0)
                for (CarrionEntity c : level().getEntitiesOfClass(CarrionEntity.class, getBoundingBox().inflate(2.5 * 1))) { // detection light
                    host = c; entityData.set(FUSE, 100); c.bombs++;
                    playSound(SoundEvents.SLIME_ATTACK, 1f, 2f);
                    break;
                }
            return;
        }
        if (!host.isAlive() || host.isRemoved()) { discard(); return; }
        setPos(host.getX(), host.getY() + host.getBbHeight() * .9, host.getZ());
        int f = fuse() - 1; entityData.set(FUSE, f);
        if (f % (f > 40 ? 20 : 8) == 0) level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BIT.get(), SoundSource.HOSTILE, 1f, 2f);
        if (host.getAtk() == CarrionEntity.BURST || host.getAtk() == CarrionEntity.ROAR) { // shockwave sheds it
            host.bombs--; ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 12, .3, .3, .3, .05); discard(); return;
        }
        if (f <= 0) detonate();
    }

    private void detonate() {
        ServerLevel l = (ServerLevel) level();
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        l.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f, .8f);
        host.bombs--;
        if (host.isHardened()) host.drainEnergy(); // blocked, but the carapace's energy is spent
        else host.hurt(damageSources().explosion(null, null), host.getMaxHealth() * .6f);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5), x -> x != host)) {
            e.hurt(damageSources().explosion(null, null), 8);
            Vec3 d = e.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1e-4) { d = d.normalize(); e.setDeltaMovement(e.getDeltaMovement().add(d.x, .5, d.z)); e.hurtMarked = true; }
        }
        discard();
    }

    @Override public boolean hurt(DamageSource s, float a) {
        if (level().isClientSide || isRemoved()) return false;
        if (s.getEntity() instanceof CarrionEntity) { if (host != null) host.bombs--; discard(); return true; } // peeled off by tendrils
        return false;
    }

    @Override public void remove(RemovalReason r) { if (host != null && r != RemovalReason.DISCARDED && host.bombs > 0) host.bombs--; super.remove(r); }
    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
