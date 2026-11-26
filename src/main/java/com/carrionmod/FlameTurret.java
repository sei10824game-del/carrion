package com.carrionmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Short-range gun emplacement that sprays burning fuel; Carrion's Pyrophilia (fire resistance) counters it. */
public class FlameTurret extends SentryTurret {
    public FlameTurret(EntityType<? extends FlameTurret> t, Level l) { super(t, l); }

    @Override protected int interval() { return 3; }
    @Override protected float range() { return 11f; }

    @Override public void performRangedAttack(LivingEntity t, float p) {
        if (!(level() instanceof ServerLevel l)) return;
        Vec3 from = position().add(0, 1.0, 0), to = t.position().add(0, t.getBbHeight() * .5, 0);
        Vec3 d = to.subtract(from); double len = d.length(); if (len < 1e-3) return;
        Vec3 dir = d.scale(1 / len);
        for (double q = 1; q < Math.min(len, 11); q += .7)
            l.sendParticles(ParticleTypes.FLAME, from.x + dir.x * q, from.y + dir.y * q, from.z + dir.z * q, 2, .12 + q * .02, .12, .12 + q * .02, .01);
        playSound(SoundEvents.BLAZE_SHOOT, .6f, 1.4f);
        if (len <= 11) { t.hurt(damageSources().inFire(), 1.5f); t.setSecondsOnFire(5); }
    }
}
