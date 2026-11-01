package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.WideDisplayBlock;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;

public class DropSlider extends AbstractSliderButton {
    private final IntConsumer onChange;
    private int drop;

    public DropSlider(int x, int y, int width, int height, int drop, IntConsumer onChange) {
        super(x, y, width, height, Component.empty(), (double) drop / WideDisplayBlock.MAX_DROP);
        this.drop = drop;
        this.onChange = onChange;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("gui.stationsticker.display.drop", String.format(java.util.Locale.ROOT, "%.2f", drop / 4f)));
    }

    @Override
    protected void applyValue() {
        drop = (int) Math.round(value * WideDisplayBlock.MAX_DROP);
        updateMessage();
        onChange.accept(drop);
    }
}
