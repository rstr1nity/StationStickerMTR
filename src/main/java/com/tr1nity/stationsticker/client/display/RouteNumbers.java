package com.tr1nity.stationsticker.client.display;

import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.List;

// Номер линии (поле «номер маршрута» в MTR). В SimplifiedRoute номера нет, а полные маршруты
// (routeIdMap) клиент получает, похоже, только когда открывают дашборд MTR. Поэтому запасной
// источник - прибытия поездов маршрута (ArrivalResponse.getRouteNumber), они приходят всегда.
public final class RouteNumbers {

    private RouteNumbers() {
    }

    public static String forRoute(SimplifiedRoute route) {
        Route full = MinecraftClientData.getInstance().routeIdMap.get(route.getId());
        if (full != null && !full.getRouteNumber().isEmpty()) return full.getRouteNumber();

        // Прибытия на первую и последнюю платформы маршрута: туда его поезда точно приходят
        List<SimplifiedRoutePlatform> stops = route.getPlatforms();
        if (stops.isEmpty()) return "";
        LongArrayList platformIds = LongArrayList.of(stops.get(0).getPlatformId(), stops.get(stops.size() - 1).getPlatformId());
        for (ArrivalResponse arrival : ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds)) {
            if (arrival.getRouteId() == route.getId() && !arrival.getRouteNumber().isEmpty()) {
                return arrival.getRouteNumber();
            }
        }
        return "";
    }

    // Номер по цвету линии: у пересадок известен только цвет
    public static String forColor(int color) {
        for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            if (route.getColor() != color) continue;
            String number = forRoute(route);
            if (!number.isEmpty()) return number;
        }
        return "";
    }
}
