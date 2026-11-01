package com.tr1nity.stationsticker.map;

import org.mtr.core.data.ClientData;
import org.mtr.core.data.Route;
import org.mtr.core.data.Station;
import org.mtr.core.operation.ListDataResponse;
import org.mtr.core.serializer.ReaderBase;
import java.util.*;

// list_data в MTR 4.0.5 отдаёт полный снимок измерения. Используем отдельный Data,
// не изменяя клиентский кэш MTR и не требуя открытия дашборда или HTTP-сервера.
public final class MtrNetworkMapResponse {
    public final NetworkMapData map;

    public MtrNetworkMapResponse(ReaderBase reader) {
        ClientData data = new ClientData();
        new ListDataResponse(reader, data).write();
        data.sync();
        Map<Long, NetworkMapData.Station> stations = new TreeMap<>();
        Map<Long, double[]> platformPositions = new HashMap<>();
        Map<String, NetworkMapData.Edge> edges = new TreeMap<>();
        Map<Integer, NetworkMapData.Line> lines = new TreeMap<>();
        List<Route> routes = new ArrayList<>(data.routes);
        routes.sort(Comparator.comparingLong(Route::getId));
        for (Route route : routes) {
            if (route.getHidden()) continue;
            List<Long> stops = new ArrayList<>();
            for (var stop : route.getRoutePlatforms()) {
                var platform = stop.getPlatform();
                if (platform == null || platform.area == null) continue;
                Station station = platform.area;
                long id = station.getId();
                var center = platform.getMidPosition();
                stations.putIfAbsent(id, new NetworkMapData.Station(id, truncate(station.getName(), 256), center.getX(), center.getZ()));
                if (stops.isEmpty() || stops.get(stops.size() - 1) != id) stops.add(id);
            }
            if (stops.size() < 2) continue;
            // Веб-карта MTR усредняет середины платформ всех видимых маршрутов станции.
            for (var stop : route.getRoutePlatforms()) {
                var platform = stop.getPlatform();
                if (platform == null || platform.area == null) continue;
                var position = platform.getMidPosition();
                var sum = platformPositions.computeIfAbsent(platform.area.getId(), id -> new double[3]);
                sum[0] += position.getX(); sum[1] += position.getZ(); sum[2]++;
            }
            int color = route.getColor() & 0xFFFFFF;
            lines.putIfAbsent(color, new NetworkMapData.Line(color, truncate(route.getRouteNumber(), 64), truncate(route.getName(), 256)));
            for (int i = 1; i < stops.size(); i++) {
                long a = Math.min(stops.get(i - 1), stops.get(i));
                long b = Math.max(stops.get(i - 1), stops.get(i));
                edges.putIfAbsent(a + ":" + b + ":" + color, new NetworkMapData.Edge(a, b, color));
            }
        }
        // Убираем одиночные станции маршрутов, которые не образовали ни одной линии.
        Set<Long> used = new HashSet<>();
        edges.values().forEach(edge -> { used.add(edge.from()); used.add(edge.to()); });
        stations.keySet().retainAll(used);
        stations.replaceAll((id, station) -> {
            var sum = platformPositions.get(id);
            return new NetworkMapData.Station(id, station.name(), sum[0] / sum[2], sum[1] / sum[2]);
        });
        for (Station station : data.stations) {
            if (!used.contains(station.getId())) continue;
            for (Station connected : station.connectedStations) {
                if (!used.contains(connected.getId()) || connected.getId() <= station.getId()) continue;
                edges.putIfAbsent(station.getId() + ":" + connected.getId() + ":transfer",
                        new NetworkMapData.Edge(station.getId(), connected.getId(), -1));
            }
        }
        map = new NetworkMapData(new ArrayList<>(stations.values()), new ArrayList<>(edges.values()), new ArrayList<>(lines.values()));
    }

    private static String truncate(String text, int length) { return text.length() > length ? text.substring(0, length) : text; }
}
