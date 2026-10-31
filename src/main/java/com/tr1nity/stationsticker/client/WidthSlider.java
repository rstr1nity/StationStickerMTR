package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.ResizableDisplayBlock;
import com.tr1nity.stationsticker.block.WideDisplayBlock;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

// Ползунок ширины экрана по целым клеткам: значение 0..1 переводится в ширину MIN_WIDTH..MAX_WIDTH
public class WidthSlider extends AbstractSliderButton {

    private static final int STEPS = WideDisplayBlock.MAX_WIDTH - ResizableDisplayBlock.MIN_WIDTH;

    private final IntConsumer onChange;
    private int displayWidth;

    public WidthSlider(int x, int y, int width, int height, int displayWidth, IntConsumer onChange) {
        super(x, y, width, height, Component.empty(), (double) (displayWidth - ResizableDisplayBlock.MIN_WIDTH) / STEPS);
        this.displayWidth = displayWidth;
        this.onChange = onChange;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("gui.stationsticker.display.width", displayWidth));
    }

    @Override
    protected void applyValue() {
        displayWidth = ResizableDisplayBlock.MIN_WIDTH + (int) Math.round(value * STEPS);
        onChange.accept(displayWidth);
    }
}
