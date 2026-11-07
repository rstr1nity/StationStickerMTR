package com.tr1nity.stationsticker.fabric;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
/** Opt-in isolated development smoke test; never runs in a normal installation. */
public final class FabricSmokeChecks {
 public static void install(){
  if(!Boolean.getBoolean("stationsticker.smoke"))return;
  ServerLifecycleEvents.SERVER_STARTED.register(server->{
   var level=server.overworld();int x=0;
   for(String name:new String[]{"station_sticker","line_scheme","arrival_display","direction_sign","nav_sign","info_display","network_map","vertical_line"}){
    var id=new ResourceLocation("stationsticker",name);var block=net.minecraft.core.Registry.BLOCK.get(id);
    if(!net.minecraft.core.Registry.BLOCK.containsKey(id))throw new AssertionError("Missing block "+name);
    var recipe=server.getRecipeManager().byKey(id).orElseThrow(()->new AssertionError("Missing recipe "+name));
    for(var ingredient:recipe.getIngredients())if(ingredient!=net.minecraft.world.item.crafting.Ingredient.EMPTY && ingredient.getItems().length==0)throw new AssertionError("Empty recipe ingredient "+name);
    var pos=new BlockPos(x,180,0);x+=12;level.setBlockAndUpdate(pos,block.defaultBlockState());
    if(level.getBlockEntity(pos)==null)throw new AssertionError("Missing block entity "+name);
   }
   var settings=new com.tr1nity.stationsticker.block.VerticalLineSettings(123,java.util.Map.of(456L,java.util.List.of("test:icon")));
   var packet=new com.tr1nity.stationsticker.network.VerticalLineSettingsPacket(BlockPos.ZERO,settings);
   var buf=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
   try{packet.encode(buf);if(!packet.equals(new com.tr1nity.stationsticker.network.VerticalLineSettingsPacket(buf)))throw new AssertionError("Settings packet roundtrip");}finally{buf.release();}
   com.tr1nity.stationsticker.StationStickerMod.LOGGER.info("FABRIC SMOKE PASS: 8 blocks and block entities, 8 recipes with resolved ingredients, registration and settings packet roundtrip");
   server.execute(()->server.halt(false));
  });
 }
}
