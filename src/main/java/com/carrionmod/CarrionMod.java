package com.carrionmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CarrionMod.ID)
public class CarrionMod {
    public static final String ID = "carrion";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<EntityType<CarrionEntity>> CARRION = ENTITIES.register("carrion",
            () -> EntityType.Builder.of(CarrionEntity::new, MobCategory.MONSTER).sized(2.6f, 1.6f)
                    .clientTrackingRange(10).build("carrion"));
    public static final RegistryObject<EntityType<CarrionShot>> SHOT = ENTITIES.register("carrion_shot",
            () -> EntityType.Builder.<CarrionShot>of(CarrionShot::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(8).updateInterval(2).build("carrion_shot"));
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final RegistryObject<Block> HIVE = BLOCKS.register("hive", () -> new Block(BlockBehaviour.Properties.of().strength(2f).sound(SoundType.HONEY_BLOCK).lightLevel(st -> 4)));
    public static final RegistryObject<Block> COCOON = BLOCKS.register("cocoon", () -> new Block(BlockBehaviour.Properties.of().strength(1f).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Item> HIVE_ITEM = ITEMS.register("hive", () -> new BlockItem(HIVE.get(), new Item.Properties()));
    public static final RegistryObject<Item> COCOON_ITEM = ITEMS.register("cocoon", () -> new BlockItem(COCOON.get(), new Item.Properties()));
    /** Right-click any block to convert it into a hive / cocoon. */
    public static final RegistryObject<Item> HIVE_SPORE = ITEMS.register("hive_spore", () -> new SporeItem(HIVE));
    public static final RegistryObject<Item> COCOON_SPORE = ITEMS.register("cocoon_spore", () -> new SporeItem(COCOON));
    public static final RegistryObject<Item> EGG = ITEMS.register("carrion_spawn_egg",
            () -> new ForgeSpawnEggItem(CARRION, 0x4a0910, 0xc4352f, new Item.Properties()));

    public CarrionMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.register(ModEvents.class);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
    }

    public static class ModEvents {
        @SubscribeEvent
        public static void attrs(EntityAttributeCreationEvent e) {
            e.put(CARRION.get(), CarrionEntity.createAttributes().build());
        }

        @SubscribeEvent
        public static void spawn(SpawnPlacementRegisterEvent e) {
            e.register(CARRION.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        }

        @SubscribeEvent
        public static void tab(BuildCreativeModeTabContentsEvent e) {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(EGG);
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) { e.accept(HIVE_SPORE); e.accept(COCOON_SPORE); }
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) { e.accept(HIVE_ITEM); e.accept(COCOON_ITEM); }
        }
    }

    /** Every other mob hates Carrion: armed mobs hunt it, unarmed ones flee. */
    public static class ForgeEvents {
        @SubscribeEvent
        public static void join(EntityJoinLevelEvent e) {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m) || m instanceof CarrionEntity) return;
            if (m.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                m.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(m, CarrionEntity.class, 0, true, false, x -> true));
            } else if (m instanceof PathfinderMob p) {
                p.goalSelector.addGoal(0, new AvoidEntityGoal<>(p, CarrionEntity.class, 18f, 1.3, 1.7));
            }
        }

        @SubscribeEvent
        public static void retarget(LivingChangeTargetEvent e) {
            var t = e.getNewTarget();
            if (t == null) return;
            // mind-controlled mobs and Carrion never fight each other
            if (CarrionEntity.isThrall(e.getEntity()) && t instanceof CarrionEntity) e.setCanceled(true);
            if (e.getEntity() instanceof CarrionEntity && CarrionEntity.isThrall(t)) e.setCanceled(true);
        }
    }

    static class SporeItem extends Item {
        final RegistryObject<Block> target;
        SporeItem(RegistryObject<Block> t) { super(new Item.Properties().stacksTo(16)); target = t; }
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c) {
            var l = c.getLevel(); var p = c.getClickedPos(); var old = l.getBlockState(p);
            if (old.is(target.get()) || old.getDestroySpeed(l, p) < 0 || old.hasBlockEntity()) return InteractionResult.PASS;
            if (!l.isClientSide) {
                l.setBlockAndUpdate(p, target.get().defaultBlockState());
                l.playSound(null, p, net.minecraft.sounds.SoundEvents.SLIME_SQUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1, .5f);
                if (c.getPlayer() == null || !c.getPlayer().getAbilities().instabuild) c.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
    }
}
