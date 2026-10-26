package com.tr1nity.stationsticker.client.display;

import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Номер линии (поле «номер маршрута» в MTR). В SimplifiedRoute номера нет, а полные маршруты
// (routeIdMap) клиент получает, похоже, только когда открывают дашборд MTR. Поэтому запасной
// источник - прибытия поездов маршрута (ArrivalResponse.getRouteNumber), но только если поезда ходят.
public final class RouteNumbers {

    private RouteNumbers() {
    }

    // Найденные номера по id маршрута: не пропадают, когда поезда встали или дашборд закрыт.
    // Живут до выхода из игры; если номер в MTR поменяли, новый придёт из тех же источников
    private static final Map<Long, String> KNOWN = new HashMap<>();

    public static String forRoute(SimplifiedRoute route) {
        String number = find(route);
        if (!number.isEmpty()) {
            KNOWN.put(route.getId(), number);
            return number;
        }
        return KNOWN.getOrDefault(route.getId(), "");
    }

    private static String find(SimplifiedRoute route) {
        // Полные маршруты: основная копия данных клиента, затем копия дашборда (после его открытия)
        for (MinecraftClientData data : new MinecraftClientData[]{MinecraftClientData.getInstance(), MinecraftClientData.getDashboardInstance()}) {
            Route full = data == null ? null : data.routeIdMap.get(route.getId());
            if (full != null && !full.getRouteNumber().isEmpty()) return full.getRouteNumber();
        }

        // Прибытия на первую и последнюю платформы маршрута: туда его поезда точно приходят.
        // Так номер получает и табло PIDS самого MTR; без поездов этого источника нет
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
