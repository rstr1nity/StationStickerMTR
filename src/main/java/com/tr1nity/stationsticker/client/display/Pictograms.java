package com.tr1nity.stationsticker.client.display;

// Пиктограммы навигации метро «пикселями»: '#' - закрашенная клетка. Линии в одну клетку, как на табличках.
public final class Pictograms {

    // Человек идёт вправо: наклон вперёд, руки в замахе, ноги в шаге, 11x14
    public static final String[] PERSON = {
            "......##...",
            ".....####..",
            "......##...",
            ".....###...",
            "....#####..",
            "...#.###.#.",
            "..#..###..#",
            ".....###...",
            ".....####..",
            "....##..##.",
            "...##....##",
            "..##......#",
            ".##.......#",
            "##........."};

    // Человек и лестница вверх по ходу движения (ступени 3x3), 22x18
    public static final String[] STAIRS = withStairs(PERSON);

    // Стрелка влево 8x7; остальные направления получаются поворотом
    public static final String[] ARROW_LEFT = {
            "...#....",
            "..##....",
            ".#######",
            "########",
            ".#######",
            "..##....",
            "...#...."};

    private Pictograms() {
    }

    public static void draw(SlideCanvas canvas, String[] rows, float x, float y, float scale, int color, boolean mirror) {
        for (int row = 0; row < rows.length; row++) {
            String line = mirror ? new StringBuilder(rows[row]).reverse().toString() : rows[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    // Чуть больше шага, чтобы между клетками не было щелей
                    canvas.rect(x + col * scale, y + row * scale, scale * 1.02f, scale * 1.02f, color);
                }
            }
        }
    }

    // «Посадки нет»: кольцо, человек и косая черта
    public static void noBoarding(SlideCanvas canvas, float centerX, float centerY, float radius, int color) {
        float ring = radius * 0.14f;
        canvas.ring(centerX, centerY, radius, radius - ring, color);

        float scale = radius * 1.2f / PERSON.length;
        draw(canvas, PERSON, centerX - PERSON[0].length() * scale / 2, centerY - PERSON.length * scale / 2, scale, color, false);

        // Черта из верхнего левого края кольца в нижний правый
        float reach = (radius - ring / 2) * 0.7071f;
        canvas.line(centerX - reach, centerY - reach, centerX + reach, centerY + reach, ring, color);
    }

    public static String[] mirror(String[] rows) {
        String[] result = new String[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = new StringBuilder(rows[i]).reverse().toString();
        return result;
    }

    public static String[] flipVertical(String[] rows) {
        String[] result = new String[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = rows[rows.length - 1 - i];
        return result;
    }

    public static String[] transpose(String[] rows) {
        String[] result = new String[rows[0].length()];
        for (int col = 0; col < rows[0].length(); col++) {
            StringBuilder builder = new StringBuilder();
            for (String row : rows) builder.append(row.charAt(col));
            result[col] = builder.toString();
        }
        return result;
    }

    // Лестница справа от человека: ступени шириной и высотой 3 клетки от низа вверх вправо
    private static String[] withStairs(String[] person) {
        int width = 22;
        int height = 18;
        char[][] grid = new char[height][width];
        for (char[] row : grid) java.util.Arrays.fill(row, '.');
        for (int y = 0; y < person.length; y++) {
            for (int x = 0; x < person[y].length(); x++) grid[y][x] = person[y].charAt(x);
        }
        int x = 11;
        int y = height - 1;
        while (x < width && y >= 0) {
            for (int i = 0; i < 3 && x + i < width; i++) grid[y][x + i] = '#';
            x += 3;
            for (int i = 0; i < 3 && y - i >= 0 && x < width; i++) grid[y - i][x] = '#';
            y -= 3;
        }
        String[] result = new String[height];
        for (int i = 0; i < height; i++) result[i] = new String(grid[i]);
        return result;
    }
}
