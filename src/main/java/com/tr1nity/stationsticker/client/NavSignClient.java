package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

// Клиентская часть навигационной таблички, вызывается из блока только на клиенте (DistExecutor)
public final class NavSignClient {

    private NavSignClient() {
    }

    public static void openSettings(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof NavSignBlockEntity sign) {
            minecraft.setScreen(new NavSignScreen(sign));
        }
    }
}
