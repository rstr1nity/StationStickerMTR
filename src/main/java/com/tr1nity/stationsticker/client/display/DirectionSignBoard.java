package com.tr1nity.stationsticker.client.display;

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

// Указатель направлений в стиле навигации Московского метро:
//   <- (6) Новоясеневская              Медведково (6) ->
//          Novoyasenevskaya             Medvedkovo
//   ------------------------------------------------
//                  (5) (2) (8)  - пересадки
// Слева и справа - ближайшие платформы станции с соответствующей стороны от зрителя.
public final class DirectionSignBoard {

    public static final float DESIGN_HEIGHT = 42;

    private static final float NAME_SCALE = 1.6f;
    private static final float SECONDARY_Y = 15;
    private static final float SECONDARY_SCALE = 0.9f;
    private static final float DIVIDER_Y = 27;
    private static final float TRANSFER_Y = 36;      // центр кружков пересадок
    private static final float LINE_RADIUS = 6;
    private static final float TRANSFER_RADIUS = 5;
    private static final float ARROW_WIDTH = 12;
    private static final float GAP = 3;

    private static final int TEXT_COLOR = 0xFF1A1A1A;
    private static final int SECONDARY_COLOR = 0xFF5A5A5A;
    private static final int DIVIDER_COLOR = 0xFFB8B8B8;
    private static final int NUMBER_COLOR = 0xFFFFFFFF;

    private record Direction2(String terminal, String number, int color) {
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

        IntArrayList ownColors = new IntArrayList();
        Direction2 leftDirection = left == null ? null : directionOf(left.getId());
        Direction2 rightDirection = right == null ? null : directionOf(right.getId());
        if (leftDirection != null) {
            drawSide(canvas, leftDirection, false);
            ownColors.add(leftDirection.color);
        }
        if (rightDirection != null) {
            drawSide(canvas, rightDirection, true);
            ownColors.add(rightDirection.color);
        }

        canvas.rect(0, DIVIDER_Y, canvas.width, 0.6f, DIVIDER_COLOR);
        drawTransfers(canvas, left != null ? left.getId() : right != null ? right.getId() : 0, ownColors);
    }

    // Направление с платформы: первый маршрут, который идёт дальше этой станции
    @Nullable
    private static Direction2 directionOf(long platformId) {
        for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            int index = route.getPlatformIndex(platformId);
            if (index < 0 || index >= route.getPlatforms().size() - 1) continue;
            String terminal = route.getPlatforms().get(route.getPlatforms().size() - 1).getStationName();
            Route fullRoute = MinecraftClientData.getInstance().routeIdMap.get(route.getId());
            String number = fullRoute == null ? "" : fullRoute.getRouteNumber();
            return new Direction2(terminal, number, route.getColor());
        }
        return null;
    }

    private static void drawSide(SlideCanvas canvas, Direction2 direction, boolean rightSide) {
        float half = canvas.width / 2 - GAP * 2;
        String name = StationNames.forCurrentLanguage(direction.terminal);
        String secondary = StationNames.secondary(direction.terminal);
        float circleY = NAME_SCALE * 7 / 2 + 1;

        if (!rightSide) {
            drawArrow(canvas, 0, circleY, false);
            float circleX = ARROW_WIDTH + GAP + LINE_RADIUS;
            drawLineCircle(canvas, circleX, circleY, LINE_RADIUS, direction);
            float textX = circleX + LINE_RADIUS + GAP;
            canvas.text(name, textX, 1, NAME_SCALE, TEXT_COLOR, half - textX);
            canvas.text(secondary, textX, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, half - textX);
        } else {
            drawArrow(canvas, canvas.width - ARROW_WIDTH, circleY, true);
            float circleX = canvas.width - ARROW_WIDTH - GAP - LINE_RADIUS;
            drawLineCircle(canvas, circleX, circleY, LINE_RADIUS, direction);
            float textRight = circleX - LINE_RADIUS - GAP;
            float maxWidth = textRight - (canvas.width - half);
            canvas.textRight(name, textRight, 1, NAME_SCALE, TEXT_COLOR, maxWidth);
            canvas.textRight(secondary, textRight, SECONDARY_Y, SECONDARY_SCALE, SECONDARY_COLOR, maxWidth);
        }
    }

    private static void drawLineCircle(SlideCanvas canvas, float x, float y, float radius, Direction2 direction) {
        drawNumberCircle(canvas, x, y, radius, direction.color, direction.number);
    }

    private static void drawNumberCircle(SlideCanvas canvas, float x, float y, float radius, int color, String number) {
        canvas.circle(x, y, radius, 0xFF000000 | color);
        if (!number.isEmpty()) {
            float scale = Math.min(radius / 6, radius * 1.4f / canvas.boldTextWidth(number, 1));
            canvas.boldText(number, x - canvas.boldTextWidth(number, scale) / 2, y - 3.5f * scale, scale, NUMBER_COLOR);
        }
    }

    // Стрелка «пикселями»: древко и треугольный наконечник из столбиков
    private static void drawArrow(SlideCanvas canvas, float x, float centerY, boolean pointRight) {
        float thickness = 1.6f;
        float head = 5;
        canvas.rect(x + (pointRight ? 0 : head - 1), centerY - thickness / 2, ARROW_WIDTH - head + 1, thickness, TEXT_COLOR);
        for (int i = 0; i < head; i++) {
            float columnX = pointRight ? x + ARROW_WIDTH - 1 - i : x + i;
            float halfHeight = i + 0.8f;
            canvas.rect(columnX, centerY - halfHeight, 1, halfHeight * 2, TEXT_COLOR);
        }
    }

    // Линии, на которые можно пересесть на этой станции (кроме своих)
    private static void drawTransfers(SlideCanvas canvas, long platformId, IntArrayList ownColors) {
        if (platformId == 0) return;
        IntArrayList colors = new IntArrayList();
        for (SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            int index = route.getPlatformIndex(platformId);
            if (index < 0) continue;
            SimplifiedRoutePlatform stop = route.getPlatforms().get(index);
            stop.forEach((color, routeNames) -> {
                if (!ownColors.contains(color) && !colors.contains(color)) {
                    colors.add(color);
                }
            });
        }

        float step = TRANSFER_RADIUS * 2 + 2;
        float x = canvas.width / 2 - (colors.size() - 1) * step / 2;
        for (int i = 0; i < colors.size(); i++) {
            int color = colors.getInt(i);
            drawNumberCircle(canvas, x + i * step, TRANSFER_Y, TRANSFER_RADIUS, color, numberForColor(color));
        }
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
