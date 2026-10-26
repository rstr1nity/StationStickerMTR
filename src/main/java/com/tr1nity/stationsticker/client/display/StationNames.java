package com.tr1nity.stationsticker.client.display;

import com.tr1nity.stationsticker.client.SPBRouteMapGenerator;
import net.minecraft.client.Minecraft;

// Названия в MTR хранятся по языкам через '|', например "Курская|Kurskaya".
// Для русского языка игры берём часть с кириллицей, для остальных - часть без неё.
public final class StationNames {

    private StationNames() {
    }

    public static String forCurrentLanguage(String name) {
        String[] parts = name.split("\\|");
        boolean russian = Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru");
        for (String part : parts) {
            if (!part.isBlank() && hasCyrillic(part) == russian) {
                return part.trim();
            }
        }
        return parts.length == 0 ? name : parts[0].trim();
    }

    // Подпись мелким шрифтом под основным названием: другой язык из MTR, иначе транслит русского названия
    public static String secondary(String name) {
        String primary = forCurrentLanguage(name);
        for (String part : name.split("\\|")) {
            if (!part.isBlank() && !part.trim().equals(primary)) {
                return part.trim();
            }
        }
        return hasCyrillic(primary) ? SPBRouteMapGenerator.transliterate(primary) : "";
    }

    private static boolean hasCyrillic(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeBlock.of(text.charAt(i)) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }
}
