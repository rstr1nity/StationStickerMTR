package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.geom.Point2D;
import java.util.*;

/** Общая монотонная деформация координат: узлы всех линий преобразуются одинаково. */
public final class NetworkMapSpacing {
    private NetworkMapSpacing() {}

    public static Map<Long, Point2D.Double> coordinates(NetworkMapData data, boolean compact) {
        return coordinates(data, compact ? 1 : 0);
    }
    public static Map<Long, Point2D.Double> coordinates(NetworkMapData data, int strength) {
        Map<Long, Point2D.Double> result = new LinkedHashMap<>();
        Map<Double, Double> xs = axis(data.stations().stream().map(NetworkMapData.Station::x).toList(), strength);
        Map<Double, Double> zs = axis(data.stations().stream().map(NetworkMapData.Station::z).toList(), strength);
        for (var station : data.stations()) result.put(station.id(), new Point2D.Double(xs.get(station.x()), zs.get(station.z())));
        return result;
    }

    private static Map<Double, Double> axis(List<Double> values, int strength) {
        List<Double> sorted = values.stream().distinct().sorted().toList();
        Map<Double, Double> result = new HashMap<>();
        if (sorted.isEmpty()) return result;
        if (strength == 0 || sorted.size() < 3) {
            sorted.forEach(value -> result.put(value, value));
            return result;
        }
        // Широкие пустоты сжимаются; плотные полосы получают больше места. Маленький
        // разброс координат платформ не превращается в большой ступенчатый сдвиг.
        double span = sorted.get(sorted.size() - 1) - sorted.get(0);
        double band = Math.max(1, span / Math.max(6, Math.sqrt(values.size())));
        double position = 0;
        result.put(sorted.get(0), position);
        for (int i = 1; i < sorted.size(); i++) {
            double gap = sorted.get(i) - sorted.get(i - 1);
            double middle = sorted.get(i - 1) + gap / 2;
            double density = 0;
            for (double value : values) {
                double distance = (value - middle) / band;
                density += Math.exp(-distance * distance / 2);
            }
            double shortened = gap > band ? (strength > 1 ? band : band * (1 + Math.log(gap / band))) : gap;
            position += shortened * (1 + Math.min(strength > 1 ? 8 : 5, (strength > 1 ? 6 : 3) * density / Math.sqrt(values.size())));
            result.put(sorted.get(i), position);
        }
        return result;
    }
}
