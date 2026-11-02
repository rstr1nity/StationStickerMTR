package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.geom.Point2D;
import java.util.*;

/** Второй режим: кольца — опоры, промежуточные станции выстраиваются между узлами. */
public final class NetworkMapSchematic {
    public record Ring(int color, List<Long> stations) {}
    private NetworkMapSchematic() {}
    public static java.awt.geom.Ellipse2D circle(Ring ring, Map<Long, Point2D.Double> points) {
        double cx = ring.stations().stream().mapToDouble(id -> points.get(id).x).average().orElse(0);
        double cy = ring.stations().stream().mapToDouble(id -> points.get(id).y).average().orElse(0);
        double radius = points.get(ring.stations().get(0)).distance(cx, cy);
        if (ring.stations().stream().anyMatch(id -> Math.abs(points.get(id).distance(cx, cy) - radius) > 0.01)) return null;
        return new java.awt.geom.Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    public static List<Ring> rings(NetworkMapData data) {
        List<Ring> result = new ArrayList<>();
        var graph = NetworkMapGeometry.graph(data);
        for (var line : data.lines()) {
            if (!line.circular()) continue;
            var nodes = graph.getOrDefault(line.color(), Map.of());
            Set<Long> visited = new HashSet<>();
            for (long start : nodes.keySet()) {
                if (visited.contains(start)) continue;
                List<Long> component = new ArrayList<>();
                Deque<Long> queue = new ArrayDeque<>(); queue.add(start); visited.add(start);
                while (!queue.isEmpty()) {
                    long id = queue.removeFirst(); component.add(id);
                    for (long other : nodes.get(id)) if (visited.add(other)) queue.add(other);
                }
                // Разветвлённый компонент не выдаём за простое кольцо.
                if (component.size() < 3 || component.stream().anyMatch(id -> nodes.get(id).size() != 2)) continue;
                List<Long> order = new ArrayList<>();
                long previous = Long.MIN_VALUE, id = start;
                do {
                    order.add(id); long next = nodes.get(id).iterator().next();
                    for (long candidate : nodes.get(id)) if (candidate != previous) { next = candidate; break; }
                    previous = id; id = next;
                } while (id != start && order.size() <= component.size());
                if (id == start) result.add(new Ring(line.color(), List.copyOf(order)));
            }
        }
        result.sort(Comparator.<Ring>comparingInt(ring -> ring.stations().size()).reversed().thenComparingInt(Ring::color));
        return result;
    }

    public static Map<Long, Point2D.Double> coordinates(NetworkMapData data) {
        var points = new LinkedHashMap<Long, Point2D.Double>();
        NetworkMapPainter.layout(data, NetworkMapPainter.spacingFor(data)).forEach((id, p) -> points.put(id, new Point2D.Double(p.x, p.y)));
        Set<Long> fixed = new HashSet<>();
        for (var ring : rings(data)) {
            // Конфликтующие кольца с общей станцией требуют совместной подгонки окружностей.
            // До неё сохраняем уже размещённое кольцо и исходные связи второго.
            if (ring.stations().stream().anyMatch(fixed::contains)) continue;
            double cx = ring.stations().stream().mapToDouble(id -> points.get(id).x).average().orElse(0);
            double cy = ring.stations().stream().mapToDouble(id -> points.get(id).y).average().orElse(0);
            double radius = Math.max(30, ring.stations().stream().mapToDouble(id -> points.get(id).distance(cx, cy)).average().orElse(30));
            double first = Math.atan2(points.get(ring.stations().get(0)).y - cy, points.get(ring.stations().get(0)).x - cx);
            double area = 0;
            for (int i = 0; i < ring.stations().size(); i++) {
                var a = points.get(ring.stations().get(i)); var b = points.get(ring.stations().get((i + 1) % ring.stations().size()));
                area += (a.x - cx) * (b.y - cy) - (b.x - cx) * (a.y - cy);
            }
            double direction = area >= 0 ? 1 : -1;
            List<Point2D.Double> before = ring.stations().stream().map(id -> points.get(id)).toList();
            List<Point2D.Double> after = new ArrayList<>();
            for (int i = 0; i < ring.stations().size(); i++) {
                double angle = first + direction * Math.PI * 2 * i / ring.stations().size();
                after.add(new Point2D.Double(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)));
            }
            // Окружающие ветки следуют за смещением своих узлов, без разрыва пересадок.
            for (var entry : points.entrySet()) if (!fixed.contains(entry.getKey()) && !ring.stations().contains(entry.getKey())) {
                double dx = 0, dy = 0, sum = 0;
                for (int i = 0; i < before.size(); i++) {
                    double weight = 1 / Math.max(100, entry.getValue().distanceSq(before.get(i)));
                    dx += (after.get(i).x - before.get(i).x) * weight;
                    dy += (after.get(i).y - before.get(i).y) * weight; sum += weight;
                }
                entry.getValue().x += dx / sum; entry.getValue().y += dy / sum;
            }
            for (int i = 0; i < after.size(); i++) points.put(ring.stations().get(i), after.get(i));
            fixed.addAll(ring.stations());
        }
        alignLines(data, points, fixed);
        straighten(data, points, fixed);
        return fit(data, points);
    }

    private record Axis(Set<Long> stations, double x, double y, double nx, double ny) {}
    private static void alignLines(NetworkMapData data, Map<Long, Point2D.Double> points, Set<Long> fixed) {
        var graphs = NetworkMapGeometry.graph(data);
        Set<Integer> ringColors = new HashSet<>(); rings(data).forEach(ring -> ringColors.add(ring.color()));
        List<Axis> axes = new ArrayList<>();
        graphs.forEach((color, graph) -> {
            if (ringColors.contains(color)) return;
            Set<Long> seen = new HashSet<>();
            for (long start : graph.keySet()) {
                if (!seen.add(start)) continue;
                Set<Long> component = new TreeSet<>();
                Deque<Long> queue = new ArrayDeque<>(); queue.add(start);
                while (!queue.isEmpty()) {
                    long id = queue.removeFirst(); component.add(id);
                    for (long next : graph.get(id)) if (seen.add(next)) queue.add(next);
                }
                if (component.size() < 3 || component.stream().anyMatch(id -> graph.get(id).size() > 2)) continue;
                var pins = component.stream().filter(fixed::contains).toList();
                var ids = pins.size() >= 2 ? pins : new ArrayList<>(component);
                double cx = ids.stream().mapToDouble(id -> points.get(id).x).average().orElse(0);
                double cy = ids.stream().mapToDouble(id -> points.get(id).y).average().orElse(0);
                double xx = 0, yy = 0, xy = 0;
                for (long id : ids) { var p = points.get(id); xx += (p.x - cx) * (p.x - cx); yy += (p.y - cy) * (p.y - cy); xy += (p.x - cx) * (p.y - cy); }
                double angle = Math.atan2(2 * xy, xx - yy) / 2;
                // Опорные пересадки на окружности важнее привязки к сетке углов.
                if (pins.size() < 2) angle = Math.rint(angle / (Math.PI / 4)) * Math.PI / 4;
                if (pins.size() == 1) { cx = points.get(pins.get(0)).x; cy = points.get(pins.get(0)).y; }
                axes.add(new Axis(Set.copyOf(component), cx, cy, -Math.sin(angle), Math.cos(angle)));
            }
        });
        for (var entry : points.entrySet()) {
            if (fixed.contains(entry.getKey())) continue;
            var p = entry.getValue();
            double aa = 3, bb = 0, cc = 3, dx = 3 * p.x, dy = 3 * p.y;
            for (var axis : axes) if (axis.stations.contains(entry.getKey())) {
                double value = axis.nx * axis.x + axis.ny * axis.y;
                aa += axis.nx * axis.nx; bb += axis.nx * axis.ny; cc += axis.ny * axis.ny;
                dx += axis.nx * value; dy += axis.ny * value;
            }
            double determinant = aa * cc - bb * bb;
            points.put(entry.getKey(), new Point2D.Double((dx * cc - dy * bb) / determinant, (dy * aa - dx * bb) / determinant));
        }
    }
    private static void straighten(NetworkMapData data, Map<Long, Point2D.Double> points, Set<Long> fixed) {
        var graphs = NetworkMapGeometry.graph(data);
        Map<Long, Set<Integer>> colors = new HashMap<>();
        graphs.forEach((color, graph) -> graph.keySet().forEach(id -> colors.computeIfAbsent(id, k -> new HashSet<>()).add(color)));
        Set<Long> anchors = new HashSet<>(fixed);
        for (var edge : data.edges()) if (edge.color() < 0) { anchors.add(edge.from()); anchors.add(edge.to()); }
        colors.forEach((id, values) -> { if (values.size() > 1) anchors.add(id); });
        graphs.forEach((color, graph) -> {
            graph.forEach((id, neighbors) -> { if (neighbors.size() != 2) anchors.add(id); });
            Set<String> visited = new HashSet<>();
            for (long start : graph.keySet()) if (anchors.contains(start)) for (long neighbor : graph.get(start)) {
                List<Long> chain = new ArrayList<>(); chain.add(start);
                long previous = start, next = neighbor;
                while (visited.add(Math.min(previous, next) + ":" + Math.max(previous, next))) {
                    chain.add(next);
                    if (anchors.contains(next) || graph.get(next).size() != 2) break;
                    long following = previous;
                    for (long candidate : graph.get(next)) if (candidate != previous) { following = candidate; break; }
                    previous = next; next = following;
                }
                if (chain.size() < 3 || fixed.containsAll(chain)) continue;
                var a = points.get(chain.get(0)); var b = points.get(chain.get(chain.size() - 1));
                // Один спокойный участок между пересадками; длина перегона не диктует интервал станций.
                for (int i = 1; i + 1 < chain.size(); i++) if (!fixed.contains(chain.get(i))) {
                    double t = (double) i / (chain.size() - 1);
                    points.put(chain.get(i), new Point2D.Double(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t));
                }
            }
        });
    }

    private static Map<Long, Point2D.Double> fit(NetworkMapData data, Map<Long, Point2D.Double> points) {
        if (points.isEmpty()) return points;
        double minX = points.values().stream().mapToDouble(p -> p.x).min().orElse(0), maxX = points.values().stream().mapToDouble(p -> p.x).max().orElse(0);
        double minY = points.values().stream().mapToDouble(p -> p.y).min().orElse(0), maxY = points.values().stream().mapToDouble(p -> p.y).max().orElse(0);
        double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
        double bottom = Math.min(1660, NetworkMapSymbols.legendTop(data) - 65), centerY = (320 + bottom) / 2;
        double rx = data.stations().size() >= 60 ? 840 : 740, ry = (bottom - 320) / 2 - 20;
        double extent = points.values().stream().mapToDouble(p -> Math.hypot((p.x - cx) / rx, (p.y - cy) / ry)).max().orElse(1);
        double scale = extent > 0 ? 1 / extent : 1;
        points.replaceAll((id, p) -> new Point2D.Double(1024 + (p.x - cx) * scale, centerY + (p.y - cy) * scale));
        return points;
    }
}
