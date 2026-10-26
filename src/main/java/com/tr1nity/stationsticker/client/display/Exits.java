package com.tr1nity.stationsticker.client.display;

import net.minecraft.core.BlockPos;
import org.mtr.core.data.Station;
import org.mtr.core.data.StationExit;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nullable;
import java.util.List;

// Выходы станции MTR (дашборд → станция → выходы): у выхода только название (буква + номер, «A1») и список строк-направлений.
// Договорённость для табличек: первое направление - улица (крупно), остальные - объекты рядом с выходом.
public final class Exits {

    private Exits() {
    }

    public static List<StationExit> at(BlockPos pos) {
        Station station = Transfers.stationNear(pos);
        if (station == null) return List.of();
        // Окно станции MTR правит копию данных дашборда, основная копия клиента обновляется позже:
        // без этого табличка показывала старые направления выхода. Берём станцию из копии дашборда, если она там есть
        MinecraftClientData dashboard = MinecraftClientData.getDashboardInstance();
        Station edited = dashboard == null ? null : dashboard.stationIdMap.get(station.getId());
        return (edited != null ? edited : station).getExits();
    }

    @Nullable
    public static StationExit find(BlockPos pos, String name) {
        if (name.isEmpty()) return null;
        for (StationExit exit : at(pos)) {
            if (exit.getName().equals(name)) return exit;
        }
        return null;
    }
}
