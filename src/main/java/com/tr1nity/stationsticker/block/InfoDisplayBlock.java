package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.InfoDisplayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// Информационный экран со сменой слайдов (погода, станция); ширина меняется в окне (клик кисточкой MTR)
public class InfoDisplayBlock extends ResizableDisplayBlock {

    public InfoDisplayBlock() {
        super(3);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfoDisplayBlockEntity(pos, state);
    }
}
