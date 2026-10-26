package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.block.WideDisplayBlock;
import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Данных нет: погода и станция берутся рендерером из мира и MTR
public class InfoDisplayBlockEntity extends BlockEntity {

    public InfoDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFO_DISPLAY.get(), pos, state);
    }

    // Экран рисуется и в соседних клетках (частях), иначе его отсекает по клетке основного блока
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(WideDisplayBlock.MAX_WIDTH);
    }
}
