package com.tr1nity.stationsticker.client.display;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.List;

// Смена слайдов по игровому времени: у всех игроков на сервере экраны показывают одно и то же.
// Переход: старый слайд уходит вверх и гаснет, новый поднимается снизу и проявляется.
public final class SlideShow {

    public static final int SLIDE_TICKS = 160;       // 8 секунд на слайд
    public static final int TRANSITION_TICKS = 12;   // 0.6 секунды на переход
    private static final float SHIFT = 4;            // сдвиг при переходе, пикселей шрифта

    private SlideShow() {
    }

    // time - игровое время в тиках с учётом partialTick
    public static void render(List<DisplaySlide> slides, double time, PoseStack poseStack,
                              MultiBufferSource bufferSource, float width, float height) {
        int count = slides.size();
        if (count == 0) return;
        if (count == 1) {
            draw(slides.get(0), 1, 0, poseStack, bufferSource, width, height);
            return;
        }

        double t = time % ((long) SLIDE_TICKS * count);
        int index = (int) (t / SLIDE_TICKS);
        double local = t - (double) index * SLIDE_TICKS;

        if (local < TRANSITION_TICKS) {
            float p = smoothstep((float) (local / TRANSITION_TICKS));
            DisplaySlide previous = slides.get((index - 1 + count) % count);
            draw(previous, 1 - p, -p * SHIFT, poseStack, bufferSource, width, height);
            draw(slides.get(index), p, (1 - p) * SHIFT, poseStack, bufferSource, width, height);
        } else {
            draw(slides.get(index), 1, 0, poseStack, bufferSource, width, height);
        }
    }

    private static void draw(DisplaySlide slide, float alpha, float shiftY, PoseStack poseStack,
                             MultiBufferSource bufferSource, float width, float height) {
        poseStack.pushPose();
        poseStack.translate(0, shiftY, 0);
        slide.render(new SlideCanvas(poseStack, bufferSource, width, height, alpha));
        poseStack.popPose();
    }

    private static float smoothstep(float x) {
        return x * x * (3 - 2 * x);
    }
}
