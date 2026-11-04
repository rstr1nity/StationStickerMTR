package com.tr1nity.stationsticker.block;

import net.minecraft.nbt.*;
import java.util.*;

public record VerticalLineSettings(long routeId, Map<Long,List<String>> icons) {
    public static final VerticalLineSettings DEFAULT=new VerticalLineSettings(0,Map.of());
    public VerticalLineSettings {
        Map<Long,List<String>> copy=new TreeMap<>();
        icons.entrySet().stream().limit(128).forEach(e->copy.put(e.getKey(),e.getValue().stream().limit(3).map(s->s.substring(0,Math.min(128,s.length()))).toList()));
        icons=Collections.unmodifiableMap(copy);
    }
    public CompoundTag save(){
        var tag=new CompoundTag();tag.putLong("RouteId",routeId);var rows=new ListTag();
        icons.forEach((station,values)->{var row=new CompoundTag();row.putLong("Station",station);var list=new ListTag();values.forEach(id->list.add(StringTag.valueOf(id)));row.put("Icons",list);rows.add(row);});tag.put("StationIcons",rows);return tag;
    }
    public static VerticalLineSettings load(CompoundTag tag){
        Map<Long,List<String>> icons=new TreeMap<>();var rows=tag.getList("StationIcons",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(128,rows.size());i++){var row=rows.getCompound(i);var list=row.getList("Icons",Tag.TAG_STRING);List<String> values=new ArrayList<>();for(int j=0;j<Math.min(3,list.size());j++)values.add(list.getString(j));icons.put(row.getLong("Station"),values);}
        return new VerticalLineSettings(tag.getLong("RouteId"),icons);
    }
}
