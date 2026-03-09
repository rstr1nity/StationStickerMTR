package com.yourname.stationsticker.registration;

import com.yourname.stationsticker.StationStickerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    // Регистрируем творческую вкладку
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StationStickerMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> STICKER_TAB = TABS.register("stationsticker_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Station Stickers"))
                    .icon(() -> new ItemStack(ModBlocks.STATION_STICKER.get()))
                    .displayItems((parameters, output) -> {
                        // Добавляем наш блок во вкладку
                        output.accept(ModBlocks.STATION_STICKER.get());
                    })
                    .build());
}