package com.tr1nity.stationsticker.client.display;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;

import javax.annotation.Nullable;
import java.util.List;

// Указатель направлений в стиле навигации Московского метро:
//   <- (7) Котельники                   |                  Планерная (7) ->
//          Kotelniki                    |                  Planernaya
//   Кузнецкий Мост (1)  Китай-город (6) | Баррикадная (5)  Улица 1905 года
//   Таганская (5)(8)    Пролетарская    | Беговая          Полежаевская
// Слева и справа - ближайшие платформы станции с соответствующей стороны от зрителя: конечная крупно,
// под ней следующие станции маршрута с пересадками. Если поезда здесь только заканчивают маршрут -
// «Посадки нет».
public final class DirectionSignBoard {

    public static final float DESIGN_HEIGHT = 44;

    private static final float ARROW_SCALE = 1.6f;
    private static final float LINE_RADIUS = 6;
    private static final float NAME_SCALE = 1.4f;
    private static final float SECONDARY_Y = 11;
    private static final float SECONDARY_SCALE = 0.65f;
    private static final float LIST_Y = 20;
    private static final float LIST_ROW = 12;
    private static final int LIST_ROWS = 2;
    private static final float LIST_NAME_SCALE = 0.75f;
    private static final float LIST_SECONDARY_SCALE = 0.5f;
    private static final float COLUMN_WIDTH = 62;
    private static final float DOT_RADIUS = 2.2f;
    private static final float GAP = 3;

    private static final int TEXT_COLOR = 0xFF1A1A1A;
    private static final int SECONDARY_COLOR = 0xFF5A5A5A;
    private static final int DIVIDER_COLOR = 0xFFB8B8B8;
    private static final int NUMBER_COLOR = 0xFFFFFFFF;
    private static final int NO_BOARDING_COLOR = 0xFFE0322A;
    private static final int SCREEN_COLOR = 0xFFF4F4F2;
    private static final String KEY = "display.stationsticker.arrival.";

    // Направление с платформы: маршрут и индекс этой остановки в нём; route == null - поезда здесь только заканчивают маршрут
    private record Side(@Nullable SimplifiedRoute route, int index) {
    }

    private DirectionSignBoard() {
    }

    // mainPos - основной блок, partAxis - в какую сторону от него стоят части, width - ширина в клетках,
    // rightAxis - «вправо» для зрителя этой стороны экрана (у обратной стороны подвесного экрана - против partAxis)
    public static void render(SlideCanvas canvas, BlockPos mainPos, Direction partAxis, int width, Direction rightAxis) {
        Station station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(mainPos.getX(), mainPos.getY(), mainPos.getZ()));
        if (station == null) return;

        // Центр экрана в мировых координатах: середина отрезка клеток
        double half = (width - 1) / 2.0;
        double centerX = mainPos.getX() + 0.5 + partAxis.getStepX() * half;
        double centerZ = mainPos.getZ() + 0.5 + partAxis.getStepZ() * half;

        Platform left = null;
        Platform right = null;
        double leftDistance = Double.MAX_VALUE;
        double rightDistance = Double.MAX_VALUE;
        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if (platform.area != station) continue;
            double dx = platform.getMidPosition().getX() + 0.5 - centerX;
            double dz = platform.getMidPosition().getZ() + 0.5 - centerZ;
            double side = dx * rightAxis.getStepX() + dz * rightAxis.getStepZ();
            double distance = dx * dx + dz * dz;
            if (side < 0 && distance < leftDistance) {
                left = platform;
                leftDistance = distance;
            } else if (side >= 0 && distance < rightDistance) {
                right = platform;
                rightDistance = distance;
            }
        }

        float halfWidth = canvas.width / 2;
        canvas.rect(halfWidth - 0.3f, 1, 0.6f, DESIGN_HEIGHT - 2, DIVIDER_COLOR);
        Side leftSide = left == null ? null : sideOf(left.getId());
        Side rightSide = right == null ? null : sideOf(right.getId());
        if (leftSide != null) drawSide(canvas, leftSide, 0, halfWidth - GAP * 2, false);
        if (rightSide != null) drawSide(canvas, rightSide, halfWidth + GAP * 2, canvas.width, true);
    }

    @Nullable
    private static Side sideOf(long platformId) {
        boolean terminating = false;
        for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            int index = route.getPlatformIndex(platformId);
            if (index < 0) continue;
            if (index < route.getPlatforms().size() - 1) return new Side(route, index);
            terminating = true;
        }
        return terminating ? new Side(null, -1) : null;
    }

    // Половина экрана от x0 до x1; rightSide - стрелка и заголовок у правого края
    private static void drawSide(SlideCanvas canvas, Side side, float x0, float x1, boolean rightSide) {
        if (side.route == null) {
            drawNoBoarding(canvas, x0, x1, rightSide);
            return;
        }

        List<SimplifiedRoutePlatform> stops = side.route.getPlatforms();
        String terminal = stops.get(stops.size() - 1).getStationName();
        String number = routeNumber(side.route);
        float centerY = NAME_SCALE * 7 / 2 + 1;

        String[] arrow = rightSide ? Pictograms.mirror(Pictograms.ARROW_LEFT) : Pictograms.ARROW_LEFT;
        float arrowWidth = arrow[0].length() * ARROW_SCALE;
        float arrowX = rightSide ? x1 - arrowWidth : x0;
        Pictograms.draw(canvas, arrow, arrowX, centerY - arrow.length * ARROW_SCALE / 2, ARROW_SCALE, TEXT_COLOR, false);

        float circleX = rightSide ? arrowX - GAP - LINE_RADIUS : arrowX + arrowWidth + GAP + LINE_RADIUS;
        numberCircle(canvas, circleX, centerY, LINE_RADIUS, side.route.getColor(), number);

        float textEdge = rightSide ? circleX - LINE_RADIUS - GAP : circleX + LINE_RADIUS + GAP;
        float maxWidth = rightSide ? textEdge - x0 : x1 - textEdge;
        if (rightSide) {
            canvas.textRight(StationNames.forCurrentLanguage(terminal), textEdge, 0, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.textRight(StationNames.secondary(terminal), textEdge, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        } else {
            canvas.text(StationNames.forCurrentLanguage(terminal), textEdge, 0, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.text(StationNames.secondary(terminal), textEdge, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        }

        // Следующие станции по столбцам сверху вниз, слева направо; конечная уже в заголовке
        int columns = Math.max(1, (int) ((x1 - x0) / COLUMN_WIDTH));
        float columnWidth = (x1 - x0) / columns;
        int capacity = columns * LIST_ROWS;
        int shown = 0;
        for (int i = side.index + 1; i < stops.size() - 1 && shown < capacity; i++, shown++) {
            float itemX = x0 + (shown / LIST_ROWS) * columnWidth;
            float itemY = LIST_Y + (shown % LIST_ROWS) * LIST_ROW;
            drawStop(canvas, stops.get(i), side.route.getColor(), itemX, itemY, columnWidth - GAP);
        }
    }

    private static void drawStop(SlideCanvas canvas, SimplifiedRoutePlatform stop, int lineColor, float x, float y, float width) {
        IntArrayList colors = new IntArrayList();
        stop.forEach((color, routeNames) -> {
            if (color != lineColor && !colors.contains((int) color)) colors.add(color);
        });
        float dotsWidth = colors.size() * (DOT_RADIUS * 2 + 0.8f);
        String name = StationNames.forCurrentLanguage(stop.getStationName());
        float nameWidth = Math.min(canvas.textWidth(name, LIST_NAME_SCALE), width - dotsWidth - 1);
        canvas.text(name, x, y, LIST_NAME_SCALE, TEXT_COLOR, width - dotsWidth - 1);
        canvas.text(StationNames.secondary(stop.getStationName()), x, y + 6.5f, LIST_SECONDARY_SCALE, SECONDARY_COLOR, width);

        float dotX = x + nameWidth + 1 + DOT_RADIUS;
        for (int i = 0; i < colors.size(); i++) {
            int color = colors.getInt(i);
            numberCircle(canvas, dotX, y + 2.6f, DOT_RADIUS, color, numberForColor(color));
            dotX += DOT_RADIUS * 2 + 0.8f;
        }
    }

    private static void drawNoBoarding(SlideCanvas canvas, float x0, float x1, boolean rightSide) {
        float radius = 9;
        float centerY = DESIGN_HEIGHT / 2;
        float iconX = rightSide ? x1 - radius : x0 + radius;
        Pictograms.noBoarding(canvas, iconX, centerY, radius, NO_BOARDING_COLOR, SCREEN_COLOR);
        String text = I18n.get(KEY + "no_boarding");
        String secondary = I18n.get(KEY + "no_boarding_secondary");
        if (rightSide) {
            float edge = iconX - radius - GAP;
            canvas.textRight(text, edge, centerY - 9, 1.3f, NO_BOARDING_COLOR, edge - x0);
            canvas.textRight(secondary, edge, centerY + 2, 0.9f, NO_BOARDING_COLOR, edge - x0);
        } else {
            float edge = iconX + radius + GAP;
            canvas.text(text, edge, centerY - 9, 1.3f, NO_BOARDING_COLOR, x1 - edge);
            canvas.text(secondary, edge, centerY + 2, 0.9f, NO_BOARDING_COLOR, x1 - edge);
        }
    }

    private static void numberCircle(SlideCanvas canvas, float x, float y, float radius, int color, String number) {
        canvas.circle(x, y, radius, 0xFF000000 | color);
        if (!number.isEmpty()) {
            float scale = Math.min(radius / 6, radius * 1.4f / canvas.boldTextWidth(number, 1));
            canvas.boldText(number, x - canvas.boldTextWidth(number, scale) / 2, y - 3.5f * scale, scale, NUMBER_COLOR);
        }
    }

    private static String routeNumber(SimplifiedRoute route) {
        Route full = MinecraftClientData.getInstance().routeIdMap.get(route.getId());
        return full == null ? "" : full.getRouteNumber();
    }

    // Номер линии по цвету: у пересадки известны только цвет и названия маршрутов
    private static String numberForColor(int color) {
        for (Route route : MinecraftClientData.getInstance().routeIdMap.values()) {
            if (route.getColor() == color && !route.getRouteNumber().isEmpty()) {
                return route.getRouteNumber();
            }
        }
        return "";
    }
}
