package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.WideDisplayBlock;
import com.tr1nity.stationsticker.block.entity.DirectionSignBlockEntity;
import com.tr1nity.stationsticker.client.display.DirectionSignBoard;
import com.tr1nity.stationsticker.client.display.SlideCanvas;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

// Указатель направлений: белый световой короб, раскладка DirectionSignBoard
public class DirectionSignRenderer extends WideDisplayRenderer<DirectionSignBlockEntity> {

    public DirectionSignRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected int screenColor() {
        return 0xFFF4F4F2;
    }

    @Override
    protected float designHeight() {
        return DirectionSignBoard.DESIGN_HEIGHT;
    }

    @Override
    protected void drawContent(DirectionSignBlockEntity entity, SlideCanvas canvas, boolean back) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof WideDisplayBlock block)) return;
        Direction partAxis = WideDisplayBlock.partDirection(state.getValue(WideDisplayBlock.FACING));
        Direction rightAxis = back ? partAxis.getOpposite() : partAxis;
        DirectionSignBoard.render(canvas, entity.getBlockPos(), partAxis, block.width(), rightAxis);
    }
}
