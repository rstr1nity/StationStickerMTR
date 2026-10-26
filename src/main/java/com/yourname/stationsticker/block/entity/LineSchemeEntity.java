package com.yourname.stationsticker.block.entity;

import com.yourname.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Данных нет: платформа определяется рендерером по позиции, направление стрелки хранится в блокстейте
public class LineSchemeEntity extends BlockEntity {

    public LineSchemeEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINE_SCHEME.get(), pos, state);
    }
}
