package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.LineSchemeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class LineSchemeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ARROW_DIRECTION = IntegerProperty.create("arrow_direction", 0, 3);

    // Тонкий хитбокс (как наклейка)
    protected static final VoxelShape NORTH_AABB = Block.box(-24.0D, -8.0D, 14.4D, 40.0D, 24.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(-24.0D, -8.0D, 0.0D, 40.0D, 24.0D, 1.6D);
    protected static final VoxelShape WEST_AABB = Block.box(14.4D, -8.0D, -24.0D, 16.0D, 24.0D, 40.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, -8.0D, -24.0D, 1.6D, 24.0D, 40.0D);

    // Схема перекрывает 5 клеток по горизонтали и 3 по вертикали, в остальные ставятся LineSchemePartBlock
    public static final int HALF_WIDTH = 2;
    public static final int HALF_HEIGHT = 1;

    public LineSchemeBlock() {
        super(Properties.of()
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noCollission() // форма выходит за клетку блока, столкновения с ней работали через раз
                .pushReaction(PushReaction.BLOCK)); // поршень оторвал бы схему от частей

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH));
    }

    public static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case NORTH -> NORTH_AABB;
            case SOUTH -> SOUTH_AABB;
            case WEST -> WEST_AABB;
            case EAST -> EAST_AABB;
            default -> NORTH_AABB;
        };
    }

    // Смещение клетки части: h - вдоль стены, v - по вертикали
    public static BlockPos partOffset(Direction facing, int h, int v) {
        return facing.getAxis() == Direction.Axis.Z ? new BlockPos(h, v, 0) : new BlockPos(0, v, h);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ARROW_DIRECTION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Как кровать: не ставится, если хоть одна клетка схемы занята
        for (int h = -HALF_WIDTH; h <= HALF_WIDTH; h++) {
            for (int v = -HALF_HEIGHT; v <= HALF_HEIGHT; v++) {
                if (h == 0 && v == 0) continue;
                BlockPos partPos = pos.offset(partOffset(facing, h, v));
                if (level.isOutsideBuildHeight(partPos)
                        || !level.getWorldBorder().isWithinBounds(partPos)
                        || !level.getBlockState(partPos).canBeReplaced(context)) {
                    return null;
                }
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ARROW_DIRECTION, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        for (int h = -HALF_WIDTH; h <= HALF_WIDTH; h++) {
            for (int v = -HALF_HEIGHT; v <= HALF_HEIGHT; v++) {
                if (h == 0 && v == 0) continue;
                level.setBlock(pos.offset(partOffset(facing, h, v)),
                        LineSchemePartBlock.partState(facing, h, v), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Убираем свои части без выпадения предметов
            Direction facing = state.getValue(FACING);
            for (int h = -HALF_WIDTH; h <= HALF_WIDTH; h++) {
                for (int v = -HALF_HEIGHT; v <= HALF_HEIGHT; v++) {
                    if (h == 0 && v == 0) continue;
                    BlockPos partPos = pos.offset(partOffset(facing, h, v));
                    BlockState partState = level.getBlockState(partPos);
                    if (partState.getBlock() instanceof LineSchemePartBlock
                            && LineSchemePartBlock.getMainPos(partState, partPos).equals(pos)) {
                        level.setBlock(partPos, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LineSchemeEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);

        // Проверяем, что в руке кисточка MTR
        if (stack.is(org.mtr.mod.Items.BRUSH.get().data)) {
            if (!level.isClientSide) {
                // Переключаем состояние (0 -> 1 -> 2 -> 3 -> 0)
                level.setBlock(pos, state.cycle(ARROW_DIRECTION), 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }
}