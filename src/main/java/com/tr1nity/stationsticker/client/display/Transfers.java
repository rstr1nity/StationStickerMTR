package com.tr1nity.stationsticker.client.display;

import net.minecraft.core.BlockPos;
import org.mtr.core.data.Platform;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Пересадки по данным MTR. Берём их из упрощённых маршрутов (SimplifiedRoute): пересадки для каждой
// остановки сервер MTR считает сам, включая станции, чьи области пересекаются. Station.getInterchange...()
// на клиенте в игре вернул пустой список (вероятно, связи станций там не заполняются), поэтому не используется.
public final class Transfers {

    // Если блок стоит вне области станции (например, в переходе), ищем станцию ближайшей платформы в этом радиусе
    private static final double NEAR_STATION_RADIUS = 32;

    public record Transfer(long stationId, String stationName, int color, String routeName, String routeNumber) {
    }

    private Transfers() {
    }

    // Варианты для окна настройки: пересадки станции, где стоит блок, кроме линий ближайшей платформы
    public static List<Transfer> optionsAt(BlockPos pos) {
        List<Transfer> options = new ArrayList<>();
        Platform platform = nearestPlatform(pos);
        if (platform == null || platform.area == null) return options;
        long stationId = platform.area.getId();

        IntArrayList ownColors = new IntArrayList();
        for (SimplifiedRoute route : routes()) {
            if (route.getPlatformIndex(platform.getId()) >= 0) ownColors.add(route.getColor());
        }

        IntArrayList seenColors = new IntArrayList();
        for (SimplifiedRoute route : routes()) {
            for (SimplifiedRoutePlatform stop : route.getPlatforms()) {
                if (stop.getStationId() != stationId) continue;
                // Своя линия этой станции тоже может быть пересадкой для другой платформы
                addOption(options, seenColors, ownColors, route.getColor(), pos, stationId);
                stop.forEach((color, routeNames) -> addOption(options, seenColors, ownColors, color, pos, stationId));
            }
        }
        return options;
    }

    // Выбранная пересадка по сохранённым в блоке станции и цвету линии
    @Nullable
    public static Transfer resolve(long stationId, int color) {
        if (stationId == 0) return null;
        Station station = MinecraftClientData.getInstance().stationIdMap.get(stationId);
        SimplifiedRoute route = routeWithColor(color, stationId);
        if (station == null || route == null) return null;
        return new Transfer(stationId, station.getName(), color, route.getName(), RouteNumbers.forRoute(route));
    }

    private static void addOption(List<Transfer> options, IntArrayList seenColors, IntArrayList ownColors,
                                  int color, BlockPos pos, long stationId) {
        if (ownColors.contains(color) || seenColors.contains(color)) return;
        SimplifiedRoute route = routeWithColor(color, stationId);
        SimplifiedRoutePlatform stop = route == null ? null : nearestStop(route, pos);
        if (stop == null) return;
        seenColors.add(color);
        options.add(new Transfer(stop.getStationId(), stop.getStationName(), color, route.getName(), RouteNumbers.forRoute(route)));
    }

    // Маршрут этого цвета; лучше тот, что проходит через указанную станцию
    @Nullable
    private static SimplifiedRoute routeWithColor(int color, long stationId) {
        SimplifiedRoute any = null;
        for (SimplifiedRoute route : routes()) {
            if (route.getColor() != color) continue;
            for (SimplifiedRoutePlatform stop : route.getPlatforms()) {
                if (stop.getStationId() == stationId) return route;
            }
            if (any == null) any = route;
        }
        return any;
    }

    // Остановка маршрута, ближайшая к блоку: это и есть станция пересадки (та же или соседняя)
    @Nullable
    private static SimplifiedRoutePlatform nearestStop(SimplifiedRoute route, BlockPos pos) {
        SimplifiedRoutePlatform nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (SimplifiedRoutePlatform stop : route.getPlatforms()) {
            Platform platform = MinecraftClientData.getInstance().platformIdMap.get(stop.getPlatformId());
            if (platform == null) continue;
            double distance = distanceSquared(platform, pos);
            if (distance < nearestDistance) {
                nearest = stop;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    // Станция, где стоит блок; вне станций (например, в переходе) - станция ближайшей платформы в радиусе
    @Nullable
    public static Station stationNear(BlockPos pos) {
        Platform platform = nearestPlatform(pos);
        return platform == null ? null : platform.area;
    }

    // Ближайшая платформа станции, где стоит блок; вне станций - ближайшая в радиусе NEAR_STATION_RADIUS
    @Nullable
    private static Platform nearestPlatform(BlockPos pos) {
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        Platform nearest = null;
        double nearestDistance = station == null ? NEAR_STATION_RADIUS * NEAR_STATION_RADIUS : Double.MAX_VALUE;
        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if (station != null && platform.area != station) continue;
            double distance = distanceSquared(platform, pos);
            if (distance < nearestDistance) {
                nearest = platform;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static double distanceSquared(Platform platform, BlockPos pos) {
        double dx = platform.getMidPosition().getX() - pos.getX();
        double dy = platform.getMidPosition().getY() - pos.getY();
        double dz = platform.getMidPosition().getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static Iterable<SimplifiedRoute> routes() {
        return MinecraftClientData.getInstance().simplifiedRoutes;
    }
}
