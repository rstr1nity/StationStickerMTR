package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

// Рисуется один раз на снимок сети, не каждый кадр. География X/Z, соединения под 45°.
public final class NetworkMapPainter {
    public static final int SIZE = 2048;
    private static final Color PAPER = new Color(0xFAF8F1);
    private NetworkMapPainter() {}

    public static BufferedImage paint(NetworkMapData data, long currentStation, boolean russian) {
        return paint(data, currentStation, russian, NetworkMapFont.fallback());
    }

    public static BufferedImage paint(NetworkMapData data, long currentStation, boolean russian, NetworkMapFont font) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(PAPER);
            g.fillOval(0, 0, SIZE, SIZE);
            g.setClip(new Ellipse2D.Double(20, 20, SIZE - 40, SIZE - 40));
            String rawCurrent = data.stations().stream().filter(s -> s.id() == currentStation)
                    .map(NetworkMapData.Station::name).findFirst().orElse("");
            String current = rawCurrent.isEmpty() ? (russian ? "СХЕМА МЕТРО" : "METRO NETWORK") : name(rawCurrent, russian);
            Set<Integer> currentColors = new TreeSet<>();
            for (var edge : data.edges()) if (edge.color() >= 0 && (edge.from() == currentStation || edge.to() == currentStation)) currentColors.add(edge.color());
            int circleX = 1024 - (currentColors.size() - 1) * 30;
            font.size(24);
            for (int color : currentColors) {
                g.setColor(new Color(color)); g.fillOval(circleX - 24, 65, 48, 48);
                String number = data.lines().stream().filter(line -> line.color() == color).map(NetworkMapData.Line::number).findFirst().orElse("");
                g.setColor(badgeTextColor(color)); font.draw(g, number, circleX - font.width(number) / 2, 99); circleX += 60;
            }
            g.setColor(new Color(0x303237));
            int titleSize = 48; font.size(titleSize);
            while (font.width(current) > 1000 && titleSize > 16) font.size(titleSize -= 8);
            centered(g, font, current, 170);
            font.size(24);
            String secondary = rawCurrent.isEmpty() ? "Station Sticker" : name(rawCurrent, false);
            if (!secondary.equals(current)) centered(g, font, secondary, 210);
            g.setColor(new Color(0x989898)); g.setStroke(new BasicStroke(1.5f)); g.drawLine(450, 245, 1598, 245);
            if (data.stations().isEmpty()) {
                centered(g, font, russian ? "Нет доступных линий MTR" : "No MTR lines available", 1010);
                return image;
            }
            Map<Long, Point2D.Double> points = layout(data, font, russian);
            Map<Long, Set<Integer>> colors = new HashMap<>();
            for (var edge : data.edges()) {
                if (edge.color() >= 0) {
                    colors.computeIfAbsent(edge.from(), id -> new TreeSet<>()).add(edge.color());
                    colors.computeIfAbsent(edge.to(), id -> new TreeSet<>()).add(edge.color());
                }
            }
            List<Shape> lineObstacles = new ArrayList<>();
            for (var edge : data.edges()) if (edge.color() < 0) {
                var a = points.get(edge.from()); var b = points.get(edge.to());
                if (a == null || b == null) continue;
                var link = new Line2D.Double(a, b);
                g.setColor(new Color(0x777777)); g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(link);
                g.setColor(PAPER); g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(link);
                lineObstacles.add(new BasicStroke(18).createStrokedShape(link));
            }
            for (var route : NetworkMapGeometry.paths(data, points)) {
                g.setColor(new Color(route.color())); g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(route.path());
                lineObstacles.add(new BasicStroke(18).createStrokedShape(route.path()));
            }
            List<Rectangle2D> labels = new ArrayList<>();
            for (var point : points.values()) labels.add(new Rectangle2D.Double(point.x - 13, point.y - 13, 26, 26));
            var terminals = NetworkMapGeometry.terminals(data);
            for (var terminal : terminals) {
                var p = points.get(terminal.station()); var neighbor = points.get(terminal.neighbor());
                if (p == null || neighbor == null) continue;
                double length = Math.max(1, p.distance(neighbor));
                double x = p.x + (p.x - neighbor.x) * 38 / length, y = p.y + (p.y - neighbor.y) * 38 / length;
                var siblings = terminals.stream().filter(t -> t.station() == terminal.station()).toList();
                double offset = (siblings.indexOf(terminal) - (siblings.size() - 1) / 2.0) * 46;
                x -= (p.y - neighbor.y) * offset / length; y += (p.x - neighbor.x) * offset / length;
                routeBadge(g, font, data, terminal.color(), x, y, 21);
                labels.add(new Rectangle2D.Double(x - 24, y - 24, 48, 48));
            }
            font.size(data.stations().size() > 150 ? 16 : 24);
            record Label(String text, Rectangle2D box, int height) {}
            List<Label> placedLabels = new ArrayList<>();
            for (var station : data.stations()) {
                Point2D.Double point = points.get(station.id());
                boolean transfer = colors.getOrDefault(station.id(), Set.of()).size() > 1;
                int radius = transfer ? 12 : 7;
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(point.x - radius, point.y - radius, radius * 2, radius * 2));
                var stationColors = new ArrayList<>(colors.getOrDefault(station.id(), Set.of(0x555555)));
                g.setStroke(new BasicStroke(4));
                for (int i = 0; i < stationColors.size(); i++) {
                    g.setColor(new Color(stationColors.get(i)));
                    g.draw(new Arc2D.Double(point.x - radius, point.y - radius, radius * 2, radius * 2,
                            i * 360.0 / stationColors.size(), 360.0 / stationColors.size(), Arc2D.OPEN));
                }
                if (station.id() == currentStation) {
                    g.setColor(new Color(0xD92E36)); g.draw(new Ellipse2D.Double(point.x - 15, point.y - 15, 30, 30));
                }
                String label = name(station.name(), russian);
                int w = font.width(label), height = font.height();
                var placement = NetworkMapLabels.place(point, w, height, labels, lineObstacles, Math.min(1580, legendTop(data) - 35));
                Rectangle2D best = placement.box();
                labels.add(best);
                placedLabels.add(new Label(label, best, height));
                if (placement.leader()) {
                    g.setColor(new Color(0xAAAAAA)); g.setStroke(new BasicStroke(1));
                    g.draw(new Line2D.Double(point.x, point.y, Math.max(best.getMinX(), Math.min(point.x, best.getMaxX())), best.getCenterY()));
                }
            }
            // Все выноски уже нарисованы; текст и его подложка всегда остаются сверху.
            for (Label label : placedLabels) {
                Rectangle2D best = label.box();
                g.setColor(PAPER); g.fill(best);
                var outline = font.outline(label.text(), best.getX(), best.getY() + label.height() - 5);
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(PAPER); g.draw(outline); g.setColor(new Color(0x292B30)); font.draw(g, label.text(), best.getX(), best.getY() + label.height() - 5);
            }
            // Краткая легенда: номер/имя каждой цветовой линии, без дублирования обратных маршрутов.
            int columns = Math.max(1, (data.lines().size() + 9) / 10), rows = Math.max(1, (data.lines().size() + columns - 1) / columns);
            int columnWidth = Math.min(450, 960 / columns), totalWidth = columns * columnWidth;
            for (int i = 0; i < data.lines().size(); i++) {
                var line = data.lines().get(i);
                int x = 1024 - totalWidth / 2 + i / rows * columnWidth, y = legendTop(data) + i % rows * 36;
                routeBadge(g, font, data, line.color(), x + 18, y, 16);
                font.size(16);
                String label = name(line.name(), russian);
                while (font.width(label) > columnWidth - 50 && label.length() > 2) label = label.substring(0, label.length() - 2) + "…";
                g.setColor(new Color(0x303237)); font.draw(g, label, x + 44, y + 9);
            }
            return image;
        } finally { g.dispose(); }
    }

    public static Map<Long, Point2D.Double> layout(NetworkMapData data) {
        return layout(data, NetworkMapFont.fallback(), true);
    }
    public static Map<Long, Point2D.Double> layout(NetworkMapData data, NetworkMapFont font, boolean russian) {
        Map<Long, Point2D.Double> result = new LinkedHashMap<>();
        if (data.stations().isEmpty()) return result;
        var xs = data.stations().stream().map(NetworkMapData.Station::x).distinct().sorted().toList();
        var zs = data.stations().stream().map(NetworkMapData.Station::z).distinct().sorted().toList();
        Map<Long, Point2D.Double> compressed = new HashMap<>();
        double extent = 0.001;
        for (var station : data.stations()) {
            double x = compact(station.x(), xs), z = compact(station.z(), zs);
            compressed.put(station.id(), new Point2D.Double(x, z)); extent = Math.max(extent, Math.hypot(x, z));
        }
        double chartBottom = Math.min(1580, legendTop(data) - 65);
        double centerY = (300 + chartBottom) / 2, chartRadius = Math.min(600, (chartBottom - 300) / 2 - 25);
        for (var station : data.stations()) result.put(station.id(), new Point2D.Double(
                1024 + compressed.get(station.id()).x * chartRadius / extent, centerY + compressed.get(station.id()).y * chartRadius / extent));
        Map<Long, Point2D.Double> anchors = new HashMap<>();
        result.forEach((id, point) -> anchors.put(id, new Point2D.Double(point.x, point.y)));
        font.size(data.stations().size() > 150 ? 16 : 24);
        Map<Long, Double> widths = new HashMap<>();
        for (var station : data.stations()) widths.put(station.id(), (double) Math.min(240, font.width(name(station.name(), russian))) + 36);
        // Раздвигаем плотные узлы, оставляя слабую привязку к исходной географии.
        for (int iteration = 0; iteration < 60; iteration++) {
            Map<String, List<Long>> grid = new HashMap<>();
            result.forEach((id, p) -> grid.computeIfAbsent((int) (p.x / 300) + ":" + (int) (p.y / 80), key -> new ArrayList<>()).add(id));
            Map<Long, Point2D.Double> forces = new HashMap<>();
            result.forEach((id, p) -> forces.put(id, new Point2D.Double((anchors.get(id).x - p.x) * 0.015, (anchors.get(id).y - p.y) * 0.015)));
            for (var entry : result.entrySet()) {
                long id = entry.getKey(); var p = entry.getValue();
                int gx = (int) (p.x / 300), gy = (int) (p.y / 80);
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (long other : grid.getOrDefault((gx + dx) + ":" + (gy + dy), List.of())) {
                    if (other <= id) continue;
                    var q = result.get(other);
                    double x = p.x - q.x, y = p.y - q.y, targetX = (widths.get(id) + widths.get(other)) / 2;
                    if (Math.abs(x) >= targetX || Math.abs(y) >= 60) continue;
                    double pushX = (targetX - Math.abs(x)) / targetX, pushY = (60 - Math.abs(y)) / 60;
                    // Чаще раздвигаем по вертикали: длинные имена получают свободную строку.
                    double fy = Math.copySign(5 * pushX * pushY, Math.abs(y) < 0.01 ? (id < other ? -1 : 1) : y);
                    double fx = Math.copySign(1.5 * pushX * pushY, Math.abs(x) < 0.01 ? 1 : x);
                    forces.get(id).x += fx; forces.get(id).y += fy;
                    forces.get(other).x -= fx; forces.get(other).y -= fy;
                }
            }
            result.forEach((id, p) -> {
                p.x += Math.max(-12, Math.min(12, forces.get(id).x)); p.y += Math.max(-12, Math.min(12, forces.get(id).y));
                double radius = Point2D.distance(p.x, p.y, 1024, centerY);
                if (radius > chartRadius) { p.x = 1024 + (p.x - 1024) * chartRadius / radius; p.y = centerY + (p.y - centerY) * chartRadius / radius; }
            });
        }
        return result;
    }
    private static void centered(Graphics2D g, NetworkMapFont font, String text, int baseline) { font.draw(g, text, (SIZE - font.width(text)) / 2, baseline); }
    private static double compact(double value, List<Double> coordinates) {
        if (coordinates.size() < 2) return 0;
        double min = coordinates.get(0), max = coordinates.get(coordinates.size() - 1);
        double geography = (value - min) / Math.max(1, max - min);
        double order = (double) Collections.binarySearch(coordinates, value) / (coordinates.size() - 1);
        return geography * 0.35 + order * 0.65 - 0.5;
    }
    private static int legendTop(NetworkMapData data) {
        int columns = Math.max(1, (data.lines().size() + 9) / 10);
        int rows = Math.max(1, (data.lines().size() + columns - 1) / columns);
        return 1860 - (rows - 1) * 36;
    }
    public static String name(String raw, boolean russian) {
        String[] parts = raw.split("\\|\\|", 2)[0].split("\\|");
        for (var script : new Character.UnicodeScript[]{russian ? Character.UnicodeScript.CYRILLIC : Character.UnicodeScript.LATIN,
                russian ? Character.UnicodeScript.LATIN : Character.UnicodeScript.CYRILLIC}) {
            for (String part : parts) {
                if (script == Character.UnicodeScript.LATIN && part.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.CYRILLIC)) continue;
                if (!part.isBlank() && part.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == script)) return part.trim();
            }
        }
        for (String part : parts) if (!part.isBlank()) return part.trim();
        return "";
    }

    private static void routeBadge(Graphics2D g, NetworkMapFont font, NetworkMapData data, int color, double x, double y, int radius) {
        g.setColor(new Color(color)); g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        String number = data.lines().stream().filter(line -> line.color() == color).map(NetworkMapData.Line::number).findFirst().orElse("");
        font.size(16);
        if (font.width(number) > radius * 1.7) font.size(8);
        g.setColor(badgeTextColor(color)); font.draw(g, number, x - font.width(number) / 2.0, y + (font.height() - 4) / 2.0);
    }
    private static Color badgeTextColor(int color) {
        double brightness = ((color >> 16) & 255) * 0.2126 + ((color >> 8) & 255) * 0.7152 + (color & 255) * 0.0722;
        return brightness > 155 ? new Color(0x202124) : Color.WHITE;
    }
}
