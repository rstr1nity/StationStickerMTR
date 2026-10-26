package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Невидимая клетка широкого экрана (WideDisplayBlock): чтобы экран выделялся и ломался по всей ширине
public class DisplayPartBlock extends Block {
    // Сколько клеток до основного блока
    public static final IntegerProperty OFFSET = IntegerProperty.create("offset", 1, WideDisplayBlock.MAX_WIDTH - 1);

    public DisplayPartBlock() {
        super(Properties.of()
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noLootTable()
                .pushReaction(PushReaction.BLOCK));

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WideDisplayBlock.FACING, Direction.NORTH)
                .setValue(WideDisplayBlock.ATTACHMENT, DisplayAttachment.WALL)
                .setValue(OFFSET, 1));
    }

    public static BlockState partState(Direction facing, DisplayAttachment attachment, int offset) {
        return ModBlocks.DISPLAY_PART.get().defaultBlockState()
                .setValue(WideDisplayBlock.FACING, facing)
                .setValue(WideDisplayBlock.ATTACHMENT, attachment)
                .setValue(OFFSET, offset);
    }

    public static BlockPos getMainPos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(WideDisplayBlock.FACING);
        return pos.relative(WideDisplayBlock.partDirection(facing).getOpposite(), state.getValue(OFFSET));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WideDisplayBlock.FACING, WideDisplayBlock.ATTACHMENT, OFFSET);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WideDisplayBlock.shapeFor(state.getValue(WideDisplayBlock.FACING), state.getValue(WideDisplayBlock.ATTACHMENT));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Клик (например, открыть настройки) передаём основному блоку
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (mainState.getBlock() instanceof WideDisplayBlock) {
            return mainState.use(level, player, hand, hit.withPosition(mainPos));
        }
        return InteractionResult.PASS;
    }

    // Игрок ломает часть - ломается весь экран (в творческом режиме без выпадения)
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (!level.isClientSide && mainState.getBlock() instanceof WideDisplayBlock) {
            if (player.isCreative()) {
                level.setBlock(mainPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, mainPos, Block.getId(mainState));
            } else {
                level.destroyBlock(mainPos, true, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // Часть убрана не игроком (взрыв и т.п.) - ломаем экран целиком
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos mainPos = getMainPos(state, pos);
            if (level.getBlockState(mainPos).getBlock() instanceof WideDisplayBlock) {
                level.destroyBlock(mainPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Колесо мыши в творческом режиме даёт предмет основного блока
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState mainState = level.getBlockState(getMainPos(state, pos));
        return mainState.getBlock() instanceof WideDisplayBlock ? new ItemStack(mainState.getBlock()) : ItemStack.EMPTY;
    }
}
