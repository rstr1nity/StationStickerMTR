package com.yourname.stationsticker.client;

import com.yourname.stationsticker.block.LineSchemeBlock;
import com.yourname.stationsticker.block.entity.LineSchemeEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.DynamicTextureCache;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.client.IDrawing;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;

public class LineSchemeRenderer implements BlockEntityRenderer<LineSchemeEntity> {

    private static final float SCHEME_WIDTH = 4.0f;
    private static final float SCHEME_HEIGHT = 2.0f;
    private static final float SCHEME_Z_OFFSET = 0.005f;

    public LineSchemeRenderer(BlockEntityRendererProvider.Context context) {
    }

    private float getRotationAngle(Direction facing) {
        switch (facing) {
            case NORTH: return 0;
            case EAST: return -90;
            case SOUTH: return 180;
            case WEST: return 90;
            default: return 0;
        }
    }

    // Поиск платформы по координатам через станцию
    private Platform findPlatformAt(BlockPos pos) {
        // Сначала находим станцию
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        if (station == null) return null;

        // Теперь нужно найти платформу этой станции, которая находится в зоне блока
        // Используем метод из MinecraftClientData для поиска платформы по координатам
        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if (platform.area == station && isPlatformAtPosition(platform, pos)) {
                return platform;
            }
        }
        return null;
    }

    // Проверка, находится ли позиция в зоне платформы
    private boolean isPlatformAtPosition(Platform platform, BlockPos pos) {
        // Простая проверка — платформа обычно находится рядом с блоком
        // В MTR платформы имеют зону, но мы используем упрощённую проверку
        return Math.abs(platform.getMidPosition().getX() - pos.getX()) < 10 &&
                Math.abs(platform.getMidPosition().getZ() - pos.getZ()) < 10;
    }

    @Override
    public void render(LineSchemeEntity entity, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        Level level = entity.getLevel();
        BlockPos pos = entity.getBlockPos();
        BlockState state = entity.getBlockState();

        if (level == null) return;

        Direction facing = state.getValue(LineSchemeBlock.FACING);

        // Пробуем найти платформу
        Platform platform = findPlatformAt(pos);

        if (platform == null) return;

        long platformId = platform.getId();

        // Трансформации как в MTR
        StoredMatrixTransformations transformations = new StoredMatrixTransformations(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
        transformations.add(graphicsHolder -> {
            graphicsHolder.rotateZDegrees(180);
            graphicsHolder.rotateYDegrees(-getRotationAngle(facing));
            graphicsHolder.translate(0, 0, 0.5 - SCHEME_Z_OFFSET);// ← ИСПРАВЛЯЕМ ПЕРЕВЁРНУТЫЙ ТЕКСТ
        });


        // Генерируем схему линии через MTR
        var texture = DynamicTextureCache.instance.getRouteMap(platformId, false, false, 1.0f, false);

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
    }

    @Override
    public boolean shouldRenderOffScreen(LineSchemeEntity entity) {
        return true;
    }
}
