package com.tr1nity.stationsticker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tr1nity.stationsticker.block.ArrivalDisplayBlock;
import com.tr1nity.stationsticker.block.DisplayAttachment;
import com.tr1nity.stationsticker.block.entity.ArrivalDisplayBlockEntity;
import com.tr1nity.stationsticker.client.display.ArrivalBoard;
import com.tr1nity.stationsticker.client.display.SlideCanvas;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.mtr.core.data.Platform;


// Локальные координаты: начало в центре клетки основного блока, +Z - лицевая сторона экрана,
// +X - вправо, если смотреть на экран (в сторону части). Табло занимает по X от -0.5 до 1.5.
public class ArrivalDisplayRenderer implements BlockEntityRenderer<ArrivalDisplayBlockEntity> {

    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    // Размеры подбираются вживую
    private static final float WIDTH = 2.0f;
    private static final float BEZEL = 1 / 16f;          // рамка корпуса вокруг экрана
    private static final float BODY_RADIUS = 0.15f;      // скругление углов корпуса (у экрана меньше на рамку)
    private static final int CORNER_SEGMENTS = 6;
    private static final float DEPTH_WALL = 2 / 16f;     // толщина корпуса на стене
    private static final float DEPTH_HANGING = 2 / 16f;  // толщина корпуса на подвесе и стойке
    private static final float TEXT_SCALE = 0.02f;       // блоков на пиксель шрифта
    private static final float TEXT_MARGIN = 0.08f;

    private static final int BODY_COLOR = 0xFF2B2B2E;
    private static final int BODY_SIDE_COLOR = 0xFF1E1E20;
    private static final int SCREEN_COLOR = 0xFF060608;

    public ArrivalDisplayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArrivalDisplayBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof ArrivalDisplayBlock)) return;

        Direction facing = state.getValue(ArrivalDisplayBlock.FACING);
        DisplayAttachment attachment = state.getValue(ArrivalDisplayBlock.ATTACHMENT);

        float bottom = ArrivalDisplayBlock.bodyBottom(attachment) / 16f - 0.5f;
        float top = ArrivalDisplayBlock.bodyTop(attachment) / 16f - 0.5f;
        float left = -0.5f;
        float right = left + WIDTH;
        float back;
        float front;
        if (attachment == DisplayAttachment.WALL) {
            back = -0.5f;
            front = back + DEPTH_WALL;
        } else {
            back = -DEPTH_HANGING / 2;
            front = DEPTH_HANGING / 2;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        Matrix4f pose = poseStack.last().pose();

        // Корпус со скруглёнными углами
        float[][] outline = roundedOutline(left, bottom, right, top, BODY_RADIUS);
        drawFlatShape(consumer, pose, outline, front, BODY_COLOR, packedLight);
        drawFlatShape(consumer, pose, outline, back, BODY_COLOR, packedLight);
        drawShapeSides(consumer, pose, outline, back, front, BODY_SIDE_COLOR, packedLight);

        // Подвес к потолку (в клетке основного блока и части)
        if (attachment == DisplayAttachment.CEILING) {
            float rod = 1 / 32f;
            for (float x : new float[]{left + 0.3f, right - 0.3f}) {
                drawBox(consumer, pose, x - rod, top, -rod, x + rod, 0.5f, rod, BODY_SIDE_COLOR, BODY_SIDE_COLOR, packedLight);
            }
        }

        Platform platform = PlatformLocator.findPlatformAt(entity.getBlockPos());
        long platformId = platform == null ? 0 : platform.getId();
        long dayTime = entity.getLevel() == null ? 0 : entity.getLevel().getDayTime();

        drawScreen(poseStack, bufferSource, platformId, dayTime, left, bottom, right, top, front);
        if (attachment == DisplayAttachment.CEILING) {
            // У подвесного табло экран с обеих сторон
            poseStack.pushPose();
            poseStack.translate((left + right) / 2, 0, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-(left + right) / 2, 0, 0);
            drawScreen(poseStack, bufferSource, platformId, dayTime, left, bottom, right, top, front);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void drawScreen(PoseStack poseStack, MultiBufferSource bufferSource, long platformId, long dayTime,
                            float left, float bottom, float right, float top, float front) {
        float z = front + 0.002f;
        float screenLeft = left + BEZEL;
        float screenRight = right - BEZEL;
        float screenBottom = bottom + BEZEL;
        float screenTop = top - BEZEL;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(WHITE));
        drawFlatShape(consumer, poseStack.last().pose(),
                roundedOutline(screenLeft, screenBottom, screenRight, screenTop, BODY_RADIUS - BEZEL), z,
                SCREEN_COLOR, LightTexture.FULL_BRIGHT);

        // Масштаб такой, чтобы раскладка табло помещалась по высоте экрана и не заходила на рамку
        float textScale = Math.min(TEXT_SCALE, (screenTop - screenBottom - 2 * TEXT_MARGIN) / ArrivalBoard.DESIGN_HEIGHT);

        poseStack.pushPose();
        poseStack.translate(screenLeft + TEXT_MARGIN, screenTop - TEXT_MARGIN, z + 0.002f);
        poseStack.scale(textScale, -textScale, textScale);
        float canvasWidth = (screenRight - screenLeft - 2 * TEXT_MARGIN) / textScale;
        float canvasHeight = (screenTop - screenBottom - 2 * TEXT_MARGIN) / textScale;
        ArrivalBoard.render(new SlideCanvas(poseStack, bufferSource, canvasWidth, canvasHeight, 1), platformId, dayTime);
        poseStack.popPose();
    }

    // Контур прямоугольника со скруглёнными углами против часовой стрелки
    private static float[][] roundedOutline(float x0, float y0, float x1, float y1, float radius) {
        radius = Math.max(0, Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2));
        float[][] centers = {{x1 - radius, y0 + radius}, {x1 - radius, y1 - radius}, {x0 + radius, y1 - radius}, {x0 + radius, y0 + radius}};
        float[][] points = new float[4 * (CORNER_SEGMENTS + 1)][];
        int n = 0;
        for (int corner = 0; corner < 4; corner++) {
            // Углы по порядку: правый нижний (от -90°), правый верхний, левый верхний, левый нижний
            double start = Math.toRadians(-90 + 90 * corner);
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                double angle = start + Math.toRadians(90.0 * i / CORNER_SEGMENTS);
                points[n++] = new float[]{
                        centers[corner][0] + (float) Math.cos(angle) * radius,
                        centers[corner][1] + (float) Math.sin(angle) * radius};
            }
        }
        return points;
    }

    // Выпуклая плоская фигура веером от центра; каждый сектор - вырожденный четырёхугольник
    private static void drawFlatShape(VertexConsumer consumer, Matrix4f pose, float[][] outline, float z, int color, int light) {
        float cx = 0;
        float cy = 0;
        for (float[] p : outline) {
            cx += p[0];
            cy += p[1];
        }
        cx /= outline.length;
        cy /= outline.length;
        for (int i = 0; i < outline.length; i++) {
            float[] a = outline[i];
            float[] b = outline[(i + 1) % outline.length];
            vertex(consumer, pose, cx, cy, z, 0, 0, color, light);
            vertex(consumer, pose, a[0], a[1], z, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z, 0, 0, color, light);

            vertex(consumer, pose, cx, cy, z, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z, 0, 0, color, light);
            vertex(consumer, pose, a[0], a[1], z, 0, 0, color, light);
            vertex(consumer, pose, a[0], a[1], z, 0, 0, color, light);
        }
    }

    // Боковая поверхность фигуры между z0 и z1
    private static void drawShapeSides(VertexConsumer consumer, Matrix4f pose, float[][] outline, float z0, float z1, int color, int light) {
        for (int i = 0; i < outline.length; i++) {
            float[] a = outline[i];
            float[] b = outline[(i + 1) % outline.length];
            vertex(consumer, pose, a[0], a[1], z0, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z0, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z1, 0, 0, color, light);
            vertex(consumer, pose, a[0], a[1], z1, 0, 0, color, light);

            vertex(consumer, pose, a[0], a[1], z1, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z1, 0, 0, color, light);
            vertex(consumer, pose, b[0], b[1], z0, 0, 0, color, light);
            vertex(consumer, pose, a[0], a[1], z0, 0, 0, color, light);
        }
    }

    private static void drawBox(VertexConsumer consumer, Matrix4f pose,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                int faceColor, int sideColor, int light) {
        drawQuad(consumer, pose, x0, y0, x1, y1, z1, faceColor, light);   // лицо
        drawQuad(consumer, pose, x0, y0, x1, y1, z0, faceColor, light);   // спина
        // Бока, верх и низ
        drawQuadYZ(consumer, pose, x0, y0, z0, y1, z1, sideColor, light);
        drawQuadYZ(consumer, pose, x1, y0, z0, y1, z1, sideColor, light);
        drawQuadXZ(consumer, pose, y0, x0, z0, x1, z1, sideColor, light);
        drawQuadXZ(consumer, pose, y1, x0, z0, x1, z1, sideColor, light);
    }

    // Все грани рисуются с обеих сторон: RenderType.text отсекает задние грани
    private static void drawQuad(VertexConsumer consumer, Matrix4f pose,
                                 float x0, float y0, float x1, float y1, float z, int color, int light) {
        vertex(consumer, pose, x0, y0, z, 0, 1, color, light);
        vertex(consumer, pose, x1, y0, z, 1, 1, color, light);
        vertex(consumer, pose, x1, y1, z, 1, 0, color, light);
        vertex(consumer, pose, x0, y1, z, 0, 0, color, light);

        vertex(consumer, pose, x0, y1, z, 0, 0, color, light);
        vertex(consumer, pose, x1, y1, z, 1, 0, color, light);
        vertex(consumer, pose, x1, y0, z, 1, 1, color, light);
        vertex(consumer, pose, x0, y0, z, 0, 1, color, light);
    }

    private static void drawQuadYZ(VertexConsumer consumer, Matrix4f pose,
                                   float x, float y0, float z0, float y1, float z1, int color, int light) {
        vertex(consumer, pose, x, y0, z0, 0, 1, color, light);
        vertex(consumer, pose, x, y0, z1, 1, 1, color, light);
        vertex(consumer, pose, x, y1, z1, 1, 0, color, light);
        vertex(consumer, pose, x, y1, z0, 0, 0, color, light);

        vertex(consumer, pose, x, y1, z0, 0, 0, color, light);
        vertex(consumer, pose, x, y1, z1, 1, 0, color, light);
        vertex(consumer, pose, x, y0, z1, 1, 1, color, light);
        vertex(consumer, pose, x, y0, z0, 0, 1, color, light);
    }

    private static void drawQuadXZ(VertexConsumer consumer, Matrix4f pose,
                                   float y, float x0, float z0, float x1, float z1, int color, int light) {
        vertex(consumer, pose, x0, y, z0, 0, 1, color, light);
        vertex(consumer, pose, x1, y, z0, 1, 1, color, light);
        vertex(consumer, pose, x1, y, z1, 1, 0, color, light);
        vertex(consumer, pose, x0, y, z1, 0, 0, color, light);

        vertex(consumer, pose, x0, y, z1, 0, 0, color, light);
        vertex(consumer, pose, x1, y, z1, 1, 0, color, light);
        vertex(consumer, pose, x1, y, z0, 1, 1, color, light);
        vertex(consumer, pose, x0, y, z0, 0, 1, color, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                               float u, float v, int color, int light) {
        consumer.vertex(pose, x, y, z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .uv(u, v)
                .uv2(light)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(ArrivalDisplayBlockEntity entity) {
        return true;
    }
}
