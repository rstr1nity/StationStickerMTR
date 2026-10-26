package com.tr1nity.stationsticker.client.display;

import net.minecraft.client.resources.language.I18n;

// Слайд погоды в стиле приложения «Погода»: небо по времени суток и погоде в игре, анимации
// (солнце, луна и звёзды, облака, дождь, снег, молния), крупно игровое время, состояние и станция.
// Всё считается от игрового времени - у всех игроков картинка одинаковая.
public final class WeatherSlide implements DisplaySlide {

    public static final float DESIGN_HEIGHT = 50;

    public enum Condition { CLEAR, RAIN, THUNDER, SNOW }

    // Что показывать: time - игровое время в тиках с долей тика, dayTime - время суток (0 = 06:00)
    public record State(Condition condition, float rainLevel, double time, long dayTime, String stationName) {
    }

    // Слои по глубине (единицы холста; небо -0.2, текст 0, вспышка 0.1): иначе фигуры на одной глубине мерцают
    private static final float SKY_OBJECTS_Z = -0.15f;   // солнце, луна, звёзды
    private static final float CLOUDS_Z = -0.09f;
    private static final float PRECIPITATION_Z = -0.04f;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM_COLOR = 0xCCFFFFFF;
    private static final String KEY = "display.stationsticker.weather.";

    // Небо [верх, низ]: день, рассвет/закат, ночь, пасмурно днём и ночью
    private static final int[] DAY = {0xFF2F80ED, 0xFF8EC5FC};
    private static final int[] DUSK = {0xFF5B4B8A, 0xFFF7A26B};
    private static final int[] NIGHT = {0xFF0B1026, 0xFF263159};
    private static final int[] OVERCAST_DAY = {0xFF4A5568, 0xFF8A97A8};
    private static final int[] OVERCAST_NIGHT = {0xFF1A202C, 0xFF353F4F};

    private final State state;

    public WeatherSlide(State state) {
        this.state = state;
    }

    @Override
    public void render(SlideCanvas canvas) {
        float w = canvas.width;
        float h = canvas.height;
        float night = nightFactor(state.dayTime);
        float overcast = state.condition == Condition.CLEAR ? 0 : Math.max(0.6f, state.rainLevel);

        int[] sky = mix(mix(DAY, DUSK, duskFactor(state.dayTime)), NIGHT, night);
        sky = mix(sky, mix(OVERCAST_DAY, OVERCAST_NIGHT, night), overcast);
        float b = canvas.bleed * 0.7f;
        canvas.gradientRect(-b, -b, w + 2 * b, h + 2 * b, sky[0], sky[1]);

        if (overcast < 0.5f) {
            if (night < 0.5f) drawSun(canvas, w, h); else drawMoonAndStars(canvas, w, h, night);
        }
        drawClouds(canvas, w, h, overcast, night);
        switch (state.condition) {
            case RAIN -> drawRain(canvas, w, h);
            case THUNDER -> {
                drawRain(canvas, w, h);
                drawLightning(canvas, w, h);
            }
            case SNOW -> drawSnow(canvas, w, h);
            default -> {
            }
        }

        // Текст слева: станция, крупно время, состояние
        long ticks = state.dayTime % 24000;
        String clock = String.format("%02d:%02d", (ticks / 1000 + 6) % 24, (ticks % 1000) * 60 / 1000);
        canvas.text(state.stationName, 0, 0, 0.9f, TEXT_DIM_COLOR, w * 0.6f);
        canvas.text(clock, 0, 10, 3, TEXT_COLOR, w * 0.6f);
        canvas.text(I18n.get(KEY + state.condition.name().toLowerCase(java.util.Locale.ROOT)), 0, 38, 1.2f, TEXT_COLOR, w * 0.6f);
    }

    private void drawSun(SlideCanvas canvas, float w, float h) {
        float x = w * 0.84f;
        float y = h * 0.32f;
        float pulse = (float) (0.5 + 0.5 * Math.sin(state.time * 0.08));
        // Ореол кольцами, а не кругами друг на друге: перекрытие на одной глубине мерцает
        canvas.ring(x, y, 12 + pulse * 2, 9.5f + pulse, alpha(0xFFFFE08A, 0.18f), SKY_OBJECTS_Z);
        canvas.ring(x, y, 9.5f + pulse, 7, alpha(0xFFFFE08A, 0.35f), SKY_OBJECTS_Z);
        canvas.circle(x, y, 7, 0xFFFFD34D, SKY_OBJECTS_Z);
    }

    private void drawMoonAndStars(SlideCanvas canvas, float w, float h, float night) {
        for (int i = 0; i < 45; i++) {
            float sx = hash(i, 1) * w;
            float sy = hash(i, 2) * h * 0.75f;
            float twinkle = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(state.time * (0.05 + hash(i, 3) * 0.1) + i)));
            float size = 0.5f + hash(i, 4) * 0.5f;
            canvas.rect(sx, sy, size, size, alpha(0xFFFFFFFF, twinkle * night), SKY_OBJECTS_Z);
        }
        float x = w * 0.84f;
        float y = h * 0.3f;
        canvas.ring(x, y, 8.5f, 6, alpha(0xFFF4F1DE, 0.2f), SKY_OBJECTS_Z);
        canvas.circle(x, y, 6, 0xFFF4F1DE, SKY_OBJECTS_Z);
    }

    // Облака из кругов плывут вправо; в непогоду их больше и они серее
    private void drawClouds(SlideCanvas canvas, float w, float h, float overcast, float night) {
        int count = 2 + Math.round(overcast * 3);
        // Непрозрачные: полупрозрачные круги облака, накладываясь, мерцали бы
        int color = overcast > 0.5f ? (night > 0.5f ? 0xFF4A5363 : 0xFFB8C2CF) : (night > 0.5f ? 0xFF2E3A52 : 0xFFF2F6FA);
        float span = w + 60;
        for (int i = 0; i < count; i++) {
            float speed = 0.05f + hash(i, 5) * 0.05f;
            float x = (float) ((hash(i, 6) * span + state.time * speed) % span) - 30;
            float y = 4 + hash(i, 7) * h * 0.35f;
            float s = 0.8f + hash(i, 8) * 0.6f;
            canvas.circle(x, y + 3 * s, 5 * s, color, CLOUDS_Z);
            canvas.circle(x + 6 * s, y, 7 * s, color, CLOUDS_Z);
            canvas.circle(x + 13 * s, y + 3 * s, 5 * s, color, CLOUDS_Z);
            canvas.rect(x, y + 3 * s, 13 * s, 5 * s, color, CLOUDS_Z);
        }
    }

    private void drawRain(SlideCanvas canvas, float w, float h) {
        int drops = Math.min(220, Math.round(w * 0.7f * Math.max(0.3f, state.rainLevel)));
        float span = h + 8;
        for (int i = 0; i < drops; i++) {
            float speed = 1.6f + hash(i, 9) * 0.8f;
            float y = (float) ((hash(i, 10) * span + state.time * speed) % span) - 4;
            float x = hash(i, 11) * (w + 6) - 3 - y * 0.25f;
            canvas.line(x, y, x - 1.1f, y + 4.5f, 0.45f, 0x99CFE3FF, PRECIPITATION_Z);
        }
    }

    private void drawSnow(SlideCanvas canvas, float w, float h) {
        int flakes = Math.min(160, Math.round(w * 0.45f));
        float span = h + 4;
        for (int i = 0; i < flakes; i++) {
            float speed = 0.18f + hash(i, 12) * 0.14f;
            float y = (float) ((hash(i, 13) * span + state.time * speed) % span) - 2;
            float x = hash(i, 14) * w + (float) Math.sin(state.time * 0.04 + i) * 2.5f;
            float size = 0.7f + hash(i, 15) * 0.6f;
            canvas.rect(x, y, size, size, 0xE6FFFFFF, PRECIPITATION_Z);
        }
    }

    // Раз в ~6 секунд короткая вспышка на весь экран и зигзаг молнии
    private void drawLightning(SlideCanvas canvas, float w, float h) {
        int period = 120;
        long cycle = (long) (state.time / period);
        double local = state.time - cycle * (double) period;
        double start = hash((int) cycle, 16) * (period - 20);
        double k = (local - start) / 6;
        if (k < 0 || k > 1) return;

        float fade = (float) (1 - k);
        float b = canvas.bleed * 0.7f;
        canvas.overlayRect(-b, -b, w + 2 * b, h + 2 * b, alpha(0xFFFFFFFF, 0.45f * fade));
        float x = w * (0.45f + hash((int) cycle, 17) * 0.4f);
        float y = 0;
        for (int i = 0; i < 6; i++) {
            float nx = x + (hash((int) cycle * 7 + i, 18) - 0.5f) * 8;
            float ny = y + h * 0.12f;
            canvas.line(x, y, nx, ny, 1.1f, alpha(0xFFFFF6A0, fade), PRECIPITATION_Z);
            x = nx;
            y = ny;
        }
    }

    // Доля ночи 0..1 по времени суток (рассвет 04:30-07:00, закат 17:30-20:00)
    private static float nightFactor(long dayTime) {
        float hour = hour(dayTime);
        if (hour >= 7 && hour < 17.5f) return 0;
        if (hour >= 20 || hour < 4.5f) return 1;
        return hour < 12 ? 1 - (hour - 4.5f) / 2.5f : (hour - 17.5f) / 2.5f;
    }

    // Насколько небо в цветах рассвета/заката: максимум в 05:45 и 18:45
    private static float duskFactor(long dayTime) {
        float hour = hour(dayTime);
        float distance = Math.min(Math.abs(hour - 5.75f), Math.abs(hour - 18.75f));
        return Math.max(0, 1 - distance / 1.5f);
    }

    private static float hour(long dayTime) {
        return ((dayTime % 24000) / 1000f + 6) % 24;
    }

    private static int[] mix(int[] a, int[] b, float t) {
        return new int[]{mixColor(a[0], b[0], t), mixColor(a[1], b[1], t)};
    }

    private static int mixColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            result |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return result;
    }

    private static int alpha(int color, float alpha) {
        return (Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, alpha))) << 24) | (color & 0xFFFFFF);
    }

    // Детерминированное псевдослучайное 0..1 для частицы i: одинаково у всех игроков и между кадрами
    private static float hash(int i, int salt) {
        long x = i * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        x ^= x >>> 33;
        x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }
}
