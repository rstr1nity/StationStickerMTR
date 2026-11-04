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
        Station station = findStationAt(pos);
        if (station == null) return null;

        return findPlatformIn(MinecraftClientData.getInstance().platforms,station,pos);
    }
    public static Platform findPlatformIn(java.util.Collection<Platform> platforms,Station station,BlockPos pos){
        Platform nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (Platform platform : platforms) {
            if (platform.area == null || platform.area.getId() != station.getId()) continue;
            long distance = distanceSquared(platform, pos);
            if (distance < nearestDistance) {
                nearest = platform;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    @Nullable
    public static Station findStationAt(BlockPos pos) {
        // Sample the occupied column of a three-block stand and the one-block sign margin.
        // No unbounded nearest-station fallback: do not silently select another station.
        for(int y:new int[]{0,1,2,3,-1})for(int[] offset:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1}}){
            var station=InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX()+offset[0],pos.getY()+y,pos.getZ()+offset[1]));
            if(station!=null)return station;
        }
        return null;
    }
    private static long distanceSquared(Platform platform, BlockPos pos) {
        long dx = platform.getMidPosition().getX() - pos.getX();
        long dy = platform.getMidPosition().getY() - pos.getY();
        long dz = platform.getMidPosition().getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
