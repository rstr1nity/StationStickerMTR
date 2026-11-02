package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.geom.*;
import java.util.*;

public final class NetworkMapGeometry {
    public record LinePath(int color, Path2D path) {}
    public record Terminal(long station, int color, long neighbor) {}
    public record Marker(Point2D.Double point, Point2D.Double normal) {}

    private NetworkMapGeometry() {}

    public static Marker marker(List<LinePath> routes, int color, Point2D.Double station) {
        double best = Double.POSITIVE_INFINITY;
        Marker result = new Marker(station, new Point2D.Double(0, 1));
        for (var route : routes) if (route.color() == color) {
            var iterator = route.path().getPathIterator(null, 0.1);
            double[] values = new double[6];
            double x = 0, y = 0, startX = 0, startY = 0;
            while (!iterator.isDone()) {
                int type = iterator.currentSegment(values);
                if (type == PathIterator.SEG_MOVETO) { x = startX = values[0]; y = startY = values[1]; }
                else if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_CLOSE) {
                    double nx = type == PathIterator.SEG_CLOSE ? startX : values[0];
                    double ny = type == PathIterator.SEG_CLOSE ? startY : values[1];
                    double dx = nx - x, dy = ny - y, lengthSquared = dx * dx + dy * dy;
                    if (lengthSquared > 0) {
                        double t = Math.max(0, Math.min(1, ((station.x - x) * dx + (station.y - y) * dy) / lengthSquared));
                        var point = new Point2D.Double(x + t * dx, y + t * dy);
                        double distance = point.distanceSq(station);
                        if (distance < best) {
                            best = distance;
                            double length = Math.sqrt(lengthSquared);
                            result = new Marker(point, new Point2D.Double(-dy / length, dx / length));
                        }
                    }
                    x = nx; y = ny;
                }
                iterator.next();
            }
        }
        return result;
    }

    public static Map<Integer, Map<Long, Set<Long>>> graph(NetworkMapData data) {
        Map<Integer, Map<Long, Set<Long>>> graph = new TreeMap<>();
        for (var edge : data.edges()) if (edge.color() >= 0) {
            var line = graph.computeIfAbsent(edge.color(), c -> new TreeMap<>());
            line.computeIfAbsent(edge.from(), id -> new TreeSet<>()).add(edge.to());
            line.computeIfAbsent(edge.to(), id -> new TreeSet<>()).add(edge.from());
        }
        return graph;
    }
    public static List<Terminal> terminals(NetworkMapData data) {
        List<Terminal> result = new ArrayList<>();
        graph(data).forEach((color, line) -> line.forEach((station, neighbors) -> {
            if (neighbors.size() == 1) result.add(new Terminal(station, color, neighbors.iterator().next()));
        }));
        return result;
    }
    public static List<LinePath> paths(NetworkMapData data, Map<Long, Point2D.Double> positions) {
        return paths(data, positions, 8);
    }
    public static List<LinePath> paths(NetworkMapData data, Map<Long, Point2D.Double> positions, float width) {
        List<LinePath> result = new ArrayList<>();
        Map<String, SortedSet<Integer>> shared = new HashMap<>();
        for (var edge : data.edges()) if (edge.color() >= 0) shared.computeIfAbsent(key(edge.from(), edge.to()), k -> new TreeSet<>()).add(edge.color());
        graph(data).forEach((color, line) -> {
            Set<String> visited = new HashSet<>();
            List<Long> starts = new ArrayList<>(line.keySet());
            starts.sort(Comparator.comparingInt(id -> line.get(id).size() == 2 ? 1 : 0));
            for (long start : starts) for (long first : line.get(start)) {
                if (visited.contains(key(start, first))) continue;
                List<Point2D.Double> points = new ArrayList<>();
                long from = start, to = first;
                while (visited.add(key(from, to))) {
                    Point2D.Double a = positions.get(from), b = positions.get(to);
                    if (a == null || b == null) break;
                    var colors = new ArrayList<>(shared.get(key(from, to)));
                    double shift = (colors.indexOf(color) - (colors.size() - 1) / 2.0) * (width + 2);
                    double length = Math.max(1, a.distance(b));
                    double sign = from < to ? 1 : -1;
                    double dx = -(b.y - a.y) / length * shift * sign, dy = (b.x - a.x) / length * shift * sign;
                    add(points, a.x + dx, a.y + dy);
                    double diagonal = Math.min(Math.abs(b.x - a.x), Math.abs(b.y - a.y));
                    add(points, a.x + Math.copySign(diagonal, b.x - a.x) + dx, a.y + Math.copySign(diagonal, b.y - a.y) + dy);
                    add(points, b.x + dx, b.y + dy);
                    if (line.get(to).size() != 2) break;
                    long next = from;
                    for (long neighbor : line.get(to)) if (neighbor != from) { next = neighbor; break; }
                    from = to; to = next;
                }
                if (points.size() > 1) result.add(new LinePath(color, rounded(points, Math.max(8, width * 3))));
            }
        });
        return result;
    }
    private static void add(List<Point2D.Double> points, double x, double y) {
        if (points.isEmpty() || points.get(points.size() - 1).distance(x, y) > 0.1) points.add(new Point2D.Double(x, y));
    }
    private static String key(long a, long b) { return Math.min(a, b) + ":" + Math.max(a, b); }
    public static Path2D rounded(List<Point2D.Double> points, double radius) {
        points = new ArrayList<>(points);
        // Убираем почти прямые микрозаломы; отклонение меньше половины толщины линии.
        for (int i = 1; i + 1 < points.size(); i++) {
            if (Line2D.ptSegDist(points.get(i - 1).x, points.get(i - 1).y,
                    points.get(i + 1).x, points.get(i + 1).y, points.get(i).x, points.get(i).y) <= 3) {
                points.remove(i--);
            }
        }
        Path2D path = new Path2D.Double();
        if (points.isEmpty()) return path;
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i + 1 < points.size(); i++) {
            var a = points.get(i - 1); var b = points.get(i); var c = points.get(i + 1);
            double first = a.distance(b), second = b.distance(c);
            if (first < 0.01 || second < 0.01) continue;
            double r = Math.min(radius, Math.min(first, second) / 3);
            path.lineTo(b.x + (a.x - b.x) * r / first, b.y + (a.y - b.y) * r / first);
            path.quadTo(b.x, b.y, b.x + (c.x - b.x) * r / second, b.y + (c.y - b.y) * r / second);
        }
        var last = points.get(points.size() - 1); path.lineTo(last.x, last.y);
        return path;
    }
}
