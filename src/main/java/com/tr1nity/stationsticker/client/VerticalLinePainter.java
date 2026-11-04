package com.tr1nity.stationsticker.client;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public final class VerticalLinePainter {
    public static final int WIDTH = 512, HEIGHT = 1344;
    private static final Color PAPER = new Color(0xFAF8F1), INK = new Color(0x25272B);
    private VerticalLinePainter() {}
    public static BufferedImage paint(VerticalLineData data, NetworkMapFont font) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(PAPER); g.fillRect(0, 0, WIDTH, HEIGHT);
        font.density(1);
        if (data == null || data.stops().isEmpty()) { text(g,font,"MTR",40,100,24,INK); g.dispose(); return image; }
        badge(g,font,data.color(),data.number(),55,85,25);
        if (data.direction().isEmpty()) text(g,font,"↑",96,57,16,INK);
        else fitted(g,font,data.direction(),96,48,12,WIDTH-130,INK);
        fitted(g,font,data.heading(),96,92,28,WIDTH-130,INK);
        g.setColor(new Color(0xAAAAAA)); g.drawLine(32,142,WIDTH-32,142);
        // Reserve a separate lower region for the transfer legend.
        int legendRows = (data.legend().size()+1)/2;
        double legendTop = HEIGHT-65-legendRows*30;
        double start = 190, end = legendTop-75;
        double step = (end-start)/Math.max(1,data.stops().size()-1);
        int fontSize = step < 32 ? 8 : step < 50 ? 12 : 16;
        int x = 160;
        for (int i=0;i<data.stops().size();i++) {
            var stop=data.stops().get(i); double y=start+i*step;
            Color line = new Color(stop.passed()?0xBABDBB:data.color());
            if (i+1<data.stops().size()) { g.setColor(line); g.setStroke(new BasicStroke(7)); g.draw(new Line2D.Double(x,y,x,y+step)); }
            int arc=0;
            for (var transfer:stop.transfers()) if(transfer.circular()) {
                g.setColor(new Color(transfer.color())); g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new QuadCurve2D.Double(20,y-27-arc*10,WIDTH/2.0,y+27-arc*10,WIDTH-20,y-27-arc*10)); arc++;
            }
            g.setColor(line); g.setStroke(new BasicStroke(5));
            if(stop.current() || !stop.transfers().isEmpty() || data.circular() && (i==0 || i==data.stops().size()-1)) {
                g.setColor(PAPER); g.fill(new Ellipse2D.Double(x-9,y-9,18,18));
                g.setColor(line); g.draw(new Ellipse2D.Double(x-9,y-9,18,18));
            } else g.draw(new Line2D.Double(x,y,x+13,y));
            if(!data.circular() && (i==0 || i==data.stops().size()-1)) g.draw(new Line2D.Double(x-10,y,x+10,y));
            double gap=Math.min(23,120.0/Math.max(1,stop.transfers().size()));
            double bx=x-25;
            for(var transfer:stop.transfers()) { badge(g,font,transfer.color(),transfer.number(),bx,y,(int)Math.min(9,gap/2-1)); bx-=gap; }
            String name=stop.name(); int size=fontSize;
            font.size(size);
            List<String> rows=wrap(name,font,WIDTH-x-48);
            while(rows.size()>2 && size>8) {size-=4; font.size(size); rows=wrap(name,font,WIDTH-x-48);}
            double top=y-(rows.size()*font.height())/2.0+font.height()-3;
            if(!stop.current()) {
                font.size(size);
                int width=rows.stream().mapToInt(font::width).max().orElse(0);
                g.setColor(PAPER); g.fill(new Rectangle2D.Double(x+20,top-font.height()+1,Math.min(WIDTH-x-40,width+12),rows.size()*font.height()+5));
            }
            if(stop.current()) {
                font.size(size);
                int nameWidth=rows.stream().mapToInt(font::width).max().orElse(0);
                g.setColor(new Color(data.color()));g.fill(new Rectangle2D.Double(x+19,top-font.height()+1,Math.min(WIDTH-x-39,nameWidth+12),rows.size()*font.height()+9));
            }
            for(String row:rows) {
                fitted(g,font,row,x+25,top,size,WIDTH-x-55,stop.current()?Color.WHITE:stop.passed()?new Color(0x888888):INK);
                top+=font.height();
            }
            if(!stop.secondary().isBlank() && step>=65) fitted(g,font,stop.secondary(),x+25,top+2,8,WIDTH-x-55,new Color(0x777777));
        }
        g.setStroke(new BasicStroke(1));g.setColor(new Color(0xAAAAAA));g.draw(new Line2D.Double(32,legendTop-20,WIDTH-32,legendTop-20));
        for(int i=0;i<data.legend().size();i++) {
            var line=data.legend().get(i); double x0=34+(i%2)*235, y=legendTop+(i/2)*30;
            badge(g,font,line.color(),line.number(),x0,y,9);
            fitted(g,font,line.name(),x0+15,y+3,8,200,INK);
        }
        g.dispose(); return image;
    }
    private static List<String> wrap(String name, NetworkMapFont font, int max) {
        List<String> rows=new ArrayList<>(); String row="";
        for(String word:name.split(" ")) {String next=row.isEmpty()?word:row+" "+word; if(font.width(next)>max && !row.isEmpty()){rows.add(row);row=word;}else row=next;}
        rows.add(row); return rows;
    }
    private static void text(Graphics2D g,NetworkMapFont f,String s,double x,double y,int size,Color color){f.size(size);g.setColor(color);f.draw(g,s,x,y);}
    private static void fitted(Graphics2D g,NetworkMapFont f,String s,double x,double y,int size,int max,Color color){
        f.size(size); double scale=Math.min(1,max/(double)Math.max(1,f.width(s))); var old=g.getTransform();g.translate(x,y);g.scale(scale,scale);text(g,f,s,0,0,size,color);g.setTransform(old);
    }
    private static void badge(Graphics2D g,NetworkMapFont f,int color,String number,double x,double y,int r){
        g.setColor(new Color(color));g.fill(new Ellipse2D.Double(x-r,y-r,2*r,2*r)); f.size(r>12?16:8);
        fitted(g,f,number,x-f.width(number)/2.0,y+f.height()/3.0,r>12?16:8,r*2-4,Color.WHITE);
    }
}
