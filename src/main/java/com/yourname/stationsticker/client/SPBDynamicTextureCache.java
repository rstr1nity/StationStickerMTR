package com.yourname.stationsticker.client;

import org.mtr.core.data.Platform;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.servlet.MessageQueue;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;
import org.mtr.mod.client.MinecraftClientData; // <-- ДОБАВЛЕН ИМПОРТ
import org.mtr.mod.config.Config;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.MoreRenderLayers;
import org.mtr.mod.data.IGui;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SPBDynamicTextureCache implements IGui {

    private final Object2ObjectLinkedOpenHashMap<String, DynamicResource> dynamicResources = new Object2ObjectLinkedOpenHashMap<>();
    private final ObjectOpenHashSet<String> generatingResources = new ObjectOpenHashSet<>();
    private final MessageQueue<Runnable> resourceRegistryQueue = new MessageQueue<>();
    private final Object2LongArrayMap<Identifier> deletedResources = new Object2LongArrayMap<>();

    public static SPBDynamicTextureCache instance = new SPBDynamicTextureCache();

    private static final int COOLDOWN_TIME = 10000;
    private static final Identifier DEFAULT_BLACK_RESOURCE = new Identifier(Init.MOD_ID, "textures/block/black.png");
    private static final Identifier DEFAULT_WHITE_RESOURCE = new Identifier(Init.MOD_ID, "textures/block/white.png");
    private static final Identifier DEFAULT_TRANSPARENT_RESOURCE = new Identifier(Init.MOD_ID, "textures/block/transparent.png");
    private static final int MAX_IMAGE_SIZE = 2048;

    public void reload() {
        refresh();
    }

    public void refresh() {
        Init.LOGGER.debug("Refreshing dynamic resources; {} textures in memory; {} textures queued to be destroyed", dynamicResources.size(), deletedResources.size());
        dynamicResources.values().forEach(dynamicResource -> dynamicResource.needsRefresh = true);
        generatingResources.clear();
    }

    public void tick() {
        final ObjectArrayList<String> keysToRemove = new ObjectArrayList<>();
        final long currentTimeMillis = System.currentTimeMillis();
        dynamicResources.forEach((checkKey, checkDynamicResource) -> {
            if (checkDynamicResource.expiryTime < currentTimeMillis) {
                checkDynamicResource.remove();
                deletedResources.put(checkDynamicResource.identifier, currentTimeMillis + COOLDOWN_TIME);
                keysToRemove.add(checkKey);
            }
        });
        keysToRemove.forEach(dynamicResources::remove);

        final ObjectArrayList<Identifier> deletedResourcesToRemove = new ObjectArrayList<>();
        deletedResources.forEach((identifier, expiryTime) -> {
            if (expiryTime < currentTimeMillis) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
                deletedResourcesToRemove.add(identifier);
            }
        });
        deletedResourcesToRemove.forEach(deletedResources::removeLong);
    }

    // ================== ИЗМЕНЕНИЯ ЗДЕСЬ ==================

    public DynamicResource getRouteMap(long platformId, boolean vertical, boolean flip, float aspectRatio, boolean transparentWhite) {

        // --- ЭТА СТРОКА РЕШАЕТ ПРОБЛЕМУ ---
        // Отправляем фейковый запрос в оригинальный кэш MTR.
        // Это заставляет MTR загрузить свои шрифты ДО того, как наш SPBRouteMapGenerator попытается ими воспользоваться.
        org.mtr.mod.client.DynamicTextureCache.instance.getPixelatedText("", 0, 10, 1, false);

        // 1. Генерируем "версию" данных
        final long dataVersion = generateDataVersion(platformId);

        // 2. Включаем эту версию в ключ
        String key = String.format("spb_route_map_v3_%s_%s_%s_%s_%s_%s", platformId, dataVersion, vertical, flip, aspectRatio, transparentWhite);

        // 3. Вызываем ваш генератор
        return getResource(key, () -> SPBRouteMapGenerator.generateRouteMap(platformId, vertical, flip, aspectRatio, transparentWhite),
                transparentWhite ? DefaultRenderingColor.TRANSPARENT : DefaultRenderingColor.WHITE);
    }


    /**
     * Создает уникальный хэш (версию) для всех данных, которые влияют на вид схемы.
     * Если этот хэш изменится, значит, схему нужно перерисовать.
     */
    private long generateDataVersion(long platformId) {
        long hash = 1;
        final MinecraftClientData clientData = MinecraftClientData.getInstance();
        if (clientData == null) return 0;

        final Platform platform = clientData.platformIdMap.get(platformId);
        if (platform == null) return 0;

        hash = 31 * hash + platform.getName().hashCode();
        if (platform.area != null) {
            hash = 31 * hash + platform.area.getName().hashCode();
            hash = 31 * hash + platform.area.getColor();
        }

        // Проверяем все маршруты, которые проходят через эту платформу
        for (SimplifiedRoute route : clientData.simplifiedRoutes) {
            if (route.getPlatformIndex(platformId) >= 0) {
                hash = 31 * hash + route.getName().hashCode();
                hash = 31 * hash + route.getColor();
                hash = 31 * hash + route.getCircularState().hashCode();

                // Хэшируем все названия станций в маршруте, чтобы отловить его изменение
                for (SimplifiedRoutePlatform srp : route.getPlatforms()) {
                    hash = 31 * hash + srp.getStationName().hashCode();
                }
            }
        }
        return hash;
    }
    // ================== КОНЕЦ ИЗМЕНЕНИЙ ==================

    private DynamicResource getResource(String key, Supplier<NativeImage> supplier, DefaultRenderingColor defaultRenderingColor) {
        resourceRegistryQueue.process(Runnable::run);
        final DynamicResource dynamicResource = dynamicResources.get(key);

        // Эта проверка теперь будет работать правильно, потому что `key` уже содержит версию данных
        if (dynamicResource != null && !dynamicResource.needsRefresh) {
            dynamicResource.expiryTime = System.currentTimeMillis() + COOLDOWN_TIME;
            return dynamicResource;
        }

        if (generatingResources.contains(key)) {
            return defaultRenderingColor.dynamicResource;
        }

        MainRenderer.WORKER_THREAD.scheduleDynamicTextures(() -> {
            final NativeImage nativeImage = supplier.get();

            resourceRegistryQueue.put(() -> {
                final DynamicResource staticTextureProviderOld = dynamicResources.get(key);
                if (staticTextureProviderOld != null) {
                    staticTextureProviderOld.remove();
                    deletedResources.put(staticTextureProviderOld.identifier, System.currentTimeMillis() + COOLDOWN_TIME);
                }

                final DynamicResource dynamicResourceNew;
                if (nativeImage != null) {
                    final NativeImage newNativeImage;
                    final int newMaxImageSize = MAX_IMAGE_SIZE * (int) Math.pow(2, Config.getClient().getDynamicTextureResolution());
                    if (nativeImage.getWidth() > newMaxImageSize || nativeImage.getHeight() > newMaxImageSize) {
                        newNativeImage = new NativeImage(NativeImageFormat.getAbgrMapped(), Math.min(newMaxImageSize, nativeImage.getWidth()), Math.min(newMaxImageSize, nativeImage.getHeight()), false);
                        for (int x = 0; x < Math.min(newMaxImageSize, nativeImage.getWidth()); x++) {
                            for (int y = 0; y < Math.min(newMaxImageSize, nativeImage.getHeight()); y++) {
                                newNativeImage.setPixelColor(x, y, nativeImage.getColor(x, y));
                            }
                        }
                    } else {
                        newNativeImage = nativeImage;
                    }

                    final NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(newNativeImage);
                    final Identifier identifier = new Identifier("stationsticker", "spb_id_" + Init.randomString());
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new AbstractTexture(nativeImageBackedTexture.data));
                    dynamicResourceNew = new DynamicResource(identifier, nativeImageBackedTexture);
                    dynamicResources.put(key, dynamicResourceNew);
                }

                generatingResources.remove(key);
            });
        });
        SPBRouteMapGenerator.setConstants();
        generatingResources.add(key);

        if (dynamicResource == null) {
            return defaultRenderingColor.dynamicResource;
        } else {
            dynamicResource.expiryTime = System.currentTimeMillis() + COOLDOWN_TIME;
            dynamicResource.needsRefresh = false;
            return dynamicResource;
        }
    }

    public static class DynamicResource {
        private long expiryTime;
        private boolean needsRefresh;
        public final int width;
        public final int height;
        public final Identifier identifier;

        private DynamicResource(Identifier identifier, @Nullable NativeImageBackedTexture nativeImageBackedTexture) {
            this.identifier = identifier;
            if (nativeImageBackedTexture != null) {
                final NativeImage nativeImage = nativeImageBackedTexture.getImage();
                if (nativeImage != null) {
                    width = nativeImage.getWidth();
                    height = nativeImage.getHeight();
                } else {
                    width = 16;
                    height = 16;
                }
            } else {
                width = 16;
                height = 16;
            }
        }

        private void remove() {
            MainRenderer.cancelRender(identifier);
            MoreRenderLayers.removeFromCache(identifier);
        }
    }

    private enum DefaultRenderingColor {
        BLACK(DEFAULT_BLACK_RESOURCE),
        WHITE(DEFAULT_WHITE_RESOURCE),
        TRANSPARENT(DEFAULT_TRANSPARENT_RESOURCE);

        private final DynamicResource dynamicResource;

        DefaultRenderingColor(Identifier identifier) {
            dynamicResource = new DynamicResource(identifier, null);
        }
    }
}
