package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.client.display.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.core.data.SimplifiedRoute;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Ordered MTR stops, never reconstructed from geographic proximity. */
public record VerticalLineData(int color, String number, String heading, List<Stop> stops, List<Transfer> legend, String direction, boolean circular, int rotation, List<List<Stop>> branches) {
    public VerticalLineData(int color,String number,String heading,List<Stop> stops,List<Transfer> legend,String direction,boolean circular,int rotation){this(color,number,heading,stops,legend,direction,circular,rotation,List.of());}
    public VerticalLineData(int color, String number, String heading, List<Stop> stops, List<Transfer> legend, String direction, boolean circular) {
        this(color, number, heading, stops, legend, direction, circular, 0);
    }
    public VerticalLineData(int color, String number, String heading, List<Stop> stops, List<Transfer> legend) {
        this(color, number, heading, stops, legend, "", false, 0);
    }
    public record Transfer(int color, String number, String name, boolean circular, boolean walking) {
        public Transfer(int color, String number, String name, boolean circular) { this(color, number, name, circular, false); }
    }
    public record Stop(String name, String secondary, boolean current, boolean passed, List<Transfer> transfers, long stationId, List<String> icons) {
        public Stop(String name,String secondary,boolean current,boolean passed,List<Transfer> transfers){this(name,secondary,current,passed,transfers,0,List.of());}
    }
    public List<Stop> allStops(){var result=new ArrayList<>(stops);branches.forEach(result::addAll);return result;}
    public static VerticalLineData at(BlockPos pos, int selection) {
        var network = NetworkMapCache.snapshot(pos);
        var platform = PlatformLocator.findPlatformAt(pos);
        if (platform == null) return null;
        var candidates = routes(platform.area.getId(), platform.getId());
        if (candidates.isEmpty()) return null;
        var boarding = candidates.stream().filter(r -> canBoard(r, platform.area.getId())).toList();
        var routes = boarding.isEmpty() ? candidates : boarding;
        var settings = net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(pos) instanceof com.tr1nity.stationsticker.block.entity.VerticalLineBlockEntity entity ? entity.settings() : com.tr1nity.stationsticker.block.VerticalLineSettings.DEFAULT;
        var route = routes.stream().filter(r -> r.getId() == settings.routeId()).findFirst().orElse(routes.get(selection % routes.size()));
        int current = 0;
        for (int i = 0; i < route.getPlatforms().size(); i++) if (route.getPlatforms().get(i).getStationId() == platform.area.getId()) { current = i; break; }
        var stops = route.getPlatforms();
        List<Stop> rows = new ArrayList<>();
        Map<Integer, Transfer> legend = new TreeMap<>();
        for (int i = 0; i < stops.size(); i++) {
            var stop = stops.get(i);
            rows.add(row(stop,i==current,i<current,route,network,settings));
        }
        String circular = CircularDirections.title(route.getCircularState());
        String heading = circular.isEmpty() ? rows.get(rows.size() - 1).name() : rows.get(current).name();
        if (!circular.isEmpty()) {
            if (stops.size() > 1 && stops.get(0).getStationId() == stops.get(stops.size()-1).getStationId()) rows.remove(rows.size()-1);
            rows = ringStops(rows, current);
        }
        String direction = circular.isEmpty() ? (boarding.isEmpty() ? StationNames.forCurrentLanguage("Посадки нет|No boarding") : "") : StationNames.forCurrentLanguage(circular);
        legend.put(route.getColor(), new Transfer(route.getColor(), RouteNumbers.forRoute(route), StationNames.forCurrentLanguage(route.getName()), !circular.isEmpty()));
        List<List<Stop>> alternatives=new ArrayList<>();
        if(circular.isEmpty())for(var other:MinecraftClientData.getInstance().simplifiedRoutes){
            if(other.getId()==route.getId()||other.getColor()!=route.getColor()||!CircularDirections.title(other.getCircularState()).isEmpty())continue;
            long boardingPlatform=stops.get(current).getPlatformId();
            int boardingIndex=other.getPlatformIndex(boardingPlatform);
            List<Stop> path=new ArrayList<>();
            for(int i=0;i<other.getPlatforms().size();i++)path.add(row(other.getPlatforms().get(i),false,boardingIndex<0 || i<boardingIndex,route,network,settings));
            alternatives.add(path);
        }
        var branches=VerticalLineBranches.compatible(rows,alternatives);
        rows.forEach(stop->stop.transfers().forEach(t->legend.put(t.color(),t)));
        branches.forEach(path->path.forEach(stop->stop.transfers().forEach(t->legend.put(t.color(),t))));
        return new VerticalLineData(route.getColor(), RouteNumbers.forRoute(route), heading, List.copyOf(rows), List.copyOf(legend.values()), direction, !circular.isEmpty(), circular.isEmpty() ? 0 : route.getCircularState() == org.mtr.core.data.Route.CircularState.CLOCKWISE ? 1 : -1, branches);
    }
    private static Stop row(org.mtr.core.data.SimplifiedRoutePlatform stop,boolean isCurrent,boolean passed,SimplifiedRoute route,com.tr1nity.stationsticker.map.NetworkMapData network,com.tr1nity.stationsticker.block.VerticalLineSettings settings){
            Map<Integer, Transfer> transfers = new TreeMap<>();
            stop.forEach((color, names) -> {
                if (color == route.getColor()) return;
                boolean circular = MinecraftClientData.getInstance().simplifiedRoutes.stream()
                        .anyMatch(r -> r.getColor() == color && !CircularDirections.title(r.getCircularState()).isEmpty()
                                && r.getPlatforms().stream().anyMatch(s -> s.getStationId() == stop.getStationId()));
                if (network != null) circular = network.lines().stream().anyMatch(line -> line.color() == color && line.circular());
                var matching = MinecraftClientData.getInstance().simplifiedRoutes.stream().filter(r -> r.getColor() == color).findFirst();
                String name = matching.map(r -> StationNames.forCurrentLanguage(r.getName())).orElse("");
                transfers.put(color, new Transfer(color, RouteNumbers.forColor(color), name, circular, walking(network, stop.getStationId(), color)));
            });
            return new Stop(StationNames.primary(stop.getStationName()), StationNames.secondary(stop.getStationName()),
                    isCurrent, passed, List.copyOf(transfers.values()), stop.getStationId(), settings.icons().getOrDefault(stop.getStationId(),List.of()));
    }
    public static List<SimplifiedRoute> routes(long station,long platformId) {
        return MinecraftClientData.getInstance().simplifiedRoutes.stream()
                .filter(r -> r.getPlatforms().stream().anyMatch(stop -> stop.getStationId() == station))
                .sorted(Comparator.comparingInt((SimplifiedRoute r) -> r.getPlatformIndex(platformId) >= 0 ? 0 : 1).thenComparingLong(SimplifiedRoute::getId)).toList();
    }
    /** Explicit MTR station links only; a direct interchange of the same color wins. */
    public static boolean walking(com.tr1nity.stationsticker.map.NetworkMapData network, long station, int color) {
        if (network == null) return false;
        if (network.edges().stream().anyMatch(e -> e.color() == color && (e.from() == station || e.to() == station))) return false;
        Set<Long> linked = new HashSet<>();
        for (var edge : network.edges()) if (edge.color() == -1) {
            if (edge.from() == station) linked.add(edge.to());
            if (edge.to() == station) linked.add(edge.from());
        }
        return network.edges().stream().anyMatch(e -> e.color() == color && (linked.contains(e.from()) || linked.contains(e.to())));
    }
    public static boolean canBoard(SimplifiedRoute route, long station) {
        if (!CircularDirections.title(route.getCircularState()).isEmpty()) return true;
        for (int i = 0; i + 1 < route.getPlatforms().size(); i++)
            if (route.getPlatforms().get(i).getStationId() == station) return true;
        return false;
    }
    /** Start a ring at the current stop and omit MTR's repeated closing stop. */
    public static List<Stop> ringStops(List<Stop> stops, int current) {
        List<Stop> result = new ArrayList<>();
        int count = stops.size();
        if (count == 0) return result;
        current %= count;
        for (int offset = 0; offset < count; offset++) {
            var stop = stops.get((current + offset) % count);
            result.add(new Stop(stop.name(), stop.secondary(), offset == 0, false, stop.transfers(), stop.stationId(), stop.icons()));
        }
        return List.copyOf(result);
    }
}
