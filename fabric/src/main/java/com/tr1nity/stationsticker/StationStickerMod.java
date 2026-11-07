package com.tr1nity.stationsticker;
import net.fabricmc.api.ModInitializer;
import com.tr1nity.stationsticker.registration.*;
public final class StationStickerMod implements ModInitializer {
 public static final String MOD_ID="stationsticker";
 public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(MOD_ID);
 @Override public void onInitialize(){
  ModBlocks.BLOCKS.register();ModBlocks.ITEMS.register();ModBlockEntities.BLOCK_ENTITIES.register();ModSounds.SOUNDS.register();
  ModTabs.TABS.register();
  com.tr1nity.stationsticker.network.ModNetwork.register();
  com.tr1nity.stationsticker.fabric.FabricSmokeChecks.install();
 }
}