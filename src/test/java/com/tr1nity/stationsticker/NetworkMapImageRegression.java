package com.tr1nity.stationsticker;

import com.mojang.blaze3d.platform.NativeImage;
import com.tr1nity.stationsticker.client.NetworkMapImages;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

// Запускается без игры/GL-контекста, но с настоящей нативной памятью LWJGL.
public final class NetworkMapImageRegression {
    public static void main(String[] args) throws Exception {
        BufferedImage image = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);
        java.util.Random random = new java.util.Random(42);
        for (int y = 0; y < 2048; y++) for (int x = 0; x < 2048; x++) image.setRGB(x, y, random.nextInt());
        image.setRGB(0, 0, 0);
        image.setRGB(100, 200, 0x80CC2233);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", png);
        boolean reproduced = false;
        try (NativeImage ignored = NativeImage.read(png.toByteArray())) {
            throw new AssertionError("Old PNG path did not reproduce stack overflow");
        } catch (OutOfMemoryError expected) {
            if (!expected.getMessage().contains("stack space")) throw expected;
            reproduced = true;
        }
        try (NativeImage actual = NetworkMapImages.create(image)) {
            if (!reproduced || actual.getWidth() != 2048 || actual.getHeight() != 2048
                    || actual.getPixelRGBA(0, 0) != 0 || actual.getPixelRGBA(100, 200) != 0x803322CC) {
                throw new AssertionError("Direct pixel transfer corrupted dimensions, colors or alpha");
            }
        } finally { image.flush(); }
        System.out.println("PASS: original Out of stack space reproduced; direct 2048x2048 transfer succeeds with correct colors and alpha");
    }
}
