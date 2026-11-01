package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.map.NetworkMapData;
import com.tr1nity.stationsticker.network.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.mtr.mod.InitClient;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class NetworkMapCache {
    private static Object connection;
    private static String dimension = "";
    private static long requested;
    private static long generation;
    private static NetworkMapData data;
    private static final LinkedHashMap<String, ResourceLocation> TEXTURES = new LinkedHashMap<>(16, 0.75f, true);
    private static final Set<String> GENERATING = new HashSet<>();
    private static final Map<String, Long> FAILED_UNTIL = new HashMap<>();
    private NetworkMapCache() {}

    public static void reset() {
        generation++;
        TEXTURES.values().forEach(id -> Minecraft.getInstance().getTextureManager().release(id));
        TEXTURES.clear(); GENERATING.clear(); FAILED_UNTIL.clear(); data = null; requested = 0; connection = null; dimension = "";
    }
    private static void ensureWorld() {
        Minecraft mc = Minecraft.getInstance();
        String current = mc.level == null ? "" : mc.level.dimension().location().toString();
        if (connection != mc.getConnection() || !dimension.equals(current)) {
            reset(); connection = mc.getConnection(); dimension = current;
        }
    }
    public static void accept(String world, NetworkMapData snapshot) {
        ensureWorld();
        if (!dimension.equals(world)) return;
        if (!snapshot.equals(data)) {
            TEXTURES.values().forEach(id -> Minecraft.getInstance().getTextureManager().release(id));
            TEXTURES.clear(); GENERATING.clear(); FAILED_UNTIL.clear(); generation++; data = snapshot;
        }
    }
    public static ResourceLocation texture(BlockPos pos) {
        ensureWorld();
        long now = System.currentTimeMillis();
        if (now - requested > 30_000 && Minecraft.getInstance().getConnection() != null) {
            requested = now;
            ModNetwork.CHANNEL.sendToServer(new NetworkMapRequestPacket(pos));
        }
        if (data == null) return null;
        var station = InitClient.findStation(new org.mtr.mapping.holder.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        long stationId = station == null ? 0 : station.getId();
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        String key = stationId + ":" + language;
        ResourceLocation found = TEXTURES.get(key);
        if (found != null || GENERATING.size() >= 2 || FAILED_UNTIL.getOrDefault(key, 0L) > now || !GENERATING.add(key)) return found;
        NetworkMapData snapshot = data;
        long version = generation;
        CompletableFuture.supplyAsync(() -> {
            var image = NetworkMapPainter.paint(snapshot, stationId, language.startsWith("ru"));
            try {
                // Не декодируем PNG через MemoryStack: для большой схемы его не хватает.
                return NetworkMapImages.create(image);
            } finally { image.flush(); }
        }, Util.backgroundExecutor()).whenComplete((image, error) -> Minecraft.getInstance().execute(() -> {
            if (version != generation) {
                if (image != null) image.close();
                return;
            }
            GENERATING.remove(key);
            if (error != null) {
                FAILED_UNTIL.put(key, System.currentTimeMillis() + 30_000);
                org.mtr.mod.Init.LOGGER.error("Could not render Station Sticker network map", error);
                return;
            }
            DynamicTexture texture = null;
            try {
                texture = new DynamicTexture(image);
                ResourceLocation id = Minecraft.getInstance().getTextureManager().register("stationsticker_network_map", texture);
                TEXTURES.put(key, id);
                while (TEXTURES.size() > 8) {
                    var iterator = TEXTURES.entrySet().iterator();
                    Minecraft.getInstance().getTextureManager().release(iterator.next().getValue()); iterator.remove();
                }
            } catch (Exception e) {
                if (texture != null) texture.close();
                else image.close();
                FAILED_UNTIL.put(key, System.currentTimeMillis() + 30_000);
                org.mtr.mod.Init.LOGGER.error("Could not upload Station Sticker network map", e);
            }
        }));
        return null;
    }
}
