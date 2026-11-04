package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.VerticalLineBlockEntity;
import com.tr1nity.stationsticker.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

// Напольный стенд: 1 клетка по ширине, 3 по высоте; основной блок внизу по центру.
public class VerticalLineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROUTE = net.minecraft.world.level.block.state.properties.IntegerProperty.create("route", 0, 63);
    public VerticalLineBlock() {
        super(Properties.of().strength(1).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROUTE, 0));
    }
    public static BlockPos offset(Direction facing, int h, int row) {
        return BlockPos.ZERO.relative(WideDisplayBlock.partDirection(facing), h).above(row);
    }
    public static Map<BlockPos, BlockState> parts(BlockPos pos, Direction facing) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        for (int row = 0; row < 3; row++) for (int h = 0; h <= 0; h++) {
            if (row != 0 || h != 0) result.put(pos.offset(offset(facing, h, row)),
                    ModBlocks.VERTICAL_LINE_PART.get().defaultBlockState().setValue(FACING, facing)
                            .setValue(VerticalLinePartBlock.HORIZONTAL, h + 1).setValue(VerticalLinePartBlock.ROW, row));
        }
        return result;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ROUTE); }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!player.mayBuild() || !player.getItemInHand(hand).is(org.mtr.mod.Items.BRUSH.get().data)) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(ROUTE), Block.UPDATE_ALL);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) return null;
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        for (BlockPos pos : parts(context.getClickedPos(), facing).keySet()) {
            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced(context)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack stack) {
        super.setPlacedBy(level, pos, state, player, stack);
        if (!level.isClientSide) parts(pos, state.getValue(FACING)).forEach((part, partState) -> level.setBlock(part, partState, Block.UPDATE_ALL));
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState target, boolean moving) {
        if (!state.is(target.getBlock())) for (BlockPos part : java.util.List.of(pos.above(), pos.above(2), pos.above(3))) {
            BlockState occupied = level.getBlockState(part);
            if (occupied.getBlock() instanceof VerticalLinePartBlock && VerticalLinePartBlock.mainPos(part, occupied).equals(pos)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        super.onRemove(state, level, pos, target, moving);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING), 0, 0);
    }
    public static VoxelShape shape(Direction facing, int h, int row) {
        if (row >= 3) return Shapes.empty();
        return box(facing, 0.04, 0, row == 0 ? 0.28 : 0.42, 0.96,
                row == 2 ? 0.95 : 1, row == 0 ? 0.72 : 0.58);
    }
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Shapes.box(x0, y0, z0, x1, y1, z1);
            case NORTH -> Shapes.box(1 - x1, y0, 1 - z1, 1 - x0, y1, 1 - z0);
            case WEST -> Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1);
            default -> Shapes.box(z0, y0, 1 - x1, z1, y1, 1 - x0);
        };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VerticalLineBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
}
