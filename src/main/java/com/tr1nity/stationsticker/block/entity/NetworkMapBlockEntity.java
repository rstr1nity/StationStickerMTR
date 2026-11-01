package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NetworkMapBlockEntity extends BlockEntity {
    public NetworkMapBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.NETWORK_MAP.get(), pos, state); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(2).expandTowards(0, 3, 0); }
}
