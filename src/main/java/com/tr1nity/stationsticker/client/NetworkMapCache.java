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
    private static final Map<String, Long> TEXTURE_BYTES = new HashMap<>();
    private static final Set<String> GENERATING = new HashSet<>();
    private static final Map<String, Long> FAILED_UNTIL = new HashMap<>();
    private NetworkMapCache() {}

    public static void reset() {
        generation++;
        TEXTURES.values().forEach(id -> Minecraft.getInstance().getTextureManager().release(id));
        TEXTURES.clear(); TEXTURE_BYTES.clear(); GENERATING.clear(); FAILED_UNTIL.clear(); data = null; requested = 0; connection = null; dimension = "";
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
            TEXTURES.clear(); TEXTURE_BYTES.clear(); GENERATING.clear(); FAILED_UNTIL.clear(); generation++; data = snapshot;
        }
    }
    public static NetworkMapData snapshot(BlockPos pos) {
        ensureWorld();
        long now = System.currentTimeMillis();
        if (now - requested > 30_000 && Minecraft.getInstance().getConnection() != null) {
            requested = now;
            ModNetwork.CHANNEL.sendToServer(new NetworkMapRequestPacket(pos));
        }
        return data;
    }
    public static ResourceLocation texture(BlockPos pos) {
        snapshot(pos);
        long now = System.currentTimeMillis();
        if (data == null) return null;
        var station = com.tr1nity.stationsticker.client.PlatformLocator.findStationAt(pos);
        long stationId = station == null ? 0 : station.getId();
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        var state = Minecraft.getInstance().level.getBlockState(pos);
        boolean schematic = state.hasProperty(com.tr1nity.stationsticker.block.NetworkMapBlock.SCHEMATIC) && state.getValue(com.tr1nity.stationsticker.block.NetworkMapBlock.SCHEMATIC);
        String key = stationId + ":" + language + ":" + schematic;
        ResourceLocation found = TEXTURES.get(key);
        if (found != null || GENERATING.size() >= 2 || FAILED_UNTIL.getOrDefault(key, 0L) > now || !GENERATING.add(key)) return found;
        NetworkMapData snapshot = data;
        long version = generation;
        CompletableFuture.supplyAsync(() -> {
            Set<Integer> characters = new HashSet<>();
            snapshot.stations().forEach(s -> s.name().codePoints().forEach(characters::add));
            snapshot.lines().forEach(line -> (line.name() + line.number()).codePoints().forEach(characters::add));
            "СХЕМА МЕТРО METRO NETWORK Station Sticker Вы здесь You are here Нет доступных линий MTR No MTR lines available0123456789…".codePoints().forEach(characters::add);
            NetworkMapFont font;
            try {
                font = NetworkMapFont.load(id -> Minecraft.getInstance().getResourceManager().open(new ResourceLocation(id)), characters);
            } catch (java.io.IOException error) {
                org.mtr.mod.Init.LOGGER.warn("Could not load Minecraft bitmap font for network map", error);
                font = NetworkMapFont.fallback();
            }
            var image = NetworkMapPainter.paint(snapshot, stationId, language.startsWith("ru"), font, schematic ? 3 : NetworkMapPainter.spacingFor(snapshot));
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
                long bytes = (long) image.getWidth() * image.getHeight() * 4;
                texture = new DynamicTexture(image);
                ResourceLocation id = Minecraft.getInstance().getTextureManager().register("stationsticker_network_map", texture);
                TEXTURES.put(key, id);
                TEXTURE_BYTES.put(key, bytes);
                while (TEXTURES.size() > 8 || TEXTURE_BYTES.values().stream().mapToLong(Long::longValue).sum() > 128L * 1024 * 1024) {
                    var iterator = TEXTURES.entrySet().iterator();
                    var oldest = iterator.next(); TEXTURE_BYTES.remove(oldest.getKey());
                    Minecraft.getInstance().getTextureManager().release(oldest.getValue()); iterator.remove();
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
