package com.yourname.stationsticker.block.entity;

import com.yourname.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StationStickerBlockEntity extends BlockEntity {
    private long platformId = 0;
    private String customName = "";

    public StationStickerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATION_STICKER.get(), pos, state);
    }

    public long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(long platformId) {
        this.platformId = platformId;
        setChanged();
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String name) {
        this.customName = name;
        setChanged();
    }

    public Component getDisplayName() {
        if (customName != null && !customName.isEmpty()) {
            return Component.literal(customName);
        }
        if (platformId != 0) {
            return Component.literal("Платформа ID: " + platformId);
        }
        return Component.literal("Наклейка станции");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("platformId", platformId);
        nbt.putString("customName", customName);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        platformId = nbt.getLong("platformId");
        customName = nbt.getString("customName");
    }
}
