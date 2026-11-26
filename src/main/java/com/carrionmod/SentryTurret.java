package com.carrionmod;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Stationary armoured gun emplacement that scans for Carrion and fires until it is destroyed. */
public class SentryTurret extends Monster implements RangedAttackMob {
    public SentryTurret(EntityType<? extends SentryTurret> t, Level l) { super(t, l); xpReward = 20; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ARMOR, 10).add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, 0, 6, 28f));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 10));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, CarrionEntity.class, 0, true, false, x -> true));
    }

    @Override public void performRangedAttack(LivingEntity t, float p) {
        Arrow a = new Arrow(level(), this);
        a.setBaseDamage(2.5);
        Vec3 d = t.getEyePosition().subtract(a.position()).add(0, -.2, 0);
        a.shoot(d.x, d.y, d.z, 2.6f, 2.5f);
        playSound(SoundEvents.DISPENSER_LAUNCH, 1f, 1.8f);
        level().addFreshEntity(a);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeLeashed(net.minecraft.world.entity.player.Player p) { return false; }
    @Override public boolean hurt(DamageSource s, float a) { return super.hurt(s, s.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) ? a * .5f : a); }
}
