package com.tr1nity.stationsticker.client.display;

import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.client.resources.language.I18n;

// Шаблон «Переход на линию» в стиле навигации Московского метро:
//   <-  [человек на лестнице]  (2)  Переход на Замоскворецкую линию
//                                   Тверская
//                                   Tverskaya
// При стрелке вправо или вверх порядок зеркальный: текст первым, стрелка последней. Вся группа по центру.
public final class TransferSignBoard {

    public static final float DESIGN_HEIGHT = 40;

    private static final float CENTER_Y = DESIGN_HEIGHT / 2;
    private static final float ARROW_SIZE = 20;      // длинная сторона стрелки
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
        float arrowScale = ARROW_SIZE / Math.max(arrowBitmap.length, arrowBitmap[0].length());
        float arrowWidth = arrowBitmap[0].length() * arrowScale;
        String[] pictogram = switch (sign.getPictogram()) {
            case NONE -> null;
            case PERSON -> Pictograms.PERSON;
            case STAIRS -> Pictograms.STAIRS;
        };
        float pictogramScale = pictogram == null ? 0 : PICTOGRAM_HEIGHT / pictogram.length;
        float pictogramWidth = pictogram == null ? 0 : pictogram[0].length() * pictogramScale + GAP;

        // Текст: «Переход на <линию>» (название линии её цветом), станция крупно, латиница мелко
        Text text = new Text(I18n.get(KEY + "transfer_to") + " ", StationNames.forCurrentLanguage(transfer.routeName()),
                StationNames.primary(transfer.stationName()), StationNames.secondary(transfer.stationName()));
        float fixedWidth = arrowWidth + GAP + pictogramWidth + CIRCLE_RADIUS * 2 + GAP;
        float textWidth = Math.min(text.width(canvas), canvas.width - fixedWidth);

        // Вся группа по центру таблички; человек идёт в сторону стрелки
        float x = (canvas.width - fixedWidth - textWidth) / 2;
        if (mirrored) {
            drawText(canvas, x, textWidth, text, transfer.color());
            x += textWidth + GAP;
            drawLineCircle(canvas, x + CIRCLE_RADIUS, transfer);
            x += CIRCLE_RADIUS * 2 + GAP;
            if (pictogram != null) Pictograms.draw(canvas, pictogram, x, CENTER_Y - PICTOGRAM_HEIGHT / 2, pictogramScale, TEXT_COLOR, false);
            x += pictogramWidth;
            Pictograms.draw(canvas, arrowBitmap, x, CENTER_Y - arrowBitmap.length * arrowScale / 2, arrowScale, TEXT_COLOR, false);
        } else {
            Pictograms.draw(canvas, arrowBitmap, x, CENTER_Y - arrowBitmap.length * arrowScale / 2, arrowScale, TEXT_COLOR, false);
            x += arrowWidth + GAP;
            if (pictogram != null) Pictograms.draw(canvas, pictogram, x, CENTER_Y - PICTOGRAM_HEIGHT / 2, pictogramScale, TEXT_COLOR, true);
            x += pictogramWidth;
            drawLineCircle(canvas, x + CIRCLE_RADIUS, transfer);
            x += CIRCLE_RADIUS * 2 + GAP;
            drawText(canvas, x, textWidth, text, transfer.color());
        }
    }

    private record Text(String header, String line, String name, String secondary) {
        float width(SlideCanvas canvas) {
            float headerWidth = canvas.textWidth(header, HEADER_SCALE) + canvas.textWidth(line, HEADER_SCALE);
            return Math.max(headerWidth, Math.max(canvas.textWidth(name, NAME_SCALE), canvas.textWidth(secondary, SECONDARY_SCALE)));
        }
    }

    private static void drawText(SlideCanvas canvas, float x, float width, Text text, int lineColor) {
        float headerWidth = canvas.textWidth(text.header, HEADER_SCALE);
        canvas.text(text.header, x, HEADER_Y, HEADER_SCALE, TEXT_COLOR, width);
        canvas.text(text.line, x + headerWidth, HEADER_Y, HEADER_SCALE, 0xFF000000 | lineColor, width - headerWidth);
        canvas.text(text.name, x, NAME_Y, NAME_SCALE, TEXT_COLOR, width);
        canvas.text(text.secondary, x, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, width);
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
