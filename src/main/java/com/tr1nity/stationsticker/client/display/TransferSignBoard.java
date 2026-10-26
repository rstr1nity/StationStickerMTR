package com.tr1nity.stationsticker.client.display;

import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.client.resources.language.I18n;

// Шаблон «Переход на линию» в стиле навигации Московского метро:
//   <-  [человек на лестнице]  (2)  Переход на Замоскворецкую линию
//                                   Тверская
//                                   Tverskaya
// При стрелке вправо или вверх порядок зеркальный: текст слева, стрелка у правого края.
public final class TransferSignBoard {

    public static final float DESIGN_HEIGHT = 40;

    private static final float CENTER_Y = DESIGN_HEIGHT / 2;
    private static final float ARROW_SCALE = 2.2f;
    private static final float PICTOGRAM_SCALE = 1.6f;
    private static final float CIRCLE_RADIUS = 9;
    private static final float GAP = 4;
    private static final float HEADER_Y = 3;
    private static final float HEADER_SCALE = 0.8f;
    private static final float NAME_Y = 12;
    private static final float NAME_SCALE = 1.8f;
    private static final float SECONDARY_Y = 29;
    private static final float SECONDARY_SCALE = 0.9f;

    private static final int TEXT_COLOR = 0xFF1A1A1A;
    private static final int SECONDARY_COLOR = 0xFF5A5A5A;
    private static final int NUMBER_COLOR = 0xFFFFFFFF;
    private static final String KEY = "display.stationsticker.nav_sign.";

    // Стрелка влево 8x7; остальные направления получаются поворотом
    private static final String[] ARROW_LEFT = {
            "...#....",
            "..##....",
            ".#######",
            "########",
            ".#######",
            "..##....",
            "...#...."};

    // Человек идёт вправо 12x7; вариант с лестницей добавляет ступени снизу справа
    private static final String[] PERSON = {
            "...##.......",
            "...##.......",
            "..####......",
            ".#.##.#.....",
            "...##.......",
            "..#..#......",
            ".#....#....."};

    private static final String[] STAIRS = {
            "...##.......",
            "...##.......",
            "..####......",
            ".#.##.#.....",
            "...##.......",
            "..#..#......",
            ".#....#.....",
            "..........##",
            "........####",
            "......######",
            "....########"};

    private TransferSignBoard() {
    }

    // back - обратная сторона подвесной таблички: для её зрителя лево и право поменяны
    public static void render(SlideCanvas canvas, NavSignBlockEntity sign, boolean back) {
        Transfers.Transfer transfer = Transfers.resolve(sign.getTransferStationId(), sign.getTransferColor());
        if (transfer == null) {
            String hint = I18n.get(KEY + "setup");
            canvas.text(hint, (canvas.width - canvas.textWidth(hint, 1)) / 2, CENTER_Y - 4, 1, SECONDARY_COLOR);
            return;
        }

        Arrow arrow = sign.getArrow();
        if (back && arrow == Arrow.LEFT) arrow = Arrow.RIGHT;
        else if (back && arrow == Arrow.RIGHT) arrow = Arrow.LEFT;
        boolean mirrored = arrow == Arrow.RIGHT || arrow == Arrow.UP;

        String[] arrowBitmap = arrowBitmap(arrow);
        String[] pictogram = switch (sign.getPictogram()) {
            case NONE -> null;
            case PERSON -> PERSON;
            case STAIRS -> STAIRS;
        };

        // Элементы от края со стрелкой к тексту: стрелка, пиктограмма, кружок
        float arrowWidth = arrowBitmap[0].length() * ARROW_SCALE;
        float pictogramWidth = pictogram == null ? 0 : pictogram[0].length() * PICTOGRAM_SCALE;
        float cursor = 0;
        drawAt(canvas, mirrored, cursor, arrowWidth, x ->
                drawBitmap(canvas, arrowBitmap, x, CENTER_Y - arrowBitmap.length * ARROW_SCALE / 2, ARROW_SCALE, TEXT_COLOR, false));
        cursor += arrowWidth + GAP;
        if (pictogram != null) {
            // Человек идёт в сторону стрелки
            drawAt(canvas, mirrored, cursor, pictogramWidth, x ->
                    drawBitmap(canvas, pictogram, x, CENTER_Y - pictogram.length * PICTOGRAM_SCALE / 2, PICTOGRAM_SCALE, TEXT_COLOR, !mirrored));
            cursor += pictogramWidth + GAP;
        }
        drawAt(canvas, mirrored, cursor, CIRCLE_RADIUS * 2, x -> drawLineCircle(canvas, x + CIRCLE_RADIUS, transfer));
        cursor += CIRCLE_RADIUS * 2 + GAP;

        // Текст: «Переход на <линию>» (название линии её цветом), станция крупно, второе название мелко
        float maxWidth = canvas.width - cursor;
        String header = I18n.get(KEY + "transfer_to") + " ";
        String line = StationNames.forCurrentLanguage(transfer.routeName());
        String name = StationNames.forCurrentLanguage(transfer.stationName());
        String secondary = StationNames.secondary(transfer.stationName());
        float headerWidth = canvas.textWidth(header, HEADER_SCALE);
        if (!mirrored) {
            canvas.text(header, cursor, HEADER_Y, HEADER_SCALE, TEXT_COLOR, maxWidth);
            canvas.text(line, cursor + headerWidth, HEADER_Y, HEADER_SCALE, 0xFF000000 | transfer.color(), maxWidth - headerWidth);
            canvas.text(name, cursor, NAME_Y, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.text(secondary, cursor, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        } else {
            float right = canvas.width - cursor;
            float lineWidth = Math.min(canvas.textWidth(line, HEADER_SCALE), maxWidth - headerWidth);
            canvas.textRight(line, right, HEADER_Y, HEADER_SCALE, 0xFF000000 | transfer.color(), maxWidth - headerWidth);
            canvas.textRight(header, right - lineWidth, HEADER_Y, HEADER_SCALE, TEXT_COLOR, headerWidth);
            canvas.textRight(name, right, NAME_Y, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.textRight(secondary, right, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        }
    }

    private interface Drawer {
        void draw(float x);
    }

    // Элемент шириной width на расстоянии offset от края со стрелкой (левого или правого)
    private static void drawAt(SlideCanvas canvas, boolean fromRight, float offset, float width, Drawer drawer) {
        drawer.draw(fromRight ? canvas.width - offset - width : offset);
    }

    private static void drawLineCircle(SlideCanvas canvas, float centerX, Transfers.Transfer transfer) {
        canvas.circle(centerX, CENTER_Y, CIRCLE_RADIUS, 0xFF000000 | transfer.color());
        String number = transfer.routeNumber();
        if (!number.isEmpty()) {
            float scale = Math.min(CIRCLE_RADIUS / 5, CIRCLE_RADIUS * 1.4f / canvas.boldTextWidth(number, 1));
            canvas.boldText(number, centerX - canvas.boldTextWidth(number, scale) / 2, CENTER_Y - 3.5f * scale, scale, NUMBER_COLOR);
        }
    }

    private static String[] arrowBitmap(Arrow arrow) {
        return switch (arrow) {
            case LEFT -> ARROW_LEFT;
            case RIGHT -> mirror(ARROW_LEFT);
            case UP -> transpose(ARROW_LEFT);
            case DOWN -> flipVertical(transpose(ARROW_LEFT));
        };
    }

    private static void drawBitmap(SlideCanvas canvas, String[] rows, float x, float y, float scale, int color, boolean mirror) {
        for (int row = 0; row < rows.length; row++) {
            String line = mirror ? new StringBuilder(rows[row]).reverse().toString() : rows[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    // Чуть больше шага, чтобы между «пикселями» не было щелей
                    canvas.rect(x + col * scale, y + row * scale, scale * 1.02f, scale * 1.02f, color);
                }
            }
        }
    }

    private static String[] mirror(String[] rows) {
        String[] result = new String[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = new StringBuilder(rows[i]).reverse().toString();
        return result;
    }

    private static String[] flipVertical(String[] rows) {
        String[] result = new String[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = rows[rows.length - 1 - i];
        return result;
    }

    private static String[] transpose(String[] rows) {
        String[] result = new String[rows[0].length()];
        for (int col = 0; col < rows[0].length(); col++) {
            StringBuilder builder = new StringBuilder();
            for (String row : rows) builder.append(row.charAt(col));
            result[col] = builder.toString();
        }
        return result;
    }
}
