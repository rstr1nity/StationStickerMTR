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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Невидимая часть схемы линии в соседней клетке: нужна, чтобы схему можно было
// выделить, сломать и покрасить в любой её точке, а не только в центральной клетке
public class LineSchemePartBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Смещение от основного блока, сдвинутое в неотрицательный диапазон
    public static final IntegerProperty OFFSET_H = IntegerProperty.create("offset_h", 0, LineSchemeBlock.HALF_WIDTH * 2);
    public static final IntegerProperty OFFSET_V = IntegerProperty.create("offset_v", 0, LineSchemeBlock.HALF_HEIGHT * 2);

    // Форма всей схемы, сдвинутая в систему координат части: [facing][offset_h][offset_v]
    private static final VoxelShape[][][] SHAPES =
            new VoxelShape[4][LineSchemeBlock.HALF_WIDTH * 2 + 1][LineSchemeBlock.HALF_HEIGHT * 2 + 1];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int h = 0; h <= LineSchemeBlock.HALF_WIDTH * 2; h++) {
                for (int v = 0; v <= LineSchemeBlock.HALF_HEIGHT * 2; v++) {
                    BlockPos offset = LineSchemeBlock.partOffset(facing,
                            h - LineSchemeBlock.HALF_WIDTH, v - LineSchemeBlock.HALF_HEIGHT);
                    SHAPES[facing.get2DDataValue()][h][v] = LineSchemeBlock.shapeFor(facing)
                            .move(-offset.getX(), -offset.getY(), -offset.getZ());
                }
            }
        }
    }

    public LineSchemePartBlock() {
        super(Properties.of(Material.METAL)
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noCollission()
                .noLootTable());

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_H, 0)
                .setValue(OFFSET_V, 0));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    public static BlockState partState(Direction facing, int h, int v) {
        return ModBlocks.LINE_SCHEME_PART.get().defaultBlockState()
                .setValue(FACING, facing)
                .setValue(OFFSET_H, h + LineSchemeBlock.HALF_WIDTH)
                .setValue(OFFSET_V, v + LineSchemeBlock.HALF_HEIGHT);
    }

    public static BlockPos getMainPos(BlockState state, BlockPos pos) {
        BlockPos offset = LineSchemeBlock.partOffset(state.getValue(FACING),
                state.getValue(OFFSET_H) - LineSchemeBlock.HALF_WIDTH,
                state.getValue(OFFSET_V) - LineSchemeBlock.HALF_HEIGHT);
        return pos.subtract(offset);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_H, OFFSET_V);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(OFFSET_H)][state.getValue(OFFSET_V)];
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Клик (например, кисточкой) передаём основному блоку
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (mainState.getBlock() instanceof LineSchemeBlock) {
            return mainState.use(level, player, hand, hit.withPosition(mainPos));
        }
        return InteractionResult.PASS;
    }

    // Игрок ломает часть - ломается вся схема (в творческом режиме без выпадения)
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (!level.isClientSide && mainState.getBlock() instanceof LineSchemeBlock) {
            if (player.isCreative()) {
                level.setBlock(mainPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, mainPos, Block.getId(mainState));
            } else {
                level.destroyBlock(mainPos, true, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // Часть убрана не игроком (взрыв и т.п.) - ломаем схему целиком
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos mainPos = getMainPos(state, pos);
            if (level.getBlockState(mainPos).getBlock() instanceof LineSchemeBlock) {
                level.destroyBlock(mainPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.LINE_SCHEME.get());
    }
}
