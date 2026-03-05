//package com.yourname.stationsticker;
//
//import com.yourname.stationsticker.registration.ModBlocks;
//import com.yourname.stationsticker.registration.ModBlockEntities;
//import net.minecraftforge.eventbus.api.IEventBus;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@Mod(StationStickerMod.MOD_ID)
//public class StationStickerMod {
//    public static final String MOD_ID = "stationsticker";
//    public static final Logger LOGGER = LoggerFactory.getLogger(StationStickerMod.class);
//
//    @SuppressWarnings("removal")
//    public StationStickerMod() {
//        LOGGER.info("Инициализация Station Sticker Addon");
//
//        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
//
//        // Регистрируем все DeferredRegister
//        ModBlocks.BLOCKS.register(modEventBus);
//        ModBlocks.ITEMS.register(modEventBus);
//        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
//    }
//}
package com.yourname.stationsticker;

import com.yourname.stationsticker.registration.ModBlocks;
import com.yourname.stationsticker.registration.ModBlockEntities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(StationStickerMod.MOD_ID)
public class StationStickerMod {
    public static final String MOD_ID = "stationsticker";
    public static final Logger LOGGER = LoggerFactory.getLogger(StationStickerMod.class);

    @SuppressWarnings("removal")
    public StationStickerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        LOGGER.info("Station Sticker Addon initialized");
    }
}