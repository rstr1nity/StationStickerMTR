package com.yourname.stationsticker.block;

import com.yourname.stationsticker.block.entity.StationStickerBlockEntity;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;



public class StationStickerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // Добавь эти константы для тонкого хитбокса:
    protected static final VoxelShape UP_AABB = Block.box(0, 14, 0, 16, 16, 16);
    protected static final VoxelShape DOWN_AABB = Block.box(0, 0, 0, 16, 2, 16);
    protected static final VoxelShape NORTH_AABB = Block.box(0, 0, 14, 16, 16, 16);
    protected static final VoxelShape SOUTH_AABB = Block.box(0, 0, 0, 16, 16, 2);
    protected static final VoxelShape WEST_AABB = Block.box(14, 0, 0, 16, 16, 16);
    protected static final VoxelShape EAST_AABB = Block.box(0, 0, 0, 2, 16, 16);

    public StationStickerBlock() {
        super(Properties.of(Material.METAL)
                .strength(1.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noCollission()); // Важно!
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_AABB;
            case SOUTH -> SOUTH_AABB;
            case WEST -> WEST_AABB;
            case EAST -> EAST_AABB;
            case UP -> UP_AABB;       // ← Добавлено
            case DOWN -> DOWN_AABB;   // ← Добавлено
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace()); // ← Используем сторону клика!
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationStickerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationStickerBlockEntity sticker) {
                player.sendSystemMessage(Component.literal(
                        "ID платформы: " + sticker.getPlatformId()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationStickerBlockEntity sticker) {
                if (stack.hasCustomHoverName()) {
                    sticker.setCustomName(stack.getHoverName().getString());
                }
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide && blockEntity instanceof StationStickerBlockEntity sticker) {
            ItemStack stack = new ItemStack(this);
            if (sticker.getPlatformId() != 0) {
                CompoundTag tag = new CompoundTag();
                tag.putLong("platformId", sticker.getPlatformId());
                stack.addTagElement("BlockEntityTag", tag);
            }
            popResource(level, pos, stack);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}