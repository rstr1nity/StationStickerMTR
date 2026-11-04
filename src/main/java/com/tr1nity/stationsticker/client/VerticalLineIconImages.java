package com.tr1nity.stationsticker.client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Shared decoder for MTR sign textures resolved through the active resource manager. */
public final class VerticalLineIconImages {
    private VerticalLineIconImages(){}
    public static BufferedImage read(InputStream input,int background,boolean flip)throws IOException{
        var source=ImageIO.read(input);if(source==null)throw new IOException("Unsupported sign image");
        var icon=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);var g=icon.createGraphics();
        g.setColor(new Color(background,true));g.fillRect(0,0,32,32);
        g.drawImage(source,flip?32:0,0,flip?0:32,32,0,0,source.getWidth(),source.getHeight(),null);g.dispose();source.flush();return icon;
    }
}
