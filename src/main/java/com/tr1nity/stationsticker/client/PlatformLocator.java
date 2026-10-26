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

    // Ближайшая платформа станции: у островной платформы пути в нескольких блоках друг от друга,
    // и «первая в радиусе» могла оказаться платформой другого направления
    @Nullable
    public static Platform findPlatformAt(BlockPos pos) {
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        if (station == null) return null;

        Platform nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if (platform.area != station) continue;
            long distance = distanceSquared(platform, pos);
            if (distance < nearestDistance) {
                nearest = platform;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static long distanceSquared(Platform platform, BlockPos pos) {
        long dx = platform.getMidPosition().getX() - pos.getX();
        long dy = platform.getMidPosition().getY() - pos.getY();
        long dz = platform.getMidPosition().getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
