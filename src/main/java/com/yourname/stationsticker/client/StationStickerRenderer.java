package com.yourname.stationsticker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourname.stationsticker.block.StationStickerBlock;
import com.yourname.stationsticker.block.entity.StationStickerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.core.data.Station;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.core.data.Position;

public class StationStickerRenderer implements BlockEntityRenderer<StationStickerBlockEntity> {
    private static final float STICKER_WIDTH = 0.5f;
    private static final float STICKER_HEIGHT = 0.2f;
    private static final float STICKER_DEPTH = 0.01f;

    public StationStickerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StationStickerBlockEntity entity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        Level level = entity.getLevel();
        BlockPos pos = entity.getBlockPos();
        BlockState state = entity.getBlockState();

        if (level == null) return;

        Direction facing = state.getValue(StationStickerBlock.FACING);
        Station station = findStationAt(pos);

        if (station == null) return;

        String stationName = station.getName();
        int stationColor = station.getColor();

        renderSticker(poseStack, bufferSource, facing,
                stationName, stationColor, packedLight);
    }

    private Station findStationAt(BlockPos pos) {
        return MinecraftClientData.getInstance().stations.stream()
                .filter(station -> station.inArea(new Position(pos.getX(), pos.getY(), pos.getZ())))
                .findFirst()
                .orElse(null);
    }

    private void renderSticker(PoseStack poseStack, MultiBufferSource bufferSource,
                               Direction facing, String stationName,
                               int stationColor, int packedLight) {

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Direction.UP.getRotation());
        poseStack.mulPose(facing.getOpposite().getRotation());
        poseStack.translate(0, 0, 0.5 - STICKER_DEPTH / 2);

        renderColoredRect(poseStack, bufferSource,
                -STICKER_WIDTH/2, -STICKER_HEIGHT/2,
                STICKER_WIDTH, STICKER_HEIGHT,
                stationColor | 0xFF000000,
                packedLight);

        renderText(poseStack, bufferSource, stationName, packedLight);

        poseStack.popPose();
    }

    private void renderColoredRect(PoseStack poseStack, MultiBufferSource bufferSource,
                                   float x, float y, float width, float height,
                                   int color, int light) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());

        for (int side = 0; side < 2; side++) {
            float zOffset = side == 0 ? 0 : STICKER_DEPTH;

            vertexConsumer.vertex(poseStack.last().pose(), x, y, zOffset)
                    .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                    .uv(0, 0).uv2(light).normal(0, 0, 1).endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x + width, y, zOffset)
                    .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                    .uv(1, 0).uv2(light).normal(0, 0, 1).endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x + width, y + height, zOffset)
                    .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                    .uv(1, 1).uv2(light).normal(0, 0, 1).endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x, y + height, zOffset)
                    .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                    .uv(0, 1).uv2(light).normal(0, 0, 1).endVertex();
        }
    }

    private void renderText(PoseStack poseStack, MultiBufferSource bufferSource,
                            String text, int light) {
        if (text == null || text.isEmpty()) return;

        Minecraft.getInstance().font.drawInBatch(
                text,
                -Minecraft.getInstance().font.width(text) / 2.0f,
                -4,
                0xFFFFFF,
                false,
                poseStack.last().pose(),
                bufferSource,
                false,
                0,
                light
        );
    }

    @Override
    public boolean shouldRenderOffScreen(StationStickerBlockEntity entity) {
        return true;
    }
}