package com.yourname.stationsticker.registration;

import com.yourname.stationsticker.StationStickerMod;
import com.yourname.stationsticker.block.LineSchemeBlock;
import com.yourname.stationsticker.block.StationStickerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, StationStickerMod.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StationStickerMod.MOD_ID);

    // Регистрируем блоки
    public static final RegistryObject<StationStickerBlock> STATION_STICKER =
            registerBlock("station_sticker", StationStickerBlock::new);

    public static final RegistryObject<LineSchemeBlock> LINE_SCHEME =
            registerBlock("line_scheme", LineSchemeBlock::new);

    // ВАЖНО: ЗДЕСЬ БОЛЬШЕ НЕТ ПЕРЕМЕННОЙ STICKER_TAB! Удалите её.

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);

        // Регистрация предмета для блока
        ITEMS.register(name, () -> new BlockItem(registeredBlock.get(), new Item.Properties()));

        return registeredBlock;
    }
}

