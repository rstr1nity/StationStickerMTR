package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.block.*;
import com.tr1nity.stationsticker.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("stationsticker")
@PrefixGameTestTemplate(false)
public class DisplayMountGameTests {
    private static void assertTrue(boolean condition,String message){if(!condition)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}

    @GameTest(template = "empty")
    public static void suspensionAndCleanup(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 5, 8));
        WideDisplayBlock block = ModBlocks.ARRIVAL_DISPLAY.get();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState initial = block.defaultBlockState().setValue(WideDisplayBlock.FACING, facing)
                    .setValue(WideDisplayBlock.ATTACHMENT, DisplayAttachment.CEILING);
            level.setBlockAndUpdate(pos, initial);
            block.setPlacedBy(level, pos, initial, null, ItemStack.EMPTY);
            level.setBlockAndUpdate(pos.below(2), Blocks.STONE.defaultBlockState());
            BlockState lowered = initial.setValue(WideDisplayBlock.DROP, 8);
            assertTrue(!WideDisplayBlock.reconfigure(level, pos, initial, lowered), "Must refuse blocked suspension");
            assertTrue(level.getBlockState(pos).equals(initial), "Refused change must preserve main state");
            assertTrue(level.getBlockState(pos.below(2)).is(Blocks.STONE), "Must preserve obstacle");
            level.removeBlock(pos.below(2), false);
            // Все четверти блока, в том числе переходы между вертикальными рядами.
            BlockState current = initial;
            for (int drop = 1; drop <= 8; drop++) {
                BlockState target = current.setValue(WideDisplayBlock.DROP, drop);
                assertTrue(WideDisplayBlock.reconfigure(level, pos, current, target), "Can lower in empty space");
                for (var part : WideDisplayBlock.parts(pos, target).entrySet()) {
                    assertTrue(level.getBlockState(part.getKey()).equals(part.getValue()), "Every part must match geometry");
                    assertTrue(DisplayPartBlock.getMainPos(part.getValue(), part.getKey()).equals(pos), "Part must resolve its owner");
                }
                BlockPos bottom = pos.below(WideDisplayBlock.rows(target));
                assertTrue(!level.getBlockState(bottom).getCollisionShape(level, bottom).isEmpty(), "Lower body must have collision after height changes");
                current = target;
            }
            var extendedParts = WideDisplayBlock.parts(pos, current).keySet();
            assertTrue(WideDisplayBlock.reconfigure(level, pos, current, initial), "Can shorten suspension");
            for (BlockPos part : extendedParts) {
                if (!WideDisplayBlock.parts(pos, initial).containsKey(part)) {
                    assertTrue(level.getBlockState(part).isAir(), "Shortening must remove obsolete rows");
                }
            }
            assertTrue(WideDisplayBlock.reconfigure(level, pos, initial, current), "Can extend suspension again");
            var occupied = WideDisplayBlock.parts(pos, current).keySet();
            // Имитация разрушения нижней части, а не основного блока у потолка.
            level.destroyBlock(pos.below(2), false);
            assertTrue(level.getBlockState(pos).isAir(), "Breaking lower part must remove main block");
            for (BlockPos part : occupied) assertTrue(level.getBlockState(part).isAir(), "No invisible parts left behind");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sideMountAndResize(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 5, 8));
        WideDisplayBlock block = ModBlocks.ARRIVAL_DISPLAY.get();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState wall = block.defaultBlockState().setValue(WideDisplayBlock.FACING, facing);
            level.setBlockAndUpdate(pos, wall);
            block.setPlacedBy(level, pos, wall, null, ItemStack.EMPTY);
            BlockState side = wall.setValue(WideDisplayBlock.ATTACHMENT, DisplayAttachment.SIDE)
                    .setValue(WideDisplayBlock.FACING, facing.getClockWise());
            BlockPos obstruction = pos.relative(facing);
            level.setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
            assertTrue(!WideDisplayBlock.reconfigure(level, pos, wall, side), "Must refuse blocked side mount");
            assertTrue(level.getBlockState(pos).equals(wall), "Refusal must retain wall mount");
            level.removeBlock(obstruction, false);
            assertTrue(WideDisplayBlock.reconfigure(level, pos, wall, side), "Can switch to side mount");
            assertTrue(level.getBlockState(pos.relative(WideDisplayBlock.partDirection(facing))).isAir(), "Old wall part removed");
            assertTrue(WideDisplayBlock.reconfigure(level, pos, side, wall), "Can switch back to wall mount");
            assertTrue(level.getBlockState(obstruction).isAir(), "Old side part removed");
            level.destroyBlock(pos, false);
        }
        // Изменение ширины уже опущенного знака должно обслуживать все ряды.
        ResizableDisplayBlock sign = ModBlocks.DIRECTION_SIGN.get();
        BlockState narrow = sign.defaultBlockState().setValue(WideDisplayBlock.ATTACHMENT, DisplayAttachment.CEILING)
                .setValue(WideDisplayBlock.DROP, 8).setValue(ResizableDisplayBlock.WIDTH, 2);
        level.setBlockAndUpdate(pos, narrow);
        sign.setPlacedBy(level, pos, narrow, null, ItemStack.EMPTY);
        assertTrue(ResizableDisplayBlock.resize(level, pos, narrow, 5), "Can widen lowered sign");
        BlockState wide = level.getBlockState(pos);
        var allParts = WideDisplayBlock.parts(pos, wide).keySet();
        assertTrue(ResizableDisplayBlock.resize(level, pos, wide, 2), "Can narrow lowered sign");
        level.destroyBlock(pos, false);
        for (BlockPos part : allParts) assertTrue(level.getBlockState(part).isAir(), "Resize must leave no stale rows");
        helper.succeed();
    }
}
