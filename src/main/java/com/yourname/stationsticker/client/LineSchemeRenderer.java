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
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.client.IDrawing;

public class LineSchemeRenderer implements BlockEntityRenderer<LineSchemeEntity> {

    private static final float SCHEME_WIDTH = 4.0f;
    private static final float SCHEME_HEIGHT = 2.0f;
    private static final float SCHEME_Z_OFFSET = 0.005f;

    private static long lastDataVersion = 0;

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

    private Platform findPlatformAt(BlockPos pos) {
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

    private long getDataVersion() {
        return MinecraftClientData.getInstance().simplifiedRoutes.hashCode();
    }

    @Override
    public void render(LineSchemeEntity entity, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        SPBDynamicTextureCache.instance.refresh();

        Level level = entity.getLevel();
        BlockPos pos = entity.getBlockPos();
        BlockState state = entity.getBlockState();

        if (level == null) return;

        Direction facing = state.getValue(LineSchemeBlock.FACING);

        Platform platform = findPlatformAt(pos);
        if (platform == null) return;

        long platformId = platform.getId();

        // Проверяем, изменились ли данные
        long currentVersion = getDataVersion();
        if (currentVersion != lastDataVersion) {
            lastDataVersion = currentVersion;
            SPBDynamicTextureCache.instance.refresh(); // Сбрасываем кэш
        }

        StoredMatrixTransformations transformations = new StoredMatrixTransformations(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
        transformations.add(graphicsHolder -> {
            graphicsHolder.rotateZDegrees(180);
            graphicsHolder.rotateYDegrees(-getRotationAngle(facing));
            graphicsHolder.translate(0, 0, 0.5 - SCHEME_Z_OFFSET);
        });

        var texture = SPBDynamicTextureCache.instance.getRouteMap(platformId, false, false, 1.0f, false);
        if (texture == null) return;

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