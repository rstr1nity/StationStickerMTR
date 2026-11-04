package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.VerticalLineSettings;
import com.tr1nity.stationsticker.block.entity.VerticalLineBlockEntity;
import com.tr1nity.stationsticker.client.display.*;
import com.tr1nity.stationsticker.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class VerticalLineScreen extends Screen {
    private final VerticalLineBlockEntity entity;
    private final Map<Long,List<String>> icons=new TreeMap<>();
    private long routeId;
    private boolean stations;
    private int page;
    private List<org.mtr.core.data.SimplifiedRoute> routes=List.of();
    public VerticalLineScreen(VerticalLineBlockEntity entity){super(Component.literal(""));this.entity=entity;routeId=entity.settings().routeId();icons.putAll(entity.settings().icons());}
    private String tr(String ru,String en){return minecraft.getLanguageManager().getSelected().startsWith("ru")?ru:en;}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());}
    private int count(){return Math.max(1,(height-125)/26);}
    private org.mtr.core.data.SimplifiedRoute selected(){return routes.stream().filter(r->r.getId()==routeId).findFirst().orElse(routes.isEmpty()?null:routes.get(Math.floorMod(entity.getBlockState().getValue(com.tr1nity.stationsticker.block.VerticalLineBlock.ROUTE),routes.size())));}
    @Override protected void init(){
        routes=List.of();
        var platform=PlatformLocator.findPlatformAt(entity.getBlockPos());
        if(platform!=null){var all=VerticalLineData.routes(platform.area.getId(),platform.getId());var boarding=all.stream().filter(r->VerticalLineData.canBoard(r,platform.area.getId())).toList();routes=boarding.isEmpty()?all:boarding;}
        var route=selected();if(route!=null)routeId=route.getId();
        int left=(width-360)/2;
        button(tr("Линия и направление","Line and direction"),left,35,176,()->{stations=false;page=0;rebuildWidgets();});
        button(tr("Значки станций","Station icons"),left+184,35,176,()->{stations=true;page=0;rebuildWidgets();});
        int total=stations && route!=null?route.getPlatforms().size():routes.size();
        page=Math.min(page,Math.max(0,(total-1)/count()));
        for(int row=0;row<count();row++){
            int index=page*count()+row;if(index>=total)break;int y=65+row*26;
            if(!stations){var candidate=routes.get(index);var stops=candidate.getPlatforms();String direction=CircularDirections.title(candidate.getCircularState());String destination=direction.isEmpty()?StationNames.primary(stops.get(stops.size()-1).getStationName()):StationNames.forCurrentLanguage(direction);
                String name=RouteNumbers.forRoute(candidate)+" "+StationNames.forCurrentLanguage(candidate.getName())+" → "+destination;
                button((candidate.getId()==routeId?"✓ ":"")+name,left,y,360,()->{routeId=candidate.getId();rebuildWidgets();});
            }else{
                var stop=route.getPlatforms().get(index);long station=stop.getStationId();
                button(StationNames.primary(stop.getStationName()),left,y,220,()->{});
                for(int slot=0;slot<3;slot++){int at=slot;var values=icons.getOrDefault(station,List.of());String current=at<values.size()?values.get(at):"";
                    button(current.isEmpty()?"+":"●",left+228+slot*44,y,40,()->minecraft.setScreen(new IconPickerScreen(this,current,id->{var updated=new ArrayList<>(icons.getOrDefault(station,List.of()));while(updated.size()<3)updated.add("");updated.set(at,id);icons.put(station,List.copyOf(updated));})));
                }
            }
        }
        button("←",left,height-52,40,()->{page=Math.max(0,page-1);rebuildWidgets();});
        button("→",left+44,height-52,40,()->{page++;rebuildWidgets();});
        button(tr("Сохранить","Save"),left+100,height-52,124,()->{ModNetwork.CHANNEL.sendToServer(new VerticalLineSettingsPacket(entity.getBlockPos(),new VerticalLineSettings(routeId,icons)));onClose();});
        button(tr("Отмена","Cancel"),left+236,height-52,124,this::onClose);
    }
    private int ticks;
    private String routeSignature="";
    @Override public void tick(){
        if(++ticks%20!=0)return;
        var platform=PlatformLocator.findPlatformAt(entity.getBlockPos());
        String signature=platform==null?"":NavigationRoutes.signature(platform.getId());
        if(!signature.equals(routeSignature)){routeSignature=signature;rebuildWidgets();}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);g.drawCenteredString(font,tr("Вертикальная схема линии","Vertical line diagram"),width/2,15,0xFFFFFF);super.render(g,x,y,partial);
        if(routes.isEmpty())g.drawCenteredString(font,tr("Нет маршрутов: проверьте область станции MTR","No routes: check the MTR station area"),width/2,75,0xCCCCCC);
        if(stations){var route=selected();if(route!=null){int left=(width-360)/2;for(int row=0;row<count();row++){int index=page*count()+row;if(index>=route.getPlatforms().size())break;var ids=icons.getOrDefault(route.getPlatforms().get(index).getStationId(),List.of());for(int i=0;i<ids.size();i++)if(!ids.get(i).isEmpty())IconPickerScreen.drawIcon(g,ids.get(i),left+238+i*44,67+row*26,16);}}}
    }
    @Override public boolean isPauseScreen(){return false;}
}
