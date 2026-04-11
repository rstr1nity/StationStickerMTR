package com.yourname.stationsticker.block;

import com.yourname.stationsticker.block.entity.LineSchemeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.IntegerProperty;


public class LineSchemeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ARROW_DIRECTION = IntegerProperty.create("arrow_direction", 0, 3);
    // Тонкий хитбокс (как наклейка)
    // Хитбокс размером 4х2 блока (64х32 пикселя). Толщина 1.6 пикселя (0.1 блока).
    protected static final VoxelShape NORTH_AABB = Block.box(-24.0D, -8.0D, 14.4D, 40.0D, 24.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(-24.0D, -8.0D, 0.0D, 40.0D, 24.0D, 1.6D);
    protected static final VoxelShape WEST_AABB = Block.box(14.4D, -8.0D, -24.0D, 16.0D, 24.0D, 40.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, -8.0D, -24.0D, 1.6D, 24.0D, 40.0D);


    public LineSchemeBlock() {
        super(Properties.of(Material.METAL)
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_AABB;
            case SOUTH -> SOUTH_AABB;
            case WEST -> WEST_AABB;
            case EAST -> EAST_AABB;
            default -> NORTH_AABB;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ARROW_DIRECTION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ARROW_DIRECTION, 0);

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