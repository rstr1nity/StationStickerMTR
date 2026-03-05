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

    // ИСПРАВЬ: CreativeModeTab (было CreativeMod eTab)
    // ИСПРАВЬ: "stationsticker" (было "stationsticker " с пробелом)
    public static final CreativeModeTab STICKER_TAB = new CreativeModeTab("stationsticker") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(STATION_STICKER.get());
        }
    };

    // ИСПРАВЬ: "station_sticker" (было "station_sticker " с пробелом)
    public static final RegistryObject<StationStickerBlock> STATION_STICKER =
            registerBlock("station_sticker",
                    () -> new StationStickerBlock());

    private static <T extends Block> RegistryObject<T> registerBlock(String name,
                                                                     java.util.function.Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registeredBlock.get(),
                new Item.Properties().tab(STICKER_TAB)));
        return registeredBlock;
    }
}