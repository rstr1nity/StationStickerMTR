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
    // Сколько экрана есть за краями холста (поля): фон слайда может заходить туда, текст - нет
    public final float bleed;

    public SlideCanvas(PoseStack poseStack, MultiBufferSource bufferSource, float width, float height, float alpha) {
        this(poseStack, bufferSource, width, height, alpha, 0);
    }

    public SlideCanvas(PoseStack poseStack, MultiBufferSource bufferSource, float width, float height, float alpha, float bleed) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.width = width;
        this.height = height;
        this.alpha = alpha;
        this.bleed = bleed;
    }

    // Рисует со сдвигом по y и дополнительной прозрачностью (переходы слайдов)
    public void drawShifted(float shiftY, float extraAlpha, java.util.function.Consumer<SlideCanvas> drawer) {
        poseStack.pushPose();
        poseStack.translate(0, shiftY, 0);
        drawer.accept(new SlideCanvas(poseStack, bufferSource, width, height, alpha * extraAlpha, bleed));
        poseStack.popPose();
    }

    // Вертикальный градиент (фон слайда) - глубже всего остального
    public void gradientRect(float x, float y, float w, float h, int top, int bottom) {
        int topAlpha = Math.round(((top >>> 24) & 0xFF) * alpha);
        int bottomAlpha = Math.round(((bottom >>> 24) & 0xFF) * alpha);
        if (topAlpha == 0 && bottomAlpha == 0) return;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();
        float z = -0.2f;
        int tr = (top >> 16) & 0xFF, tg = (top >> 8) & 0xFF, tb = top & 0xFF;
        int br = (bottom >> 16) & 0xFF, bg = (bottom >> 8) & 0xFF, bb = bottom & 0xFF;
        vertex(consumer, pose, x, y, z, tr, tg, tb, topAlpha);
        vertex(consumer, pose, x + w, y, z, tr, tg, tb, topAlpha);
        vertex(consumer, pose, x + w, y + h, z, br, bg, bb, bottomAlpha);
        vertex(consumer, pose, x, y + h, z, br, bg, bb, bottomAlpha);

        vertex(consumer, pose, x, y + h, z, br, bg, bb, bottomAlpha);
        vertex(consumer, pose, x + w, y + h, z, br, bg, bb, bottomAlpha);
        vertex(consumer, pose, x + w, y, z, tr, tg, tb, topAlpha);
        vertex(consumer, pose, x, y, z, tr, tg, tb, topAlpha);
    }

    // Полупрозрачная заливка поверх всего (вспышка молнии)
    public void overlayRect(float x, float y, float w, float h, int color) {
        rect(x, y, w, h, color, 0.1f);
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

    // То же с явной максимальной шириной (например, половина экрана)
    public void text(String text, float x, float y, float scale, int color, float maxWidth) {
        drawFitted(Component.literal(text), x, y, scale, color, maxWidth, false);
    }

    public void textRight(String text, float x, float y, float scale, int color, float maxWidth) {
        drawFitted(Component.literal(text), x, y, scale, color, maxWidth, true);
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
        rect(x, y, w, h, color, 0);
    }

    // Подложка под текст (плашка): чуть глубже, иначе текст на ней мерцает (z-fighting)
    public void backgroundRect(float x, float y, float w, float h, int color) {
        rect(x, y, w, h, color, -0.1f);
    }

    // Прямоугольник на заданной глубине (единицы холста): больше - ближе к зрителю, текст на 0
    public void rect(float x, float y, float w, float h, int color, float z) {
        int rectAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (rectAlpha == 0) return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();
        // y вниз меняет направление обхода - рисуем обе стороны
        vertex(consumer, pose, x, y, z, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y, z, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y + h, z, r, g, b, rectAlpha);
        vertex(consumer, pose, x, y + h, z, r, g, b, rectAlpha);

        vertex(consumer, pose, x, y + h, z, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y + h, z, r, g, b, rectAlpha);
        vertex(consumer, pose, x + w, y, z, r, g, b, rectAlpha);
        vertex(consumer, pose, x, y, z, r, g, b, rectAlpha);
    }

    // Закрашенный круг; чуть глубже текста, чтобы цифра на нём была видна
    public void circle(float centerX, float centerY, float radius, int color) {
        circle(centerX, centerY, radius, color, -0.1f);
    }

    // Круг на заданной глубине (слои фона слайда: солнце глубже облаков)
    public void circle(float centerX, float centerY, float radius, int color, float z) {
        int circleAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (circleAlpha == 0) return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();
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

    // Кольцо полосой между двумя радиусами на уровне текста. Не круг поверх круга:
    // две фигуры на одной глубине перемешиваются пятнами (z-fighting)
    public void ring(float centerX, float centerY, float outerRadius, float innerRadius, int color) {
        ring(centerX, centerY, outerRadius, innerRadius, color, 0);
    }

    public void ring(float centerX, float centerY, float outerRadius, float innerRadius, int color, float z) {
        int ringAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        if (ringAlpha == 0) return;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float c0 = (float) Math.cos(Math.PI * 2 * i / segments);
            float s0 = (float) Math.sin(Math.PI * 2 * i / segments);
            float c1 = (float) Math.cos(Math.PI * 2 * (i + 1) / segments);
            float s1 = (float) Math.sin(Math.PI * 2 * (i + 1) / segments);
            quad(consumer, pose,
                    centerX + c0 * outerRadius, centerY + s0 * outerRadius,
                    centerX + c1 * outerRadius, centerY + s1 * outerRadius,
                    centerX + c1 * innerRadius, centerY + s1 * innerRadius,
                    centerX + c0 * innerRadius, centerY + s0 * innerRadius, r, g, b, ringAlpha, z);
        }
    }

    // Отрезок заданной толщины одним повёрнутым прямоугольником (без ступенек)
    public void line(float x0, float y0, float x1, float y1, float thickness, int color) {
        line(x0, y0, x1, y1, thickness, color, 0);
    }

    public void line(float x0, float y0, float x1, float y1, float thickness, int color, float z) {
        int lineAlpha = Math.round(((color >>> 24) & 0xFF) * alpha);
        float length = (float) Math.hypot(x1 - x0, y1 - y0);
        if (lineAlpha == 0 || length == 0) return;
        float nx = -(y1 - y0) / length * thickness / 2;
        float ny = (x1 - x0) / length * thickness / 2;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        quad(consumer, poseStack.last().pose(),
                x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny,
                (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, lineAlpha, z);
    }

    // Картинка целиком в прямоугольник (например, значок таблички MTR); flipX - зеркально по горизонтали
    public void texture(ResourceLocation texture, float x, float y, float w, float h, boolean flipX) {
        int textureAlpha = Math.round(255 * alpha);
        if (textureAlpha == 0) return;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(texture));
        Matrix4f pose = poseStack.last().pose();
        float u0 = flipX ? 1 : 0;
        float u1 = flipX ? 0 : 1;
        texturedVertex(consumer, pose, x, y, u0, 0, textureAlpha);
        texturedVertex(consumer, pose, x + w, y, u1, 0, textureAlpha);
        texturedVertex(consumer, pose, x + w, y + h, u1, 1, textureAlpha);
        texturedVertex(consumer, pose, x, y + h, u0, 1, textureAlpha);

        texturedVertex(consumer, pose, x, y + h, u0, 1, textureAlpha);
        texturedVertex(consumer, pose, x + w, y + h, u1, 1, textureAlpha);
        texturedVertex(consumer, pose, x + w, y, u1, 0, textureAlpha);
        texturedVertex(consumer, pose, x, y, u0, 0, textureAlpha);
    }

    // Чуть выше прямоугольников и кругов, чтобы картинка лежала поверх своего фона
    private static void texturedVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float u, float v, int a) {
        consumer.vertex(pose, x, y, 0.05f).color(255, 255, 255, a).uv(u, v).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }

    // Четырёхугольник с обеих сторон (обход зависит от направления осей)
    private static void quad(VertexConsumer consumer, Matrix4f pose, float ax, float ay, float bx, float by,
                             float cx, float cy, float dx, float dy, int r, int g, int b, int a, float z) {
        vertex(consumer, pose, ax, ay, z, r, g, b, a);
        vertex(consumer, pose, bx, by, z, r, g, b, a);
        vertex(consumer, pose, cx, cy, z, r, g, b, a);
        vertex(consumer, pose, dx, dy, z, r, g, b, a);

        vertex(consumer, pose, dx, dy, z, r, g, b, a);
        vertex(consumer, pose, cx, cy, z, r, g, b, a);
        vertex(consumer, pose, bx, by, z, r, g, b, a);
        vertex(consumer, pose, ax, ay, z, r, g, b, a);
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
