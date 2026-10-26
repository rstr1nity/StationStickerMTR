package com.tr1nity.stationsticker.block;

import com.tr1nity.stationsticker.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Невидимая вторая клетка табло прибытия: чтобы табло выделялось и ломалось по всей ширине
public class ArrivalDisplayPartBlock extends Block {

    public ArrivalDisplayPartBlock() {
        super(Properties.of()
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noLootTable()
                .pushReaction(PushReaction.BLOCK));

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ArrivalDisplayBlock.FACING, Direction.NORTH)
                .setValue(ArrivalDisplayBlock.ATTACHMENT, DisplayAttachment.WALL));
    }

    public static BlockState partState(Direction facing, DisplayAttachment attachment) {
        return ModBlocks.ARRIVAL_DISPLAY_PART.get().defaultBlockState()
                .setValue(ArrivalDisplayBlock.FACING, facing)
                .setValue(ArrivalDisplayBlock.ATTACHMENT, attachment);
    }

    public static BlockPos getMainPos(BlockState state, BlockPos pos) {
        return pos.relative(ArrivalDisplayBlock.partDirection(state.getValue(ArrivalDisplayBlock.FACING)).getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ArrivalDisplayBlock.FACING, ArrivalDisplayBlock.ATTACHMENT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ArrivalDisplayBlock.shapeFor(state.getValue(ArrivalDisplayBlock.FACING), state.getValue(ArrivalDisplayBlock.ATTACHMENT));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // Игрок ломает часть - ломается всё табло (в творческом режиме без выпадения)
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (!level.isClientSide && mainState.getBlock() instanceof ArrivalDisplayBlock) {
            if (player.isCreative()) {
                level.setBlock(mainPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, mainPos, Block.getId(mainState));
            } else {
                level.destroyBlock(mainPos, true, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // Часть убрана не игроком (взрыв и т.п.) - ломаем табло целиком
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos mainPos = getMainPos(state, pos);
            if (level.getBlockState(mainPos).getBlock() instanceof ArrivalDisplayBlock) {
                level.destroyBlock(mainPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.ARRIVAL_DISPLAY.get());
    }
}
