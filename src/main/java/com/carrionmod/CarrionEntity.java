package com.carrionmod;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import net.minecraftforge.event.ForgeEventFactory;

import java.util.*;

/**
 * Carrion: a formless mass of hungry flesh. It cloaks, snares with webbing, spits acid, whips and grabs
 * with tendrils, lunges, bursts into spines, hardens into a carapace, takes control of other monsters
 * and grows by eating what it kills. Hated by (and hating) every other mob.
 */
public class CarrionEntity extends Monster {
    public static final int NONE = 0, LASH = 1, GRAB = 2, WEB = 3, SPIT = 4, LUNGE = 5, BURST = 6, CONSUME = 7, ROAR = 8, MIND = 9, HARDEN = 10, DASH = 11, HARPOON = 12, ECHO = 13;
    public static final int[] LEN = {0, 38, 66, 40, 34, 46, 52, 78, 48, 46, 36, 40, 44, 44};
    public static final int T_NONE = 0, T_WEB = 1, T_FLESH = 2, T_MIND = 3;
    private static final String THRALL = "CarrionThrallUntil";
    private static final int[] STAGE_BIOMASS = {0, 60, 160, 320};

    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(CarrionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(CarrionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TETHER = SynchedEntityData.defineId(CarrionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TKIND = SynchedEntityData.defineId(CarrionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HARDENED = SynchedEntityData.defineId(CarrionEntity.class, EntityDataSerializers.BOOLEAN);

    // client: when the current attack animation started
    private int atkStartAge;
    // server state
    private int atkTick, cooldown = 40, webTicks, hardenTicks, cloakTicks, recentHurt, lastAtk;
    private int biomass, energy = 100;
    private final List<LivingEntity> hooked = new ArrayList<>();
    private boolean enraged, enraged2, ambush;
    private LivingEntity victim, webTarget, mindTarget;
    private final Set<UUID> hit = new HashSet<>();
    private final Map<UUID, Long> thralls = new HashMap<>();

    public CarrionEntity(EntityType<? extends CarrionEntity> t, Level l) {
        super(t, l);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9).add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.ARMOR, 4);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK, 0); entityData.define(STAGE, 0); entityData.define(TETHER, -1);
        entityData.define(TKIND, 0); entityData.define(HARDENED, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CombatGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, this::isPrey));
    }

    private boolean isPrey(LivingEntity e) {
        return e.isAlive() && !(e instanceof CarrionEntity) && !(e instanceof ArmorStand) && !isThrall(e);
    }

    public static boolean isThrall(Entity e) {
        return e != null && e.getPersistentData().getLong(THRALL) > e.level().getGameTime();
    }

    // ---------- energy (Electrosis: cap grows with class) ----------
    public int maxEnergy() { return 100 + 50 * getStage(); }
    private boolean spend(int n) { if (energy < n) return false; energy -= n; return true; }

    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor w, net.minecraft.world.DifficultyInstance d,
            MobSpawnType t, net.minecraft.world.entity.SpawnGroupData g, CompoundTag tag) {
        int r = random.nextInt(10), st = r < 5 ? 0 : r < 8 ? 1 : r < 9 ? 2 : 3; // mostly small, rarely a giant
        biomass = STAGE_BIOMASS[st]; entityData.set(STAGE, st);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(140 + 60 * st); setHealth(getMaxHealth()); energy = maxEnergy();
        return super.finalizeSpawn(w, d, t, g, tag);
    }

    // ---------- accessors ----------
    public int getAtk() { return entityData.get(ATTACK); }
    public int getStage() { return entityData.get(STAGE); }
    public int getTether() { return entityData.get(TETHER); }
    public int getTetherKind() { return entityData.get(TKIND); }
    public boolean isHardened() { return entityData.get(HARDENED); }
    public float attackProgress(float age) { return getAtk() == NONE ? 0 : age - atkStartAge; }
    public float scale() { return 1f + 0.38f * getStage(); }
    public double dmg(double mult) { return getAttributeValue(Attributes.ATTACK_DAMAGE) * mult * (1 + 0.22 * getStage()) * (ambush ? 1.5 : 1); }

    /** Hydrophilia: in water the mass breaks into a swarm of worms and shrinks to slip through tight gaps. */
    public float morph, morphO;
    private boolean morphed;
    public float swarm(float pt) { return Mth.lerp(pt, morphO, morph); }
    @Override public void tick() {
        super.tick();
        morphO = morph; morph += ((isInWater() ? 1f : 0f) - morph) * .2f;
        if (morphed != morph > .5f) { morphed = morph > .5f; refreshDimensions(); }
    }
    @Override public EntityDimensions getDimensions(Pose p) { return super.getDimensions(p).scale(scale() * (morphed ? .4f : 1f)); }
    @Override public float maxUpStep() { return 1.1f * scale(); }
    @Override public boolean causeFallDamage(float f, float m, DamageSource s) { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> k) {
        super.onSyncedDataUpdated(k);
        if (k == ATTACK) atkStartAge = tickCount;
        if (k == STAGE) refreshDimensions();
    }

    // ---------- sounds ----------
    @Override protected SoundEvent getAmbientSound() { return CarrionMod.S_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return CarrionMod.S_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return CarrionMod.S_DEATH.get(); }
    @Override public float getVoicePitch() { return 1f - 0.1f * getStage(); }
    @Override protected void playStepSound(net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        playSound(CarrionMod.S_STEP.get(), 0.8f, 0.9f - 0.1f * getStage());
    }

    private void snd(SoundEvent e, float vol, float pitch) { level().playSound(null, getX(), getY(), getZ(), e, SoundSource.HOSTILE, vol, pitch); }

    private ServerLevel sl() { return (ServerLevel) level(); }

    private void blood(double x, double y, double z, int n, double spread, double speed) {
        sl().sendParticles(new DustParticleOptions(new Vector3f(.55f, .03f, .06f), 1.5f), x, y, z, n, spread, spread, spread, speed);
    }

    // ---------- damage ----------
    @Override public boolean hurt(DamageSource src, float amt) {
        if (level().isClientSide) return super.hurt(src, amt);
        if (isHardened()) {
            amt *= 0.35f;
            if (src.getEntity() instanceof LivingEntity a && !(a instanceof CarrionEntity) && src.getDirectEntity() == a && distanceToSqr(a) < 25) {
                a.hurt(damageSources().mobAttack(this), (float) dmg(0.2)); // spiked carapace
                snd(SoundEvents.ANVIL_PLACE, .4f, 1.6f);
            }
        }
        if (src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) amt *= Math.max(0, .6f - .3f * getStage()); // Pyrophilia
        if (cloakTicks > 0) uncloak();
        recentHurt = 60;
        boolean r = super.hurt(src, amt);
        if (r && src.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) && getAtk() == NONE && hardenTicks <= 0 && getStage() >= 2 && energy >= 35 && random.nextInt(3) == 0) startAttack(HARDEN);
        return r;
    }

    @Override public void tickDeath() {
        if (deathTime == 1) { snd(SoundEvents.SLIME_DEATH, 1.5f, .4f); }
        if (level() instanceof ServerLevel s && deathTime % 2 == 0)
            blood(getX(), getY() + .6 * scale(), getZ(), 14, .9 * scale(), .15);
        super.tickDeath();
    }

    @Override public boolean killedEntity(ServerLevel l, LivingEntity v) {
        addBiomass((int) v.getMaxHealth() / 2);
        return super.killedEntity(l, v);
    }

    public void addBiomass(int n) {
        biomass += n;
        int st = 0;
        for (int i = 0; i < STAGE_BIOMASS.length; i++) if (biomass >= STAGE_BIOMASS[i]) st = i;
        if (st > getStage()) {
            entityData.set(STAGE, st);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(140 + 60 * st);
            heal(60);
            snd(SoundEvents.WARDEN_ROAR, 1.5f, .6f);
            sl().sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 30, 1, .8, 1, .1);
        }
        heal(n * 0.5f);
    }

    // ---------- cloak / harden ----------
    private void cloak() {
        if (!spend(25)) return;
        cloakTicks = 400;
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0, false, false));
        snd(SoundEvents.SLIME_SQUISH_SMALL, .8f, .4f);
    }
    private void uncloak() { cloakTicks = 0; removeEffect(MobEffects.INVISIBILITY); }

    // ---------- tendril/projectile hooks ----------
    public void snare(LivingEntity v) {
        webTarget = v; webTicks = 70;
        entityData.set(TETHER, v.getId()); entityData.set(TKIND, T_WEB);
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
    }

    private void endTether() { entityData.set(TETHER, -1); entityData.set(TKIND, T_NONE); }

    // ---------- main server tick ----------
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (isInWater()) { // Hydrophilia: swarm-swimming
            addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 1, false, false));
            if (tickCount % 4 == 0) sl().sendParticles(ParticleTypes.BUBBLE, getX(), getY() + .8, getZ(), 6, 1, .5, 1, .05);
        }
        if (tickCount % 6 == 0 && energy < maxEnergy()) energy++;
        if (tickCount % 10 == 0) eatRemains();
        if (tickCount % 20 == 0) tickHiveCocoon();
        if (getAtk() == NONE && getTarget() == null && random.nextInt(500) == 0) startAttack(ECHO);
        if (cooldown > 0) cooldown--;
        if (recentHurt > 0) recentHurt--;
        if (hardenTicks > 0 && --hardenTicks == 0) { entityData.set(HARDENED, false); removeEffect(MobEffects.MOVEMENT_SLOWDOWN); }
        if (cloakTicks > 0 && --cloakTicks == 0) uncloak();

        LivingEntity t = getTarget();
        if (t != null && !t.isAlive()) { setTarget(null); t = null; }
        if (getAtk() != NONE) tickAttack(t);
        tickWeb();
        if (tickCount % 10 == 0) tickThralls(t);
        if (getAtk() == NONE && cloakTicks <= 0 && t != null && distanceToSqr(t) > 196 && random.nextInt(80) == 0) cloak();
        if (getAtk() == NONE && t == null && cloakTicks <= 0 && random.nextInt(900) == 0) cloak();
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth() && getAtk() == NONE) heal(0.5f + getStage() * .3f); // slow regeneration
    }

    private static boolean isMeat(ItemStack st) { var f = st.getItem().getFoodProperties(); return st.getItem() == Items.ROTTEN_FLESH || f != null && f.isMeat(); }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> HIVE = net.minecraft.tags.BlockTags.create(new net.minecraft.resources.ResourceLocation(CarrionMod.ID, "hive_blocks"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> COCOON = net.minecraft.tags.BlockTags.create(new net.minecraft.resources.ResourceLocation(CarrionMod.ID, "cocoon_blocks"));

    /** Hive blocks (tag carrion:hive_blocks) mend and recharge it; cocoon blocks (carrion:cocoon_blocks) are reabsorbed as biomass. Both tags are datapack-editable. */
    private void tickHiveCocoon() {
        boolean hive = false;
        for (var bp : net.minecraft.core.BlockPos.betweenClosed(blockPosition().offset(-3, -1, -3), blockPosition().offset(3, 2, 3))) {
            var bs = level().getBlockState(bp);
            if (bs.is(HIVE)) hive = true;
            else if (bs.is(COCOON) && getHealth() < getMaxHealth() * .9f) {
                level().destroyBlock(bp, false, this);
                addBiomass(5); snd(SoundEvents.SLIME_SQUISH, 1f, .5f);
                blood(bp.getX() + .5, bp.getY() + .5, bp.getZ() + .5, 8, .3, .03);
                break;
            }
        }
        // deposit surplus biomass as a cocoon while healthy in water
        if (isInWater() && getHealth() >= getMaxHealth() * .9f && biomass >= STAGE_BIOMASS[getStage()] + 10 && random.nextInt(4) == 0
                && level().getBlockState(blockPosition()).getFluidState().isSource() | level().getBlockState(blockPosition()).is(net.minecraft.world.level.block.Blocks.WATER)) {
            level().setBlockAndUpdate(blockPosition(), CarrionMod.COCOON.get().defaultBlockState());
            biomass -= 5; snd(SoundEvents.SLIME_SQUISH_SMALL, 1f, .5f);
        }
        if (hive) {
            heal(getMaxHealth() * .04f); energy = Math.min(maxEnergy(), energy + 10);
            sl().sendParticles(ParticleTypes.HEART, getX(), getY() + 1.4 * scale(), getZ(), 1, .5, .3, .5, 0);
        }
    }

    /** Reabsorb meat left behind by dead creatures. */
    private void eatRemains() {
        for (var it : level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(2.2 * scale(), 1, 2.2 * scale()))) {
            var st = it.getItem();
            if (isMeat(st)) {
                addBiomass(st.getCount() * 2); it.discard();
                snd(SoundEvents.GENERIC_EAT, .8f, .5f);
                blood(it.getX(), it.getY() + .2, it.getZ(), 6, .2, .03);
            }
        }
    }

    private void tickWeb() {
        if (webTicks <= 0) return;
        webTicks--;
        if (webTarget == null || !webTarget.isAlive() || webTarget.level() != level() || getAtk() == GRAB || getAtk() == CONSUME) { webTicks = 0; }
        else {
            Vec3 d = position().add(0, .4, 0).subtract(webTarget.position());
            double len = d.length();
            if (len > 2.6 * scale()) {
                Vec3 pull = d.normalize().scale(.5);
                webTarget.setDeltaMovement(pull.x, Math.max(webTarget.getDeltaMovement().y, .08), pull.z);
                webTarget.hurtMarked = true;
                if (webTicks % 3 == 0) sl().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB)),
                        webTarget.getX(), webTarget.getY() + .8, webTarget.getZ(), 3, .3, .4, .3, .02);
            } else webTicks = 0;
        }
        if (webTicks == 0) {
            if (getAtk() != GRAB && getAtk() != CONSUME) endTether();
            webTarget = null; cooldown = Math.min(cooldown, 4);
        }
    }

    private void tickThralls(LivingEntity t) {
        long now = level().getGameTime();
        thralls.entrySet().removeIf(en -> en.getValue() < now);
        for (UUID id : thralls.keySet()) {
            if (sl().getEntity(id) instanceof Mob m && m.isAlive()) {
                if (t != null && t.isAlive() && m.getTarget() != t) m.setTarget(t);
                if (t == null && m.getTarget() != null && m.getTarget() instanceof CarrionEntity) m.setTarget(null);
            }
        }
    }

    // ---------- attack scheduling ----------
    public void startAttack(int id) {
        entityData.set(ATTACK, id);
        atkTick = 0; lastAtk = id; hit.clear();
        // ambush: striking out of the cloak
        ambush = cloakTicks > 0 && (id == LASH || id == GRAB || id == LUNGE || id == CONSUME);
        if (id != WEB && id != MIND && id != SPIT && id != HARDEN && id != ROAR && id != BURST) uncloak();
        else if (id == BURST || id == ROAR) uncloak();
        if (id == GRAB || id == CONSUME) webTicks = 0;
    }

    private void endAttack() {
        victim = null; mindTarget = null; ambush = false;
        entityData.set(ATTACK, NONE);
        if (getTetherKind() != T_WEB) endTether();
        atkTick = 0;
        cooldown = 18 + random.nextInt(18);
    }

    private int countEnemies(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r), e -> e != this && isPrey(e) && e.isAlive()).size();
    }

    private Monster findThrallCandidate() {
        List<Monster> l = level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(14),
                m -> !(m instanceof CarrionEntity) && m.isAlive() && !isThrall(m) && m.getMaxHealth() <= 60);
        return l.isEmpty() ? null : l.get(random.nextInt(l.size()));
    }

    private boolean canConsume(LivingEntity t) {
        if (t.getArmorValue() > 0) return false; // armored prey cannot be devoured
        return t.getBbWidth() < 2.5f && (t.getHealth() <= t.getMaxHealth() * .35f || t.getMaxHealth() <= 24) && t.getMaxHealth() < 120;
    }

    void choose(LivingEntity t, double d) {
        double s = scale();
        float hp = getHealth() / getMaxHealth();
        if (!enraged && hp < .5f) { enraged = true; startAttack(ROAR); return; }
        if (!enraged2 && hp < .2f) { enraged2 = true; startAttack(ROAR); return; }
        int[] w = new int[14]; int st = getStage();
        int near = countEnemies(4.5 * s);
        if (d < 5.2 * s) {
            w[LASH] = 6; w[GRAB] = 3;
            if ((near >= 2 || hp < .5f) && st >= 1 && energy >= 35) w[BURST] = 5; // Acanthosis
            if (canConsume(t) && d < 4 * s) w[CONSUME] = 12;
        } else if (d < 9 * s) {
            w[GRAB] = 3; w[LUNGE] = 4; w[WEB] = 3; w[SPIT] = 3;
            if (st >= 1) w[DASH] = 5;
            if (st >= 2) w[HARPOON] = 5;
        } else if (d < 22) {
            if (st >= 1 && d < 14) w[DASH] = 4;
            if (st >= 2 && d < 15) w[HARPOON] = 4;
            w[WEB] = 4; w[SPIT] = 3; w[LUNGE] = d < 15 ? 3 : 0;
        }
        if (hardenTicks <= 0 && recentHurt > 0 && hp < .8f && st >= 2 && energy >= 35) w[HARDEN] = 3; // Keratosis
        if (st >= 1 && findThrallCandidate() != null && random.nextInt(3) == 0) w[MIND] = 3;
        w[lastAtk] = 0;
        if (webTicks > 0) { w[WEB] = 0; w[SPIT] = 0; w[LASH] += 2; w[GRAB] += 2; }
        int sum = 0; for (int x : w) sum += x;
        if (sum == 0) return;
        int r = random.nextInt(sum);
        for (int i = 1; i < w.length; i++) { r -= w[i]; if (r < 0) { startAttack(i); return; } }
    }

    private Vec3 front() { return Vec3.directionFromRotation(0, yBodyRot).normalize(); }

    private boolean canHit(Entity e) { return e instanceof LivingEntity l && l != this && !(l instanceof CarrionEntity) && !isThrall(l) && !(l instanceof ArmorStand) && l.isAlive(); }

    private void knock(LivingEntity e, Vec3 dir, double str, double up) {
        e.setDeltaMovement(e.getDeltaMovement().add(dir.x * str, up, dir.z * str));
        e.hurtMarked = true;
    }

    // ---------- attacks ----------
    private void tickAttack(LivingEntity t) {
        int a = getAtk();
        switch (a) {
            case LASH -> lash(t);
            case GRAB -> grab(t, false);
            case CONSUME -> grab(t, true);
            case WEB -> web(t);
            case SPIT -> spit(t);
            case LUNGE -> lunge(t);
            case BURST -> burst();
            case ROAR -> roar();
            case MIND -> mind();
            case HARDEN -> harden();
            case DASH -> dash();
            case HARPOON -> harpoon();
            case ECHO -> echo();
        }
        atkTick++;
        if (atkTick >= LEN[a]) endAttack();
    }

    /** Two crossing tendril sweeps: left arms across, then right arms back. */
    private void lash(LivingEntity t) {
        if (atkTick == 2) snd(SoundEvents.SLIME_SQUISH, 1f, .3f);
        if (atkTick == 12 || atkTick == 25) {
            boolean second = atkTick == 25;
            snd(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, second ? .5f : .65f);
            snd(SoundEvents.RAVAGER_ATTACK, .6f, 1.2f);
            double s = scale(), reach = 5.4 * s;
            Vec3 f = front(); Vec3 side = new Vec3(-f.z, 0, f.x).scale(second ? -1 : 1);
            for (int i = -6; i <= 6; i++) { // visible whip arc
                double ang = i / 6.0 * 1.1;
                Vec3 p = f.scale(Math.cos(ang)).add(side.scale(Math.sin(ang) * (second ? -1 : 1)));
                for (double r = 1.5 * s; r < reach; r += 1.1 * s)
                    blood(getX() + p.x * r, getY() + .5 * s + .3 * Math.abs(i) / 6, getZ() + p.z * r, 1, .05, .02);
            }
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach, 1.2 * s, reach), this::canHit)) {
                Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                if (to.length() > reach + e.getBbWidth() / 2 || to.length() < 1e-3) continue;
                if (f.dot(to.normalize()) < .15) continue;
                if (e.hurt(damageSources().mobAttack(this), (float) dmg(second ? 1.0 : .85)))
                    knock(e, to.normalize().scale(.6).add(side.scale(second ? .9 : -.9)).normalize(), second ? 1.5 : 1.0, .25);
            }
        }
    }

    private void holdVictim(Vec3 pos) {
        if (victim == null) return;
        if (victim instanceof ServerPlayer sp) sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
        else victim.moveTo(pos.x, pos.y, pos.z);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.fallDistance = 0; victim.hurtMarked = true;
    }

    /** Grab -> hoist -> squeeze -> slam. Consume replaces the slam with devouring. */
    private void grab(LivingEntity t, boolean eat) {
        double s = scale();
        if (atkTick == 0) {
            if (t == null) { endAttack(); return; }
            entityData.set(TETHER, t.getId()); entityData.set(TKIND, T_FLESH);
            snd(SoundEvents.HONEY_BLOCK_SLIDE, 1.2f, .5f);
        }
        if (atkTick == 12) {
            if (t == null || !t.isAlive() || distanceToSqr(t) > Mth.square(9 * s)) { snd(SoundEvents.SLIME_SQUISH_SMALL, 1f, .5f); cooldown = 8; endAttack(); return; }
            victim = t;
            snd(SoundEvents.SLIME_ATTACK, 1.2f, .5f);
            if (victim instanceof Mob m) m.setTarget(null);
        }
        if (victim == null || !victim.isAlive()) {
            if (atkTick > 12) { victim = null; if (eat && atkTick < LEN[CONSUME] - 6) { atkTick = LEN[CONSUME] - 6; } else if (!eat) { atkTick = LEN[GRAB]; } }
            return;
        }
        if (atkTick < 12) return;
        Vec3 f = front();
        if (!eat) {
            double lift;
            if (atkTick < 28) lift = Mth.lerp(ease((atkTick - 12) / 16f), .2, 2.6 * s);
            else if (atkTick < 40) lift = 2.6 * s + Math.sin(atkTick * 1.3) * .12;
            else if (atkTick < 47) lift = Mth.lerp(ease((atkTick - 40) / 7f), 2.6 * s, 0);
            else lift = 0;
            double fwd = atkTick < 40 ? 1.7 * s : Mth.lerp(ease((atkTick - 40) / 7f), 1.7 * s, 2.6 * s);
            if (atkTick < 47) holdVictim(position().add(f.scale(fwd)).add(0, lift, 0));
            if (atkTick >= 28 && atkTick < 40 && atkTick % 6 == 0) { // squeeze
                victim.hurt(damageSources().mobAttack(this), (float) dmg(.3));
                snd(SoundEvents.SLIME_SQUISH, 1f, .35f);
                blood(victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 6, .3, .02);
            }
            if (atkTick == 47) { // slam
                victim.hurt(damageSources().mobAttack(this), (float) dmg(1.3));
                snd(SoundEvents.GENERIC_EXPLODE, .9f, .6f); snd(SoundEvents.ANVIL_LAND, 1f, .5f);
                sl().sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + .1, victim.getZ(), 2, .6, 0, .6, 0);
                blood(victim.getX(), victim.getY() + .2, victim.getZ(), 40, 1, .25);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(4 * s, 1, 4 * s), x -> canHit(x) && x != victim)) {
                    e.hurt(damageSources().mobAttack(this), (float) dmg(.5));
                    knock(e, e.position().subtract(victim.position()).multiply(1, 0, 1).normalize(), .9, .5);
                }
                if (random.nextBoolean()) { knock(victim, front().scale(1), 3.0, .55); snd(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f, .5f); } // hurl
                else knock(victim, f, 1.1, .35);
            }
        } else {
            // pull into the maw and devour
            double q = ease((atkTick - 12) / 12f);
            Vec3 maw = position().add(f.scale(Mth.lerp(q, 1.9 * s, 1.5 * s))).add(0, Mth.lerp(q, 1.2 * s, .7 * s), 0);
            if (atkTick < 70) holdVictim(maw.add(0, Math.sin(atkTick * 2.1) * .06, 0));
            if (atkTick >= 24 && atkTick < 70 && atkTick % 8 == 0) {
                float before = victim.getHealth();
                victim.hurt(damageSources().mobAttack(this), (float) dmg(.28));
                heal(Math.max(1, before - victim.getHealth()) * .6f);
                snd(SoundEvents.GENERIC_EAT, 1.2f, .5f); snd(SoundEvents.SLIME_SQUISH, 1f, .3f);
                blood(maw.x, maw.y, maw.z, 10, .3, .08);
                sl().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ROTTEN_FLESH)), maw.x, maw.y, maw.z, 6, .3, .3, .3, .06);
            }
            if (atkTick == 70) {
                float mh = victim.getMaxHealth();
                victim.hurt(damageSources().mobAttack(this), (float) dmg(2.2)); // final bite
                if (!victim.isAlive()) { addBiomass((int) mh); snd(SoundEvents.PLAYER_BURP, 1f, .4f); }
                else knock(victim, f, 1.4, .3);
            }
        }
    }

    private static float ease(float x) { x = Mth.clamp(x, 0, 1); return x * x * (3 - 2 * x); }

    /** Fire a sticky strand that drags the victim in. */
    private void web(LivingEntity t) {
        if (atkTick == 6) snd(SoundEvents.SPIDER_AMBIENT, 1.2f, .4f);
        if (atkTick == 14 && t != null) {
            snd(SoundEvents.SPIDER_HURT, 1f, .5f); snd(SoundEvents.SLIME_ATTACK, 1f, .6f);
            shoot(t, CarrionShot.WEB, 2.2f, 0, .012);
        }
    }

    private void shoot(LivingEntity t, int kind, float speed, float spread, double grav) {
        CarrionShot p = new CarrionShot(level(), this, kind);
        double s = scale();
        Vec3 from = position().add(front().scale(1.8 * s)).add(0, .9 * s, 0);
        p.setPos(from.x, from.y, from.z);
        double dx = t.getX() - from.x, dz = t.getZ() - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double dy = t.getY(.5) - from.y + dist * (kind == CarrionShot.SPIT ? .06 : .02);
        p.shoot(dx, dy, dz, speed, spread);
        level().addFreshEntity(p);
    }

    /** Acid volley: three then two blobs. */
    private void spit(LivingEntity t) {
        if (atkTick == 4) snd(SoundEvents.RAVAGER_STUNNED, .8f, .5f);
        if (t == null) return;
        if (atkTick == 18 || atkTick == 25) {
            snd(SoundEvents.LLAMA_SPIT, 1.4f, .4f); snd(SoundEvents.SLIME_SQUISH, 1f, .5f);
            int n = atkTick == 18 ? 3 : 2;
            for (int i = 0; i < n; i++) shoot(t, CarrionShot.SPIT, 1.5f, 5.5f, .035);
        }
    }

    /** Coil, then leap through the target, bowling over whatever it hits. */
    private void lunge(LivingEntity t) {
        if (atkTick < 14) { getNavigation().stop(); setDeltaMovement(getDeltaMovement().multiply(.2, 1, .2)); }
        if (atkTick == 14) {
            if (t == null) { endAttack(); return; }
            Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
            double dist = d.length();
            d = d.normalize().scale(Mth.clamp(.6 + dist * .09, 1, 1.9));
            setDeltaMovement(d.x, .42, d.z);
            hasImpulse = true;
            snd(SoundEvents.RAVAGER_ROAR, 1.2f, .7f); snd(SoundEvents.SLIME_JUMP, 1f, .3f);
        }
        if (atkTick >= 14 && atkTick < 32) {
            sl().sendParticles(new DustParticleOptions(new Vector3f(.55f, .03f, .06f), 1.6f), getX(), getY() + .7 * scale(), getZ(), 4, .6, .3, .6, .02);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(.5 * scale()), this::canHit)) {
                if (!hit.add(e.getUUID())) continue;
                e.hurt(damageSources().mobAttack(this), (float) dmg(1.15));
                knock(e, getDeltaMovement().multiply(1, 0, 1).normalize(), 1.6, .4);
                snd(SoundEvents.SLIME_ATTACK, 1.2f, .4f);
            }
            if (atkTick > 20 && onGround()) atkTick = Math.max(atkTick, 32);
        }
    }

    /** Draw everything in, then detonate in a ring of bone spines. */
    private void burst() {
        double s = scale();
        if (atkTick < 20) {
            getNavigation().stop();
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7 * s), this::canHit)) {
                Vec3 d = position().subtract(e.position()).multiply(1, 0, 1);
                if (d.length() > 1.8 * s) { e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(.07))); e.hurtMarked = true; }
            }
            if (atkTick % 4 == 0) snd(SoundEvents.SLIME_SQUISH, .8f, .3f + atkTick * .02f);
        }
        if (atkTick == 20) {
            spend(35);
            snd(SoundEvents.GENERIC_EXPLODE, 1.3f, .7f); snd(SoundEvents.EVOKER_FANGS_ATTACK, 1.5f, .6f);
            sl().sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + .8, getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 14; i++) {
                double ang = i / 14.0 * Math.PI * 2;
                CarrionShot sp = new CarrionShot(level(), this, CarrionShot.SPINE);
                sp.setPos(getX() + Math.cos(ang) * 1.2 * s, getY() + .7 * s, getZ() + Math.sin(ang) * 1.2 * s);
                sp.shoot(Math.cos(ang), .08 + (i % 2) * .1, Math.sin(ang), 1.5f, 0);
                level().addFreshEntity(sp);
            }
            for (int i = 0; i < 40; i++) { // blood ring
                double ang = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 5 * s;
                blood(getX() + Math.cos(ang) * r, getY() + .3, getZ() + Math.sin(ang) * r, 2, .2, .05);
            }
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6 * s), this::canHit)) {
                Vec3 d = e.position().subtract(position()).multiply(1, 0, 1);
                e.hurt(damageSources().mobAttack(this), (float) dmg(1.0));
                knock(e, d.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : d.normalize(), 1.4, .6);
            }
        }
    }

    /** Rear up and bellow; shockwave, then a frenzy. */
    private void roar() {
        double s = scale();
        getNavigation().stop();
        if (atkTick == 4) snd(SoundEvents.WARDEN_ROAR, 2f, .55f);
        if (atkTick == 16) {
            snd(SoundEvents.RAVAGER_ROAR, 2f, .4f); snd(SoundEvents.WARDEN_SONIC_BOOM, 1f, .7f);
            sl().sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.2 * s, getZ(), 1, 0, 0, 0, 0);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(9 * s), this::canHit)) {
                e.hurt(damageSources().mobAttack(this), (float) dmg(.35));
                knock(e, e.position().subtract(position()).multiply(1, 0, 1).normalize(), 1.2, .45);
            }
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        }
        if (atkTick >= 16 && atkTick < 34 && atkTick % 3 == 0)
            for (int i = 0; i < 16; i++) {
                double ang = i / 16.0 * Math.PI * 2, r = (atkTick - 16) * .5 * s + 1;
                sl().sendParticles(ParticleTypes.POOF, getX() + Math.cos(ang) * r, getY() + .2, getZ() + Math.sin(ang) * r, 1, 0, 0, 0, .02);
            }
    }

    /** Thread a tendril into a nearby monster and puppet it against the target. */
    private void mind() {
        getNavigation().stop();
        if (atkTick == 0) {
            mindTarget = findThrallCandidate();
            if (mindTarget == null) { endAttack(); return; }
            entityData.set(TETHER, mindTarget.getId()); entityData.set(TKIND, T_MIND);
            snd(SoundEvents.SLIME_SQUISH, 1f, .3f);
        }
        if (mindTarget == null) return;
        if (atkTick == 10) snd(SoundEvents.ELDER_GUARDIAN_CURSE, .7f, .5f);
        if (atkTick == 24) {
            long until = level().getGameTime() + 900;
            mindTarget.getPersistentData().putLong(THRALL, until);
            thralls.put(mindTarget.getUUID(), until);
            mindTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 900, 0, false, false));
            mindTarget.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 1));
            if (mindTarget instanceof Mob m) m.setTarget(getTarget());
            sl().sendParticles(ParticleTypes.SOUL, mindTarget.getX(), mindTarget.getY() + 1, mindTarget.getZ(), 20, .4, .6, .4, .05);
            snd(SoundEvents.SOUL_ESCAPE, 1.2f, .5f);
        }
    }

    /** Xiphorrhea: coil, then a slashing dash that shreds wooden barriers. */
    private void dash() {
        double s = scale();
        if (atkTick < 12) { getNavigation().stop(); setDeltaMovement(getDeltaMovement().multiply(.2, 1, .2)); if (atkTick == 3) snd(SoundEvents.SLIME_SQUISH, 1f, .3f); }
        if (atkTick == 12) {
            LivingEntity t = getTarget();
            Vec3 d = t != null ? t.position().subtract(position()).multiply(1, 0, 1) : front();
            d = d.lengthSqr() < 1e-4 ? front() : d.normalize();
            setDeltaMovement(d.x * 2.1, .12, d.z * 2.1); hasImpulse = true; yBodyRot = (float) (Mth.atan2(d.z, d.x) * 57.2958 - 90);
            snd(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, .4f); snd(SoundEvents.RAVAGER_ATTACK, 1f, .6f);
        }
        if (atkTick >= 12 && atkTick < 26) {
            sl().sendParticles(new DustParticleOptions(new Vector3f(.55f, .03f, .06f), 1.4f), getX(), getY() + .6 * s, getZ(), 5, .8 * s, .3, .8 * s, .03);
            sl().sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + front().x * 2 * s, getY() + .8 * s, getZ() + front().z * 2 * s, 1, 0, 0, 0, 0);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.1 * s, .5, 1.1 * s), this::canHit)) {
                if (!hit.add(e.getUUID())) continue;
                e.hurt(damageSources().mobAttack(this), (float) dmg(1.1));
                Vec3 side = new Vec3(-front().z, 0, front().x);
                knock(e, front().scale(.7).add(side.scale(.8 * (random.nextBoolean() ? 1 : -1))).normalize(), 1.5, .35);
                snd(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, .5f);
            }
            if (ForgeEventFactory.getMobGriefingEvent(level(), this))
                for (var bp : net.minecraft.core.BlockPos.betweenClosed(blockPosition().offset(-2, 0, -2), blockPosition().offset(2, 2, 2))) {
                    var bs = level().getBlockState(bp);
                    if ((bs.is(net.minecraft.tags.BlockTags.PLANKS) || bs.is(net.minecraft.tags.BlockTags.WOODEN_DOORS) || bs.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)
                            || bs.is(net.minecraft.tags.BlockTags.LEAVES) || bs.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)) && random.nextInt(3) == 0)
                        level().destroyBlock(bp, true, this);
                }
            if (atkTick > 18 && onGround()) atkTick = Math.max(atkTick, 26);
        }
    }

    /** Harpagorrhea: fan of harpoon tendrils out front, then hauls everything it hooked back in. */
    private void harpoon() {
        double s = scale(), reach = 12 * s;
        getNavigation().stop();
        if (atkTick == 0) hooked.clear();
        if (atkTick == 4) snd(SoundEvents.SLIME_SQUISH, 1.2f, .3f);
        if (atkTick == 10) { snd(SoundEvents.TRIDENT_THROW, 1.5f, .4f); snd(SoundEvents.SPIDER_HURT, 1f, .4f); }
        if (atkTick >= 10 && atkTick < 22) {
            double r = reach * ease((atkTick - 10) / 6f);
            Vec3 f = front();
            for (int k = -2; k <= 2; k++) {
                double ang = k * .22; Vec3 dir = new Vec3(f.x * Math.cos(ang) - f.z * Math.sin(ang), 0, f.x * Math.sin(ang) + f.z * Math.cos(ang));
                for (double q = 1.5 * s; q < r; q += 1.4 * s)
                    blood(getX() + dir.x * q, getY() + .7 * s, getZ() + dir.z * q, 1, .04, .01);
                sl().sendParticles(ParticleTypes.CRIT, getX() + dir.x * r, getY() + .7 * s, getZ() + dir.z * r, 2, .1, .1, .1, .02);
            }
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach, 1.5 * s, reach), this::canHit)) {
                Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                double dist = to.length();
                if (dist > r || dist < 1e-3 || f.dot(to.normalize()) < .8 || hooked.contains(e)) continue;
                hooked.add(e);
                e.hurt(damageSources().mobAttack(this), (float) dmg(.75));
                snd(SoundEvents.TRIDENT_HIT, 1f, .6f);
            }
        }
        if (atkTick >= 24 && atkTick < 38) { // retract: reel the catch in
            if (atkTick == 24) snd(SoundEvents.FISHING_BOBBER_RETRIEVE, 1.5f, .4f);
            for (LivingEntity e : hooked) {
                if (!e.isAlive()) continue;
                Vec3 d = position().add(front().scale(1.6 * s)).subtract(e.position());
                if (d.length() > 1.8 * s) { Vec3 v = d.normalize().scale(1.1); e.setDeltaMovement(v.x, Math.max(.1, v.y), v.z); e.hurtMarked = true; }
            }
        }
        if (atkTick == 38) for (LivingEntity e : hooked) if (e.isAlive() && distanceToSqr(e) < Mth.square(3 * s)) {
            e.hurt(damageSources().mobAttack(this), (float) dmg(.9)); knock(e, front(), .8, .3); snd(SoundEvents.SLIME_ATTACK, 1f, .5f);
        }
    }

    /** Echolocation: a pulse that outlines living prey and edible remains through walls. */
    private void echo() {
        getNavigation().stop();
        if (atkTick == 8) snd(SoundEvents.WARDEN_SONIC_CHARGE, 1.2f, .8f);
        if (atkTick >= 14 && atkTick < 34 && atkTick % 2 == 0) {
            double r = (atkTick - 14) * 1.6;
            for (int i = 0; i < 24; i++) {
                double a = i / 24.0 * Math.PI * 2;
                sl().sendParticles(ParticleTypes.SCULK_CHARGE_POP, getX() + Math.cos(a) * r, getY() + .3, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        if (atkTick == 14) {
            snd(SoundEvents.WARDEN_SONIC_BOOM, .7f, 1.6f);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32), x -> x != this))
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            for (var it : level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(32)))
                if (isMeat(it.getItem())) it.setGlowingTag(true);
        }
    }

    private void harden() {
        getNavigation().stop();
        if (atkTick == 14) {
            spend(35);
            entityData.set(HARDENED, true); hardenTicks = 200;
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, false, false));
            snd(SoundEvents.NETHERITE_BLOCK_PLACE, 1.5f, .5f); snd(SoundEvents.TURTLE_SHAMBLE, 1f, .5f);
            sl().sendParticles(ParticleTypes.CRIT, getX(), getY() + 1, getZ(), 24, 1.2, .6, 1.2, .1);
        }
    }

    // ---------- movement / combat goal ----------
    class CombatGoal extends Goal {
        CombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { LivingEntity t = getTarget(); return t != null && t.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 40, 40);
            double d = distanceTo(t), s = scale();
            if (getAtk() != NONE) {
                if (getAtk() != LUNGE) getNavigation().stop();
                return;
            }
            getNavigation().moveTo(t, d < 3 * s ? .7 : 1.15);
            if (cooldown <= 0 && (hasLineOfSight(t) || d < 6 * s)) choose(t, d);
        }
    }

    // ---------- persistence ----------
    @Override public void addAdditionalSaveData(CompoundTag n) {
        super.addAdditionalSaveData(n);
        n.putInt("Biomass", biomass); n.putInt("Energy", energy); n.putBoolean("Enraged", enraged); n.putBoolean("Enraged2", enraged2);
    }

    @Override public void readAdditionalSaveData(CompoundTag n) {
        super.readAdditionalSaveData(n);
        biomass = n.getInt("Biomass"); energy = n.contains("Energy") ? n.getInt("Energy") : 100; enraged = n.getBoolean("Enraged"); enraged2 = n.getBoolean("Enraged2");
        int st = 0;
        for (int i = 0; i < STAGE_BIOMASS.length; i++) if (biomass >= STAGE_BIOMASS[i]) st = i;
        entityData.set(STAGE, st);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(140 + 60 * st);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
}
