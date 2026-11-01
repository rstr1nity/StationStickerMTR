package com.tr1nity.stationsticker.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;

public final class NetworkMapImages {
    private NetworkMapImages() {}

    // Выделяем память под пиксели вне маленького временного стека LWJGL.
    public static NativeImage create(BufferedImage image) {
        NativeImage result = new NativeImage(image.getWidth(), image.getHeight(), false);
        try {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int abgr = (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
                result.setPixelRGBA(x, y, abgr);
            }
            return result;
        } catch (RuntimeException | Error error) {
            result.close();
            throw error;
        }
    }
}
