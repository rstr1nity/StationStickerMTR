package com.tr1nity.stationsticker.client.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

// Холст экрана в пикселях шрифта: начало в левом верхнем углу, y вниз.
// Прозрачность задаётся переходом и применяется ко всему, что рисует слайд.
public final class SlideCanvas {

    // Font считает цвет с альфой меньше 4 непрозрачным, такой текст не рисуем
    private static final int MIN_FONT_ALPHA = 4;
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    public final float width;
    public final float height;
    private final float alpha;

    public SlideCanvas(PoseStack poseStack, MultiBufferSource bufferSource, float width, float height, float alpha) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.width = width;
        this.height = height;
        this.alpha = alpha;
    }

    public float textWidth(String text, float scale) {
        return Minecraft.getInstance().font.width(text) * scale;
    }

    public float boldTextWidth(String text, float scale) {
        return Minecraft.getInstance().font.width(bold(text)) * scale;
    }

    // Текст от x до правого края холста; уменьшается, если не помещается
    public void text(String text, float x, float y, float scale, int color) {
        drawFitted(Component.literal(text), x, y, scale, color, width - x, false);
    }

    public void boldText(String text, float x, float y, float scale, int color) {
        drawFitted(bold(text), x, y, scale, color, width - x, false);
    }

    // Текст, прижатый правым краем к x
    public void textRight(String text, float x, float y, float scale, int color) {
        drawFitted(Component.literal(text), x, y, scale, color, x, true);
    }

    private static Component bold(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BOLD);
    }

    // Текст точечным шрифтом табло от x до правого края холста; уменьшается, если не помещается
    public void ledText(String text, float x, float y, float scale, int color, boolean bold) {
        LedFont.draw(this, text, x, y, scale, color, bold, width - x);
    }

    // Точечный текст, прижатый правым краем к x
    public void ledTextRight(String text, float x, float y, float scale, int color) {
        float textWidth = Math.min(LedFont.width(text, scale), x);
        LedFont.draw(this, text, x - textWidth, y, scale, color, false, x);
    }

    public float ledTextWidth(String text, float scale) {
        return LedFont.width(text, scale);
    }

    // Закрашенный прямоугольник на уровне текста
    public void rect(float x, float y, float w, float h, int color) {
        int rectAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (rectAlpha == 0) return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();
        // y вниз меняет направление обхода - рисуем обе стороны
        vertex(consumer, pose, x, y, 0, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y, 0, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y + h, 0, r, g, b, rectAlpha);
        vertex(consumer, pose, x, y + h, 0, r, g, b, rectAlpha);

        vertex(consumer, pose, x, y + h, 0, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y + h, 0, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y, 0, r, g, b, rectAlpha);
        vertex(consumer, pose, x, y, 0, r, g, b, rectAlpha);
    }

    // Закрашенный круг; чуть глубже текста, чтобы цифра на нём была видна
    public void circle(float centerX, float centerY, float radius, int color) {
        int circleAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (circleAlpha == 0) return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();
        float z = -0.1f;
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = centerX + (float) Math.cos(a0) * radius;
            float y0 = centerY + (float) Math.sin(a0) * radius;
            float x1 = centerX + (float) Math.cos(a1) * radius;
            float y1 = centerY + (float) Math.sin(a1) * radius;
            // Сектор как вырожденный четырёхугольник; y вниз, поэтому обход меняет направление - рисуем обе стороны
            vertex(consumer, pose, centerX, centerY, z, r, g, b, circleAlpha);
            vertex(consumer, pose, x0, y0, z, r, g, b, circleAlpha);
            vertex(consumer, pose, x1, y1, z, r, g, b, circleAlpha);
            vertex(consumer, pose, x1, y1, z, r, g, b, circleAlpha);

            vertex(consumer, pose, centerX, centerY, z, r, g, b, circleAlpha);
            vertex(consumer, pose, x1, y1, z, r, g, b, circleAlpha);
            vertex(consumer, pose, x0, y0, z, r, g, b, circleAlpha);
            vertex(consumer, pose, x0, y0, z, r, g, b, circleAlpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int r, int g, int b, int a) {
        consumer.vertex(pose, x, y, z).color(r, g, b, a).uv(0, 0).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }

    private void drawFitted(Component text, float x, float y, float scale, int color, float maxWidth, boolean alignRight) {
        if (text.getString().isEmpty()) return;
        int textAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (textAlpha < MIN_FONT_ALPHA) return;

        float textWidth = Minecraft.getInstance().font.width(text) * scale;
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
