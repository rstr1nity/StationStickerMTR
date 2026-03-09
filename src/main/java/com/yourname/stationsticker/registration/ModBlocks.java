package com.yourname.stationsticker.registration;

import com.yourname.stationsticker.StationStickerMod;
import com.yourname.stationsticker.block.StationStickerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, StationStickerMod.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StationStickerMod.MOD_ID);

    // Регистрируем блок
    public static final RegistryObject<StationStickerBlock> STATION_STICKER =
            registerBlock("station_sticker",
                    () -> new StationStickerBlock());

    // В 1.20.1 вкладка создается, но предметы добавляются в неё отдельно
    public static final CreativeModeTab STICKER_TAB = CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.literal("Station Stickers"))
            .icon(() -> new ItemStack(STATION_STICKER.get()))
            .build();

    private static <T extends Block> RegistryObject<T> registerBlock(String name,
                                                                     java.util.function.Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);

        // В 1.20.1 НЕТ .tab() - просто регистрируем предмет
        ITEMS.register(name, () -> new BlockItem(registeredBlock.get(),
                new Item.Properties()));

        return registeredBlock;
    }

    // Метод для добавления предметов в творческую вкладку
    public static void addToTab() {
        // Этот метод будет вызван из события
    }
}