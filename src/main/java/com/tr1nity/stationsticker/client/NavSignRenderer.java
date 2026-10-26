package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import com.tr1nity.stationsticker.client.display.ExitSignBoard;
import com.tr1nity.stationsticker.client.display.SlideCanvas;
import com.tr1nity.stationsticker.client.display.TransferSignBoard;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

// Навигационная табличка: белый световой короб, раскладка по выбранному шаблону
public class NavSignRenderer extends WideDisplayRenderer<NavSignBlockEntity> {

    public NavSignRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected int screenColor() {
        return 0xFFF4F4F2;
    }

    @Override
    protected float designHeight(NavSignBlockEntity entity) {
        return switch (entity.getTemplate()) {
            case TRANSFER -> TransferSignBoard.DESIGN_HEIGHT;
            case EXIT -> ExitSignBoard.DESIGN_HEIGHT;
        };
    }

    @Override
    protected void drawContent(NavSignBlockEntity entity, SlideCanvas canvas, boolean back) {
        switch (entity.getTemplate()) {
            case TRANSFER -> TransferSignBoard.render(canvas, entity, back);
            case EXIT -> ExitSignBoard.render(canvas, entity, back);
        }
    }
}
