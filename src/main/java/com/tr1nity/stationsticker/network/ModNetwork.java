package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.StationStickerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Сетевой канал мода. Версия протокола меняется при изменении формата пакетов.
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "7";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StationStickerMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(NetworkMapRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NetworkMapRequestPacket::encode).decoder(NetworkMapRequestPacket::new)
                .consumerMainThread(NetworkMapRequestPacket::handle).add();
        CHANNEL.messageBuilder(NetworkMapPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NetworkMapPacket::encode).decoder(NetworkMapPacket::new)
                .consumerMainThread(NetworkMapPacket::handle).add();
        CHANNEL.messageBuilder(DisplayMountPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DisplayMountPacket::encode)
                .decoder(DisplayMountPacket::new)
                .consumerMainThread(DisplayMountPacket::handle)
                .add();
        CHANNEL.messageBuilder(NavSignSettingsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NavSignSettingsPacket::encode)
                .decoder(NavSignSettingsPacket::new)
                .consumerMainThread(NavSignSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(DisplayWidthPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DisplayWidthPacket::encode)
                .decoder(DisplayWidthPacket::new)
                .consumerMainThread(DisplayWidthPacket::handle)
                .add();
    }
}
