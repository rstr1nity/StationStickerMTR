package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.map.NetworkMapData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record NetworkMapPacket(String dimension, NetworkMapData data) {
    public NetworkMapPacket(FriendlyByteBuf buf) { this(buf.readUtf(256), new NetworkMapData(buf)); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(dimension, 256); data.write(buf); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().setPacketHandled(true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tr1nity.stationsticker.client.NetworkMapCache.accept(dimension, data));
    }
}
