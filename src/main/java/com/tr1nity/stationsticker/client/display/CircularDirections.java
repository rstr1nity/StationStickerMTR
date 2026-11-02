package com.tr1nity.stationsticker.client.display;

import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoutePlatform;
import java.util.*;

/** Используем настройку самого маршрута MTR, а не форму/название линии. */
public final class CircularDirections {
    private CircularDirections() {}
    public static String title(Route.CircularState state) {
        return switch (state) {
            case CLOCKWISE -> "По часовой стрелке|Clockwise";
            case ANTICLOCKWISE -> "Против часовой стрелки|Counterclockwise";
            default -> "";
        };
    }
    public static List<SimplifiedRoutePlatform> nextStops(List<SimplifiedRoutePlatform> stops, int index, boolean circular, int capacity) {
        List<SimplifiedRoutePlatform> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        seen.add(stops.get(index).getStationId());
        int count = circular ? stops.size() - 1 : stops.size() - index - 2;
        for (int offset = 1; offset <= count && result.size() < capacity; offset++) {
            var stop = stops.get((index + offset) % stops.size());
            if (!circular || seen.add(stop.getStationId())) result.add(stop);
        }
        return result;
    }
}
