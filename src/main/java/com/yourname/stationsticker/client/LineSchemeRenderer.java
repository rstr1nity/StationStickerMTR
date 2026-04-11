package com.yourname.stationsticker.client;

import com.yourname.stationsticker.block.LineSchemeBlock;
import com.yourname.stationsticker.block.entity.LineSchemeEntity;
// Убедитесь, что импортируете ваш класс кэша
// import com.yourname.stationsticker.client.SPBDynamicTextureCache;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.client.IDrawing;
import org.mtr.mapping.holder.Identifier;



public class LineSchemeRenderer implements BlockEntityRenderer<LineSchemeEntity> {

    private static final float SCHEME_WIDTH = 4.0f;
    private static final float SCHEME_HEIGHT = 1.5f;
    private static final float SCHEME_Z_OFFSET = 0.005f;

    public LineSchemeRenderer(BlockEntityRendererProvider.Context context) {
    }

    private float getRotationAngle(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
    }

    // Этот метод можно оставить, он нужен для поиска
    private Platform findPlatformAt(BlockPos pos) {
        // ... ваш код поиска платформы без изменений ...
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        if (station == null) return null;

        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if (platform.area == station && isPlatformAtPosition(platform, pos)) {
                return platform;
            }
        }
        return null;
    }

    private boolean isPlatformAtPosition(Platform platform, BlockPos pos) {
        return Math.abs(platform.getMidPosition().getX() - pos.getX()) < 10 &&
                Math.abs(platform.getMidPosition().getZ() - pos.getZ()) < 10;
    }


    @Override
    public void render(LineSchemeEntity entity, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        Level level = entity.getLevel();
        if (level == null) return;

        BlockPos pos = entity.getBlockPos();
        BlockState state = entity.getBlockState();

        Direction facing = state.getValue(LineSchemeBlock.FACING);

        Platform platform = findPlatformAt(pos);
        if (platform == null) return;

        long platformId = platform.getId();

        int arrowDir = state.getValue(LineSchemeBlock.ARROW_DIRECTION);
        boolean flip = (arrowDir == 2);
        var texture = SPBDynamicTextureCache.instance.getRouteMap(platformId, false, flip, SCHEME_WIDTH / SCHEME_HEIGHT, false);
        if (texture == null) return;

        StoredMatrixTransformations transformations = new StoredMatrixTransformations(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
        transformations.add(graphicsHolder -> {
            graphicsHolder.rotateZDegrees(180);
            graphicsHolder.rotateYDegrees(-getRotationAngle(facing));
            graphicsHolder.translate(0, 0, 0.5 - 0.081f);
        });

        MainRenderer.scheduleRender(texture.identifier, false, QueuedRenderLayer.EXTERIOR,
                (graphicsHolder, offset) -> {
                    transformations.transform(graphicsHolder, offset);
                    IDrawing.drawTexture(graphicsHolder,
                            -SCHEME_WIDTH / 2, -SCHEME_HEIGHT / 2,
                            SCHEME_WIDTH, SCHEME_HEIGHT,
                            0, 0, 1, 1,
                            org.mtr.mapping.holder.Direction.convert(facing),
                            0xFFFFFFFF,
                            packedLight);

                    graphicsHolder.pop();
                }
        );

        // --- ОТРИСОВКА ПОЛНОЦЕННОГО 3D-КОРПУСА ---
        MainRenderer.scheduleRender(new Identifier("mtr", "textures/block/white.png"), false, QueuedRenderLayer.EXTERIOR,
                (graphicsHolder, offset) -> {
                    StoredMatrixTransformations frameTransformations = new StoredMatrixTransformations(
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
                    );
                    frameTransformations.add(gh -> {
                        gh.rotateZDegrees(180);
                        gh.rotateYDegrees(-getRotationAngle(facing));
                        gh.translate(0, 0, 0.5); // Прижимаем корпус к стене
                    });
                    frameTransformations.transform(graphicsHolder, offset);

                    float W = SCHEME_WIDTH;
                    float H = SCHEME_HEIGHT;
                    float border = 0.05f;      // Толщина рамки (спереди)
                    float totalDepth = 0.1f;   // Насколько весь стенд выпирает от стены
                    float screenDepth = 0.08f; // Глубина, на которой лежит экран (он утоплен)

                    int frameColor = 0xFF505050; // Серый цвет корпуса
                    int backColor = 0xFF101010;  // Черный фон-подложка под экраном

                    // 1. Черный фон для экрана
                    graphicsHolder.push();
                    graphicsHolder.translate(0, 0, -screenDepth);
                    IDrawing.drawTexture(graphicsHolder, -W/2, -H/2, W, H, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, backColor, packedLight);
                    graphicsHolder.pop();

                    // 2. Передние рамки (выступают вперед)
                    graphicsHolder.push();
                    graphicsHolder.translate(0, 0, -totalDepth);
                    IDrawing.drawTexture(graphicsHolder, -W/2, -H/2, border, H, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight); // Левая
                    IDrawing.drawTexture(graphicsHolder, W/2 - border, -H/2, border, H, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight); // Правая
                    IDrawing.drawTexture(graphicsHolder, -W/2 + border, -H/2, W - border*2, border, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight); // Верхняя
                    IDrawing.drawTexture(graphicsHolder, -W/2 + border, H/2 - border, W - border*2, border, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight); // Нижняя
                    graphicsHolder.pop();

                    // 3. Внешние боковые стенки (придают объем сбоку, сверху и снизу)
                    // Левая стенка
                    graphicsHolder.push();
                    graphicsHolder.translate(-W/2, 0, 0);
                    graphicsHolder.rotateYDegrees(90);
                    IDrawing.drawTexture(graphicsHolder, 0, -H/2, totalDepth, H, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight);
                    graphicsHolder.pop();

                    // Правая стенка
                    graphicsHolder.push();
                    graphicsHolder.translate(W/2, 0, 0);
                    graphicsHolder.rotateYDegrees(-90);
                    IDrawing.drawTexture(graphicsHolder, -totalDepth, -H/2, totalDepth, H, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight);
                    graphicsHolder.pop();

                    // Верхняя стенка
                    graphicsHolder.push();
                    graphicsHolder.translate(0, -H/2, 0);
                    graphicsHolder.rotateXDegrees(-90);
                    IDrawing.drawTexture(graphicsHolder, -W/2, 0, W, totalDepth, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight);
                    graphicsHolder.pop();

                    // Нижняя стенка
                    graphicsHolder.push();
                    graphicsHolder.translate(0, H/2, 0);
                    graphicsHolder.rotateXDegrees(90);
                    IDrawing.drawTexture(graphicsHolder, -W/2, -totalDepth, W, totalDepth, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight);
                    graphicsHolder.pop();


                    graphicsHolder.push();
                    // Поворачиваем ее на 180 градусов, чтобы она "смотрела" назад
                    graphicsHolder.rotateYDegrees(180);
                    // Рисуем плоскость, которая закрывает всю заднюю часть стенда
                    // frameColor или backColor - на ваш выбор
                    IDrawing.drawTexture(graphicsHolder, -W/2, -H/2, W, H, 0, 0, 1, 1, org.mtr.mapping.holder.Direction.UP, frameColor, packedLight);
                    graphicsHolder.pop();

                    graphicsHolder.pop();
                }
        );

        // --- ОТРИСОВКА ПАСХАЛКИ (На задней стенке) ---
        // Укажите ваш MOD_ID и путь к картинке
        Identifier easterEggTexture = new Identifier("stationsticker", "textures/block/sticker.png");

        MainRenderer.scheduleRender(easterEggTexture, false, QueuedRenderLayer.EXTERIOR,
                (graphicsHolder, offset) -> {
                    StoredMatrixTransformations eggTransformations = new StoredMatrixTransformations(
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
                    );
                    eggTransformations.add(gh -> {
                        gh.rotateZDegrees(180);
                        gh.rotateYDegrees(-getRotationAngle(facing));
                        gh.translate(0, 0, 0.5); // Идем к задней стенке корпуса
                        gh.rotateYDegrees(180);  // Разворачиваемся, чтобы смотреть назад
                        gh.translate(0, 0, -0.001f); // Выдвигаем на 1 миллиметр, чтобы не мерцало с серой стеной
                    });

                    eggTransformations.transform(graphicsHolder, offset);

                    // Размеры вашей картинки (1.0f = 1 блок)
                    float eggWidth = 1.0f;
                    float eggHeight = 1.0f;

                    // Позиция картинки на задней стенке.
                    // 0 и 0 - это ровно по центру таблички.
                    // Меняйте эти числа, чтобы сдвинуть картинку (например, offsetX = 1.0f сдвинет вправо)
                    float offsetX = 0.0f;
                    float offsetY = 0.0f;

                    // Отрисовка (используем ARGB_WHITE, чтобы картинка сохранила свои оригинальные цвета)
                    int whiteColor = 0xFFFFFFFF;
                    IDrawing.drawTexture(graphicsHolder,
                            -eggWidth / 2 + offsetX, -eggHeight / 2 + offsetY,
                            eggWidth, eggHeight,
                            0, 0, 1, 1,
                            org.mtr.mapping.holder.Direction.UP, whiteColor, packedLight);

                    graphicsHolder.pop();
                }
        );


    }

    @Override
    public boolean shouldRenderOffScreen(LineSchemeEntity entity) {
        return true;
    }
}
