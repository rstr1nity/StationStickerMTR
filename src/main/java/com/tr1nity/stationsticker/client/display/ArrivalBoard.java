package com.tr1nity.stationsticker.client.display;

import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mod.data.ArrivalsCacheClient;

import javax.annotation.Nullable;

// Табло прибытия в стиле Московского метро (без анимации):
//   Направление                 День 15  20:52
//   (3) Варшавская
//   Поезд прибывает через
//   01 мин 26 сек
public final class ArrivalBoard {

    // Высота раскладки в пикселях шрифта: под неё рендерер подбирает масштаб
    public static final float DESIGN_HEIGHT = 57;

    private static final int TEXT_COLOR = 0xFFF2F2F2;
    private static final int TEXT_DIM_COLOR = 0xFFB0B0B0;

    private ArrivalBoard() {
    }

    public static void render(SlideCanvas canvas, long platformId, long dayTime) {
        canvas.text("Направление", 0, 0, 1, TEXT_DIM_COLOR);
        canvas.textRight(formatDayTime(dayTime), canvas.width, 0, 1, TEXT_DIM_COLOR);

        ArrivalResponse arrival = platformId == 0 ? null : nextArrival(platformId);
        if (arrival == null) {
            canvas.text("Нет данных о поездах", 0, 28, 1, TEXT_DIM_COLOR);
            return;
        }

        // Кружок линии с номером и конечная станция
        float radius = 6;
        canvas.circle(radius, 11 + radius, radius, 0xFF000000 | arrival.getRouteColor());
        String number = arrival.getRouteNumber();
        if (!number.isEmpty()) {
            float numberScale = 0.9f;
            canvas.text(number, radius - canvas.textWidth(number, numberScale) / 2, 11 + radius - 3.5f, numberScale, TEXT_COLOR);
        }
        canvas.text(firstLanguage(arrival.getDestination()), radius * 2 + 4, 11, 1.5f, TEXT_COLOR);

        long seconds = Math.max(0, (arrival.getArrival() - currentMillis()) / 1000);
        if (seconds == 0) {
            canvas.text("Поезд прибывает", 0, 39, 2, TEXT_COLOR);
            return;
        }
        canvas.text("Поезд прибывает через", 0, 28, 1, TEXT_DIM_COLOR);

        // Крупные цифры, мелкие подписи по нижнему краю цифр
        float x = 0;
        x = timerPart(canvas, x, String.format("%02d", seconds / 60), "мин");
        timerPart(canvas, x + 4, String.format("%02d", seconds % 60), "сек");
    }

    private static float timerPart(SlideCanvas canvas, float x, String digits, String unit) {
        canvas.text(digits, x, 39, 2, TEXT_COLOR);
        x += canvas.textWidth(digits, 2) + 1;
        canvas.text(unit, x, 46, 1, TEXT_COLOR);
        return x + canvas.textWidth(unit, 1);
    }

    // Ближайший поезд, который ещё не ушёл; конечные (поезд дальше не идёт) пропускаем
    @Nullable
    private static ArrivalResponse nextArrival(long platformId) {
        long now = currentMillis();
        ArrivalResponse next = null;
        for (ArrivalResponse arrival : ArrivalsCacheClient.INSTANCE.requestArrivals(LongArrayList.of(platformId))) {
            if (arrival.getIsTerminating() || arrival.getDeparture() < now) continue;
            if (next == null || arrival.getArrival() < next.getArrival()) {
                next = arrival;
            }
        }
        return next;
    }

    // Время сервера MTR, как в RenderPIDS
    private static long currentMillis() {
        return System.currentTimeMillis() + ArrivalsCacheClient.INSTANCE.getMillisOffset();
    }

    // Игровые день и время: тик 0 - это 06:00 первого дня
    private static String formatDayTime(long dayTime) {
        long day = dayTime / 24000 + 1;
        long ticksInDay = dayTime % 24000;
        long hours = (ticksInDay / 1000 + 6) % 24;
        long minutes = (ticksInDay % 1000) * 60 / 1000;
        return String.format("День %d  %02d:%02d", day, hours, minutes);
    }

    // Названия в MTR хранятся как "Русское|English": берём первую часть
    private static String firstLanguage(String name) {
        int separator = name.indexOf('|');
        return separator < 0 ? name : name.substring(0, separator);
    }
}
