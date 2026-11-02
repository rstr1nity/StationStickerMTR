package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** Подписи выбираются совместно: тесная станция может переставить уже занятые подписи. */
public final class NetworkMapLabelLayout {
    private record Option(List<String> rows, Rectangle2D box, int size, double cost) {}
    private final Map<Long, List<Option>> options = new LinkedHashMap<>();
    private final Map<Long, Option> chosen = new LinkedHashMap<>();
    private int attempts;
    private boolean minimumOnly;
    private NetworkMapLabelLayout() {}

    public static List<NetworkMapSymbols.Label> place(NetworkMapData data, Map<Long, Point2D.Double> points,
            NetworkMapFont font, boolean russian, List<Rectangle2D> fixed, List<Shape> paths, int bottom) {
        return place(data, points, font, russian, fixed, paths, bottom, 0);
    }
    public static List<NetworkMapSymbols.Label> place(NetworkMapData data, Map<Long, Point2D.Double> points,
            NetworkMapFont font, boolean russian, List<Rectangle2D> fixed, List<Shape> paths, int bottom, long current) {
        var solver = new NetworkMapLabelLayout();
        solver.minimumOnly = data.stations().size() > 60;
        List<Rectangle2D> pathBounds = paths.stream().map(Shape::getBounds2D).toList();
        for (var station : data.stations()) {
            var point = points.get(station.id());
            List<Option> candidates = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int size : new int[]{16, 8}) {
                font.size(size);
                for (int wrap : size == 8 ? new int[]{160, 110, 75, 50} : new int[]{190, 120}) {
                    var rows = NetworkMapSymbols.wrap(NetworkMapPainter.name(station.name(), russian), font, wrap);
                    int width = rows.stream().mapToInt(font::width).max().orElse(0) + 4;
                    int height = rows.size() * (font.height() + 1) + 4;
                    if (station.id() == current) {
                        width = Math.max(width + (int) Math.ceil(font.boldOffset()), font.width(NetworkMapSymbols.youAreHere(russian)) + 10);
                        height += font.height() + 8;
                    }
                    // Сдвиги вдоль края подписи позволяют использовать узкие промежутки внутри сети.
                    for (int gap : new int[]{9, 16, 26, 42, 64, 90}) for (double align : new double[]{0, 0.25, 0.5, 0.75, 1}) {
                        double[][] positions = {{point.x + gap, point.y - height * align}, {point.x - gap - width, point.y - height * align},
                                {point.x - width * align, point.y - gap - height}, {point.x - width * align, point.y + gap}};
                        for (var position : positions) {
                            var box = new Rectangle2D.Double(Math.rint(position[0]), Math.rint(position[1]), width, height);
                            String key = size + ":" + rows + ":" + box.x + ":" + box.y;
                            if (!seen.add(key) || !inside(box, bottom) || fixed.stream().anyMatch(box::intersects)) continue;
                            boolean blocked = false;
                            for (int i = 0; i < paths.size(); i++) if (pathBounds.get(i).intersects(box) && paths.get(i).intersects(box)) { blocked = true; break; }
                            if (blocked) continue;
                            double distance = distance(point, box);
                            candidates.add(new Option(rows, box, size, distance + rows.size() * 3 + (size == 8 && data.stations().size() <= 60 ? 20 : 0)));
                        }
                    }
                }
            }
            candidates.sort(Comparator.comparingDouble(Option::cost));
            solver.options.put(station.id(), candidates.stream().limit(600).toList());
        }
        var order = new ArrayList<>(solver.options.keySet());
        order.sort(Comparator.<Long>comparingInt(id -> solver.options.get(id).size()).thenComparingLong(id -> id));
        for (long id : order) { solver.attempts = 0; solver.assign(id, 0, new HashSet<>()); }
        for (int pass = 0; pass < 3; pass++) for (long id : order) if (!solver.chosen.containsKey(id)) {
            solver.attempts = 0; solver.assign(id, 3, new HashSet<>());
        }
        // Никакую станцию не скрываем: для действительно неразрешённого узла остаётся выноска.
        List<Rectangle2D> occupied = new ArrayList<>(fixed);
        solver.chosen.values().forEach(option -> occupied.add(option.box()));
        List<NetworkMapSymbols.Label> result = new ArrayList<>();
        for (var station : data.stations()) {
            var option = solver.chosen.get(station.id());
            if (option == null) {
                font.size(8);
                var rows = NetworkMapSymbols.wrap(NetworkMapPainter.name(station.name(), russian), font, 90);
                int width = rows.stream().mapToInt(font::width).max().orElse(0);
                int height = rows.size() * (font.height() + 1);
                if (station.id() == current) {
                    width = Math.max(width + (int) Math.ceil(font.boldOffset()), font.width(NetworkMapSymbols.youAreHere(russian)) + 6);
                    height += font.height() + 8;
                }
                var placement = NetworkMapLabels.place(points.get(station.id()), width, height, occupied, paths, bottom);
                option = new Option(rows, placement.box(), 8, 0); occupied.add(option.box());
            }
            result.add(new NetworkMapSymbols.Label(station.id(), option.rows(), option.box(), option.size(), distance(points.get(station.id()), option.box()) > 28));
        }
        // Сначала всем обеспечиваем близкое место мелким шрифтом; затем увеличиваем
        // подписи там, где это не отнимает место у остальных станций.
        for (int i = 0; i < result.size(); i++) {
            var label = result.get(i);
            if (label.size() >= 16) continue;
            long station = label.station();
            occupied.remove(label.box());
            var replacement = solver.options.get(station).stream()
                    .filter(option -> option.size() == 16 && distance(points.get(station), option.box()) <= 28)
                    .filter(option -> occupied.stream().noneMatch(box -> box.intersects(option.box())))
                    .min(Comparator.comparingDouble(Option::cost)).orElse(null);
            if (replacement != null) label = new NetworkMapSymbols.Label(label.station(), replacement.rows(), replacement.box(), replacement.size(), false);
            result.set(i, label); occupied.add(label.box());
        }
        return result;
    }

    private boolean assign(long id, int depth, Set<Long> locked) {
        if (!locked.add(id)) return false;
        for (var option : options.get(id)) {
            if (minimumOnly && option.size() != 8) continue;
            if (++attempts > 4000) break;
            List<Long> conflicts = new ArrayList<>();
            for (var entry : chosen.entrySet()) if (entry.getKey() != id && entry.getValue().box().intersects(option.box())) conflicts.add(entry.getKey());
            if (conflicts.isEmpty()) { chosen.put(id, option); locked.remove(id); return true; }
            if (depth == 0 || conflicts.size() > 3 || conflicts.stream().anyMatch(locked::contains)) continue;
            var previous = new LinkedHashMap<>(chosen);
            conflicts.forEach(chosen::remove); chosen.put(id, option);
            boolean success = true;
            for (long other : conflicts) if (!assign(other, depth - 1, locked)) { success = false; break; }
            if (success) { locked.remove(id); return true; }
            chosen.clear(); chosen.putAll(previous);
        }
        locked.remove(id); return false;
    }
    public static double distance(Point2D.Double p, Rectangle2D box) {
        return p.distance(Math.max(box.getMinX(), Math.min(p.x, box.getMaxX())), Math.max(box.getMinY(), Math.min(p.y, box.getMaxY())));
    }
    private static boolean inside(Rectangle2D box, int bottom) {
        if (box.getMinY() < 290 || box.getMaxY() > bottom) return false;
        for (double x : new double[]{box.getMinX(), box.getMaxX()}) for (double y : new double[]{box.getMinY(), box.getMaxY()})
            if (Point2D.distance(x, y, 1024, 1024) > 940) return false;
        return true;
    }
}
