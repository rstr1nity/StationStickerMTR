package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Данных нет: станция определяется рендерером по позиции блока
public class StationStickerBlockEntity extends BlockEntity {

    public StationStickerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATION_STICKER.get(), pos, state);
    }
}
