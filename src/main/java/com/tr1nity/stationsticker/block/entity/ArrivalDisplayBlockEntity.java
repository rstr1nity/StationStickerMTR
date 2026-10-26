package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Данных пока нет: платформа определяется рендерером по позиции блока
public class ArrivalDisplayBlockEntity extends BlockEntity {

    public ArrivalDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARRIVAL_DISPLAY.get(), pos, state);
    }

    // Экран рисуется и в соседней клетке (части), иначе его отсекает по клетке основного блока
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }
}
