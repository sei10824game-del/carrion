package com.carrionmod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Spinning-blade hazard: anything touching it is shredded. A carapace is stripped of all energy by it. */
public class TurbineBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15); // inset like a cactus so contact registers

    public TurbineBlock(Properties p) { super(p); }

    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public void entityInside(BlockState s, Level l, BlockPos p, Entity e) {
        if (l.isClientSide || !(e instanceof LivingEntity le)) return;
        if (e instanceof CarrionEntity c && c.isHardened()) c.drainEnergy();
        le.hurt(l.damageSources().generic(), 6f);
    }

    @Override public BlockPathTypes getBlockPathType(BlockState s, BlockGetter l, BlockPos p, Mob m) { return BlockPathTypes.DAMAGE_OTHER; }
}
