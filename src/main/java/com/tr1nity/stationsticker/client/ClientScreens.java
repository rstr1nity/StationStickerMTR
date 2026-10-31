package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.ResizableDisplayBlock;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

// Окна настройки блоков; вызываются из блоков только на клиенте (DistExecutor)
public final class ClientScreens {

    private ClientScreens() {
    }

    public static void openNavSign(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof NavSignBlockEntity sign) {
            minecraft.setScreen(new NavSignScreen(sign));
        }
    }

    // Ширина экрана ползунком; заголовок окна - название блока
    public static void openWidthSettings(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.getBlock() instanceof ResizableDisplayBlock) {
            minecraft.setScreen(new DisplayWidthScreen(pos, state.getValue(ResizableDisplayBlock.WIDTH),
                    Component.translatable(state.getBlock().getDescriptionId())));
        }
    }
}
