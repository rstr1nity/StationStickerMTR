package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import com.tr1nity.stationsticker.map.NetworkMapData;
import java.util.*;

public final class NetworkMapSchematicRegression {
    public static void run() {
        var stations = List.of(new NetworkMapData.Station(1, "West", -200, 0),
                new NetworkMapData.Station(2, "North", 0, -40), new NetworkMapData.Station(3, "East", 70, 0),
                new NetworkMapData.Station(4, "South", 0, 100), new NetworkMapData.Station(5, "Branch", 180, 180));
        var edges = List.of(new NetworkMapData.Edge(1, 2, 1), new NetworkMapData.Edge(2, 3, 1),
                new NetworkMapData.Edge(3, 4, 1), new NetworkMapData.Edge(4, 1, 1), new NetworkMapData.Edge(3, 5, 2));
        var data = new NetworkMapData(stations, edges, List.of(new NetworkMapData.Line(1, "1", "Ring", true), new NetworkMapData.Line(2, "2", "Branch")));
        var original = NetworkMapPainter.layout(data);
        var points = NetworkMapPainter.layout(data, 3);
        var rings = NetworkMapSchematic.rings(data);
        check(rings.size() == 1, "Only the explicit same-color MTR ring is selected");
        var circle = NetworkMapSchematic.circle(rings.get(0), points);
        check(circle != null && Math.abs(circle.getWidth() - circle.getHeight()) < 0.0001, "Uniform final fit preserves an exact circle");
        for (long id : rings.get(0).stations()) check(Math.abs(points.get(id).distance(circle.getCenterX(), circle.getCenterY()) - circle.getWidth() / 2) < 0.0001, "Ring station lies on the rendered circle");
        for (var route : NetworkMapGeometry.paths(data, points, 5, true)) {
            if (route.color() == 1) {
                check(Math.abs(route.path().getBounds2D().getWidth() - route.path().getBounds2D().getHeight()) < 0.0001, "Ring bounds are circular within coordinate precision");
                boolean closed = false, curved = false;
                for (var iterator = route.path().getPathIterator(null); !iterator.isDone(); iterator.next()) {
                    int segment = iterator.currentSegment(new double[6]);
                    closed |= segment == java.awt.geom.PathIterator.SEG_CLOSE;
                    curved |= segment == java.awt.geom.PathIterator.SEG_CUBICTO;
                }
                check(closed && curved, "Ring is drawn as a closed curve, not polygon chords");
            }
        }
        check(original.equals(NetworkMapPainter.layout(data)), "Schematic solver must not mutate the old mode or snapshot");
        var ordinary = new NetworkMapData(stations, edges, List.of(new NetworkMapData.Line(1, "1", "Not a ring")));
        check(NetworkMapSchematic.rings(ordinary).isEmpty(), "A geometric cycle alone does not imply an MTR circular route");
        var branchedEdges = new ArrayList<>(edges); branchedEdges.add(new NetworkMapData.Edge(1, 5, 1));
        check(NetworkMapSchematic.rings(new NetworkMapData(stations, branchedEdges, data.lines())).isEmpty(), "Branched same-color network is not forced into one fake circle");
        var bytes = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try { data.write(bytes); check(new NetworkMapData(bytes).equals(data), "Circular metadata survives network roundtrip"); }
        finally { bytes.release(); }
        System.out.println("PASS: explicit ring classification, exact circular geometry, shared nodes, packet roundtrip and unchanged original mode");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
