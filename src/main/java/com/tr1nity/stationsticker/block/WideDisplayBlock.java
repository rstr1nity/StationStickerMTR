package com.tr1nity.stationsticker.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Экран шириной в несколько клеток на стене или подвесе к потолку: основной блок + DisplayPartBlock
// в соседних клетках справа (если смотреть на экран)
public abstract class WideDisplayBlock extends BaseEntityBlock {
    // Куда смотрит лицевая сторона экрана
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DisplayAttachment> ATTACHMENT = EnumProperty.create("attachment", DisplayAttachment.class);
    public static final int MAX_WIDTH = 8;

    // Форма одной клетки: [facing][attachment]
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][DisplayAttachment.values().length];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DisplayAttachment attachment : DisplayAttachment.values()) {
                SHAPES[facing.get2DDataValue()][attachment.ordinal()] = createShape(facing, attachment);
            }
        }
    }

    protected WideDisplayBlock() {
        super(Properties.of()
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)); // поршень оторвал бы экран от частей

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ATTACHMENT, DisplayAttachment.WALL));
    }

    // Ширина экрана в клетках, от 1 до MAX_WIDTH; может зависеть от состояния (указатель направлений)
    public abstract int width(BlockState state);

    // Направление от основного блока к частям: вправо, если смотреть на экран
    public static Direction partDirection(Direction facing) {
        return facing.getCounterClockWise();
    }

    // Высота корпуса экрана в пикселях клетки, общая для формы и рендера
    public static float bodyBottom(DisplayAttachment attachment) {
        return switch (attachment) {
            case WALL -> 2;
            case CEILING -> 1;
        };
    }

    public static float bodyTop(DisplayAttachment attachment) {
        return switch (attachment) {
            case WALL -> 14;
            case CEILING -> 12;
        };
    }

    public static VoxelShape shapeFor(Direction facing, DisplayAttachment attachment) {
        return SHAPES[facing.get2DDataValue()][attachment.ordinal()];
    }

    private static VoxelShape createShape(Direction facing, DisplayAttachment attachment) {
        double y0 = bodyBottom(attachment);
        double y1 = bodyTop(attachment);
        // Глубина корпуса для facing = NORTH (лицом на север): на стене прижат к южной стороне клетки
        double z0 = attachment == DisplayAttachment.WALL ? 14 : 7;
        double z1 = attachment == DisplayAttachment.WALL ? 16 : 9;
        return switch (facing) {
            case SOUTH -> Block.box(0, y0, 16 - z1, 16, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 0, z1, y1, 16);
            case EAST -> Block.box(16 - z1, y0, 0, 16 - z0, y1, 16);
            default -> Block.box(0, y0, z0, 16, y1, z1);
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING), state.getValue(ATTACHMENT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHMENT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        DisplayAttachment attachment;
        Direction facing;
        if (clickedFace == Direction.UP) {
            return null; // на пол не ставится
        } else if (clickedFace == Direction.DOWN) {
            attachment = DisplayAttachment.CEILING;
            facing = context.getHorizontalDirection().getOpposite();
        } else {
            attachment = DisplayAttachment.WALL;
            facing = clickedFace; // лицом от стены
        }

        // Как кровать: не ставится, если хоть одна клетка экрана занята
        BlockState state = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ATTACHMENT, attachment);
        Level level = context.getLevel();
        for (int offset = 1; offset < width(state); offset++) {
            BlockPos partPos = context.getClickedPos().relative(partDirection(facing), offset);
            if (!canPlacePart(level, partPos) || !level.getBlockState(partPos).canBeReplaced(context)) {
                return null;
            }
        }
        return state;
    }

    protected static boolean canPlacePart(Level level, BlockPos partPos) {
        return !level.isOutsideBuildHeight(partPos) && level.getWorldBorder().isWithinBounds(partPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        for (int offset = 1; offset < width(state); offset++) {
            level.setBlock(pos.relative(partDirection(facing), offset),
                    DisplayPartBlock.partState(facing, state.getValue(ATTACHMENT), offset), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Убираем свои части без выпадения предметов
            Direction facing = state.getValue(FACING);
            for (int offset = 1; offset < width(state); offset++) {
                BlockPos partPos = pos.relative(partDirection(facing), offset);
                BlockState partState = level.getBlockState(partPos);
                if (partState.getBlock() instanceof DisplayPartBlock
                        && DisplayPartBlock.getMainPos(partState, partPos).equals(pos)) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
