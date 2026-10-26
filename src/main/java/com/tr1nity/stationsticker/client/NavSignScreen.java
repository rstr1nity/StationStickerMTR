package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.NavSignOptions.Pictogram;
import com.tr1nity.stationsticker.block.NavSignOptions.Template;
import com.tr1nity.stationsticker.block.NavSignSettings;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import com.tr1nity.stationsticker.client.display.Exits;
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
import org.mtr.core.data.StationExit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Окно настройки навигационной таблички: шаблон и его параметры. Набор кнопок зависит от шаблона.
public class NavSignScreen extends Screen {

    private static final String KEY = "gui.stationsticker.nav_sign.";
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW = 24;

    private final NavSignBlockEntity sign;
    private final List<Transfers.Transfer> transfers;
    private final List<String> exitNames = new ArrayList<>();
    private NavSignSettings settings;

    public NavSignScreen(NavSignBlockEntity sign) {
        super(Component.translatable(KEY + "title"));
        this.sign = sign;
        this.transfers = Transfers.optionsAt(sign.getBlockPos());
        this.settings = sign.getSettings();
        exitNames.add("");
        for (StationExit exit : Exits.at(sign.getBlockPos())) exitNames.add(exit.getName());

        // Текущая пересадка, если она ещё есть на станции, иначе первая из списка
        Transfers.Transfer transfer = transfers.stream()
                .filter(t -> t.stationId() == settings.transferStationId() && t.color() == settings.transferColor())
                .findFirst()
                .orElse(transfers.isEmpty() ? null : transfers.get(0));
        settings = transfer == null ? settings.withTransfer(0, 0) : settings.withTransfer(transfer.stationId(), transfer.color());
    }

    NavSignBlockEntity sign() {
        return sign;
    }

    NavSignSettings settings() {
        return settings;
    }

    void setSettings(NavSignSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2;
        int y = height / 2 - ROW * 2;

        addRenderableWidget(CycleButton.<Template>builder(value -> option("template", value))
                .withValues(Template.values())
                .withInitialValue(settings.template())
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "template"), (button, value) -> {
                    settings = settings.withTemplate(value);
                    rebuildWidgets();
                }));

        int row = 1;
        if (settings.template() == Template.TRANSFER) {
            if (transfers.isEmpty()) {
                addDisabled(x, y + ROW * row++, Component.translatable(KEY + "no_transfers"));
            } else {
                Transfers.Transfer current = transfers.stream()
                        .filter(t -> t.stationId() == settings.transferStationId() && t.color() == settings.transferColor())
                        .findFirst().orElse(transfers.get(0));
                addRenderableWidget(CycleButton.<Transfers.Transfer>builder(NavSignScreen::describe)
                        .withValues(transfers)
                        .withInitialValue(current)
                        .create(x, y + ROW * row++, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "transfer"),
                                (button, value) -> settings = settings.withTransfer(value.stationId(), value.color())));
            }
            addRenderableWidget(CycleButton.<Arrow>builder(value -> option("arrow", value))
                    .withValues(Arrow.values())
                    .withInitialValue(settings.arrow())
                    .create(x, y + ROW * row++, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "arrow"),
                            (button, value) -> settings = settings.withArrow(value)));
            addRenderableWidget(CycleButton.<Pictogram>builder(value -> option("pictogram", value))
                    .withValues(Pictogram.values())
                    .withInitialValue(settings.pictogram())
                    .create(x, y + ROW * row++, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(KEY + "pictogram"),
                            (button, value) -> settings = settings.withPictogram(value)));
        } else {
            if (exitNames.size() == 1) {
                addDisabled(x, y + ROW * row++, Component.translatable(KEY + "no_exits"));
            } else {
                row = addExitRow(x, y, row, false);
                row = addExitRow(x, y, row, true);
            }
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save())
                .bounds(x, y + ROW * row + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    // Выход для половины таблички и кнопка его значков
    private int addExitRow(int x, int y, int row, boolean right) {
        String side = right ? "right" : "left";
        String current = right ? settings.exitRight() : settings.exitLeft();
        int iconsWidth = 90;
        addRenderableWidget(CycleButton.<String>builder(name -> name.isEmpty()
                        ? Component.translatable(KEY + "exit.none") : Component.literal(name))
                .withValues(exitNames)
                .withInitialValue(exitNames.contains(current) ? current : "")
                .create(x, y + ROW * row, BUTTON_WIDTH - iconsWidth - 4, BUTTON_HEIGHT, Component.translatable(KEY + "exit." + side),
                        (button, value) -> settings = settings.withExit(right, value)));
        addRenderableWidget(Button.builder(Component.translatable(KEY + "icons"),
                        button -> minecraft.setScreen(new ExitIconsScreen(this, right)))
                .bounds(x + BUTTON_WIDTH - iconsWidth, y + ROW * row, iconsWidth, BUTTON_HEIGHT)
                .build());
        return row + 1;
    }

    private void addDisabled(int x, int y, Component text) {
        Button button = Button.builder(text, b -> {
        }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        button.active = false;
        addRenderableWidget(button);
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new NavSignSettingsPacket(sign.getBlockPos(), settings));
        onClose();
    }

    // Название варианта из файла языка, например gui.stationsticker.nav_sign.arrow.left
    private static Component option(String group, Enum<?> value) {
        return Component.translatable(KEY + group + "." + value.name().toLowerCase(Locale.ROOT));
    }

    private static Component describe(Transfers.Transfer transfer) {
        String number = transfer.routeNumber().isEmpty() ? "" : "(" + transfer.routeNumber() + ") ";
        return Component.literal(number + StationNames.primary(transfer.stationName())
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
