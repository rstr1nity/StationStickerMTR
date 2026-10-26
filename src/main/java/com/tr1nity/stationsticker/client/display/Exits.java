package com.tr1nity.stationsticker.client.display;

import net.minecraft.core.BlockPos;
import org.mtr.core.data.Station;
import org.mtr.core.data.StationExit;

import javax.annotation.Nullable;
import java.util.List;

// Выходы станции MTR (дашборд → станция → выходы): у выхода только название («1–4») и список строк-направлений.
// Договорённость для табличек: первое направление - улица (крупно), остальные - объекты рядом с выходом.
public final class Exits {

    private Exits() {
    }

    public static List<StationExit> at(BlockPos pos) {
        Station station = Transfers.stationNear(pos);
        return station == null ? List.of() : station.getExits();
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
