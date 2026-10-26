package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.block.NavSignOptions;
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

    private Template template = Template.TRANSFER;
    private Arrow arrow = Arrow.LEFT;
    private Pictogram pictogram = Pictogram.STAIRS;
    // Пересадка: станция MTR и цвет линии; 0 - не выбрана
    private long transferStationId;
    private int transferColor;

    public NavSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAV_SIGN.get(), pos, state);
    }

    public Template getTemplate() {
        return template;
    }

    public Arrow getArrow() {
        return arrow;
    }

    public Pictogram getPictogram() {
        return pictogram;
    }

    public long getTransferStationId() {
        return transferStationId;
    }

    public int getTransferColor() {
        return transferColor;
    }

    // Вызывается на сервере из пакета настройки; рассылает изменения клиентам
    public void applySettings(Template template, Arrow arrow, Pictogram pictogram, long transferStationId, int transferColor) {
        this.template = template;
        this.arrow = arrow;
        this.pictogram = pictogram;
        this.transferStationId = transferStationId;
        this.transferColor = transferColor & 0xFFFFFF;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("template", template.name());
        tag.putString("arrow", arrow.name());
        tag.putString("pictogram", pictogram.name());
        tag.putLong("transferStationId", transferStationId);
        tag.putInt("transferColor", transferColor);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        template = NavSignOptions.byName(Template.class, tag.getString("template"), Template.TRANSFER);
        arrow = NavSignOptions.byName(Arrow.class, tag.getString("arrow"), Arrow.LEFT);
        pictogram = NavSignOptions.byName(Pictogram.class, tag.getString("pictogram"), Pictogram.STAIRS);
        transferStationId = tag.getLong("transferStationId");
        transferColor = tag.getInt("transferColor");
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
        return new AABB(worldPosition).inflate(2);
    }
}
