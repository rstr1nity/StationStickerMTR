package com.tr1nity.stationsticker.registration;

import com.tr1nity.stationsticker.StationStickerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StationStickerMod.MOD_ID);

    // Сигнал табло прибытия перед приходом поезда (файл sounds/arrival_chime.ogg)
    public static final RegistryObject<SoundEvent> ARRIVAL_CHIME = SOUNDS.register("arrival_chime",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StationStickerMod.MOD_ID, "arrival_chime")));
}
