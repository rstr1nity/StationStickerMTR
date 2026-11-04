package com.tr1nity.stationsticker.network;

import com.tr1nity.stationsticker.block.VerticalLineSettings;
import com.tr1nity.stationsticker.block.entity.VerticalLineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record VerticalLineSettingsPacket(BlockPos pos,VerticalLineSettings settings){
    public VerticalLineSettingsPacket(FriendlyByteBuf buf){this(buf.readBlockPos(),VerticalLineSettings.load(java.util.Objects.requireNonNull(buf.readNbt())));}
    public void encode(FriendlyByteBuf buf){buf.writeBlockPos(pos);buf.writeNbt(settings.save());}
    public void handle(Supplier<NetworkEvent.Context> context){
        var player=context.get().getSender();context.get().setPacketHandled(true);
        if(player==null || !player.mayBuild() || !player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter())>64)return;
        if(player.level().getBlockEntity(pos) instanceof VerticalLineBlockEntity entity)entity.applySettings(settings);
    }
}
