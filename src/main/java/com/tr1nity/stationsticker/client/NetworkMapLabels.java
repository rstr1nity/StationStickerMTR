package com.tr1nity.stationsticker.client;

import java.awt.Shape;
import java.awt.geom.*;
import java.util.List;

public final class NetworkMapLabels {
    public record Placement(Rectangle2D box, boolean leader) {}
    private NetworkMapLabels() {}
    public static Placement place(Point2D.Double station, int width, int height, List<Rectangle2D> occupied, List<Shape> lines, int bottom) {
        Rectangle2D best = null; double bestScore = Double.POSITIVE_INFINITY;
        for (int radius : new int[]{18, 30, 48, 72, 110, 160}) {
            double[][] offsets = {{radius, -height}, {radius, 5}, {-width - radius, -height}, {-width - radius, 5}, {-width / 2.0, -height - radius}, {-width / 2.0, radius}};
            for (double[] offset : offsets) {
                Rectangle2D box = new Rectangle2D.Double(Math.rint(station.x + offset[0]), Math.rint(station.y + offset[1]), width + 6, height + 6);
                double score = score(box, occupied, lines, bottom);
                if (score == 0) return new Placement(box, radius > 48);
                if (score < bestScore) { best = box; bestScore = score; }
            }
        }
        // Плотный узел: ищем свободное место во всей области схемы и добавляем выноску.
        double distance = Double.POSITIVE_INFINITY;
        Rectangle2D free = null;
        for (int y = 300; y + height < bottom; y += height + 10) for (int x = 150; x + width < 1900; x += 24) {
            Rectangle2D box = new Rectangle2D.Double(x, y, width + 6, height + 6);
            double d = Point2D.distanceSq(station.x, station.y, box.getCenterX(), box.getCenterY());
            if (d < distance && score(box, occupied, lines, bottom) == 0) { free = box; distance = d; }
        }
        return new Placement(free == null ? best : free, true);
    }
    private static double score(Rectangle2D box, List<Rectangle2D> occupied, List<Shape> lines, int bottom) {
        if (box.getMinY() < 290 || box.getMaxY() > bottom) return 1e12;
        for (double x : new double[]{box.getMinX(), box.getMaxX()}) for (double y : new double[]{box.getMinY(), box.getMaxY()}) {
            if (Point2D.distance(x, y, 1024, 1024) > 940) return 1e12;
        }
        double score = 0;
        for (var previous : occupied) if (previous.intersects(box)) score += 1_000_000;
        for (var line : lines) if (line.intersects(box)) score += 10_000;
        return score;
    }
}
