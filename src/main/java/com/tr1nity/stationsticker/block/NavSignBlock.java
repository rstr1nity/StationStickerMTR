package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import com.tr1nity.stationsticker.client.NavSignClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

// Навигационная табличка 3x1 с настройкой по шаблону (правый клик открывает окно)
public class NavSignBlock extends WideDisplayBlock {

    @Override
    public int width() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NavSignBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // С блоком в руке игрок, скорее всего, строит рядом, а не настраивает табличку
        if (player.isShiftKeyDown() || !player.mayBuild()) return InteractionResult.PASS;
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NavSignClient.openSettings(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
