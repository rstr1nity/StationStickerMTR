package com.tr1nity.stationsticker.registration;

import com.tr1nity.stationsticker.StationStickerMod;
import com.tr1nity.stationsticker.block.ArrivalDisplayBlock;
import com.tr1nity.stationsticker.block.ArrivalDisplayPartBlock;
import com.tr1nity.stationsticker.block.LineSchemeBlock;
import com.tr1nity.stationsticker.block.LineSchemePartBlock;
import com.tr1nity.stationsticker.block.StationStickerBlock;
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

    // Без предмета: ставится только самой схемой линии
    public static final RegistryObject<LineSchemePartBlock> LINE_SCHEME_PART =
            BLOCKS.register("line_scheme_part", LineSchemePartBlock::new);

    public static final RegistryObject<ArrivalDisplayBlock> ARRIVAL_DISPLAY =
            registerBlock("arrival_display", ArrivalDisplayBlock::new);

    // Без предмета: ставится только самим табло
    public static final RegistryObject<ArrivalDisplayPartBlock> ARRIVAL_DISPLAY_PART =
            BLOCKS.register("arrival_display_part", ArrivalDisplayPartBlock::new);

    // ВАЖНО: ЗДЕСЬ БОЛЬШЕ НЕТ ПЕРЕМЕННОЙ STICKER_TAB! Удалите её.

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);

        // Регистрация предмета для блока
        ITEMS.register(name, () -> new BlockItem(registeredBlock.get(), new Item.Properties()));

        return registeredBlock;
    }
}

