package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.client.*;
import org.mtr.core.data.*;
import java.util.*;
import com.google.gson.*;

public final class NavigationConsistencyRegression {
    private static SimplifiedRoute route(long id,long[] platforms,long[] stations){
        var json=new JsonObject();json.addProperty("id",id);json.addProperty("name","Line");json.addProperty("color",0xDD072B);json.addProperty("circularState","NONE");
        var route=new SimplifiedRoute(new org.mtr.core.serializer.JsonReader(org.mtr.libraries.com.google.gson.JsonParser.parseString(json.toString())));
        for(int i=0;i<platforms.length;i++)route.getPlatforms().add(new SimplifiedRoutePlatform(platforms[i],stations[i],"Station "+stations[i],""));return route;
    }
    public static void run(NetworkMapFont font)throws Exception{
        var a=route(1,new long[]{11,12,14},new long[]{1,2,4});var b=route(2,new long[]{11,13,14},new long[]{1,3,4});
        var reverse=route(3,new long[]{22,21},new long[]{2,1});var end=route(4,new long[]{25,11},new long[]{5,1});var routes=List.of(a,b,reverse,end);
        if(NavigationRoutes.atPlatform(routes,11).size()!=3||NavigationRoutes.destinations(routes,11).size()!=1||NavigationRoutes.destinations(routes,11).values().iterator().next().size()!=2)throw new AssertionError("Same-platform services, arrival-only routes or shared terminal grouping disagree");
        if(NavigationRoutes.stationPassed(routes,11,2)||NavigationRoutes.stationPassed(routes,11,3)||!NavigationRoutes.stationPassed(routes,12,1)||!NavigationRoutes.stationPassed(routes,12,3))throw new AssertionError("Passed/reachable state must use the same platform across all boards");
        var badges=com.tr1nity.stationsticker.client.display.RouteNumbers.badges(List.of(a,b,a),r->r.getId()==1?"1":"12");
        if(badges.size()!=2 || !badges.get(0).number().equals("1") || !badges.get(1).number().equals("12"))throw new AssertionError("Shared destination must show both route numbers once");
        var same=com.tr1nity.stationsticker.client.display.RouteNumbers.badges(List.of(a,b),r->"1");
        var unknown=com.tr1nity.stationsticker.client.display.RouteNumbers.badges(List.of(a,b),r->r.getId()==1?"1":"");
        if(same.size()!=1 || unknown.size()!=2 || !unknown.get(1).number().isEmpty())throw new AssertionError("Repeated numbers must deduplicate; unknown service must not borrow another branch number");
        var other=route(5,new long[]{11,15},new long[]{1,5});var multiple=new ArrayList<>(routes);multiple.add(other);
        if(NavigationRoutes.destinations(multiple,11).size()!=2)throw new AssertionError("Distinct destinations must remain distinct");
        var data=new ClientData();var station=new Station(data);station.setCorners(new Position(-10,-10,-10),new Position(10,10,10));data.stations.add(station);
        var p=new Platform(new Position(0,0,0),new Position(0,0,5),TransportMode.TRAIN,data);data.platforms.add(p);data.sync();
        var cloned=new Station(new org.mtr.core.serializer.JsonReader(org.mtr.core.tool.Utilities.getJsonObjectFromData(station)),data);
        if(PlatformLocator.findPlatformIn(List.of(p),cloned,new net.minecraft.core.BlockPos(0,0,0))!=p)throw new AssertionError("Station identity must be matched by ID after an MTR refresh");
        liveMap(font);
        System.out.println("PASS: shared-platform services, multiple destinations, passed/reachable consistency, duplicate station objects by ID, live split/rejoin snapshot");
    }
    private static void liveMap(NetworkMapFont font)throws Exception{
        var file=java.nio.file.Path.of("current-fork-live.json");if(!java.nio.file.Files.exists(file))return;
        var root=JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject().getAsJsonObject("data");
        Map<Long,String> names=new HashMap<>();for(var item:root.getAsJsonArray("stations")){var o=item.getAsJsonObject();names.put(Long.parseUnsignedLong(o.get("id").getAsString(),16),o.get("name").getAsString());}
        List<List<VerticalLineData.Stop>> red=new ArrayList<>();Map<Long,double[]> coords=new HashMap<>();Map<String,com.tr1nity.stationsticker.map.NetworkMapData.Edge> edges=new TreeMap<>();Map<Integer,com.tr1nity.stationsticker.map.NetworkMapData.Line> lines=new TreeMap<>();
        for(var item:root.getAsJsonArray("routes")){
            var r=item.getAsJsonObject();if(r.get("hidden").getAsBoolean())continue;int color=r.get("color").getAsInt();var stops=r.getAsJsonArray("stations");List<VerticalLineData.Stop> rows=new ArrayList<>();long before=0;
            for(var node:stops){var o=node.getAsJsonObject();long id=Long.parseUnsignedLong(o.get("id").getAsString(),16);String name=names.get(id);var sum=coords.computeIfAbsent(id,k->new double[3]);sum[0]+=o.get("x").getAsDouble();sum[1]+=o.get("z").getAsDouble();sum[2]++;
                rows.add(new VerticalLineData.Stop(NetworkMapPainter.name(name,true),"",rows.isEmpty(),false,List.of(),id,List.of()));
                if(before!=0){long a=Math.min(before,id),b=Math.max(before,id);edges.put(a+":"+b+":"+color,new com.tr1nity.stationsticker.map.NetworkMapData.Edge(a,b,color));}before=id;
            }
            lines.putIfAbsent(color,new com.tr1nity.stationsticker.map.NetworkMapData.Line(color,r.get("number").getAsString(),r.get("name").getAsString()));
            if(r.get("name").getAsString().contains("IN")&&color==0xDD072B)red.add(rows);
        }
        var stations=coords.entrySet().stream().map(e->new com.tr1nity.stationsticker.map.NetworkMapData.Station(e.getKey(),names.get(e.getKey()),e.getValue()[0]/e.getValue()[2],e.getValue()[1]/e.getValue()[2])).toList();
        var network=new com.tr1nity.stationsticker.map.NetworkMapData(stations,new ArrayList<>(edges.values()),new ArrayList<>(lines.values()));
        var map=NetworkMapPainter.paint(network,stations.get(0).id(),true,font);javax.imageio.ImageIO.write(map,"PNG",new java.io.File("network-map-live-rejoin.png"));map.flush();
        if(red.size()>=2){red.sort(Comparator.comparingInt(List::size));var main=red.get(0);var paths=VerticalLineBranches.compatible(main,red.subList(1,red.size()));var dag=VerticalLineBranches.layout(main,paths);
            if(paths.isEmpty()||dag.points().size()!=5)throw new AssertionError("User's Zavod–Avtovo / Avt–abvgde–terminal rejoin failed");
            var model=new VerticalLineData(0xDD072B,"1",main.get(main.size()-1).name(),main,List.of(),"",false,0,paths);var image=VerticalLinePainter.paint(model,font);javax.imageio.ImageIO.write(image,"PNG",new java.io.File("vertical-line-live-rejoin.png"));image.flush();
        }
    }
}
