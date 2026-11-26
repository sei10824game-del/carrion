package com.carrionmod;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Hovering gun drone: keeps its distance from Carrion and fires 3-round bursts. A web or a hard hit grounds it. */
public class SentryDrone extends Monster {
    private int stun, burst, shotCd = 30;

    public SentryDrone(EntityType<? extends SentryDrone> t, Level l) {
        super(t, l);
        moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.FLYING_SPEED, .7)
                .add(Attributes.MOVEMENT_SPEED, .5).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 4);
    }

    @Override protected PathNavigation createNavigation(Level l) {
        var n = new FlyingPathNavigation(this, l); n.setCanOpenDoors(false); n.setCanFloat(true); return n;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new Hover());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, CarrionEntity.class, 0, true, false, x -> true));
    }

    /** Webbed / hit hard: drops out of the air and stops shooting for a while. */
    public void stun() { stun = 80; setNoGravity(false); }
    @Override public boolean causeFallDamage(float f, float m, DamageSource s) { if (stun > 0) { hurt(damageSources().fall(), 8); stun = 0; } return false; }

    @Override public void aiStep() {
        super.aiStep();
        if (stun > 0) { if (--stun == 0) setNoGravity(true); }
        else if (!isNoGravity()) setNoGravity(true);
        if (!level().isClientSide && stun <= 0) { // idle bob
            setDeltaMovement(getDeltaMovement().add(0, Math.sin(tickCount * .15) * .004, 0));
        }
    }

    class Hover extends Goal {
        Hover() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return getTarget() != null && stun <= 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            LivingEntity t = getTarget(); if (t == null) return;
            getLookControl().setLookAt(t, 40, 40);
            double d = distanceTo(t);
            Vec3 want = t.position().add(0, t.getBbHeight() + 3.5, 0);
            Vec3 away = position().subtract(t.position()).multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-away.z, 0, away.x).scale(Math.sin(tickCount * .05) * 4);
            Vec3 p = d > 13 ? want : want.add(away.scale(10 - d + 0)).add(side);
            getMoveControl().setWantedPosition(p.x, p.y, p.z, 1.0);
            if (--shotCd <= 0 && d < 24 && hasLineOfSight(t)) {
                if (burst % 3 == 0 && burst > 0) { burst = 0; shotCd = 45; return; }
                burst++; shotCd = 4; shoot(t);
            }
        }
    }

    private void shoot(LivingEntity t) {
        Arrow a = new Arrow(level(), this); a.setBaseDamage(1.8);
        Vec3 d = t.getEyePosition().subtract(a.position());
        a.shoot(d.x, d.y, d.z, 2.4f, 4f);
        playSound(SoundEvents.DISPENSER_LAUNCH, .8f, 2f);
        level().addFreshEntity(a);
    }

    @Override public boolean isPushable() { return true; }
}
