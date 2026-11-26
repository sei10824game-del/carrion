package com.carrionmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

/** Walking gatling mech with three armour plates: plates soak nearly all damage until tendrils tear them off. */
public class SentryMech extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> PLATES = SynchedEntityData.defineId(SentryMech.class, EntityDataSerializers.INT);
    private int peelCd;

    public SentryMech(EntityType<? extends SentryMech> t, Level l) { super(t, l); xpReward = 40; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 90).add(Attributes.MOVEMENT_SPEED, .22)
                .add(Attributes.KNOCKBACK_RESISTANCE, .8).add(Attributes.FOLLOW_RANGE, 30);
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(PLATES, 3); }
    public int plates() { return entityData.get(PLATES); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new RangedAttackGoal(this, .8, 2, 18f));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 10));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, CarrionEntity.class, 0, true, false, x -> true));
    }

    @Override public void performRangedAttack(LivingEntity t, float p) {
        Arrow a = new Arrow(level(), this); a.setBaseDamage(1.6);
        Vec3 d = t.getEyePosition().subtract(a.position());
        a.shoot(d.x, d.y, d.z, 2.8f, 5f);
        if (tickCount % 4 == 0) playSound(SoundEvents.DISPENSER_LAUNCH, .7f, 1.6f);
        level().addFreshEntity(a);
    }

    public void peel() {
        if (plates() <= 0 || peelCd > 0) return;
        entityData.set(PLATES, plates() - 1); peelCd = 15;
        playSound(SoundEvents.ANVIL_BREAK, 1f, 1.4f);
    }

    @Override public void aiStep() { super.aiStep(); if (peelCd > 0) peelCd--; }

    @Override public boolean hurt(DamageSource s, float a) {
        if (s.getEntity() instanceof CarrionEntity) peel(); // ripped at by tendrils
        return super.hurt(s, plates() > 0 ? a * .15f : a);
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("Plates", plates()); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); if (t.contains("Plates")) entityData.set(PLATES, t.getInt("Plates")); }
}
