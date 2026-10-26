package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.block.NavSignSettings;
import com.tr1nity.stationsticker.block.WideDisplayBlock;
import com.tr1nity.stationsticker.block.NavSignOptions.Arrow;
import com.tr1nity.stationsticker.block.NavSignOptions.Pictogram;
import com.tr1nity.stationsticker.block.NavSignOptions.Template;
import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Навигационная табличка: настройки задаются игроком в окне и рассылаются всем клиентам
public class NavSignBlockEntity extends BlockEntity {

    private NavSignSettings settings = NavSignSettings.DEFAULT;

    public NavSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAV_SIGN.get(), pos, state);
    }

    public NavSignSettings getSettings() {
        return settings;
    }

    public Template getTemplate() {
        return settings.template();
    }

    public Arrow getArrow() {
        return settings.arrow();
    }

    public Pictogram getPictogram() {
        return settings.pictogram();
    }

    public long getTransferStationId() {
        return settings.transferStationId();
    }

    public int getTransferColor() {
        return settings.transferColor();
    }

    // Вызывается на сервере из пакета настройки; рассылает изменения клиентам
    public void applySettings(NavSignSettings settings) {
        this.settings = settings;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        settings.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        settings = NavSignSettings.load(tag);
    }

    // Синхронизация с клиентами: при загрузке чанка и после sendBlockUpdated
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Экран рисуется и в соседних клетках (частях), иначе его отсекает по клетке основного блока
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(WideDisplayBlock.MAX_WIDTH);
    }
}
