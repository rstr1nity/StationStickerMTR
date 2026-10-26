package com.tr1nity.stationsticker.client.display;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Временные слайды для проверки переходов; заменятся слайдами табло прибытия
public final class TestSlides {

    private static final int TEXT_COLOR = 0xFFF2F2F2;
    private static final int TEXT_DIM_COLOR = 0xFFB0B0B0;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TestSlides() {
    }

    public static DisplaySlide station(String platformName, String stationName) {
        return canvas -> {
            canvas.text(platformName, 0, 0, 1, TEXT_DIM_COLOR);
            canvas.text(stationName, 0, 11, 2, TEXT_COLOR);
        };
    }

    public static DisplaySlide clock() {
        return canvas -> {
            LocalDateTime now = LocalDateTime.now();
            canvas.text(now.format(DATE), 0, 0, 1, TEXT_DIM_COLOR);
            canvas.text(now.format(TIME), 0, 11, 2, TEXT_COLOR);
        };
    }
}
