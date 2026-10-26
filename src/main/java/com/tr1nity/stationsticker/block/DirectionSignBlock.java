package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.DirectionSignBlockEntity;
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

// Указатель направлений; ширина меняется в окне (клик кисточкой MTR), от неё зависит, сколько станций помещается
public class DirectionSignBlock extends ResizableDisplayBlock {

    public DirectionSignBlock() {
        super(3);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).is(org.mtr.mod.Items.BRUSH.get().data) || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.openDirectionSign(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DirectionSignBlockEntity(pos, state);
    }
}
