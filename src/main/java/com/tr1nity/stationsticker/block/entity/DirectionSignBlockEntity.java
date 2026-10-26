package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.registration.ModBlockEntities;
import com.tr1nity.stationsticker.block.WideDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Данных нет: направления определяются рендерером по позиции блока
public class DirectionSignBlockEntity extends BlockEntity {

    public DirectionSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIRECTION_SIGN.get(), pos, state);
    }

    // Экран рисуется и в соседних клетках (частях), иначе его отсекает по клетке основного блока
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(WideDisplayBlock.MAX_WIDTH);
    }
}
