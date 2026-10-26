package com.tr1nity.stationsticker.client.display;

import com.tr1nity.stationsticker.block.NavSignSettings;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.client.resources.language.I18n;
import org.mtr.core.data.StationExit;

import java.util.List;

// Шаблон «Выход в город» в стиле навигации Московского метро:
//   <- [1-4] Волгоградский проспект          |   Люблинская улица [5-8] ->
//            Volgogradsky Prospekt           |   Lyublinskaya Ulitsa
//        [зн] Спортивный комплекс «Москвич»  |   Платформа Текстильщики [зн]
//        [зн] Кинотеатр «Молодёжный»         |   Технополис «Москва»    [зн]
// Выходы и их направления - из станции MTR: первое направление - улица, остальные - объекты.
// Если выход выбран только с одной стороны, он занимает всю табличку.
public final class ExitSignBoard {

    public static final float DESIGN_HEIGHT = 44;

    private static final float ARROW_SCALE = 1.0f;     // стрелка 13x10 клеток
    private static final float NAME_SCALE = 1.3f;
    private static final float SECONDARY_Y = 10.5f;
    private static final float SECONDARY_SCALE = 0.65f;
    private static final float BOX_HEIGHT = 11;
    private static final float BOX_SCALE = 1.1f;
    private static final float LIST_Y = 18.5f;
    private static final float LIST_ROW = 8.4f;
    private static final float ICON_SIZE = 7;
    private static final float ITEM_SCALE = 0.7f;
    private static final float ITEM_SECONDARY_SCALE = 0.45f;
    private static final float GAP = 3;

    private static final int TEXT_COLOR = 0xFF1A1A1A;
    private static final int SECONDARY_COLOR = 0xFF5A5A5A;
    private static final int DIVIDER_COLOR = 0xFFB8B8B8;
    private static final int BOX_COLOR = 0xFF1A1A1A;
    private static final int BOX_TEXT_COLOR = 0xFFF5C400;
    private static final String KEY = "display.stationsticker.nav_sign.";

    private ExitSignBoard() {
    }

    // back - обратная сторона подвесной таблички: для её зрителя лево и право поменяны
    public static void render(SlideCanvas canvas, NavSignBlockEntity sign, boolean back) {
        NavSignSettings settings = sign.getSettings();
        StationExit left = Exits.find(sign.getBlockPos(), back ? settings.exitRight() : settings.exitLeft());
        StationExit right = Exits.find(sign.getBlockPos(), back ? settings.exitLeft() : settings.exitRight());
        List<String> leftIcons = back ? settings.iconsRight() : settings.iconsLeft();
        List<String> rightIcons = back ? settings.iconsLeft() : settings.iconsRight();

        if (left == null && right == null) {
            String hint = I18n.get(KEY + "setup");
            canvas.text(hint, (canvas.width - canvas.textWidth(hint, 1)) / 2, DESIGN_HEIGHT / 2 - 4, 1, SECONDARY_COLOR);
            return;
        }
        if (left != null && right != null) {
            float half = canvas.width / 2;
            canvas.rect(half - 0.3f, 1, 0.6f, DESIGN_HEIGHT - 2, DIVIDER_COLOR);
            drawSide(canvas, left, leftIcons, 0, half - GAP * 2, false);
            drawSide(canvas, right, rightIcons, half + GAP * 2, canvas.width, true);
        } else if (left != null) {
            drawSide(canvas, left, leftIcons, 0, canvas.width, false);
        } else {
            drawSide(canvas, right, rightIcons, 0, canvas.width, true);
        }
    }

    // Половина таблички от x0 до x1; rightSide - стрелка у правого края, текст прижат вправо
    private static void drawSide(SlideCanvas canvas, StationExit exit, List<String> icons, float x0, float x1, boolean rightSide) {
        List<String> destinations = exit.getDestinations();
        float headingCenter = NAME_SCALE * 7 / 2 + 0.5f;

        // Стрелка и плашка выхода
        String[] arrow = rightSide ? Pictograms.mirror(Pictograms.ARROW_LEFT) : Pictograms.ARROW_LEFT;
        float arrowWidth = arrow[0].length() * ARROW_SCALE;
        float arrowX = rightSide ? x1 - arrowWidth : x0;
        Pictograms.draw(canvas, arrow, arrowX, headingCenter - arrow.length * ARROW_SCALE / 2, ARROW_SCALE, TEXT_COLOR, false);

        String name = exit.getName();
        float boxWidth = canvas.boldTextWidth(name, BOX_SCALE) + 4;
        float boxX = rightSide ? arrowX - GAP - boxWidth : arrowX + arrowWidth + GAP;
        float boxY = headingCenter - BOX_HEIGHT / 2;
        canvas.rect(boxX, boxY, boxWidth, BOX_HEIGHT, BOX_COLOR);
        canvas.boldText(name, boxX + 2, headingCenter - 3.5f * BOX_SCALE, BOX_SCALE, BOX_TEXT_COLOR);

        // Улица: крупно и латиница под ней
        float textEdge = rightSide ? boxX - GAP : boxX + boxWidth + GAP;
        float maxWidth = rightSide ? textEdge - x0 : x1 - textEdge;
        if (!destinations.isEmpty()) {
            String street = destinations.get(0);
            text(canvas, StationNames.primary(street), textEdge, 0, NAME_SCALE, TEXT_COLOR, maxWidth, rightSide);
            text(canvas, StationNames.secondary(street), textEdge, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth, rightSide);
        }

        // Объекты рядом с выходом со значками MTR; значок со стороны стрелки, как на табличках метро
        float listEdge = rightSide ? x1 - arrowWidth - GAP : x0 + arrowWidth + GAP;
        for (int i = 0; i < NavSignSettings.EXIT_ITEMS && i + 1 < destinations.size(); i++) {
            String item = destinations.get(i + 1);
            float y = LIST_Y + i * LIST_ROW;
            String icon = icons.get(i);
            float iconWidth = icon.isEmpty() ? 0 : ICON_SIZE + 2;
            if (!icon.isEmpty()) {
                MtrSigns.draw(canvas, icon, rightSide ? listEdge - ICON_SIZE : listEdge, y - 0.5f, ICON_SIZE);
            }
            float itemEdge = rightSide ? listEdge - iconWidth : listEdge + iconWidth;
            float itemWidth = rightSide ? itemEdge - x0 : x1 - itemEdge;
            text(canvas, StationNames.primary(item), itemEdge, y, ITEM_SCALE, TEXT_COLOR, itemWidth, rightSide);
            text(canvas, StationNames.secondary(item), itemEdge, y + 5, ITEM_SECONDARY_SCALE, SECONDARY_COLOR, itemWidth, rightSide);
        }
    }

    private static void text(SlideCanvas canvas, String text, float edge, float y, float scale, int color, float maxWidth, boolean alignRight) {
        if (alignRight) {
            canvas.textRight(text, edge, y, scale, color, maxWidth);
        } else {
            canvas.text(text, edge, y, scale, color, maxWidth);
        }
    }
}
