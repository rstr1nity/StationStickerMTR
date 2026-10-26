package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.DirectionSignBlock;
import com.tr1nity.stationsticker.block.ResizableDisplayBlock;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

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

    public static void openDirectionSign(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockState(pos).getBlock() instanceof DirectionSignBlock) {
            minecraft.setScreen(new DirectionSignScreen(pos, minecraft.level.getBlockState(pos).getValue(ResizableDisplayBlock.WIDTH)));
        }
    }
}
