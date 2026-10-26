package com.tr1nity.stationsticker.client.display;

import com.tr1nity.stationsticker.client.SPBRouteMapGenerator;
import net.minecraft.client.Minecraft;

// Названия в MTR хранятся по языкам через '|', например "Курская|Kurskaya".
// Всё после '||' - заметка для дашборда («линия 1|line 1||in»), на табличках не показывается.
public final class StationNames {

    private StationNames() {
    }

    // Станции - как в метро, независимо от языка игры: крупно русское название (часть с кириллицей),
    // если его нет - первая часть (латинское название показывается одно, без второй строки)
    public static String primary(String name) {
        String[] parts = withoutNote(name).split("\\|");
        for (String part : parts) {
            if (!part.isBlank() && hasCyrillic(part)) {
                return part.trim();
            }
        }
        return parts.length == 0 ? name : parts[0].trim();
    }

    // Мелкая строка под названием станции: латиница из MTR, иначе транслит; у латинского названия - пусто
    public static String secondary(String name) {
        String primary = primary(name);
        if (!hasCyrillic(primary)) return "";
        for (String part : withoutNote(name).split("\\|")) {
            if (!part.isBlank() && !hasCyrillic(part)) {
                return part.trim();
            }
        }
        return SPBRouteMapGenerator.transliterate(primary);
    }

    // Линии и прочее, что переводится вместе с надписью: часть на языке игры
    // (для русского - с кириллицей, для остальных - без неё)
    public static String forCurrentLanguage(String name) {
        name = withoutNote(name);
        String[] parts = name.split("\\|");
        boolean russian = Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru");
        for (String part : parts) {
            if (!part.isBlank() && hasCyrillic(part) == russian) {
                return part.trim();
            }
        }
        return parts.length == 0 ? name : parts[0].trim();
    }

    private static String withoutNote(String name) {
        int note = name.indexOf("||");
        return note < 0 ? name : name.substring(0, note);
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
