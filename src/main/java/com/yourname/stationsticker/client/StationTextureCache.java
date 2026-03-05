package com.yourname.stationsticker.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.AttributedString;

public class StationTextureCache {

    private static Font font;

    public static ResourceLocation getStationTexture(String stationName, int lineColor) {
        String key = "station_" + stationName.hashCode() + "_" + lineColor;
        ResourceLocation location = new ResourceLocation("stationsticker", key);

        var textureManager = Minecraft.getInstance().getTextureManager();
        if (textureManager.getTexture(location, null) != null) {
            return location;
        }

        BufferedImage image = generateStationTexture(stationName, lineColor);
        registerTexture(location, image);

        return location;
    }

    private static BufferedImage generateStationTexture(String stationName, int lineColor) {
        if (font == null) {
            try {
                font = new Font("Arial", Font.BOLD, 20);
            } catch (Exception e) {
                font = new Font("SansSerif", Font.BOLD, 20);
            }
        }

        FontRenderContext frc = new FontRenderContext(null, true, true);
        int textWidth = (int) font.getStringBounds(stationName, frc).getWidth();
        int textHeight = (int) font.getStringBounds(stationName, frc).getHeight();

        int padding = 10;
        int width = Math.max(textWidth + padding * 2, 64);
        int height = (int) (textHeight * 1.5) + padding * 2;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Рисуем цветную полосу (фон)
        g2d.setColor(new Color(lineColor, true));
        g2d.fillRect(0, 0, width, height);

        // Рисуем белый текст по центру
        AttributedString attributedString = new AttributedString(stationName);
        attributedString.addAttribute(TextAttribute.FONT, font);
        attributedString.addAttribute(TextAttribute.FOREGROUND, Color.WHITE);

        int textX = (width - textWidth) / 2;
        int textY = (height + textHeight) / 2 - 3;

        g2d.drawString(attributedString.getIterator(), textX, textY);
        g2d.dispose();
        return image;
    }

    private static void registerTexture(ResourceLocation location, BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                nativeImage.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        DynamicTexture texture = new DynamicTexture(nativeImage);
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }
}