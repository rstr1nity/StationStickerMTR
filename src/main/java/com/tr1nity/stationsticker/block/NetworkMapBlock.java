package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.block.entity.NetworkMapBlockEntity;
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

// Напольный стенд: 3 клетки по ширине, 4 по высоте; основной блок внизу по центру.
public class NetworkMapBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SCHEMATIC = net.minecraft.world.level.block.state.properties.BooleanProperty.create("schematic");
    public static final float RADIUS = 1.45f;
    public static final float CENTER_Y = 2;
    public static final float BASE_HALF_WIDTH = 1.18f;
    public static final float BASE_HEIGHT = 0.68f;
    public static final float BASE_HALF_DEPTH = 0.22f;
    public NetworkMapBlock() {
        super(Properties.of().strength(1).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SCHEMATIC, false));
    }
    public static BlockPos offset(Direction facing, int h, int row) {
        return BlockPos.ZERO.relative(WideDisplayBlock.partDirection(facing), h).above(row);
    }
    public static Map<BlockPos, BlockState> parts(BlockPos pos, Direction facing) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        for (int row = 0; row < 4; row++) for (int h = -1; h <= 1; h++) {
            if (row != 0 || h != 0) result.put(pos.offset(offset(facing, h, row)),
                    ModBlocks.NETWORK_MAP_PART.get().defaultBlockState().setValue(FACING, facing)
                            .setValue(NetworkMapPartBlock.HORIZONTAL, h + 1).setValue(NetworkMapPartBlock.ROW, row));
        }
        return result;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, SCHEMATIC); }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!player.mayBuild() || !player.getItemInHand(hand).is(org.mtr.mod.Items.BRUSH.get().data)) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {
            boolean schematic = !state.getValue(SCHEMATIC);
            level.setBlock(pos, state.setValue(SCHEMATIC, schematic), Block.UPDATE_ALL);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("display.stationsticker.network_map.mode." + (schematic ? "schematic" : "world")), true);
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
        if (!state.is(target.getBlock())) for (BlockPos part : parts(pos, state.getValue(FACING)).keySet()) {
            BlockState occupied = level.getBlockState(part);
            if (occupied.getBlock() instanceof NetworkMapPartBlock && NetworkMapPartBlock.mainPos(part, occupied).equals(pos)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        super.onRemove(state, level, pos, target, moving);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING), 0, 0);
    }
    public static VoxelShape shape(Direction facing, int h, int row) {
        VoxelShape shape = Shapes.empty();
        // Разбиваем круг на короткие горизонтальные полосы и обрезаем каждую по своей клетке.
        for (int i = 0; i < 8; i++) {
            double y0 = row + i / 8.0, y1 = y0 + 1 / 8.0;
            double distance = Math.max(0, Math.max(CENTER_Y - y1, y0 - CENTER_Y));
            if (distance >= RADIUS) continue;
            double half = Math.sqrt(RADIUS * RADIUS - distance * distance);
            double x0 = Math.max(h - 0.5, -half), x1 = Math.min(h + 0.5, half);
            double clipped0 = Math.max(y0, CENTER_Y - RADIUS), clipped1 = Math.min(y1, CENTER_Y + RADIUS);
            if (x1 > x0 && clipped1 > clipped0) shape = Shapes.or(shape,
                    box(facing, x0 - h + 0.5, clipped0 - row, 0.42, x1 - h + 0.5, clipped1 - row, 0.58));
        }
        if (row == 0) {
            double x0 = Math.max(h - 0.5, -BASE_HALF_WIDTH), x1 = Math.min(h + 0.5, BASE_HALF_WIDTH);
            if (x1 > x0) shape = Shapes.or(shape, box(facing, x0 - h + 0.5, 0, 0.5 - BASE_HALF_DEPTH,
                    x1 - h + 0.5, BASE_HEIGHT, 0.5 + BASE_HALF_DEPTH));
        }
        return shape;
    }
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Shapes.box(x0, y0, z0, x1, y1, z1);
            case NORTH -> Shapes.box(1 - x1, y0, 1 - z1, 1 - x0, y1, 1 - z0);
            case WEST -> Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1);
            default -> Shapes.box(z0, y0, 1 - x1, z1, y1, 1 - x0);
        };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NetworkMapBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
}
