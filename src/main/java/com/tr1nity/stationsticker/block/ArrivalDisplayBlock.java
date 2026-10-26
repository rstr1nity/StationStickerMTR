package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.ArrivalDisplayBlockEntity;
import com.tr1nity.stationsticker.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// Табло прибытия 2x1. Сигнал перед приходом поезда включается и выключается кисточкой MTR.
public class ArrivalDisplayBlock extends WideDisplayBlock {
    public static final BooleanProperty SOUND = BooleanProperty.create("sound");

    public ArrivalDisplayBlock() {
        this.registerDefaultState(this.defaultBlockState().setValue(SOUND, true));
    }

    @Override
    public int width(BlockState state) {
        return 2;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SOUND);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).is(org.mtr.mod.Items.BRUSH.get().data)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            BlockState newState = state.cycle(SOUND);
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable(newState.getValue(SOUND)
                    ? "message.stationsticker.arrival_display.sound_on"
                    : "message.stationsticker.arrival_display.sound_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArrivalDisplayBlockEntity(pos, state);
    }

    // Сигнал играет только на клиенте: сервер о расписании прибытия не спрашивает
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ModBlockEntities.ARRIVAL_DISPLAY.get(), ArrivalDisplayBlockEntity::clientTick)
                : null;
    }
}
