package com.tr1nity.stationsticker.client.display;

import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;

// Слайд станции: крупно название, латиница под ним, кружки линий станции с номерами
public final class StationSlide implements DisplaySlide {

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_COLOR = 0xFFB8C0CC;
    private static final float CIRCLE_RADIUS = 6;

    private final String stationName;
    private final IntArrayList lineColors;
    private final java.util.List<String> lineNumbers;

    public StationSlide(String stationName, IntArrayList lineColors, java.util.List<String> lineNumbers) {
        this.stationName = stationName;
        this.lineColors = lineColors;
        this.lineNumbers = lineNumbers;
    }

    @Override
    public void render(SlideCanvas canvas) {
        canvas.text(StationNames.primary(stationName), 0, 4, 2.6f, TEXT_COLOR, canvas.width);
        canvas.text(StationNames.secondary(stationName), 0, 28, 1.1f, SECONDARY_COLOR, canvas.width);
        float x = CIRCLE_RADIUS;
        for (int i = 0; i < lineColors.size(); i++) {
            canvas.circle(x, 44, CIRCLE_RADIUS, 0xFF000000 | lineColors.getInt(i));
            String number = lineNumbers.get(i);
            if (!number.isEmpty()) {
                float scale = Math.min(1, CIRCLE_RADIUS * 1.4f / canvas.boldTextWidth(number, 1));
                canvas.boldText(number, x - canvas.boldTextWidth(number, scale) / 2, 44 - 3.5f * scale, scale, TEXT_COLOR);
            }
            x += CIRCLE_RADIUS * 2 + 3;
        }
    }
}
