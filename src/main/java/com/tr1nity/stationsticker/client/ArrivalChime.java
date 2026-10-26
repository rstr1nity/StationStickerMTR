package com.tr1nity.stationsticker.client;

import com.tr1nity.stationsticker.block.WideDisplayBlock;
import com.tr1nity.stationsticker.block.entity.ArrivalDisplayBlockEntity;
import com.tr1nity.stationsticker.client.display.ArrivalBoard;
import com.tr1nity.stationsticker.registration.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;

// Сигнал табло прибытия: один раз на каждый поезд, когда до прибытия по расписанию остаётся CHIME_BEFORE_MILLIS.
// Отдельного события «поезд въехал на станцию» MTR клиенту не даёт, поэтому ориентируемся на расписание.
public final class ArrivalChime {

    public static final long CHIME_BEFORE_MILLIS = 5000;
    // Если поезд уже стоит дольше этого (табло только загрузилось), сигнал не играем
    private static final long LATE_MILLIS = 1000;
    private static final float VOLUME = 1.5f;     // больше 1 - слышно дальше 16 блоков

    private ArrivalChime() {
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ArrivalDisplayBlockEntity entity) {
        Platform platform = PlatformLocator.findPlatformAt(pos);
        if (platform == null) return;
        ArrivalResponse arrival = ArrivalBoard.nextArrival(platform.getId());
        if (arrival == null || arrival.getArrival() == entity.chimedArrival) return;

        long remaining = arrival.getArrival() - ArrivalBoard.currentMillis();
        if (remaining > CHIME_BEFORE_MILLIS || remaining < -LATE_MILLIS) return;
        entity.chimedArrival = arrival.getArrival();

        // Звук из середины табло
        Direction toPart = WideDisplayBlock.partDirection(state.getValue(WideDisplayBlock.FACING));
        level.playLocalSound(pos.getX() + 0.5 + toPart.getStepX() * 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 + toPart.getStepZ() * 0.5,
                ModSounds.ARRIVAL_CHIME.get(), SoundSource.BLOCKS, VOLUME, 1.0f, false);
    }
}
