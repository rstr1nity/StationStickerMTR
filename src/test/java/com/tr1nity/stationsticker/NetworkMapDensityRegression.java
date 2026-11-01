package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import com.tr1nity.stationsticker.map.NetworkMapData;
import java.util.*;

public final class NetworkMapDensityRegression {
    public static void run() {
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
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
