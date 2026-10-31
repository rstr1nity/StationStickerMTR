package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.network.DisplayWidthPacket;
import com.tr1nity.stationsticker.network.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// Окно ширины экрана (ResizableDisplayBlock): ползунок в клетках, заголовок - название блока
public class DisplayWidthScreen extends Screen {

    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;

    private final BlockPos pos;
    private int signWidth;

    public DisplayWidthScreen(BlockPos pos, int signWidth, Component title) {
        super(title);
        this.pos = pos;
        this.signWidth = signWidth;
    }

    @Override
    protected void init() {
        int x = (width - WIDGET_WIDTH) / 2;
        int y = height / 2 - WIDGET_HEIGHT;
        addRenderableWidget(new WidthSlider(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, signWidth, value -> signWidth = value));
        addRenderableWidget(new Button(x, y + WIDGET_HEIGHT + 8, WIDGET_WIDTH, WIDGET_HEIGHT, CommonComponents.GUI_DONE, button -> save()));
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new DisplayWidthPacket(pos, signWidth));
        onClose();
    }

    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawCenteredString(graphics, font, title, width / 2, height / 2 - WIDGET_HEIGHT - 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
