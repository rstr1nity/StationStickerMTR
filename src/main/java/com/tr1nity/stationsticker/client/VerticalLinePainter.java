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
        return paint(data,font,Map.of());
    }
    public static BufferedImage paint(VerticalLineData data, NetworkMapFont font, Map<String,BufferedImage> icons) {
        int resolution=data!=null && data.allStops().size()>=40?2:1;
        BufferedImage image = new BufferedImage(WIDTH*resolution, HEIGHT*resolution, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.scale(resolution,resolution);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(PAPER); g.fillRect(0, 0, WIDTH, HEIGHT);
        font.density(1);
        if (data == null || data.stops().isEmpty()) { text(g,font,"MTR",40,100,24,INK); g.dispose(); return image; }
        badge(g,font,data.color(),data.number(),55,85,25);
        if (data.direction().isEmpty()) forwardArrow(g,WIDTH-54,84);
        else {
            int captionWidth=WIDTH-96-38;
            fitted(g,font,data.direction(),96,48,12,captionWidth,INK);
            if (data.circular() && data.rotation()!=0) {
                font.size(12);
                circularArrow(g,96+Math.min(captionWidth,font.width(data.direction()))+13,42,data.rotation());
            }
        }
        fitted(g,font,data.heading(),96,92,28,data.direction().isEmpty()?WIDTH-190:WIDTH-130,INK);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(0xAAAAAA)); g.drawLine(32,142,WIDTH-32,142);
        // Reserve a separate lower region for the transfer legend.
        int legendRows = (data.legend().size()+1)/2;
        double legendTop = HEIGHT-65-legendRows*30;
        double start = 190, end = legendTop-75;
        double step = (end-start)/Math.max(1,data.stops().size()-1);
        font.density(step<20?2:1);
        int fontSize = step < 32 ? 8 : step < 50 ? 12 : 16;
        var tree=VerticalLineBranches.layout(data.stops(),data.branches());
        if(!data.branches().isEmpty() && !tree.points().isEmpty()) {
            double centerOffset=WIDTH/2.0-190;
            double treeStep=(end-start)/Math.max(1,tree.depth());
            font.density(treeStep<20?2:1);
            var points=new HashMap<Long,VerticalLineBranches.Point>();tree.points().forEach(p->points.put(p.stop().stationId(),p));
            for(var point:tree.points())for(long child:tree.children().get(point.stop().stationId())){
                var target=points.get(child);double y0=start+point.depth()*treeStep,y1=start+target.depth()*treeStep;
                g.setColor(new Color(point.stop().passed() || target.stop().passed()?0xBABDBB:data.color()));g.setStroke(new BasicStroke(7,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));
                g.draw(branchPath(point.x()+centerOffset,y0,target.x()+centerOffset,y1));
            }
            for(var point:tree.points())drawStop(g,font,icons,data,point.stop(),(int)(point.x()+centerOffset),start+point.depth()*treeStep,treeStep,
                    tree.leaves()>1?Math.min(WIDTH-12,(int)(point.x()+centerOffset)+170):WIDTH,treeStep<32?8:12,point.first(),point.last());
        } else for(int i=0;i<data.stops().size();i++) {
            var stop=data.stops().get(i);double y=start+i*step;int x=WIDTH/2;
            if(i+1<data.stops().size()){g.setColor(new Color(stop.passed()?0xBABDBB:data.color()));g.setStroke(new BasicStroke(7,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Double(x,y,x,y+step));}
            drawStop(g,font,icons,data,stop,x,y,step,WIDTH,fontSize,i==0,i==data.stops().size()-1);

        }
        font.density(1);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(0xAAAAAA));g.draw(new Line2D.Double(32,legendTop-20,WIDTH-32,legendTop-20));
        int legendColumns=Math.min(2,data.legend().size());
        double[] columnWidths=new double[legendColumns];font.size(8);
        for(int i=0;i<data.legend().size();i++)columnWidths[i%legendColumns]=Math.max(columnWidths[i%legendColumns],24+Math.min(200,font.width(data.legend().get(i).name())));
        double legendWidth=java.util.Arrays.stream(columnWidths).sum()+Math.max(0,legendColumns-1)*24;
        double legendLeft=(WIDTH-legendWidth)/2;
        for(int i=0;i<data.legend().size();i++) {
            int column=i%legendColumns;double x0=legendLeft+9;
            for(int c=0;c<column;c++)x0+=columnWidths[c]+24;
            var line=data.legend().get(i);double y=legendTop+(i/legendColumns)*30;
            badge(g,font,line.color(),line.number(),x0,y,9);
            fitted(g,font,line.name(),x0+15,y+3,8,200,INK);
        }
        g.dispose(); return image;
    }
    private static void forwardArrow(Graphics2D g,int x,int y){
        g.setColor(INK);g.setStroke(new BasicStroke(7,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(x,y+22,x,y-20));
        var head=new Path2D.Double();head.moveTo(x-12,y-8);head.lineTo(x,y-22);head.lineTo(x+12,y-8);g.draw(head);
    }
    public static Path2D branchPath(double x0,double y0,double x1,double y1){
        var path=new Path2D.Double();path.moveTo(x0,y0);
        if(Math.abs(x1-x0)<.01){path.lineTo(x1,y1);return path;}
        double direction=Math.signum(x1-x0),run=Math.min(Math.abs(x1-x0),(y1-y0)*.45);
        double start=y0+Math.min(10,(y1-y0)*.1),end=start+run,r=Math.min(6,run/4);
        path.lineTo(x0,start-r);path.quadTo(x0,start,x0+direction*r,start+r);
        path.lineTo(x1-direction*r,end-r);path.quadTo(x1,end,x1,end+r);path.lineTo(x1,y1);return path;
    }
    private static void drawStop(Graphics2D g,NetworkMapFont font,Map<String,BufferedImage> icons,VerticalLineData data,VerticalLineData.Stop stop,int x,double y,double step,int right,int fontSize,boolean first,boolean last){
            Color line = new Color(stop.passed()?0xBABDBB:data.color());
            int arc=0;
            for (var transfer:stop.transfers()) if(transfer.circular()) {
                g.setColor(new Color(transfer.color())); g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new QuadCurve2D.Double(20,y-27-arc*10,WIDTH/2.0,y+27-arc*10,right-20,y-27-arc*10)); arc++;
            }
            g.setColor(line); g.setStroke(new BasicStroke(5));
            boolean stationRing=stop.current() || stop.transfers().stream().anyMatch(t -> !t.walking()) || data.circular() && (first || last);
            if(stationRing) {
                g.setColor(PAPER); g.fill(new Ellipse2D.Double(x-9,y-9,18,18));
                g.setColor(line); g.draw(new Ellipse2D.Double(x-9,y-9,18,18));
            } else g.draw(new Line2D.Double(x,y,x+13,y));
            if(!data.circular() && !stationRing && (first || last)) g.draw(new Line2D.Double(x-10,y,x+10,y));
            double units=stop.transfers().stream().mapToInt(t -> t.walking() ? 38 : 23).sum();
            double compression=Math.min(1,120.0/Math.max(1,units));
            double bx=x, previousRadius=9;
            for(var transfer:stop.transfers()) {
                double previous=bx;
                bx-=(transfer.walking()?38:23)*compression;
                int radius=Math.max(3,(int)Math.min(9,10*compression));
                if(transfer.walking()) {
                    g.setColor(new Color(0x999999));
                    g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{3,3},0));
                    g.draw(new Line2D.Double(bx+radius+2,y,previous-previousRadius-2,y));
                }
                badge(g,font,transfer.color(),transfer.number(),bx,y,radius);
                previousRadius=radius;
            }
            String name=stop.name(); int size=fontSize;
            font.size(size);
            var stopIcons=stop.icons().stream().filter(id -> !id.isBlank()).limit(3).toList();
            int iconSize=Math.max(10,fontSize), iconWidth=stopIcons.size()*(iconSize+5);
            int textWidth=right-x-55-iconWidth;
            List<String> rows=wrap(name,font,textWidth);
            while(rows.size()>2 && size>8) {size-=4; font.size(size); rows=wrap(name,font,textWidth);}
            if(rows.size()*font.height()+9>step)rows=List.of(name);
            double top=y-(rows.size()*font.height())/2.0+font.height()-3;
            if(!stop.current()) {
                font.size(size);
                int width=rows.stream().mapToInt(font::width).max().orElse(0);
                g.setColor(PAPER); g.fill(new Rectangle2D.Double(x+20,top-font.height()+1,Math.min(textWidth+12,width+12),rows.size()*font.height()+5));
            }
            if(stop.current()) {
                font.size(size);
                int nameWidth=rows.stream().mapToInt(font::width).max().orElse(0);
                g.setColor(new Color(data.color()));g.fill(new Rectangle2D.Double(x+19,top-font.height()+1,Math.min(textWidth+12,nameWidth+12),rows.size()*font.height()+9));
            }
            double iconY=top-font.height()+3;
            font.size(size);
            double iconX=x+30+Math.min(textWidth,rows.stream().mapToInt(font::width).max().orElse(0));
            for(String id:stopIcons){var icon=icons.get(id);if(icon!=null)g.drawImage(icon,(int)iconX,(int)iconY,iconSize,iconSize,null);iconX+=iconSize+5;}
            for(String row:rows) {
                fitted(g,font,row,x+25,top,size,textWidth,stop.current()?Color.WHITE:stop.passed()?new Color(0x888888):INK);
                top+=font.height();
            }
            if(!stop.secondary().isBlank() && step>=65) fitted(g,font,stop.secondary(),x+25,top+2,8,right-x-55,new Color(0x777777));
    }
    private static void circularArrow(Graphics2D g, double x, double y, int direction) {
        double radius=7, angle=0;
        var path=new Path2D.Double();
        for(int i=0;i<=40;i++){
            angle=Math.toRadians(direction>0 ? -235 : 415)+direction*i/40.0*Math.toRadians(280);
            double px=x+Math.cos(angle)*radius,py=y+Math.sin(angle)*radius;
            if(i==0)path.moveTo(px,py);else path.lineTo(px,py);
        }
        g.setColor(INK);g.setStroke(new BasicStroke(2.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(path);
        double px=x+Math.cos(angle)*radius,py=y+Math.sin(angle)*radius;
        double tx=-Math.sin(angle)*direction,ty=Math.cos(angle)*direction;
        var tip=new Path2D.Double();tip.moveTo(px+tx*4,py+ty*4);
        tip.lineTo(px-tx*3-ty*4,py-ty*3+tx*4);tip.lineTo(px-tx*3+ty*4,py-ty*3-tx*4);tip.closePath();g.fill(tip);
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
