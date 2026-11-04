package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class VerticalLineAdvancedRegression {
    private static VerticalLineData.Stop stop(long id,String name,boolean current){return new VerticalLineData.Stop(name,"",current,id==1,List.of(),id,List.of());}
    public static void run(NetworkMapFont font)throws Exception{
        var main=List.of(stop(1,"Начало",false),stop(2,"Текущая",true),stop(3,"Развилка",false),stop(4,"Ветка А",false),stop(5,"Конечная А",false));
        var alternative=List.of(main.get(0),main.get(1),main.get(2),stop(6,"Ветка Б",false),stop(7,"Конечная Б",false));
        var reversed=new ArrayList<>(main);Collections.reverse(reversed);
        var paths=VerticalLineBranches.compatible(main,List.of(alternative,reversed,List.of(stop(9,"Чужая",false)),List.of(main.get(1),main.get(2),stop(8,"Объезд",false),main.get(4))));
        if(paths.size()!=1)throw new AssertionError("Reverse, disconnected, rejoining routes must not form a false branch");
        var afterFork=new ArrayList<>(main);afterFork.set(1,stop(2,"Узел",false));afterFork.set(3,stop(4,"Здесь на ветке",true));
        for(int i=0;i<3;i++){var s=afterFork.get(i);afterFork.set(i,new VerticalLineData.Stop(s.name(),s.secondary(),false,true,s.transfers(),s.stationId(),s.icons()));}
        if(VerticalLineBranches.compatible(afterFork,List.of(alternative)).size()!=1)throw new AssertionError("Alternative branch must remain visible after the current station left the common trunk");
        var layout=VerticalLineBranches.layout(main,paths);
        if(layout.points().size()!=7||layout.leaves()!=2||layout.points().stream().filter(p->p.stop().stationId()==3).count()!=1)throw new AssertionError("Common trunk must be drawn once");
        if(layout.points().stream().filter(p->p.stop().stationId()==6||p.stop().stationId()==7).anyMatch(p->p.stop().passed()))throw new AssertionError("Both directly reachable future branches must remain colored");
        var unreachable=alternative.stream().map(s->new VerticalLineData.Stop(s.name(),s.secondary(),false,true,s.transfers(),s.stationId(),s.icons())).toList();
        var grayLayout=VerticalLineBranches.layout(afterFork,VerticalLineBranches.compatible(afterFork,List.of(unreachable)));
        if(grayLayout.points().stream().filter(p->p.stop().stationId()==6||p.stop().stationId()==7).anyMatch(p->!p.stop().passed()))throw new AssertionError("Branch without a train from the current platform must stay gray");
        var model=new VerticalLineData(0x9556A1,"7","Конечная А",main,List.of(),"",false,0,paths);
        var image=VerticalLinePainter.paint(model,font);ImageIO.write(image,"PNG",new File("vertical-line-branches.png"));image.flush();
        var grayModel=new VerticalLineData(0x9556A1,"7","Конечная А",afterFork,List.of(),"",false,0,VerticalLineBranches.compatible(afterFork,List.of(unreachable)));
        var grayImage=VerticalLinePainter.paint(grayModel,font);ImageIO.write(grayImage,"PNG",new File("vertical-line-after-fork.png"));grayImage.flush();
        List<VerticalLineData.Stop> longStops=new ArrayList<>();
        for(int i=0;i<64;i++)longStops.add(new VerticalLineData.Stop("Very long station name with several words and a district number "+i,"",i==20,i<20,List.of(),i+1,List.of("test:arrow","test:pack")));
        Map<String,BufferedImage> icons=new HashMap<>();
        try(var input=VerticalLineAdvancedRegression.class.getResourceAsStream("/assets/mtr/textures/block/sign/arrow.png")){
            if(input==null)throw new AssertionError("Actual MTR sign resource missing");icons.put("test:arrow",VerticalLineIconImages.read(input,0xFF222222,false));
        }
        var asymmetric=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);asymmetric.setRGB(0,0,0xFFFF0000);asymmetric.setRGB(1,0,0);
        var png=new ByteArrayOutputStream();ImageIO.write(asymmetric,"PNG",png);
        // ZIP override exercises the same resource stream and decoder as a loaded resourcepack.
        var pack=new File("vertical-sign-test-pack.zip");
        try(var zip=new java.util.zip.ZipOutputStream(new FileOutputStream(pack))){zip.putNextEntry(new java.util.zip.ZipEntry("assets/test/textures/sign.png"));zip.write(png.toByteArray());zip.closeEntry();}
        try(var zip=new java.util.zip.ZipFile(pack);var input=zip.getInputStream(zip.getEntry("assets/test/textures/sign.png"))){
            var icon=VerticalLineIconImages.read(input,0xFF123456,true);
            if(icon.getRGB(0,16)!=0xFF123456 || icon.getRGB(31,16)!=0xFFFF0000)throw new AssertionError("Pack transparency, background or flip broken");icons.put("test:pack",icon);
        }
        var dense=new VerticalLineData(0x167AA3,"3","Terminal",longStops,List.of());
        var longImage=VerticalLinePainter.paint(dense,font,icons);if(longImage.getWidth()!=1024 || longImage.getHeight()!=2688)throw new AssertionError("Dense labels need a higher resolution bitmap");ImageIO.write(longImage,"PNG",new File("vertical-line-long-icons.png"));longImage.flush();icons.values().forEach(BufferedImage::flush);
        try{VerticalLineIconImages.read(new ByteArrayInputStream(new byte[]{1,2}),0,false);throw new AssertionError("Invalid image accepted");}catch(IOException expected){}
        System.out.println("PASS: shared MTR branch topology, reversed/disconnected/rejoining route rejection, 64 long stops, actual MTR PNG, ZIP override transparency/background/flip, invalid image");
    }
}
