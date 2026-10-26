package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.block.NavSignOptions;
import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.NavSignOptions.Pictogram;
import com.tr1nity.stationsticker.block.NavSignOptions.Template;
import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Клиент -> сервер: игрок сохранил настройки навигационной таблички в окне
public record NavSignSettingsPacket(BlockPos pos, Template template, Arrow arrow, Pictogram pictogram,
                                    long transferStationId, int transferColor) {

    // Дальше этого расстояния (в квадрате) настройки не принимаются: как у ванильных табличек
    private static final double MAX_DISTANCE_SQR = 8 * 8;

    public NavSignSettingsPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(),
                NavSignOptions.byOrdinal(Template.class, buf.readVarInt(), Template.TRANSFER),
                NavSignOptions.byOrdinal(Arrow.class, buf.readVarInt(), Arrow.LEFT),
                NavSignOptions.byOrdinal(Pictogram.class, buf.readVarInt(), Pictogram.STAIRS),
                buf.readLong(),
                buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(template.ordinal());
        buf.writeVarInt(arrow.ordinal());
        buf.writeVarInt(pictogram.ordinal());
        buf.writeLong(transferStationId);
        buf.writeInt(transferColor);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        if (player.level().getBlockEntity(pos) instanceof NavSignBlockEntity sign) {
            sign.applySettings(template, arrow, pictogram, transferStationId, transferColor);
        }
    }
}
