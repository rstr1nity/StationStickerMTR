package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class VerticalLineBlockEntity extends BlockEntity {
    public VerticalLineBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.VERTICAL_LINE.get(), pos, state); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(2).expandTowards(0, 2, 0); }
}
