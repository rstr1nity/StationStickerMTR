package com.tr1nity.stationsticker.fabric.compat;
public final class PacketContext {
 public record Context(net.minecraft.server.level.ServerPlayer sender){public net.minecraft.server.level.ServerPlayer getSender(){return sender;}public void setPacketHandled(boolean value){}}
}