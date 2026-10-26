package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.entity.InfoDisplayBlockEntity;
import com.tr1nity.stationsticker.client.display.DisplaySlide;
import com.tr1nity.stationsticker.client.display.RouteNumbers;
import com.tr1nity.stationsticker.client.display.SlideCanvas;
import com.tr1nity.stationsticker.client.display.SlideShow;
import com.tr1nity.stationsticker.client.display.StationNames;
import com.tr1nity.stationsticker.client.display.StationSlide;
import com.tr1nity.stationsticker.client.display.Transfers;
import com.tr1nity.stationsticker.client.display.WeatherSlide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.List;

// Информационный экран: слайды погоды и станции со сменой и переходами (SlideShow)
public class InfoDisplayRenderer extends WideDisplayRenderer<InfoDisplayBlockEntity> {

    public InfoDisplayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected int screenColor() {
        return 0xFF0A0F1A;
    }

    @Override
    protected float designHeight(InfoDisplayBlockEntity entity) {
        return WeatherSlide.DESIGN_HEIGHT;
    }

    @Override
    protected void drawContent(InfoDisplayBlockEntity entity, SlideCanvas canvas, boolean back) {
        Level level = entity.getLevel();
        if (level == null) return;
        BlockPos pos = entity.getBlockPos();
        float partialTick = Minecraft.getInstance().getFrameTime();
        double time = level.getGameTime() + partialTick;
        Station station = Transfers.stationNear(pos);
        String stationName = station == null ? "" : StationNames.primary(station.getName());

        List<DisplaySlide> slides = new ArrayList<>();
        slides.add(new WeatherSlide(new WeatherSlide.State(condition(level, pos), level.getRainLevel(partialTick),
                time, level.getDayTime(), stationName)));
        if (station != null) slides.add(stationSlide(station));
        SlideShow.render(slides, time, canvas);
    }

    private static WeatherSlide.Condition condition(Level level, BlockPos pos) {
        if (!level.isRaining()) return WeatherSlide.Condition.CLEAR;
        if (level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW) return WeatherSlide.Condition.SNOW;
        return level.isThundering() ? WeatherSlide.Condition.THUNDER : WeatherSlide.Condition.RAIN;
    }

    // Линии, которые останавливаются на этой станции (по одной на цвет)
    private static StationSlide stationSlide(Station station) {
        IntArrayList colors = new IntArrayList();
        List<String> numbers = new ArrayList<>();
        for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            if (colors.contains(route.getColor())) continue;
            for (SimplifiedRoutePlatform stop : route.getPlatforms()) {
                if (stop.getStationId() == station.getId()) {
                    colors.add(route.getColor());
                    numbers.add(RouteNumbers.forRoute(route));
                    break;
                }
            }
        }
        return new StationSlide(station.getName(), colors, numbers);
    }
}
