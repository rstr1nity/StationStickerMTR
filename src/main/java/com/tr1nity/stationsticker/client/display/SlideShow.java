package com.tr1nity.stationsticker.client.display;

import java.util.List;

// Смена слайдов по игровому времени: у всех игроков на сервере экраны показывают одно и то же.
// Переход: старый слайд уходит вверх, затем новый появляется снизу.
public final class SlideShow {

    public static final int SLIDE_TICKS = 160;       // 8 секунд на слайд
    public static final int TRANSITION_TICKS = 12;   // 0.6 секунды на переход
    private static final float SHIFT = 4;            // сдвиг при переходе, пикселей шрифта

    private SlideShow() {
    }

    // time - игровое время в тиках с учётом partialTick
    public static void render(List<DisplaySlide> slides, double time, SlideCanvas canvas) {
        int count = slides.size();
        if (count == 0) return;
        if (count == 1) {
            slides.get(0).render(canvas);
            return;
        }

        double t = time % ((long) SLIDE_TICKS * count);
        int index = (int) (t / SLIDE_TICKS);
        double local = t - (double) index * SLIDE_TICKS;

        if (local < TRANSITION_TICKS) {
            float p = smoothstep((float) (local / TRANSITION_TICKS));
            if (p < 0.5f) {
                float progress = p * 2;
                DisplaySlide previous = slides.get((index - 1 + count) % count);
                canvas.drawShifted(-progress * SHIFT, 1 - progress, previous::render);
            } else {
                float progress = (p - 0.5f) * 2;
                canvas.drawShifted((1 - progress) * SHIFT, progress, slides.get(index)::render);
            }
        } else {
            slides.get(index).render(canvas);
        }
    }

    private static float smoothstep(float x) {
        return x * x * (3 - 2 * x);
    }
}
