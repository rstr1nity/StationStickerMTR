package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;

public final class VerticalLineImageRegression {
    public static void main(String[] args) throws Exception {
        var ring=new VerticalLineData.Transfer(0x8B5B41,"5","Кольцевая линия",true);
        var blue=new VerticalLineData.Transfer(0x167AA3,"3","Арбатско-Покровская линия",false);
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
        ImageIO.write(image,"PNG",new File("vertical-line-preview.png"));image.flush();
        var ringRows=VerticalLineData.ringStops(stops,3);
        if(!ringRows.get(0).current() || ringRows.stream().anyMatch(VerticalLineData.Stop::passed) || ringRows.size()!=stops.size()) throw new AssertionError("Ring must begin at current stop without passed stations");
        var circular=new VerticalLineData(main.color(),"7","Таганская",ringRows,List.of(main,ring),"По часовой стрелке",true);
        var ringImage=VerticalLinePainter.paint(circular,font);
        if ((ringImage.getRGB(160, 1344-65-30-75)&0xFFFFFF)!=0xFAF8F1) throw new AssertionError("Ring end has a terminus bar through its circle");
        ImageIO.write(ringImage,"PNG",new File("vertical-line-ring-preview.png"));ringImage.flush();
        var empty=VerticalLinePainter.paint(null,font);empty.flush();
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
                var transfer=new VerticalLineData.Transfer(other.getColor(),other.getRouteNumber(),NetworkMapPainter.name(other.getName(),true),other.getCircularState()!=org.mtr.core.data.Route.CircularState.NONE);
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
