package com.tr1nity.stationsticker.client;

import org.mtr.core.data.SimplifiedRoute;
import org.mtr.mod.client.MinecraftClientData;
import java.util.*;

/** Shared ordered services and stop status for navigation displays. */
public final class NavigationRoutes {
    private NavigationRoutes(){}
    public static List<SimplifiedRoute> atPlatform(long platform){
        return atPlatform(new ArrayList<>(MinecraftClientData.getInstance().simplifiedRoutes),platform);
    }
    public static List<SimplifiedRoute> atPlatform(List<SimplifiedRoute> routes,long platform){return routes.stream().filter(r->r.getPlatformIndex(platform)>=0).sorted(Comparator.comparingLong(SimplifiedRoute::getId)).toList();}
    public static Map<String,List<SimplifiedRoute>> destinations(List<SimplifiedRoute> routes,long platform){
        Map<String,List<SimplifiedRoute>> groups=new LinkedHashMap<>();
        for(var route:atPlatform(routes,platform))if(boarding(route,route.getPlatformIndex(platform))){var stops=route.getPlatforms();String key=route.getColor()+":"+route.getCircularState()+":"+stops.get(stops.size()-1).getStationId();groups.computeIfAbsent(key,k->new ArrayList<>()).add(route);}return groups;
    }
    public static boolean stationPassed(List<SimplifiedRoute> routes,long platform,long station){
        for(var route:atPlatform(routes,platform)){int current=route.getPlatformIndex(platform);if(!boarding(route,current))continue;
            for(int i=0;i<route.getPlatforms().size();i++)if(route.getPlatforms().get(i).getStationId()==station && (i>=current || route.getCircularState()!=org.mtr.core.data.Route.CircularState.NONE))return false;
        }return true;
    }
    public static boolean boarding(SimplifiedRoute route,int index){return index>=0 && (index+1<route.getPlatforms().size()||route.getCircularState()!=org.mtr.core.data.Route.CircularState.NONE);}
    public static boolean passed(int index,int current){return index<current;}
    public static List<SimplifiedRoute> boardingAt(long platform){return atPlatform(platform).stream().filter(r->boarding(r,r.getPlatformIndex(platform))).toList();}
    public static String signature(long platform){var out=new StringBuilder(Long.toString(platform));for(var r:atPlatform(platform)){out.append(r.getId()).append(':').append(r.getColor()).append(':').append(r.getName()).append(':').append(r.getCircularState());for(var s:r.getPlatforms())out.append('/').append(s.getPlatformId()).append(':').append(s.getStationId()).append(':').append(s.getStationName());}return out.toString();}
}
