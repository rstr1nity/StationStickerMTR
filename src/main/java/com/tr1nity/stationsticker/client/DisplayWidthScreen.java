package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import com.tr1nity.stationsticker.block.*;
import net.minecraft.world.level.block.state.BlockState;
import com.tr1nity.stationsticker.network.DisplayMountPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// Общие настройки экрана: ширина, высота подвеса, крепление и сигнал прибытия.
public class DisplayWidthScreen extends Screen {

    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;

    private final BlockPos pos;
    private int signWidth;
    private final BlockState state;
    private int drop;
    private boolean side;
    private boolean sound;

    public DisplayWidthScreen(BlockPos pos, BlockState state) {
        super(Component.translatable(state.getBlock().getDescriptionId()));
        this.pos = pos;
        this.state = state;
        this.signWidth = ((WideDisplayBlock) state.getBlock()).width(state);
        this.drop = state.getValue(WideDisplayBlock.DROP);
        this.side = state.getValue(WideDisplayBlock.ATTACHMENT) == DisplayAttachment.SIDE;
        this.sound = state.hasProperty(ArrivalDisplayBlock.SOUND) && state.getValue(ArrivalDisplayBlock.SOUND);
    }

    @Override
    protected void init() {
        int x = (width - WIDGET_WIDTH) / 2;
        int y = height / 2 - 40;
        if (state.getBlock() instanceof ResizableDisplayBlock) {
            addRenderableWidget(new WidthSlider(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, signWidth, value -> signWidth = value));
            y += 24;
        }
        if (state.getValue(WideDisplayBlock.ATTACHMENT) == DisplayAttachment.CEILING) {
            addRenderableWidget(new DropSlider(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, drop, value -> drop = value));
            y += 24;
        } else if (state.getBlock() instanceof ArrivalDisplayBlock) {
            addRenderableWidget(CycleButton.<Boolean>builder(value -> Component.translatable("gui.stationsticker.display.mount." + (value ? "side" : "wall")))
                    .withValues(false, true).withInitialValue(side)
                    .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.translatable("gui.stationsticker.display.mount"), (button, value) -> side = value));
            y += 24;
        }
        if (state.getBlock() instanceof ArrivalDisplayBlock) {
            addRenderableWidget(CycleButton.onOffBuilder(sound).create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                    Component.translatable("gui.stationsticker.display.sound"), (button, value) -> sound = value));
            y += 24;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
                .bounds(x, y + 8, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new DisplayMountPacket(pos, signWidth, drop, side, sound));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 64, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
