package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.block.DirectionSignBlock;
import com.tr1nity.stationsticker.block.WideDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Клиент -> сервер: игрок выбрал ширину указателя направлений в окне
public record DirectionSignWidthPacket(BlockPos pos, int width) {

    // Дальше этого расстояния (в квадрате) настройки не принимаются: как у ванильных табличек
    private static final double MAX_DISTANCE_SQR = 8 * 8;

    public DirectionSignWidthPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(width);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null || !player.mayBuild()) return;
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) return;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof DirectionSignBlock)) return;

        int clamped = Math.max(DirectionSignBlock.MIN_WIDTH, Math.min(WideDisplayBlock.MAX_WIDTH, width));
        if (!DirectionSignBlock.resize(player.level(), pos, state, clamped)) {
            player.displayClientMessage(Component.translatable("message.stationsticker.direction_sign.no_space"), true);
        }
    }
}
