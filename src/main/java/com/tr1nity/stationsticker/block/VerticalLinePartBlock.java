package com.tr1nity.stationsticker.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;

public class VerticalLinePartBlock extends Block {
    @Override public PushReaction getPistonPushReaction(BlockState state){return PushReaction.BLOCK;}
    public static final IntegerProperty HORIZONTAL = IntegerProperty.create("horizontal", 0, 2);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 3);
    public VerticalLinePartBlock() {
        super(Properties.of(net.minecraft.world.level.material.Material.METAL).strength(1).sound(SoundType.METAL).noOcclusion().noLootTable());
    }
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        return pos.subtract(VerticalLineBlock.offset(state.getValue(VerticalLineBlock.FACING), state.getValue(HORIZONTAL) - 1, state.getValue(ROW)));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VerticalLineBlock.FACING, HORIZONTAL, ROW, VerticalLineBlock.WALL);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VerticalLineBlock.shape(state.getValue(VerticalLineBlock.FACING), state.getValue(HORIZONTAL) - 1, state.getValue(ROW), state.getValue(VerticalLineBlock.WALL));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        BlockPos main = mainPos(pos, state);
        BlockState mainState = level.getBlockState(main);
        return mainState.getBlock() instanceof VerticalLineBlock block ? block.use(mainState, level, main, player, hand, hit) : net.minecraft.world.InteractionResult.PASS;
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState main = level.getBlockState(mainPos(pos, state));
        return main.getBlock() instanceof VerticalLineBlock ? new ItemStack(main.getBlock()) : ItemStack.EMPTY;
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos main = mainPos(pos, state);
        if (!level.isClientSide && level.getBlockState(main).getBlock() instanceof VerticalLineBlock) {
            if (player.isCreative()) level.setBlock(main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            else level.destroyBlock(main, true, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState target, boolean moving) {
        if (!state.is(target.getBlock())) {
            BlockPos main = mainPos(pos, state);
            if (level.getBlockState(main).getBlock() instanceof VerticalLineBlock) level.destroyBlock(main, true);
        }
        super.onRemove(state, level, pos, target, moving);
    }
}
