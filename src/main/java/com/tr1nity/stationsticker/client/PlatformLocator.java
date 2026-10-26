package com.tr1nity.stationsticker.client;

import net.minecraft.core.BlockPos;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nullable;

// Поиск платформы MTR по позиции блока: общий для схемы линии и экранов
public final class PlatformLocator {

    private PlatformLocator() {
    }

    @Nullable
    public static Platform findPlatformAt(BlockPos pos) {
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        if (station == null) return null;

        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if (platform.area == station && isPlatformAtPosition(platform, pos)) {
                return platform;
            }
        }
        return null;
    }

    private static boolean isPlatformAtPosition(Platform platform, BlockPos pos) {
        return Math.abs(platform.getMidPosition().getX() - pos.getX()) < 10 &&
                Math.abs(platform.getMidPosition().getZ() - pos.getZ()) < 10;
    }
}
