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
    private static final float PICTOGRAM_HEIGHT = 24;
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
            case PERSON -> Pictograms.PERSON;
            case STAIRS -> Pictograms.STAIRS;
        };

        // Элементы от края со стрелкой к тексту: стрелка, пиктограмма, кружок
        float arrowWidth = arrowBitmap[0].length() * ARROW_SCALE;
        float pictogramScale = pictogram == null ? 0 : PICTOGRAM_HEIGHT / pictogram.length;
        float pictogramWidth = pictogram == null ? 0 : pictogram[0].length() * pictogramScale;
        float cursor = 0;
        drawAt(canvas, mirrored, cursor, arrowWidth, x ->
                Pictograms.draw(canvas, arrowBitmap, x, CENTER_Y - arrowBitmap.length * ARROW_SCALE / 2, ARROW_SCALE, TEXT_COLOR, false));
        cursor += arrowWidth + GAP;
        if (pictogram != null) {
            // Человек идёт в сторону стрелки
            drawAt(canvas, mirrored, cursor, pictogramWidth, x ->
                    Pictograms.draw(canvas, pictogram, x, CENTER_Y - PICTOGRAM_HEIGHT / 2, pictogramScale, TEXT_COLOR, !mirrored));
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
            case LEFT -> Pictograms.ARROW_LEFT;
            case RIGHT -> Pictograms.mirror(Pictograms.ARROW_LEFT);
            case UP -> Pictograms.transpose(Pictograms.ARROW_LEFT);
            case DOWN -> Pictograms.flipVertical(Pictograms.transpose(Pictograms.ARROW_LEFT));
        };
    }
}
