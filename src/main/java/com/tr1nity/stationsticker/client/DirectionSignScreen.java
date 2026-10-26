package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.DirectionSignBlock;
import com.tr1nity.stationsticker.block.WideDisplayBlock;
import com.tr1nity.stationsticker.network.DirectionSignWidthPacket;
import com.tr1nity.stationsticker.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// Окно указателя направлений: ширина в клетках ползунком
public class DirectionSignScreen extends Screen {

    private static final String KEY = "gui.stationsticker.direction_sign.";
    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int STEPS = WideDisplayBlock.MAX_WIDTH - DirectionSignBlock.MIN_WIDTH;

    private final BlockPos pos;
    private int signWidth;

    public DirectionSignScreen(BlockPos pos, int signWidth) {
        super(Component.translatable(KEY + "title"));
        this.pos = pos;
        this.signWidth = signWidth;
    }

    @Override
    protected void init() {
        int x = (width - WIDGET_WIDTH) / 2;
        int y = height / 2 - WIDGET_HEIGHT;
        addRenderableWidget(new WidthSlider(x, y, (double) (signWidth - DirectionSignBlock.MIN_WIDTH) / STEPS));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
                .bounds(x, y + WIDGET_HEIGHT + 8, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new DirectionSignWidthPacket(pos, signWidth));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - WIDGET_HEIGHT - 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Ползунок по целым клеткам: значение 0..1 переводится в ширину MIN_WIDTH..MAX_WIDTH
    private class WidthSlider extends AbstractSliderButton {

        WidthSlider(int x, int y, double value) {
            super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty(), value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(KEY + "width", signWidth));
        }

        @Override
        protected void applyValue() {
            signWidth = DirectionSignBlock.MIN_WIDTH + (int) Math.round(value * STEPS);
        }
    }
}
