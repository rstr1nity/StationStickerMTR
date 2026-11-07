package com.tr1nity.stationsticker.network;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.resources.ResourceLocation;import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.*;import com.tr1nity.stationsticker.fabric.compat.PacketContext;import java.util.function.*;
public final class ModNetwork {
 public static final Channel CHANNEL=new Channel();
 public static final ResourceLocation MAP=new ResourceLocation("stationsticker","v8/network_map");
 public static void register(){
  server("vertical_line",VerticalLineSettingsPacket::new,VerticalLineSettingsPacket::handle);
  server("network_request",NetworkMapRequestPacket::new,NetworkMapRequestPacket::handle);
  server("mount",DisplayMountPacket::new,DisplayMountPacket::handle);
  server("nav_sign",NavSignSettingsPacket::new,NavSignSettingsPacket::handle);
  server("width",DisplayWidthPacket::new,DisplayWidthPacket::handle);
 }
 private static <T> void server(String name,Function<FriendlyByteBuf,T> decoder,BiConsumer<T,Supplier<PacketContext.Context>> handler){
  ServerPlayNetworking.registerGlobalReceiver(id(name),(server,player,listener,buf,response)->{T packet=decoder.apply(buf);server.execute(()->handler.accept(packet,()->new PacketContext.Context(player)));});
 }
 public static ResourceLocation id(String name){return new ResourceLocation("stationsticker","v8/"+name);}
 public static final class Channel {
  public void sendToServer(Object packet){com.tr1nity.stationsticker.fabric.FabricClient.send(packet);}
  public void send(ServerPlayer player,NetworkMapPacket packet){var buf=PacketByteBufs.create();packet.encode(buf);ServerPlayNetworking.send(player,MAP,buf);}
 }
}