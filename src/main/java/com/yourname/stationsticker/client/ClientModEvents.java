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
import com.yourname.stationsticker.registration.ModBlocks; // ← Добавь этот импорт!
import net.minecraft.client.renderer.ItemBlockRenderTypes; // ← Добавь!
import net.minecraft.client.renderer.RenderType; // ← Добавь!
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent; // ← Добавь!

@Mod.EventBusSubscriber(modid = StationStickerMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.STATION_STICKER.get(),
                StationStickerRenderer::new
        );
        event.registerBlockEntityRenderer(
                ModBlockEntities.LINE_SCHEME.get(),
                LineSchemeRenderer::new
        );
    }


    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.STATION_STICKER.get(),
                    RenderType.translucent() // или RenderType.cutout()
            );
        });
    }
}