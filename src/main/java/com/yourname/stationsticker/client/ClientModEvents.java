package com.yourname.stationsticker.client;

import com.yourname.stationsticker.StationStickerMod;
import com.yourname.stationsticker.registration.ModBlockEntities;
import com.yourname.stationsticker.registration.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

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