package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import com.tr1nity.stationsticker.map.NetworkMapData;
import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public final class NetworkMapStyleRegression {
    public static void run() throws Exception {
        check(NetworkMapPainter.name("中文|Завод|Factory||note", true).equals("Завод"), "Russian preference");
        check(NetworkMapPainter.name("中文|Завод|Factory", false).equals("Factory"), "English preference over Chinese");
        check(NetworkMapPainter.name("Завод R|Factory", false).equals("Factory"), "Mixed Cyrillic text is not an English translation");
        check(NetworkMapPainter.name("中文|Factory", true).equals("Factory"), "English fallback when Russian is missing");
        check(NetworkMapPainter.name("中文", false).equals("中文"), "Original-language fallback");
        Set<Integer> needed = new HashSet<>();
        "Станция Центральная Красная Синяя Factory Station Line IIIWWW0123456789中文…".codePoints().forEach(needed::add);
        NetworkMapFont font = NetworkMapFont.load(id -> {
            String[] path = id.split(":", 2);
            var input = NetworkMapStyleRegression.class.getResourceAsStream("/assets/" + path[0] + "/" + path[1]);
            if (input == null) throw new IOException("Missing Minecraft font resource: " + id);
            return input;
        }, needed);
        font.size(16);
        check(font.width("III") < font.width("WWW"), "Actual proportional Minecraft bitmap glyphs loaded");
        var rounded = NetworkMapGeometry.rounded(List.of(new Point2D.Double(0, 0), new Point2D.Double(100, 0), new Point2D.Double(100, 100)), 24);
        boolean curved = false;
        for (var iterator = rounded.getPathIterator(null); !iterator.isDone(); iterator.next()) {
            if (iterator.currentSegment(new double[6]) == PathIterator.SEG_QUADTO) curved = true;
        }
        check(curved, "Line bends use quadratic curves");
        List<Rectangle2D> occupied = new ArrayList<>();
        occupied.add(new Rectangle2D.Double(1010, 880, 28, 28));
        List<Shape> lines = List.of(new BasicStroke(18).createStrokedShape(new Line2D.Double(400, 900, 1650, 900)));
        for (int i = 0; i < 50; i++) {
            var placement = NetworkMapLabels.place(new Point2D.Double(1024, 900), 130, 26, occupied, lines, 1530);
            for (var previous : occupied) check(!previous.intersects(placement.box()), "Dense-node names must not overlap");
            for (var line : lines) check(!line.intersects(placement.box()), "Names must not overlap route geometry");
            occupied.add(placement.box());
        }
        List<NetworkMapData.Station> stations = new ArrayList<>();
        List<NetworkMapData.Edge> edges = new ArrayList<>();
        List<NetworkMapData.Line> routes = new ArrayList<>();
        for (int line = 0; line < 6; line++) {
            int color = Color.HSBtoRGB(line / 6f, 0.8f, 0.7f) & 0xFFFFFF;
            routes.add(new NetworkMapData.Line(color, "" + (line + 1), "中文|Линия " + (line + 1) + "|Line " + (line + 1)));
            for (int stop = 0; stop < 10; stop++) {
                long id = line * 10L + stop;
                double x = stop == 0 ? -400 : stop == 9 ? 400 : (stop - 5) * 10;
                double z = stop == 0 || stop == 9 ? (line - 3) * 80 : (line - 3) * 7;
                stations.add(new NetworkMapData.Station(id, "中文|Станция " + id + "|Station " + id, x, z));
                if (stop > 0) edges.add(new NetworkMapData.Edge(id - 1, id, color));
            }
        }
        var data = new NetworkMapData(stations, edges, routes);
        check(NetworkMapGeometry.terminals(data).size() == 12, "A badge at each end of every line");
        var image = NetworkMapPainter.paint(data, 15, true, font);
        javax.imageio.ImageIO.write(image, "PNG", new java.io.File("network-map-style-preview.png")); image.flush();
        System.out.println("PASS: Minecraft bitmap font, language selection, curved routes, terminal badges and 50 nonoverlapping dense-node labels");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
