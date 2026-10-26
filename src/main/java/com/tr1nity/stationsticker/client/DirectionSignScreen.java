package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.network.DisplayWidthPacket;
import com.tr1nity.stationsticker.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// Окно указателя направлений: ширина в клетках ползунком
public class DirectionSignScreen extends Screen {

    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;

    private final BlockPos pos;
    private int signWidth;

    public DirectionSignScreen(BlockPos pos, int signWidth) {
        super(Component.translatable("gui.stationsticker.direction_sign.title"));
        this.pos = pos;
        this.signWidth = signWidth;
    }

    @Override
    protected void init() {
        int x = (width - WIDGET_WIDTH) / 2;
        int y = height / 2 - WIDGET_HEIGHT;
        addRenderableWidget(new WidthSlider(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, signWidth, value -> signWidth = value));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
                .bounds(x, y + WIDGET_HEIGHT + 8, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build());
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new DisplayWidthPacket(pos, signWidth));
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
}
