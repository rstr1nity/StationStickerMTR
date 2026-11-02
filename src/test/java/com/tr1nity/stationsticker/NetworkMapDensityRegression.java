package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import com.tr1nity.stationsticker.map.NetworkMapData;
import java.util.*;

public final class NetworkMapDensityRegression {
    public static void run() {
        verifyCurrentStationAndGradient();
        verifyCircularDirections();
        verifyMarkers();
        verifyLegendAndNearbyLabels();
        var stations = List.of(new NetworkMapData.Station(1, "West", -200, 0),
                new NetworkMapData.Station(2, "Middle", 0, 0),
                new NetworkMapData.Station(3, "Interchange", 200, 0),
                new NetworkMapData.Station(4, "Linked station", 200, -200));
        var network = new NetworkMapData(stations,
                List.of(new NetworkMapData.Edge(1, 2, 0xCC2233), new NetworkMapData.Edge(2, 3, 0xCC2233),
                        new NetworkMapData.Edge(4, 3, 0x228833), new NetworkMapData.Edge(2, 4, -1)),
                List.of(new NetworkMapData.Line(0xCC2233, "1", "Red"), new NetworkMapData.Line(0x228833, "LR2", "Green")));
        var points = NetworkMapPainter.layout(network);
        var font = NetworkMapFont.fallback();
        font.size(8);
        var integerGlyphs = new java.awt.geom.Area(font.outline("Station 1124", 10, 20));
        integerGlyphs.exclusiveOr(new java.awt.geom.Area(font.outline("Station 1124", 10.5, 20.5)));
        check(integerGlyphs.isEmpty(), "Half-pixel centering must not distort bitmap glyphs");
        int normalWidth = font.width("Station");
        font.density(2);
        check(Math.abs(font.width("Station") * 2 - normalWidth) <= 1, "Dense text keeps the same glyphs at half the logical width");
        font.density(1);
        var plan = NetworkMapSymbols.plan(network, points, font, false);
        check(plan.badges().size() == 4, "All four numbered ends remain visible, including two on the same station");
        check(plan.transfers().containsAll(Set.of(2L, 3L, 4L)), "Shared routes and linked MTR stations both use interchange symbols");
        for (int i = 0; i < plan.badges().size(); i++) {
            var badge = plan.badges().get(i);
            for (int j = i + 1; j < plan.badges().size(); j++) check(!badge.bounds().intersects(plan.badges().get(j).bounds()), "End badges cannot overlap");
            for (var label : plan.labels()) check(!badge.bounds().intersects(label.box()), "Badges cannot cover station names");
            for (var p : points.values()) check(!badge.bounds().contains(p), "Badges cannot cover a station marker");
        }
        var noNumbers = new NetworkMapData(stations, network.edges(), List.of());
        check(NetworkMapSymbols.plan(noNumbers, points, font, false).badges().isEmpty(), "Missing route numbers do not create empty end circles");
        font.size(8);
        var rows = NetworkMapSymbols.wrap("A long airport station name with transfers", font, 60);
        check(rows.size() > 1 && rows.stream().allMatch(row -> font.width(row) <= 60), "Long names wrap at the measured glyph width");
        check(String.join("", rows).replace(" ", "").equals("Alongairportstationnamewithtransfers"), "Wrapping preserves all name characters");
        List<NetworkMapData.Station> dense = new ArrayList<>();
        for (int i = 0; i < 80; i++) dense.add(new NetworkMapData.Station(i, "Station " + i, i < 79 ? i : 10000, i % 4));
        var data = new NetworkMapData(dense, List.of(), List.of());
        var compressed = NetworkMapSpacing.coordinates(data, true);
        for (var a : dense) for (var b : dense) {
            check(Math.signum(a.x() - b.x()) == Math.signum(compressed.get(a.id()).x - compressed.get(b.id()).x), "Compression preserves west/east order and equal coordinates");
            check(Math.signum(a.z() - b.z()) == Math.signum(compressed.get(a.id()).y - compressed.get(b.id()).y), "Compression preserves north/south order and aligned stations");
        }
        double denseGap = compressed.get(1L).x - compressed.get(0L).x;
        double remoteGap = compressed.get(79L).x - compressed.get(78L).x;
        check(denseGap / remoteGap > 1.0 / (10000 - 78), "Compressing empty space gives the dense interval a larger share of the map");
        System.out.println("PASS: shared-terminal badge separation, transfer classification, wrapped names and monotone gap compression");
    }
    private static void verifyCircularDirections() {
        var clockwise = org.mtr.core.data.Route.CircularState.CLOCKWISE;
        var counter = org.mtr.core.data.Route.CircularState.ANTICLOCKWISE;
        check(com.tr1nity.stationsticker.client.display.CircularDirections.title(clockwise).endsWith("|Clockwise"), "Use explicit MTR clockwise state");
        check(com.tr1nity.stationsticker.client.display.CircularDirections.title(counter).endsWith("|Counterclockwise"), "Use explicit MTR anticlockwise state");
        check(com.tr1nity.stationsticker.client.display.CircularDirections.title(org.mtr.core.data.Route.CircularState.NONE).isEmpty(), "Ordinary routes keep terminal title");
        var a = new org.mtr.core.data.SimplifiedRoutePlatform(1, 11, "A", "");
        var b = new org.mtr.core.data.SimplifiedRoutePlatform(2, 12, "B", "");
        var c = new org.mtr.core.data.SimplifiedRoutePlatform(3, 13, "C", "");
        var stops = List.of(a, b, c, a);
        var next = com.tr1nity.stationsticker.client.display.CircularDirections.nextStops(stops, 2, true, 6);
        check(next.size() == 2 && next.get(0) == a && next.get(1) == b, "Ring wraps once and skips duplicate closing station");
        check(com.tr1nity.stationsticker.client.display.CircularDirections.nextStops(stops, 0, false, 6).equals(List.of(b, c)), "Normal route excludes terminal already shown in heading");
        check(com.tr1nity.stationsticker.client.display.CircularDirections.nextStops(stops, 3, false, 6).isEmpty(), "Normal terminating route has no next stops");
        System.out.println("PASS: explicit MTR circular directions, wrapped stop lists and unchanged ordinary termini");
    }
    private static void verifyCurrentStationAndGradient() {
        var colors = List.of(0xFF0000, 0x00FF00, 0x0000FF);
        check(NetworkMapSymbols.transferColor(colors, 0).getRGB() == new java.awt.Color(colors.get(0)).getRGB(), "Gradient preserves route colors");
        var halfway = NetworkMapSymbols.transferColor(colors, 1.0 / 6);
        check(halfway.getRed() > 100 && halfway.getGreen() > 100, "Interchange has blended colors between stops");
        check(NetworkMapSymbols.transferColor(colors, 0).equals(NetworkMapSymbols.transferColor(colors, 1)), "Gradient closes without seam");
        var network = new NetworkMapData(List.of(new NetworkMapData.Station(1, "Текущая|Current", 0, 0),
                new NetworkMapData.Station(2, "Другая|Other", 100, 0)), List.of(new NetworkMapData.Edge(1, 2, 0xFF0000)), List.of(new NetworkMapData.Line(0xFF0000, "1", "Red")));
        var points = NetworkMapPainter.layout(network);
        for (boolean russian : new boolean[]{true, false}) {
            var font = NetworkMapFont.fallback();
            var plan = NetworkMapSymbols.plan(network, points, font, russian, 1);
            var here = plan.labels().stream().filter(label -> label.station() == 1).findFirst().orElseThrow();
            font.size(here.size());
            check(here.box().getWidth() >= font.width(NetworkMapSymbols.youAreHere(russian)) + 10, "Reserve pill width in the placement solver");
            check(here.box().getHeight() >= here.rows().size() * (font.height() + 1) + font.height() + 12, "Reserve pill height in the placement solver");
            for (var other : plan.labels()) if (other.station() != 1) check(!other.box().intersects(here.box()), "Whole current-station composition avoids labels");
            for (var badge : plan.badges()) check(!badge.bounds().intersects(here.box()), "Whole composition avoids endpoint badges");
            for (var path : NetworkMapGeometry.paths(network, points, plan.width())) check(!new java.awt.BasicStroke(plan.width()).createStrokedShape(path.path()).intersects(here.box()), "Whole composition avoids routes");
            var image = NetworkMapPainter.paint(network, 1, russian, font);
            try { javax.imageio.ImageIO.write(image, "PNG", new java.io.File("network-map-you-are-here-" + (russian ? "ru" : "en") + ".png")); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
            finally { image.flush(); }
        }
        System.out.println("PASS: smooth cyclic interchange colors and reserved bilingual current-station labels");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void verifyMarkers() {
        var corner = new java.awt.geom.Point2D.Double(100, 0);
        var curve = NetworkMapGeometry.rounded(List.of(new java.awt.geom.Point2D.Double(0, 0), corner, new java.awt.geom.Point2D.Double(100, 100)), 24);
        var routes = List.of(new NetworkMapGeometry.LinePath(1, curve));
        var marker = NetworkMapGeometry.marker(routes, 1, corner);
        check(marker.point().distance(corner) > 1, "Regression fixture must round away from the original vertex");
        check(new java.awt.BasicStroke(0.5f).createStrokedShape(curve).contains(marker.point()), "Tick origin must lie on the actual rounded route");
        check(Math.abs(Math.abs(marker.normal().x) - Math.abs(marker.normal().y)) < 0.15, "Bend tick follows local curve normal, not the interstation chord");
        var end = NetworkMapGeometry.marker(routes, 1, new java.awt.geom.Point2D.Double(100, 100));
        check(end.point().distance(100, 100) < 0.01 && Math.abs(end.normal().y) < 0.01, "Terminal cap is attached and perpendicular to the route end");
        var other = new java.awt.geom.Path2D.Double(); other.moveTo(100, -10); other.lineTo(100, 10);
        var filtered = NetworkMapGeometry.marker(List.of(new NetworkMapGeometry.LinePath(2, other), routes.get(0)), 1, corner);
        check(filtered.point().equals(marker.point()), "Nearby routes of another color cannot capture a station tick");
        System.out.println("PASS: ticks and terminal caps attach to rounded curves with the local normal and correct route color");
    }
    private static void verifyLegendAndNearbyLabels() {
        var font = NetworkMapFont.fallback();
        font.size(8);
        int normalWidth = font.width("Station");
        List<NetworkMapData.Line> lines = new ArrayList<>();
        for (int i = 0; i < 25; i++) lines.add(new NetworkMapData.Line(i, i == 0 ? "1124" : "1", i < 12 ? "A" : "A much longer route name " + i));
        var data = new NetworkMapData(List.of(), List.of(), lines);
        font.density(2);
        var legend = NetworkMapSymbols.legendEntries(data, false, font);
        check(font.width("Station") == normalWidth, "Legend text must ignore diagram density");
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        font.size(8);
        for (var entry : legend) {
            min = Math.min(min, entry.left());
            max = Math.max(max, entry.left() + NetworkMapSymbols.LEGEND_RADIUS * 2 + 6 + entry.rows().stream().mapToInt(font::width).max().orElse(0));
        }
        check(Math.abs((min + max) / 2 - 1024) < 0.01, "Legend content, including circles and unequal column widths, must center on the block");
        check(NetworkMapSymbols.LEGEND_RADIUS * 2 < 26, "Uniform legend circles must fit the row pitch");
        List<NetworkMapData.Station> stations = new ArrayList<>();
        Map<Long, java.awt.geom.Point2D.Double> points = new LinkedHashMap<>();
        for (long i = 0; i < 4; i++) {
            stations.add(new NetworkMapData.Station(i, "Station " + i, 0, 0));
            points.put(i, new java.awt.geom.Point2D.Double(1000 + i % 2 * 20, 800 + i / 2 * 20));
        }
        var labels = NetworkMapLabelLayout.place(new NetworkMapData(stations, List.of(), List.of()), points, font, false, List.of(), List.of(), 1530);
        for (int i = 0; i < labels.size(); i++) {
            check(NetworkMapLabelLayout.distance(points.get(labels.get(i).station()), labels.get(i).box()) <= 28, "Nearby free space must be used before long leaders");
            for (int j = i + 1; j < labels.size(); j++) check(!labels.get(i).box().intersects(labels.get(j).box()), "Joint label assignment cannot create overlaps");
        }
        System.out.println("PASS: uniformly sized legend circles, actual-content centering and joint nearby-label placement");
    }
}
