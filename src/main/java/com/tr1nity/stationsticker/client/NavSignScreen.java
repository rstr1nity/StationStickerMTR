package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.NavSignOptions.Pictogram;
import com.tr1nity.stationsticker.block.NavSignOptions.Template;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import com.tr1nity.stationsticker.client.display.StationNames;
import com.tr1nity.stationsticker.client.display.Transfers;
import com.tr1nity.stationsticker.network.ModNetwork;
import com.tr1nity.stationsticker.network.NavSignSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

// Окно настройки навигационной таблички: шаблон и его параметры
public class NavSignScreen extends Screen {

    private static final String KEY = "gui.stationsticker.nav_sign.";
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW = 24;

    private final NavSignBlockEntity sign;
    private final List<Transfers.Transfer> transfers;
    private Template template;
    private Arrow arrow;
    private Pictogram pictogram;
    private Transfers.Transfer transfer;

    public NavSignScreen(NavSignBlockEntity sign) {
        super(Component.translatable(KEY + "title"));
        this.sign = sign;
        this.transfers = Transfers.optionsAt(sign.getBlockPos());
        this.template = sign.getTemplate();
        this.arrow = sign.getArrow();
        this.pictogram = sign.getPictogram();
        // Текущая пересадка, если она ещё есть на станции, иначе первая из списка
        this.transfer = transfers.stream()
                .filter(t -> t.stationId() == sign.getTransferStationId() && t.color() == sign.getTransferColor())
                .findFirst()
                .orElse(transfers.isEmpty() ? null : transfers.get(0));
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2;
        int y = height / 2 - ROW * 2;

        addRenderableWidget(CycleButton.<Template>builder(value -> option("template", value))
                .withValues(Template.values())
                .withInitialValue(template)
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "template"), (button, value) -> template = value));

        if (transfers.isEmpty()) {
            Button empty = Button.builder(Component.translatable(KEY + "no_transfers"), button -> {
            }).bounds(x, y + ROW, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            empty.active = false;
            addRenderableWidget(empty);
        } else {
            addRenderableWidget(CycleButton.<Transfers.Transfer>builder(NavSignScreen::describe)
                    .withValues(transfers)
                    .withInitialValue(transfer)
                    .create(x, y + ROW, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "transfer"), (button, value) -> transfer = value));
        }

        addRenderableWidget(CycleButton.<Arrow>builder(value -> option("arrow", value))
                .withValues(Arrow.values())
                .withInitialValue(arrow)
                .create(x, y + ROW * 2, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "arrow"), (button, value) -> arrow = value));

        addRenderableWidget(CycleButton.<Pictogram>builder(value -> option("pictogram", value))
                .withValues(Pictogram.values())
                .withInitialValue(pictogram)
                .create(x, y + ROW * 3, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "pictogram"), (button, value) -> pictogram = value));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
                .bounds(x, y + ROW * 4 + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void save() {
        long stationId = transfer == null ? 0 : transfer.stationId();
        int color = transfer == null ? 0 : transfer.color();
        ModNetwork.CHANNEL.sendToServer(new NavSignSettingsPacket(sign.getBlockPos(), template, arrow, pictogram, stationId, color));
        onClose();
    }

    // Название варианта из файла языка, например gui.stationsticker.nav_sign.arrow.left
    private static Component option(String group, Enum<?> value) {
        return Component.translatable(KEY + group + "." + value.name().toLowerCase(Locale.ROOT));
    }

    private static Component describe(Transfers.Transfer transfer) {
        String number = transfer.routeNumber().isEmpty() ? "" : "(" + transfer.routeNumber() + ") ";
        return Component.literal(number + StationNames.forCurrentLanguage(transfer.stationName())
                + " — " + StationNames.forCurrentLanguage(transfer.routeName()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - ROW * 2 - 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
