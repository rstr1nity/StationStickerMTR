package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import com.tr1nity.stationsticker.map.*;
import org.mtr.core.data.*;
import org.mtr.core.serializer.*;
import java.nio.file.*;
import java.util.*;

/** Только чтение сохранения. Не создаёт Simulator и не записывает данные мира. */
public final class NetworkMapWorldPreview {
    public static void run() throws Exception {
        Path root = Path.of("../run-1.20.1/saves/MTR World - test/mtr/minecraft/overworld");
        if (!Files.isDirectory(root)) return;
        ClientData data = new ClientData();
        read(root.resolve("stations"), reader -> data.stations.add(new Station(reader, data)));
        read(root.resolve("platforms"), reader -> data.platforms.add(new Platform(reader, data)));
        read(root.resolve("routes"), reader -> data.routes.add(new Route(reader, data)));
        data.sync();
        var response = new org.mtr.core.operation.ListDataResponse(data).list();
        var network = new MtrNetworkMapResponse(new JsonReader(org.mtr.core.tool.Utilities.getJsonObjectFromData(response))).map;
        if (network.stations().isEmpty()) throw new AssertionError("Saved user network must not be empty");
        Set<Integer> needed = new HashSet<>();
        network.stations().forEach(s -> s.name().codePoints().forEach(needed::add));
        network.lines().forEach(s -> (s.name() + s.number()).codePoints().forEach(needed::add));
        "СХЕМА МЕТРО METRO NETWORK Station Sticker …0123456789".codePoints().forEach(needed::add);
        var font = NetworkMapFont.load(id -> {
            String[] path = id.split(":", 2);
            var input = NetworkMapWorldPreview.class.getResourceAsStream("/assets/" + path[0] + "/" + path[1]);
            if (input == null) throw new java.io.IOException(id);
            return input;
        }, needed);
        for (int spacing : new int[]{0, 1, 2}) {
            var points = NetworkMapPainter.layout(network, spacing);
            long start = System.nanoTime();
            var plan = NetworkMapSymbols.plan(network, points, font, false);
            int overlaps = 0, badgeOverlaps = 0, distant = 0, markerOverlaps = 0;
            var stationIds = new ArrayList<>(points.keySet());
            for (int i = 0; i < stationIds.size(); i++) for (int j = i + 1; j < stationIds.size(); j++) {
                long a = stationIds.get(i), b = stationIds.get(j);
                if (plan.transfers().contains(a) && plan.transfers().contains(b)
                        && points.get(a).distance(points.get(b)) < NetworkMapSymbols.transferRadius(a, plan, points) + NetworkMapSymbols.transferRadius(b, plan, points) + 2) markerOverlaps++;
            }
            for (int i = 0; i < plan.labels().size(); i++) {
                var label = plan.labels().get(i);
                var p = points.get(label.station()); var box = label.box();
                if (p.distance(Math.max(box.getMinX(), Math.min(p.x, box.getMaxX())), Math.max(box.getMinY(), Math.min(p.y, box.getMaxY()))) > 150) distant++;
                for (int j = i + 1; j < plan.labels().size(); j++) if (label.box().intersects(plan.labels().get(j).box())) overlaps++;
                for (var badge : plan.badges()) if (label.box().intersects(badge.bounds())) badgeOverlaps++;
            }
            for (int i = 0; i < plan.badges().size(); i++) for (int j = i + 1; j < plan.badges().size(); j++)
                if (plan.badges().get(i).bounds().intersects(plan.badges().get(j).bounds())) badgeOverlaps++;
            String variant = spacing == 0 ? "geographic" : spacing == 1 ? "compact" : "equalized";
            long numberedEnds = NetworkMapGeometry.terminals(network).stream().filter(terminal -> network.lines().stream().anyMatch(line -> line.color() == terminal.color() && !line.number().isBlank())).count();
            System.out.printf("USER NETWORK %s: %d stations, %d lines, %d labels, %d/%d badges; label overlaps=%d, badge overlaps=%d, close transfer markers=%d, distant labels=%d, layout=%dms%n",
                    variant, network.stations().size(), network.lines().size(), plan.labels().size(), plan.badges().size(), numberedEnds, overlaps, badgeOverlaps, markerOverlaps, distant, (System.nanoTime() - start) / 1_000_000);
            if (overlaps > 0 || badgeOverlaps > 0 || markerOverlaps > 0) throw new AssertionError("User network symbols/labels overlap: " + variant);
            var image = NetworkMapPainter.paint(network, network.stations().get(0).id(), false, font, spacing);
            javax.imageio.ImageIO.write(image, "PNG", new java.io.File("network-map-user-" + variant + ".png")); image.flush();
        }
    }
    private static void read(Path directory, java.util.function.Consumer<ReaderBase> callback) throws Exception {
        try (var files = Files.walk(directory)) {
            for (var file : files.filter(Files::isRegularFile).filter(file -> !file.getFileName().toString().startsWith(".")).sorted().toList()) {
                try (var unpacker = org.mtr.libraries.org.msgpack.core.MessagePack.newDefaultUnpacker(Files.readAllBytes(file))) {
                    callback.accept(new MessagePackReader(unpacker));
                }
            }
        }
    }
}
