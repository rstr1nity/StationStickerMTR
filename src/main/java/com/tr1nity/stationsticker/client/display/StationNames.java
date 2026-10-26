package com.tr1nity.stationsticker.client.display;

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

    private static boolean hasCyrillic(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeBlock.of(text.charAt(i)) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }
}
