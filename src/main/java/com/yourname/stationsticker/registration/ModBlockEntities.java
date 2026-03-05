package com.yourname.stationsticker.registration;

import com.yourname.stationsticker.StationStickerMod;
import com.yourname.stationsticker.block.entity.StationStickerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StationStickerMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<StationStickerBlockEntity>> STATION_STICKER =
            BLOCK_ENTITIES.register("station_sticker",
                    () -> BlockEntityType.Builder.of(
                            StationStickerBlockEntity::new,
                            ModBlocks.STATION_STICKER.get()
                    ).build(null));
}
