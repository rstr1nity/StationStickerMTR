package com.tr1nity.stationsticker.client;

import java.util.*;

/** A directed acyclic graph of real ordered MTR stops. Supports ordered branch/rejoin paths; rejects reverse cycles. */
public final class VerticalLineBranches {
    public record Point(VerticalLineData.Stop stop,double x,int depth,int column,boolean first,boolean last){}
    public record Layout(List<Point> points,Map<Long,List<Long>> children,int depth,int leaves){}
    private VerticalLineBranches(){}
    public static List<List<VerticalLineData.Stop>> compatible(List<VerticalLineData.Stop> main,List<List<VerticalLineData.Stop>> candidates){
        var result=new ArrayList<List<VerticalLineData.Stop>>();
        if(main.stream().map(VerticalLineData.Stop::stationId).distinct().count()!=main.size())return List.of();
        for(var path:candidates){
            if(path.isEmpty()||main.isEmpty())continue;
            int current=-1,start=-1;
            for(int i=0;i<main.size() && start<0;i++)for(int j=0;j<path.size();j++)if(path.get(j).stationId()==main.get(i).stationId()){current=i;start=j;break;}
            if(start<0)continue;
            int shared=0;while(start+shared<path.size()&&current+shared<main.size()&&path.get(start+shared).stationId()==main.get(current+shared).stationId())shared++;
            if(shared==0||start+shared==path.size()||current+shared==main.size())continue;
            Set<Long> selected=new HashSet<>();main.forEach(s->selected.add(s.stationId()));
            boolean reversed=false;int last=current+shared-1;
            for(var stop:path.subList(start+shared,path.size()))if(selected.contains(stop.stationId())){
                int index=0;while(main.get(index).stationId()!=stop.stationId())index++;
                if(index<=last){reversed=true;break;}last=index;
            }
            if(reversed)continue;
            var candidate=List.copyOf(path.subList(start,path.size()));
            if(!result.contains(candidate))result.add(candidate);
        }
        return List.copyOf(result);
    }
    public static Layout layout(List<VerticalLineData.Stop> main,List<List<VerticalLineData.Stop>> paths){
        Map<Long,VerticalLineData.Stop> stops=new LinkedHashMap<>();Map<Long,List<Long>> children=new LinkedHashMap<>();Map<Long,Set<Integer>> lanes=new HashMap<>();
        List<List<VerticalLineData.Stop>> all=new ArrayList<>();all.add(main);all.addAll(paths);
        for(int lane=0;lane<all.size();lane++)for(var stop:all.get(lane)){
            var previous=stops.get(stop.stationId());
            if(previous==null || previous.passed() && !stop.passed())stops.put(stop.stationId(),stop);
            children.computeIfAbsent(stop.stationId(),id->new ArrayList<>());lanes.computeIfAbsent(stop.stationId(),id->new HashSet<>()).add(lane);
        }
        for(var path:all)for(int i=1;i<path.size();i++){long a=path.get(i-1).stationId(),b=path.get(i).stationId();if(!children.get(a).contains(b))children.get(a).add(b);}
        Map<Long,Integer> incoming=new LinkedHashMap<>(),depth=new HashMap<>();stops.keySet().forEach(id->incoming.put(id,0));
        children.values().forEach(ids->ids.forEach(id->incoming.put(id,incoming.get(id)+1)));
        ArrayDeque<Long> ready=new ArrayDeque<>();incoming.forEach((id,count)->{if(count==0){ready.add(id);depth.put(id,0);}});
        List<Point> points=new ArrayList<>();int maxDepth=0,leaves=0;
        while(!ready.isEmpty()){
            long id=ready.remove();var memberships=lanes.get(id);double column=memberships.stream().mapToInt(Integer::intValue).average().orElse(0);
            double x=memberships.size()>1?190:90+column*200/Math.max(1,all.size()-1);
            int d=depth.get(id);maxDepth=Math.max(maxDepth,d);if(children.get(id).isEmpty())leaves++;
            points.add(new Point(stops.get(id),x,d,(int)column,d==0,children.get(id).isEmpty()));
            for(long next:children.get(id)){depth.merge(next,d+1,Math::max);incoming.put(next,incoming.get(next)-1);if(incoming.get(next)==0)ready.add(next);}
        }
        if(points.size()!=stops.size())return new Layout(List.of(),Map.of(),0,0);
        return new Layout(points,children,maxDepth,Math.max(all.size(),leaves));
    }
}
