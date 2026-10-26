package com.tr1nity.stationsticker.client.display;

import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

// Слайд погоды в стиле приложения «Погода»: небо по времени суток и погоде в игре, анимации
// (солнце, луна и звёзды, облака и тучи, дождь, снег, молния), крупно игровое время, состояние и станция.
// Всё считается от игрового времени - у всех игроков картинка одинаковая.
//
// Всё на слайде непрозрачное: «прозрачность» - это смешивание с цветом неба в этой точке. Полупрозрачные
// фигуры Minecraft сортирует по расстоянию до камеры, и под углом они рисовались раньше неба и показывали
// мир за экраном. По той же причине вспышка молнии - это посветлевшее небо, а не слой поверх.
// Всё, кроме текста, лежит глубже него; ничего не выходит за края экрана (обрезать корпус не умеет).
public final class WeatherSlide implements DisplaySlide {

    public static final float DESIGN_HEIGHT = 50;

    public enum Condition { CLEAR, RAIN, THUNDER, SNOW }

    // Что показывать: time - игровое время в тиках с долей тика, dayTime - время суток (0 = 06:00)
    public record State(Condition condition, float rainLevel, double time, long dayTime, String stationName) {
    }

    // Слои по глубине (единицы холста; небо -0.2, текст 0): иначе фигуры на одной глубине мерцают
    private static final float SKY_OBJECTS_Z = -0.15f;   // солнце, луна, звёзды
    private static final float CLOUDS_Z = -0.1f;
    private static final float PRECIPITATION_Z = -0.05f;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM_COLOR = 0xFFDCE6F2;
    private static final String KEY = "display.stationsticker.weather.";

    // Небо [верх, низ]: день, рассвет/закат, ночь, пасмурно днём и ночью
    private static final int[] DAY = {0xFF2F80ED, 0xFF8EC5FC};
    private static final int[] DUSK = {0xFF5B4B8A, 0xFFF7A26B};
    private static final int[] NIGHT = {0xFF0B1026, 0xFF263159};
    private static final int[] OVERCAST_DAY = {0xFF4A5568, 0xFF8A97A8};
    private static final int[] OVERCAST_NIGHT = {0xFF1A202C, 0xFF353F4F};

    private final State state;
    private int[] sky;
    private float height;

    public WeatherSlide(State state) {
        this.state = state;
    }

    @Override
    public void render(SlideCanvas canvas) {
        float w = canvas.width;
        float h = canvas.height;
        height = h;
        float night = nightFactor(state.dayTime);
        boolean bad = state.condition != Condition.CLEAR;
        float overcast = bad ? Math.max(0.6f, state.rainLevel) : 0;

        sky = mix(mix(DAY, DUSK, duskFactor(state.dayTime)), NIGHT, night);
        sky = mix(sky, mix(OVERCAST_DAY, OVERCAST_NIGHT, night), overcast);
        float flash = state.condition == Condition.THUNDER ? lightningFlash() : 0;
        int[] litSky = mix(sky, new int[]{0xFFFFFFFF, 0xFFFFFFFF}, flash * 0.45f);
        float b = canvas.bleed * 0.7f;
        canvas.gradientRect(-b, -b, w + 2 * b, h + 2 * b, litSky[0], litSky[1]);

        // Солнце или луна; в непогоду блёклые, как за тучами
        float dim = bad ? 0.3f : 1;
        if (night < 0.5f) drawSun(canvas, w, h, dim); else drawMoonAndStars(canvas, w, h, night, dim, bad);
        drawClouds(canvas, w, h, bad, night);
        switch (state.condition) {
            case RAIN -> drawRain(canvas, w, h);
            case THUNDER -> {
                drawRain(canvas, w, h);
                if (flash > 0) drawBolt(canvas, w, h, flash);
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
        canvas.text(I18n.get(KEY + state.condition.name().toLowerCase(Locale.ROOT)), 0, 38, 1.2f, TEXT_COLOR, w * 0.6f);
    }

    private void drawSun(SlideCanvas canvas, float w, float h, float dim) {
        float x = w * 0.84f;
        float y = h * 0.32f;
        float pulse = (float) (0.5 + 0.5 * Math.sin(state.time * 0.08));
        canvas.ring(x, y, 12 + pulse * 2, 9.5f + pulse, overSky(0xFFFFE08A, 0.18f * dim, y), SKY_OBJECTS_Z);
        canvas.ring(x, y, 9.5f + pulse, 7, overSky(0xFFFFE08A, 0.35f * dim, y), SKY_OBJECTS_Z);
        canvas.circle(x, y, 7, overSky(0xFFFFD34D, dim, y), SKY_OBJECTS_Z);
    }

    private void drawMoonAndStars(SlideCanvas canvas, float w, float h, float night, float dim, boolean bad) {
        if (!bad) {
            for (int i = 0; i < 45; i++) {
                float sx = hash(i, 1) * w;
                float sy = hash(i, 2) * h * 0.75f;
                float twinkle = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(state.time * (0.05 + hash(i, 3) * 0.1) + i)));
                float size = 0.5f + hash(i, 4) * 0.5f;
                canvas.rect(sx, sy, size, size, overSky(0xFFFFFFFF, twinkle * night, sy), SKY_OBJECTS_Z);
            }
        }
        float x = w * 0.84f;
        float y = h * 0.3f;
        canvas.ring(x, y, 8.5f, 6, overSky(0xFFF4F1DE, 0.2f * dim, y), SKY_OBJECTS_Z);
        canvas.circle(x, y, 6, overSky(0xFFF4F1DE, dim, y), SKY_OBJECTS_Z);
    }

    // Облака из кругов плывут вправо внутри экрана; у краёв растворяются в небе и появляются с другой
    // стороны. В непогоду их больше, они серее (в грозу и ночью - темнее)
    private void drawClouds(SlideCanvas canvas, float w, float h, boolean bad, float night) {
        int count = bad ? 5 : 2;
        int color;
        if (bad) {
            color = state.condition == Condition.THUNDER ? 0xFF5A6270 : 0xFF8A94A2;
            color = mixColor(color, 0xFF343C4A, night);
        } else {
            color = mixColor(0xFFF2F6FA, 0xFF2E3A52, night);
        }
        for (int i = 0; i < count; i++) {
            float s = (bad ? 1.0f : 0.8f) + hash(i, 8) * 0.6f;
            float cloudWidth = 18 * s;
            float span = w - cloudWidth;
            if (span <= 0) continue;
            float speed = 0.05f + hash(i, 5) * 0.05f;
            float x = (float) ((hash(i, 6) * span + state.time * speed) % span);
            float y = 2 + hash(i, 7) * (h * 0.4f);
            float fade = Math.min(1, Math.min(x, span - x) / 12);
            int c = overSky(color, fade, y + 4 * s);
            canvas.circle(x + 5 * s, y + 6 * s, 5 * s, c, CLOUDS_Z);
            canvas.circle(x + 10 * s, y + 4 * s, 6 * s, c, CLOUDS_Z);
            canvas.circle(x + 15 * s, y + 6 * s, 3 * s, c, CLOUDS_Z);
            canvas.rect(x + 5 * s, y + 6 * s, 10 * s, 5 * s, c, CLOUDS_Z);
        }
    }

    private void drawRain(SlideCanvas canvas, float w, float h) {
        int drops = Math.min(220, Math.round(w * 0.7f * Math.max(0.3f, state.rainLevel)));
        float span = h - 5;
        for (int i = 0; i < drops; i++) {
            float speed = 1.6f + hash(i, 9) * 0.8f;
            float y = (float) ((hash(i, 10) * span + state.time * speed) % span);
            float x = wrap(hash(i, 11) * w - y * 0.25f, 2, w);
            canvas.line(x, y, x - 1.1f, y + 4.5f, 0.45f, overSky(0xFFCFE3FF, 0.6f, y), PRECIPITATION_Z);
        }
    }

    private void drawSnow(SlideCanvas canvas, float w, float h) {
        int flakes = Math.min(160, Math.round(w * 0.45f));
        float span = h - 1.5f;
        for (int i = 0; i < flakes; i++) {
            float speed = 0.18f + hash(i, 12) * 0.14f;
            float y = (float) ((hash(i, 13) * span + state.time * speed) % span);
            float x = wrap(hash(i, 14) * w + (float) Math.sin(state.time * 0.04 + i) * 2.5f, 0, w - 1.5f);
            float size = 0.7f + hash(i, 15) * 0.6f;
            canvas.rect(x, y, size, size, overSky(0xFFFFFFFF, 0.9f, y), PRECIPITATION_Z);
        }
    }

    // Яркость вспышки 0..1: раз в ~6 секунд короткая вспышка
    private float lightningFlash() {
        int period = 120;
        long cycle = (long) (state.time / period);
        double local = state.time - cycle * (double) period;
        double k = (local - hash((int) cycle, 16) * (period - 20)) / 6;
        return k < 0 || k > 1 ? 0 : (float) (1 - k);
    }

    // Зигзаг молнии сверху во время вспышки
    private void drawBolt(SlideCanvas canvas, float w, float h, float flash) {
        long cycle = (long) (state.time / 120);
        float x = w * (0.45f + hash((int) cycle, 17) * 0.4f);
        float y = 0;
        for (int i = 0; i < 6; i++) {
            float nx = Math.max(1, Math.min(w - 1, x + (hash((int) cycle * 7 + i, 18) - 0.5f) * 8));
            float ny = y + h * 0.12f;
            canvas.line(x, y, nx, ny, 1.1f, overSky(0xFFFFF6A0, flash, ny), PRECIPITATION_Z);
            x = nx;
            y = ny;
        }
    }

    // Цвет color с «прозрачностью» alpha поверх неба на высоте y - уже смешанный, непрозрачный
    private int overSky(int color, float alpha, float y) {
        int skyColor = mixColor(sky[0], sky[1], y / height);
        return mixColor(skyColor, 0xFF000000 | color, alpha);
    }

    private static float wrap(float value, float min, float max) {
        float span = max - min;
        return ((value - min) % span + span) % span + min;
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

    // Детерминированное псевдослучайное 0..1 для частицы i: одинаково у всех игроков и между кадрами
    private static float hash(int i, int salt) {
        long x = i * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
        x ^= x >>> 33;
        x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }
}
