package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.StationStickerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Сетевой канал мода. Версия протокола меняется при изменении формата пакетов.
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StationStickerMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(NavSignSettingsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NavSignSettingsPacket::encode)
                .decoder(NavSignSettingsPacket::new)
                .consumerMainThread(NavSignSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(DirectionSignWidthPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DirectionSignWidthPacket::encode)
                .decoder(DirectionSignWidthPacket::new)
                .consumerMainThread(DirectionSignWidthPacket::handle)
                .add();
    }
}
