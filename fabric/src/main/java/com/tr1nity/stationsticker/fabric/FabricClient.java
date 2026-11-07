package com.tr1nity.stationsticker.fabric;
import net.fabricmc.api.ClientModInitializer;import net.fabricmc.fabric.api.client.networking.v1.*;import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import com.tr1nity.stationsticker.client.*;import com.tr1nity.stationsticker.registration.*;import com.tr1nity.stationsticker.network.*;
public final class FabricClient implements ClientModInitializer {
 @Override public void onInitializeClient(){
  FabricClientSmoke.install();
  BlockEntityRendererRegistry.register(ModBlockEntities.STATION_STICKER.get(),StationStickerRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.LINE_SCHEME.get(),LineSchemeRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.ARRIVAL_DISPLAY.get(),ArrivalDisplayRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.DIRECTION_SIGN.get(),DirectionSignRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.NAV_SIGN.get(),NavSignRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.INFO_DISPLAY.get(),InfoDisplayRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.NETWORK_MAP.get(),NetworkMapRenderer::new);
  BlockEntityRendererRegistry.register(ModBlockEntities.VERTICAL_LINE.get(),VerticalLineRenderer::new);
  ClientTickEvents.END_CLIENT_TICK.register(client->SPBDynamicTextureCache.instance.tick());
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{NetworkMapCache.reset();VerticalLineRenderer.reset();});
  ClientPlayNetworking.registerGlobalReceiver(ModNetwork.MAP,(client,handler,buf,response)->{var packet=new NetworkMapPacket(buf);client.execute(()->packet.handle(()->new com.tr1nity.stationsticker.fabric.compat.PacketContext.Context(null)));});
  net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STATION_STICKER.get(),net.minecraft.client.renderer.RenderType.translucent());
  net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener(){
   public net.minecraft.resources.ResourceLocation getFabricId(){return ModNetwork.id("textures");}
   public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager resources){net.minecraft.client.Minecraft.getInstance().execute(()->{NetworkMapCache.reset();VerticalLineRenderer.reset();});}
  });
 }
 public static void send(Object packet){var buf=PacketByteBufs.create();String name;
  if(packet instanceof VerticalLineSettingsPacket p){name="vertical_line";p.encode(buf);}else if(packet instanceof NetworkMapRequestPacket p){name="network_request";p.encode(buf);}else if(packet instanceof DisplayMountPacket p){name="mount";p.encode(buf);}else if(packet instanceof NavSignSettingsPacket p){name="nav_sign";p.encode(buf);}else if(packet instanceof DisplayWidthPacket p){name="width";p.encode(buf);}else throw new IllegalArgumentException("Unknown packet");
  ClientPlayNetworking.send(ModNetwork.id(name),buf);
 }
}