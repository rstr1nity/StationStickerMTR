package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.NavSignSettings;
import com.tr1nity.stationsticker.client.display.Exits;
import com.tr1nity.stationsticker.client.display.StationNames;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.mtr.core.data.StationExit;

import java.util.List;

// Значки к объектам выхода (направления выхода после первого - улицы) для одной половины таблички
public class ExitIconsScreen extends Screen {

    private static final String KEY = "gui.stationsticker.nav_sign.";
    private static final int ROW_WIDTH = 260;
    private static final int ROW_HEIGHT = 24;
    private static final int ICON_SIZE = 20;

    private final NavSignScreen parent;
    private final boolean right;

    public ExitIconsScreen(NavSignScreen parent, boolean right) {
        super(Component.translatable(KEY + (right ? "icons.right" : "icons.left")));
        this.parent = parent;
        this.right = right;
    }

    private List<String> destinations() {
        String exitName = right ? parent.settings().exitRight() : parent.settings().exitLeft();
        StationExit exit = Exits.find(parent.sign().getBlockPos(), exitName);
        return exit == null ? List.of() : exit.getDestinations();
    }

    private List<String> icons() {
        return right ? parent.settings().iconsRight() : parent.settings().iconsLeft();
    }

    @Override
    protected void init() {
        int x = (width - ROW_WIDTH) / 2;
        int y = height / 2 - ROW_HEIGHT * 2;
        List<String> destinations = destinations();
        for (int i = 0; i < NavSignSettings.EXIT_ITEMS; i++) {
            if (i + 1 >= destinations.size()) break; // первое направление - улица, у неё значка нет
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(StationNames.primary(destinations.get(i + 1))),
                            button -> minecraft.setScreen(new IconPickerScreen(this, icons().get(index), id -> {
                                parent.setSettings(parent.settings().withIcon(right, index, id));
                            })))
                    .bounds(x + ICON_SIZE + 6, y + ROW_HEIGHT * i, ROW_WIDTH - ICON_SIZE - 6, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> minecraft.setScreen(parent))
                .bounds(x, y + ROW_HEIGHT * NavSignSettings.EXIT_ITEMS + 8, ROW_WIDTH, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - ROW_HEIGHT * 2 - 20, 0xFFFFFF);
        int x = (width - ROW_WIDTH) / 2;
        int y = height / 2 - ROW_HEIGHT * 2;
        List<String> destinations = destinations();
        if (destinations.size() < 2) {
            graphics.drawCenteredString(font, Component.translatable(KEY + "no_exit_items"), width / 2, y + 6, 0xAAAAAA);
        }
        for (int i = 0; i < NavSignSettings.EXIT_ITEMS && i + 1 < destinations.size(); i++) {
            IconPickerScreen.drawIcon(graphics, icons().get(i), x, y + ROW_HEIGHT * i, ICON_SIZE);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
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
