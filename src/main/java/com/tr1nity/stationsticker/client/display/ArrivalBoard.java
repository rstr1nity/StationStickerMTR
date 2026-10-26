package com.tr1nity.stationsticker.client.display;

import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mod.data.ArrivalsCacheClient;

import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nullable;

// Табло прибытия в стиле Московского метро (без анимации):
//   Направление                 День 15  20:52
//   (3) Варшавская
//   Поезд прибывает через
//   01 мин 26 сек
public final class ArrivalBoard {

    // Раскладка в шагах точки LED-шрифта (y верха строки). Акцент на цифрах таймера, как на табло в метро.
    private static final float STATION_Y = 11;
    private static final float CAPTION_Y = 25;
    private static final float TIMER_Y = 36;
    private static final float DIGIT_SCALE = 3.5f;
    private static final float UNIT_SCALE = 1.3f;
    private static final float GLYPH_HEIGHT = LedFont.CAP_HEIGHT;   // высота заглавных и цифр до базовой линии
    private static final float TIMER_BASELINE = TIMER_Y + GLYPH_HEIGHT * DIGIT_SCALE;

    // Высота раскладки: под неё рендерер подбирает масштаб
    public static final float DESIGN_HEIGHT = TIMER_BASELINE + 1;

    private static final String KEY = "display.stationsticker.arrival.";

    private static final int TEXT_COLOR = 0xFFF2F2F2;
    private static final int TEXT_DIM_COLOR = 0xFFB0B0B0;

    private ArrivalBoard() {
    }

    public static void render(SlideCanvas canvas, long platformId, long dayTime) {
        canvas.ledText(I18n.get(KEY + "direction"), 0, 0, 1, TEXT_DIM_COLOR, false);
        canvas.ledTextRight(formatDayTime(dayTime), canvas.width, 0, 1, TEXT_DIM_COLOR);

        ArrivalResponse arrival = platformId == 0 ? null : nextArrival(platformId);
        if (arrival == null) {
            canvas.ledText(I18n.get(KEY + "no_trains"), 0, CAPTION_Y, 1, TEXT_DIM_COLOR, false);
            return;
        }

        // Кружок линии с номером и конечная станция
        float radius = 6;
        float circleCenterY = STATION_Y + radius;
        canvas.circle(radius, circleCenterY, radius, 0xFF000000 | arrival.getRouteColor());
        String number = arrival.getRouteNumber();
        if (!number.isEmpty()) {
            float numberScale = Math.min(1.1f, radius * 1.5f / canvas.ledTextWidth(number, 1));
            canvas.ledText(number, radius - canvas.ledTextWidth(number, numberScale) / 2,
                    circleCenterY - GLYPH_HEIGHT * numberScale / 2, numberScale, TEXT_COLOR, true);
        }
        float stationScale = 1.5f;
        String destination = arrival.getIsTerminating()
                ? I18n.get(KEY + "terminates")
                : StationNames.forCurrentLanguage(arrival.getDestination());
        canvas.ledText(destination, radius * 2 + 4,
                circleCenterY - GLYPH_HEIGHT * stationScale / 2, stationScale, TEXT_COLOR, false);

        long seconds = Math.max(0, (arrival.getArrival() - currentMillis()) / 1000);
        if (seconds == 0) {
            canvas.ledText(I18n.get(KEY + "arriving"), 0, TIMER_Y, 2, TEXT_COLOR, true);
            return;
        }
        canvas.ledText(I18n.get(KEY + "arrives_in"), 0, CAPTION_Y, 1, TEXT_DIM_COLOR, true);

        // Крупные цифры, мелкие подписи по базовой линии цифр
        float x = timerPart(canvas, 0, String.format("%02d", seconds / 60), I18n.get(KEY + "min"));
        timerPart(canvas, x + 6, String.format("%02d", seconds % 60), I18n.get(KEY + "sec"));
    }

    private static float timerPart(SlideCanvas canvas, float x, String digits, String unit) {
        canvas.ledText(digits, x, TIMER_Y, DIGIT_SCALE, TEXT_COLOR, true);
        x += canvas.ledTextWidth(digits, DIGIT_SCALE) + 2;
        canvas.ledText(unit, x, TIMER_BASELINE - GLYPH_HEIGHT * UNIT_SCALE, UNIT_SCALE, TEXT_COLOR, false);
        return x + canvas.ledTextWidth(unit, UNIT_SCALE);
    }

    // Ближайший поезд, который ещё не ушёл. Поезда, идущие дальше, важнее тех, что здесь заканчивают маршрут;
    // но на конечной у платформы прибытия других нет, и тогда показываем их («Поезд дальше не идёт»)
    @Nullable
    public static ArrivalResponse nextArrival(long platformId) {
        long now = currentMillis();
        ArrivalResponse next = null;
        ArrivalResponse nextTerminating = null;
        for (ArrivalResponse arrival : ArrivalsCacheClient.INSTANCE.requestArrivals(LongArrayList.of(platformId))) {
            if (arrival.getDeparture() < now) continue;
            if (arrival.getIsTerminating()) {
                if (nextTerminating == null || arrival.getArrival() < nextTerminating.getArrival()) nextTerminating = arrival;
            } else if (next == null || arrival.getArrival() < next.getArrival()) {
                next = arrival;
            }
        }
        return next != null ? next : nextTerminating;
    }

    // Время сервера MTR, как в RenderPIDS
    public static long currentMillis() {
        return System.currentTimeMillis() + ArrivalsCacheClient.INSTANCE.getMillisOffset();
    }

    // Игровые день и время: тик 0 - это 06:00 первого дня
    private static String formatDayTime(long dayTime) {
        long day = dayTime / 24000 + 1;
        long ticksInDay = dayTime % 24000;
        long hours = (ticksInDay / 1000 + 6) % 24;
        long minutes = (ticksInDay % 1000) * 60 / 1000;
        return I18n.get(KEY + "day", day) + String.format("  %02d:%02d", hours, minutes);
    }
}
