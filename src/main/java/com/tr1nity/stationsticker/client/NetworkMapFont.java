package com.tr1nity.stationsticker.client;

import com.google.gson.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

// Снимок битовых глифов из ресурсов игры; после загрузки можно рисовать в рабочем потоке.
public final class NetworkMapFont {
    @FunctionalInterface public interface Resources { InputStream open(String id) throws IOException; }
    private record Glyph(boolean[][] pixels, float unit, float advance, float ascent) {}
    private final Map<Integer, Glyph> glyphs;
    private final Map<Integer, Glyph> missing = new HashMap<>();
    private int size = 24;
    private int density = 1;
    public void density(int density) { this.density = Math.max(1, density); }
    private NetworkMapFont(Map<Integer, Glyph> glyphs) { this.glyphs = Map.copyOf(glyphs); }

    public static NetworkMapFont load(Resources resources, Set<Integer> needed) throws IOException {
        Map<Integer, Glyph> glyphs = new HashMap<>();
        loadProviders(resources, "minecraft:default", needed, glyphs, new HashSet<>());
        return new NetworkMapFont(glyphs);
    }
    public static NetworkMapFont fallback() { return new NetworkMapFont(Map.of()); }
    private static void loadProviders(Resources resources, String id, Set<Integer> needed, Map<Integer, Glyph> glyphs, Set<String> visited) throws IOException {
        if (!visited.add(id)) return;
        String[] path = id.split(":", 2);
        String namespace = path.length == 2 ? path[0] : "minecraft", name = path[path.length - 1];
        JsonObject root;
        try (Reader reader = new InputStreamReader(resources.open(namespace + ":font/" + name + ".json"), StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (JsonElement element : root.getAsJsonArray("providers")) {
            JsonObject provider = element.getAsJsonObject();
            String type = provider.get("type").getAsString().replace("minecraft:", "");
            if (type.equals("reference")) loadProviders(resources, provider.get("id").getAsString(), needed, glyphs, visited);
            if (type.equals("bitmap")) {
                String file = provider.get("file").getAsString();
                String[] location = file.split(":", 2);
                BufferedImage sheet;
                try (InputStream input = resources.open((location.length == 2 ? location[0] : "minecraft") + ":textures/" + location[location.length - 1])) {
                    sheet = ImageIO.read(input);
                }
                JsonArray rows = provider.getAsJsonArray("chars");
                int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
                int w = sheet.getWidth() / columns, h = sheet.getHeight() / rows.size();
                float unit = (provider.has("height") ? provider.get("height").getAsFloat() : 8) / h;
                float ascent = provider.get("ascent").getAsFloat();
                for (int y = 0; y < rows.size(); y++) {
                    int[] chars = rows.get(y).getAsString().codePoints().toArray();
                    for (int x = 0; x < chars.length; x++) {
                        int code = chars[x];
                        if (code == 0 || !needed.contains(code) || glyphs.containsKey(code)) continue;
                        boolean[][] pixels = new boolean[h][w]; int right = 0;
                        for (int py = 0; py < h; py++) for (int px = 0; px < w; px++) {
                            if ((sheet.getRGB(x * w + px, y * h + py) >>> 24) > 127) { pixels[py][px] = true; right = Math.max(right, px + 1); }
                        }
                        if (right > 0) glyphs.put(code, new Glyph(pixels, unit, right * unit + 1, ascent));
                    }
                }
                sheet.flush();
            }
            if (type.equals("legacy_unicode")) {
                byte[] sizes;
                try (InputStream input=resources.open(provider.get("sizes").getAsString())) { sizes=input.readAllBytes(); }
                Map<Integer,BufferedImage> pages=new HashMap<>();
                try {
                    for(int code:needed){
                        if(code<0 || code>=sizes.length || glyphs.containsKey(code))continue;
                        int packed=sizes[code]&255;if(packed==0)continue;
                        int left=packed>>>4,right=(packed&15)+1,w=right-left;if(w<=0)continue;
                        int page=code>>>8;BufferedImage sheet=pages.get(page);
                        if(sheet==null){
                            String pagePath=String.format(Locale.ROOT,provider.get("template").getAsString(),String.format(Locale.ROOT,"%02x",page));
                            String[] location=pagePath.split(":",2);
                            try(InputStream input=resources.open((location.length==2?location[0]:"minecraft")+":textures/"+location[location.length-1])){sheet=ImageIO.read(input);}
                            pages.put(page,sheet);
                        }
                        boolean[][] pixels=new boolean[16][w];
                        for(int y=0;y<16;y++)for(int x=0;x<w;x++)pixels[y][x]=(sheet.getRGB((code&15)*16+left+x,((code&255)>>>4)*16+y)>>>24)>127;
                        glyphs.put(code,new Glyph(pixels,.5f,w*.5f+1,7));
                    }
                } finally {pages.values().forEach(BufferedImage::flush);}
            }
            if (type.equals("unihex")) {
                try (ZipInputStream zip = new ZipInputStream(resources.open(provider.get("hex_file").getAsString()))) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".hex")) continue;
                        BufferedReader reader = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            int colon = line.indexOf(':');
                            // Архив содержит также лицензию/README; проверяем и сами записи .hex.
                            if (colon < 4 || colon > 6 || !hexadecimal(line.substring(0, colon))) continue;
                            int code = Integer.parseInt(line.substring(0, colon), 16);
                            if (!Character.isValidCodePoint(code) || !needed.contains(code) || glyphs.containsKey(code)) continue;
                            String hex = line.substring(colon + 1);
                            if ((hex.length() != 32 && hex.length() != 64 && hex.length() != 96 && hex.length() != 128) || !hexadecimal(hex)) continue;
                            int w = hex.length() / 4;
                            boolean[][] pixels = new boolean[16][w];
                            for (int y = 0; y < 16; y++) for (int x = 0; x < w; x++) {
                                int value = Character.digit(hex.charAt(y * w / 4 + x / 4), 16);
                                pixels[y][x] = (value & (8 >> (x % 4))) != 0;
                            }
                            glyphs.put(code, new Glyph(pixels, 0.5f, w / 2f + 1, 7));
                        }
                    }
                }
            }
        }
    }
    private static boolean hexadecimal(String value) {
        return value.chars().allMatch(c -> Character.digit(c, 16) >= 0);
    }
    // Для символов, отсутствующих в bitmap-провайдерах, сохраняем читаемый пиксельный fallback.
    private Glyph glyph(int code) {
        Glyph found = glyphs.get(code);
        if (found != null) return found;
        if (missing.containsKey(code)) return missing.get(code);
        BufferedImage tile = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tile.createGraphics();
        try {
            g.setFont(new Font("Monospaced", Font.PLAIN, 15)); g.setColor(Color.WHITE);
            g.drawString(new String(Character.toChars(code)), 0, 13);
        } finally { g.dispose(); }
        boolean[][] pixels = new boolean[16][16]; int right = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((tile.getRGB(x, y) >>> 24) > 127) { pixels[y][x] = true; right = Math.max(right, x + 1); }
        Glyph fallback = new Glyph(pixels, 0.5f, right / 2f + 1, 6.5f);
        missing.put(code, fallback);
        return fallback;
    }
    public void size(int size) { this.size = Math.max(8, size); }
    private int scale() { return Math.max(1, Math.round(size / 8f)); }
    public int height() { return (int) Math.ceil(9.0 * scale() / density); }
    public int width(String text) {
        float width = 0;
        for (int code : text.codePoints().toArray()) width += code == 32 ? 4 : glyph(code).advance;
        return (int) Math.ceil(width * scale() / density);
    }
    public double boldOffset() { return 1.0 / density; }
    public void drawBold(Graphics2D g, String text, double x, double baseline) {
        draw(g, text, x, baseline);
        draw(g, text, x + boldOffset(), baseline);
    }
    public Shape outline(String text, double x, double baseline) {
        // Полупиксельный якорь с rint на каждой границе чередовал ширины 0/2 вместо 1.
        x = Math.rint(x * density) / density; baseline = Math.rint(baseline * density) / density;
        Path2D result = new Path2D.Double();
        for (int code : text.codePoints().toArray()) {
            if (code == 32) { x += 4.0 * scale() / density; continue; }
            Glyph glyph = glyph(code);
            for (int y = 0; y < glyph.pixels.length; y++) for (int px = 0; px < glyph.pixels[y].length; px++) {
                if (glyph.pixels[y][px]) {
                    double x0 = Math.rint(x * density + px * glyph.unit * scale()) / density, y0 = Math.rint(baseline * density - glyph.ascent * scale() + y * glyph.unit * scale()) / density;
                    double x1 = Math.rint(x * density + (px + 1) * glyph.unit * scale()) / density, y1 = Math.rint(baseline * density - glyph.ascent * scale() + (y + 1) * glyph.unit * scale()) / density;
                    result.append(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0), false);
                }
            }
            x += glyph.advance * scale() / density;
        }
        return result;
    }
    public void draw(Graphics2D g, String text, double x, double baseline) {
        Object antialias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.fill(outline(text, x, baseline));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
    }
}
