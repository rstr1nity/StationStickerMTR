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
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            for (int color : currentColors) {
                g.setColor(new Color(color)); g.fillOval(circleX - 24, 65, 48, 48);
                String number = data.lines().stream().filter(line -> line.color() == color).map(NetworkMapData.Line::number).findFirst().orElse("");
                g.setColor(Color.WHITE); g.drawString(number, circleX - g.getFontMetrics().stringWidth(number) / 2, 99); circleX += 60;
            }
            g.setColor(new Color(0x303237));
            g.setFont(new Font("SansSerif", Font.BOLD, 46));
            while (g.getFontMetrics().stringWidth(current) > 1000 && g.getFont().getSize() > 18) g.setFont(g.getFont().deriveFont((float) g.getFont().getSize() - 1));
            centered(g, current, 170);
            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            String secondary = rawCurrent.isEmpty() ? "Station Sticker" : name(rawCurrent, false);
            if (!secondary.equals(current)) centered(g, secondary, 210);
            g.setColor(new Color(0x989898)); g.setStroke(new BasicStroke(1.5f)); g.drawLine(450, 245, 1598, 245);
            if (data.stations().isEmpty()) {
                centered(g, russian ? "Нет доступных линий MTR" : "No MTR lines available", 1010);
                return image;
            }
            Map<Long, Point2D.Double> points = layout(data);
            Map<Long, Set<Integer>> colors = new HashMap<>();
            Map<String, List<NetworkMapData.Edge>> shared = new LinkedHashMap<>();
            for (var edge : data.edges()) {
                if (edge.color() >= 0) {
                    colors.computeIfAbsent(edge.from(), id -> new TreeSet<>()).add(edge.color());
                    colors.computeIfAbsent(edge.to(), id -> new TreeSet<>()).add(edge.color());
                }
                shared.computeIfAbsent(edge.from() + ":" + edge.to(), key -> new ArrayList<>()).add(edge);
            }
            for (var group : shared.values()) {
                for (int i = 0; i < group.size(); i++) {
                    var edge = group.get(i);
                    Point2D.Double a = points.get(edge.from()), b = points.get(edge.to());
                    if (a == null || b == null) continue;
                    double length = Math.max(1, a.distance(b)), shift = (i - (group.size() - 1) / 2.0) * 7;
                    double dx = -(b.y - a.y) / length * shift, dy = (b.x - a.x) / length * shift;
                    g.setColor(new Color(edge.color() < 0 ? 0x999999 : edge.color()));
                    g.setStroke(edge.color() < 0 ? new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 6}, 0)
                            : new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double x = Math.abs(b.x - a.x), y = Math.abs(b.y - a.y), diagonal = Math.min(x, y);
                    Path2D path = new Path2D.Double();
                    path.moveTo(a.x + dx, a.y + dy);
                    path.lineTo(a.x + Math.copySign(diagonal, b.x - a.x) + dx,
                            a.y + Math.copySign(diagonal, b.y - a.y) + dy);
                    path.lineTo(b.x + dx, b.y + dy);
                    g.draw(path);
                }
            }
            g.setFont(new Font("SansSerif", Font.PLAIN, data.stations().size() > 150 ? 16 : 21));
            List<Rectangle2D> labels = new ArrayList<>();
            for (var station : data.stations()) {
                Point2D.Double point = points.get(station.id());
                boolean transfer = colors.getOrDefault(station.id(), Set.of()).size() > 1;
                int radius = transfer ? 9 : 6;
                g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(point.x - radius, point.y - radius, radius * 2, radius * 2));
                g.setColor(new Color(transfer ? 0x444444 : colors.getOrDefault(station.id(), Set.of(0x555555)).iterator().next()));
                g.setStroke(new BasicStroke(3)); g.draw(new Ellipse2D.Double(point.x - radius, point.y - radius, radius * 2, radius * 2));
                if (station.id() == currentStation) {
                    g.setColor(new Color(0xD92E36)); g.draw(new Ellipse2D.Double(point.x - 15, point.y - 15, 30, 30));
                }
                String label = name(station.name(), russian);
                int w = g.getFontMetrics().stringWidth(label), height = g.getFontMetrics().getHeight();
                double[][] candidates = {{14, -5}, {14, height}, {-w - 14, -5}, {-w - 14, height}, {-w / 2.0, -16}, {-w / 2.0, height + 14}};
                Rectangle2D best = null;
                double bestScore = Double.MAX_VALUE;
                for (double[] candidate : candidates) {
                    Rectangle2D box = new Rectangle2D.Double(point.x + candidate[0], point.y + candidate[1] - height, w + 4, height + 4);
                    double score = 0;
                    for (Rectangle2D previous : labels) if (previous.intersects(box)) score += 1000;
                    if (box.getMinX() < 80 || box.getMaxX() > SIZE - 80 || box.getMinY() < 270 || box.getMaxY() > 1740) score += 10000;
                    if (score < bestScore) { best = box; bestScore = score; }
                }
                labels.add(best);
                var outline = g.getFont().createGlyphVector(g.getFontRenderContext(), label)
                        .getOutline((float) best.getX(), (float) (best.getY() + height));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(PAPER); g.draw(outline); g.setColor(new Color(0x292B30)); g.fill(outline);
            }
            // Краткая легенда: номер/имя каждой цветовой линии, без дублирования обратных маршрутов.
            int columns = 4, rows = Math.max(1, (data.lines().size() + columns - 1) / columns);
            int rowHeight = Math.min(32, 180 / rows), fontSize = Math.min(22, Math.max(10, rowHeight - 6));
            g.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
            for (int i = 0; i < data.lines().size(); i++) {
                var line = data.lines().get(i);
                int x = 500 + i % columns * 270, y = 1800 + i / columns * rowHeight;
                g.setColor(new Color(line.color())); g.fillOval(x, y - 15, 18, 18);
                String label = (line.number().isBlank() ? "" : line.number() + " ") + name(line.name(), russian);
                while (g.getFontMetrics().stringWidth(label) > 230 && label.length() > 2) label = label.substring(0, label.length() - 2) + "…";
                g.setColor(new Color(0x303237)); g.drawString(label, x + 25, y);
            }
            return image;
        } finally { g.dispose(); }
    }

    public static Map<Long, Point2D.Double> layout(NetworkMapData data) {
        Map<Long, Point2D.Double> result = new LinkedHashMap<>();
        if (data.stations().isEmpty()) return result;
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minZ = minX, maxZ = maxX;
        for (var station : data.stations()) {
            minX = Math.min(minX, station.x()); maxX = Math.max(maxX, station.x());
            minZ = Math.min(minZ, station.z()); maxZ = Math.max(maxZ, station.z());
        }
        double cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2, extent = 1;
        for (var station : data.stations()) extent = Math.max(extent, Math.hypot(station.x() - cx, station.z() - cz));
        for (var station : data.stations()) result.put(station.id(), new Point2D.Double(
                1024 + (station.x() - cx) * 690 / extent, 1010 + (station.z() - cz) * 690 / extent));
        return result;
    }
    private static void centered(Graphics2D g, String text, int baseline) { g.drawString(text, (SIZE - g.getFontMetrics().stringWidth(text)) / 2, baseline); }
    private static String name(String raw, boolean russian) {
        String[] parts = raw.split("\\|\\|", 2)[0].split("\\|");
        for (String part : parts) if (!part.isBlank() && part.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.CYRILLIC) == russian) return part.trim();
        return parts.length == 0 ? "" : parts[0].trim();
    }
}
