package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.client.display.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.core.data.SimplifiedRoute;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Ordered MTR stops, never reconstructed from geographic proximity. */
public record VerticalLineData(int color, String number, String heading, List<Stop> stops, List<Transfer> legend) {
    public record Transfer(int color, String number, String name, boolean circular) {}
    public record Stop(String name, String secondary, boolean current, boolean passed, List<Transfer> transfers) {}
    public static VerticalLineData at(BlockPos pos, int selection) {
        var platform = PlatformLocator.findPlatformAt(pos);
        if (platform == null) return null;
        var routes = MinecraftClientData.getInstance().simplifiedRoutes.stream()
                .filter(r -> r.getPlatforms().stream().anyMatch(stop -> stop.getStationId() == platform.area.getId()))
                .sorted(Comparator.comparingInt((SimplifiedRoute r) -> r.getPlatformIndex(platform.getId()) >= 0 ? 0 : 1).thenComparingLong(SimplifiedRoute::getId)).toList();
        if (routes.isEmpty()) return null;
        var route = routes.get(selection % routes.size());
        int current = 0;
        for (int i = 0; i < route.getPlatforms().size(); i++) if (route.getPlatforms().get(i).getStationId() == platform.area.getId()) { current = i; break; }
        var stops = route.getPlatforms();
        List<Stop> rows = new ArrayList<>();
        Map<Integer, Transfer> legend = new TreeMap<>();
        for (int i = 0; i < stops.size(); i++) {
            var stop = stops.get(i);
            Map<Integer, Transfer> transfers = new TreeMap<>();
            stop.forEach((color, names) -> {
                if (color == route.getColor()) return;
                boolean circular = MinecraftClientData.getInstance().simplifiedRoutes.stream()
                        .anyMatch(r -> r.getColor() == color && !CircularDirections.title(r.getCircularState()).isEmpty()
                                && r.getPlatforms().stream().anyMatch(s -> s.getStationId() == stop.getStationId()));
                var matching = MinecraftClientData.getInstance().simplifiedRoutes.stream().filter(r -> r.getColor() == color).findFirst();
                String name = matching.map(r -> StationNames.forCurrentLanguage(r.getName())).orElse("");
                transfers.put(color, new Transfer(color, RouteNumbers.forColor(color), name, circular));
            });
            legend.putAll(transfers);
            rows.add(new Stop(StationNames.primary(stop.getStationName()), StationNames.secondary(stop.getStationName()),
                    i == current, i < current, List.copyOf(transfers.values())));
        }
        String circular = CircularDirections.title(route.getCircularState());
        String heading = circular.isEmpty() ? rows.get(rows.size() - 1).name() : StationNames.forCurrentLanguage(circular);
        legend.put(route.getColor(), new Transfer(route.getColor(), RouteNumbers.forRoute(route), StationNames.forCurrentLanguage(route.getName()), !circular.isEmpty()));
        return new VerticalLineData(route.getColor(), RouteNumbers.forRoute(route), heading, List.copyOf(rows), List.copyOf(legend.values()));
    }
}
