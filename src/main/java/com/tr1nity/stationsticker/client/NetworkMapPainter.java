package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

// Рисуется один раз на снимок сети, не каждый кадр. География X/Z, соединения под 45°.
public final class NetworkMapPainter {
    public static final int SIZE = 2048;
    private static final Color PAPER = new Color(0xFAF8F1);
    private NetworkMapPainter() {}

    public static BufferedImage paint(NetworkMapData data, long currentStation, boolean russian) {
        return paint(data, currentStation, russian, NetworkMapFont.fallback());
    }

    public static BufferedImage paint(NetworkMapData data, long currentStation, boolean russian, NetworkMapFont font) {
        return paint(data, currentStation, russian, font, data.stations().size() > 20 ? 1 : 0);
    }
    public static BufferedImage paint(NetworkMapData data, long currentStation, boolean russian, NetworkMapFont font, int spacing) {
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
            Map<Long, Point2D.Double> points = layout(data, spacing);
            NetworkMapSymbols.draw(g, data, points, currentStation, russian, font);
            return image;
        } finally { g.dispose(); }
    }

    public static Map<Long, Point2D.Double> layout(NetworkMapData data) {
        return layout(data, NetworkMapFont.fallback(), true);
    }
    public static Map<Long, Point2D.Double> layout(NetworkMapData data, NetworkMapFont font, boolean russian) {
        return layout(data, data.stations().size() > 20);
    }
    public static Map<Long, Point2D.Double> layout(NetworkMapData data, boolean compact) {
        return layout(data, compact ? 1 : 0);
    }
    public static Map<Long, Point2D.Double> layout(NetworkMapData data, int spacing) {
        Map<Long, Point2D.Double> result = new LinkedHashMap<>();
        if (data.stations().isEmpty()) return result;
        var coordinates = NetworkMapSpacing.coordinates(data, spacing);
        double minX = coordinates.values().stream().mapToDouble(p -> p.x).min().orElse(0);
        double maxX = coordinates.values().stream().mapToDouble(p -> p.x).max().orElse(0);
        double minZ = coordinates.values().stream().mapToDouble(p -> p.y).min().orElse(0);
        double maxZ = coordinates.values().stream().mapToDouble(p -> p.y).max().orElse(0);
        double centerX = minX / 2 + maxX / 2, centerZ = minZ / 2 + maxZ / 2;
        double chartBottom = Math.min(1660, NetworkMapSymbols.legendTop(data) - 65);
        double centerY = (320 + chartBottom) / 2, radiusY = (chartBottom - 320) / 2 - 30;
        double extent = 0;
        for (var p : coordinates.values()) extent = Math.max(extent, Math.hypot((p.x - centerX) / 740, (p.y - centerZ) / radiusY));
        double scale = extent > 0 ? 1 / extent : 1;
        coordinates.forEach((id, p) -> result.put(id, new Point2D.Double(1024 + (p.x - centerX) * scale, centerY + (p.y - centerZ) * scale)));
        return result;
    }
    private static void centered(Graphics2D g, NetworkMapFont font, String text, int baseline) { font.draw(g, text, (SIZE - font.width(text)) / 2, baseline); }
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

    private static Color badgeTextColor(int color) {
        double brightness = ((color >> 16) & 255) * 0.2126 + ((color >> 8) & 255) * 0.7152 + (color & 255) * 0.0722;
        return brightness > 155 ? new Color(0x202124) : Color.WHITE;
    }
}
