package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.ArrivalDisplayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// Табло прибытия 2x1
public class ArrivalDisplayBlock extends WideDisplayBlock {

    @Override
    public int width() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArrivalDisplayBlockEntity(pos, state);
    }
}
