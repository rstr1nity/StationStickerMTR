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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

// Экран шириной в несколько клеток на стене или подвесе к потолку: основной блок + DisplayPartBlock
// в соседних клетках справа (если смотреть на экран)
public abstract class WideDisplayBlock extends BaseEntityBlock {
    // Куда смотрит лицевая сторона экрана
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DisplayAttachment> ATTACHMENT = EnumProperty.create("attachment", DisplayAttachment.class);
    public static final int MAX_WIDTH = 8;
    // Дополнительное опускание от штатной высоты, шаг 1/4 блока.
    public static final int MAX_DROP = 8;
    public static final IntegerProperty DROP = IntegerProperty.create("drop", 0, MAX_DROP);

    protected WideDisplayBlock() {
        super(Properties.of(net.minecraft.world.level.material.Material.METAL)
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                ); // поршень оторвал бы экран от частей

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(DROP, 0)
                .setValue(ATTACHMENT, DisplayAttachment.WALL));
    }

    // Ширина экрана в клетках, от 1 до MAX_WIDTH; может зависеть от состояния (указатель направлений)
    public abstract int width(BlockState state);

    public static float drop(BlockState state) {
        return state.getValue(ATTACHMENT) == DisplayAttachment.CEILING ? state.getValue(DROP) / 4f : 0;
    }

    public static int rows(BlockState state) {
        return (int) Math.ceil(Math.max(0, drop(state) - 1 / 16f));
    }

    public static Map<BlockPos, BlockState> parts(BlockPos pos, BlockState state) {
        Map<BlockPos, BlockState> parts = new LinkedHashMap<>();
        WideDisplayBlock block = (WideDisplayBlock) state.getBlock();
        for (int row = 0; row <= rows(state); row++) {
            for (int offset = 0; offset < block.width(state); offset++) {
                if (row == 0 && offset == 0) continue;
                parts.put(pos.relative(partDirection(state.getValue(FACING)), offset).below(row),
                        DisplayPartBlock.partState(state.getValue(FACING), state.getValue(ATTACHMENT), offset)
                                .setValue(DisplayPartBlock.ROW, row));
            }
        }
        return parts;
    }

    // Сначала проверяем весь новый объём; при отказе старый экран остаётся целиком.
    public static boolean reconfigure(Level level, BlockPos pos, BlockState state, BlockState target) {
        Map<BlockPos, BlockState> oldParts = parts(pos, state);
        Map<BlockPos, BlockState> newParts = parts(pos, target);
        for (BlockPos partPos : newParts.keySet()) {
            if (!canPlacePart(level, partPos) || !level.isLoaded(partPos)) return false;
            BlockState occupied = level.getBlockState(partPos);
            boolean own = oldParts.containsKey(partPos) && occupied.getBlock() instanceof DisplayPartBlock
                    && DisplayPartBlock.getMainPos(occupied, partPos).equals(pos);
            if (!own && !occupied.getMaterial().isReplaceable()) return false;
        }
        // onRemove старых частей уже не должен разрушать изменённый экран.
        level.setBlock(pos, target, Block.UPDATE_ALL);
        for (BlockPos partPos : oldParts.keySet()) {
            if (!newParts.containsKey(partPos)) {
                BlockState occupied = level.getBlockState(partPos);
                if (occupied.getBlock() instanceof DisplayPartBlock && DisplayPartBlock.getMainPos(occupied, partPos).equals(pos)) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        newParts.forEach((partPos, partState) -> level.setBlock(partPos, partState, Block.UPDATE_ALL));
        return true;
    }

    // Направление от основного блока к частям: вправо, если смотреть на экран
    public static Direction partDirection(Direction facing) {
        return facing.getCounterClockWise();
    }

    // Высота корпуса экрана в пикселях клетки, общая для формы и рендера
    public static float bodyBottom(DisplayAttachment attachment) {
        return switch (attachment) {
            case WALL, SIDE -> 2;
            case CEILING -> 1;
        };
    }

    public static float bodyTop(DisplayAttachment attachment) {
        return switch (attachment) {
            case WALL, SIDE -> 14;
            case CEILING -> 12;
        };
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAt(state, 0, 0);
    }

    public static VoxelShape shapeAt(BlockState state, int offset, int row) {
        DisplayAttachment attachment = state.getValue(ATTACHMENT);
        Direction facing = state.getValue(FACING);
        double y0 = Math.max(0, bodyBottom(attachment) - drop(state) * 16 + row * 16);
        double y1 = Math.min(16, bodyTop(attachment) - drop(state) * 16 + row * 16);
        VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
        if (y1 > y0) shape = localBox(facing, attachment == DisplayAttachment.SIDE && offset == 0 ? 4 : 0,
                y0, attachment == DisplayAttachment.WALL ? 14 : 7, 16, y1, attachment == DisplayAttachment.WALL ? 16 : 9);
        int width = ((WideDisplayBlock) state.getBlock()).width(state);
        if (attachment == DisplayAttachment.CEILING && (offset == 0 || offset == width - 1)) {
            double rodBottom = Math.max(0, bodyTop(attachment) - drop(state) * 16 + row * 16);
            double x = offset == 0 ? 4.8 : 11.2;
            if (rodBottom < 16) shape = net.minecraft.world.phys.shapes.Shapes.or(shape,
                    localBox(facing, x - 0.5, rodBottom, 7.5, x + 0.5, 16, 8.5));
        }
        if (attachment == DisplayAttachment.SIDE && offset == 0) {
            for (double y : new double[]{4, 12}) shape = net.minecraft.world.phys.shapes.Shapes.or(shape,
                    localBox(facing, 0, y - 0.5, 7.5, 5, y + 0.5, 8.5));
        }
        return shape;
    }

    private static VoxelShape localBox(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Block.box(x0, y0, 16 - z1, x1, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, x0, z1, y1, x1);
            case EAST -> Block.box(16 - z1, y0, 16 - x1, 16 - z0, y1, 16 - x0);
            default -> Block.box(16 - x1, y0, z0, 16 - x0, y1, z1);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHMENT, DROP);
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

        parts(pos, state).forEach((partPos, partState) -> level.setBlock(partPos, partState, Block.UPDATE_ALL));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Убираем свои части без выпадения предметов
            for (BlockPos partPos : parts(pos, state).keySet()) {
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
