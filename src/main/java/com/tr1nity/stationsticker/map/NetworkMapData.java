package com.tr1nity.stationsticker.map;

import net.minecraft.network.FriendlyByteBuf;
import java.util.List;

// Только публичная информация схемы, без депо, расписаний и данных игроков.
public record NetworkMapData(List<Station> stations, List<Edge> edges, List<Line> lines) {
    public static final NetworkMapData EMPTY = new NetworkMapData(List.of(), List.of(), List.of());
    public record Station(long id, String name, double x, double z) {}
    public record Edge(long from, long to, int color) {}
    public record Line(int color, String number, String name) {}

    public NetworkMapData {
        stations = List.copyOf(stations);
        edges = List.copyOf(edges);
        lines = List.copyOf(lines);
    }

    public NetworkMapData(FriendlyByteBuf buf) {
        this(buf.readList(b -> new Station(b.readLong(), b.readUtf(256), b.readDouble(), b.readDouble())),
                buf.readList(b -> new Edge(b.readLong(), b.readLong(), b.readInt())),
                buf.readList(b -> new Line(b.readInt(), b.readUtf(64), b.readUtf(256))));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(stations, (b, s) -> { b.writeLong(s.id); b.writeUtf(s.name, 256); b.writeDouble(s.x); b.writeDouble(s.z); });
        buf.writeCollection(edges, (b, e) -> { b.writeLong(e.from); b.writeLong(e.to); b.writeInt(e.color); });
        buf.writeCollection(lines, (b, l) -> { b.writeInt(l.color); b.writeUtf(l.number, 64); b.writeUtf(l.name, 256); });
    }
}
