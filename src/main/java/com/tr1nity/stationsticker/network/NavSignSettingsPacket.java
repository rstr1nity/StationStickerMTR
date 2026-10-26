package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.block.NavSignSettings;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Клиент -> сервер: игрок сохранил настройки навигационной таблички в окне
public record NavSignSettingsPacket(BlockPos pos, NavSignSettings settings) {

    // Дальше этого расстояния (в квадрате) настройки не принимаются: как у ванильных табличек
    private static final double MAX_DISTANCE_SQR = 8 * 8;

    public NavSignSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), NavSignSettings.read(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        settings.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        if (player.level().getBlockEntity(pos) instanceof NavSignBlockEntity sign) {
            sign.applySettings(settings);
        }
    }
}
