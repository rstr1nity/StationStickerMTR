package com.tr1nity.stationsticker.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.tr1nity.stationsticker.block.VerticalLineBlock;
import com.tr1nity.stationsticker.block.entity.VerticalLineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.util.*;

public final class VerticalLineRenderer implements BlockEntityRenderer<VerticalLineBlockEntity> {
    private static final ResourceLocation WHITE=new ResourceLocation("minecraft","textures/misc/white.png");
    private record Entry(VerticalLineData data,ResourceLocation texture) {}
    private static final Map<net.minecraft.core.BlockPos, Entry> TEXTURES=new LinkedHashMap<>();
    private static Object world;
    public VerticalLineRenderer(BlockEntityRendererProvider.Context context) {}
    public static void reset(){TEXTURES.values().forEach(e->Minecraft.getInstance().getTextureManager().release(e.texture()));TEXTURES.clear();world=null;}
    private static ResourceLocation texture(VerticalLineBlockEntity entity){
        if(world!=entity.getLevel()){reset();world=entity.getLevel();}
        var pos=entity.getBlockPos();var previous=TEXTURES.get(pos);
        // Re-evaluate routes on a staggered two-second interval, including while trains are stopped.
        if(previous!=null && (entity.getLevel().getGameTime()+pos.asLong())%40!=0) return previous.texture();
        var data=VerticalLineData.at(pos,entity.getBlockState().getValue(VerticalLineBlock.ROUTE));
        if(previous!=null && Objects.equals(data,previous.data())) return previous.texture();
        Set<Integer> chars=new HashSet<>();"MTR↑По часовой стрелке Против часовой стрелки Посадки нет Clockwise Counterclockwise No boarding".codePoints().forEach(chars::add);
        if(data!=null){data.heading().codePoints().forEach(chars::add);data.number().codePoints().forEach(chars::add);data.stops().forEach(s->{(s.name()+s.secondary()).codePoints().forEach(chars::add);s.transfers().forEach(t->t.number().codePoints().forEach(chars::add));});data.legend().forEach(t->(t.name()+t.number()).codePoints().forEach(chars::add));}
        try{
            var font=NetworkMapFont.load(id->Minecraft.getInstance().getResourceManager().open(new ResourceLocation(id)),chars);
            Map<String,java.awt.image.BufferedImage> icons=new HashMap<>();
            if(data!=null)for(var stop:data.stops())for(String id:stop.icons())if(!icons.containsKey(id)){
                var sign=com.tr1nity.stationsticker.client.display.MtrSigns.get(id);if(sign==null)continue;
                try(var input=Minecraft.getInstance().getResourceManager().open(sign.texture())){
                    var source=javax.imageio.ImageIO.read(input);if(source==null)continue;
                    var icon=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_ARGB);var g=icon.createGraphics();
                    g.setColor(new java.awt.Color(sign.background(),true));g.fillRect(0,0,32,32);
                    g.drawImage(source,sign.flip()?32:0,0,sign.flip()?0:32,32,0,0,source.getWidth(),source.getHeight(),null);g.dispose();source.flush();icons.put(id,icon);
                }catch(java.io.IOException ignored){}
            }
            var image=VerticalLinePainter.paint(data,font,icons);icons.values().forEach(java.awt.image.BufferedImage::flush);
            var nativeImage=NetworkMapImages.create(image);image.flush();
            var texture=Minecraft.getInstance().getTextureManager().register("stationsticker_vertical_line",new DynamicTexture(nativeImage));
            if(previous!=null)Minecraft.getInstance().getTextureManager().release(previous.texture());
            TEXTURES.put(pos.immutable(),new Entry(data,texture));
            while(TEXTURES.size()>16){var it=TEXTURES.entrySet().iterator();var first=it.next();Minecraft.getInstance().getTextureManager().release(first.getValue().texture());it.remove();}
            return texture;
        }catch(java.io.IOException error){org.mtr.mod.Init.LOGGER.error("Could not draw vertical line diagram",error);return previous==null?WHITE:previous.texture();}
    }
    @Override public void render(VerticalLineBlockEntity e,float tick,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        poses.pushPose();poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotationDegrees(-e.getBlockState().getValue(VerticalLineBlock.FACING).toYRot()));
        boolean wall=e.getBlockState().getValue(VerticalLineBlock.WALL);
        if(wall)poses.translate(0,0,-.41);
        if(!wall)panel(poses,buffers,WHITE,-.46f,0,.46f,.55f,.22f,0xFF202124,light);
        panel(poses,buffers,WHITE,-.46f,wall?.05f:.5f,.46f,2.95f,.08f,0xFF303237,light);
        ResourceLocation texture=texture(e);
        for(int side=0;side<(wall?1:2);side++){
            poses.pushPose();if(side==1)poses.mulPose(Axis.YP.rotationDegrees(180));
            surface(buffers.getBuffer(RenderType.text(texture)),poses.last().pose(),-.425f,wall?.09f:.59f,.425f,2.91f,.081f,0xFFFFFFFF,LightTexture.FULL_BRIGHT,.11f);
            poses.popPose();
        }
        poses.popPose();
    }
    private static void panel(PoseStack poses,MultiBufferSource buffers,ResourceLocation texture,float x0,float y0,float x1,float y1,float depth,int color,int light){
        var out=buffers.getBuffer(RenderType.text(texture)); var p=poses.last().pose();
        surface(out,p,x0,y0,x1,y1,depth,color,light,.12f);surface(out,p,x0,y0,x1,y1,-depth,color,light,.12f);
        var outline=WideDisplayRenderer.roundedOutline(x0,y0,x1,y1,.12f);
        for(int i=0;i<outline.length;i++){var a=outline[i];var b=outline[(i+1)%outline.length];vertex(out,p,a[0],a[1],-depth,0,0,color,light);vertex(out,p,b[0],b[1],-depth,1,0,color,light);vertex(out,p,b[0],b[1],depth,1,1,color,light);vertex(out,p,a[0],a[1],depth,0,1,color,light);}
    }
    private static void surface(VertexConsumer out,Matrix4f p,float x0,float y0,float x1,float y1,float z,int color,int light,float radius){
        var outline=WideDisplayRenderer.roundedOutline(x0,y0,x1,y1,radius);float cx=(x0+x1)/2,cy=(y0+y1)/2;
        for(int i=0;i<outline.length;i++)for(int reverse=0;reverse<2;reverse++){
            var a=outline[reverse==0?i:(i+1)%outline.length];var b=outline[reverse==0?(i+1)%outline.length:i];
            vertex(out,p,cx,cy,z,.5f,.5f,color,light);vertex(out,p,a[0],a[1],z,(a[0]-x0)/(x1-x0),1-(a[1]-y0)/(y1-y0),color,light);
            vertex(out,p,b[0],b[1],z,(b[0]-x0)/(x1-x0),1-(b[1]-y0)/(y1-y0),color,light);vertex(out,p,b[0],b[1],z,(b[0]-x0)/(x1-x0),1-(b[1]-y0)/(y1-y0),color,light);
        }
    }
    private static void vertex(VertexConsumer out,Matrix4f p,float x,float y,float z,float u,float v,int c,int light){out.vertex(p,x,y,z).color((c>>>16)&255,(c>>>8)&255,c&255,(c>>>24)&255).uv(u,v).uv2(light).endVertex();}
    @Override public boolean shouldRenderOffScreen(VerticalLineBlockEntity e){return true;}
}
