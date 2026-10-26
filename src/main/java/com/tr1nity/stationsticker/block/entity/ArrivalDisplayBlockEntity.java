package com.tr1nity.stationsticker.block.entity;

import com.tr1nity.stationsticker.block.ArrivalDisplayBlock;
import com.tr1nity.stationsticker.client.ArrivalChime;
import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

// Данных нет: платформа определяется по позиции блока, включённость звука - в блокстейте
public class ArrivalDisplayBlockEntity extends BlockEntity {

    // Когда (время MTR, мс) прозвучал последний сигнал; только на клиенте
    public long lastChimeMillis;
    // Сглаженное время прибытия, которое показывает таймер, и когда оно обновлялось; только на клиенте
    public long shownArrival;
    public long shownAt;

    public ArrivalDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARRIVAL_DISPLAY.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArrivalDisplayBlockEntity entity) {
        if (state.getValue(ArrivalDisplayBlock.SOUND)) {
            ArrivalChime.tick(level, pos, state, entity);
        }
    }

    // Экран рисуется и в соседней клетке (части), иначе его отсекает по клетке основного блока
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }
}
