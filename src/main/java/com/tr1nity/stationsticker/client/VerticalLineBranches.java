package com.tr1nity.stationsticker.client;

import java.util.*;

/** A directed tree of real ordered MTR stops. Rejects cycles and ambiguous rejoining paths. */
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
            if(path.subList(start+shared,path.size()).stream().anyMatch(s->selected.contains(s.stationId())))continue;
            var candidate=List.copyOf(path.subList(start,path.size()));
            if(!result.contains(candidate))result.add(candidate);
        }
        return List.copyOf(result);
    }
    public static Layout layout(List<VerticalLineData.Stop> main,List<List<VerticalLineData.Stop>> paths){
        Map<Long,VerticalLineData.Stop> stops=new LinkedHashMap<>();Map<Long,List<Long>> children=new LinkedHashMap<>();Map<Long,Long> parent=new HashMap<>();
        for(var stop:main){stops.put(stop.stationId(),stop);children.put(stop.stationId(),new ArrayList<>());}
        for(int i=1;i<main.size();i++){long a=main.get(i-1).stationId(),b=main.get(i).stationId();if(parent.put(b,a)!=null)return new Layout(List.of(),Map.of(),0,0);children.get(a).add(b);}
        Set<Long> mainIds=new HashSet<>();main.forEach(s->mainIds.add(s.stationId()));
        for(var path:paths){
            boolean valid=true;var seen=new HashSet<Long>();
            for(int i=0;i<path.size();i++){long id=path.get(i).stationId();if(!seen.add(id)){valid=false;break;}if(i>0&&parent.containsKey(id)&&parent.get(id)!=path.get(i-1).stationId()){valid=false;break;}}
            if(!valid)continue;
            for(int i=1;i<path.size();i++){var stop=path.get(i);long from=path.get(i-1).stationId();if(!children.containsKey(from))break;
                if(!stops.containsKey(stop.stationId())){stops.put(stop.stationId(),new VerticalLineData.Stop(stop.name(),stop.secondary(),false,stop.passed(),stop.transfers(),stop.stationId(),stop.icons()));children.put(stop.stationId(),new ArrayList<>());}
                if(!mainIds.contains(stop.stationId()) && stops.get(stop.stationId()).passed() && !stop.passed())stops.put(stop.stationId(),stop);
                if(!children.get(from).contains(stop.stationId())){children.get(from).add(stop.stationId());parent.put(stop.stationId(),from);}
            }
        }
        if(main.isEmpty())return new Layout(List.of(),Map.of(),0,0);
        List<Point> points=new ArrayList<>();int[] leaf={0};walk(main.get(0).stationId(),0,stops,children,points,leaf);
        int leaves=leaf[0];List<Point> positioned=points.stream().map(p->new Point(p.stop(),70+p.x()*360/Math.max(1,leaves),p.depth(),p.column(),p.first(),p.last())).toList();
        return new Layout(positioned,children,points.stream().mapToInt(Point::depth).max().orElse(0),leaves);
    }
    private static double walk(long id,int depth,Map<Long,VerticalLineData.Stop> stops,Map<Long,List<Long>> children,List<Point> points,int[] leaf){
        var next=children.get(id);double x;int column=leaf[0];
        if(next.isEmpty())x=leaf[0]+++.5;
        else{double total=0;for(long child:next)total+=walk(child,depth+1,stops,children,points,leaf);x=total/next.size();}
        points.add(new Point(stops.get(id),x,depth,column,depth==0,next.isEmpty()));return x;
    }
}
