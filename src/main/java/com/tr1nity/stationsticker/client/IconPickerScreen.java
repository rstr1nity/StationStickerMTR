package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.client.display.MtrSigns;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Выбор значка из табличек MTR (встроенные и из ресурспаков/аддонов) сеткой, как в окне Railway Sign.
// Первая клетка - «без значка». Колесо мыши листает, наведение показывает id значка.
public class IconPickerScreen extends Screen {

    private static final int CELL = 24;
    private static final int ICON = 20;
    private static final int TOP = 32;

    private final Screen parent;
    private final String current;
    private final Consumer<String> onPick;
    private final List<String> ids = new ArrayList<>();
    private int scrollRows;

    public IconPickerScreen(Screen parent, String current, Consumer<String> onPick) {
        super(Component.translatable("gui.stationsticker.icon_picker.title"));
        this.parent = parent;
        this.current = current;
        this.onPick = onPick;
        ids.add("");
        ids.addAll(MtrSigns.ids());
    }

    private int columns() {
        return Math.max(1, (width - 40) / CELL);
    }

    private int visibleRows() {
        return Math.max(1, (height - TOP - 16) / CELL);
    }

    private int left() {
        return (width - columns() * CELL) / 2;
    }

    // Индекс значка под мышью или -1
    private int indexAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - left()) / CELL);
        int row = (int) Math.floor((mouseY - TOP) / CELL);
        if (column < 0 || column >= columns() || row < 0 || row >= visibleRows()) return -1;
        int index = (row + scrollRows) * columns() + column;
        return index < ids.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = indexAt(mouseX, mouseY);
        if (button == 0 && index >= 0) {
            onPick.accept(ids.get(index));
            minecraft.setScreen(parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalRows = (ids.size() + columns() - 1) / columns();
        scrollRows = Math.max(0, Math.min(totalRows - visibleRows(), scrollRows - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int columns = columns();
        for (int row = 0; row < visibleRows(); row++) {
            for (int column = 0; column < columns; column++) {
                int index = (row + scrollRows) * columns + column;
                if (index >= ids.size()) break;
                int x = left() + column * CELL;
                int y = TOP + row * CELL;
                String id = ids.get(index);
                if (id.equals(current)) graphics.fill(x, y, x + CELL, y + CELL, 0xFF3A7BD5);
                if (id.isEmpty()) {
                    graphics.drawCenteredString(font, "—", x + CELL / 2, y + 8, 0xFFFFFF);
                } else {
                    drawIcon(graphics, id, x + 2, y + 2, ICON);
                }
            }
        }
        int hovered = indexAt(mouseX, mouseY);
        if (hovered > 0) {
            graphics.renderTooltip(font, Component.literal(ids.get(hovered)), mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // Значок MTR на его фоне в окне; пустой id - пустая рамка
    static void drawIcon(GuiGraphics graphics, String id, int x, int y, int size) {
        MtrSigns.Sign sign = MtrSigns.get(id);
        if (sign == null) {
            graphics.renderOutline(x, y, size, size, 0xFF808080);
            return;
        }
        graphics.fill(x, y, x + size, y + size, sign.background());
        graphics.blit(sign.texture(), x, y, 0, 0, size, size, size, size);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
