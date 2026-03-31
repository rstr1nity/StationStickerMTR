package com.yourname.stationsticker.block.entity;

import com.yourname.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LineSchemeEntity extends BlockEntity {
    private long platformId = 0;

    public LineSchemeEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINE_SCHEME.get(), pos, state);
    }

    public long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(long platformId) {
        this.platformId = platformId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("platformId", platformId);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        platformId = nbt.getLong("platformId");
    }
}