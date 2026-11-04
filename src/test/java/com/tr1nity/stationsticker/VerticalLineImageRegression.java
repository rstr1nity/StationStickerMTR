package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;

public final class VerticalLineImageRegression {
    public static void main(String[] args) throws Exception {
        var ring=new VerticalLineData.Transfer(0x8B5B41,"5","Кольцевая линия",true);
        var blue=new VerticalLineData.Transfer(0x167AA3,"3","Арбатско-Покровская линия",false,true);
        var main=new VerticalLineData.Transfer(0x9556A1,"7","Таганско-Краснопресненская",false);
        String[] names={"Пушкинская","Кузнецкий Мост","Китай-город","Таганская","Пролетарская","Волгоградский проспект","Текстильщики","Кузьминки","Рязанский проспект","Выхино","Лермонтовский проспект","Жулебино"};
        List<VerticalLineData.Stop> stops=new ArrayList<>();
        for(int i=0;i<names.length;i++)stops.add(new VerticalLineData.Stop(names[i],"",i==3,i<3,i==3?List.of(ring):i==1?List.of(blue):List.of()));
        var data=new VerticalLineData(main.color(),main.number(),"Котельники",List.copyOf(stops),List.of(main,ring,blue));
        Set<Integer> needed=new HashSet<>();("По часовой стрелке↑MTRКотельники"+Arrays.toString(names)+ring.name()+blue.name()+main.name()+"0123456789").codePoints().forEach(needed::add);
        var font=NetworkMapFont.load(id->{var path=id.split(":",2);var input=VerticalLineImageRegression.class.getResourceAsStream("/assets/"+path[0]+"/"+path[1]);if(input==null)throw new IOException(id);return input;},needed);
        var image=VerticalLinePainter.paint(data,font);
        if(image.getWidth()!=512||image.getHeight()!=1344)throw new AssertionError("Wrong panel aspect");
        // Ring transfer must extend to both sides of the main line.
        double y=190+3*((1344-65-2*30-75-190)/11.0);
        boolean left=false,right=false;
        for(int py=(int)y-40;py<y+40;py++)for(int x=20;x<492;x++)if((image.getRGB(x,py)&0xFFFFFF)==ring.color()){if(x<100)left=true;if(x>350)right=true;}
        if(!left||!right)throw new AssertionError("Ring arc missing on a side");
        int transferY=(int)Math.round(190+(1344-65-60-75-190)/11.0);
        int dashes=0;
        for(int y0=transferY-2;y0<=transferY+2;y0++)for(int x0=134;x0<149;x0++){
            int pixel=image.getRGB(x0,y0)&0xFFFFFF;
            int r=(pixel>>16)&255,g0=(pixel>>8)&255,b=pixel&255;
            if(Math.abs(r-g0)<3 && Math.abs(g0-b)<3 && r<210)dashes++;
        }
        if(dashes==0)throw new AssertionError("Walking interchange has no gray dashed connector");
        ImageIO.write(image,"PNG",new File("vertical-line-preview.png"));image.flush();
        // Initial current station is a ring, never a terminus bar across its centre.
        var firstData=new VerticalLineData(data.color(),data.number(),data.heading(),List.of(new VerticalLineData.Stop("Начало","",true,false,List.of()), new VerticalLineData.Stop("Конец","",false,false,List.of())),List.of());
        var firstImage=VerticalLinePainter.paint(firstData,font);
        if((firstImage.getRGB(160,190)&0xFFFFFF)!=0xFAF8F1)throw new AssertionError("Initial station circle crossed by terminus bar");firstImage.flush();
        var ringRows=VerticalLineData.ringStops(stops,3);
        if(!ringRows.get(0).current() || ringRows.stream().anyMatch(VerticalLineData.Stop::passed) || ringRows.size()!=stops.size()) throw new AssertionError("Ring must begin at current stop without passed stations");
        var circular=new VerticalLineData(main.color(),"7","Таганская",ringRows,List.of(main,ring),"По часовой стрелке",true,1);
        var ringImage=VerticalLinePainter.paint(circular,font);
        if ((ringImage.getRGB(160, 1344-65-30-75)&0xFFFFFF)!=0xFAF8F1) throw new AssertionError("Ring end has a terminus bar through its circle");
        font.size(12);
        int arrowX=96+Math.min(512-96-38,font.width(circular.direction()))+13;
        int arrowInk=0;
        for(int py=30;py<56;py++)for(int px=arrowX-12;px<=arrowX+12;px++)if((ringImage.getRGB(px,py)&0xFFFFFF)==0x25272B)arrowInk++;
        if(arrowInk<15)throw new AssertionError("Circular direction arrow must follow the caption on its right");
        ImageIO.write(ringImage,"PNG",new File("vertical-line-ring-preview.png"));ringImage.flush();
        var reverse=new VerticalLineData(main.color(),"7","Таганская",ringRows,List.of(main,ring),"Против часовой стрелки",true,-1);
        var reverseImage=VerticalLinePainter.paint(reverse,font);ImageIO.write(reverseImage,"PNG",new File("vertical-line-ring-counterclockwise.png"));reverseImage.flush();
        var icon=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var ig=icon.createGraphics();ig.setColor(java.awt.Color.RED);ig.fillRect(0,0,16,16);ig.dispose();
        var iconRows=new ArrayList<>(stops);var original=iconRows.get(3);
        iconRows.set(3,new VerticalLineData.Stop(original.name(),original.secondary(),true,false,original.transfers(),42,List.of("test:train")));
        var iconData=new VerticalLineData(data.color(),data.number(),data.heading(),iconRows,data.legend());
        var iconImage=VerticalLinePainter.paint(iconData,font,Map.of("test:train",icon));
        int redPixels=0;for(int py=430;py<470;py++)for(int px=180;px<512;px++)if((iconImage.getRGB(px,py)&0xFFFFFF)==0xFF0000)redPixels++;
        if(redPixels==0)throw new AssertionError("Station icon was not rendered beside name");
        ImageIO.write(iconImage,"PNG",new File("vertical-line-station-icons.png"));iconImage.flush();icon.flush();
        var empty=VerticalLinePainter.paint(null,font);empty.flush();
        var network=new com.tr1nity.stationsticker.map.NetworkMapData(List.of(),List.of(
                new com.tr1nity.stationsticker.map.NetworkMapData.Edge(1,2,-1),
                new com.tr1nity.stationsticker.map.NetworkMapData.Edge(2,3,blue.color())),List.of());
        if(!VerticalLineData.walking(network,1,blue.color()) || VerticalLineData.walking(network,2,blue.color()) || VerticalLineData.walking(network,9,blue.color()))throw new AssertionError("Walking links must follow explicit MTR edges, with direct transfer priority");
        var directEdges=new ArrayList<>(network.edges());
        directEdges.add(new com.tr1nity.stationsticker.map.NetworkMapData.Edge(1,4,blue.color()));
        if(VerticalLineData.walking(new com.tr1nity.stationsticker.map.NetworkMapData(List.of(),directEdges,List.of()),1,blue.color())) throw new AssertionError("Direct interchange must override walking interchange of the same color");
        savedWorld();
        System.out.println("PASS: vertical panel, real Minecraft font, ring arc across the station, empty state");
    }

    private static void savedWorld() throws Exception {
        var root=java.nio.file.Path.of("../run-1.20.1/saves/Rossonezh/mtr/minecraft/overworld");
        if(!java.nio.file.Files.isDirectory(root))return;
        var data=new org.mtr.core.data.ClientData();
        read(root.resolve("stations"), r->data.stations.add(new org.mtr.core.data.Station(r,data)));
        read(root.resolve("platforms"), r->data.platforms.add(new org.mtr.core.data.Platform(r,data)));
        read(root.resolve("routes"), r->data.routes.add(new org.mtr.core.data.Route(r,data)));
        data.sync();
        var route=data.routes.stream().filter(r->!r.getHidden() && r.getRoutePlatforms().size()>=15 && r.getRoutePlatforms().size()<=35)
                .max(java.util.Comparator.comparingInt(r->r.getRoutePlatforms().size())).orElseThrow();
        List<VerticalLineData.Stop> rows=new ArrayList<>();var legend=new TreeMap<Integer,VerticalLineData.Transfer>();
        for(var stop:route.getRoutePlatforms()){
            var platform=stop.getPlatform();if(platform==null||platform.area==null)continue;
            var station=platform.area;
            var transfers=new TreeMap<Integer,VerticalLineData.Transfer>();
            for(var other:data.routes)if(!other.getHidden() && other.getColor()!=route.getColor() && other.getRoutePlatforms().stream().anyMatch(p->p.getPlatform()!=null && p.getPlatform().area!=null && (p.getPlatform().area==station || station.connectedStations.contains(p.getPlatform().area)))){
                var transfer=new VerticalLineData.Transfer(other.getColor(),other.getRouteNumber(),NetworkMapPainter.name(other.getName(),true),other.getCircularState()!=org.mtr.core.data.Route.CircularState.NONE,
                        other.getRoutePlatforms().stream().noneMatch(p->p.getPlatform()!=null && p.getPlatform().area==station));
                transfers.put(other.getColor(),transfer);legend.put(other.getColor(),transfer);
            }
            rows.add(new VerticalLineData.Stop(NetworkMapPainter.name(station.getName(),true),"",rows.size()==4,rows.size()<4,List.copyOf(transfers.values())));
        }
        var model=new VerticalLineData(route.getColor(),route.getRouteNumber(),rows.get(rows.size()-1).name(),rows,List.copyOf(legend.values()));
        Set<Integer> needed=new HashSet<>();("MTR↑0123456789"+model.heading()+model.number()).codePoints().forEach(needed::add);rows.forEach(r->r.name().codePoints().forEach(needed::add));legend.values().forEach(t->(t.name()+t.number()).codePoints().forEach(needed::add));
        var font=NetworkMapFont.load(id->{var path=id.split(":",2);return VerticalLineImageRegression.class.getResourceAsStream("/assets/"+path[0]+"/"+path[1]);},needed);
        var image=VerticalLinePainter.paint(model,font);ImageIO.write(image,"PNG",new File("vertical-line-rossonezh.png"));image.flush();
        System.out.println("PASS: saved Rossonezh route: "+rows.size()+" stops, "+legend.size()+" transfer lines");
    }
    private static void read(java.nio.file.Path root,java.util.function.Consumer<org.mtr.core.serializer.ReaderBase> action)throws Exception{
        try(var files=java.nio.file.Files.walk(root)){for(var file:files.filter(java.nio.file.Files::isRegularFile).filter(p->!p.getFileName().toString().startsWith(".")).toList())try(var input=org.mtr.libraries.org.msgpack.core.MessagePack.newDefaultUnpacker(java.nio.file.Files.readAllBytes(file))){action.accept(new org.mtr.core.serializer.MessagePackReader(input));}}
    }
}
