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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.core.data.Station;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.core.data.Position;
import com.mojang.math.Vector3f;

public class StationStickerRenderer implements BlockEntityRenderer<StationStickerBlockEntity> {

    private static final float STICKER_WIDTH = 0.5f;
    private static final float STICKER_HEIGHT = 0.2f;
    private static final float STICKER_DEPTH = 0.01f;

    // ТВОЯ ТЕКСТУРА
    private static final ResourceLocation STICKER_TEXTURE =
            new ResourceLocation("stationsticker", "textures/block/station_sticker.png");

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

        int[] lineInfo = getLineInfo(level, pos, facing);
        int lineLength = lineInfo[0];
        int indexInLine = lineInfo[1];
        int desiredSpacing = 8;

        int spacing = Math.max(1, desiredSpacing);



        Station station = findStationAt(pos);

        if (station == null) return;

        String stationName = station.getName();
        int stationColor = station.getColor();

        renderSticker(poseStack, bufferSource, facing,
                stationName, stationColor, packedLight,
                lineLength, indexInLine);
    }

    private Station findStationAt(BlockPos pos) {
        return MinecraftClientData.getInstance().stations.stream()
                .filter(station -> station.inArea(new Position(pos.getX(), pos.getY(), pos.getZ())))
                .findFirst()
                .orElse(null);
    }
    private int[] getLineInfo(Level level, BlockPos pos, Direction facing) {

        Direction dir1;
        Direction dir2;

        // Определяем ось линии
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            dir1 = Direction.WEST;
            dir2 = Direction.EAST;
        } else {
            dir1 = Direction.NORTH;
            dir2 = Direction.SOUTH;
        }

        int length = 1;
        int index = 0;

        // Идём назад
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        checkPos.set(pos);

        for (int i = 0; i < 128; i++) {
            checkPos.move(dir1);

            if (!(level.getBlockState(checkPos).getBlock() instanceof StationStickerBlock)) {
                break;
            }

            length++;
            index++;
        }

        // Идём вперёд
        checkPos.set(pos);

        for (int i = 0; i < 128; i++) {
            checkPos.move(dir2);

            if (!(level.getBlockState(checkPos).getBlock() instanceof StationStickerBlock)) {
                break;
            }

            length++;
        }

        return new int[]{length, index};
    }
    private void renderSticker(PoseStack poseStack, MultiBufferSource bufferSource,
                               Direction facing, String stationName,
                               int stationColor, int packedLight,
                               int lineLength, int indexInLine) {

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.501);

        if (facing == Direction.UP) {
            poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
            poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
            poseStack.translate(0, 0.375f, 0);
        } else if (facing == Direction.DOWN) {
            poseStack.translate(0, -0.01f, 0);
        } else {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180));
            poseStack.translate(0, 0, -0.379f);
        }
        // Полоска цвета станции сверху
        renderColoredRect(
                poseStack,
                bufferSource,
                -0.5f,
                -0.15f,
                1.0f,
                0.325f,
                stationColor | 0xFF000000,
                packedLight,
                -0.01f
        );


        poseStack.pushPose();

        if (facing == Direction.UP) {
            poseStack.scale(0.025f, -0.025f, 0.025f);
        } else {
            poseStack.scale(-0.025f, 0.025f, 0.025f);
        }
        int desiredSpacing = 8;
        int textCount = Math.max(1, lineLength / desiredSpacing);
        int spacing = Math.max(1, lineLength / textCount);
        if (indexInLine % spacing == 0) {
            renderText(poseStack, bufferSource, stationName, packedLight);
        }

        poseStack.popPose();
        poseStack.popPose();
    }


    private void renderTexturedRect(PoseStack poseStack, MultiBufferSource bufferSource,
                                    float x, float y, float width, float height,
                                    int color, int light) {

        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.text(STICKER_TEXTURE)
        );

        float zOffset = -0.1f;

            vertexConsumer.vertex(poseStack.last().pose(), x, y, zOffset)
                    .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                    .uv(0, 0)
                    .uv2(light)
                    .normal(0, 0, 1)
                    .endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x + width, y, zOffset)
                    .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                    .uv(1, 0)
                    .uv2(light)
                    .normal(0, 0, 1)
                    .endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x + width, y + height, zOffset)
                    .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                    .uv(1, 1)
                    .uv2(light)
                    .normal(0, 0, 1)
                    .endVertex();

            vertexConsumer.vertex(poseStack.last().pose(), x, y + height, zOffset)
                    .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                    .uv(0, 1)
                    .uv2(light)
                    .normal(0, 0, 1)
                    .endVertex();

    }

    private void renderText(PoseStack poseStack, MultiBufferSource bufferSource,
                            String text, int light) {

        if (text == null || text.isEmpty()) return;

        float width = Minecraft.getInstance().font.width(text);

        Minecraft.getInstance().font.drawInBatch(
                text,
                -width / 2.0f,
                -4.0f,
                0xFFFFFF,
                false,
                poseStack.last().pose(),
                bufferSource,
                false,
                0,
                light
        );
    }
    private void renderColoredRect(PoseStack poseStack, MultiBufferSource bufferSource,
                                   float x, float y, float width, float height,
                                   int color, int light, float zOffset) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.text(
                new ResourceLocation("textures/misc/white.png")));

        vertexConsumer.vertex(poseStack.last().pose(), x, y, zOffset)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .uv(0, 0)
                .uv2(light)
                .endVertex();

        vertexConsumer.vertex(poseStack.last().pose(), x + width, y, zOffset)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .uv(1, 0)
                .uv2(light)
                .endVertex();

        vertexConsumer.vertex(poseStack.last().pose(), x + width, y + height, zOffset)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .uv(1, 1)
                .uv2(light)
                .endVertex();

        vertexConsumer.vertex(poseStack.last().pose(), x, y + height, zOffset)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .uv(0, 1)
                .uv2(light)
                .endVertex();
    }
    @Override
    public boolean shouldRenderOffScreen(StationStickerBlockEntity entity) {
        return true;
    }
}