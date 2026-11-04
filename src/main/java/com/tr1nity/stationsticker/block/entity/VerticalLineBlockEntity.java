package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class VerticalLineBlockEntity extends BlockEntity {
    public VerticalLineBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.VERTICAL_LINE.get(), pos, state); }
    private com.tr1nity.stationsticker.block.VerticalLineSettings settings=com.tr1nity.stationsticker.block.VerticalLineSettings.DEFAULT;
    public com.tr1nity.stationsticker.block.VerticalLineSettings settings(){return settings;}
    public void applySettings(com.tr1nity.stationsticker.block.VerticalLineSettings value){settings=value;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.put("Settings",settings.save());}
    @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);settings=com.tr1nity.stationsticker.block.VerticalLineSettings.load(tag.getCompound("Settings"));}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(2).expandTowards(0, 2, 0); }
}
