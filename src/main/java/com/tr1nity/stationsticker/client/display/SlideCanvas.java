package com.tr1nity.stationsticker.client.display;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

// Холст экрана в пикселях шрифта: начало в левом верхнем углу, y вниз.
// Прозрачность задаётся переходом и применяется ко всему, что рисует слайд.
public final class SlideCanvas {

    // Font считает цвет с альфой меньше 4 непрозрачным, такой текст не рисуем
    private static final int MIN_FONT_ALPHA = 4;

    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    public final float width;
    public final float height;
    private final float alpha;

    SlideCanvas(PoseStack poseStack, MultiBufferSource bufferSource, float width, float height, float alpha) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.width = width;
        this.height = height;
        this.alpha = alpha;
    }

    public float textWidth(String text, float scale) {
        return Minecraft.getInstance().font.width(text) * scale;
    }

    // Текст от x до правого края холста; уменьшается, если не помещается
    public void text(String text, float x, float y, float scale, int color) {
        drawFitted(text, x, y, scale, color, width - x, false);
    }

    // Текст, прижатый правым краем к x
    public void textRight(String text, float x, float y, float scale, int color) {
        drawFitted(text, x, y, scale, color, x, true);
    }

    private void drawFitted(String text, float x, float y, float scale, int color, float maxWidth, boolean alignRight) {
        if (text.isEmpty()) return;
        int textAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (textAlpha < MIN_FONT_ALPHA) return;

        float textWidth = textWidth(text, scale);
        if (textWidth > maxWidth && maxWidth > 0) {
            scale *= maxWidth / textWidth;
            textWidth = maxWidth;
        }

        poseStack.pushPose();
        poseStack.translate(alignRight ? x - textWidth : x, y, 0);
        poseStack.scale(scale, scale, scale);
        Minecraft.getInstance().font.drawInBatch(text, 0, 0, (textAlpha << 24) | (color & 0xFFFFFF), false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
