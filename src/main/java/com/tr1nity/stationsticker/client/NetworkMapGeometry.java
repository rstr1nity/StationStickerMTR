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
        return paths(data, positions, width, false);
    }
    public static List<LinePath> paths(NetworkMapData data, Map<Long, Point2D.Double> positions, float width, boolean schematic) {
        List<LinePath> result = new ArrayList<>();
        Map<String, SortedSet<Integer>> shared = new HashMap<>();
        for (var edge : data.edges()) if (edge.color() >= 0) shared.computeIfAbsent(key(edge.from(), edge.to()), k -> new TreeSet<>()).add(edge.color());
        graph(data).forEach((color, line) -> {
            if (schematic) for (var ring : NetworkMapSchematic.rings(data)) if (ring.color() == color) {
                var circle = NetworkMapSchematic.circle(ring, positions);
                if (circle != null) {
                    Path2D path = new Path2D.Double(); path.append(circle, false); result.add(new LinePath(color, path));
                    ring.stations().forEach(line::remove);
                }
            }
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
                    if (!schematic) add(points, a.x + Math.copySign(diagonal, b.x - a.x) + dx, a.y + Math.copySign(diagonal, b.y - a.y) + dy);
                    add(points, b.x + dx, b.y + dy);
                    if (line.get(to).size() != 2) break;
                    long next = from;
                    for (long neighbor : line.get(to)) if (neighbor != from) { next = neighbor; break; }
                    from = to; to = next;
                }
                if (points.size() > 1) result.add(new LinePath(color, schematic ? smooth(points) : rounded(points, Math.max(8, width * 3))));
            }
        });
        return schematic?result:alignOverlappingRuns(result,width);
    }
    private record Run(int color,double y,double left,double right){}
    // Merge only near-coincident, same-color horizontal runs that already overlap visually.
    private static List<LinePath> alignOverlappingRuns(List<LinePath> paths,float width){
        List<Run> runs=new ArrayList<>();
        for(var path:paths){var it=path.path().getPathIterator(null);double[] v=new double[6];double x=0,y=0;
            while(!it.isDone()){int type=it.currentSegment(v);if(type==PathIterator.SEG_LINETO && Math.abs(v[1]-y)<.0001 && Math.abs(v[0]-x)>width*8)runs.add(new Run(path.color(),y,Math.min(x,v[0]),Math.max(x,v[0])));
                if(type==PathIterator.SEG_MOVETO||type==PathIterator.SEG_LINETO){x=v[0];y=v[1];}else if(type==PathIterator.SEG_QUADTO){x=v[2];y=v[3];}else if(type==PathIterator.SEG_CUBICTO){x=v[4];y=v[5];}it.next();}
        }
        int[] parent=new int[runs.size()];for(int i=0;i<parent.length;i++)parent[i]=i;
        for(int i=0;i<runs.size();i++)for(int j=i+1;j<runs.size();j++){var a=runs.get(i);var b=runs.get(j);
            if(a.color()!=b.color())continue;double delta=Math.abs(a.y()-b.y());
            if(delta<.0001 || delta<width*1.25 && Math.min(a.right(),b.right())-Math.max(a.left(),b.left())>width*8){int x=i,y=j;while(parent[x]!=x)x=parent[x];while(parent[y]!=y)y=parent[y];parent[y]=x;}
        }
        Map<Integer,double[]> sums=new HashMap<>();for(int i=0;i<runs.size();i++){int root=i;while(parent[root]!=root)root=parent[root];var sum=sums.computeIfAbsent(root,k->new double[2]);sum[0]+=runs.get(i).y();sum[1]++;}
        Map<Integer,Map<Double,Double>> levels=new HashMap<>();for(int i=0;i<runs.size();i++){int root=i;while(parent[root]!=root)root=parent[root];var sum=sums.get(root);levels.computeIfAbsent(runs.get(i).color(),c->new HashMap<>()).put(runs.get(i).y(),sum[0]/sum[1]);}
        List<LinePath> result=new ArrayList<>();for(var original:paths){var mapping=levels.getOrDefault(original.color(),Map.of());var path=new Path2D.Double();var it=original.path().getPathIterator(null);double[] v=new double[6];
            while(!it.isDone()){int type=it.currentSegment(v);int count=type==PathIterator.SEG_CUBICTO?6:type==PathIterator.SEG_QUADTO?4:2;for(int i=1;i<count;i+=2)v[i]=mapping.getOrDefault(v[i],v[i]);
                switch(type){case PathIterator.SEG_MOVETO->path.moveTo(v[0],v[1]);case PathIterator.SEG_LINETO->path.lineTo(v[0],v[1]);case PathIterator.SEG_QUADTO->path.quadTo(v[0],v[1],v[2],v[3]);case PathIterator.SEG_CUBICTO->path.curveTo(v[0],v[1],v[2],v[3],v[4],v[5]);case PathIterator.SEG_CLOSE->path.closePath();}it.next();}
            result.add(new LinePath(original.color(),path));}
        return result;
    }
    private static Path2D smooth(List<Point2D.Double> points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points.get(0).x, points.get(0).y);
        // Кубическая интерполяция проходит через станции, сохраняя общую касательную в узлах.
        for (int i = 0; i + 1 < points.size(); i++) {
            var a = points.get(i); var b = points.get(i + 1);
            var before = points.get(Math.max(0, i - 1)); var after = points.get(Math.min(points.size() - 1, i + 2));
            double length = a.distance(b);
            double first = Math.max(0.001, before.distance(b)), second = Math.max(0.001, a.distance(after));
            double firstControl = Math.min(length, i == 0 ? length : before.distance(a)) / 3;
            double secondControl = Math.min(length, i + 2 >= points.size() ? length : b.distance(after)) / 3;
            // Короткий соседний перегон не должен создавать длинную петлю касательной.
            path.curveTo(a.x + (b.x - before.x) / first * firstControl, a.y + (b.y - before.y) / first * firstControl,
                    b.x - (after.x - a.x) / second * secondControl, b.y - (after.y - a.y) / second * secondControl, b.x, b.y);
        }
        return path;
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
