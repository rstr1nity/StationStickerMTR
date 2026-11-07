package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

// Общая настройка геометрии: сервер проверяет занятый объём до изменения блоков.
public record DisplayMountPacket(BlockPos pos, int width, int drop, boolean side, boolean sound) {
    public DisplayMountPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(width);
        buf.writeVarInt(drop);
        buf.writeBoolean(side);
        buf.writeBoolean(sound);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().setPacketHandled(true);
        if (player == null || !player.mayBuild() || player.isSpectator()) return;
        if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(org.mtr.mod.Items.BRUSH.get().data)
                && !player.getItemInHand(InteractionHand.OFF_HAND).is(org.mtr.mod.Items.BRUSH.get().data)) return;
        if (!player.level.isLoaded(pos) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64
                || !player.level.mayInteract(player, pos)) return;
        BlockState state = player.level.getBlockState(pos);
        if (!(state.getBlock() instanceof WideDisplayBlock)) return;
        BlockState target = state;
        if (state.getBlock() instanceof ResizableDisplayBlock) {
            target = target.setValue(ResizableDisplayBlock.WIDTH, Math.max(2, Math.min(WideDisplayBlock.MAX_WIDTH, width)));
        }
        DisplayAttachment attachment = state.getValue(WideDisplayBlock.ATTACHMENT);
        if (attachment == DisplayAttachment.CEILING) {
            target = target.setValue(WideDisplayBlock.DROP, Math.max(0, Math.min(WideDisplayBlock.MAX_DROP, drop)));
        } else if (state.getBlock() instanceof ArrivalDisplayBlock && side != (attachment == DisplayAttachment.SIDE)) {
            // При боковом креплении цепочка частей идёт от стены в сторону взгляда старого экрана.
            target = target.setValue(WideDisplayBlock.ATTACHMENT, side ? DisplayAttachment.SIDE : DisplayAttachment.WALL)
                    .setValue(WideDisplayBlock.FACING, side ? state.getValue(WideDisplayBlock.FACING).getClockWise()
                            : state.getValue(WideDisplayBlock.FACING).getCounterClockWise());
        }
        if (state.getBlock() instanceof ArrivalDisplayBlock) target = target.setValue(ArrivalDisplayBlock.SOUND, sound);
        for (BlockPos occupied : WideDisplayBlock.parts(pos, state).keySet()) {
            if (!player.level.isLoaded(occupied) || !player.level.mayInteract(player, occupied)) return;
        }
        for (BlockPos occupied : WideDisplayBlock.parts(pos, target).keySet()) {
            if (!player.level.isLoaded(occupied) || !player.level.mayInteract(player, occupied)) return;
        }
        if (!WideDisplayBlock.reconfigure(player.level, pos, state, target)) {
            player.displayClientMessage(Component.translatable("message.stationsticker.display.no_space"), true);
        }
    }
}
