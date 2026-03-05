//package com.yourname.stationsticker.client;
//
//import com.yourname.stationsticker.StationStickerMod;
//import com.yourname.stationsticker.registration.ModBlockEntities;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.event.EntityRenderersEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//@Mod.EventBusSubscriber(modid = StationStickerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//public class ClientModEvents {
//
//    @SubscribeEvent
//    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
//        event.registerBlockEntityRenderer(
//                ModBlockEntities.STATION_STICKER.get(),
//                StationStickerRenderer::new
//        );
//    }
//}
package com.yourname.stationsticker.client;

import com.yourname.stationsticker.StationStickerMod;
import com.yourname.stationsticker.registration.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StationStickerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.STATION_STICKER.get(),
                StationStickerRenderer::new
        );
    }
}