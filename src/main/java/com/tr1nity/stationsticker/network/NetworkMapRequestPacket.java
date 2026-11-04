package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.block.NetworkMapBlock;
import com.tr1nity.stationsticker.map.MtrNetworkMapResponse;
import com.tr1nity.stationsticker.map.NetworkMapData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mtr.core.data.ClientData;
import org.mtr.core.operation.ListDataResponse;
import org.mtr.mod.Init;
import java.util.*;
import java.util.function.Supplier;

public record NetworkMapRequestPacket(BlockPos pos) {
    private static final Map<ServerLevel, Cached> CACHE = new WeakHashMap<>();
    private static final class Cached {
        NetworkMapData data;
        long fetched;
        long requested;
        final Set<UUID> waiting = new HashSet<>();
        final Map<UUID, Long> lastRequest = new HashMap<>();
    }
    public NetworkMapRequestPacket(FriendlyByteBuf buf) { this(buf.readBlockPos()); }
    public void encode(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null) return;
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > 64 * 64
                || (!(level.getBlockState(pos).getBlock() instanceof NetworkMapBlock)
                && !(level.getBlockState(pos).getBlock() instanceof com.tr1nity.stationsticker.block.VerticalLineBlock))) return;
        Cached cached = CACHE.computeIfAbsent(level, unused -> new Cached());
        long now = System.currentTimeMillis();
        cached.lastRequest.entrySet().removeIf(entry -> now - entry.getValue() > 60_000);
        if (now - cached.lastRequest.getOrDefault(player.getUUID(), 0L) < 2_000) return;
        cached.lastRequest.put(player.getUUID(), now);
        String dimension = level.dimension().location().toString();
        if (cached.data != null && now - cached.fetched < 30_000) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new NetworkMapPacket(dimension, cached.data));
            return;
        }
        cached.waiting.add(player.getUUID());
        if (now - cached.requested < 10_000) return;
        cached.requested = now;
        Init.sendMessageC2S("list_data", new org.mtr.mapping.holder.MinecraftServer(level.getServer()),
                new org.mtr.mapping.holder.World(level), new ListDataResponse(new ClientData()), response -> {
                    cached.data = new MtrNetworkMapResponse(new org.mtr.core.serializer.JsonReader(
                            org.mtr.core.tool.Utilities.getJsonObjectFromData(response))).map;
                    cached.fetched = System.currentTimeMillis();
                    for (UUID uuid : cached.waiting) {
                        var recipient = level.getServer().getPlayerList().getPlayer(uuid);
                        if (recipient != null && recipient.serverLevel() == level) {
                            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new NetworkMapPacket(dimension, cached.data));
                        }
                    }
                    cached.waiting.clear();
                }, ListDataResponse.class);
    }
}
