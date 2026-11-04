package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** Размеры и размещение обозначений рассчитываются вместе с подписями. */
public final class NetworkMapSymbols {
    public record Badge(NetworkMapGeometry.Terminal terminal, Point2D.Double center, int radius, String number) {
        public Rectangle2D bounds() { return new Rectangle2D.Double(center.x - radius - 3, center.y - radius - 3, radius * 2 + 6, radius * 2 + 6); }
    }
    public record Label(long station, List<String> rows, Rectangle2D box, int size, boolean leader) {}
    public record Plan(List<Badge> badges, List<Label> labels, Map<Long, Set<Integer>> colors, Set<Long> transfers, float width) {}
    private static final Color PAPER = new Color(0xFAF8F1);
    private NetworkMapSymbols() {}

    public static float lineWidth(NetworkMapData data) { return data.stations().size() > 60 ? 3 : 5; }
    public static int legendColumns(NetworkMapData data) { return Math.max(1, (data.lines().size() + 11) / 12); }
    public static int legendRows(NetworkMapData data) { return Math.max(1, (data.lines().size() + legendColumns(data) - 1) / legendColumns(data)); }
    public static int legendTop(NetworkMapData data) { return 1850 - (legendRows(data) - 1) * 26; }

    public static Plan plan(NetworkMapData data, Map<Long, Point2D.Double> points, NetworkMapFont font, boolean russian) {
        return plan(data, points, font, russian, 0);
    }
    public static Plan plan(NetworkMapData data, Map<Long, Point2D.Double> points, NetworkMapFont font, boolean russian, long current) {
        return plan(data, points, font, russian, current, false);
    }
    public static Plan plan(NetworkMapData data, Map<Long, Point2D.Double> points, NetworkMapFont font, boolean russian, long current, boolean schematic) {
        font.density(data.stations().size() >= 250 ? 2 : 1);
        float width = lineWidth(data);
        Map<Long, Set<Integer>> colors = new HashMap<>();
        Set<Long> transfers = new HashSet<>();
        for (var edge : data.edges()) {
            if (edge.color() < 0) { transfers.add(edge.from()); transfers.add(edge.to()); }
            else {
                colors.computeIfAbsent(edge.from(), id -> new TreeSet<>()).add(edge.color());
                colors.computeIfAbsent(edge.to(), id -> new TreeSet<>()).add(edge.color());
            }
        }
        colors.forEach((id, values) -> { if (values.size() > 1) transfers.add(id); });
        List<Shape> obstacles = obstacles(data, points, width, schematic);
        List<Rectangle2D> occupied = new ArrayList<>();
        int marker = width > 3 ? 10 : 7;
        points.values().forEach(p -> occupied.add(new Rectangle2D.Double(p.x - marker, p.y - marker, marker * 2, marker * 2)));
        List<Badge> badges = new ArrayList<>();
        int bottom = Math.min(1660, legendTop(data) - 45);
        for (var terminal : NetworkMapGeometry.terminals(data)) {
            String number = number(data, terminal.color());
            // Пустое поле номера не превращаем в большой бесполезный цветной круг.
            if (number.isBlank()) continue;
            font.size(8);
            int radius = Math.max(width > 3 ? 11 : 8, (font.width(number) + 5) / 2);
            Point2D.Double p = points.get(terminal.station()), q = points.get(terminal.neighbor());
            double outward = Math.atan2(p.y - q.y, p.x - q.x);
            Badge found = null;
            for (int distance = radius + marker + 6; distance <= 180 && found == null; distance += radius * 2 + 8) {
                for (double angle : new double[]{0, Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2, Math.PI}) {
                    var center = new Point2D.Double(p.x + Math.cos(outward + angle) * distance, p.y + Math.sin(outward + angle) * distance);
                    var candidate = new Badge(terminal, center, radius, number);
                    if (free(candidate.bounds(), occupied, obstacles, bottom)) { found = candidate; break; }
                }
            }
            // При экстремально плотном узле сохраняем номер в легенде, не рисуем поверх другой станции.
            if (found != null) { badges.add(found); occupied.add(found.bounds()); }
        }
        List<Label> labels = NetworkMapLabelLayout.place(data, points, font, russian, occupied, obstacles, bottom, current);
        return new Plan(List.copyOf(badges), List.copyOf(labels), colors, transfers, width);
    }

    private static int nearCount(Point2D.Double p, Map<Long, Point2D.Double> points) {
        int count = 0;
        for (var q : points.values()) if (p.distanceSq(q) < 120 * 120) count++;
        return count;
    }
    private static boolean free(Rectangle2D box, List<Rectangle2D> occupied, List<Shape> paths, int bottom) {
        if (box.getMinY() < 290 || box.getMaxY() > bottom) return false;
        for (double x : new double[]{box.getMinX(), box.getMaxX()}) for (double y : new double[]{box.getMinY(), box.getMaxY()})
            if (Point2D.distance(x, y, 1024, 1024) > 930) return false;
        return occupied.stream().noneMatch(box::intersects) && paths.stream().noneMatch(path -> path.intersects(box));
    }
    private static List<Shape> obstacles(NetworkMapData data, Map<Long, Point2D.Double> points, float width, boolean schematic) {
        List<Shape> result = new ArrayList<>();
        for (var path : NetworkMapGeometry.paths(data, points, width, schematic)) result.add(new BasicStroke(width + 5).createStrokedShape(path.path()));
        for (var edge : data.edges()) if (edge.color() < 0) result.add(new BasicStroke(width + 7).createStrokedShape(new Line2D.Double(points.get(edge.from()), points.get(edge.to()))));
        return result;
    }

    public static List<String> wrap(String text, NetworkMapFont font, int maximum) {
        List<String> rows = new ArrayList<>();
        String row = "";
        for (String word : text.trim().split("\\s+")) {
            if (!row.isEmpty() && font.width(row + " " + word) > maximum) { rows.add(row); row = ""; }
            for (int code : word.codePoints().toArray()) {
                String character = new String(Character.toChars(code));
                if (!row.isEmpty() && font.width(row + character) > maximum) { rows.add(row); row = ""; }
                row += character;
            }
            row += " ";
        }
        if (!row.isBlank()) rows.add(row.stripTrailing());
        return rows.isEmpty() ? List.of("") : rows.stream().map(String::stripTrailing).toList();
    }

    public static void draw(Graphics2D g, NetworkMapData data, Map<Long, Point2D.Double> points, long current, boolean russian, NetworkMapFont font) {
        draw(g, data, points, current, russian, font, false);
    }
    public static void draw(Graphics2D g, NetworkMapData data, Map<Long, Point2D.Double> points, long current, boolean russian, NetworkMapFont font, boolean schematic) {
        Plan plan = plan(data, points, font, russian, current, schematic);
        float width = plan.width();
        var routes = NetworkMapGeometry.paths(data, points, width, schematic);
        Map<Integer,java.awt.geom.Area> ink=new TreeMap<>();
        for(var route:routes){
            var stroke=new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
            ink.computeIfAbsent(route.color(),color->new java.awt.geom.Area()).add(new java.awt.geom.Area(stroke.createStrokedShape(route.path())));
        }
        ink.forEach((color,shape)->{g.setColor(new Color(color));g.fill(shape);});
        // Белый коридор с нейтральным контуром означает переход между отдельными станциями.
        for (var edge : data.edges()) if (edge.color() < 0) {
            var path = new Line2D.Double(points.get(edge.from()), points.get(edge.to()));
            g.setColor(new Color(0x777777)); g.setStroke(new BasicStroke(width + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);
            g.setColor(PAPER); g.setStroke(new BasicStroke(width + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);
        }
        for (var label : plan.labels()) {
            Point2D.Double p = points.get(label.station());
            Rectangle2D box = label.box();
            if (label.leader()) {
                // Сначала пробуем меньший шрифт и перенос; оставшиеся выноски сохраняют связь со станцией.
                double x = Math.max(box.getMinX(), Math.min(p.x, box.getMaxX()));
                double y = Math.max(box.getMinY(), Math.min(p.y, box.getMaxY()));
                g.setColor(new Color(0xB5B2A8)); g.setStroke(new BasicStroke(0.6f)); g.draw(new Line2D.Double(p.x, p.y, x, y));
            }
        }
        Set<Long> terminals = new HashSet<>();
        for (var terminal : NetworkMapGeometry.terminals(data)) terminals.add(terminal.station());
        Map<Long, Label> byStation = new HashMap<>(); plan.labels().forEach(label -> byStation.put(label.station(), label));
        for (var station : data.stations()) {
            var p = points.get(station.id()); var colors = new ArrayList<>(plan.colors().getOrDefault(station.id(), Set.of(0x555555)));
            boolean transfer = plan.transfers().contains(station.id());
            if (transfer) {
                double radius = transferRadius(station.id(), plan, points);
                g.setColor(PAPER); g.fill(new Ellipse2D.Double(p.x - radius, p.y - radius, radius * 2, radius * 2));
                g.setStroke(new BasicStroke((float) Math.min(width > 3 ? 2.5 : 2, Math.max(0.75, radius / 2))));
                for (int i = 0; i < 180; i++) {
                    g.setColor(transferColor(colors, i / 180.0));
                    g.draw(new Arc2D.Double(p.x - radius, p.y - radius, radius * 2, radius * 2, i * 2, 2.2, Arc2D.OPEN));
                }
            } else {
                var mark = NetworkMapGeometry.marker(routes, colors.get(0), p);
                var origin = mark.point(); var normal = mark.normal();
                var label = byStation.get(station.id());
                double side = label == null || (label.box().getCenterX() - p.x) * normal.x + (label.box().getCenterY() - p.y) * normal.y >= 0 ? 1 : -1;
                double length = width > 3 ? 7 : 5;
                g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND)); g.setColor(new Color(colors.get(0)));
                if (terminals.contains(station.id())) g.draw(new Line2D.Double(origin.x - normal.x * length, origin.y - normal.y * length, origin.x + normal.x * length, origin.y + normal.y * length));
                else g.draw(new Line2D.Double(origin.x, origin.y, origin.x + normal.x * length * side, origin.y + normal.y * length * side));
            }
        }
        for (var badge : plan.badges()) drawBadge(g, font, badge.terminal().color(), badge.number(), badge.center().x, badge.center().y, badge.radius());
        for (var label : plan.labels()) {
            font.size(label.size()); g.setColor(PAPER); g.fill(label.box());
            g.setColor(new Color(0x292B30));
            boolean here = label.station() == current;
            for (int i = 0; i < label.rows().size(); i++) {
                double baseline = label.box().getY() + 2 + (i + 1) * (font.height() + 1) - 3;
                if (here) font.drawBold(g, label.rows().get(i), label.box().getX() + 2, baseline);
                else font.draw(g, label.rows().get(i), label.box().getX() + 2, baseline);
            }
            if (here) {
                double x = label.box().getX() + 2, y = label.box().getY() + 4 + label.rows().size() * (font.height() + 1);
                g.setColor(new Color(0xB44B50));
                g.fill(new Rectangle2D.Double(x, y, font.width(youAreHere(russian)) + 6, font.height() + 4));
                g.setColor(Color.WHITE); font.draw(g, youAreHere(russian), x + 3, y + font.height());
            }
        }
        legend(g, data, russian, font);
    }

    public static String youAreHere(boolean russian) { return russian ? "Вы здесь" : "You are here"; }
    /** Замкнутый градиент без шва между последним и первым цветом. */
    public static Color transferColor(List<Integer> colors, double turn) {
        double position = (turn - Math.floor(turn)) * colors.size();
        int index = (int) position;
        Color a = new Color(colors.get(index)), b = new Color(colors.get((index + 1) % colors.size()));
        double t = position - index;
        return new Color((int) Math.round(a.getRed() * (1 - t) + b.getRed() * t),
                (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    public static double transferRadius(long station, Plan plan, Map<Long, Point2D.Double> points) {
        double radius = plan.width() > 3 ? 8 : 5.5;
        for (long other : plan.transfers()) if (other != station) {
            double distance = points.get(station).distance(points.get(other));
            // Две близкие станции не превращаются в один большой круг.
            if (distance > 0) radius = Math.min(radius, Math.max(0.5, (distance - 3) / 2));
        }
        return radius;
    }
    private static String number(NetworkMapData data, int color) { return data.lines().stream().filter(line -> line.color() == color).map(NetworkMapData.Line::number).findFirst().orElse(""); }
    private static void drawBadge(Graphics2D g, NetworkMapFont font, int color, String number, double x, double y, int radius) {
        g.setColor(new Color(color)); g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        font.size(8);
        double brightness = ((color >> 16) & 255) * 0.2126 + ((color >> 8) & 255) * 0.7152 + (color & 255) * 0.0722;
        g.setColor(brightness > 155 ? new Color(0x202124) : Color.WHITE);
        double textWidth = font.width(number);
        if (textWidth > radius * 2 - 4) {
            var text = (Graphics2D) g.create();
            try {
                text.translate(Math.rint(x), Math.rint(y)); text.scale((radius * 2 - 4) / textWidth, 1);
                font.draw(text, number, -textWidth / 2, 3);
            } finally { text.dispose(); }
        } else font.draw(g, number, x - textWidth / 2.0, y + 3);
    }
    public static final int LEGEND_RADIUS = 12;
    public record LegendEntry(NetworkMapData.Line line, List<String> rows, double left, double y) {}
    public static List<LegendEntry> legendEntries(NetworkMapData data, boolean russian, NetworkMapFont font) {
        // Размер легенды независим от плотности текста самой схемы.
        font.density(1);
        int columns = legendColumns(data), rows = legendRows(data);
        int textLimit = Math.max(30, Math.min(400, (1080 - (columns - 1) * 32) / columns - LEGEND_RADIUS * 2 - 6));
        font.size(8);
        List<List<String>> texts = new ArrayList<>();
        double[] widths = new double[columns];
        for (int i = 0; i < data.lines().size(); i++) {
            var wrapped = wrap(NetworkMapPainter.name(data.lines().get(i).name(), russian), font, textLimit - 8);
            List<String> displayed = new ArrayList<>(wrapped.subList(0, Math.min(2, wrapped.size())));
            if (wrapped.size() > 2) displayed.set(1, displayed.get(1) + "…");
            texts.add(List.copyOf(displayed));
            widths[i / rows] = Math.max(widths[i / rows], LEGEND_RADIUS * 2 + 6 + displayed.stream().mapToInt(font::width).max().orElse(0));
        }
        double total = Arrays.stream(widths).sum() + (columns - 1) * 32;
        double left = 1024 - total / 2;
        List<LegendEntry> entries = new ArrayList<>();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                int i = column * rows + row;
                if (i < data.lines().size()) entries.add(new LegendEntry(data.lines().get(i), texts.get(i), left, legendTop(data) + row * 26));
            }
            left += widths[column] + 32;
        }
        return List.copyOf(entries);
    }
    private static void legend(Graphics2D g, NetworkMapData data, boolean russian, NetworkMapFont font) {
        for (var entry : legendEntries(data, russian, font)) {
            drawBadge(g, font, entry.line().color(), entry.line().number(), entry.left() + LEGEND_RADIUS, entry.y(), LEGEND_RADIUS);
            g.setColor(new Color(0x303237));
            for (int row = 0; row < entry.rows().size(); row++) font.draw(g, entry.rows().get(row), entry.left() + LEGEND_RADIUS * 2 + 6, entry.y() - 2 + row * 10);
        }
    }
}
