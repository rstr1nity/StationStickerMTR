package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.client.ClientScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

// Экран, ширину которого игрок меняет в окне (ползунок): от MIN_WIDTH до MAX_WIDTH клеток, хранится в блокстейте
public abstract class ResizableDisplayBlock extends WideDisplayBlock {
    public static final int MIN_WIDTH = 2;
    public static final IntegerProperty WIDTH = IntegerProperty.create("width", MIN_WIDTH, MAX_WIDTH);

    protected ResizableDisplayBlock(int defaultWidth) {
        this.registerDefaultState(this.defaultBlockState().setValue(WIDTH, defaultWidth));
    }

    @Override
    public int width(BlockState state) {
        return state.getValue(WIDTH);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WIDTH);
    }

    // Клик кисточкой MTR открывает окно ширины; блоки со своим окном (навигационная табличка) переопределяют
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).is(org.mtr.mod.Items.BRUSH.get().data) || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScreens.openWidthSettings(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Меняет ширину на сервере: достраивает или убирает невидимые клетки справа.
    // false - для расширения не хватает места, ничего не изменено
    public static boolean resize(Level level, BlockPos pos, BlockState state, int newWidth) {
        int oldWidth = state.getValue(WIDTH);
        if (newWidth == oldWidth) return true;
        Direction facing = state.getValue(FACING);
        Direction toParts = partDirection(facing);

        for (int offset = oldWidth; offset < newWidth; offset++) {
            BlockPos partPos = pos.relative(toParts, offset);
            if (!canPlacePart(level, partPos) || !level.getBlockState(partPos).canBeReplaced()) return false;
        }

        // Сначала новая ширина, потом клетки: убранная клетка за пределами ширины не ломает экран
        level.setBlock(pos, state.setValue(WIDTH, newWidth), Block.UPDATE_ALL);
        for (int offset = oldWidth; offset < newWidth; offset++) {
            level.setBlock(pos.relative(toParts, offset),
                    DisplayPartBlock.partState(facing, state.getValue(ATTACHMENT), offset), Block.UPDATE_ALL);
        }
        for (int offset = newWidth; offset < oldWidth; offset++) {
            BlockPos partPos = pos.relative(toParts, offset);
            if (level.getBlockState(partPos).getBlock() instanceof DisplayPartBlock) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return true;
    }
}
