package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.entity.ArrivalDisplayBlockEntity;
import com.tr1nity.stationsticker.client.display.ArrivalBoard;
import com.tr1nity.stationsticker.client.display.SlideCanvas;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.mtr.core.data.Platform;

// Табло прибытия: чёрный экран, раскладка ArrivalBoard
public class ArrivalDisplayRenderer extends WideDisplayRenderer<ArrivalDisplayBlockEntity> {

    public ArrivalDisplayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected int screenColor() {
        return ArrivalBoard.SCREEN_COLOR;
    }

    @Override
    protected float designHeight() {
        return ArrivalBoard.DESIGN_HEIGHT;
    }

    @Override
    protected void drawContent(ArrivalDisplayBlockEntity entity, SlideCanvas canvas, boolean back) {
        Platform platform = PlatformLocator.findPlatformAt(entity.getBlockPos());
        long platformId = platform == null ? 0 : platform.getId();
        long dayTime = entity.getLevel() == null ? 0 : entity.getLevel().getDayTime();
        ArrivalBoard.render(canvas, platformId, dayTime);
    }
}
