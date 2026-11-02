package com.tr1nity.stationsticker;

import com.tr1nity.stationsticker.block.*;
import com.tr1nity.stationsticker.client.NetworkMapPainter;
import com.tr1nity.stationsticker.map.*;
import com.tr1nity.stationsticker.registration.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.mtr.core.data.ClientData;
import org.mtr.core.operation.ListDataResponse;
import org.mtr.mod.Init;
import java.util.List;

@GameTestHolder("stationsticker")
@PrefixGameTestTemplate(false)
public class NetworkMapGameTests {
    @GameTest(template = "empty")
    public static void extractPublicRoutes(GameTestHelper helper) {
        ClientData data = new ClientData();
        var a = new org.mtr.core.data.Station(data);
        a.setName("А|A");
        a.setCorners(new org.mtr.core.data.Position(-10, -10, -10), new org.mtr.core.data.Position(10, 10, 10));
        var b = new org.mtr.core.data.Station(data);
        b.setName("Б|B");
        b.setCorners(new org.mtr.core.data.Position(90, -10, -10), new org.mtr.core.data.Position(110, 10, 10));
        data.stations.add(a); data.stations.add(b);
        var p = new org.mtr.core.data.Platform(new org.mtr.core.data.Position(0, 0, 0), new org.mtr.core.data.Position(0, 0, 5), org.mtr.core.data.TransportMode.TRAIN, data);
        var q = new org.mtr.core.data.Platform(new org.mtr.core.data.Position(100, 0, 0), new org.mtr.core.data.Position(100, 0, 5), org.mtr.core.data.TransportMode.TRAIN, data);
        data.platforms.add(p); data.platforms.add(q);
        for (int direction = 0; direction < 3; direction++) {
            var route = new org.mtr.core.data.Route(org.mtr.core.data.TransportMode.TRAIN, data);
            route.setName("Линия|Line"); route.setColor(direction == 2 ? 0x00FF00 : 0xCC2233); route.setRouteNumber("1");
            route.setHidden(direction == 2);
            route.getRoutePlatforms().add(new org.mtr.core.data.RoutePlatformData(direction == 1 ? q.getId() : p.getId()));
            route.getRoutePlatforms().add(new org.mtr.core.data.RoutePlatformData(direction == 1 ? p.getId() : q.getId()));
            data.routes.add(route);
        }
        data.sync();
        var response = new ListDataResponse(data).list();
        var map = new MtrNetworkMapResponse(new org.mtr.core.serializer.JsonReader(org.mtr.core.tool.Utilities.getJsonObjectFromData(response))).map;
        helper.assertTrue(map.stations().size() == 2, "Full snapshot preserves station coordinates without nearby-client data");
        helper.assertTrue(map.stations().stream().allMatch(station -> station.z() == p.getMidPosition().getZ()),
                "Map positions use platform midpoints like the MTR website, not station area centers");
        helper.assertTrue(map.lines().size() == 1 && map.edges().size() == 1, "Hidden routes excluded, reverse directions deduplicated");
        var bytes = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            map.write(bytes);
            helper.assertTrue(new NetworkMapData(bytes).equals(map), "Network snapshot survives packet roundtrip");
        } finally { bytes.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void mtrNetworkResponse(GameTestHelper helper) {
        boolean[] received = {false};
        helper.runAfterDelay(5, () -> Init.sendMessageC2S("list_data",
                new org.mtr.mapping.holder.MinecraftServer(helper.getLevel().getServer()),
                new org.mtr.mapping.holder.World(helper.getLevel()), new ListDataResponse(new ClientData()),
                response -> received[0] = new MtrNetworkMapResponse(new org.mtr.core.serializer.JsonReader(
                        org.mtr.core.tool.Utilities.getJsonObjectFromData(response))).map != null, ListDataResponse.class));
        helper.succeedWhen(() -> helper.assertTrue(received[0], "MTR must return full network snapshot without HTTP"));
    }

    @GameTest(template = "empty")
    public static void standCleanupAndShapes(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos main = helper.absolutePos(new BlockPos(8, 1, 8));
        NetworkMapBlock block = ModBlocks.NETWORK_MAP.get();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = block.defaultBlockState().setValue(NetworkMapBlock.FACING, facing);
            level.setBlockAndUpdate(main, state);
            block.setPlacedBy(level, main, state, null, ItemStack.EMPTY);
            var parts = NetworkMapBlock.parts(main, facing);
            helper.assertTrue(parts.size() == 11, "Stand must occupy 3x4 cells including main");
            var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(org.mtr.mod.Items.BRUSH.get().data));
            var clicked = parts.keySet().iterator().next();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked), facing, clicked, false);
            var partState = level.getBlockState(clicked);
            partState.getBlock().use(partState, level, clicked, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(level.getBlockState(main).getValue(NetworkMapBlock.SCHEMATIC), "Brush on a stand part switches its owner to schematic mode");
            helper.assertTrue(level.getBlockEntity(main) != null, "Mode switch preserves the block entity");
            block.use(level.getBlockState(main), level, main, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(!level.getBlockState(main).getValue(NetworkMapBlock.SCHEMATIC), "Second brush click restores original mode");
            for (var part : parts.entrySet()) {
                helper.assertTrue(NetworkMapPartBlock.mainPos(part.getKey(), part.getValue()).equals(main), "Every stand part resolves its owner");
                var shape = level.getBlockState(part.getKey()).getShape(level, part.getKey());
                if (!shape.isEmpty()) {
                    var box = shape.bounds();
                    helper.assertTrue(box.minX >= 0 && box.maxX <= 1 && box.minY >= 0 && box.maxY <= 1 && box.minZ >= 0 && box.maxZ <= 1, "Shapes must stay inside their cell");
                }
            }
            level.destroyBlock(parts.keySet().iterator().next(), false);
            helper.assertTrue(level.getBlockState(main).isAir(), "Breaking a stand part destroys the stand");
            for (BlockPos pos : parts.keySet()) helper.assertTrue(level.getBlockState(pos).isAir(), "No invisible stand parts left");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapImageAndProjection(GameTestHelper helper) throws Exception {
        NetworkMapData map = new NetworkMapData(List.of(
                new NetworkMapData.Station(1, "Площадь|Square", -100, 100),
                new NetworkMapData.Station(2, "Центральная|Central", 0, 0),
                new NetworkMapData.Station(3, "Северная|North", 0, -200),
                new NetworkMapData.Station(4, "Речной вокзал|River terminal", 200, 100)),
                List.of(new NetworkMapData.Edge(1, 2, 0xD92E36), new NetworkMapData.Edge(2, 3, 0xD92E36),
                        new NetworkMapData.Edge(2, 4, 0x1673BB)),
                List.of(new NetworkMapData.Line(0xD92E36, "1", "Красная|Red"), new NetworkMapData.Line(0x1673BB, "2", "Синяя|Blue")));
        var image = NetworkMapPainter.paint(map, 2, true);
        helper.assertTrue((image.getRGB(0, 0) >>> 24) == 0, "Corners of circular texture must be transparent");
        helper.assertTrue((image.getRGB(1024, 1024) >>> 24) == 255, "Face must be opaque");
        for (var point : NetworkMapPainter.layout(map).values()) helper.assertTrue(point.distance(1024, 1010) <= 691, "Entire network must fit the circular face");
        var preview = new java.io.File("../network-map-preview.png");
        javax.imageio.ImageIO.write(image, "PNG", preview);
        helper.succeed();
    }
}
