package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.DirectionSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// Указатель направлений 3x1
public class DirectionSignBlock extends WideDisplayBlock {

    @Override
    public int width() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DirectionSignBlockEntity(pos, state);
    }
}
