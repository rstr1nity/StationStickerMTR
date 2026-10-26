package com.tr1nity.stationsticker.client.display;

import com.tr1nity.stationsticker.client.PlatformLocator;
import net.minecraft.core.BlockPos;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Пересадки по данным MTR: другие линии на той же станции и на станциях, чьи области с ней пересекаются
public final class Transfers {

    public record Transfer(long stationId, String stationName, int color, String routeName, String routeNumber) {
    }

    private Transfers() {
    }

    // Варианты для окна настройки: все пересадки станции, где стоит блок, кроме линий ближайшей платформы
    public static List<Transfer> optionsAt(BlockPos pos) {
        List<Transfer> options = new ArrayList<>();
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        if (station == null) return options;

        IntArrayList ownColors = new IntArrayList();
        Platform platform = PlatformLocator.findPlatformAt(pos);
        if (platform != null) {
            for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
                if (route.getPlatformIndex(platform.getId()) >= 0) ownColors.add(route.getColor());
            }
        }

        station.getInterchangeStationToColorToRoutesMap(true).forEach((interchangeStation, colorToRoutes) ->
                colorToRoutes.forEach((Integer color, ObjectArrayList<Route> routes) -> {
                    if (!ownColors.contains((int) color) && !routes.isEmpty()) {
                        options.add(toTransfer(interchangeStation, color, routes.get(0)));
                    }
                }));
        return options;
    }

    // Выбранная пересадка по сохранённым в блоке станции и цвету
    @Nullable
    public static Transfer resolve(long stationId, int color) {
        if (stationId == 0) return null;
        Station station = MinecraftClientData.getInstance().stationIdMap.get(stationId);
        if (station == null) return null;
        Int2ObjectAVLTreeMap<ObjectArrayList<Route>> colorToRoutes = station.getInterchangeStationToColorToRoutesMap(false).get(station);
        ObjectArrayList<Route> routes = colorToRoutes == null ? null : colorToRoutes.get(color);
        return routes == null || routes.isEmpty() ? null : toTransfer(station, color, routes.get(0));
    }

    private static Transfer toTransfer(Station station, int color, Route route) {
        return new Transfer(station.getId(), station.getName(), color, route.getName(), route.getRouteNumber());
    }
}
