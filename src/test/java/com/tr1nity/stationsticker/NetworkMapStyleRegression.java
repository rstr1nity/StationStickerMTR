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
        verifyUnifontArchive();
        verifyGeography();
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

    private static void verifyGeography() throws Exception {
        var stations = List.of(new NetworkMapData.Station(1, "West", -1000, 20),
                new NetworkMapData.Station(2, "Middle", -990, 20),
                new NetworkMapData.Station(3, "East", 1000, 20),
                new NetworkMapData.Station(4, "North", 1000, -180));
        var data = new NetworkMapData(stations, List.of(), List.of());
        var points = NetworkMapPainter.layout(data);
        var west = points.get(1L); var middle = points.get(2L); var east = points.get(3L); var north = points.get(4L);
        check(west.y == middle.y && middle.y == east.y, "Collinear stations must not be pushed apart for labels");
        check(east.x == north.x && north.y < east.y, "World north is up, equal X coordinates stay aligned");
        double scale = (east.x - west.x) / 2000;
        check(Math.abs((middle.x - west.x) - 10 * scale) < 1e-8, "Unequal world distances keep their ratio");
        check(Math.abs((east.y - north.y) - 200 * scale) < 1e-8, "X and Z use the same scale");
        var otherLanguage = NetworkMapPainter.layout(data, NetworkMapFont.fallback(), false);
        check(points.equals(otherLanguage), "Font and language must not move stations");
        var localData = java.nio.file.Path.of("mtr-local-map-data.json");
        if (java.nio.file.Files.exists(localData)) {
            var root = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(localData)).getAsJsonObject().getAsJsonObject("data");
            Map<Long, double[]> positions = new TreeMap<>();
            List<NetworkMapData.Edge> edges = new ArrayList<>();
            Map<Integer, NetworkMapData.Line> lines = new TreeMap<>();
            for (var element : root.getAsJsonArray("routes")) {
                var route = element.getAsJsonObject(); var stops = route.getAsJsonArray("stations");
                if (route.get("hidden").getAsBoolean() || stops.size() < 2) continue;
                int color = route.get("color").getAsInt();
                lines.putIfAbsent(color, new NetworkMapData.Line(color, route.get("number").getAsString(), route.get("name").getAsString()));
                Long previous = null;
                for (var stopElement : stops) {
                    var stop = stopElement.getAsJsonObject(); long id = Long.parseUnsignedLong(stop.get("id").getAsString(), 16);
                    var sum = positions.computeIfAbsent(id, key -> new double[3]);
                    sum[0] += stop.get("x").getAsDouble(); sum[1] += stop.get("z").getAsDouble(); sum[2]++;
                    if (previous != null && previous != id) {
                        var edge = new NetworkMapData.Edge(Math.min(previous, id), Math.max(previous, id), color);
                        if (!edges.contains(edge)) edges.add(edge);
                    }
                    previous = id;
                }
            }
            List<NetworkMapData.Station> liveStations = new ArrayList<>();
            for (var element : root.getAsJsonArray("stations")) {
                var station = element.getAsJsonObject(); long id = Long.parseUnsignedLong(station.get("id").getAsString(), 16);
                var sum = positions.get(id);
                if (sum != null) liveStations.add(new NetworkMapData.Station(id, station.get("name").getAsString(), sum[0] / sum[2], sum[1] / sum[2]));
            }
            var live = new NetworkMapData(liveStations, edges, new ArrayList<>(lines.values()));
            var projected = NetworkMapPainter.layout(live);
            double liveScale = 0;
            for (int i = 0; i < liveStations.size(); i++) for (int j = i + 1; j < liveStations.size(); j++) {
                var a = liveStations.get(i); var b = liveStations.get(j);
                double distance = Math.hypot(a.x() - b.x(), a.z() - b.z());
                if (distance == 0) continue;
                double ratio = projected.get(a.id()).distance(projected.get(b.id())) / distance;
                if (liveScale == 0) liveScale = ratio;
                check(Math.abs(ratio - liveScale) < 1e-8, "Live MTR map keeps every pairwise distance ratio");
            }
            var image = NetworkMapPainter.paint(live, 0, true);
            javax.imageio.ImageIO.write(image, "PNG", new java.io.File("network-map-local-world-preview.png")); image.flush();
            System.out.println("PASS: live localhost MTR network uses identical station coordinates and uniform world scale");
        }
        System.out.println("PASS: geographic projection preserves alignment, distance ratios and directions independently of labels");
    }

    private static void verifyUnifontArchive() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(bytes)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("LICENSE.txt"));
            zip.write("release is licensed as follows: not glyph data".getBytes(java.nio.charset.StandardCharsets.UTF_8)); zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("font.hex"));
            String rows = "release is licensed as follows: malformed comment\nGGGG:BAD\n0041:XYZ\n0041:" + "FF".repeat(16) + "\n";
            zip.write(rows.getBytes(java.nio.charset.StandardCharsets.UTF_8)); zip.closeEntry();
        }
        byte[] provider = "{\"providers\":[{\"type\":\"unihex\",\"hex_file\":\"minecraft:font/test.zip\"}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var sampleFont = NetworkMapFont.load(id -> new java.io.ByteArrayInputStream(id.endsWith(".zip") ? bytes.toByteArray() : provider), Set.of(65));
        sampleFont.size(8);
        check(sampleFont.width("A") == 5, "Valid glyph after license, comments and malformed rows must be loaded");

        var assets = java.nio.file.Path.of(System.getProperty("stationsticker.test.assets"));
        com.google.gson.JsonObject objects = null;
        try (var indexes = java.nio.file.Files.list(assets.resolve("indexes"))) {
            for (var index : indexes.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (var reader = java.nio.file.Files.newBufferedReader(index)) {
                    var candidate = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("objects");
                    if (candidate.has("minecraft/font/unifont.zip")) { objects = candidate; break; }
                }
            }
        }
        check(objects != null, "Installed Minecraft Unifont archive must be tested");
        final var assetObjects = objects;
        NetworkMapFont realFont = NetworkMapFont.load(id -> {
            String[] location = id.split(":", 2);
            String key = location[0] + "/" + location[1];
            if (assetObjects.has(key)) {
                String hash = assetObjects.getAsJsonObject(key).get("hash").getAsString();
                return java.nio.file.Files.newInputStream(assets.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash));
            }
            var input = NetworkMapStyleRegression.class.getResourceAsStream("/assets/" + key);
            if (input == null) throw new IOException("Missing resource " + key);
            return input;
        }, Set.of((int) 'A', (int) 'Я', (int) '中'));
        realFont.size(8);
        check(realFont.width("中") == 9, "Chinese glyph must come from the installed 16-pixel Unifont, not a fallback");
        System.out.println("PASS: Unifont archive ignores license/README and malformed records; installed Minecraft archive loads Russian, Latin and Chinese glyphs");
    }
}
