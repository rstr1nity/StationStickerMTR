package com.tr1nity.stationsticker.client.display;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.mtr.core.data.Platform;
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

    private static final float ARROW_SCALE = 1.0f;     // стрелка 13x10 клеток - по высоте заголовка
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
    private static final String KEY = "display.stationsticker.arrival.";

    // Направление с платформы: маршрут и индекс остановки; последний индекс означает конечную.
    private record Side(SimplifiedRoute route, int index, long platformId) {
    }

    private DirectionSignBoard() {
    }

    // mainPos - основной блок, partAxis - в какую сторону от него стоят части, width - ширина в клетках,
    // rightAxis - «вправо» для зрителя этой стороны экрана (у обратной стороны подвесного экрана - против partAxis)
    public static void render(SlideCanvas canvas, BlockPos mainPos, Direction partAxis, int width, Direction rightAxis, boolean ceiling) {
        Station station = com.tr1nity.stationsticker.client.PlatformLocator.findStationAt(mainPos);
        if (station == null) return;

        // Центр экрана в мировых координатах: середина отрезка клеток
        double half = (width - 1) / 2.0;
        double centerX = mainPos.getX() + 0.5 + partAxis.getStepX() * half;
        double centerZ = mainPos.getZ() + 0.5 + partAxis.getStepZ() * half;

        // Выбираем линию по ближайшей платформе, учитывая высоту этажа.
        Integer lineColor = null;
        double nearestDistance = Double.MAX_VALUE;
        boolean selectedBelow = false;
        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if ((platform.area == null || platform.area.getId() != station.getId())) continue;
            Side candidate = sideOf(platform.getId(), null);
            if (candidate == null) continue;
            double dx = platform.getMidPosition().getX() + 0.5 - centerX;
            double dy = platform.getMidPosition().getY() + 0.5 - (mainPos.getY() + 0.5);
            double dz = platform.getMidPosition().getZ() + 0.5 - centerZ;
            double distance = dx * dx + dy * dy + dz * dz;
            boolean below = ceiling && platform.getMidPosition().getY() < mainPos.getY();
            if ((below && !selectedBelow) || (below == selectedBelow && distance < nearestDistance)) {
                selectedBelow = below;
                nearestDistance = distance;
                lineColor = candidate.route.getColor();
            }
        }
        if (lineColor == null) return;

        Side leftSide = null;
        Side rightSide = null;
        double leftDistance = Double.MAX_VALUE;
        double rightDistance = Double.MAX_VALUE;
        for (Platform platform : MinecraftClientData.getInstance().platformIdMap.values()) {
            if ((platform.area == null || platform.area.getId() != station.getId())) continue;
            if (selectedBelow && platform.getMidPosition().getY() >= mainPos.getY()) continue;
            Side candidate = sideOf(platform.getId(), lineColor);
            if (candidate == null) continue;
            double dx = platform.getMidPosition().getX() + 0.5 - centerX;
            double dy = platform.getMidPosition().getY() + 0.5 - (mainPos.getY() + 0.5);
            double dz = platform.getMidPosition().getZ() + 0.5 - centerZ;
            double side = dx * rightAxis.getStepX() + dz * rightAxis.getStepZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (side < 0 && (leftSide == null || better(candidate, distance, leftSide, leftDistance))) {
                leftSide = candidate;
                leftDistance = distance;
            } else if (side >= 0 && (rightSide == null || better(candidate, distance, rightSide, rightDistance))) {
                rightSide = candidate;
                rightDistance = distance;
            }
        }

        float halfWidth = canvas.width / 2;
        canvas.rect(halfWidth - 0.3f, 1, 0.6f, DESIGN_HEIGHT - 2, DIVIDER_COLOR);
        if (leftSide != null) drawPlatform(canvas, leftSide, 0, halfWidth - GAP * 2, false);
        if (rightSide != null) drawPlatform(canvas, rightSide, halfWidth + GAP * 2, canvas.width, true);
    }

    private static boolean isBoarding(Side side) {
        return com.tr1nity.stationsticker.client.NavigationRoutes.boarding(side.route,side.index);
    }

    private static boolean better(Side candidate, double distance, Side current, double currentDistance) {
        return isBoarding(candidate) != isBoarding(current) ? isBoarding(candidate) : distance < currentDistance;
    }

    @Nullable
    private static Side sideOf(long platformId, @Nullable Integer color) {
        Side terminating = null;
        for (SimplifiedRoute route : com.tr1nity.stationsticker.client.NavigationRoutes.atPlatform(platformId)) {
            if (color != null && route.getColor() != color) continue;
            int index = route.getPlatformIndex(platformId);
            if (index < 0) continue;
            Side side = new Side(route, index, platformId);
            if (isBoarding(side)) return side;
            terminating = side;
        }
        return terminating;
    }
    private static void drawPlatform(SlideCanvas canvas,Side chosen,float x0,float x1,boolean right){
        var groups=new java.util.LinkedHashMap<String,java.util.List<Side>>();
        com.tr1nity.stationsticker.client.NavigationRoutes.destinations(new java.util.ArrayList<>(MinecraftClientData.getInstance().simplifiedRoutes),chosen.platformId)
                .forEach((key,routes)->{var variants=new java.util.ArrayList<Side>();routes.forEach(r->variants.add(new Side(r,r.getPlatformIndex(chosen.platformId),chosen.platformId)));groups.put(key,variants);});
        if(groups.isEmpty()){drawNoBoarding(canvas,x0,x1,right);return;}
        float column=(x1-x0)/groups.size();int i=0;
        for(var variants:groups.values()){
            float left=x0+i*column;
            if(i>0)canvas.rect(left-1,1,.3f,DESIGN_HEIGHT-2,DIVIDER_COLOR);
            drawSide(canvas,variants,left,left+column-GAP,right);i++;
        }
    }
    // Половина экрана от x0 до x1; rightSide - стрелка и заголовок у правого края
    private static void drawSide(SlideCanvas canvas, java.util.List<Side> variants, float x0, float x1, boolean rightSide) {
        Side side=variants.get(0);
        if (!isBoarding(side)) {
            drawNoBoarding(canvas, x0, x1, rightSide);
            return;
        }

        List<SimplifiedRoutePlatform> stops = side.route.getPlatforms();
        String terminal = stops.get(stops.size() - 1).getStationName();
        String circularTitle = CircularDirections.title(side.route.getCircularState());
        if (!circularTitle.isEmpty()) terminal = circularTitle;
        String number = RouteNumbers.forRoute(side.route);
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
            canvas.textRight(StationNames.primary(terminal), textEdge, 0, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.textRight(StationNames.secondary(terminal), textEdge, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        } else {
            canvas.text(StationNames.primary(terminal), textEdge, 0, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.text(StationNames.secondary(terminal), textEdge, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        }

        // Следующие станции по столбцам сверху вниз, слева направо; конечная уже в заголовке
        int columns = Math.max(1, (int) ((x1 - x0) / COLUMN_WIDTH));
        float columnWidth = (x1 - x0) / columns;
        int capacity = columns * LIST_ROWS;
        int shown = 0;
        var next=new java.util.LinkedHashMap<Long,SimplifiedRoutePlatform>();
        for(var variant:variants)for(var stop:CircularDirections.nextStops(variant.route.getPlatforms(),variant.index,!circularTitle.isEmpty(),capacity))next.putIfAbsent(stop.getStationId(),stop);
        if(variants.size()>1){columns=Math.max(columns,Math.min(4,(next.size()+LIST_ROWS-1)/LIST_ROWS));columnWidth=(x1-x0)/columns;capacity=columns*LIST_ROWS;}
        for (var stop : next.values()) {
            if(shown>=capacity)break;
            float itemX = x0 + (shown / LIST_ROWS) * columnWidth;
            float itemY = LIST_Y + (shown % LIST_ROWS) * LIST_ROW;
            drawStop(canvas, stop, side.route.getColor(), itemX, itemY, columnWidth - GAP);
            shown++;
        }
    }

    private static void drawStop(SlideCanvas canvas, SimplifiedRoutePlatform stop, int lineColor, float x, float y, float width) {
        IntArrayList colors = new IntArrayList();
        stop.forEach((color, routeNames) -> {
            if (color != lineColor && !colors.contains((int) color)) colors.add(color);
        });
        float dotsWidth = colors.size() * (DOT_RADIUS * 2 + 0.8f);
        String name = StationNames.primary(stop.getStationName());
        float nameWidth = Math.min(canvas.textWidth(name, LIST_NAME_SCALE), width - dotsWidth - 1);
        canvas.text(name, x, y, LIST_NAME_SCALE, TEXT_COLOR, width - dotsWidth - 1);
        canvas.text(StationNames.secondary(stop.getStationName()), x, y + 6.5f, LIST_SECONDARY_SCALE, SECONDARY_COLOR, width);

        float dotX = x + nameWidth + 1 + DOT_RADIUS;
        for (int i = 0; i < colors.size(); i++) {
            int color = colors.getInt(i);
            numberCircle(canvas, dotX, y + 2.6f, DOT_RADIUS, color, RouteNumbers.forColor(color));
            dotX += DOT_RADIUS * 2 + 0.8f;
        }
    }

    private static void drawNoBoarding(SlideCanvas canvas, float x0, float x1, boolean rightSide) {
        float radius = 9;
        float centerY = DESIGN_HEIGHT / 2;
        float iconX = rightSide ? x1 - radius : x0 + radius;
        Pictograms.noBoarding(canvas, iconX, centerY, radius, NO_BOARDING_COLOR);
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
}
