package com.yourname.stationsticker.client;

import org.mtr.core.data.*;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.DynamicTextureCache;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.config.Config;
import org.mtr.mod.data.IGui;
import org.mtr.mod.generated.lang.TranslationProvider;

import java.util.Locale;
import java.util.function.BiConsumer;

public class SPBRouteMapGenerator implements IGui {

    private static final float TEXT_SPACING_FROM_CIRCLE = 1.05f; // Объяви этот константой выше в классе
    private static int scale;
    private static int lineSize;
    private static int lineSpacing;
    private static int fontSizeBig;
    private static int fontSizeSmall;

    public static final int PIXEL_SCALE = 4;
    private static final int MIN_VERTICAL_SIZE = 5;
    private static final String LOGO_RESOURCE = "textures/block/sign/logo.png";
    private static final String EXIT_RESOURCE = "textures/block/sign/exit_letter_blank.png";
    private static final String ARROW_RESOURCE = "textures/block/sign/arrow.png";
    private static final String CIRCLE_RESOURCE = "textures/block/sign/circle.png";
    private static final String TEMP_CIRCULAR_MARKER_CLOCKWISE = String.format("temp_circular_marker_%s_clockwise", Init.randomString());
    private static final String TEMP_CIRCULAR_MARKER_ANTICLOCKWISE = String.format("temp_circular_marker_%s_anticlockwise", Init.randomString());
    private static final int PIXEL_RESOLUTION = 24;

    public static void setConstants() {
        scale = (int) Math.pow(2, Config.getClient().getDynamicTextureResolution() + 5);
        lineSize = scale / 8;
        lineSpacing = lineSize * 3 / 2;
        fontSizeBig = lineSize * 2;
        fontSizeSmall = fontSizeBig / 2;
    }

    public static NativeImage generateRouteMap(long platformId, boolean vertical, boolean flip, float aspectRatio, boolean transparentWhite) {
        if (aspectRatio <= 0) {
            return null;
        }

        try {
            final ObjectArrayList<ObjectIntImmutablePair<SimplifiedRoute>> routeDetails = new ObjectArrayList<>();
            getRouteStream(platformId, (simplifiedRoute, currentStationIndex) -> routeDetails.add(new ObjectIntImmutablePair<>(simplifiedRoute, currentStationIndex)));
            final int routeCount = routeDetails.size();

            if (routeCount > 0) {
                final DynamicTextureCache clientCache = DynamicTextureCache.instance;
                final ObjectArrayList<LongArrayList> stationsIdsBefore = new ObjectArrayList<>();
                final ObjectArrayList<LongArrayList> stationsIdsAfter = new ObjectArrayList<>();
                final ObjectArrayList<Int2ObjectAVLTreeMap<StationPosition>> stationPositions = new ObjectArrayList<>();
                final IntAVLTreeSet colors = new IntAVLTreeSet();
                final int[] colorIndices = new int[routeCount];
                int colorIndex = -1;
                int previousColor = -1;
                for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                    stationsIdsBefore.add(new LongArrayList());
                    stationsIdsAfter.add(new LongArrayList());
                    stationPositions.add(new Int2ObjectAVLTreeMap<>());

                    final ObjectIntImmutablePair<SimplifiedRoute> routeDetail = routeDetails.get(routeIndex);
                    final ObjectArrayList<SimplifiedRoutePlatform> simplifiedRoutePlatforms = routeDetail.left().getPlatforms();
                    final int currentIndex = routeDetail.rightInt();
                    for (int stationIndex = 0; stationIndex < simplifiedRoutePlatforms.size(); stationIndex++) {
                        if (stationIndex != currentIndex) {
                            final long stationId = simplifiedRoutePlatforms.get(stationIndex).getStationId();
                            if (stationIndex < currentIndex) {
                                stationsIdsBefore.get(stationsIdsBefore.size() - 1).add(0, stationId);
                            } else {
                                stationsIdsAfter.get(stationsIdsAfter.size() - 1).add(stationId);
                            }
                        }
                    }

                    final int color = routeDetail.left().getColor();
                    colors.add(color);
                    if (color != previousColor) {
                        colorIndex++;
                        previousColor = color;
                    }
                    colorIndices[routeIndex] = colorIndex;
                }

                for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                    stationPositions.get(routeIndex).put(0, new StationPosition(0, getLineOffset(routeIndex, colorIndices), true));
                }

                final float[] bounds = new float[3];
                setup(stationPositions, flip ? stationsIdsBefore : stationsIdsAfter, colorIndices, bounds, flip, true);
                final float xOffset = bounds[0] + 0.5F;
                setup(stationPositions, flip ? stationsIdsAfter : stationsIdsBefore, colorIndices, bounds, !flip, false);
                final float rawHeightPart = Math.abs(bounds[1]) + (vertical ? 0.6F : 1);
                final float rawWidth = xOffset + bounds[0] + 0.5F;
                final float rawHeightTotal = rawHeightPart + bounds[2] + (vertical ? 0.6F : 1);
                final float rawHeight;
                final float yOffset;
                final float extraPadding;
                if (vertical && rawHeightTotal < MIN_VERTICAL_SIZE) {
                    rawHeight = MIN_VERTICAL_SIZE;
                    extraPadding = (MIN_VERTICAL_SIZE - rawHeightTotal) / 2;
                    yOffset = rawHeightPart + extraPadding;
                } else {
                    rawHeight = rawHeightTotal;
                    extraPadding = 0;
                    yOffset = rawHeightPart;
                }

                final int height;
                final int width;
                final float widthScale;
                final float heightScale;
                if (rawWidth / rawHeight > aspectRatio) {
                    width = Math.round(rawWidth * scale);
                    height = Math.round(width / aspectRatio);
                    widthScale = 1;
                    heightScale = height / rawHeight / scale;
                } else {
                    height = Math.round(rawHeight * scale);
                    width = Math.round(height * aspectRatio);
                    heightScale = 1;
                    widthScale = width / rawWidth / scale;
                }

                if (width <= 0 || height <= 0) {
                    return null;
                }

                final NativeImage nativeImage = new NativeImage(NativeImageFormat.getAbgrMapped(), width, height, false);
                nativeImage.fillRect(0, 0, width, height, ARGB_WHITE);

                final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<StationPositionGrouped>> stationPositionsGrouped = new Object2ObjectOpenHashMap<>();
                for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                    final SimplifiedRoute simplifiedRoute = routeDetails.get(routeIndex).left();
                    final int currentIndex = routeDetails.get(routeIndex).rightInt();
                    final Int2ObjectAVLTreeMap<StationPosition> routeStationPositions = stationPositions.get(routeIndex);

                    for (int stationIndex = 0; stationIndex < simplifiedRoute.getPlatforms().size(); stationIndex++) {
                        final StationPosition stationPosition = routeStationPositions.get(stationIndex - currentIndex);
                        if (stationIndex < simplifiedRoute.getPlatforms().size() - 1) {
                            drawLine(nativeImage, stationPosition, routeStationPositions.get(stationIndex + 1 - currentIndex), widthScale, heightScale, xOffset, yOffset, stationIndex < currentIndex ? ARGB_LIGHT_GRAY : ARGB_BLACK | simplifiedRoute.getColor());
                        }

                        final SimplifiedRoutePlatform simplifiedRoutePlatform = simplifiedRoute.getPlatforms().get(stationIndex);
                        final String key = String.format("%s||%s", simplifiedRoutePlatform.getStationName(), simplifiedRoutePlatform.getStationId());

                        if (!stationPosition.isCommon || stationPositionsGrouped.getOrDefault(key, new ObjectOpenHashSet<>()).stream().noneMatch(stationPosition2 -> stationPosition2.stationPosition.x == stationPosition.x)) {
                            final IntArrayList interchangeColors = new IntArrayList();
                            final ObjectArrayList<String> interchangeNames = new ObjectArrayList<>();
                            simplifiedRoutePlatform.forEach((color, interchangeRouteNamesForColor) -> {
                                if (!colors.contains(color)) {
                                    interchangeColors.add(color);
                                    interchangeRouteNamesForColor.forEach(interchangeNames::add);
                                }
                            });
                            Data.put(stationPositionsGrouped, key, new StationPositionGrouped(stationPosition, stationIndex - currentIndex, interchangeColors, interchangeNames), ObjectOpenHashSet::new);
                        }
                    }
                }

                final int maxStringWidth = (int) (scale * 0.9 * ((vertical ? heightScale : widthScale) / 2 + extraPadding / routeCount));
                stationPositionsGrouped.forEach((key, stationPositionGroupedSet) -> stationPositionGroupedSet.forEach(stationPositionGrouped -> {
                    final int x = Math.round((stationPositionGrouped.stationPosition.x + xOffset) * scale * widthScale);
                    final int y = Math.round((stationPositionGrouped.stationPosition.y + yOffset) * scale * heightScale);
                    final int lines = stationPositionGrouped.stationPosition.isCommon ? colorIndices[colorIndices.length - 1] : 0;
                    final boolean textBelow = vertical || (stationPositionGrouped.stationPosition.isCommon ? Math.round(stationPositionGrouped.stationPosition.x * 2) % 2 == 0 : y >= yOffset * scale);
                    final boolean currentStation = stationPositionGrouped.stationOffset == 0;
                    final boolean passed = stationPositionGrouped.stationOffset < 0;

                    final IntArrayList interchangeColors = stationPositionGrouped.interchangeColors;


                    // Получаем цвет линии для этой станции
                    int stationLineColor;
                    if (passed) {
                        stationLineColor = ARGB_LIGHT_GRAY;
                    } else {
                        // Берём цвет из маршрута (первый маршрут для упрощения)
                        stationLineColor = routeDetails.get(0).left().getColor();
                    }
                    // Добавляем currentStation седьмым аргументом
                    drawStation(nativeImage, x, y, heightScale, lines, passed, currentStation, stationLineColor, interchangeColors);






                    // --- Внутри цикла stationPositionsGrouped.forEach ---

                    String stationName = key.split("\\|\\|")[0];
                    boolean isEnglishOnly = stationName.matches("[A-Za-z0-9\\s\\-]+");

                    int nameX = x; // Привязка строго по центру кружка
                    int textColor = passed ? ARGB_LIGHT_GRAY : (currentStation ? darkenColor(stationLineColor, 0.75f) : ARGB_BLACK);
                    float angle = -55f;

// Учет высоты пересадочного узла (если кружков несколько)
                    int totalLinesHeight = (int) Math.round(lines * lineSpacing * heightScale);
                    int verticalOffset = (int) (lineSize * TEXT_SPACING_FROM_CIRCLE);

                    if (isEnglishOnly) {
                        // --- ТОЛЬКО АНГЛИЙСКОЕ (Сверху, начинается от верхнего края) ---
                        int[] engDims = new int[2];
                        byte[] engPix = clientCache.getTextPixels(stationName, engDims, scale * 5, fontSizeBig, fontSizeBig, fontSizeSmall, 0, HorizontalAlignment.CENTER);

                        // anchorX = x (центр), anchorY = y - verticalOffset (самый верх кружка)
                        // Pivot: 0.0f (начало слова), 1.0f (низ слова)
                        drawRotatedString(nativeImage, engPix, engDims, x+22, y - verticalOffset, angle, textColor, 0.0f, 1.0f);

                    } else {
                        // --- РУССКОЕ (Сверху) + ТРАНСЛИТ (Снизу) ---
                        String englishName = transliterate(stationName);

                        // 1. Русское название (Верхнее)
                        int[] rusDims = new int[2];
                        byte[] rusPix = clientCache.getTextPixels(stationName, rusDims, scale * 5, fontSizeBig, fontSizeBig, fontSizeSmall, 0, HorizontalAlignment.CENTER);

                        // Начинается от верхней точки кружка
                        drawRotatedString(nativeImage, rusPix, rusDims, x+22, y - verticalOffset, angle, textColor, 0.0f, 1.0f);

                        // 2. Английское название (Нижнее)
                        if (!englishName.isEmpty()) {
                            int[] transDims = new int[2];
                            byte[] transPix = clientCache.getTextPixels(englishName, transDims, scale * 5, fontSizeSmall * 2, fontSizeSmall * 2, fontSizeSmall, 0, HorizontalAlignment.CENTER);

                            // Заканчивается у нижней точки кружка (с учетом высоты пересадки)
                            // anchorX = x (центр), anchorY = y + высота + verticalOffset (самый низ кружка)
                            // Pivot: 1.0f (КОНЕЦ слова), 0.0f (ВЕРХ слова)
                            drawRotatedString(nativeImage, transPix, transDims, x-20, y + totalLinesHeight + verticalOffset, angle, textColor, 1.0f, 0.0f);
                        }
                    }



                }));

                if (transparentWhite) {
                    clearColor(nativeImage, ARGB_WHITE);
                }

                int arrowColor = ARGB_BLACK;
                if (!routeDetails.isEmpty()) {

                    arrowColor = 0xFF000000 | routeDetails.get(0).left().getColor();
                }

                int imgWidth = nativeImage.getWidth();
                int topPadding = 30; // Отступ сверху
                int sidePadding = 40; // Отступ от краев


                drawArrow(nativeImage, sidePadding, topPadding, arrowColor, false);


                drawArrow(nativeImage, imgWidth - 2*sidePadding, topPadding, arrowColor, false);

                return nativeImage;
            } else {
                final NativeImage nativeImage = new NativeImage(NativeImageFormat.getAbgrMapped(), 1, 1, false);
                nativeImage.setPixelColor(0, 0, transparentWhite ? 0 : ARGB_WHITE);
            }
        } catch (Exception e) {
            Init.LOGGER.error("", e);
        }

        return null;
    }



    private static void setup(ObjectArrayList<Int2ObjectAVLTreeMap<StationPosition>> stationPositions, ObjectArrayList<LongArrayList> stationsIdLists, int[] colorIndices, float[] bounds, boolean passed, boolean reverse) {
        final int passedMultiplier = passed ? -1 : 1;
        final int reverseMultiplier = reverse ? -1 : 1;
        bounds[0] = 0;

        final LongArrayList commonStationIds = new LongArrayList();
        stationsIdLists.get(0).forEach(stationId -> {
            if (stationId != 0 && !commonStationIds.contains(stationId) && stationsIdLists.stream().allMatch(stationsIds -> stationsIds.contains(stationId))) {
                commonStationIds.add(stationId);
            }
        });

        int positionXOffset = 0;
        final int routeCount = stationsIdLists.size();
        final int[] traverseIndex = new int[routeCount];
        for (int commonStationIndex = 0; commonStationIndex <= commonStationIds.size(); commonStationIndex++) {
            final boolean lastStation = commonStationIndex == commonStationIds.size();
            final long commonStationId = lastStation ? -1 : commonStationIds.getLong(commonStationIndex);

            int intermediateSegmentsMaxCount = 0;
            final int[] intermediateSegmentsCounts = new int[routeCount];
            for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                intermediateSegmentsCounts[routeIndex] = (lastStation ? stationsIdLists.get(routeIndex).size() : stationsIdLists.get(routeIndex).indexOf(commonStationId) + 1) - traverseIndex[routeIndex];
                intermediateSegmentsMaxCount = Math.max(intermediateSegmentsMaxCount, intermediateSegmentsCounts[routeIndex]);
            }

            final IntArrayList routesIndicesInSection = new IntArrayList();
            for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                if (!lastStation || intermediateSegmentsCounts[routeIndex] > 0) {
                    routesIndicesInSection.add(routeIndex);
                }
            }

            for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                if (intermediateSegmentsCounts[routeIndex] > 0) {
                    final float increment = (float) intermediateSegmentsMaxCount / intermediateSegmentsCounts[routeIndex];
                    for (int j = 0; j < intermediateSegmentsCounts[routeIndex] - (lastStation ? 0 : 1); j++) {
                        final float stationX = positionXOffset + increment * (j + 1);
                        bounds[0] = Math.max(bounds[0], stationX / 2);
                        final float stationY = routesIndicesInSection.indexOf(routeIndex) - (routesIndicesInSection.size() - 1) / 2F + getLineOffset(routeIndex, colorIndices);
                        bounds[1] = Math.min(bounds[1], stationY);
                        bounds[2] = Math.max(bounds[2], stationY);
                        stationPositions.get(routeIndex).put(passedMultiplier * (j + traverseIndex[routeIndex] + 1), new StationPosition(reverseMultiplier * stationX / 2, stationY, false));
                    }
                    traverseIndex[routeIndex] += intermediateSegmentsCounts[routeIndex];
                }
            }

            if (!lastStation) {
                positionXOffset += intermediateSegmentsMaxCount;
                for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
                    final float stationY = getLineOffset(routeIndex, colorIndices);
                    bounds[1] = Math.min(bounds[1], stationY);
                    bounds[2] = Math.max(bounds[2], stationY);
                    stationPositions.get(routeIndex).put(passedMultiplier * traverseIndex[routeIndex], new StationPosition(reverseMultiplier * positionXOffset / 2F, stationY, true));
                }
                bounds[0] = positionXOffset / 2F;
            }
        }
    }

    private static float getLineOffset(int routeIndex, int[] colorIndices) {
        return (float) lineSpacing / scale * (colorIndices[routeIndex] - colorIndices[colorIndices.length - 1] / 2F);
    }

    private static IntArrayList getRouteStream(long platformId, BiConsumer<SimplifiedRoute, Integer> nonTerminatingCallback) {
        final IntArrayList colors = new IntArrayList();
        final IntArrayList terminatingColors = new IntArrayList();
        MinecraftClientData.getInstance().simplifiedRoutes.stream().filter(simplifiedRoute -> simplifiedRoute.getPlatformIndex(platformId) >= 0 && !simplifiedRoute.getName().isEmpty()).sorted().forEach(simplifiedRoute -> {
            final int currentStationIndex = simplifiedRoute.getPlatformIndex(platformId);
            if (currentStationIndex < simplifiedRoute.getPlatforms().size() - 1) {
                nonTerminatingCallback.accept(simplifiedRoute, currentStationIndex);
                if (!colors.contains(simplifiedRoute.getColor())) {
                    colors.add(simplifiedRoute.getColor());
                }
            } else {
                if (!terminatingColors.contains(simplifiedRoute.getColor())) {
                    terminatingColors.add(simplifiedRoute.getColor());
                }
            }
        });
        if (colors.isEmpty()) {
            colors.addAll(terminatingColors);
        }
        return colors;
    }

    private static String getStationName(long platformId) {
        final Platform platform = MinecraftClientData.getInstance().platformIdMap.get(platformId);
        final Station station = platform == null ? null : platform.area;
        return station == null ? "" : station.getName();
    }

    private static void drawLine(NativeImage nativeImage, StationPosition stationPosition1, StationPosition stationPosition2, float widthScale, float heightScale, float xOffset, float yOffset, int color) {
        final int x1 = Math.round((stationPosition1.x + xOffset) * scale * widthScale);
        final int x2 = Math.round((stationPosition2.x + xOffset) * scale * widthScale);
        final int y1 = Math.round((stationPosition1.y + yOffset) * scale * heightScale);
        final int y2 = Math.round((stationPosition2.y + yOffset) * scale * heightScale);
        final int xChange = x2 - x1;
        final int yChange = y2 - y1;
        final int xChangeAbs = Math.abs(xChange);
        final int yChangeAbs = Math.abs(yChange);
        final int changeDifference = Math.abs(yChangeAbs - xChangeAbs);

        if (xChangeAbs > yChangeAbs) {
            final boolean y1OffsetGreater = Math.abs(y1 - yOffset * scale) > Math.abs(y2 - yOffset * scale);
            drawLine(nativeImage, x1, y1, x2 - x1, y1OffsetGreater ? 0 : y2 - y1, y1OffsetGreater ? changeDifference : yChangeAbs, color);
            drawLine(nativeImage, x2, y2, x1 - x2, y1OffsetGreater ? y1 - y2 : 0, y1OffsetGreater ? yChangeAbs : changeDifference, color);
        } else {
            final int halfXChangeAbs = xChangeAbs / 2;
            drawLine(nativeImage, x1, y1, x2 - x1, y2 - y1, halfXChangeAbs, color);
            drawLine(nativeImage, x2, y2, x1 - x2, y1 - y2, halfXChangeAbs, color);
            drawLine(nativeImage, (x1 + x2) / 2, y1 + (int) Math.copySign(halfXChangeAbs, y2 - y1), 0, y2 - y1, changeDifference, color);
        }
    }

    private static void drawLine(NativeImage nativeImage, int x, int y, int directionX, int directionY, int length, int color) {
        final int halfLineHeight = lineSize / 2;
        final int xWidth = directionX == 0 ? halfLineHeight : 0;
        final int yWidth = directionX == 0 ? 0 : directionY == 0 ? halfLineHeight : Math.round(lineSize * MathHelper.getSquareRootOfTwoMapped() / 2);
        final int yMin = y - halfLineHeight - (directionY < 0 ? length : 0) + 1;
        final int yMax = y + halfLineHeight + (directionY > 0 ? length : 0) - 1;
        final int drawOffset = directionX != 0 && directionY != 0 ? halfLineHeight : 0;

        for (int i = -drawOffset; i < Math.abs(length) + drawOffset; i++) {
            final int drawX = x + (directionX == 0 ? 0 : (int) Math.copySign(i, directionX)) + (directionX < 0 ? -1 : 0);
            final int drawY = y + (directionY == 0 ? 0 : (int) Math.copySign(i, directionY)) + (directionY < 0 ? -1 : 0);

            for (int xOffset = 0; xOffset < xWidth; xOffset++) {
                drawPixelSafe(nativeImage, drawX - xOffset - 1, drawY, color);
                drawPixelSafe(nativeImage, drawX + xOffset, drawY, color);
            }

            for (int yOffset = 0; yOffset < yWidth; yOffset++) {
                drawPixelSafe(nativeImage, drawX, Math.max(drawY - yOffset, yMin) - 1, color);
                drawPixelSafe(nativeImage, drawX, Math.min(drawY + yOffset, yMax), color);
            }
        }
    }

    /**
     * Отрисовывает кружок станции. Если есть пересадки, кружок делится на равные сектора (доли).
     */
    /**
     * Отрисовывает кружок станции. Если есть пересадки, кружок делится на равные сектора (доли).
     */
    // Добавили boolean isCurrentStation перед int lineColor
    private static void drawStation(NativeImage nativeImage, int x, int y, float heightScale, int lines, boolean passed, boolean isCurrentStation, int lineColor, IntArrayList interchangeColors) {
        IntArrayList allColors = new IntArrayList();
        allColors.add(lineColor);
        // Если станция пройдена и она НЕ текущая, то мы не собираем цвета пересадок (она будет серой)
        if (interchangeColors != null && !(passed && !isCurrentStation)) {
            for (int i = 0; i < interchangeColors.size(); i++) {
                int c = interchangeColors.getInt(i);
                if (allColors.indexOf(c) == -1 && c != lineColor) {
                    allColors.add(c);
                }
            }
        }
        int numSegments = allColors.size();

        double radius = lineSize;

        for (int offsetX = -lineSize - 1; offsetX <= lineSize + 1; offsetX++) {
            for (int offsetY = -lineSize - 1; offsetY <= lineSize + 1; offsetY++) {
                final int extraOffsetY = offsetY > 0 ? (int) (lines * lineSpacing * heightScale) : 0;
                final int repeatDraw = offsetY == 0 ? (int) (lines * lineSpacing * heightScale) : 0;

                double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);

                if (distance <= radius + 0.5) {
                    float alphaMultiplier = 1.0f;
                    if (distance > radius - 0.5) {
                        alphaMultiplier = (float) (radius + 0.5 - distance);
                    }

                    int color;

                    // Если станция текущая, делаем ей белый центр
                    if (distance <= 3 && isCurrentStation) {
                        float dotAlpha = distance > 2.5 ? (float)(3.5 - distance) : 1.0f;
                        color = ((int)(255 * dotAlpha) << 24) | 0x00FFFFFF;
                    }
                    // Для остальных станций (или если не текущая, но расстояние <= 3)
                    // Если вы хотите, чтобы у всех станций был белый центр, уберите && isCurrentStation выше
                    else {
                        int segmentColor = lineColor;

                        // Логика сегментов пересадок
                        if (numSegments > 1 && !(passed && !isCurrentStation)) {
                            double angle = (Math.toDegrees(Math.atan2(offsetY, offsetX)) + 450) % 360;
                            int segmentIndex = (int) (angle / (360.0 / numSegments));
                            if (segmentIndex >= numSegments) segmentIndex = numSegments - 1;
                            segmentColor = allColors.getInt(segmentIndex);
                        }

                        // Если станция пройдена и НЕ текущая, она серая
                        if (passed && !isCurrentStation) segmentColor = ARGB_LIGHT_GRAY;

                        float t = Math.min(1.0f, Math.max(0.0f, (float) ((distance - 3) / (radius - 3))));
                        int r2 = (segmentColor >> 16) & 0xFF;
                        int g2 = (segmentColor >> 8) & 0xFF;
                        int b2 = segmentColor & 0xFF;

                        int finalR = (int) (255 * (1 - t) + r2 * t);
                        int finalG = (int) (255 * (1 - t) + g2 * t);
                        int finalB = (int) (255 * (1 - t) + b2 * t);

                        int finalA = (int) (255 * alphaMultiplier);

                        color = (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
                    }

                    for (int i = 0; i <= repeatDraw; i++) {
                        drawPixelSafe(nativeImage, x + offsetX, y + offsetY + extraOffsetY + i, color);
                    }
                }
            }
        }
    }






    private static void drawString(NativeImage nativeImage, byte[] pixels, int x, int y, int[] textDimensions, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, int backgroundColor, int textColor, boolean rotate90) {
        if (((backgroundColor >> 24) & 0xFF) > 0) {
            for (int drawX = 0; drawX < textDimensions[rotate90 ? 1 : 0]; drawX++) {
                for (int drawY = 0; drawY < textDimensions[rotate90 ? 0 : 1]; drawY++) {
                    drawPixelSafe(nativeImage, (int) horizontalAlignment.getOffset(drawX + x, textDimensions[rotate90 ? 1 : 0]), (int) verticalAlignment.getOffset(drawY + y, textDimensions[rotate90 ? 0 : 1]), backgroundColor);
                }
            }
        }
        int drawX = 0;
        int drawY = rotate90 ? textDimensions[0] - 1 : 0;
        for (int i = 0; i < textDimensions[0] * textDimensions[1]; i++) {
            blendPixel(nativeImage, (int) horizontalAlignment.getOffset(x + drawX, textDimensions[rotate90 ? 1 : 0]), (int) verticalAlignment.getOffset(y + drawY, textDimensions[rotate90 ? 0 : 1]), ((pixels[i] & 0xFF) << 24) + (textColor & RGB_WHITE));
            if (rotate90) {
                drawY--;
                if (drawY < 0) {
                    drawY = textDimensions[0] - 1;
                    drawX++;
                }
            } else {
                drawX++;
                if (drawX == textDimensions[0]) {
                    drawX = 0;
                    drawY++;
                }
            }
        }
    }

    private static void drawStringPixelated(NativeImage nativeImage, byte[] pixels, int[] textDimensions, int textColor, boolean fullPixel) {
        final int yOffset = (textDimensions[1] * (fullPixel ? 1 : PIXEL_SCALE) - nativeImage.getHeight()) / 2;
        int drawX = 0;
        int drawY = 0;
        for (int i = 0; i < textDimensions[0] * textDimensions[1]; i++) {
            if ((pixels[i] & 0xFF) > 0x7F) {
                if (fullPixel) {
                    drawPixelSafe(nativeImage, drawX, drawY - yOffset, textColor);
                } else {
                    for (int j = 0; j < 3; j++) {
                        for (int k = 0; k < 3; k++) {
                            drawPixelSafe(nativeImage, drawX * PIXEL_SCALE + j, drawY * PIXEL_SCALE + k - yOffset, textColor);
                        }
                    }
                }
            }
            drawX++;
            if (drawX == textDimensions[0]) {
                drawX = 0;
                drawY++;
            }
        }
    }

    private static void drawResource(NativeImage nativeImage, String resource, int x, int y, int width, int height, boolean flipX, float v1, float v2, int color, boolean useActualColor) {
        ResourceManagerHelper.readResource(new Identifier(Init.MOD_ID, resource), inputStream -> {
            try {
                final NativeImage nativeImageResource = NativeImage.read(NativeImageFormat.getAbgrMapped(), inputStream);
                final int resourceWidth = nativeImageResource.getWidth();
                final int resourceHeight = nativeImageResource.getHeight();
                for (int drawX = 0; drawX < width; drawX++) {
                    for (int drawY = Math.round(v1 * height); drawY < Math.round(v2 * height); drawY++) {
                        final float pixelX = (float) drawX / width * resourceWidth;
                        final float pixelY = (float) drawY / height * resourceHeight;
                        final int floorX = (int) pixelX;
                        final int floorY = (int) pixelY;
                        final int ceilX = floorX + 1;
                        final int ceilY = floorY + 1;
                        final float percentX1 = ceilX - pixelX;
                        final float percentY1 = ceilY - pixelY;
                        final float percentX2 = pixelX - floorX;
                        final float percentY2 = pixelY - floorY;
                        final int pixel1 = nativeImageResource.getColor(MathHelper.clamp(floorX, 0, resourceWidth - 1), MathHelper.clamp(floorY, 0, resourceHeight - 1));
                        final int pixel2 = nativeImageResource.getColor(MathHelper.clamp(ceilX, 0, resourceWidth - 1), MathHelper.clamp(floorY, 0, resourceHeight - 1));
                        final int pixel3 = nativeImageResource.getColor(MathHelper.clamp(floorX, 0, resourceWidth - 1), MathHelper.clamp(ceilY, 0, resourceHeight - 1));
                        final int pixel4 = nativeImageResource.getColor(MathHelper.clamp(ceilX, 0, resourceWidth - 1), MathHelper.clamp(ceilY, 0, resourceHeight - 1));
                        final int newColor;
                        if (useActualColor) {
                            newColor = invertColor(pixel1);
                        } else {
                            final float luminance1 = ((pixel1 >> 24) & 0xFF) * percentX1 * percentY1;
                            final float luminance2 = ((pixel2 >> 24) & 0xFF) * percentX2 * percentY1;
                            final float luminance3 = ((pixel3 >> 24) & 0xFF) * percentX1 * percentY2;
                            final float luminance4 = ((pixel4 >> 24) & 0xFF) * percentX2 * percentY2;
                            newColor = (color & RGB_WHITE) + ((int) (luminance1 + luminance2 + luminance3 + luminance4) << 24);
                        }
                        blendPixel(nativeImage, (flipX ? width - drawX - 1 : drawX) + x, drawY + y, newColor);
                    }
                }
            } catch (Exception e) {
                Init.LOGGER.error("", e);
            }
        });
    }

    private static void blendPixel(NativeImage nativeImage, int x, int y, int color) {
        if (Utilities.isBetween(x, 0, nativeImage.getWidth() - 1) && Utilities.isBetween(y, 0, nativeImage.getHeight() - 1)) {
            final float percent = (float) ((color >> 24) & 0xFF) / 0xFF;
            if (percent > 0) {
                final int existingPixel = nativeImage.getColor(x, y);
                final boolean existingTransparent = ((existingPixel >> 24) & 0xFF) == 0;
                final int r1 = existingTransparent ? 0xFF : (existingPixel & 0xFF);
                final int g1 = existingTransparent ? 0xFF : ((existingPixel >> 8) & 0xFF);
                final int b1 = existingTransparent ? 0xFF : ((existingPixel >> 16) & 0xFF);
                final int r2 = (color >> 16) & 0xFF;
                final int g2 = (color >> 8) & 0xFF;
                final int b2 = color & 0xFF;
                final float inversePercent = 1 - percent;
                final int finalColor = ARGB_BLACK | (((int) (r1 * inversePercent + r2 * percent) << 16) + ((int) (g1 * inversePercent + g2 * percent) << 8) + (int) (b1 * inversePercent + b2 * percent));
                drawPixelSafe(nativeImage, x, y, finalColor);
            }
        }
    }

    private static void drawPixelSafe(NativeImage nativeImage, int x, int y, int color) {
        if (Utilities.isBetween(x, 0, nativeImage.getWidth() - 1) && Utilities.isBetween(y, 0, nativeImage.getHeight() - 1)) {
            nativeImage.setPixelColor(x, y, invertColor(color));
        }
    }

    private static int invertColor(int color) {
        return ((color & ARGB_BLACK) != 0 ? ARGB_BLACK : 0) + ((color & 0xFF) << 16) + (color & 0xFF00) + ((color & 0xFF0000) >> 16);
    }

    private static void clearColor(NativeImage nativeImage, int color) {
        for (int x = 0; x < nativeImage.getWidth(); x++) {
            for (int y = 0; y < nativeImage.getHeight(); y++) {
                if (nativeImage.getColor(x, y) == color) {
                    nativeImage.setPixelColor(x, y, 0);
                }
            }
        }
    }

    private static class StationPosition {

        private final float x;
        private final float y;
        private final boolean isCommon;

        private StationPosition(float x, float y, boolean isCommon) {
            this.x = x;
            this.y = y;
            this.isCommon = isCommon;
        }
    }

    private static class StationPositionGrouped {

        private final StationPosition stationPosition;
        private final int stationOffset;
        private final IntArrayList interchangeColors;
        private final ObjectArrayList<String> interchangeNames;

        private StationPositionGrouped(StationPosition stationPosition, int stationOffset, IntArrayList interchangeColors, ObjectArrayList<String> interchangeNames) {
            this.stationPosition = stationPosition;
            this.stationOffset = stationOffset;
            this.interchangeColors = interchangeColors;
            this.interchangeNames = interchangeNames;
        }
    }

    private static String transliterate(String text) {
        char[] rus = {'а','б','в','г','д','е','ё','ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','ъ','ы','ь','э','ю','я'};
        String[] eng = {"a","b","v","g","d","e","yo","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","kh","ts","ch","sh","sch","","y","","e","yu","ya"};

        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {

            boolean upper = Character.isUpperCase(c);
            char lower = Character.toLowerCase(c);

            boolean found = false;

            for (int i = 0; i < rus.length; i++) {
                if (lower == rus[i]) {

                    String r = eng[i];

                    if (upper) {
                        r = Character.toUpperCase(r.charAt(0)) + r.substring(1);
                    }

                    result.append(r);
                    found = true;
                    break;
                }
            }

            if (!found) result.append(c);
        }

        return result.toString();
    }


    private static void drawRotatedString(NativeImage nativeImage, byte[] pixels, int[] dimensions, int anchorX, int anchorY, float angleDegrees, int textColor, float alignX, float alignY) {
        if (pixels == null) return;

        int textWidth = dimensions[0];
        int textHeight = dimensions[1];
        double angleRad = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);

        // Точка вращения внутри текста (теперь дробная для точности)
        double pivotX = textWidth * alignX;
        double pivotY = textHeight * alignY;

        // Размер области для поиска пикселей (с запасом)
        int size = (int) (Math.max(textWidth, textHeight) * 1.5);

        for (int dx = -size; dx <= size; dx++) {
            for (int dy = -size; dy <= size; dy++) {
                // Обратное вращение: получаем точную ДРОБНУЮ координату исходного пикселя шрифта
                double srcX = dx * cos + dy * sin + pivotX;
                double srcY = dy * cos - dx * sin + pivotY;

                // Проверяем, попадает ли точка в границы исходного шрифта
                if (srcX >= 0 && srcX < textWidth - 1 && srcY >= 0 && srcY < textHeight - 1) {
                    // --- БИЛИНЕЙНАЯ ИНТЕРПОЛЯЦИЯ ДЛЯ СГЛАЖИВАНИЯ ---

                    // Целая часть координат
                    int x1 = (int) srcX;
                    int y1 = (int) srcY;
                    int x2 = x1 + 1;
                    int y2 = y1 + 1;

                    // Дробная часть (веса для смешивания)
                    double fx = srcX - x1;
                    double fy = srcY - y1;

                    // Получаем альфа-канал из 4-х соседних пикселей
                    double alpha11 = pixels[y1 * textWidth + x1] & 0xFF; // верхний левый
                    double alpha21 = pixels[y1 * textWidth + x2] & 0xFF; // верхний правый
                    double alpha12 = pixels[y2 * textWidth + x1] & 0xFF; // нижний левый
                    double alpha22 = pixels[y2 * textWidth + x2] & 0xFF; // нижний правый

                    // Смешиваем их с учетом весов
                    double interpolatedAlpha =
                            alpha11 * (1 - fx) * (1 - fy) +
                                    alpha21 * fx * (1 - fy) +
                                    alpha12 * (1 - fx) * fy +
                                    alpha22 * fx * fy;

                    int finalAlpha = (int) interpolatedAlpha;

                    // Порог видимости оставляем низким, чтобы сохранить мягкие края
                    if (finalAlpha > 10) {
                        blendPixel(nativeImage, anchorX + dx, anchorY + dy, (finalAlpha << 24) | (textColor & RGB_WHITE));
                    }
                }
            }
        }
    }


    private static void drawArrow(NativeImage image, int x, int y, int color, boolean isRight) {
        int size = 20;
        int thickness = 10;
        int shaftLength = 50;

        // Рисуем наконечник
        for (int i = 0; i < size; i++) {
            for (int t = 0; t < thickness; t++) {
                if (isRight) {
                    // Острие вправо
                    drawPixelSafe(image, x - i, y - i + t, color);
                    drawPixelSafe(image, x - i, y + i + t, color);
                } else {
                    // Острие влево
                    drawPixelSafe(image, x + i, y - i + t, color);
                    drawPixelSafe(image, x + i, y + i + t, color);
                }
            }
        }

        // Рисуем древко (палочку)
        for (int i = 0; i < shaftLength; i++) {
            for (int t = 0; t < thickness; t++) {
                if (isRight) {
                    drawPixelSafe(image, x - i, y + t, color);
                } else {
                    drawPixelSafe(image, x + i, y + t, color);
                }
            }
        }
    }

    private static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int)(r * factor);
        g = (int)(g * factor);
        b = (int)(b * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }








}
