package com.tr1nity.stationsticker.fabric;
import com.tr1nity.stationsticker.client.*;import com.tr1nity.stationsticker.registration.*;
public final class FabricClientSmoke {
 private static boolean finished;
 public static void install(){
  if(!Boolean.getBoolean("stationsticker.smokeClient"))return;
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(finished || client.screen==null || client.getOverlay()!=null)return;finished=true;
   try{
    var font=NetworkMapFont.load(id->client.getResourceManager().open(new net.minecraft.resources.ResourceLocation(id)),java.util.Set.of((int)'A',(int)'Я',(int)'中'));
    font.size(8);if(font.width("AЯ中")<=0)throw new AssertionError("Minecraft font loading");
    for(var type:java.util.List.of(ModBlockEntities.STATION_STICKER.get(),ModBlockEntities.LINE_SCHEME.get(),ModBlockEntities.ARRIVAL_DISPLAY.get(),ModBlockEntities.DIRECTION_SIGN.get(),ModBlockEntities.NAV_SIGN.get(),ModBlockEntities.INFO_DISPLAY.get(),ModBlockEntities.NETWORK_MAP.get(),ModBlockEntities.VERTICAL_LINE.get())){
     var entity=type.create(net.minecraft.core.BlockPos.ZERO,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
     if(client.getBlockEntityRenderDispatcher().getRenderer(entity)==null)throw new AssertionError("Missing renderer");
    }
    var image=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,0x80cc2233);
    var id=new net.minecraft.resources.ResourceLocation("stationsticker","smoke_texture");client.getTextureManager().register(id,new net.minecraft.client.renderer.texture.DynamicTexture(NetworkMapImages.create(image)));client.getTextureManager().release(id);image.flush();
    com.tr1nity.stationsticker.StationStickerMod.LOGGER.info("FABRIC CLIENT SMOKE PASS: 8 renderers, Minecraft fonts, native texture upload");
   }catch(Exception error){throw new RuntimeException("Fabric client smoke failed",error);}finally{if(Boolean.getBoolean("stationsticker.hotSwapVerification"))try{Thread.sleep(45000);}catch(InterruptedException e){Thread.currentThread().interrupt();}client.stop();}
  });
 }
}
