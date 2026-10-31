package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.NavSignBlockEntity;
import com.tr1nity.stationsticker.client.ClientScreens;
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

// Навигационная табличка с настройкой по шаблону и шириной (клик кисточкой MTR открывает окно)
public class NavSignBlock extends ResizableDisplayBlock {

    public NavSignBlock() {
        super(3);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NavSignBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Настройка только кисточкой MTR, как у остальных блоков мода; иначе клик работает как обычно
        if (!player.getItemInHand(hand).is(org.mtr.mod.Items.BRUSH.get().data) || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.openNavSign(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
